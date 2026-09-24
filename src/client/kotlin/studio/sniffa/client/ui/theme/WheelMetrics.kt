package studio.sniffa.client.ui.theme

import kotlin.math.roundToInt

class WheelMetrics private constructor(val size: Int) {

    val outer: Double = size * OUTER_SHARE

    val inner: Double = size * INNER_SHARE

    val iconRadius: Int = (size * ICON_SHARE).roundToInt()

    val hubSize: Int = (size * HUB_SHARE).roundToInt()

    val iconSize: Int = (size * ICON_SIZE_SHARE).roundToInt().coerceAtLeast(NATIVE_ICON)

    val haloWidth: Double = size * HALO_SHARE

    val rim: Double = (size * RIM_SHARE).coerceAtLeast(MINIMUM_RIM)

    val deadZone: Double get() = inner

    companion object {

        private const val OUTER_SHARE = 112.0 / 280
        private const val INNER_SHARE = 52.0 / 280
        private const val ICON_SHARE = 82.0 / 280
        private const val HALO_SHARE = 14.0 / 280

        private const val HUB_SHARE = 56.0 / 280

        private const val ICON_SIZE_SHARE = 32.0 / 280

        private const val NATIVE_ICON = 16

        private const val RIM_SHARE = 1.5 / 280
        private const val MINIMUM_RIM = 1.0

        private const val MINIMUM = 150

        private const val MAXIMUM = 320

        private const val MARGIN = 10

        fun forViewport(width: Int, height: Int, reservedBelow: Int): WheelMetrics {
            val vertical = height - reservedBelow - 2 * MARGIN
            val horizontal = width - 2 * MARGIN
            val size = minOf(vertical, horizontal).coerceIn(MINIMUM, MAXIMUM)

            val result = WheelMetrics(size)
            check(result.outer + result.haloWidth <= size / 2.0) {
                "wheel proportions do not fit their own box - check the shares in WheelMetrics"
            }
            return result
        }
    }
}
