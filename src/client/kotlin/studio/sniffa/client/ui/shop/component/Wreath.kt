package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class Wreath(private val metrics: WheelMetrics) {

    private val middle = (metrics.wreathInner + metrics.wreathOuter) / 2

    private val sprig = metrics.wreathWidth + SPRIG_OVERHANG

    private val sprigs = ceil(2 * PI * middle / (sprig * SPRIG_SPACING)).toInt()

    private val bulbs = max(MINIMUM_BULBS, (2 * PI * middle / (metrics.size * BULB_SPACING_SHARE)).roundToInt())

    private val segments = Radial.segmentsFor(360.0, metrics.wreathOuter)

    fun draw(graphics: OwoUIGraphics, centerX: Int, centerY: Int, openedMillis: Float, seconds: Double) {
        val alpha = (openedMillis / FADE_MILLIS).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        graphics.drawRing(
            centerX, centerY, 0.0, 360.0, segments,
            metrics.wreathInner + 1, metrics.wreathOuter - 1,
            Color.ofArgb(Radial.fade(Palette.WREATH_BASE, alpha)), Color.ofArgb(Radial.fade(Palette.WREATH_BASE, alpha)),
        )

        for (index in 0 until sprigs) {
            val degrees = index * 360.0 / sprigs + Radial.hash(index * 5) * 3
            val radius = middle + (Radial.hash(index * 11) - 0.5) * SPRIG_JITTER
            val x = (Radial.x(centerX, degrees, radius) - sprig / 2.0).roundToInt()
            val y = (Radial.y(centerY, degrees, radius) - sprig / 2.0).roundToInt()
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Textures.SPRIGS[index % Textures.SPRIGS.size], x, y, sprig, sprig, alpha)
        }

        for (index in 0 until bulbs) {
            val degrees = (index + 0.5) * 360.0 / bulbs
            val colour = Palette.BULBS[index % Palette.BULBS.size]
            val period = TWINKLE_SECONDS * (0.75 + 0.6 * Radial.hash(index * 3 + 1))
            val wave = 0.5 + 0.5 * sin(2 * PI * (seconds / period + Radial.hash(index * 3 + 2)))
            val shine = (DIM + (1 - DIM) * wave * wave).toFloat()
            val x = Radial.x(centerX, degrees, middle).roundToInt()
            val y = Radial.y(centerY, degrees, middle).roundToInt()

            graphics.drawRing(
                x, y + 1, 0.0, 360.0, GLOW_SEGMENTS, 0.0, GLOW_RADIUS,
                Color.ofArgb(Radial.opaque(colour, GLOW_ALPHA * shine * shine * alpha)), Radial.CLEAR,
            )

            val left = x - Textures.BULB_WIDTH / 2
            val top = y - Textures.BULB_HEIGHT / 2
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, Textures.BULB_GLASS, left, top, Textures.BULB_WIDTH, Textures.BULB_HEIGHT,
                Radial.opaque(scale(colour, shine), alpha),
            )
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, Textures.BULB_CAP, left, top, Textures.BULB_WIDTH, Textures.BULB_HEIGHT, alpha,
            )
        }
    }

    private fun scale(rgb: Int, amount: Float): Int {
        val r = (((rgb shr 16) and 0xFF) * amount).toInt()
        val g = (((rgb shr 8) and 0xFF) * amount).toInt()
        val b = ((rgb and 0xFF) * amount).toInt()
        return (r shl 16) or (g shl 8) or b
    }

    private companion object {
        const val FADE_MILLIS = 250f

        const val SPRIG_OVERHANG = 3

        const val SPRIG_SPACING = 0.5

        const val SPRIG_JITTER = 1.5

        const val BULB_SPACING_SHARE = 0.1

        const val MINIMUM_BULBS = 16

        const val TWINKLE_SECONDS = 2.6

        const val DIM = 0.55

        const val GLOW_RADIUS = 5.0

        const val GLOW_SEGMENTS = 12

        const val GLOW_ALPHA = 0.28f
    }
}
