package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.ui.theme.Palette

object ScanReadout {

    private const val HOLD_MILLIS = 2200L
    private const val FADE_MILLIS = 500f

    private const val WAIT_MILLIS = 2600L

    @Volatile
    private var count = 0

    private var range = 0

    @Volatile
    private var startedAt = 0L

    fun show(count: Int, range: Int) {
        this.count = count
        this.range = range
        this.startedAt = System.currentTimeMillis()
    }

    fun forget() {
        startedAt = 0L
    }

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()
            if (minecraft.options.hideGui) return@register
            if (startedAt == 0L) return@register

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < WAIT_MILLIS || elapsed > WAIT_MILLIS + HOLD_MILLIS) return@register

            draw(graphics, (elapsed - WAIT_MILLIS).toFloat())
        }
    }

    private fun draw(graphics: GuiGraphics, sinceLanded: Float) {
        val fade = ((HOLD_MILLIS - sinceLanded) / FADE_MILLIS).coerceIn(0f, 1f)
        if (fade <= 0f) return

        val font = Minecraft.getInstance().font

        val where = if (range > 0) "in $range Blöcken" else "in der Nähe"
        val text = when {
            count == 0 -> "niemand $where"
            count == 1 -> "1 Verstecker $where"
            else -> "$count Verstecker $where"
        }
        val colour = if (count == 0) Palette.MUTED else Palette.TEXT

        val x = (graphics.guiWidth() - font.width(text)) / 2
        val y = graphics.guiHeight() - 68

        graphics.drawString(font, text, x, y, alpha(colour, fade), true)
    }

    private fun alpha(argb: Int, amount: Float): Int {
        val a = (((argb ushr 24) and 0xFF) * amount.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }
}
