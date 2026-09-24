package studio.sniffa.client.ui.hud

import net.minecraft.client.gui.GuiGraphics

object Vignette {

    private const val EDGE_SHARE = 0.18f

    fun draw(graphics: GuiGraphics, argb: Int, strength: Float) {
        if (strength <= 0f) {
            return
        }

        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        val inset = (minOf(width, height) * EDGE_SHARE).toInt().coerceAtLeast(1)
        val edge = alpha(argb, strength)
        val clear = alpha(argb, 0f)

        graphics.fillGradient(0, 0, width, inset, edge, clear)
        graphics.fillGradient(0, height - inset, width, height, clear, edge)

        for (step in 0 until inset) {
            val fade = 1f - step.toFloat() / inset
            val ink = alpha(argb, strength * fade)
            graphics.fill(step, 0, step + 1, height, ink)
            graphics.fill(width - step - 1, 0, width - step, height, ink)
        }
    }

    fun alpha(argb: Int, amount: Float): Int {
        val a = (((argb ushr 24) and 0xFF) * amount.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }
}
