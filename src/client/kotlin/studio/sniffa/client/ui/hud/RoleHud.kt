package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.theme.Palette
import kotlin.math.sin

object RoleHud {

    private const val MARGIN = 8
    private const val PADDING = 9
    private const val ACCENT = 2
    private const val MIN_WIDTH = 156
    private const val HEADER = 11
    private const val CHIP_PADDING = 4
    private const val LETTER_GAP = 1
    private const val CLOCK_SCALE = 2f
    private const val CLOCK_HEIGHT = 16
    private const val BAR_HEIGHT = 3
    private const val BAR_STEP = 3
    private const val PIP = 5
    private const val PIP_GAP = 4
    private const val STATS_GAP = 10
    private const val HINT_GAP = 5
    private const val KEY_PADDING = 4
    private const val SHADOW = 2

    private const val SLIDE_MILLIS = 380f
    private const val POP_MILLIS = 450f
    private const val POP_SCALE = 0.45f
    private const val FLASH_MILLIS = 1_200L
    private const val TICK_MILLIS = 650f
    private const val READY_MILLIS = 2_400f
    private const val SHIMMER_MILLIS = 2_200.0
    private const val FINAL_SECONDS = 10

    private const val PANEL_TOP = 0xDC161C1A.toInt()
    private const val PANEL_BOTTOM = 0xDC080C0A.toInt()
    private const val SHADOW_COLOUR = 0x50000000
    private const val HIGHLIGHT = 0x1CFFFFFF
    private const val CHIP_TEXT = 0xFF0B100D.toInt()
    private const val TRACK = 0x22FFFFFF
    private const val HEAD = 0xFFFFFFFF.toInt()
    private const val HEAD_GLOW = 0x55FFFFFF
    private const val GLOW_ALPHA = 0x38
    private const val KEY_FACE = 0xE6202624.toInt()
    private const val KEY_EDGE = 0x66FFFFFF
    private const val POINTS_FROM = 0xFF4EC471.toInt()
    private const val POINTS_TO = 0xFFA9F2C4.toInt()
    private const val POINTS_LABEL = " ᴘᴋᴛ"

    private const val HIDERS = "ᴠᴇʀꜱᴛᴇᴄᴋᴛ"
    private const val SEEKERS = "ꜱᴜᴄʜᴇɴ"

    private data class Look(val title: String, val colour: Int, val key: Component?, val action: String, val ready: Boolean)

    private var shownProgress = 1f
    private var drawnAt = 0L
    private var lastRemaining = -1
    private var remainingChangedAt = 0L
    private var runningSince = 0L
    private var lastSecond = -1
    private var secondChangedAt = 0L
    private var wasReady = false
    private var readySince = 0L

