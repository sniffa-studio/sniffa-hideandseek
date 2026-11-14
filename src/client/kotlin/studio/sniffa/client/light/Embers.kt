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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.cos
import kotlin.math.sin

object Embers {

    private const val SHOWN_WITHIN = 48.0
    private const val FIRE_RATE = 0.4f
    private const val CHIMNEY_RATE = 0.2f
    private const val MOST = 600
    private const val SIZE = 0.085f

    private class Ember(var x: Double, var y: Double, var z: Double, val rise: Double, val sway: Double, val phase: Double, val life: Float) {
        var age = 0f
    }

    private val embers = ArrayList<Ember>()
    private var owed = 0f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/embers"))
        .withVertexShader(Hideandseek.id("core/ember"))
        .withFragmentShader(Hideandseek.id("core/ember"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            embers.clear()
            return
        }
        if (client.isPaused) return
        embers.forEach { ember ->
            ember.age += 1f
            val t = ember.age / ember.life
            ember.y += ember.rise * (1.0 - 0.5 * t)
            ember.x += sin(ember.age * 0.25 + ember.phase) * ember.sway
            ember.z += cos(ember.age * 0.21 + ember.phase) * ember.sway
        }
        embers.removeIf { it.age >= it.life }

        val camera = client.gameRenderer.mainCamera().position()
        val random = level.random
        fun emit(at: Vector3d, rate: Float, lift: Double, spread: Double) {
            if (camera.distanceToSqr(at.x, at.y, at.z) > SHOWN_WITHIN * SHOWN_WITHIN) return
            if (random.nextFloat() >= rate || embers.size >= MOST) return
            embers += Ember(
                at.x + (random.nextDouble() - 0.5) * spread,
                at.y + lift,
                at.z + (random.nextDouble() - 0.5) * spread,
                0.05 + random.nextDouble() * 0.05,
                0.008 + random.nextDouble() * 0.012,
                random.nextDouble() * 6.28,
                24f + random.nextFloat() * 30f,
            )
        }
        LampGlow.fires.forEach { emit(it, FIRE_RATE, 0.1, 0.5) }
        Chimneys.tops().forEach { emit(it, CHIMNEY_RATE, 0.05, 0.35) }
    }

    private fun draw() {
        if (embers.isEmpty()) return
        val client = Minecraft.getInstance()
        val camera = client.gameRenderer.mainCamera().position()
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val corners = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))
        ByteBufferBuilder(format.vertexSize * 4 * embers.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (ember in embers) {
                val t = ((ember.age + partial) / ember.life).coerceIn(0f, 1f)
                val glow = (1f - t) * (1f - t) * (t * 8f).coerceAtMost(1f)
                val flicker = 0.75f + 0.25f * sin((ember.age + partial) * 1.7f + ember.phase.toFloat())
                val r = 1f
                val g = 0.55f - 0.35f * t
                val b = 0.12f - 0.1f * t
                val x = (ember.x - camera.x).toFloat()
                val y = (ember.y + ember.rise * partial - camera.y).toFloat()
                val z = (ember.z - camera.z).toFloat()
                val strength = glow * flicker
                for (corner in corners) {
                    builder.addVertex(x, y, z).setUv(corner[0], corner[1]).setColor(r * strength, g * strength, b * strength, 1f)
                }
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa embers" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, vertices, embers.size * 6)
                }
            }
        }
    }

    private fun pass(client: Minecraft, vertices: GpuBuffer, indexCount: Int) {
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f), Vector3f(), Matrix4f().zero().m00(SIZE))
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa embers" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
