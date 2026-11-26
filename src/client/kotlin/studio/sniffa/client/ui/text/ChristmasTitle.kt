package studio.sniffa.client.ui.text

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import studio.sniffa.Hideandseek
import java.util.Optional

object ChristmasTitle {

    private const val SHOW_MILLIS = 3800L
    private const val FADE_IN = 200f
    private const val FADE_OUT = 650f
    private const val HEADLINE_SCALE = 4f
    private const val DETAIL_SCALE = 2f

    private class Shown(val headline: String, val detail: Component?, val at: Long)

    @Volatile
    private var shown: Shown? = null

    fun install() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> shown = null }
        HudElementRegistry.addLast(Hideandseek.id("christmas_title")) { graphics, _ -> draw(graphics) }
    }

    @JvmStatic
    fun claims(title: Component): Boolean = colourOf(title) == ShineText.TITLE_XMAS

    @JvmStatic
    fun show(title: Component, detail: Component?) {
        shown = Shown(title.string, detail, System.currentTimeMillis())
    }

    private fun colourOf(component: Component): Int? {
        val found = component.visit({ style: Style, text: String ->
            if (text.isBlank()) Optional.empty() else Optional.ofNullable(style.color?.value)
        }, Style.EMPTY)
        return found.orElse(null)
    }

    private fun draw(graphics: GuiGraphicsExtractor) {
        val title = shown ?: return
        val since = System.currentTimeMillis() - title.at
        if (since > SHOW_MILLIS) {
            shown = null
            return
        }
        val fade = minOf(1f, (SHOW_MILLIS - since) / FADE_OUT).coerceIn(0f, 1f)
        val appear = (since / FADE_IN).coerceIn(0f, 1f)
        val font = Minecraft.getInstance().font
        val centreX = graphics.guiWidth() / 2f
        val centreY = graphics.guiHeight() / 2f

        graphics.pose().pushMatrix()
        graphics.pose().translate(centreX, centreY)
        graphics.pose().scale(HEADLINE_SCALE, HEADLINE_SCALE)
        ShineText.drawCentred(graphics, font, title.headline, 0f, -10f, ShineText.Style.XMAS, fade, since, bounce = true)
        graphics.pose().popMatrix()

        val detail = title.detail ?: return
        val alpha = ((fade * appear * 255f).toInt().coerceIn(0, 255)) shl 24
        graphics.pose().pushMatrix()
        graphics.pose().translate(centreX, centreY)
        graphics.pose().scale(DETAIL_SCALE, DETAIL_SCALE)
        graphics.text(font, detail, -font.width(detail) / 2, 5, alpha or 0xFFFFFF, true)
        graphics.pose().popMatrix()
    }
}
