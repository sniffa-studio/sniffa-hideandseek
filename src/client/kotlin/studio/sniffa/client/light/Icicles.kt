package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
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
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.floor

object Icicles {

    val ROW = Hideandseek.id("props/eiszapfen")

    private const val SCAN_EVERY_TICKS = 10
    private const val REFIT_EVERY_TICKS = 100
    private const val SHOWN_WITHIN = 128.0
    private const val SPACING = 0.2f
    private const val LONGEST_DEFAULT = 1.2f
    private const val SHORTEST = 0.12f
    private const val BASE_SINK = 0.05f
    private const val LONGEST_ROW = 64f

    private data class Row(val x: Double, val y: Double, val z: Double, val span: Vector3f, val longest: Float)

    private class Mesh(val row: Row, val vertices: GpuBuffer, val indexCount: Int, val light: Float) : AutoCloseable {
        override fun close() = vertices.close()
    }

    private val rows = HashMap<Int, Mesh>()
    private var ticks = 0

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/icicles"))
        .withVertexShader(Hideandseek.id("core/icicle"))
        .withFragmentShader(Hideandseek.id("core/icicle"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun forget() {
        rows.values.forEach { it.close() }
        rows.clear()
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            if (rows.isNotEmpty()) forget()
            return
        }
        if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)
    }

