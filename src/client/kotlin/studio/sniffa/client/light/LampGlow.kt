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
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble

object LampGlow {

    private const val SCAN_EVERY_TICKS = 40
    private const val REACH_CHUNKS = 4
    private const val FLOOR_SEARCH = 6
    private const val POOL_LIFT = 0.03f
    private const val FADE_FROM = 48f
    private const val FADE_TO = 64f

    enum class Kind(val colour: Vector3f, val halo: Float, val pool: Float, val gain: Float) {
        LANTERN(Vector3f(1f, 0.62f, 0.28f), 1.5f, 3.2f, 1f),
        SOUL(Vector3f(0.35f, 0.8f, 1f), 1.3f, 2.8f, 0.85f),
        CANDLE(Vector3f(1f, 0.68f, 0.32f), 0.8f, 1.8f, 0.6f),
        FIRE(Vector3f(1f, 0.48f, 0.18f), 2.2f, 4f, 1f),
    }

    private class Lamp(val x: Float, val y: Float, val z: Float, val kind: Kind, val floor: Float?, val phase: Float)

    private class Batch(val originX: Int, val originY: Int, val originZ: Int, val halos: GpuBuffer?, val haloCount: Int, val pools: GpuBuffer?, val poolCount: Int) : AutoCloseable {
        override fun close() {
            halos?.close()
            pools?.close()
        }
    }

    private var batch: Batch? = null
    private var ticks = 0

    @Volatile
    var fires: List<org.joml.Vector3d> = emptyList()
        private set

    private val ADDITIVE = BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)

    private val HALO: RenderPipeline = pipeline("lamp_halo", "core/lamphalo")
    private val POOL: RenderPipeline = pipeline("lamp_pool", "core/lamppool")

    private fun pipeline(name: String, shader: String): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/$name"))
        .withVertexShader(Hideandseek.id(shader))
        .withFragmentShader(Hideandseek.id(shader))
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
        RenderPipelines.register(POOL)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun forget() {
        batch?.close()
        batch = null
        fires = emptyList()
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            forget()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS != 0) return
        val camera = client.gameRenderer.mainCamera().position()
        val origin = BlockPos.containing(camera.x, camera.y, camera.z)
        val lamps = scan(level, origin)
        fires = lamps.filter { it.kind == Kind.FIRE }.map { org.joml.Vector3d(origin.x + it.x.toDouble(), origin.y + it.y.toDouble(), origin.z + it.z.toDouble()) }
        batch?.close()
        batch = build(origin, lamps)
    }

    fun kindOf(state: BlockState): Kind? = when {
        state.`is`(BlockTags.LANTERNS) -> if (state.`is`(Blocks.SOUL_LANTERN)) Kind.SOUL else Kind.LANTERN
        state.`is`(BlockTags.CAMPFIRES) && state.getValueOrElse(BlockStateProperties.LIT, false) ->
            if (state.`is`(Blocks.SOUL_CAMPFIRE)) Kind.SOUL else Kind.FIRE
        (state.`is`(BlockTags.CANDLES) || state.`is`(BlockTags.CANDLE_CAKES)) && state.getValueOrElse(BlockStateProperties.LIT, false) -> Kind.CANDLE
        else -> null
    }

    private fun scan(level: ClientLevel, origin: BlockPos): List<Lamp> {
        val lamps = ArrayList<Lamp>()
        val pos = BlockPos.MutableBlockPos()
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
                    val floor = floorUnder(level, pos, bx, by, bz)
                    lamps += Lamp((bx - origin.x) + 0.5f, (by - origin.y) + lift, (bz - origin.z) + 0.5f, kind, floor?.let { it - origin.y }, phaseOf(bx, by, bz))
                }
            }
        }
        return lamps
    }

    private fun floorUnder(level: ClientLevel, pos: BlockPos.MutableBlockPos, x: Int, y: Int, z: Int): Float? {
        for (dy in 0..FLOOR_SEARCH) {
            pos.set(x, y - dy, z)
            val state = level.getBlockState(pos)
            if (dy == 0) {
                val own = state.getCollisionShape(level, pos)
                if (!own.isEmpty && own.max(Direction.Axis.Y) < 0.5) return (y + own.max(Direction.Axis.Y)).toFloat()
                continue
            }
            if (state.isAir) continue
            val shape = state.getCollisionShape(level, pos)
            if (shape.isEmpty) continue
            return (y - dy + shape.max(Direction.Axis.Y)).toFloat()
        }
        return null
    }

    private fun phaseOf(x: Int, y: Int, z: Int): Float {
        var h = x * 73856093 xor y * 19349663 xor z * 83492791
        h = h xor (h ushr 13)
        h *= 0x5bd1e995
        h = h xor (h ushr 15)
        return (h and 0xffff) / 65535f
    }

    private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

    private fun build(origin: BlockPos, lamps: List<Lamp>): Batch? {
        if (lamps.isEmpty()) return null
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val halos = ByteBufferBuilder(format.vertexSize * 4 * lamps.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (lamp in lamps) for (corner in CORNERS) {
                builder.addVertex(lamp.x, lamp.y, lamp.z)
                    .setUv(corner[0] * lamp.kind.halo, corner[1] * lamp.kind.halo)
                    .setColor(lamp.kind.colour.x * lamp.kind.gain, lamp.kind.colour.y * lamp.kind.gain, lamp.kind.colour.z * lamp.kind.gain, lamp.phase)
            }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa lamp halos" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        val pooled = lamps.filter { it.floor != null }
        val pools = if (pooled.isEmpty()) null else ByteBufferBuilder(format.vertexSize * 4 * pooled.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (lamp in pooled) {
                val floor = lamp.floor!! + POOL_LIFT
                val height = (lamp.y - floor).coerceAtLeast(0.2f)
                val radius = lamp.kind.pool * (0.7f + 0.3f * height / FLOOR_SEARCH)
                val strength = lamp.kind.gain / (1f + height * 0.35f)
                for (corner in CORNERS) {
                    builder.addVertex(lamp.x + corner[0] * radius, floor, lamp.z + corner[1] * radius)
                        .setUv(corner[0], corner[1])
                        .setColor(lamp.kind.colour.x * strength, lamp.kind.colour.y * strength, lamp.kind.colour.z * strength, lamp.phase)
                }
            }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa lamp pools" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        return Batch(origin.x, origin.y, origin.z, halos, lamps.size * 6, pools, pooled.size * 6)
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
        val tint = Vector4f(1f, 1f, 1f, 1f)
        batch.pools?.let { pass(POOL, it, batch.poolCount, offset, tint, values, colour, depth) }
        batch.halos?.let { pass(HALO, it, batch.haloCount, offset, tint, values, colour, depth) }
    }

    private fun pass(
        pipeline: RenderPipeline,
        vertices: GpuBuffer,
        indexCount: Int,
        offset: Vector3f,
        tint: Vector4f,
        values: Matrix4f,
        colour: com.mojang.blaze3d.textures.GpuTextureView,
        depth: com.mojang.blaze3d.textures.GpuTextureView,
    ) {
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), tint, offset, values)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa lamp glow" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
