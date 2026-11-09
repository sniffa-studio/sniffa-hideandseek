package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import studio.sniffa.Hideandseek
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.zone.DangerZone
import studio.sniffa.client.zone.ZoneStorm
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object ZoneWarning {

    private const val FROM_BOTTOM = 96

    private const val FROST = 0xFFE4F1FF.toInt()
    private const val ICE = 0xFF9FD4FF.toInt()
    private const val WHITE = 0xFFF4F8FF.toInt()
    private const val MUTED = 0xFFB8C8DC.toInt()

    private const val FROST_STRENGTH = 0.55f
    private const val URGENT_FROST = 0.8f
    private const val PULSE_DEPTH = 0.2f
    private const val CALM_PERIOD_MILLIS = 2200.0
    private const val URGENT_PERIOD_MILLIS = 600.0
    private const val URGENT_SECONDS = 10

    private const val FROST_STEPS = 18
    private const val FROST_REACH = 0.32f

    private const val STREAKS = 70
    private const val WIND_ANGLE = 0.3f
    private const val SLOWEST = 260f
    private const val FASTEST = 560f
    private const val SHORTEST = 10f
    private const val LONGEST = 34f
    private const val FAINTEST = 0.12f
    private const val BRIGHTEST = 0.5f
    private const val URGENT_RUSH = 1.5f

    private class Streak(var x: Float, var y: Float, var length: Float, var speed: Float, var alpha: Float)

    private val streaks = ArrayList<Streak>()
    private val random = Random(7)
    private var lastFrame = 0L

    fun install() {
        HudElementRegistry.addLast(Hideandseek.id("zone_warning")) { graphics, _ ->
            val minecraft = Minecraft.getInstance()
            if (minecraft.gui.hud.isHidden) return@addLast

            val presence = ZoneStorm.presence
            if (presence <= 0f) {
                lastFrame = 0L
                return@addLast
            }

            val secondsLeft = DangerZone.secondsLeft()
            storm(graphics, presence, secondsLeft)

            if (RoundState.role == RoundState.Role.HIDER && DangerZone.caughtInside()) {
                words(graphics, secondsLeft)
            }
        }
    }

    private fun storm(graphics: GuiGraphicsExtractor, presence: Float, secondsLeft: Int) {
        val now = System.currentTimeMillis()
        val urgent = secondsLeft <= URGENT_SECONDS
        val period = if (urgent) URGENT_PERIOD_MILLIS else CALM_PERIOD_MILLIS
        val beat = (0.5 + 0.5 * sin(now / period * 2.0 * PI)).toFloat()
        val frost = (if (urgent) URGENT_FROST else FROST_STRENGTH) * (1f - PULSE_DEPTH + PULSE_DEPTH * beat)
        frostFrame(graphics, frost * presence)

        val width = graphics.guiWidth().toFloat()
        val height = graphics.guiHeight().toFloat()
        val seconds = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1000f).coerceAtMost(0.1f)
        lastFrame = now

        if (streaks.isEmpty()) {
            repeat(STREAKS) { streaks.add(fresh(width, height, scattered = true)) }
        }

        val rush = if (urgent) URGENT_RUSH else 1f
        val stepX = kotlin.math.cos(WIND_ANGLE)
        val stepY = kotlin.math.sin(WIND_ANGLE)

        for (streak in streaks) {
            streak.x += stepX * streak.speed * rush * seconds
            streak.y += stepY * streak.speed * rush * seconds
            if (streak.x > width + LONGEST || streak.y > height + LONGEST) {
                val again = fresh(width, height, scattered = false)
                streak.x = again.x; streak.y = again.y
                streak.length = again.length; streak.speed = again.speed; streak.alpha = again.alpha
            }

            val alpha = (streak.alpha * presence * 255f).toInt().coerceIn(0, 255)
            if (alpha == 0) continue
            graphics.pose().pushMatrix()
            graphics.pose().translate(streak.x, streak.y)
            graphics.pose().rotate(WIND_ANGLE)
            graphics.fill(0, 0, streak.length.toInt(), 1, (alpha shl 24) or (WHITE and 0xFFFFFF))
            graphics.pose().popMatrix()
        }
    }

    private fun frostFrame(graphics: GuiGraphicsExtractor, strength: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val reach = (minOf(width, height) * FROST_REACH).toInt().coerceAtLeast(FROST_STEPS)
        val edge = strength.coerceIn(0f, 0.95f)
        val each = 1f - Math.pow((1f - edge).toDouble(), 1.0 / FROST_STEPS).toFloat()
        val colour = ((each * 255f).toInt().coerceIn(1, 255) shl 24) or (FROST and 0xFFFFFF)

        var inner = 0
        for (step in 1..FROST_STEPS) {
            val depth = (reach * step.toFloat() / FROST_STEPS * (step.toFloat() / FROST_STEPS)).toInt().coerceAtLeast(1)
            if (depth <= inner) continue
            graphics.fill(0, 0, width, depth, colour)
            graphics.fill(0, height - depth, width, height, colour)
            graphics.fill(0, depth, depth, height - depth, colour)
            graphics.fill(width - depth, depth, width, height - depth, colour)
            inner = depth
        }
    }

    private fun fresh(width: Float, height: Float, scattered: Boolean): Streak {
        val length = SHORTEST + random.nextFloat() * (LONGEST - SHORTEST)
        val near = random.nextFloat()
        val x = if (scattered) random.nextFloat() * width else -length - random.nextFloat() * width * 0.4f
        val y = random.nextFloat() * (height + 40f) - 60f
        return Streak(
            x, y, length * (0.6f + 0.6f * near),
            SLOWEST + (FASTEST - SLOWEST) * near,
            FAINTEST + (BRIGHTEST - FAINTEST) * near,
        )
    }

    private fun words(graphics: GuiGraphicsExtractor, secondsLeft: Int) {
        val font = Minecraft.getInstance().font
        val urgent = secondsLeft <= URGENT_SECONDS
        val flicker = urgent && (System.currentTimeMillis() / 250L) % 2L == 0L

        val headline = "ꜱᴄʜɴᴇᴇꜱᴛᴜʀᴍ"
        val detail = "Raus aus dem Sturm  ·  ${clock(secondsLeft)}"
        val y = graphics.guiHeight() - FROM_BOTTOM

        graphics.text(font, headline, (graphics.guiWidth() - font.width(headline)) / 2, y, if (flicker) WHITE else ICE, true)
        graphics.text(font, detail, (graphics.guiWidth() - font.width(detail)) / 2, y + 12, if (urgent) WHITE else MUTED, true)
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
