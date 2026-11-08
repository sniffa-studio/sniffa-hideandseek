package studio.sniffa.client.radar

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import java.util.Optional
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.ui.sound.UiSound

object HotCold {

    private const val PATIENCE_MILLIS = 1600f

    private const val EASING = 6.0f

    private const val PULSE_SLOW = 2f

    private const val PULSE_FAST = 12f

    private const val TAU = 6.2831855f

    private const val FADE_MILLIS = 700f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/hotcold"))
        .withVertexShader(Hideandseek.id("core/hotcold"))
        .withFragmentShader(Hideandseek.id("core/hotcold"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("FrostSampler").build())
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()

    @Volatile
    private var target = 0f

    @Volatile
    private var lastHeard = 0L

    private var shown = 0f
    private var lastFrame = 0L
    private var pulse = 0f
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

        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain {
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
        pulse = (pulse + sinceFrame * (PULSE_SLOW + (PULSE_FAST - PULSE_SLOW) * shown)) % TAU

        blip(now)

        val client = Minecraft.getInstance()
        if (client.gui.hud.isHidden) return

        val screen = client.gameRenderer.mainRenderTarget()
        val colour = screen.colorTextureView ?: return

        val current = quad ?: ScanQuad.build().also { quad = it }

        val seconds = (now - startedAt) / 1000f

        val parameters = Matrix4f().m00(shown).m01(seconds).m02(pulse)

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            Vector4f(1f, 1f, 1f, alive),
            Vector3f(),
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)

        val frost = client.textureManager.getTexture(FROST)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa hot cold" },
            colour,
            Optional.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.bindTexture("FrostSampler", frost.textureView, frost.sampler)
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices.slice())
            pass.drawIndexed(current.indexCount, 1, 0, 0, 0)
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
