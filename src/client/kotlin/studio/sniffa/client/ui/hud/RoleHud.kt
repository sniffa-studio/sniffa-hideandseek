package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import studio.sniffa.Hideandseek
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.theme.Palette
import kotlin.math.sin

object RoleHud {

    private const val MARGIN = 8
    private const val PADDING = 9
    private const val MIN_WIDTH = 156
    private const val HEADER = 11
    private const val CHIP_PADDING = 4
    private const val LETTER_GAP = 1
    private const val CLOCK_SCALE = 2f
    private const val CLOCK_HEIGHT = 16
    private const val BAR_HEIGHT = 3
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

    private const val PANEL_TOP = 0xE0122A20.toInt()
    private const val PANEL_BOTTOM = 0xE00A1424.toInt()
    private const val GOLD = 0xFFD9A93A.toInt()
    private const val GOLD_LIGHT = 0xFFFFE07A.toInt()
    private const val GOLD_DARK = 0xFF8A6418.toInt()
    private const val SNOW = 0xFFF2F6FF.toInt()
    private const val SNOW_SHADE = 0xFFC9D8EE.toInt()
    private const val ICE = 0xFFA9DEFF.toInt()
    private const val FROST_GLOW = 0xFF8FD3FF.toInt()
    private const val CANE_RED = 0xFFC8102E.toInt()
    private const val WIRE = 0xFF1B3A24.toInt()
    private const val BULB_OFF = 0xFF2E3A36.toInt()
    private const val BULB_GAP = 7
    private const val SNOW_CAP = 3
    private const val EFFECTS_GAP = 4

    private val BULBS = intArrayOf(0xFFC8102E.toInt(), 0xFFE8B33A.toInt(), 0xFF2FA85A.toInt(), 0xFF3D9CFF.toInt())

    private val GIFT = arrayOf(
        "..g.g..",
        "...g...",
        "ggggggg",
        "BBBgBBB",
        "BBBgBBB",
        "bbbgbbb",
        "bbbgbbb",
    )

    private val HAT = arrayOf(
        "....RRw",
        "...RRR.",
        "..RRRR.",
        ".RRRRrr",
        "RRRRRrr",
        "WWWWWWW",
        "SSSSSSS",
    )

    private val FLAKE = arrayOf(
        "..w..",
        "w.w.w",
        ".www.",
        "w.w.w",
        "..w..",
    )

    private val HOLLY = arrayOf(
        "LL.....LL",
        "LLL...LLL",
        ".LLLrLLL.",
        "..LrRrL..",
        "...rRr...",
    )

    private val PIXELS = mapOf(
        'g' to 0xFFE8B33A.toInt(), 'B' to 0xFF2FA85A.toInt(), 'b' to 0xFF1E7A3F.toInt(),
        'R' to 0xFFC8102E.toInt(), 'r' to 0xFF8E0B20.toInt(), 'w' to 0xFFF2F6FF.toInt(),
        'W' to 0xFFF2F6FF.toInt(), 'S' to 0xFFC9D8EE.toInt(), 'L' to 0xFF2E8B57.toInt(),
    )

    private var effectsBelow = 0
    private const val SHADOW_COLOUR = 0x50000000
    private const val HIGHLIGHT = 0x1CFFFFFF
    private const val CHIP_TEXT = 0xFF0B100D.toInt()
    private const val HEAD = 0xFFFFFFFF.toInt()
    private const val GLOW_ALPHA = 0x38
    private const val KEY_FACE = 0xE6202624.toInt()
    private const val POINTS_FROM = 0xFFE8B33A.toInt()
    private const val POINTS_TO = 0xFFFFF1B0.toInt()
    private const val POINTS_LABEL = " ᴘᴋᴛ"

