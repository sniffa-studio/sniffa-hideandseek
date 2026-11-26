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
    const val TITLE_XMAS = 0xC3142F
    const val TITLE_WINNER = 0xCB1A35

    enum class Style(val colour: Int, val glow: Int) {
        BLUE(TITLE_BLUE, 0x3A7BFF),
        RED(TITLE_RED, 0xFF2840),
        XMAS(TITLE_XMAS, 0xFF3A4A),
        WINNER(TITLE_WINNER, 0xF2B84A),
        SHINE(SHINE_GOLD, 0xE8A33A),
    }

    private const val LETTER_DELAY_MILLIS = 35f
    private const val LETTER_IN_MILLIS = 180f
    private const val DROP = 5f
    private const val WAVE_HEIGHT = 0.8f
    private const val BOUNCE_IN_MILLIS = 420f
    private const val BOUNCE_DROP = 12f
    private const val BURST_MILLIS = 380f
    private const val BURST_REACH = 7f
    private const val BURST_COLOUR = 0xFFF6E2
    private val BURST = arrayOf(
        floatArrayOf(0f, -1f), floatArrayOf(0.95f, -0.31f), floatArrayOf(0.59f, 0.81f),
        floatArrayOf(-0.59f, 0.81f), floatArrayOf(-0.95f, -0.31f),
    )
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
        bounce: Boolean = false,
    ) {
        if (alpha <= 0.02f || text.isEmpty()) return
        var cursor = x
        for ((index, letter) in text.withIndex()) {
            val glyph = letter.toString()
            val advance = font.width(glyph).toFloat()
            val since = elapsedMillis - index * LETTER_DELAY_MILLIS
            val shown = if (bounce) (since / BOUNCE_IN_MILLIS).coerceIn(0f, 1f)
                else if (stagger) (since / LETTER_IN_MILLIS).coerceIn(0f, 1f) else 1f
            if (shown > 0f && !letter.isWhitespace()) {
                val ease = if (bounce) (shown * 3f).coerceAtMost(1f) else 1f - (1f - shown) * (1f - shown)
                val fall = if (bounce) (1f - backOut(shown)) * BOUNCE_DROP else (1f - ease) * DROP
                val lift = fall + if (wave) sin(elapsedMillis / 260f - index * 0.55f) * WAVE_HEIGHT else 0f
                val strength = alpha * ease
                graphics.pose().pushMatrix()
                graphics.pose().translate(cursor, y - lift)
                for (offset in RING) {
                    graphics.pose().pushMatrix()
                    graphics.pose().translate(offset[0], offset[1])
                    graphics.text(font, glyph, 0, 0, argb(style.glow, strength * 0.06f), false)
                    graphics.pose().popMatrix()
                }
                graphics.text(font, glyph, 0, 0, argb(style.colour, strength), true)
                if (bounce) burst(graphics, advance, font.lineHeight.toFloat(), since - BOUNCE_IN_MILLIS * 0.55f, alpha)
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
        bounce: Boolean = false,
    ) = draw(graphics, font, text, centreX - font.width(text) / 2f, y, style, alpha, elapsedMillis, stagger, wave, bounce)

    private fun backOut(t: Float): Float {
        val c = 1.9f
        val u = t - 1f
        return 1f + (c + 1f) * u * u * u + c * u * u
    }

    private fun burst(graphics: GuiGraphicsExtractor, width: Float, height: Float, since: Float, alpha: Float) {
        if (since < 0f || since > BURST_MILLIS) return
        val progress = since / BURST_MILLIS
        val fade = (1f - progress) * alpha
        val reach = 2f + BURST_REACH * (1f - (1f - progress) * (1f - progress))
        val centreX = width / 2f - 0.5f
        val centreY = height / 2f - 1f
        for (ray in BURST) {
            val x = (centreX + ray[0] * reach).toInt()
            val y = (centreY + ray[1] * reach).toInt()
            graphics.fill(x, y, x + 1, y + 1, argb(BURST_COLOUR, fade))
        }
    }

    private fun argb(rgb: Int, alpha: Float): Int = ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xFFFFFF)
}
