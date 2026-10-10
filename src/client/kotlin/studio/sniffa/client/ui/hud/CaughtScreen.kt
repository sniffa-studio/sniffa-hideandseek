package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.net.payload.CaughtPayload

object CaughtScreen {

    private const val FADE_MILLIS = 250f
    private const val SHOW_MILLIS = 5000L

    private const val SHADE = 0xC8141618.toInt()

    private const val HEADLINE_SCALE = 4f
    private const val HEADLINE_LIFT = 26
    private const val DETAIL_DROP = 18

    @Volatile
    private var caught: CaughtPayload? = null

    @Volatile
    private var startedAt = 0L

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(CaughtPayload.TYPE) { payload, _ ->
            caught = payload
            startedAt = System.currentTimeMillis()
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> caught = null }

        HudRenderCallback.EVENT.register { graphics, _ ->
            val shown = caught ?: return@register

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed > SHOW_MILLIS) {
                caught = null
                return@register
            }

            draw(graphics, shown, (elapsed / FADE_MILLIS).coerceIn(0f, 1f))
        }
    }

    private fun draw(graphics: GuiGraphics, shown: CaughtPayload, fade: Float) {
        val font = Minecraft.getInstance().font
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        graphics.fill(0, 0, width, height, Vignette.alpha(SHADE, fade))

        val headline = "GEFANGEN"
        graphics.pose().pushMatrix()
        graphics.pose().translate(width / 2f, height / 2f - HEADLINE_LIFT)
        graphics.pose().scale(HEADLINE_SCALE, HEADLINE_SCALE)
        graphics.drawString(font, headline, -font.width(headline) / 2, -font.lineHeight / 2, Vignette.alpha(Palette.ERROR, fade), true)
        graphics.pose().popMatrix()

        val detail = "von ${shown.seeker}  ·  Platz #${shown.place} von ${shown.total}"
        graphics.drawString(
            font, detail,
            (width - font.width(detail)) / 2, height / 2 + DETAIL_DROP,
            Vignette.alpha(Palette.TEXT, fade), true,
        )
    }
}