    fun install() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.options.hideGui) return@register
            if (!RoundState.running) {
                lastRemaining = -1
                runningSince = 0L
                wasReady = false
                return@register
            }

            draw(graphics, minecraft.font, look())
        }
    }

    private fun look(): Look {
        val hiding = RoundState.phase == RoundState.Phase.HIDING
        return when (RoundState.role) {
            RoundState.Role.SEEKER -> Look(
                "ꜱᴜᴄʜᴇʀ", Palette.ERROR, Keybindings.shopKey(),
                if (hiding) "Rad ab der Jagd" else "Fähigkeiten-Rad", !hiding,
            )
            RoundState.Role.HIDER -> Look(
                "ᴠᴇʀꜱᴛᴇᴄᴋᴇʀ", Palette.BRAND, Keybindings.shopKey(),
                if (hiding) "Rad ab der Jagd" else "Fähigkeiten-Rad", !hiding,
            )
            RoundState.Role.STAFF -> Look(
                "ʀᴇɢɪᴇ", Palette.MUTED, Keybindings.watchKey(), "Teleporter", true,
            )
            RoundState.Role.SPECTATOR -> Look("ᴢᴜꜱᴄʜᴀᴜᴇʀ", Palette.MUTED, null, "", false)
        }
    }

    private fun phaseLabel(): Pair<String, Int> = when (RoundState.clock) {
        RoundState.Clock.HIDING -> "ᴠᴇʀꜱᴛᴇᴄᴋᴇɴ" to Palette.BRAND
        RoundState.Clock.HUNT -> "ᴊᴀɢᴅ" to Palette.ERROR
        RoundState.Clock.PAUSED -> "ᴘᴀᴜꜱᴇ" to Palette.WARNING
    }

    private fun clockLabel(): String = when (RoundState.clock) {
        RoundState.Clock.HUNT -> "seit"
        RoundState.Clock.PAUSED -> "Sucher weg"
        RoundState.Clock.HIDING -> "noch"
    }

    private fun finalCountdown(): Boolean =
        RoundState.clock == RoundState.Clock.HIDING && RoundState.clockSeconds <= FINAL_SECONDS

    private fun clockColour(): Int = when {
        RoundState.clock == RoundState.Clock.PAUSED -> Palette.WARNING
        finalCountdown() -> Palette.ERROR
        else -> Palette.TEXT
    }

    private fun draw(graphics: GuiGraphics, font: Font, look: Look) {
        val now = System.currentTimeMillis()
        if (runningSince == 0L) runningSince = now

        val remaining = RoundState.remainingHiders
        if (lastRemaining >= 0 && remaining != lastRemaining) {
            remainingChangedAt = now
        }
        lastRemaining = remaining

        if (RoundState.clockSeconds != lastSecond) {
            lastSecond = RoundState.clockSeconds
            secondChangedAt = now
        }

        if (look.ready && !wasReady) readySince = now
        wasReady = look.ready

        val (phase, phaseColour) = phaseLabel()
        val chipWidth = spacedWidth(font, phase) + 2 * CHIP_PADDING

        val pointsWidth = RoundState.points?.let { STATS_GAP + font.width(it.toString()) + font.width(POINTS_LABEL) } ?: 0
        val statsWidth = statWidth(font, remaining.toString(), HIDERS) + STATS_GAP +
            statWidth(font, RoundState.seekers.toString(), SEEKERS) + pointsWidth
        val width = maxOf(
            MIN_WIDTH,
            PADDING * 2 + ACCENT + spacedWidth(font, look.title) + 12 + chipWidth,
            PADDING * 2 + ACCENT + statsWidth,
        )
        val height = PADDING + HEADER + 7 + CLOCK_HEIGHT + 7 + BAR_HEIGHT + 7 + font.lineHeight + PADDING - 1

        val slide = ease(((now - runningSince) / SLIDE_MILLIS).coerceIn(0f, 1f))
        val left = MARGIN - ((width + MARGIN) * (1f - slide)).toInt()
        val top = MARGIN
        val right = left + width
        val textLeft = left + ACCENT + PADDING
        val textRight = right - PADDING

        rounded(graphics, left + SHADOW, top + SHADOW, right + SHADOW, top + height + SHADOW, SHADOW_COLOUR)
        panel(graphics, left, top, right, top + height)
        graphics.fill(left, top + 1, left + ACCENT, top + height - 1, look.colour)
        graphics.fill(left + ACCENT + 1, top, right - 1, top + 1, HIGHLIGHT)

        var row = top + PADDING

        spaced(graphics, font, look.title, textLeft, row + 1, look.colour)
        val chipLeft = textRight - chipWidth
        rounded(graphics, chipLeft, row - 1, textRight, row + HEADER - 1, phaseColour)
        spaced(graphics, font, phase, chipLeft + CHIP_PADDING, row + 1, CHIP_TEXT, shadow = false)
        row += HEADER + 7

        val tick = if (finalCountdown()) 1f - ((now - secondChangedAt) / TICK_MILLIS).coerceIn(0f, 1f) else 0f
        val clock = clock(RoundState.clockSeconds)
        val colour = clockColour()
        val clockScale = CLOCK_SCALE * (1f + 0.12f * ease(tick))
        val lift = (CLOCK_HEIGHT * (clockScale / CLOCK_SCALE - 1f) / 2f)
        graphics.pose().pushMatrix()
        graphics.pose().translate(textLeft.toFloat(), row - lift)
        graphics.pose().scale(clockScale, clockScale)
        val glow = withAlpha(colour, (GLOW_ALPHA * (1f + 2f * tick)).toInt().coerceAtMost(0xFF))
        for ((dx, dy) in GLOW_OFFSETS) {
            graphics.drawString(font, clock, dx, dy, glow, false)
        }
        graphics.drawString(font, clock, 0, 0, colour, true)
        graphics.pose().popMatrix()

        val label = clockLabel()
        graphics.drawString(font, label, textRight - font.width(label), row + CLOCK_HEIGHT - font.lineHeight, Palette.MUTED, true)
        row += CLOCK_HEIGHT + 7

        bar(graphics, textLeft, row, textRight, smoothed(now), phaseColour)
        row += BAR_HEIGHT + 7

        var x = textLeft
        x = stat(graphics, font, x, row, Palette.BRAND, remaining.toString(), HIDERS, now)
        x += STATS_GAP
        stat(graphics, font, x, row, Palette.ERROR, RoundState.seekers.toString(), SEEKERS, null)

        RoundState.points?.let {
            val points = it.toString()
            val pointsLeft = textRight - font.width(POINTS_LABEL) - font.width(points)
            shimmer(graphics, font, points, pointsLeft, row, now)
            graphics.drawString(font, POINTS_LABEL, textRight - font.width(POINTS_LABEL), row, Palette.MUTED, true)
        }

        look.key?.let { key -> hint(graphics, font, key, look, left, top + height + HINT_GAP, now) }
    }

    private fun stat(graphics: GuiGraphics, font: Font, atX: Int, row: Int, colour: Int, count: String, label: String, now: Long?): Int {
        var x = atX
        val pipTop = row + (font.lineHeight - PIP) / 2 - 1
        rounded(graphics, x, pipTop, x + PIP, pipTop + PIP, colour)
        x += PIP + PIP_GAP

        val since = now?.let { (it - remainingChangedAt).toFloat() } ?: Float.MAX_VALUE
        val pop = 1f - (since / POP_MILLIS).coerceIn(0f, 1f)
        val scale = 1f + POP_SCALE * ease(pop)
        val countColour = if (now != null && since < FLASH_MILLIS) blend(Palette.ERROR, Palette.TEXT, since / FLASH_MILLIS) else Palette.TEXT
        val countWidth = font.width(count)

        graphics.pose().pushMatrix()
        graphics.pose().translate(x + countWidth / 2f, row + font.lineHeight / 2f)
        graphics.pose().scale(scale, scale)
        graphics.drawString(font, count, -countWidth / 2, -font.lineHeight / 2, countColour, true)
        graphics.pose().popMatrix()
        x += countWidth + 3

        graphics.drawString(font, label, x, row, Palette.MUTED, true)
        return x + font.width(label)
    }

    private fun statWidth(font: Font, count: String, label: String): Int =
        PIP + PIP_GAP + font.width(count) + 3 + font.width(label)

    private fun bar(graphics: GuiGraphics, from: Int, row: Int, to: Int, progress: Float, colour: Int) {
        rounded(graphics, from, row, to, row + BAR_HEIGHT, TRACK)
        val filled = from + ((to - from) * progress).toInt()
        if (filled <= from) return

        var x = from
        while (x < filled) {
            val next = minOf(filled, x + BAR_STEP)
            val towardsHead = (x - from).toFloat() / (filled - from).coerceAtLeast(1)
            graphics.fill(x, row, next, row + BAR_HEIGHT, blend(withAlpha(colour, 0x99), colour, towardsHead))
            x = next
        }
        graphics.fill(filled - 2, row - 1, filled + 2, row + BAR_HEIGHT + 1, HEAD_GLOW)
        graphics.fill(filled - 1, row - 1, filled, row + BAR_HEIGHT + 1, HEAD)
    }

    private fun hint(graphics: GuiGraphics, font: Font, key: Component, look: Look, left: Int, top: Int, now: Long) {
        val keyWidth = font.width(key) + 2 * KEY_PADDING
        val keyBottom = top + font.lineHeight + 3

        val fresh = 1f - ((now - readySince) / READY_MILLIS).coerceIn(0f, 1f)
        if (look.ready && fresh > 0f) {
            val beat = 0.5f + 0.5f * sin(now / 120.0).toFloat()
            rounded(graphics, left - 2, top - 3, left + keyWidth + 2, keyBottom + 2, withAlpha(look.colour, (0x90 * fresh * beat).toInt()))
        }

        rounded(graphics, left, top - 1, left + keyWidth, keyBottom, KEY_FACE)
        graphics.fill(left + 1, keyBottom - 1, left + keyWidth - 1, keyBottom, if (look.ready) look.colour else KEY_EDGE)
        graphics.drawString(font, key, left + KEY_PADDING, top + 1, Palette.TEXT, false)
        graphics.drawString(
            font, look.action, left + keyWidth + 5, top + 1,
            if (look.ready) Palette.TEXT else Palette.MUTED, true,
        )
    }

    private fun panel(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int) {
        graphics.fillGradient(left + 1, top, right - 1, bottom, PANEL_TOP, PANEL_BOTTOM)
        graphics.fillGradient(left, top + 1, left + 1, bottom - 1, PANEL_TOP, PANEL_BOTTOM)
        graphics.fillGradient(right - 1, top + 1, right, bottom - 1, PANEL_TOP, PANEL_BOTTOM)
    }

    private fun rounded(graphics: GuiGraphics, left: Int, top: Int, right: Int, bottom: Int, colour: Int) {
        graphics.fill(left + 1, top, right - 1, bottom, colour)
        graphics.fill(left, top + 1, left + 1, bottom - 1, colour)
        graphics.fill(right - 1, top + 1, right, bottom - 1, colour)
    }

    private fun spacedWidth(font: Font, text: String): Int =
        text.sumOf { font.width(it.toString()) } + LETTER_GAP * (text.length - 1).coerceAtLeast(0)

    private fun spaced(graphics: GuiGraphics, font: Font, text: String, atX: Int, atY: Int, colour: Int, shadow: Boolean = true) {
        var x = atX
        for (character in text) {
            val piece = character.toString()
            graphics.drawString(font, piece, x, atY, colour, shadow)
            x += font.width(piece) + LETTER_GAP
        }
    }

    private fun shimmer(graphics: GuiGraphics, font: Font, text: String, atX: Int, atY: Int, now: Long) {
        var x = atX
        val sweep = ((now % SHIMMER_MILLIS.toLong()) / SHIMMER_MILLIS).toFloat() * (text.length + 4) - 2f
        text.forEachIndexed { index, character ->
            val piece = character.toString()
            val shine = (1f - kotlin.math.abs(index - sweep) / 2f).coerceIn(0f, 1f)
            val base = blend(POINTS_FROM, POINTS_TO, index.toFloat() / (text.length - 1).coerceAtLeast(1))
            graphics.drawString(font, piece, x, atY, blend(base, HEAD, shine * 0.6f), true)
            x += font.width(piece)
        }
    }

    private fun withAlpha(colour: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (colour and 0xFFFFFF)

    private fun smoothed(now: Long): Float {
        val target = RoundState.progress
        val elapsed = if (drawnAt == 0L) 1f else ((now - drawnAt) / 1000f).coerceIn(0f, 1f)
        drawnAt = now
        shownProgress += (target - shownProgress) * (elapsed * 6f).coerceAtMost(1f)
        return shownProgress.coerceIn(0f, 1f)
    }

    private fun ease(value: Float): Float {
        val t = 1f - value.coerceIn(0f, 1f)
        return 1f - t * t * t
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

    private val GLOW_OFFSETS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
}
