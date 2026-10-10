package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.theme.Palette

object RoleHud {

    private const val MARGIN = 6
    private const val PADDING = 7
    private const val MIN_WIDTH = 128
    private const val PILL_PADDING = 4
    private const val HEADER_GAP = 10
    private const val HEADER = 11
    private const val CLOCK_SCALE = 2f
    private const val CLOCK_HEIGHT = 16
    private const val BAR_HEIGHT = 2
    private const val PIP = 4
    private const val PIP_GAP = 3
    private const val STATS_GAP = 9
    private const val HINT_GAP = 4
    private const val KEY_PADDING = 3
    private const val FLASH_MILLIS = 1_200L
    private const val FINAL_SECONDS = 10

    private const val PANEL = 0xB4080C0A.toInt()
    private const val PILL_ALPHA = 0x30
    private const val EDGE_ALPHA = 0xCC
    private const val TRACK = 0x26FFFFFF
    private const val HEAD = 0xFFFFFFFF.toInt()
    private const val KEY_EDGE = 0x55FFFFFF
    private const val POINTS_FROM = 0xFF4EC471.toInt()
    private const val POINTS_TO = 0xFFA9F2C4.toInt()
    private const val POINTS_LABEL = " Pkt"

    private data class Look(val title: String, val colour: Int, val key: Component?, val action: String, val ready: Boolean)

    private var shownProgress = 1f
    private var drawnAt = 0L
    private var lastRemaining = -1
    private var remainingChangedAt = 0L

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register
            if (!RoundState.running) {
                lastRemaining = -1
                return@register
            }

