package studio.sniffa.client.radar

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.ui.sound.UiSound
import java.util.OptionalInt

object HotCold {

    private const val PATIENCE_MILLIS = 1600f

    private const val EASING = 6.0f

    private const val FADE_MILLIS = 700f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/hotcold"))
        .withVertexShader(Hideandseek.id("core/hotcold"))
        .withFragmentShader(Hideandseek.id("core/hotcold"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withSampler("FrostSampler")
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build()

    @Volatile
    private var target = 0f

    @Volatile
    private var lastHeard = 0L

    private var shown = 0f
    private var lastFrame = 0L
    private var startedAt = 0L
    private var nextBlip = 0L

    private var quad: ScanQuad? = null

    private val FROST: Identifier = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png")

    fun accept(reading: Float, live: Boolean) {
        if (startedAt == 0L || System.currentTimeMillis() - lastHeard > PATIENCE_MILLIS) {
            startedAt = System.currentTimeMillis()
        }
        target = if (live) reading.coerceIn(0f, 1f) else 0f
        lastHeard = System.currentTimeMillis()
    }

    fun forget() {
        target = 0f
        shown = 0f
        lastHeard = 0L
        startedAt = 0L
    }

    fun install() {
        RenderPipelines.register(PIPELINE)

        WorldRenderEvents.END_MAIN.register(WorldRenderEvents.EndMain {
            draw()
        })
    }

    private fun draw() {
        if (lastHeard == 0L) return

        val now = System.currentTimeMillis()
        val silent = (now - lastHeard).toFloat()

        val alive = (1f - (silent - PATIENCE_MILLIS) / FADE_MILLIS).coerceIn(0f, 1f)
        if (alive <= 0f) {
            lastHeard = 0L
            startedAt = 0L
            shown = 0f
            return
        }

        val sinceFrame = if (lastFrame == 0L) 0f else (now - lastFrame) / 1000f
        lastFrame = now
        shown += (target - shown) * (EASING * sinceFrame).coerceIn(0f, 1f)

        blip(now)

        val client = Minecraft.getInstance()
        if (client.options.hideGui) return

        val screen = client.mainRenderTarget
        val colour = screen.colorTextureView ?: return

        val current = quad ?: ScanQuad.build().also { quad = it }

        val seconds = (now - startedAt) / 1000f

        val parameters = Matrix4f().m00(shown).m01(seconds)

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            Vector4f(1f, 1f, 1f, alive),
            Vector3f(),
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)

        val frost = client.textureManager.getTexture(FROST)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa hot cold" },
            colour,
            OptionalInt.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.bindTexture("FrostSampler", frost.textureView, frost.sampler)
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices)
            pass.drawIndexed(0, 0, current.indexCount, 1)
        }
    }

    private fun blip(now: Long) {
        if (nextBlip == 0L) {
            nextBlip = now
        }
        if (now < nextBlip) return

        val heat = shown.coerceIn(0f, 1f)
        nextBlip = now + (1100f - 950f * heat).toLong()

        UiSound.hotColdBlip(heat)
    }
}
