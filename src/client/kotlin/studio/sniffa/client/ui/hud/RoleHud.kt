package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek
import studio.sniffa.client.input.Keybindings
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.theme.Palette
import kotlin.math.sin

object RoleHud {

    private val PANEL: Identifier = Hideandseek.id("hud/xmas_panel")
    private val GIFT: Identifier = Hideandseek.id("hud/gift")
    private val HAT: Identifier = Hideandseek.id("hud/hat")
    private val STAR: Identifier = Hideandseek.id("hud/star")
    private val FLAKE: Identifier = Hideandseek.id("hud/flake")

    private const val ICON = 9
    private const val SNOW_ABOVE = 7

    private const val MARGIN = 8
    private const val PADDING = 9
    private const val MIN_WIDTH = 150
    private const val HEADER = 11
    private const val PILL_PADDING = 5
    private const val LETTER_GAP = 1
    private const val CLOCK_SCALE = 2f
    private const val CLOCK_HEIGHT = 16
    private const val BAR_HEIGHT = 2
    private const val ROW_GAP = 7
    private const val STAT_GAP = 12
    private const val ICON_GAP = 3
    private const val HINT_GAP = 6
    private const val KEY_PADDING = 4
    private const val EFFECTS_GAP = 4

    private const val SLIDE_MILLIS = 380f
    private const val POP_MILLIS = 450f
    private const val POP_SCALE = 0.45f
    private const val FLASH_MILLIS = 1_200L
    private const val TICK_MILLIS = 650f
    private const val READY_MILLIS = 2_400f
    private const val SHIMMER_MILLIS = 2_200.0
    private const val FINAL_SECONDS = 10

    private const val TITLE = 0xFFEAF4FF.toInt()
    private const val PILL_FACE = 0xC0101A2E.toInt()
    private const val TRACK = 0x30FFFFFF
    private const val BAR_FROM = 0xFF5FA8E8.toInt()
    private const val BAR_TO = 0xFFF2F8FF.toInt()
    private const val HEAD = 0xFFFFFFFF.toInt()
    private const val KEY_FACE = 0xD0101A2E.toInt()
    private const val KEY_EDGE = 0xFF5F8FC0.toInt()
    private const val GOLD = 0xFFF2C65A.toInt()
    private const val POINTS_FROM = 0xFFE8B33A.toInt()
    private const val POINTS_TO = 0xFFFFF1B0.toInt()

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
    private var effectsBelow = 0

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
        else -> TITLE
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
        val pillWidth = spacedWidth(font, phase) + 2 * PILL_PADDING
        val titleWidth = 5 + spacedWidth(font, look.title)

        val points = RoundState.points?.toString()
        val statsWidth = statWidth(font, remaining.toString()) + STAT_GAP + statWidth(font, RoundState.seekers.toString()) +
            (points?.let { STAT_GAP + statWidth(font, it) } ?: 0)

        val width = maxOf(MIN_WIDTH, PADDING * 2 + titleWidth + 10 + pillWidth, PADDING * 2 + statsWidth)
        val body = PADDING + HEADER + ROW_GAP + CLOCK_HEIGHT + ROW_GAP + BAR_HEIGHT + ROW_GAP + ICON + PADDING