            draw(graphics, minecraft.font, look())
        }
    }

    private fun look(): Look {
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
            RoundState.Role.SPECTATOR -> Look("ZUSCHAUER", Palette.MUTED, null, "", false)
        }
    }

    private fun phaseLabel(): Pair<String, Int> = when (RoundState.clock) {
        RoundState.Clock.HIDING -> "VERSTECKEN" to Palette.BRAND
        RoundState.Clock.HUNT -> "JAGD" to Palette.ERROR
        RoundState.Clock.PAUSED -> "PAUSIERT" to Palette.WARNING
    }

    private fun clockLabel(): String = when (RoundState.clock) {
        RoundState.Clock.HUNT -> "seit"
        RoundState.Clock.PAUSED -> "Sucher weg"
        RoundState.Clock.HIDING -> "noch"
    }

    private fun clockColour(): Int = when {
        RoundState.clock == RoundState.Clock.PAUSED -> Palette.WARNING
        RoundState.clock == RoundState.Clock.HIDING && RoundState.clockSeconds <= FINAL_SECONDS -> Palette.ERROR
        else -> Palette.TEXT
    }

    private fun draw(graphics: GuiGraphics, font: Font, look: Look) {
        val now = System.currentTimeMillis()
        val remaining = RoundState.remainingHiders
        if (lastRemaining >= 0 && remaining != lastRemaining) {
            remainingChangedAt = now
        }
        lastRemaining = remaining

        val (phase, phaseColour) = phaseLabel()
        val pillWidth = font.width(look.title) + 2 * PILL_PADDING

        val width = maxOf(MIN_WIDTH, PADDING * 2 + pillWidth + HEADER_GAP + font.width(phase))
        val height = PADDING + HEADER + 5 + CLOCK_HEIGHT + 6 + BAR_HEIGHT + 6 + font.lineHeight + PADDING - 1

        val left = MARGIN
        val top = MARGIN
        val textLeft = left + PADDING
        val textRight = left + width - PADDING

        card(graphics, left, top, left + width, top + height, PANEL)
        graphics.fill(left + 1, top, left + width - 1, top + 1, withAlpha(look.colour, EDGE_ALPHA))

        var row = top + PADDING

        card(graphics, textLeft, row - 1, textLeft + pillWidth, row + HEADER - 1, withAlpha(look.colour, PILL_ALPHA))
        graphics.drawString(font, look.title, textLeft + PILL_PADDING, row + 1, look.colour, false)
        graphics.drawString(font, phase, textRight - font.width(phase), row + 1, phaseColour, true)
        row += HEADER + 5

        val clock = clock(RoundState.clockSeconds)
        graphics.pose().pushMatrix()
        graphics.pose().translate(textLeft.toFloat(), row.toFloat())
        graphics.pose().scale(CLOCK_SCALE, CLOCK_SCALE)
        graphics.drawString(font, clock, 0, 0, clockColour(), true)
        graphics.pose().popMatrix()

        val label = clockLabel()
        graphics.drawString(font, label, textRight - font.width(label), row + CLOCK_HEIGHT - font.lineHeight, Palette.MUTED, true)
        row += CLOCK_HEIGHT + 6

        val progress = smoothed(now)
        graphics.fill(textLeft, row, textRight, row + BAR_HEIGHT, TRACK)
        val filled = textLeft + ((textRight - textLeft) * progress).toInt()
        if (filled > textLeft) {
            graphics.fill(textLeft, row, filled, row + BAR_HEIGHT, phaseColour)
            graphics.fill(filled - 1, row - 1, filled + 1, row + BAR_HEIGHT + 1, HEAD)
        }
        row += BAR_HEIGHT + 6

        val pipTop = row + (font.lineHeight - PIP) / 2 - 1
        var x = textLeft

        graphics.fill(x, pipTop, x + PIP, pipTop + PIP, Palette.BRAND)
        x += PIP + PIP_GAP
        val hiderCount = remaining.toString()
        val hiderTotal = "/${RoundState.hidersAtStart.coerceAtLeast(remaining)}"
        graphics.drawString(font, hiderCount, x, row, countColour(now), true)
        x += font.width(hiderCount)
        graphics.drawString(font, hiderTotal, x, row, Palette.MUTED, true)
        x += font.width(hiderTotal) + STATS_GAP

        graphics.fill(x, pipTop, x + PIP, pipTop + PIP, Palette.ERROR)
        x += PIP + PIP_GAP
        graphics.drawString(font, RoundState.seekers.toString(), x, row, Palette.TEXT, true)

        RoundState.points?.let {
            val points = it.toString()
            val pointsLeft = textRight - font.width(POINTS_LABEL) - font.width(points)
            gradient(graphics, font, points, pointsLeft, row, POINTS_FROM, POINTS_TO)
            graphics.drawString(font, POINTS_LABEL, textRight - font.width(POINTS_LABEL), row, Palette.MUTED, true)
        }

        look.key?.let { key -> hint(graphics, font, key, look, top + height + HINT_GAP) }
    }

    private fun hint(graphics: GuiGraphics, font: Font, key: Component, look: Look, top: Int) {
        val keyWidth = font.width(key) + 2 * KEY_PADDING
        val left = MARGIN

        card(graphics, left, top - 1, left + keyWidth, top + font.lineHeight + 1, PANEL)
        graphics.fill(left + 1, top + font.lineHeight, left + keyWidth - 1, top + font.lineHeight + 1, KEY_EDGE)
        graphics.drawString(font, key, left + KEY_PADDING, top + 1, Palette.TEXT, false)
        graphics.drawString(
            font, look.action, left + keyWidth + 4, top + 1,
            if (look.ready) Palette.TEXT else Palette.MUTED, true,
        )
    }

    private fun card(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int, colour: Int) {
        graphics.fill(left + 1, top, right - 1, bottom, colour)
        graphics.fill(left, top + 1, left + 1, bottom - 1, colour)
        graphics.fill(right - 1, top + 1, right, bottom - 1, colour)
    }

    private fun withAlpha(colour: Int, alpha: Int): Int = (alpha shl 24) or (colour and 0xFFFFFF)

    private fun gradient(graphics: GuiGraphics, font: Font, text: String, atX: Int, atY: Int, from: Int, to: Int) {
        var x = atX
        val last = (text.length - 1).coerceAtLeast(1)
        text.forEachIndexed { index, character ->
            val piece = character.toString()
            graphics.drawString(font, piece, x, atY, blend(from, to, index.toFloat() / last), true)
            x += font.width(piece)
        }
    }

    private fun smoothed(now: Long): Float {
        val target = RoundState.progress
        val elapsed = if (drawnAt == 0L) 1f else ((now - drawnAt) / 1000f).coerceIn(0f, 1f)
        drawnAt = now
        shownProgress += (target - shownProgress) * (elapsed * 6f).coerceAtMost(1f)
        return shownProgress.coerceIn(0f, 1f)
    }

    private fun countColour(now: Long): Int {
        val since = now - remainingChangedAt
        if (since >= FLASH_MILLIS) return Palette.TEXT
        return blend(Palette.ERROR, Palette.TEXT, since.toFloat() / FLASH_MILLIS)
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

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
}
