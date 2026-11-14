package studio.sniffa.client.snow

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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.attribute.EnvironmentAttributes
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ShootingStars {

    private const val SLOT_TICKS = 400L
    private const val CHANCE = 0.45
    private const val DURATION_TICKS = 30f
    private const val DISTANCE = 300f
    private const val TAIL_SHARE = 0.45f
    private const val SEGMENTS = 12
    private const val WIDTH = 2.4f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/shooting_star"))
        .withVertexShader(Hideandseek.id("core/shootingstar"))
        .withFragmentShader(Hideandseek.id("core/shootingstar"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    private class Flight(val from: Vector3f, val to: Vector3f, val progress: Float)

    fun install() {
        RenderPipelines.register(PIPELINE)
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun random(slot: Long, salt: Int): Double {
        var h = slot * -0x61c8864680b583ebL + salt * 0x27d4eb2f165667c5L
        h = (h xor (h ushr 31)) * -0x40a7b892e31b1a47L
        h = h xor (h ushr 29)
        return (h ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    private fun flightAt(ticks: Double): Flight? {
        val slot = (ticks / SLOT_TICKS).toLong()
        if (random(slot, 0) > CHANCE) return null
        val start = slot * SLOT_TICKS + random(slot, 1) * (SLOT_TICKS - DURATION_TICKS)
        val progress = ((ticks - start) / DURATION_TICKS).toFloat()
        if (progress < 0f || progress > 1f) return null
        val azimuth = random(slot, 2) * 2.0 * PI
        val elevation = Math.toRadians(35.0 + random(slot, 3) * 35.0)
        val sweep = Math.toRadians(26.0 + random(slot, 4) * 20.0) * (if (random(slot, 5) < 0.5) 1.0 else -1.0)
        val drop = Math.toRadians(12.0 + random(slot, 6) * 10.0)
        return Flight(direction(azimuth, elevation), direction(azimuth + sweep, elevation - drop), progress)
    }

    private fun direction(azimuth: Double, elevation: Double): Vector3f =
        Vector3f((cos(azimuth) * cos(elevation)).toFloat(), sin(elevation).toFloat(), (sin(azimuth) * cos(elevation)).toFloat())

    private fun along(flight: Flight, t: Float): Vector3f =
        Vector3f(flight.from).lerp(flight.to, t).normalize().mul(DISTANCE)

    private fun draw() {
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val camera = client.gameRenderer.mainCamera()
        val stars = camera.attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partial) as Float
        val dark = (stars * 2.2f).coerceIn(0f, 1f) * (1f - level.getRainLevel(partial))
        if (dark <= 0.02f) return
        val flight = flightAt(level.gameTime + partial.toDouble()) ?: return

        val head = flight.progress
        val tail = (head - TAIL_SHARE).coerceAtLeast(0f)
        val envelope = sin(head * PI.toFloat()).coerceAtLeast(0f)
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * SEGMENTS).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (segment in 0 until SEGMENTS) {
                val s0 = segment / SEGMENTS.toFloat()
                val s1 = (segment + 1) / SEGMENTS.toFloat()
                val p0 = along(flight, tail + (head - tail) * s0)
                val p1 = along(flight, tail + (head - tail) * s1)
                val slope = Vector3f(p1).sub(p0)
                if (slope.lengthSquared() < 1.0e-6f) slope.set(flight.to).sub(flight.from)
                slope.normalize()
                vertex(builder, p0, slope, -1f, s0)
                vertex(builder, p0, slope, 1f, s0)
                vertex(builder, p1, slope, 1f, s1)
                vertex(builder, p1, slope, -1f, s1)
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa shooting star" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, vertices, SEGMENTS * 6, dark * envelope)
                }
            }
        }
    }

    private fun vertex(builder: BufferBuilder, point: Vector3f, slope: Vector3f, side: Float, toward: Float) {
        builder.addVertex(point.x, point.y, point.z)
            .setUv(side, toward)
            .setColor(slope.x * 0.5f + 0.5f, slope.y * 0.5f + 0.5f, slope.z * 0.5f + 0.5f, 1f)
    }

    private fun pass(client: Minecraft, vertices: GpuBuffer, indexCount: Int, strength: Float) {
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            Vector4f(0.85f, 0.92f, 1f, strength),
            Vector3f(),
            Matrix4f().zero().m00(WIDTH),
        )
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa shooting star" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