    private fun rowOf(display: Display.ItemDisplay): Row? {
        val stack = display.itemRenderState()?.itemStack() ?: return null
        if (stack.get(DataComponents.ITEM_MODEL) != ROW) return null
        val floats = stack.get(DataComponents.CUSTOM_MODEL_DATA)?.floats() ?: return null
        if (floats.size < 3) return null
        val span = Vector3f(floats[0], floats[1], floats[2])
        val length = span.length()
        if (!length.isFinite() || length < 0.3f || length > LONGEST_ROW) return null
        val asked = floats.getOrNull(3) ?: 0f
        val longest = if (asked > 0f) asked.coerceIn(0.2f, 3f) else LONGEST_DEFAULT
        val at = display.position()
        return Row(at.x, at.y, at.z, span, longest)
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        val refit = ticks % REFIT_EVERY_TICKS == 0
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val row = rowOf(entity) ?: continue
            seen += entity.id
            val known = rows[entity.id]
            if (known == null || known.row != row || refit) {
                known?.close()
                rows[entity.id] = build(level, row)
            }
        }
        val gone = rows.keys - seen
        for (id in gone) rows.remove(id)?.close()
    }

    private fun hash(index: Int, salt: Int): Float {
        var h = index * -0x61c88647 + salt * 0x27d4eb2d
        h = h xor (h ushr 15)
        h *= 0x2c1b3c6d
        h = h xor (h ushr 12)
        return (h and 0xffff) / 65535f
    }

    private fun build(level: ClientLevel, row: Row): Mesh {
        val pos = BlockPos.MutableBlockPos()
        val length = row.span.length()
        val along = Vector3f(row.span).normalize()
        val flat = Vector3f(along.x, 0f, along.z)
        val across = if (flat.lengthSquared() < 1.0e-4f) Vector3f(1f, 0f, 0f) else Vector3f(-flat.z, 0f, flat.x).normalize()
        val count = (length / SPACING).toInt().coerceAtLeast(1)
        val format = DefaultVertexFormat.POSITION_TEX_COLOR

        class Spike(val base: Vector3f, val length: Float, val width: Float, val seed: Float)
        val spikes = ArrayList<Spike>(count)
        for (index in 0 until count) {
            val t = (index + 0.5f + (hash(index, 1) - 0.5f) * 0.6f) / count
            val base = Vector3f(row.span).mul(t.coerceIn(0f, 1f))
            base.add(Vector3f(across).mul((hash(index, 2) - 0.5f) * 0.08f))
            base.y += BASE_SINK
            val shape = hash(index, 3)
            var spike = row.longest * (0.3f + 0.7f * shape * shape)
            if (hash(index, 4) < 0.18f) spike *= 0.35f
            while (spike >= SHORTEST && !clear(level, pos, row, base, spike)) spike *= 0.7f
            if (spike < SHORTEST) continue
            val width = 0.035f + 0.045f * (spike / row.longest) + 0.015f * hash(index, 5)
            spikes += Spike(base, spike, width, hash(index, 6))
        }

        val vertices = ByteBufferBuilder(format.vertexSize * 4 * 4 * spikes.size.coerceAtLeast(1)).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (spike in spikes) {
                val a = Vector3f(along).mul(spike.width)
                val b = Vector3f(across).mul(spike.width)
                val c = spike.base
                val corners = arrayOf(
                    Vector3f(c).add(a).add(b),
                    Vector3f(c).sub(a).add(b),
                    Vector3f(c).sub(a).sub(b),
                    Vector3f(c).add(a).sub(b),
                )
                val outs = arrayOf(Vector3f(across), Vector3f(along).negate(), Vector3f(across).negate(), Vector3f(along))
                val tip = Vector3f(c.x, c.y - spike.length - BASE_SINK, c.z)
                for (face in 0 until 4) {
                    val p = corners[face]
                    val q = corners[(face + 1) % 4]
                    val normal = Vector3f(outs[face]).add(0f, -spike.width / spike.length, 0f).normalize()
                    vertex(builder, p, 0f, face, normal, spike.seed)
                    vertex(builder, q, 0f, face, normal, spike.seed)
                    vertex(builder, tip, 1f, face, normal, spike.seed)
                    vertex(builder, tip, 1f, face, normal, spike.seed)
                }
            }
            if (spikes.isEmpty()) {
                repeat(4) { builder.addVertex(0f, 0f, 0f).setUv(0f, 0f).setColor(0f, 0f, 0f, 0f) }
            }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa icicles" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        val middle = BlockPos.containing(row.x + row.span.x * 0.5, row.y + row.span.y * 0.5 - 0.5, row.z + row.span.z * 0.5)
        val light = level.getMaxLocalRawBrightness(middle) / 15f
        return Mesh(row, vertices, (if (spikes.isEmpty()) 1 else spikes.size * 4) * 6, light)
    }

    private fun vertex(builder: BufferBuilder, point: Vector3f, along: Float, face: Int, normal: Vector3f, seed: Float) {
        builder.addVertex(point.x, point.y, point.z)
            .setUv(along, face.toFloat())
            .setColor(normal.x * 0.5f + 0.5f, normal.y * 0.5f + 0.5f, normal.z * 0.5f + 0.5f, seed)
    }

    private fun clear(level: ClientLevel, pos: BlockPos.MutableBlockPos, row: Row, base: Vector3f, length: Float): Boolean {
        var drop = 0.15f
        while (drop <= length) {
            if (Garland.solidAt(level, pos, row.x + base.x, row.y + base.y - BASE_SINK - drop, row.z + base.z)) return false
            drop += 0.15f
        }
        return !Garland.solidAt(level, pos, row.x + base.x, row.y + base.y - BASE_SINK - length, row.z + base.z)
    }

    private fun draw() {
        if (rows.isEmpty()) return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        for (mesh in rows.values) {
            val row = mesh.row
            val dx = row.x - camera.x
            val dy = row.y - camera.y
            val dz = row.z - camera.z
            if (dx * dx + dy * dy + dz * dz > (SHOWN_WITHIN + row.span.length()).let { it * it }) continue
            val offset = Vector3f(dx.toFloat(), dy.toFloat(), dz.toFloat())
            pass(mesh, offset, colour, depth)
        }
    }

    private fun pass(mesh: Mesh, offset: Vector3f, colour: GpuTextureView, depth: GpuTextureView) {
        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            Vector4f(1f, 1f, 1f, mesh.light),
            offset,
            Matrix4f().zero(),
        )
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa icicles" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(mesh.indexCount), indices.type())
            pass.setVertexBuffer(0, mesh.vertices.slice())
            pass.drawIndexed(mesh.indexCount, 1, 0, 0, 0)
        }
    }
}
