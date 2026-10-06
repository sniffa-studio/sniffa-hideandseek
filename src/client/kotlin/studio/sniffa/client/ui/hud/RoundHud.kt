package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.state.RoundState

object RoundHud {

    private const val MARGIN = 6

    private const val ROW_HEIGHT = 11

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register
            if (!RoundState.running) return@register

            draw(graphics, minecraft.font)
        }
    }

    private fun draw(graphics: GuiGraphics, font: Font) {
        val points = RoundState.points ?: return
        val text = pointsText(points)

        val right = graphics.guiWidth() - MARGIN
        val row = graphics.guiHeight() - MARGIN - ROW_HEIGHT

        gradient(graphics, font, text, right - font.width(text), row, POINTS_FROM, POINTS_TO)
    }

    private fun gradient(
        graphics: GuiGraphics,
        font: Font,
        text: String,
        atX: Int,
        atY: Int,
        from: Int,
        to: Int,
    ) {
        var x = atX
        val last = (text.length - 1).coerceAtLeast(1)

        text.forEachIndexed { index, character ->
            val piece = character.toString()
            graphics.drawString(font, piece, x, atY, blend(from, to, index.toFloat() / last), true)
            x += font.width(piece)
        }
    }

    private fun pointsText(points: Int): String = "$points Punkte"

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        var result = 0
        for (shift in intArrayOf(24, 16, 8, 0)) {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            result = result or (((a + (b - a) * t).toInt().coerceIn(0, 255)) shl shift)
        }
        return result
    }

    private const val POINTS_FROM = 0xFF4EC471.toInt()
    private const val POINTS_TO = 0xFFA9F2C4.toInt()
}
