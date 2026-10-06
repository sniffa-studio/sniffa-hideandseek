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
    private const val MIN_WIDTH = 132
    private const val GAP = 10
    private const val BAR_HEIGHT = 3
    private const val PIP = 5
    private const val FLASH_MILLIS = 1_200L
    private const val FINAL_SECONDS = 10

    private const val PANEL = 0x99060B09.toInt()
    private const val CHIP = 0x33FFFFFF
    private const val CHIP_EDGE = 0x66FFFFFF
    private const val DIVIDER = 0x22FFFFFF
    private const val TRACK = 0x33FFFFFF
    private const val POINTS_FROM = 0xFF4EC471.toInt()
    private const val POINTS_TO = 0xFFA9F2C4.toInt()
    private const val POINTS_LABEL = "Punkte"

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
        RoundState.Clock.HIDING -> "Verstecken" to Palette.BRAND
        RoundState.Clock.HUNT -> "Jagd" to Palette.ERROR
        RoundState.Clock.PAUSED -> "Pausiert · Sucher weg" to Palette.WARNING
    }

    private fun clockPrefix(): String = when (RoundState.clock) {
        RoundState.Clock.HUNT -> "seit "
        else -> "noch "
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

        val title = Component.literal(look.title).withStyle(ChatFormatting.BOLD)
        val keyWidth = look.key?.let { font.width(it) + 2 * CHIP_PADDING } ?: 0
        val actionGap = 4

        val (phase, phaseColour) = phaseLabel()
        val clock = Component.literal(clock(RoundState.clockSeconds)).withStyle(ChatFormatting.BOLD)
        val prefix = clockPrefix()
        val clockWidth = font.width(prefix) + font.width(clock)
        val points = RoundState.points?.let { "$it" }

        val hiderCount = remaining.toString()
        val hiderTotal = "/${RoundState.hidersAtStart.coerceAtLeast(remaining)} Verstecker"
        val seekerText = "${RoundState.seekers} Sucher"
        val hidersWidth = PIP + 4 + font.width(hiderCount) + font.width(hiderTotal)
        val seekersWidth = PIP + 4 + font.width(seekerText)

        val contentWidth = maxOf(
            MIN_WIDTH,
            font.width(title),
            if (look.key == null) 0 else keyWidth + actionGap + font.width(look.action),
            font.width(phase) + GAP + clockWidth,
            hidersWidth + GAP + seekersWidth,
            points?.let { font.width(POINTS_LABEL) + GAP + font.width(it) } ?: 0,
        )
        val roleRows = if (look.key == null) 1 else 2
        val pointsRows = if (points == null) 0 else 5 + ROW_HEIGHT
        val height = PADDING * 2 + ROW_HEIGHT * roleRows + 5 + ROW_HEIGHT + BAR_HEIGHT + 5 + ROW_HEIGHT - 2 + pointsRows
        val width = ACCENT + PADDING * 2 + contentWidth

        val left = MARGIN
        val top = MARGIN
        val textLeft = left + ACCENT + PADDING
        val textRight = textLeft + contentWidth

        graphics.fill(left, top, left + width, top + height, PANEL)
        graphics.fill(left, top, left + ACCENT, top + height, look.colour)

        var row = top + PADDING

        graphics.drawString(font, title, textLeft, row, look.colour, true)
        row += ROW_HEIGHT

        if (look.key != null) {
            val chipTop = row - 2
            val chipBottom = row + font.lineHeight
            graphics.fill(textLeft, chipTop, textLeft + keyWidth, chipBottom, CHIP_EDGE)
            graphics.fill(textLeft + 1, chipTop + 1, textLeft + keyWidth - 1, chipBottom - 1, CHIP)
            graphics.drawString(font, look.key, textLeft + CHIP_PADDING, row, Palette.TEXT, false)
            graphics.drawString(
                font, look.action, textLeft + keyWidth + actionGap, row,
                if (look.ready) Palette.TEXT else Palette.MUTED, true,
            )
            row += ROW_HEIGHT
        }

        row += 1
        graphics.fill(textLeft, row, textRight, row + 1, DIVIDER)
        row += 4

        graphics.drawString(font, phase, textLeft, row, phaseColour, true)
        graphics.drawString(font, prefix, textRight - clockWidth, row, Palette.MUTED, true)
        graphics.drawString(font, clock, textRight - font.width(clock), row, clockColour(), true)
        row += ROW_HEIGHT

        val progress = smoothed(now)
        graphics.fill(textLeft, row, textRight, row + BAR_HEIGHT, TRACK)
        val filled = textLeft + ((textRight - textLeft) * progress).toInt()
        if (filled > textLeft) {
            graphics.fill(textLeft, row, filled, row + BAR_HEIGHT, phaseColour)
        }
        row += BAR_HEIGHT + 5

        val pipTop = row + (font.lineHeight - PIP) / 2 - 1
        graphics.fill(textLeft, pipTop, textLeft + PIP, pipTop + PIP, Palette.BRAND)
        var x = textLeft + PIP + 4
        graphics.drawString(font, hiderCount, x, row, countColour(now), true)
        x += font.width(hiderCount)
        graphics.drawString(font, hiderTotal, x, row, Palette.MUTED, true)

        val seekersLeft = textRight - seekersWidth
        graphics.fill(seekersLeft, pipTop, seekersLeft + PIP, pipTop + PIP, Palette.ERROR)
        graphics.drawString(font, seekerText, seekersLeft + PIP + 4, row, Palette.TEXT, true)

        if (points != null) {
            row += ROW_HEIGHT
            graphics.fill(textLeft, row, textRight, row + 1, DIVIDER)
            row += 4
            graphics.drawString(font, POINTS_LABEL, textLeft, row, Palette.MUTED, true)
            gradient(graphics, font, points, textRight - font.width(points), row, POINTS_FROM, POINTS_TO)
        }
    }

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
