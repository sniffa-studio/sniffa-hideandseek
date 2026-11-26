package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.OwoUIGraphics
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RadialPainter(
    private val slots: Int,
    private val metrics: WheelMetrics,
) {

    val share: Double = 360.0 / slots

    private val intensity = FloatArray(slots)
    private val flashes = FloatArray(slots)
    private var lastFrame = 0L

    private val bands: List<Pair<Double, Double>> = bandsBetween(metrics.inner, metrics.outer)

    fun centreOf(index: Int): Double = FIRST_CENTRE + index * share

    fun indexAt(dx: Double, dy: Double): Int {
        val offset = Radial.degreesOf(dx, dy) - FIRST_CENTRE
        return Math.floorMod((offset / share).roundToInt(), slots)
    }

    fun burn(index: Int): Float = intensity[index]

    fun flash(index: Int) {
        if (index in flashes.indices) flashes[index] = 1f
    }

    fun advance(active: Int?) {
        val now = System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0.0 else (now - lastFrame) / NANOS_PER_SECOND
        lastFrame = now

        val step = (1.0 - Math.exp(-elapsed / HOVER_EASE_SECONDS)).toFloat()
        val fade = (1.0 - Math.exp(-elapsed / FLASH_EASE_SECONDS)).toFloat()

        for (index in intensity.indices) {
            val target = if (index == active) 1f else 0f
            intensity[index] += (target - intensity[index]) * step
            flashes[index] -= flashes[index] * fade
        }
    }

    fun drawWedge(graphics: OwoUIGraphics, centerX: Int, centerY: Int, index: Int, alpha: Float) {
        val burn = intensity[index]
        val fill = Radial.blend(Radial.blend(Palette.WEDGE, Palette.WEDGE_HOVER, burn), Palette.ACCENT, flashes[index] * FLASH_STRENGTH)
        for ((from, to) in bands) paint(graphics, centerX, centerY, index, from, to, fill, alpha)

        if (burn > VISIBLE) {
            for ((from, to) in bandsBetween(metrics.outer - metrics.accent, metrics.outer)) {
                paint(graphics, centerX, centerY, index, from, to, Palette.ACCENT, alpha * burn)
            }
        }
    }

    fun drawCooldown(graphics: OwoUIGraphics, centerX: Int, centerY: Int, index: Int, progress: Float, alpha: Float) {
        val inner = metrics.inner
        val outer = inner + metrics.cooldownWidth
        for ((from, to) in bandsBetween(inner, outer)) {
            paint(graphics, centerX, centerY, index, from, to, Palette.COOLDOWN_TRACK, alpha)
            paint(graphics, centerX, centerY, index, from, to, Palette.COOLDOWN_FILL, alpha, progress.toDouble())
        }
    }

    private fun paint(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        from: Double,
        to: Double,
        colour: Int,
        alpha: Float,
        sweep: Double = 1.0,
    ) {
        if (sweep <= 0.0) return
        val radius = (from + to) / 2
        val inset = Math.toDegrees((metrics.gap / 2 + cornerInset(radius)) / radius)
        val start = centreOf(index) - share / 2 + inset
        val end = centreOf(index) + share / 2 - inset
        if (end <= start) return
        val stop = start + (end - start) * sweep.coerceAtMost(1.0)
        graphics.drawRing(
            centerX, centerY, start, stop, Radial.segmentsFor(stop - start, to), from, to,
            Radial.ink(colour, alpha), Radial.ink(colour, alpha),
        )
    }

    private fun cornerInset(radius: Double): Double {
        val corner = metrics.corner
        val distance = minOf(radius - metrics.inner, metrics.outer - radius)
        if (distance >= corner) return 0.0
        val d = corner - distance.coerceAtLeast(0.0)
        return corner - sqrt((corner * corner - d * d).coerceAtLeast(0.0))
    }

    private fun bandsBetween(from: Double, to: Double): List<Pair<Double, Double>> {
        val edges = mutableListOf(from)
        var r = from
        while (r < to - 1e-6) {
            val nearEdge = r < metrics.inner + metrics.corner || r > metrics.outer - metrics.corner - FINE_STEP
            r = minOf(to, r + if (nearEdge) FINE_STEP else COARSE_STEP)
            edges += r
        }
        return edges.zipWithNext()
    }

    private companion object {
        const val FIRST_CENTRE = 90.0

        const val NANOS_PER_SECOND = 1_000_000_000.0

        const val HOVER_EASE_SECONDS = 0.06

        const val FLASH_EASE_SECONDS = 0.18

        const val FLASH_STRENGTH = 0.35f

        const val VISIBLE = 0.02f

        const val FINE_STEP = 0.5

        const val COARSE_STEP = 6.0
    }
}
