package studio.sniffa.client.ui.text

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.sin

object ShineText {

    const val SHINE_GOLD = 0xF2C55B
    const val SHINE_TEXT = 0xCFD7E3
    const val SHINE_GREEN = 0x4FD08C
    const val TITLE_BLUE = 0x5FA2F9
    const val TITLE_RED = 0xFD3C4F
    const val TITLE_XMAS = 0xF5C352
    const val TITLE_GOLD = 0xF7BE52

    enum class Style(val colour: Int, val glow: Int) {
        BLUE(TITLE_BLUE, 0x3A7BFF),
        RED(TITLE_RED, 0xFF2840),
        XMAS(TITLE_XMAS, 0xD42A3A),
        GOLD(TITLE_GOLD, 0xD42A3A),
        SHINE(SHINE_GOLD, 0xE8A33A),
    }

    private const val LETTER_DELAY_MILLIS = 35f
    private const val LETTER_IN_MILLIS = 180f
    private const val DROP = 5f
    private const val WAVE_HEIGHT = 0.8f
    private val RING = arrayOf(
        floatArrayOf(-1f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, -1f), floatArrayOf(0f, 1f),
        floatArrayOf(-0.7f, -0.7f), floatArrayOf(0.7f, -0.7f), floatArrayOf(-0.7f, 0.7f), floatArrayOf(0.7f, 0.7f),
    )

    fun width(font: Font, text: String): Int = font.width(text)

    fun draw(
        graphics: GuiGraphicsExtractor,
        font: Font,
        text: String,
        x: Float,
        y: Float,
        style: Style,
        alpha: Float,
        elapsedMillis: Long,
        stagger: Boolean = true,
        wave: Boolean = false,
    ) {
        if (alpha <= 0.02f || text.isEmpty()) return
        var cursor = x
        for ((index, letter) in text.withIndex()) {
            val glyph = letter.toString()
            val advance = font.width(glyph).toFloat()
            val since = elapsedMillis - index * LETTER_DELAY_MILLIS
            val shown = if (stagger) (since / LETTER_IN_MILLIS).coerceIn(0f, 1f) else 1f
            if (shown > 0f && !letter.isWhitespace()) {
                val ease = 1f - (1f - shown) * (1f - shown)
                val lift = (1f - ease) * DROP + if (wave) sin(elapsedMillis / 260f - index * 0.55f) * WAVE_HEIGHT else 0f
                val strength = alpha * ease
                graphics.pose().pushMatrix()
                graphics.pose().translate(cursor, y - lift)
                for (offset in RING) {
                    graphics.pose().pushMatrix()
                    graphics.pose().translate(offset[0], offset[1])
                    graphics.text(font, glyph, 0, 0, argb(style.glow, strength * 0.16f), false)
                    graphics.pose().popMatrix()
                }
                graphics.text(font, glyph, 0, 0, argb(style.colour, strength), true)
                graphics.pose().popMatrix()
            }
            cursor += advance
        }
    }

    fun drawCentred(
        graphics: GuiGraphicsExtractor,
        font: Font,
        text: String,
        centreX: Float,
        y: Float,
        style: Style,
        alpha: Float,
        elapsedMillis: Long,
        stagger: Boolean = true,
        wave: Boolean = false,
    ) = draw(graphics, font, text, centreX - font.width(text) / 2f, y, style, alpha, elapsedMillis, stagger, wave)

    private fun argb(rgb: Int, alpha: Float): Int = ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xFFFFFF)
}
