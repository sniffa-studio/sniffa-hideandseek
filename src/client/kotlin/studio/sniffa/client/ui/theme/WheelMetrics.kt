package studio.sniffa.client.ui.theme

import kotlin.math.max
import kotlin.math.roundToInt

class WheelMetrics private constructor(val size: Int) {

    val wreathOuter: Double = size / 2.0 - 1

    val wreathWidth: Int = max(MINIMUM_WREATH, (size * WREATH_SHARE).roundToInt())

    val wreathInner: Double = wreathOuter - wreathWidth

    val pop: Double = max(MINIMUM_POP, size * POP_SHARE)

    val outer: Double = wreathInner - WREATH_GAP - pop

    val inner: Double = size * INNER_SHARE

    val depth: Double = outer - inner

    val iconSize: Int = if (depth >= LARGE_ICON_DEPTH) NATIVE_ICON * 2 else NATIVE_ICON

    val badgeInset: Double = if (iconSize > NATIVE_ICON) 7.0 else 5.5

    val iconRadius: Double = (inner + outer - BADGE_ROOM) / 2

    val badgeScale: Float = if (size >= LARGE_TEXT) 1f else SMALL_TEXT_SCALE

    val textScale: Float = if (size >= LARGE_TEXT) 1f else SMALL_TEXT_SCALE

    val hubRadius: Double = inner - HUB_GAP

    val hubImage: Int = (size * HUB_IMAGE_SHARE).roundToInt()

    val rim: Double = max(MINIMUM_RIM, size * RIM_SHARE)

    val deadZone: Double get() = inner

    companion object {

        private const val WREATH_SHARE = 0.042
        private const val MINIMUM_WREATH = 9
        private const val WREATH_GAP = 3.0

        private const val POP_SHARE = 0.012
        private const val MINIMUM_POP = 2.0

        private const val INNER_SHARE = 0.285

        private const val BADGE_ROOM = 9.0

        private const val NATIVE_ICON = 16
        private const val LARGE_ICON_DEPTH = 42.0

        private const val LARGE_TEXT = 290
        private const val SMALL_TEXT_SCALE = 0.75f

        private const val HUB_GAP = 4.0
        private const val HUB_IMAGE_SHARE = 0.2

        private const val RIM_SHARE = 1.5 / 280
        private const val MINIMUM_RIM = 1.0

        private const val MINIMUM = 170

        private const val MAXIMUM = 320

        private const val MARGIN = 10

        fun forViewport(width: Int, height: Int, reservedBelow: Int): WheelMetrics {
            val vertical = height - reservedBelow - 2 * MARGIN
            val horizontal = width - 2 * MARGIN
            val size = minOf(vertical, horizontal).coerceIn(MINIMUM, MAXIMUM)

            val result = WheelMetrics(size)
            check(result.outer > result.inner && result.wreathOuter <= size / 2.0) {
                "wheel proportions do not fit their own box - check the shares in WheelMetrics"
            }
            return result
        }
    }
}
