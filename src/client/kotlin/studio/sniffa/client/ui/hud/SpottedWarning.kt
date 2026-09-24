package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.ui.theme.Palette

object SpottedWarning {

    private const val WAIT_MILLIS = 2600L

    private const val HOLD_MILLIS = 2400L

    private const val RISE_MILLIS = 140f
    private const val FALL_MILLIS = 900f

    private const val EDGE_SHARE = 0.18f

    @Volatile
    private var range = 0

    @Volatile
    private var startedAt = 0L

    fun show(range: Int) {
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
        val rising = (sinceLanded / RISE_MILLIS).coerceIn(0f, 1f)
        val falling = ((HOLD_MILLIS - sinceLanded) / FALL_MILLIS).coerceIn(0f, 1f)
        val strength = rising * falling
        if (strength <= 0f) return

        Vignette.draw(graphics, Palette.ERROR, strength * 0.55f)

        val font = Minecraft.getInstance().font
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        val headline = "ERFASST"
        val detail = "Ein Radar hat dich gefunden  ·  $range Blöcke"

        val top = height / 2 - 34

        graphics.drawString(
            font, headline,
            (width - font.width(headline)) / 2, top,
            alpha(Palette.ERROR, strength), true,
        )
        graphics.drawString(
            font, detail,
            (width - font.width(detail)) / 2, top + 12,
            alpha(Palette.TEXT, strength * 0.9f), true,
        )
    }

    private fun alpha(argb: Int, amount: Float): Int = Vignette.alpha(argb, amount)
}
