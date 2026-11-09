package studio.sniffa.client.radar

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.net.payload.TracksPayload
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.cos
import kotlin.math.sin

object Footprints {

    private const val LIFETIME_MILLIS = 20_000L
    private const val FADE_IN_MILLIS = 250L
    private const val LENGTH = 0.56f
    private const val WIDTH = 0.26f
    private const val SIDE_OFFSET = 0.13f
    private const val LIFT = 0.025f
    private const val MAX_PRINTS = 900

    private class Print(val x: Double, val y: Double, val z: Double, val yaw: Float, val left: Boolean, val at: Long)

    private val prints = ArrayDeque<Print>()

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/footprints"))
        .withVertexShader(Hideandseek.id("core/footprint"))
        .withFragmentShader(Hideandseek.id("core/footprint"))
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
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    fun accept(fresh: List<TracksPayload.Print>) {
        val now = System.currentTimeMillis()
        for (print in fresh) {
            prints.addLast(Print(print.x, print.y, print.z, print.yaw, print.left, now))
        }
        while (prints.size > MAX_PRINTS) prints.removeFirst()
    }

    fun forget() {
        prints.clear()
    }

    private fun draw() {
        val now = System.currentTimeMillis()
        while (prints.isNotEmpty() && now - prints.first().at > LIFETIME_MILLIS) prints.removeFirst()
        if (prints.isEmpty()) return

        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val format = DefaultVertexFormat.POSITION_TEX_COLOR

        ByteBufferBuilder(format.vertexSize * 4 * prints.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (print in prints) {
                val age = now - print.at
                val fade = (minOf(1f, age / FADE_IN_MILLIS.toFloat()) * (1f - age / LIFETIME_MILLIS.toFloat())).coerceIn(0f, 1f)
                val radians = Math.toRadians(print.yaw.toDouble())
                val forwardX = (-sin(radians)).toFloat()
                val forwardZ = cos(radians).toFloat()
                val rightX = -forwardZ
                val rightZ = forwardX
                val side = if (print.left) -SIDE_OFFSET else SIDE_OFFSET
                val cx = (print.x - camera.x).toFloat() + rightX * side
                val cz = (print.z - camera.z).toFloat() + rightZ * side
                val y = (print.y - camera.y).toFloat() + LIFT
                val hx = rightX * WIDTH / 2f
                val hz = rightZ * WIDTH / 2f
                val fx = forwardX * LENGTH / 2f
                val fz = forwardZ * LENGTH / 2f
                val mirror = if (print.left) 1f else 0f

                builder.addVertex(cx - hx - fx, y, cz - hz - fz).setUv(0f, 0f).setColor(mirror, 0f, 0f, fade)
                builder.addVertex(cx + hx - fx, y, cz + hz - fz).setUv(1f, 0f).setColor(mirror, 0f, 0f, fade)
                builder.addVertex(cx + hx + fx, y, cz + hz + fz).setUv(1f, 1f).setColor(mirror, 0f, 0f, fade)
                builder.addVertex(cx - hx + fx, y, cz - hz + fz).setUv(0f, 1f).setColor(mirror, 0f, 0f, fade)
            }

            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa footprints" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(vertices, prints.size * 6)
                }
            }
        }
    }

    private fun pass(vertices: GpuBuffer, indexCount: Int) {
        val client = Minecraft.getInstance()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            Vector4f(1f, 1f, 1f, 1f),
            Vector3f(),
            Matrix4f(),
        )
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa footprints" },
            colour,
            Optional.empty(),
            depth,
            OptionalDouble.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
