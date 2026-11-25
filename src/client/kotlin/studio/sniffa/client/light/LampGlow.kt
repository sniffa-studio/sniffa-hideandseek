package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.sqrt

object LampGlow {

    private const val SCAN_EVERY_TICKS = 40
    private const val REACH_CHUNKS = 4
    private const val REACH = 64.0
    private const val FADE_FROM = 48f
    private const val FADE_TO = 64f
    private const val POOL_BOOST = 2f
    private const val STALL_HALO_FROM = 0.2f

    enum class Kind(val colour: Vector3f, val halo: Float, val reach: Float, val gain: Float, val flame: Flame) {
        LANTERN(Vector3f(1f, 0.6f, 0.26f), 1.5f, 5f, 1f, Flame.LANTERN),
        SOUL(Vector3f(0.35f, 0.8f, 1f), 1.3f, 4.5f, 0.85f, Flame.LANTERN),
        CANDLE(Vector3f(1f, 0.64f, 0.3f), 0.8f, 3.2f, 0.6f, Flame.CANDLE),
        FIRE(Vector3f(1f, 0.46f, 0.16f), 2.2f, 6.5f, 1f, Flame.FIRE),
    }

    private class Lamp(val x: Float, val y: Float, val z: Float, val colour: Vector3f, val halo: Float, val reach: Float, val flame: Flame, val phase: Float)

    private class Batch(val originX: Int, val originY: Int, val originZ: Int, val halos: GpuBuffer, val haloCount: Int) : AutoCloseable {
        override fun close() = halos.close()
    }

    private var batch: Batch? = null
    private var ticks = 0

    @Volatile
    var fires: List<Vector3d> = emptyList()
        private set

