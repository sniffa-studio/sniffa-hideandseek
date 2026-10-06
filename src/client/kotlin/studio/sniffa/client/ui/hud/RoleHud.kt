package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.theme.Palette

object RoleHud {

    private const val MARGIN = 6
    private const val PADDING = 6
    private const val ACCENT = 2
    private const val ROW_HEIGHT = 12
    private const val CHIP_PADDING = 3

    private const val PANEL = 0x99060B09.toInt()
    private const val CHIP = 0x33FFFFFF
    private const val CHIP_EDGE = 0x66FFFFFF

    private data class Look(val title: String, val colour: Int, val key: Component, val action: String, val ready: Boolean)

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register
            if (!RoundState.running) return@register

            val look = look() ?: return@register
            draw(graphics, minecraft.font, look)
        }
    }

    private fun look(): Look? {
        val hiding = RoundState.phase == RoundState.Phase.HIDING
        return when (RoundState.role) {
            RoundState.Role.SEEKER -> Look(
                "SUCHER", Palette.ERROR, Keybindings.shopKey(),
                if (hiding) "Rad ab der Jagd" else "Fähigkeiten-Rad", !hiding,
            )
            RoundState.Role.HIDER -> Look(
                "VERSTECKER", Palette.BRAND, Keybindings.shopKey(),
                if (hiding) "Rad ab der Jagd" else "Fähigkeiten-Rad", !hiding,
            )
            RoundState.Role.STAFF -> Look(
                "REGIE", Palette.MUTED, Keybindings.watchKey(), "Teleporter", true,
            )
            RoundState.Role.SPECTATOR -> null
        }
    }

    private fun draw(graphics: GuiGraphics, font: Font, look: Look) {
        val title = Component.literal(look.title).withStyle(ChatFormatting.BOLD)
        val keyWidth = font.width(look.key) + 2 * CHIP_PADDING
        val actionGap = 4

        val contentWidth = maxOf(font.width(title), keyWidth + actionGap + font.width(look.action))
        val width = ACCENT + PADDING * 2 + contentWidth
        val height = PADDING * 2 + ROW_HEIGHT * 2 - 2

        val left = MARGIN
        val top = MARGIN

        graphics.fill(left, top, left + width, top + height, PANEL)
        graphics.fill(left, top, left + ACCENT, top + height, look.colour)

        val textLeft = left + ACCENT + PADDING
        var row = top + PADDING

        graphics.drawString(font, title, textLeft, row, look.colour, true)
        row += ROW_HEIGHT

        val chipTop = row - 2
        val chipBottom = row + font.lineHeight
        graphics.fill(textLeft, chipTop, textLeft + keyWidth, chipBottom, CHIP_EDGE)
        graphics.fill(textLeft + 1, chipTop + 1, textLeft + keyWidth - 1, chipBottom - 1, CHIP)
        graphics.drawString(font, look.key, textLeft + CHIP_PADDING, row, Palette.TEXT, false)

        graphics.drawString(
            font, look.action, textLeft + keyWidth + actionGap, row,
            if (look.ready) Palette.TEXT else Palette.MUTED, true,
        )
    }
}
