package studio.sniffa.client.ui.theme

object Spacing {

    const val LOCKUP_HEIGHT = 12

    const val MARK_WIDTH = (
        LOCKUP_HEIGHT * Textures.MARK_SOURCE_WIDTH + Textures.MARK_SOURCE_HEIGHT / 2
        ) / Textures.MARK_SOURCE_HEIGHT

    const val WORDMARK_WIDTH = (
        LOCKUP_HEIGHT * Textures.WORDMARK_SOURCE_WIDTH + Textures.WORDMARK_SOURCE_HEIGHT / 2
        ) / Textures.WORDMARK_SOURCE_HEIGHT

    const val MARK_GAP = 4

    const val HINT_HEIGHT = 9

    const val SECTION_GAP = 8

    const val BELOW_WHEEL = SECTION_GAP + HINT_HEIGHT + SECTION_GAP + LOCKUP_HEIGHT
}
