package studio.sniffa.client.ui.theme

import kotlin.math.roundToInt

class WheelMetrics private constructor(val size: Int) {

    val outer: Double = size / 2.0 - 1

    val inner: Double = size * INNER_SHARE

    val depth: Double = outer - inner

    val iconSize: Int = if (depth >= LARGE_ICON_DEPTH) NATIVE_ICON * 2 else NATIVE_ICON

    val textScale: Float = if (size >= LARGE_TEXT) 1f else SMALL_TEXT_SCALE

    val labelHeight: Double = 8.0 * textScale

    val slotRadius: Double = (inner + outer) / 2

    val iconLift: Double = (labelHeight + LABEL_GAP) / 2

    val labelDrop: Double = iconSize / 2.0 + LABEL_GAP + labelHeight / 2 - iconLift

    val corner: Double = CORNER

    val gap: Double = GAP

    val accent: Double = ACCENT_WIDTH

    val cooldownWidth: Double = COOLDOWN_WIDTH

    val hubRadius: Double = inner - HUB_GAP

    val hubImage: Int = (size * HUB_IMAGE_SHARE).roundToInt()

    val deadZone: Double get() = inner

    companion object {

        private const val INNER_SHARE = 0.27

        private const val NATIVE_ICON = 16
        private const val LARGE_ICON_DEPTH = 52.0

        private const val LARGE_TEXT = 260
        private const val SMALL_TEXT_SCALE = 0.75f

        private const val LABEL_GAP = 2.0

        private const val CORNER = 3.0
        private const val GAP = 2.0
        private const val ACCENT_WIDTH = 2.0
        private const val COOLDOWN_WIDTH = 2.0

        private const val HUB_GAP = 4.0
        private const val HUB_IMAGE_SHARE = 0.2

        private const val MINIMUM = 170

        private const val MAXIMUM = 320

        private const val MARGIN = 10

        fun forViewport(width: Int, height: Int, reservedBelow: Int): WheelMetrics {
            val vertical = height - reservedBelow - 2 * MARGIN
            val horizontal = width - 2 * MARGIN
            val size = minOf(vertical, horizontal).coerceIn(MINIMUM, MAXIMUM)

            val result = WheelMetrics(size)
            check(result.outer > result.inner + 2 * CORNER) {
                "wheel proportions do not fit their own box - check the shares in WheelMetrics"
            }
            return result
        }
    }
}
