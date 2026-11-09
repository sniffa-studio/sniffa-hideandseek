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

    private val GIFT: Identifier = Hideandseek.id("hud/gift")
    private val HAT: Identifier = Hideandseek.id("hud/hat")
    private val STAR: Identifier = Hideandseek.id("hud/star")

    private const val ICON = 9

    private const val MARGIN = 8
    private const val LETTER_GAP = 1
    private const val CLOCK_SCALE = 2f
    private const val CLOCK_HEIGHT = 16
    private const val LINE_WIDTH = 72
    private const val LINE_HEIGHT = 1
    private const val SLIDE_OFFSET = 40
    private const val STAT_GAP = 12
    private const val ICON_GAP = 3
    private const val EFFECTS_GAP = 4

    private const val SLIDE_MILLIS = 380f
    private const val POP_MILLIS = 450f
    private const val POP_SCALE = 0.45f
    private const val FLASH_MILLIS = 1_200L
    private const val TICK_MILLIS = 650f
    private const val READY_MILLIS = 2_400f
    private const val FINAL_SECONDS = 10

    private const val TITLE = 0xFFEAF4FF.toInt()
    private const val TRACK = 0x30FFFFFF
    private const val GOLD = 0xFFF2C65A.toInt()

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
        val fade = ease(((now - runningSince) / SLIDE_MILLIS).coerceIn(0f, 1f))
        val right = graphics.guiWidth() - MARGIN + ((1f - fade) * SLIDE_OFFSET).toInt()
        var row = MARGIN

        val separator = "  ·  "
        val headWidth = spacedWidth(font, look.title) + font.width(separator) + spacedWidth(font, phase)
        var x = right - headWidth
        x = spaced(graphics, font, look.title, x, row, look.colour)
        graphics.text(font, separator, x, row, Palette.MUTED, true)
        x += font.width(separator)
        spaced(graphics, font, phase, x, row, phaseColour)
        row += font.lineHeight + 3

        val tick = if (finalCountdown()) 1f - ((now - secondChangedAt) / TICK_MILLIS).coerceIn(0f, 1f) else 0f
        val clock = clock(RoundState.clockSeconds)
        val clockScale = CLOCK_SCALE * (1f + 0.12f * ease(tick))
        val clockWidth = font.width(clock) * clockScale
        graphics.pose().pushMatrix()
        graphics.pose().translate(right - clockWidth, row.toFloat() - CLOCK_HEIGHT * (clockScale / CLOCK_SCALE - 1f) / 2f)
        graphics.pose().scale(clockScale, clockScale)
        graphics.text(font, clock, 0, 0, clockColour(), true)
        graphics.pose().popMatrix()

        val label = clockLabel()
        graphics.text(font, label, right - clockWidth.toInt() - 4 - font.width(label), row + CLOCK_HEIGHT - font.lineHeight - 1, Palette.MUTED, true)
        row += CLOCK_HEIGHT + 4

        line(graphics, right - LINE_WIDTH, row, right, smoothed(now))
        row += LINE_HEIGHT + 5

        val points = RoundState.points?.toString()
        val statsWidth = statWidth(font, remaining.toString()) + STAT_GAP + statWidth(font, RoundState.seekers.toString()) +
            (points?.let { STAT_GAP + statWidth(font, it) } ?: 0)
        x = right - statsWidth
        x = stat(graphics, font, x, row, GIFT, remaining.toString(), now)
        x += STAT_GAP
        x = stat(graphics, font, x, row, HAT, RoundState.seekers.toString(), null)
        if (points != null) {
            x += STAT_GAP
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, STAR, x, row, ICON, ICON)
            graphics.text(font, points, x + ICON + ICON_GAP, row + 1, GOLD, true)
        }
        row += ICON + 6

        effectsBelow = row + (if (look.key != null) font.lineHeight + 4 else 0) + EFFECTS_GAP
        look.key?.let { key -> hint(graphics, font, key, look, right, row, now) }
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

    private fun line(graphics: GuiGraphicsExtractor, from: Int, row: Int, to: Int, progress: Float) {
        graphics.fill(from, row, to, row + LINE_HEIGHT, TRACK)
        val filled = to - ((to - from) * progress).toInt()
        if (filled < to) graphics.fill(filled, row, to, row + LINE_HEIGHT, TITLE)
    }

    private fun hint(graphics: GuiGraphicsExtractor, font: Font, key: Component, look: Look, right: Int, top: Int, now: Long) {
        val fresh = 1f - ((now - readySince) / READY_MILLIS).coerceIn(0f, 1f)
        val keyColour = when {
            look.ready && fresh > 0f -> blend(TITLE, GOLD, (0.5f + 0.5f * sin(now / 120.0).toFloat()) * fresh)
            look.ready -> TITLE
            else -> Palette.MUTED
        }
        val keyText = "[" + key.string + "]"
        val keyWidth = font.width(keyText)
        graphics.text(font, keyText, right - keyWidth, top, keyColour, true)
        graphics.text(font, look.action, right - keyWidth - 4 - font.width(look.action), top, Palette.MUTED, true)
    }

    private fun spacedWidth(font: Font, text: String): Int =
        text.sumOf { font.width(it.toString()) } + LETTER_GAP * (text.length - 1).coerceAtLeast(0)

    private fun spaced(graphics: GuiGraphicsExtractor, font: Font, text: String, atX: Int, atY: Int, colour: Int, shadow: Boolean = true): Int {
        var x = atX
        for (character in text) {
            val piece = character.toString()
            graphics.text(font, piece, x, atY, colour, shadow)
            x += font.width(piece) + LETTER_GAP
        }
        return x - LETTER_GAP
    }

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
