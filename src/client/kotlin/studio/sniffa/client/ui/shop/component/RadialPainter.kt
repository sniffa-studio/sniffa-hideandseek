package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.WedgeStyle
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.acos
import kotlin.math.ceil

class RadialPainter(
    private val slots: Int,
    private val metrics: WheelMetrics,
    private val firstCentreDegrees: Double,
    private val gapDegrees: Double,
) {

    private val intensity = FloatArray(slots)
    private var lastFrame = 0L

    private val wedgeFrom = DoubleArray(slots)
    private val wedgeTo = DoubleArray(slots)

    private val wedgeSegments: Int

    private val edgeInk = Color.ofArgb(Palette.WEDGE_EDGE)
    private val trackInk = Color.ofArgb(Palette.WHEEL_TRACK)
    private val shadeInk = Color.ofArgb(Palette.COOLDOWN_SHADE)

    private val innerEdge = metrics.inner

    init {
        val share = 360.0 / slots
        for (index in 0 until slots) {
            val from = firstCentreDegrees + index * share - share / 2 + gapDegrees / 2
            wedgeFrom[index] = from
            wedgeTo[index] = from + share - gapDegrees
        }

        wedgeSegments = segmentsFor(share - gapDegrees, metrics.outer + metrics.haloWidth)
    }

    fun advance(active: Int?) {
        val now = System.nanoTime()
        val elapsed = if (lastFrame == 0L) 0.0 else (now - lastFrame) / NANOS_PER_SECOND
        lastFrame = now

        val step = (1.0 - Math.exp(-elapsed / Spacing.GLOW_EASE_SECONDS)).toFloat()

        for (index in intensity.indices) {
            val target = if (index == active) 1f else 0f
            intensity[index] += (target - intensity[index]) * step
        }
    }

    fun drawWedge(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        style: WedgeStyle,
        cooling: Float,
    ) {
        val from = wedgeFrom[index]
        val to = wedgeTo[index]
        val burn = intensity[index]

        graphics.drawRing(
            centerX, centerY,
            from, to,
            wedgeSegments,
            innerEdge, metrics.outer,
            trackInk, trackInk,
        )

        graphics.drawRing(
            centerX, centerY,
            from, to,
            wedgeSegments,
            innerEdge, metrics.outer,
            Color.ofArgb(blend(style.innerIdle, style.innerHot, burn)),
            Color.ofArgb(blend(style.outerIdle, style.outerHot, burn)),
        )

        drawCooldown(graphics, centerX, centerY, from, to, cooling)

        drawContour(graphics, centerX, centerY, from, to)

        if (burn > INVISIBLE) {
            graphics.drawRing(
                centerX, centerY,
                from, to,
                wedgeSegments,
                metrics.outer, metrics.outer + metrics.haloWidth,
                Color.ofArgb(blend(TRANSPARENT, style.halo, burn)), TRANSPARENT_INK,
            )
        }
    }

    private fun drawCooldown(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        from: Double,
        to: Double,
        cooling: Float,
    ) {
        if (cooling <= INVISIBLE) return

        val covered = (to - from) * cooling.coerceIn(0f, 1f).toDouble()
        graphics.drawRing(
            centerX, centerY,
            to - covered, to,
            segmentsFor(covered, metrics.outer),
            innerEdge, metrics.outer,
            shadeInk, shadeInk,
        )
    }

    private fun drawContour(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        from: Double,
        to: Double,
    ) {
        graphics.drawRing(
            centerX, centerY,
            from, to,
            wedgeSegments,
            metrics.outer - metrics.rim, metrics.outer,
            edgeInk, edgeInk,
        )

        val span = Math.toDegrees(metrics.rim / ((metrics.inner + metrics.outer) / 2.0))

        graphics.drawRing(
            centerX, centerY, from, from + span, EDGE_SEGMENTS,
            innerEdge, metrics.outer, edgeInk, edgeInk,
        )
        graphics.drawRing(
            centerX, centerY, to - span, to, EDGE_SEGMENTS,
            innerEdge, metrics.outer, edgeInk, edgeInk,
        )
    }

    private fun segmentsFor(spanDegrees: Double, radius: Double): Int {
        val widest = 2 * acos((1.0 - FLATNESS / radius).coerceIn(-1.0, 1.0))
        return ceil(Math.toRadians(spanDegrees) / widest).toInt().coerceAtLeast(1)
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return (channel(from, to, t, 24) shl 24) or
            (channel(from, to, t, 16) shl 16) or
            (channel(from, to, t, 8) shl 8) or
            channel(from, to, t, 0)
    }

    private fun channel(from: Int, to: Int, t: Float, shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }

    private companion object {
        const val INVISIBLE = 0.01f

        const val TRANSPARENT = 0x00000000
        val TRANSPARENT_INK: Color = Color.ofArgb(TRANSPARENT)

        const val FLATNESS = 0.12

        const val EDGE_SEGMENTS = 1

        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
