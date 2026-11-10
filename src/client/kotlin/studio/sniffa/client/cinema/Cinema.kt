package studio.sniffa.client.cinema

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.phys.Vec3
import studio.sniffa.client.ui.hud.Vignette
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.net.payload.CinemaPayload
import studio.sniffa.net.payload.IntroPayload
import studio.sniffa.net.payload.MomentPayload
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Cinema {

    private const val STEP_SPACING = 1.75
    private val HEIGHTS = doubleArrayOf(1.598, 1.148, 0.798)
    private val PLACE_COLOURS = intArrayOf(0xFFF2C65A.toInt(), 0xFFDDE6EF.toInt(), 0xFFD9884A.toInt())
    private const val SEEKER_RED = 0xFFE8394A.toInt()

    private const val TICK_MILLIS = 50L
    private const val LIFT_MILLIS = 1500L
    private const val LEAVE_BEFORE_REVEAL_MILLIS = 350L
    private const val ARRIVE_AFTER_REVEAL_MILLIS = 650L
    private const val HOLD_WINNER_MILLIS = 2600L
    private const val PULL_BACK_MILLIS = 1200L
    private const val RETURN_MILLIS = 1500L
    private const val BARS_MILLIS = 600f

    private const val BAR_SHARE = 0.12f
    private const val MOMENT_BAR_SHARE = 0.08f
    private const val CAPTION_SCALE = 2f
    private const val CAPTION_FADE_MILLIS = 300f
    private const val CEREMONY = "❄ ꜱɪᴇɢᴇʀᴇʜʀᴜɴɢ ❄"
    private const val ROUND_OPENS = "ʀᴜɴᴅᴇ ʙᴇɢɪɴɴᴛ"
    private const val SEEKS = "ꜱᴜᴄʜᴛ ᴇᴜᴄʜ"

    private const val WIDE_BACK = 10.5
    private const val WIDE_UP = 3.2
    private const val WIDE_LOOK_UP = 2.7
    private const val CLOSE_BACK = 3.4
    private const val CLOSE_UP = 1.8
    private const val CLOSE_LOOK_UP = 1.4

    private const val ORBIT_RADIUS = 45.0
    private const val ORBIT_UP = 18.0
    private const val ORBIT_LOOK_DOWN = 45.0
    private const val ORBIT_DEGREES = 200.0
    private const val ORBIT_FROM_MILLIS = 1200L
    private const val ORBIT_MILLIS = 4200L
    private const val ORBIT_STEP_MILLIS = 100L
    private const val TO_SEEKER_MILLIS = 900L
    private const val SEEKER_BACK = 4.6
    private const val SEEKER_UP = 0.85
    private const val SEEKER_CLOSE_BACK = 2.9
    private const val SEEKER_CLOSE_UP = 1.05
    private const val SEEKER_LOOK_UP = 1.65
    private const val INTRO_RETURN_MILLIS = 1300L
    private const val INTRO_END_EARLY_MILLIS = 200L

    private const val BLACK = 0xFF000000.toInt()
    private const val NIGHT = 0xFF0B1530.toInt()
    private const val TRIM = 0xFFF2C65A.toInt()
    private const val SNOW = 0xFFF4F8FF.toInt()
    private val BULBS = intArrayOf(0xFFE8394A.toInt(), 0xFFF2C65A.toInt(), 0xFF3FBF6E.toInt(), 0xFF9FD4FF.toInt())
    private const val BULB_GAP = 16
    private const val BULB_SIZE = 3
    private const val TWINKLE_MILLIS = 450L
    private const val FLAKES = 70
    private const val FALL_PER_SECOND = 0.09f
    private const val SNOW_ALPHA = 0.7f

    private class Flake(val x: Float, val y: Float, val size: Int, val speed: Float, val sway: Float, val alpha: Float)

    private class Shot(val eye: Vec3, val look: Vec3)

    private class Key(val at: Long, val shot: Shot?, val linear: Boolean = false)

    private class Caption(val at: Long, val name: String, val detail: String, val colour: Int)

    private class Film(
        val startedAt: Long,
        val keys: List<Key>,
        val captions: List<Caption>,
        val title: String,
        val length: Long,
        val moves: Boolean,
        val barShare: Float,
        val flakes: List<Flake> = emptyList(),
    )

    @Volatile
    private var film: Film? = null

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(CinemaPayload.TYPE) { payload, _ -> ceremony(payload) }
        ClientPlayNetworking.registerGlobalReceiver(IntroPayload.TYPE) { payload, _ -> intro(payload) }
        ClientPlayNetworking.registerGlobalReceiver(MomentPayload.TYPE) { payload, _ -> moment(payload) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> film = null }
    }

    private fun ceremony(payload: CinemaPayload) {
        val count = payload.names.size
        if (count == 0) {
            film = null
            return
        }

        val origin = Vec3(payload.x, payload.y, payload.z)
        val facing = facing(payload.yaw)
        val side = Vec3(-facing.z, 0.0, facing.x)

        fun step(place: Int): Vec3 = when (place) {
            0 -> origin
            1 -> origin.add(side.scale(STEP_SPACING))
            else -> origin.subtract(side.scale(STEP_SPACING))
        }

        val wide = Shot(origin.add(facing.scale(WIDE_BACK)).add(0.0, WIDE_UP, 0.0), origin.add(0.0, WIDE_LOOK_UP, 0.0))
        fun close(place: Int): Shot {
            val height = HEIGHTS[place]
            return Shot(
                step(place).add(facing.scale(CLOSE_BACK)).add(0.0, height + CLOSE_UP, 0.0),
                step(place).add(0.0, height + CLOSE_LOOK_UP, 0.0),
            )
        }

        val first = payload.untilFirstTicks * TICK_MILLIS
        val gap = payload.gapTicks * TICK_MILLIS

        val keys = mutableListOf(Key(0L, null), Key(LIFT_MILLIS, wide))
        val captions = mutableListOf<Caption>()
        var shot = wide
        for (order in 0 until count) {
            val place = count - 1 - order
            val reveal = first + order * gap
            val leave = maxOf(keys.last().at, reveal - LEAVE_BEFORE_REVEAL_MILLIS)
            keys += Key(leave, shot)
            shot = close(place)
            keys += Key(maxOf(leave + 1, reveal + ARRIVE_AFTER_REVEAL_MILLIS), shot)
            captions += Caption(reveal, payload.names[place], payload.details.getOrElse(place) { "" }, PLACE_COLOURS[place])
        }
        val held = keys.last().at + HOLD_WINNER_MILLIS
        keys += Key(held, shot)
        keys += Key(held + PULL_BACK_MILLIS, wide)
        keys += Key(held + PULL_BACK_MILLIS + RETURN_MILLIS, null)

        val startedAt = System.currentTimeMillis()
        val random = Random(startedAt)
        val flakes = List(FLAKES) {
            Flake(random.nextFloat(), random.nextFloat(), 1 + random.nextInt(2), 0.6f + random.nextFloat() * 0.8f,
                random.nextFloat() * 6.28f, 0.35f + random.nextFloat() * 0.65f)
        }
        film = Film(startedAt, keys, captions, CEREMONY, keys.last().at, true, BAR_SHARE, flakes)
    }

    private fun intro(payload: IntroPayload) {
        val centre = Vec3(payload.x, payload.y, payload.z)
        val facing = facing(payload.yaw)
        val below = centre.add(0.0, -ORBIT_LOOK_DOWN, 0.0)
        val startAngle = atan2(facing.z, facing.x) + Math.PI

        fun orbit(angle: Double): Shot = Shot(
            centre.add(cos(angle) * ORBIT_RADIUS, ORBIT_UP, sin(angle) * ORBIT_RADIUS),
            below,
        )

        val keys = mutableListOf(Key(0L, null))
        val steps = (ORBIT_MILLIS / ORBIT_STEP_MILLIS).toInt()
        for (index in 0..steps) {
            val progress = smooth(index / steps.toDouble())
            val angle = startAngle + Math.toRadians(ORBIT_DEGREES) * progress
            keys += Key(ORBIT_FROM_MILLIS + index * ORBIT_STEP_MILLIS, orbit(angle), linear = index > 0)
        }

        val seeker = Shot(centre.add(facing.scale(SEEKER_BACK)).add(0.0, SEEKER_UP, 0.0), centre.add(0.0, SEEKER_LOOK_UP, 0.0))
        val pushedIn = Shot(centre.add(facing.scale(SEEKER_CLOSE_BACK)).add(0.0, SEEKER_CLOSE_UP, 0.0), centre.add(0.0, SEEKER_LOOK_UP, 0.0))
        val atSeeker = keys.last().at + TO_SEEKER_MILLIS
        val length = payload.lengthTicks * TICK_MILLIS - INTRO_END_EARLY_MILLIS
        val leaveSeeker = maxOf(atSeeker + 1, length - INTRO_RETURN_MILLIS)
        keys += Key(atSeeker, seeker)
        keys += Key(leaveSeeker, pushedIn)
        keys += Key(maxOf(leaveSeeker + 1, length), null)

        val captions = listOf(Caption(atSeeker - TO_SEEKER_MILLIS / 2, payload.seeker, SEEKS, SEEKER_RED))
        film = Film(System.currentTimeMillis(), keys, captions, ROUND_OPENS, keys.last().at, true, BAR_SHARE)
    }

    private fun moment(payload: MomentPayload) {
        val current = running()
        if (current != null && current.moves) {
            return
        }
        val length = payload.millis.toLong().coerceAtLeast(1L)
        film = Film(
            System.currentTimeMillis(),
            listOf(Key(0L, null), Key(length, null)),
            listOf(Caption(0L, payload.name, payload.detail, payload.colour)),
            "",
            length,
            false,
            MOMENT_BAR_SHARE,
        )
    }

    private fun running(): Film? {
        val playing = film ?: return null
        if (System.currentTimeMillis() - playing.startedAt > playing.length) {
            film = null
            return null
        }
        return playing
    }

    @JvmStatic
    fun showing(): Boolean = running() != null

    @JvmStatic
    fun takesOver(): Boolean = running()?.moves == true

    @JvmStatic
    fun frame(homeX: Double, homeY: Double, homeZ: Double, homeYaw: Float, homePitch: Float): DoubleArray? {
        val playing = running() ?: return null
        if (!playing.moves) {
            return null
        }
        val now = System.currentTimeMillis() - playing.startedAt

        val home = Vec3(homeX, homeY, homeZ)
        val homeShot = Shot(home, home.add(Vec3.directionFromRotation(homePitch, homeYaw)))

        val keys = playing.keys
        var index = 0
        while (index < keys.size - 2 && keys[index + 1].at <= now) {
            index++
        }
        val from = keys[index]
        val to = keys[index + 1]
        val span = (to.at - from.at).coerceAtLeast(1L).toDouble()
        val raw = ((now - from.at) / span).coerceIn(0.0, 1.0)
        val progress = if (to.linear) raw else smooth(raw)

        val a = from.shot ?: homeShot
        val b = to.shot ?: homeShot
        val eye = a.eye.lerp(b.eye, progress)
        val look = a.look.lerp(b.look, progress).subtract(eye)

        val flat = sqrt(look.x * look.x + look.z * look.z)
        val yaw = Math.toDegrees(atan2(-look.x, look.z))
        val pitch = -Math.toDegrees(atan2(look.y, flat))
        return doubleArrayOf(eye.x, eye.y, eye.z, yaw, pitch)
    }

    @JvmStatic
    fun drawBars(graphics: GuiGraphicsExtractor) {
        val playing = running() ?: return
        val now = System.currentTimeMillis() - playing.startedAt
        val opening = (now / BARS_MILLIS).coerceIn(0f, 1f)
        val closing = ((playing.length - now) / BARS_MILLIS).coerceIn(0f, 1f)
        val amount = smoothF(minOf(opening, closing))

        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val bar = (height * playing.barShare * amount).toInt()
        if (bar <= 0) {
            return
        }

        val festive = playing.flakes.isNotEmpty()
        if (festive) {
            snow(graphics, playing.flakes, now, amount)
        }
        val shade = if (festive) NIGHT else BLACK
        graphics.fill(0, 0, width, bar, shade)
        graphics.fill(0, height - bar, width, height, shade)
        if (festive) {
            garland(graphics, bar, now, amount)
        }

        val font = Minecraft.getInstance().font
        if (playing.title.isNotEmpty()) {
            graphics.text(font, playing.title, (width - font.width(playing.title)) / 2, (bar - font.lineHeight) / 2,
                Vignette.alpha(if (festive) TRIM else Palette.GOLD, amount), false)
        }

        val caption = playing.captions.lastOrNull { it.at <= now } ?: return
        val shown = ((now - caption.at) / CAPTION_FADE_MILLIS).coerceIn(0f, 1f) * amount
        val scale = if (playing.moves) CAPTION_SCALE else 1f
        val centreY = height - bar / 2f

        if (caption.detail.isEmpty()) {
            drawScaled(graphics, caption.name, centreY, scale, Vignette.alpha(caption.colour, shown))
            return
        }
        drawScaled(graphics, caption.name, centreY - font.lineHeight * 0.45f * scale, scale, Vignette.alpha(caption.colour, shown))
        drawScaled(graphics, caption.detail, centreY + font.lineHeight * 0.55f * scale, 1f, Vignette.alpha(Palette.TEXT, shown))
    }

    private fun snow(graphics: GuiGraphicsExtractor, flakes: List<Flake>, now: Long, amount: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val seconds = now / 1000f
        for (flake in flakes) {
            val y = ((flake.y + seconds * FALL_PER_SECOND * flake.speed) % 1f) * height
            val x = flake.x * width + sin(seconds * 1.4f + flake.sway) * 6f
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + flake.size, y.toInt() + flake.size,
                Vignette.alpha(SNOW, amount * flake.alpha * SNOW_ALPHA))
        }
    }

    private fun garland(graphics: GuiGraphicsExtractor, bar: Int, now: Long, amount: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val trim = Vignette.alpha(TRIM, amount)
        graphics.fill(0, bar - 1, width, bar, trim)
        graphics.fill(0, height - bar, width, height - bar + 1, trim)
        val step = (now / TWINKLE_MILLIS).toInt()
        var index = 0
        var x = BULB_GAP / 2
        while (x < width) {
            val lit = (index + step) % 3 != 0
            val colour = Vignette.alpha(BULBS[(index + step / 2) % BULBS.size], amount * if (lit) 1f else 0.35f)
            val sag = if (index % 2 == 0) 1 else 2
            graphics.fill(x, bar + sag, x + BULB_SIZE, bar + sag + BULB_SIZE, colour)
            graphics.fill(x, height - bar - sag - BULB_SIZE, x + BULB_SIZE, height - bar - sag, colour)
            index++
            x += BULB_GAP
        }
    }

    private fun drawScaled(graphics: GuiGraphicsExtractor, text: String, centreY: Float, scale: Float, colour: Int) {
        val font = Minecraft.getInstance().font
        graphics.pose().pushMatrix()
        graphics.pose().translate(graphics.guiWidth() / 2f, centreY)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, -font.width(text) / 2, -font.lineHeight / 2, colour, true)
        graphics.pose().popMatrix()
    }

    private fun facing(yawDegrees: Float): Vec3 {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        return Vec3(-sin(yaw), 0.0, cos(yaw))
    }

    private fun smooth(value: Double): Double = value * value * (3.0 - 2.0 * value)

    private fun smoothF(value: Float): Float = value * value * (3f - 2f * value)
}
