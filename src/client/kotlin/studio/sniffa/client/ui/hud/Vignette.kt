package studio.sniffa.client.ui.hud

import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.map.MapOverlay

object Vignette {

    fun draw(graphics: GuiGraphics, argb: Int, strength: Float) {
        if (strength <= 0f) {
            return
        }

        MapOverlay.panel(
            graphics,
            MapOverlay.VIGNETTE,
            MapOverlay.wholeScreen(graphics.guiWidth(), graphics.guiHeight()),
            alpha(argb, strength),
        )
    }

    fun alpha(argb: Int, amount: Float): Int {
        val a = (((argb ushr 24) and 0xFF) * amount.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }
}
