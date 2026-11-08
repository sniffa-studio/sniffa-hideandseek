package studio.sniffa.client.radar

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import java.util.Optional
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.exp

object CompassRose {

    private const val PATIENCE_MILLIS = 900f

    private const val FADE_MILLIS = 600f

    private const val RISE_MILLIS = 320f

    private const val SWING_SECONDS = 0.14

    private const val LIGHT_SECONDS = 0.12

    private const val PLATE_BLOCKS = 4f

    private const val LIFT = 0.02

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/compass"))
        .withVertexShader(Hideandseek.id("core/compass"))
        .withFragmentShader(Hideandseek.id("core/compass"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.ADDITIVE))
        .withCull(false)
        .build()

    @Volatile
    private var live = false

    @Volatile
    private var bearing = 0.0

    @Volatile
    private var lastReadingAt = 0L

    @Volatile
    private var startedAt = 0L

    private var needle = 0.0
    private var lit = 0.0

    private var lastFrame = 0L

    private var plate: GpuBuffer? = null

    fun accept(degrees: Float, live: Boolean) {
        val first = lastReadingAt == 0L
        bearing = Math.toRadians(degrees.toDouble())
        this.live = live
        lastReadingAt = System.currentTimeMillis()

        if (first) {
            startedAt = lastReadingAt
            needle = bearing
        }
    }

    fun forget() {
        lastReadingAt = 0L
        startedAt = 0L
        live = false
    }

    fun install() {
        RenderPipelines.register(PIPELINE)

        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain {
            draw()
        })
    }

    private fun draw() {
        if (lastReadingAt == 0L) return

        val now = System.currentTimeMillis()
        val since = (now - lastReadingAt).toFloat()
        if (since > PATIENCE_MILLIS + FADE_MILLIS) {
            forget()
            return
        }

        val rising = ((now - startedAt) / RISE_MILLIS).coerceIn(0f, 1f)
        val falling = ((PATIENCE_MILLIS + FADE_MILLIS - since) / FADE_MILLIS).coerceIn(0f, 1f)
        val strength = rising * falling
        if (strength <= 0f) return

        val client = Minecraft.getInstance()
        val player = client.player ?: return

        advanceNeedle()

        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(true)
        val eye = player.getEyePosition(partial)
        val camera = client.gameRenderer.mainCamera().position()

        val offset = Vector3f(
            (eye.x - camera.x).toFloat(),
            (eye.y - player.eyeHeight - camera.y + LIFT).toFloat(),
            (eye.z - camera.z).toFloat(),
        )

        val target = client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return

        val current = plate ?: build().also { plate = it }

        val parameters = Matrix4f()
            .m00((now - startedAt) / 1000f)
            .m01(needle.toFloat())
            .m02(lit.toFloat())

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(),
            Vector4f(
                ARGB.red(Palette.BRAND) / 255f,
                ARGB.green(Palette.BRAND) / 255f,
                ARGB.blue(Palette.BRAND) / 255f,
                strength,
            ),
            offset,
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa compass" },
            colour,
            Optional.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(INDEX_COUNT), indices.type())
            pass.setVertexBuffer(0, current.slice())
            pass.drawIndexed(INDEX_COUNT, 1, 0, 0, 0)
        }
    }

    private fun advanceNeedle() {
        val now = System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0.0 else (now - lastFrame) / NANOS_PER_SECOND
        lastFrame = now

        val step = 1.0 - exp(-elapsed / SWING_SECONDS)
        needle += wrapToPi(bearing - needle) * step

        val fade = 1.0 - exp(-elapsed / LIGHT_SECONDS)
        lit += ((if (live) 1.0 else 0.0) - lit) * fade
    }

    private fun wrapToPi(angle: Double): Double {
        var result = angle
        while (result > PI) result -= TAU
        while (result < -PI) result += TAU
        return result
    }

    private fun build(): GpuBuffer {
        val format = DefaultVertexFormat.POSITION_TEX

        ByteBufferBuilder(format.vertexSize * 4).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)

            builder.addVertex(-PLATE_BLOCKS, 0f, -PLATE_BLOCKS).setUv(-PLATE_BLOCKS, -PLATE_BLOCKS)
            builder.addVertex(-PLATE_BLOCKS, 0f, PLATE_BLOCKS).setUv(-PLATE_BLOCKS, PLATE_BLOCKS)
            builder.addVertex(PLATE_BLOCKS, 0f, PLATE_BLOCKS).setUv(PLATE_BLOCKS, PLATE_BLOCKS)
            builder.addVertex(PLATE_BLOCKS, 0f, -PLATE_BLOCKS).setUv(PLATE_BLOCKS, -PLATE_BLOCKS)

            builder.buildOrThrow().use {
                return RenderSystem.getDevice().createBuffer(
                    { "sniffa compass plate" },
                    GpuBuffer.USAGE_VERTEX,
                    it.vertexBuffer(),
                )
            }
        }
    }

    private const val INDEX_COUNT = 6

    private const val TAU = 2 * PI
    private const val NANOS_PER_SECOND = 1_000_000_000.0
}
