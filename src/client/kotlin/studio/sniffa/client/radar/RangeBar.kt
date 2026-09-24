package studio.sniffa.client.radar

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.ui.sound.UiSound
import java.util.OptionalInt

object RangeBar {

    private const val PATIENCE_MILLIS = 900f

    private const val FADE_MILLIS = 500f

    private const val EMPTY_AT = 80f

    private const val WIDTH = 150
    private const val HEIGHT = 9
    private const val TOP = 36

    private const val PAD = 12

    private const val EASING = 9f

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/range"))
        .withVertexShader(Hideandseek.id("core/range"))
        .withFragmentShader(Hideandseek.id("core/range"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build()

    @Volatile
    private var blocks = 0

    @Volatile
    private var live = false

    @Volatile
    private var lastHeard = 0L

    private var quad: ScanQuad? = null

    private var shownFill = 0f
    private var lastFrame = 0L

    fun accept(blocks: Int, live: Boolean) {
        val fresh = lastHeard == 0L || System.currentTimeMillis() - lastHeard > PATIENCE_MILLIS
        if (fresh && live) {
            UiSound.rangeLocked()
        }

        this.blocks = blocks
        this.live = live
        this.lastHeard = System.currentTimeMillis()
    }

    fun forget() {
        lastHeard = 0L
        live = false
    }

    fun install() {
        RenderPipelines.register(PIPELINE)

        WorldRenderEvents.END_MAIN.register(WorldRenderEvents.EndMain { drawBar() })
        HudRenderCallback.EVENT.register { graphics, _ -> drawFigure(graphics) }
    }

    private fun alive(now: Long): Float {
        if (lastHeard == 0L) return 0f
        val silent = (now - lastHeard).toFloat()
        return (1f - (silent - PATIENCE_MILLIS) / FADE_MILLIS).coerceIn(0f, 1f)
    }

    private fun drawBar() {
        val now = System.currentTimeMillis()
        val alive = alive(now)
        if (alive <= 0f) {
            if (lastHeard != 0L && now - lastHeard > PATIENCE_MILLIS + FADE_MILLIS) {
                lastHeard = 0L
            }
            return
        }

        val client = Minecraft.getInstance()
        if (client.options.hideGui) return

        val screen = client.mainRenderTarget
        val colour = screen.colorTextureView ?: return

        val current = quad ?: ScanQuad.build().also { quad = it }

        val width = client.window.guiScaledWidth.toFloat()
        val height = client.window.guiScaledHeight.toFloat()
        if (width <= 0f || height <= 0f) return

        val left = ((width - WIDTH) / 2f - PAD) / width
        val right = ((width + WIDTH) / 2f + PAD) / width
        val top = 1f - (TOP - PAD) / height
        val bottom = 1f - (TOP + HEIGHT + PAD) / height

        val measured = if (live) (1f - blocks / EMPTY_AT).coerceIn(0f, 1f) else 0f

        val sinceFrame = if (lastFrame == 0L) 0f else (now - lastFrame) / 1000f
        lastFrame = now
        shownFill += (measured - shownFill) * (EASING * sinceFrame).coerceIn(0f, 1f)

        val unitsX = (WIDTH + 2 * PAD).toFloat() / HEIGHT
        val unitsY = (HEIGHT + 2 * PAD).toFloat() / HEIGHT
        val padUnits = PAD.toFloat() / HEIGHT

        val parameters = Matrix4f()
            .m00(shownFill).m02(unitsX).m03(unitsY)
            .m10(left).m11(bottom).m12(right).m13(top)
            .m20(padUnits)

        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            Vector4f(1f, 1f, 1f, alive),
            Vector3f(),
            parameters,
        )

        val indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS)

        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa rangefinder" },
            colour,
            OptionalInt.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(current.indexCount), indices.type())
            pass.setVertexBuffer(0, current.vertices)
            pass.drawIndexed(0, 0, current.indexCount, 1)
        }
    }

    private fun drawFigure(graphics: GuiGraphics) {
        val client = Minecraft.getInstance()
        if (client.options.hideGui) return

        val alive = alive(System.currentTimeMillis())
        if (alive <= 0f) return

        val font = client.font
        val text = if (live) "$blocks Blöcke" else "niemand"

        var x = (graphics.guiWidth() - font.width(text)) / 2
        val y = TOP - 12

        val opacity = (255 * alive).toInt().coerceIn(0, 255)
        val last = (text.length - 1).coerceAtLeast(1)

        text.forEachIndexed { index, character ->
            val piece = character.toString()
            val shade = blend(FROM, TO, index.toFloat() / last, opacity)
            graphics.drawString(font, piece, x, y, shade, true)
            x += font.width(piece)
        }
    }

    private fun blend(from: Int, to: Int, amount: Float, opacity: Int): Int {
        val t = amount.coerceIn(0f, 1f)
        var result = opacity shl 24
        for (shift in intArrayOf(16, 8, 0)) {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            result = result or (((a + (b - a) * t).toInt().coerceIn(0, 255)) shl shift)
        }
        return result
    }

    private const val FROM = 0x4EC471
    private const val TO = 0xA9F2C4
}
