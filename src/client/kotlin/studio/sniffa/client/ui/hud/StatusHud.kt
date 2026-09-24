package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import studio.sniffa.Hideandseek
import studio.sniffa.client.net.Handshake
import studio.sniffa.client.ui.theme.Palette

object StatusHud {

    private const val MARGIN = 4

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register

            val (text, colour) = line()
            graphics.drawString(minecraft.font, text, MARGIN, MARGIN, colour)
        }
    }

    private fun line(): Pair<String, Int> = when (Handshake.state) {
        Handshake.State.REGISTERED ->
            "Hide & Seek ${Hideandseek.version}  ·  connected" to Palette.BRAND

        Handshake.State.SERVER_UNAWARE ->
            "Hide & Seek ${Hideandseek.version}  ·  no event server" to Palette.MUTED

        Handshake.State.DISCONNECTED ->
            "Hide & Seek ${Hideandseek.version}" to Palette.ERROR
    }
}
