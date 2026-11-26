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
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.max
import kotlin.math.sqrt

object SkyBeams {

    private const val SEGMENTS = 28
    private const val SKY = 480.0
    private const val SHORTEST = 160.0
    private const val NARROWEST = 0.5f
    private const val WIDENING = 0.006f

    private class Light(val x: Double, val y: Double, val z: Double, val red: Float, val green: Float, val blue: Float) {
        val height: Double = max(SKY - y, SHORTEST)
    }

    private val lights = LinkedHashMap<Int, Light>()
    private var seenLevel: ClientLevel? = null

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/sky_beam"))
        .withVertexShader(Hideandseek.id("core/skybeam"))
        .withFragmentShader(Hideandseek.id("core/skybeam"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> watchLevel(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    fun accept(id: Int, x: Double, y: Double, z: Double, argb: Int, on: Boolean) {
        if (!on) {
            lights.remove(id)
            return
        }
        val alpha = (argb ushr 24) and 0xFF
        val strength = if (alpha == 0) 1f else alpha / 255f
        val red = ((argb shr 16) and 0xFF) / 255f * strength
        val green = ((argb shr 8) and 0xFF) / 255f * strength
        val blue = (argb and 0xFF) / 255f * strength
        lights[id] = Light(x, y, z, red, green, blue)
    }

    fun forget() {
        lights.clear()
        seenLevel = null
    }

    private fun watchLevel(client: Minecraft) {
        val level = client.level
        if (level !== seenLevel) {
            lights.clear()
            seenLevel = level
        }
    }

    private fun draw() {
        if (lights.isEmpty()) return
        val client = Minecraft.getInstance()
        if (client.level == null) return
        val camera = client.gameRenderer.mainCamera().position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val farthest = client.options.effectiveRenderDistance * 16f * 2f

        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val quads = lights.size * SEGMENTS
        ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (light in lights.values) {
                val dx = (light.x - camera.x).toFloat()
                val dy = (light.y - camera.y).toFloat()
                val dz = (light.z - camera.z).toFloat()
                val flat = sqrt(dx * dx + dz * dz)
                val sideX = if (flat > 1.0e-3f) -dz / flat else 1f
                val sideZ = if (flat > 1.0e-3f) dx / flat else 0f
                var lower = 0f
                for (segment in 1..SEGMENTS) {
                    val t = segment / SEGMENTS.toFloat()
                    val upper = (light.height * t * t).toFloat()
                    ribbon(builder, dx, dy, dz, flat, sideX, sideZ, lower, upper, light)
                    lower = upper
                }
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa sky beams" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    val values = Matrix4f().zero().m00(farthest)
                    val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f), Vector3f(), values)
                    val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
                    val count = quads * 6
                    RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa sky beams" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
                        pass.setPipeline(PIPELINE)
                        RenderSystem.bindDefaultUniforms(pass)
                        pass.setUniform("DynamicTransforms", transform)
                        pass.setIndexBuffer(indices.getBuffer(count), indices.type())
                        pass.setVertexBuffer(0, vertices.slice())
                        pass.drawIndexed(count, 1, 0, 0, 0)
                    }
                }
            }
        }
    }

    private fun ribbon(builder: BufferBuilder, dx: Float, dy: Float, dz: Float, flat: Float, sideX: Float, sideZ: Float, lower: Float, upper: Float, light: Light) {
        val lowWidth = widthAt(flat, dy + lower)
        val highWidth = widthAt(flat, dy + upper)
        val share = light.height.toFloat()
        vertex(builder, dx - sideX * lowWidth, dy + lower, dz - sideZ * lowWidth, -1f, lower, lower / share, light)
        vertex(builder, dx + sideX * lowWidth, dy + lower, dz + sideZ * lowWidth, 1f, lower, lower / share, light)
        vertex(builder, dx + sideX * highWidth, dy + upper, dz + sideZ * highWidth, 1f, upper, upper / share, light)
        vertex(builder, dx - sideX * highWidth, dy + upper, dz - sideZ * highWidth, -1f, upper, upper / share, light)
    }

    private fun widthAt(flat: Float, rise: Float): Float = max(NARROWEST, sqrt(flat * flat + rise * rise) * WIDENING)

    private fun vertex(builder: BufferBuilder, x: Float, y: Float, z: Float, across: Float, rise: Float, along: Float, light: Light) {
        builder.addVertex(x, y, z).setUv(across, rise).setColor(light.red, light.green, light.blue, along)
    }
}
