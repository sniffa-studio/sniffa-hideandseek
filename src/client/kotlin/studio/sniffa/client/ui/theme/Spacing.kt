package studio.sniffa.client.ui.theme

object Spacing {

    const val SLOT_SIZE = 22

    const val SLOTS_PER_PAGE = 8

    const val WEDGE_GAP_DEGREES = 2.0

    const val LOCKUP_HEIGHT = 12

    const val MARK_WIDTH = (
        LOCKUP_HEIGHT * Textures.MARK_SOURCE_WIDTH + Textures.MARK_SOURCE_HEIGHT / 2
        ) / Textures.MARK_SOURCE_HEIGHT

    const val WORDMARK_WIDTH = (
        LOCKUP_HEIGHT * Textures.WORDMARK_SOURCE_WIDTH + Textures.WORDMARK_SOURCE_HEIGHT / 2
        ) / Textures.WORDMARK_SOURCE_HEIGHT

    const val MARK_GAP = 4

    const val DETAIL_WIDTH = 240

    const val DETAIL_HEIGHT = 34

    const val LINE_GAP = 3

    const val GLOW_EASE_SECONDS = 0.07

    const val SECTION_GAP = 10

    const val DOT_SIZE = 5
    const val DOT_GAP = 5

    const val BELOW_WHEEL =
        SECTION_GAP + DOT_SIZE + SECTION_GAP + DETAIL_HEIGHT + SECTION_GAP + LOCKUP_HEIGHT
}
