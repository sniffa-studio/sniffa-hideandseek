package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.WedgeStyle
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RadialPainter(
    private val slots: Int,
    private val metrics: WheelMetrics,
) {

    val share: Double = 360.0 / slots

    private val half = (share - Spacing.WEDGE_GAP_DEGREES) / 2

    private val intensity = FloatArray(slots)
    private val flashes = FloatArray(slots)
    private var lastFrame = 0L

    private val segments = Radial.segmentsFor(share, metrics.outer + metrics.pop)

    private val edgeInk = Color.ofArgb(Palette.WEDGE_EDGE)

    private val stagger = if (slots > 1) (OPEN_MILLIS - UNFOLD_MILLIS) / (slots - 1) else 0f

    fun centreOf(index: Int): Double = FIRST_CENTRE + index * share

    fun indexAt(dx: Double, dy: Double): Int {
        val offset = Radial.degreesOf(dx, dy) - FIRST_CENTRE
        return Math.floorMod((offset / share).roundToInt(), slots)
    }

    fun burn(index: Int): Float = intensity[index]

    fun flash(index: Int) {
        if (index in flashes.indices) flashes[index] = 1f
    }

    fun startOf(index: Int): Float = index * stagger

    fun unfold(index: Int, openedMillis: Float): Float =
        ((openedMillis - startOf(index)) / UNFOLD_MILLIS).coerceIn(0f, 1f)

    fun reach(unfold: Float): Float {
        val u = unfold - 1f
        return 1f + (OVERSHOOT + 1f) * u * u * u + OVERSHOOT * u * u
    }

    fun outerOf(index: Int, unfold: Float): Double =
        metrics.inner + metrics.depth * reach(unfold) + metrics.pop * intensity[index]

    fun advance(active: Int?) {
        val now = System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0.0 else (now - lastFrame) / NANOS_PER_SECOND
        lastFrame = now

        val step = (1.0 - Math.exp(-elapsed / Spacing.GLOW_EASE_SECONDS)).toFloat()
        val fade = (1.0 - Math.exp(-elapsed / FLASH_EASE_SECONDS)).toFloat()

        for (index in intensity.indices) {
            val target = if (index == active) 1f else 0f
            intensity[index] += (target - intensity[index]) * step
            flashes[index] -= flashes[index] * fade
        }
    }

    fun drawWedge(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        style: WedgeStyle,
        unfold: Float,
        seconds: Double,
    ) {
        val centre = centreOf(index)
        val from = centre - half
        val to = centre + half
        val burn = intensity[index]
        val flash = flashes[index] * FLASH_STRENGTH
        val outer = outerOf(index, unfold)
        val inner = metrics.inner
        val alpha = unfold

        val innerInk = Radial.blend(Radial.blend(style.innerIdle, style.innerHot, burn), FLASH, flash)
        val outerInk = Radial.blend(Radial.blend(style.outerIdle, style.outerHot, burn), FLASH, flash)

        graphics.drawRing(
            centerX, centerY, from, to, segments, inner, outer,
            Color.ofArgb(Radial.fade(innerInk, alpha)), Color.ofArgb(Radial.fade(outerInk, alpha)),
        )

        val band = (outer - inner) * FROST_BAND
        graphics.drawRing(
            centerX, centerY, from, to, segments, outer - band, outer,
            Radial.CLEAR, Color.ofArgb(Radial.fade(style.frost, alpha)),
        )

        frost(graphics, centerX, centerY, index, from, outer, style, alpha)
        sparkles(graphics, centerX, centerY, index, from, outer, alpha, seconds)
        contour(graphics, centerX, centerY, from, to, outer, alpha)

        val halo = Radial.blend(Radial.blend(0, style.halo, burn), FLASH_HALO, flash)
        if ((halo ushr 24) > 2) {
            graphics.drawRing(
                centerX, centerY, from, to, segments, outer, outer + metrics.pop * HALO_REACH,
                Color.ofArgb(Radial.fade(halo, alpha)), Radial.CLEAR,
            )
        }
    }

    private fun frost(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        from: Double,
        outer: Double,
        style: WedgeStyle,
        alpha: Float,
    ) {
        val inner = metrics.inner + EDGE_MARGIN
        val span = outer - EDGE_MARGIN - inner
        if (span <= 0) return
        for (speck in 0 until FROST_SPECKS) {
            val seed = index * 97 + speck * 13
            val along = 0.08 + 0.84 * Radial.hash(seed)
            val out = Radial.hash(seed + 5).let { it * it }
            val radius = outer - EDGE_MARGIN - span * out
            val degrees = from + along * half * 2
            val x = Radial.x(centerX, degrees, radius).toInt()
            val y = Radial.y(centerY, degrees, radius).toInt()
            val strength = 0.35f + 0.65f * Radial.hash(seed + 9)
            graphics.fill(x, y, x + 1, y + 1, Radial.fade(style.frost, alpha * strength))
        }
    }

    private fun sparkles(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        from: Double,
        outer: Double,
        alpha: Float,
        seconds: Double,
    ) {
        for (spark in 0 until SPARKLES) {
            val seed = index * 31 + spark * 7 + 3
            val period = SPARKLE_PERIOD * (0.8 + 0.5 * Radial.hash(seed + 1))
            val phase = ((seconds / period + Radial.hash(seed + 2)) % 1.0).toFloat()
            if (phase > SPARKLE_SHARE) continue
            val shine = sin(PI * phase / SPARKLE_SHARE).toFloat() * alpha
            val along = 0.15 + 0.7 * Radial.hash(seed + 3)
            val radius = metrics.inner + (outer - metrics.inner) * (0.55 + 0.35 * Radial.hash(seed + 4))
            val degrees = from + along * half * 2
            val x = Radial.x(centerX, degrees, radius).toInt()
            val y = Radial.y(centerY, degrees, radius).toInt()
            graphics.fill(x, y, x + 1, y + 1, Radial.opaque(SPARKLE_CORE, shine * 0.85f))
            if (shine > 0.6f) {
                val arm = Radial.opaque(SPARKLE_ARM, (shine - 0.6f) * 1.1f)
                graphics.fill(x - 1, y, x, y + 1, arm)
                graphics.fill(x + 1, y, x + 2, y + 1, arm)
                graphics.fill(x, y - 1, x + 1, y, arm)
                graphics.fill(x, y + 1, x + 1, y + 2, arm)
            }
        }
    }

    private fun contour(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        from: Double,
        to: Double,
        outer: Double,
        alpha: Float,
    ) {
        val ink = Color.ofArgb(Radial.fade(Palette.WEDGE_EDGE, alpha))
        graphics.drawRing(centerX, centerY, from, to, segments, outer - metrics.rim, outer, ink, ink)

        val rim = Color.ofArgb(Radial.fade(RIM_LIGHT, alpha))
        graphics.drawRing(centerX, centerY, from, to, segments, outer - metrics.rim * 2, outer - metrics.rim, rim, rim)

        val span = Math.toDegrees(metrics.rim / ((metrics.inner + outer) / 2.0))
        graphics.drawRing(centerX, centerY, from, from + span, 1, metrics.inner, outer, ink, ink)
        graphics.drawRing(centerX, centerY, to - span, to, 1, metrics.inner, outer, ink, ink)
    }

    companion object {
        const val OPEN_MILLIS = 250f

        const val UNFOLD_MILLIS = 110f

        private const val FIRST_CENTRE = 90.0

        private const val OVERSHOOT = 1.2f

        private const val NANOS_PER_SECOND = 1_000_000_000.0

        private const val FLASH_EASE_SECONDS = 0.18

        private const val FLASH_STRENGTH = 0.55f

        private const val FLASH = 0xFFFFFFFF.toInt()

        private const val FLASH_HALO = 0x99FFFFFF.toInt()

        private const val HALO_REACH = 1.6

        private const val FROST_BAND = 0.28

        private const val FROST_SPECKS = 16

        private const val EDGE_MARGIN = 2.5

        private const val SPARKLES = 2

        private const val SPARKLE_PERIOD = 2.8

        private const val SPARKLE_SHARE = 0.16f

        private const val SPARKLE_CORE = 0xFFFFFF

        private const val SPARKLE_ARM = 0xB8E4FA

        private const val RIM_LIGHT = 0x30DDF2FF
    }
}