    private const val HIDERS = "ᴠᴇʀꜱᴛᴇᴄᴋᴛ"
    private const val SEEKERS = "ꜱᴜᴄʜᴇʀ"

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
        HudElementRegistry.addLast(Hideandseek.id("role_hud")) { graphics, _ ->
            val minecraft = Minecraft.getInstance()

            if (minecraft.gui.hud.isHidden || minecraft.debugOverlay.showDebugScreen()) return@addLast
            if (!RoundState.running) {
                lastRemaining = -1
                runningSince = 0L
                wasReady = false
                effectsBelow = 0
                return@addLast
            }

            draw(graphics, minecraft.font, look())
        }
    }

    fun effectsOffset(): Int = effectsBelow

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

    private fun glowColour(): Int = when {
        RoundState.clock == RoundState.Clock.PAUSED -> Palette.WARNING
        finalCountdown() -> Palette.ERROR
        else -> FROST_GLOW
    }

    private fun draw(graphics: GuiGraphicsExtractor, font: Font, look: Look) {
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

        val pointsWidth = RoundState.points?.let { STATS_GAP + 7 + font.width(it.toString()) + font.width(POINTS_LABEL) } ?: 0
        val statsWidth = statWidth(font, remaining.toString(), HIDERS) + STATS_GAP +
            statWidth(font, RoundState.seekers.toString(), SEEKERS) + pointsWidth
        val width = maxOf(
            MIN_WIDTH,
            PADDING * 2 + spacedWidth(font, look.title) + 12 + chipWidth,
            PADDING * 2 + statsWidth,
        )
        val height = PADDING + HEADER + 7 + CLOCK_HEIGHT + 7 + BAR_HEIGHT + 9 + font.lineHeight + PADDING - 1

        val screenRight = graphics.guiWidth() - MARGIN
        val slide = ease(((now - runningSince) / SLIDE_MILLIS).coerceIn(0f, 1f))
        val right = screenRight + ((width + MARGIN) * (1f - slide)).toInt()
        val left = right - width
        val top = MARGIN + SNOW_CAP
        val textLeft = left + PADDING
        val textRight = right - PADDING

        rounded(graphics, left + SHADOW, top + SHADOW, right + SHADOW, top + height + SHADOW, SHADOW_COLOUR)
        rounded(graphics, left - 1, top - 1, right + 1, top + height + 1, GOLD_DARK)
        rounded(graphics, left, top, right, top + height, GOLD)
        panel(graphics, left + 1, top + 1, right - 1, top + height - 1)
        graphics.fill(left + 2, top + 1, right - 2, top + 2, HIGHLIGHT)
        snowCap(graphics, left, right, top)
        sprite(graphics, HOLLY, left - 2, top - 4)

        var row = top + PADDING

        spaced(graphics, font, look.title, textLeft, row + 1, look.colour)
        val chipLeft = textRight - chipWidth
        candyChip(graphics, chipLeft, row - 1, textRight, row + HEADER - 1, phaseColour)
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
        val glow = withAlpha(glowColour(), (GLOW_ALPHA * 2 * (1f + 2f * tick)).toInt().coerceAtMost(0xFF))
        for ((dx, dy) in GLOW_OFFSETS) {
            graphics.text(font, clock, dx, dy, glow, false)
        }
        graphics.text(font, clock, 0, 0, colour, true)
        graphics.pose().popMatrix()

        val label = clockLabel()
        graphics.text(font, label, textRight - font.width(label), row + CLOCK_HEIGHT - font.lineHeight, Palette.MUTED, true)
        row += CLOCK_HEIGHT + 7

        garland(graphics, textLeft, row, textRight, smoothed(now), now)
        row += BAR_HEIGHT + 9

        var x = textLeft
        x = stat(graphics, font, x, row, GIFT, remaining.toString(), HIDERS, now)
        x += STATS_GAP
        stat(graphics, font, x, row, HAT, RoundState.seekers.toString(), SEEKERS, null)

        RoundState.points?.let {
            val points = it.toString()
            val pointsLeft = textRight - font.width(POINTS_LABEL) - font.width(points)
            sprite(graphics, FLAKE, pointsLeft - 8, row + 1)
            shimmer(graphics, font, points, pointsLeft, row, now)
            graphics.text(font, POINTS_LABEL, textRight - font.width(POINTS_LABEL), row, Palette.MUTED, true)
        }

        val hintTop = top + height + HINT_GAP + 1
        effectsBelow = if (look.key != null) hintTop + font.lineHeight + 4 + EFFECTS_GAP else top + height + EFFECTS_GAP
        look.key?.let { key -> hint(graphics, font, key, look, right, hintTop, now) }
    }

    private fun stat(graphics: GuiGraphicsExtractor, font: Font, atX: Int, row: Int, icon: Array<String>, count: String, label: String, now: Long?): Int {
        var x = atX
        sprite(graphics, icon, x, row)
        x += icon[0].length + PIP_GAP

        val since = now?.let { (it - remainingChangedAt).toFloat() } ?: Float.MAX_VALUE
        val pop = 1f - (since / POP_MILLIS).coerceIn(0f, 1f)
        val scale = 1f + POP_SCALE * ease(pop)
        val countColour = if (now != null && since < FLASH_MILLIS) blend(Palette.ERROR, Palette.TEXT, since / FLASH_MILLIS) else Palette.TEXT
        val countWidth = font.width(count)

        graphics.pose().pushMatrix()
        graphics.pose().translate(x + countWidth / 2f, row + font.lineHeight / 2f)
        graphics.pose().scale(scale, scale)
        graphics.text(font, count, -countWidth / 2, -font.lineHeight / 2, countColour, true)
        graphics.pose().popMatrix()
        x += countWidth + 3

        graphics.text(font, label, x, row, Palette.MUTED, true)
        return x + font.width(label)
    }

    private fun statWidth(font: Font, count: String, label: String): Int =
        7 + PIP_GAP + font.width(count) + 3 + font.width(label)

    private fun garland(graphics: GuiGraphicsExtractor, from: Int, row: Int, to: Int, progress: Float, now: Long) {
        graphics.fill(from, row, to, row + 1, WIRE)
        val lit = from + ((to - from) * progress).toInt()
        var x = from + 2
        var index = 0
        var last = -1
        while (x < to - 1) {
            val on = x <= lit
            val colour = BULBS[index % BULBS.size]
            if (on) {
                graphics.fill(x - 1, row, x + 3, row + 5, withAlpha(colour, 0x40))
                graphics.fill(x, row + 1, x + 2, row + 4, colour)
                graphics.fill(x, row + 1, x + 1, row + 2, 0xC0FFFFFF.toInt())
                last = x
            } else {
                graphics.fill(x, row + 1, x + 2, row + 4, BULB_OFF)
            }
            x += BULB_GAP
            index++
        }
        if (last >= 0) {
            val beat = 0.5f + 0.5f * sin(now / 160.0).toFloat()
            graphics.fill(last - 2, row - 1, last + 4, row + 6, withAlpha(HEAD, (0x30 + 0x50 * beat).toInt()))
        }
    }

    private fun snowCap(graphics: GuiGraphicsExtractor, left: Int, right: Int, top: Int) {
        graphics.fill(left, top - 1, right, top + 2, SNOW)
        graphics.fill(left + 1, top + 2, right - 1, top + 3, SNOW_SHADE)
        var x = left
        while (x < right) {
            val bump = (x * 7 + 3) % 4
            if (bump > 0) graphics.fill(x, top - 1 - bump.coerceAtMost(SNOW_CAP), minOf(x + 3, right), top - 1, SNOW)
            if ((x * 13) % 11 == 0 && x > left + 3 && x < right - 4) {
                graphics.fill(x, top + 3, x + 1, top + 4 + (x % 3), ICE)
            }
            x += 3
        }
    }

    private fun candyChip(graphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int, colour: Int) {
        rounded(graphics, left - 1, top - 1, right + 1, bottom + 1, SNOW)
        var x = left - 1
        while (x < right + 1) {
            if (((x - left) / 2) % 2 == 0) {
                graphics.fill(x, top - 1, minOf(x + 2, right + 1), top, CANE_RED)
                graphics.fill(x, bottom, minOf(x + 2, right + 1), bottom + 1, CANE_RED)
            }
            x += 2
        }
        rounded(graphics, left, top, right, bottom, colour)
    }

    private fun sprite(graphics: GuiGraphicsExtractor, rows: Array<String>, atX: Int, atY: Int) {
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, pixel ->
                PIXELS[pixel]?.let { graphics.fill(atX + x, atY + y, atX + x + 1, atY + y + 1, it) }
            }
        }
    }

    private fun hint(graphics: GuiGraphicsExtractor, font: Font, key: Component, look: Look, right: Int, top: Int, now: Long) {
        val keyWidth = font.width(key) + 2 * KEY_PADDING
        val keyBottom = top + font.lineHeight + 3
        val keyLeft = right - keyWidth

        val fresh = 1f - ((now - readySince) / READY_MILLIS).coerceIn(0f, 1f)
        if (look.ready && fresh > 0f) {
            val beat = 0.5f + 0.5f * sin(now / 120.0).toFloat()
            rounded(graphics, keyLeft - 2, top - 3, right + 2, keyBottom + 2, withAlpha(GOLD_LIGHT, (0x90 * fresh * beat).toInt()))
        }

        rounded(graphics, keyLeft, top - 1, right, keyBottom, if (look.ready) CANE_RED else KEY_FACE)
        graphics.fill(keyLeft + keyWidth / 2 - 1, top - 1, keyLeft + keyWidth / 2 + 1, keyBottom, withAlpha(GOLD, if (look.ready) 0xFF else 0x55))
        graphics.text(font, key, keyLeft + KEY_PADDING, top + 1, Palette.TEXT, true)
        graphics.text(
            font, look.action, keyLeft - 5 - font.width(look.action), top + 1,
            if (look.ready) Palette.TEXT else Palette.MUTED, true,
        )
    }

    private fun panel(graphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int) {
        graphics.fillGradient(left + 1, top, right - 1, bottom, PANEL_TOP, PANEL_BOTTOM)
        graphics.fillGradient(left, top + 1, left + 1, bottom - 1, PANEL_TOP, PANEL_BOTTOM)
        graphics.fillGradient(right - 1, top + 1, right, bottom - 1, PANEL_TOP, PANEL_BOTTOM)
    }

    private fun rounded(graphics: GuiGraphicsExtractor, left: Int, top: Int, right: Int, bottom: Int, colour: Int) {
        graphics.fill(left + 1, top, right - 1, bottom, colour)
        graphics.fill(left, top + 1, left + 1, bottom - 1, colour)
        graphics.fill(right - 1, top + 1, right, bottom - 1, colour)
    }

    private fun spacedWidth(font: Font, text: String): Int =
        text.sumOf { font.width(it.toString()) } + LETTER_GAP * (text.length - 1).coerceAtLeast(0)

    private fun spaced(graphics: GuiGraphicsExtractor, font: Font, text: String, atX: Int, atY: Int, colour: Int, shadow: Boolean = true) {
        var x = atX
        for (character in text) {
            val piece = character.toString()
            graphics.text(font, piece, x, atY, colour, shadow)
            x += font.width(piece) + LETTER_GAP
        }
    }

    private fun shimmer(graphics: GuiGraphicsExtractor, font: Font, text: String, atX: Int, atY: Int, now: Long) {
        var x = atX
        val sweep = ((now % SHIMMER_MILLIS.toLong()) / SHIMMER_MILLIS).toFloat() * (text.length + 4) - 2f
        text.forEachIndexed { index, character ->
            val piece = character.toString()
            val shine = (1f - kotlin.math.abs(index - sweep) / 2f).coerceIn(0f, 1f)
            val base = blend(POINTS_FROM, POINTS_TO, index.toFloat() / (text.length - 1).coerceAtLeast(1))
            graphics.text(font, piece, x, atY, blend(base, HEAD, shine * 0.6f), true)
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