    private val ADDITIVE = BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)

    private val HALO: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/lamp_halo"))
        .withVertexShader(Hideandseek.id("core/lamphalo"))
        .withFragmentShader(Hideandseek.id("core/lamphalo"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(ADDITIVE))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(HALO)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun forget() {
        batch?.close()
        batch = null
        fires = emptyList()
        PropLights.forget()
        WarmLight.publish(WarmLight.Owner.LAMPS, emptyList())
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            if (batch != null) forget()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS != 0) return
        val camera = client.gameRenderer.mainCamera().position()
        val origin = BlockPos.containing(camera.x, camera.y, camera.z)
        val lamps = scan(level, origin)
        fires = lamps.filter { it.flame == Flame.FIRE }.map { Vector3d(origin.x + it.x.toDouble(), origin.y + it.y.toDouble(), origin.z + it.z.toDouble()) }
        props(client, level, origin, lamps)
        batch?.close()
        batch = build(origin, lamps)
        WarmLight.publish(WarmLight.Owner.LAMPS, lamps.map {
            val boost = if (it.flame == Flame.STEADY) 1f else POOL_BOOST
            WarmLight.Source(origin.x + it.x.toDouble(), origin.y + it.y.toDouble(), origin.z + it.z.toDouble(), Vector3f(it.colour).mul(boost), it.reach, it.flame, it.phase)
        })
    }

    fun kindOf(state: BlockState): Kind? = when {
        state.`is`(BlockTags.LANTERNS) -> if (state.`is`(Blocks.SOUL_LANTERN)) Kind.SOUL else Kind.LANTERN
        state.`is`(BlockTags.CAMPFIRES) && state.getValueOrElse(BlockStateProperties.LIT, false) ->
            if (state.`is`(Blocks.SOUL_CAMPFIRE)) Kind.SOUL else Kind.FIRE
        (state.`is`(BlockTags.CANDLES) || state.`is`(BlockTags.CANDLE_CAKES)) && state.getValueOrElse(BlockStateProperties.LIT, false) -> Kind.CANDLE
        else -> null
    }

    private fun scan(level: ClientLevel, origin: BlockPos): MutableList<Lamp> {
        val lamps = ArrayList<Lamp>()
        val centreX = SectionPos.blockToSectionCoord(origin.x)
        val centreZ = SectionPos.blockToSectionCoord(origin.z)
        for (cx in centreX - REACH_CHUNKS..centreX + REACH_CHUNKS) for (cz in centreZ - REACH_CHUNKS..centreZ + REACH_CHUNKS) {
            val chunk = level.chunkSource.getChunk(cx, cz, ChunkStatus.FULL, false) ?: continue
            chunk.sections.forEachIndexed { index, section ->
                if (section.hasOnlyAir() || !section.maybeHas { kindOf(it) != null }) return@forEachIndexed
                val baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index))
                for (y in 0 until 16) for (z in 0 until 16) for (x in 0 until 16) {
                    val state = section.getBlockState(x, y, z)
                    val kind = kindOf(state) ?: continue
                    val bx = (cx shl 4) + x
                    val by = baseY + y
                    val bz = (cz shl 4) + z
                    val hanging = state.getValueOrElse(BlockStateProperties.HANGING, false)
                    val lift = when (kind) {
                        Kind.CANDLE -> 0.55f
                        Kind.FIRE -> 0.6f
                        else -> if (hanging) 0.45f else 0.35f
                    }
                    val colour = Vector3f(kind.colour).mul(kind.gain)
                    lamps += Lamp((bx - origin.x) + 0.5f, (by - origin.y) + lift, (bz - origin.z) + 0.5f, colour, kind.halo, kind.reach, kind.flame, phaseOf(bx, by, bz))
                }
            }
        }
        return lamps
    }

    private fun props(client: Minecraft, level: ClientLevel, origin: BlockPos, lamps: MutableList<Lamp>) {
        val ox = origin.x.toDouble()
        val oy = origin.y.toDouble()
        val oz = origin.z.toDouble()
        val at = Vector3f()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            if (entity.distanceToSqr(ox, oy, oz) > REACH * REACH) continue
            val kind = PropLights.of(client, entity)
            if (kind.emitters.isEmpty()) continue
            val placement = PropLights.placement(entity, ox, oy, oz) ?: continue
            for (emitter in kind.emitters) {
                placement.transformPosition(emitter.at, at)
                val size = sqrt(emitter.area)
                val gain = (0.3f + size * 1.4f).coerceIn(0.3f, 1f)
                val halo = if (kind.stall && size < STALL_HALO_FROM) 0f else (0.3f + size * 1.3f).coerceIn(0.3f, 1.6f) * if (kind.flame == Flame.FIRE) 1.4f else 1f
                val reach = if (kind.stall) (2.6f + size * 4f).coerceIn(2.6f, 4.5f) else (3f + size * 6.5f).coerceIn(3f, 7.5f)
                val colour = Vector3f(emitter.colour).mul(gain)
                lamps += Lamp(at.x, at.y, at.z, colour, halo, reach, kind.flame, phaseOf((at.x * 16f).toInt(), (at.y * 16f).toInt(), (at.z * 16f).toInt()))
            }
        }
    }

    private fun phaseOf(x: Int, y: Int, z: Int): Float {
        var h = x * 73856093 xor y * 19349663 xor z * 83492791
        h = h xor (h ushr 13)
        h *= 0x5bd1e995
        h = h xor (h ushr 15)
        return (h and 0xffff) / 65535f
    }

    private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

    private fun build(origin: BlockPos, all: List<Lamp>): Batch? {
        val lamps = all.filter { it.halo > 0f }
        if (lamps.isEmpty()) return null
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val halos = ByteBufferBuilder(format.vertexSize * 4 * lamps.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (lamp in lamps) {
                val code = lamp.flame.ordinal + 1f + lamp.phase * 0.98f
                for (corner in CORNERS) {
                    builder.addVertex(lamp.x, lamp.y, lamp.z)
                        .setUv(corner[0] * lamp.halo, corner[1] * code)
                        .setColor(lamp.colour.x.coerceAtMost(1f), lamp.colour.y.coerceAtMost(1f), lamp.colour.z.coerceAtMost(1f), 1f)
                }
            }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa lamp halos" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        return Batch(origin.x, origin.y, origin.z, halos, lamps.size * 6)
    }

    private fun draw() {
        val batch = batch ?: return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val offset = Vector3f((batch.originX - camera.x).toFloat(), (batch.originY - camera.y).toFloat(), (batch.originZ - camera.z).toFloat())
        val values = Matrix4f().zero().m00(FADE_FROM).m01(FADE_TO)
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f, 1f, 1f, 1f), offset, values)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa lamp glow" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(HALO)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(batch.haloCount), indices.type())
            pass.setVertexBuffer(0, batch.halos.slice())
            pass.drawIndexed(batch.haloCount, 1, 0, 0, 0)
        }
    }
}
