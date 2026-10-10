package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.zone.DangerZone
import kotlin.math.PI
import kotlin.math.sin

object ZoneWarning {

    private const val FROM_BOTTOM = 96

    private const val EDGE_STRENGTH = 0.32f
    private const val PULSE_DEPTH = 0.25f
    private const val CALM_PERIOD_MILLIS = 1600.0
    private const val URGENT_PERIOD_MILLIS = 500.0
    private const val URGENT_SECONDS = 10

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register
            if (RoundState.role != RoundState.Role.HIDER) return@register
            if (!DangerZone.caughtInside()) return@register

            draw(graphics, DangerZone.secondsLeft())
        }
    }

    private fun draw(graphics: GuiGraphics, secondsLeft: Int) {
        val period = if (secondsLeft <= URGENT_SECONDS) URGENT_PERIOD_MILLIS else CALM_PERIOD_MILLIS
        val beat = (0.5 + 0.5 * sin(System.currentTimeMillis() / period * 2.0 * PI)).toFloat()
        Vignette.draw(graphics, Palette.ERROR, EDGE_STRENGTH * (1f - PULSE_DEPTH + PULSE_DEPTH * beat))

        val font = Minecraft.getInstance().font

        val headline = "GEFAHRENZONE"
        val detail = "Raus hier  ·  ${clock(secondsLeft)}"

        val y = graphics.guiHeight() - FROM_BOTTOM

        graphics.drawString(
            font, headline,
            (graphics.guiWidth() - font.width(headline)) / 2, y,
            Palette.ERROR, true,
        )
        graphics.drawString(
            font, detail,
            (graphics.guiWidth() - font.width(detail)) / 2, y + 12,
            Palette.TEXT, true,
        )
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