        val slide = ease(((now - runningSince) / SLIDE_MILLIS).coerceIn(0f, 1f))
        val right = graphics.guiWidth() - MARGIN + ((width + MARGIN) * (1f - slide)).toInt()
        val left = right - width
        val top = MARGIN + SNOW_ABOVE
        val textLeft = left + PADDING
        val textRight = right - PADDING

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, left, top - SNOW_ABOVE, width, body + SNOW_ABOVE)

        var row = top + PADDING

        graphics.fill(textLeft, row + 2, textLeft + 3, row + 5, look.colour)
        spaced(graphics, font, look.title, textLeft + 5, row, TITLE)
        pill(graphics, font, phase, phaseColour, textRight - pillWidth, row - 2, textRight, row + HEADER - 1)
        row += HEADER + ROW_GAP

        val tick = if (finalCountdown()) 1f - ((now - secondChangedAt) / TICK_MILLIS).coerceIn(0f, 1f) else 0f
        val clock = clock(RoundState.clockSeconds)
        val clockScale = CLOCK_SCALE * (1f + 0.12f * ease(tick))
        val lift = CLOCK_HEIGHT * (clockScale / CLOCK_SCALE - 1f) / 2f
        graphics.pose().pushMatrix()
        graphics.pose().translate(textLeft.toFloat(), row - lift)
        graphics.pose().scale(clockScale, clockScale)
        graphics.text(font, clock, 0, 0, clockColour(), true)
        graphics.pose().popMatrix()

        val label = clockLabel()
        graphics.text(font, label, textRight - font.width(label), row + CLOCK_HEIGHT - font.lineHeight, Palette.MUTED, false)
        row += CLOCK_HEIGHT + ROW_GAP

        bar(graphics, textLeft, row, textRight, smoothed(now), now)
        row += BAR_HEIGHT + ROW_GAP

        var x = textLeft
        x = stat(graphics, font, x, row, GIFT, remaining.toString(), now)
        x += STAT_GAP
        stat(graphics, font, x, row, HAT, RoundState.seekers.toString(), null)

        if (points != null) {
            val pointsLeft = textRight - statWidth(font, points)
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, STAR, pointsLeft, row, ICON, ICON)
            shimmer(graphics, font, points, pointsLeft + ICON + ICON_GAP, row + 1, now)
        }

        val hintTop = top + body + HINT_GAP
        effectsBelow = if (look.key != null) hintTop + font.lineHeight + 4 + EFFECTS_GAP else top + body + EFFECTS_GAP
        look.key?.let { key -> hint(graphics, font, key, look, right - 2, hintTop, now) }
    }

    private fun pill(graphics: GuiGraphicsExtractor, font: Font, text: String, colour: Int, left: Int, top: Int, right: Int, bottom: Int) {
        rounded(graphics, left, top, right, bottom, withAlpha(colour, 0x70))
        rounded(graphics, left + 1, top + 1, right - 1, bottom - 1, PILL_FACE)
        spaced(graphics, font, text, left + PILL_PADDING, top + 2, colour, shadow = false)
    }

    private fun stat(graphics: GuiGraphicsExtractor, font: Font, atX: Int, row: Int, icon: Identifier, count: String, now: Long?): Int {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, atX, row, ICON, ICON)
        val x = atX + ICON + ICON_GAP

        val since = now?.let { (it - remainingChangedAt).toFloat() } ?: Float.MAX_VALUE
        val pop = 1f - (since / POP_MILLIS).coerceIn(0f, 1f)
        val scale = 1f + POP_SCALE * ease(pop)
        val colour = if (now != null && since < FLASH_MILLIS) blend(Palette.ERROR, TITLE, since / FLASH_MILLIS) else TITLE
        val countWidth = font.width(count)

        graphics.pose().pushMatrix()
        graphics.pose().translate(x + countWidth / 2f, row + 1 + font.lineHeight / 2f)
        graphics.pose().scale(scale, scale)
        graphics.text(font, count, -countWidth / 2, -font.lineHeight / 2, colour, true)
        graphics.pose().popMatrix()
        return x + countWidth
    }

    private fun statWidth(font: Font, count: String): Int = ICON + ICON_GAP + font.width(count)

    private fun bar(graphics: GuiGraphicsExtractor, from: Int, row: Int, to: Int, progress: Float, now: Long) {
        graphics.fill(from, row, to, row + BAR_HEIGHT, TRACK)
        val filled = from + ((to - from) * progress).toInt()
        if (filled <= from) return

        var x = from
        while (x < filled) {
            val next = minOf(filled, x + 2)
            graphics.fill(x, row, next, row + BAR_HEIGHT, blend(BAR_FROM, BAR_TO, (x - from).toFloat() / (filled - from).coerceAtLeast(1)))
            x = next
        }
        val beat = 0.5f + 0.5f * sin(now / 180.0).toFloat()
        graphics.fill(filled - 3, row - 2, filled + 3, row + BAR_HEIGHT + 2, withAlpha(HEAD, (0x18 + 0x30 * beat).toInt()))
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAKE, filled - 4, row - 3, 8, 8)
    }

    private fun hint(graphics: GuiGraphicsExtractor, font: Font, key: Component, look: Look, right: Int, top: Int, now: Long) {
        val keyWidth = font.width(key) + 2 * KEY_PADDING
        val keyBottom = top + font.lineHeight + 3
        val keyLeft = right - keyWidth

        val fresh = 1f - ((now - readySince) / READY_MILLIS).coerceIn(0f, 1f)
        val edge = when {
            look.ready && fresh > 0f -> blend(KEY_EDGE, GOLD, (0.5f + 0.5f * sin(now / 120.0).toFloat()) * fresh)
            look.ready -> KEY_EDGE
            else -> withAlpha(KEY_EDGE, 0x60)
        }

        rounded(graphics, keyLeft - 1, top - 2, right + 1, keyBottom + 1, edge)
        rounded(graphics, keyLeft, top - 1, right, keyBottom, KEY_FACE)
        graphics.text(font, key, keyLeft + KEY_PADDING, top + 1, if (look.ready) TITLE else Palette.MUTED, false)
        graphics.text(
            font, look.action, keyLeft - 5 - font.width(look.action), top + 1,
            if (look.ready) TITLE else Palette.MUTED, true,
        )
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
}
