package studio.sniffa.client.cinema

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.Vec3
import studio.sniffa.client.ui.hud.Vignette
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.net.payload.CinemaPayload
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Cinema {

    private const val STEP_SPACING = 1.75
    private val HEIGHTS = doubleArrayOf(1.6, 1.1, 0.7)
    private val COLOURS = intArrayOf(0xFFCCA120.toInt(), 0xFFC4CCD4.toInt(), 0xFFC8844E.toInt())

    private const val TICK_MILLIS = 50L
    private const val LIFT_MILLIS = 1500L
    private const val LEAVE_BEFORE_REVEAL_MILLIS = 350L
    private const val ARRIVE_AFTER_REVEAL_MILLIS = 650L
    private const val HOLD_WINNER_MILLIS = 2600L
    private const val PULL_BACK_MILLIS = 1200L
    private const val RETURN_MILLIS = 1500L
    private const val BARS_MILLIS = 600f

    private const val BAR_SHARE = 0.12f
    private const val CAPTION_SCALE = 2f
    private const val CAPTION_FADE_MILLIS = 300f
    private const val TITLE = "ꜱɪᴇɢᴇʀᴇʜʀᴜɴɢ"

    private const val WIDE_BACK = 9.0
    private const val WIDE_UP = 3.4
    private const val WIDE_LOOK_UP = 1.8
    private const val CLOSE_BACK = 3.4
    private const val CLOSE_UP = 1.8
    private const val CLOSE_LOOK_UP = 1.4

    private const val BLACK = 0xFF000000.toInt()

    private class Shot(val eye: Vec3, val look: Vec3)

    private class Key(val at: Long, val shot: Shot?)

    private class Caption(val at: Long, val place: Int, val name: String, val detail: String)

    private class Film(val startedAt: Long, val keys: List<Key>, val captions: List<Caption>) {
        val length: Long = keys.last().at
    }

    @Volatile
    private var film: Film? = null

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(CinemaPayload.TYPE) { payload, _ -> play(payload) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> film = null }
    }

    private fun play(payload: CinemaPayload) {
        val count = payload.names.size
        if (count == 0) {
            film = null
            return
        }

        val origin = Vec3(payload.x, payload.y, payload.z)
        val yaw = Math.toRadians(payload.yaw.toDouble())
        val facing = Vec3(-sin(yaw), 0.0, cos(yaw))
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
            captions += Caption(reveal, place, payload.names[place], payload.details.getOrElse(place) { "" })
        }
        val held = keys.last().at + HOLD_WINNER_MILLIS
        keys += Key(held, shot)
        keys += Key(held + PULL_BACK_MILLIS, wide)
        keys += Key(held + PULL_BACK_MILLIS + RETURN_MILLIS, null)

        film = Film(System.currentTimeMillis(), keys, captions)
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
    fun frame(homeX: Double, homeY: Double, homeZ: Double, homeYaw: Float, homePitch: Float): DoubleArray? {
        val playing = running() ?: return null
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
        val progress = smooth(((now - from.at) / span).coerceIn(0.0, 1.0))

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
    fun drawBars(graphics: GuiGraphics) {
        val playing = running() ?: return
        val now = System.currentTimeMillis() - playing.startedAt
        val opening = (now / BARS_MILLIS).coerceIn(0f, 1f)
        val closing = ((playing.length - now) / BARS_MILLIS).coerceIn(0f, 1f)
        val amount = smoothF(minOf(opening, closing))

        val width = graphics.guiWidth()
        val height = graphics.guiHeight()
        val bar = (height * BAR_SHARE * amount).toInt()
        if (bar <= 0) {
            return
        }

        graphics.fill(0, 0, width, bar, BLACK)
        graphics.fill(0, height - bar, width, height, BLACK)

        val font = Minecraft.getInstance().font
        val fade = amount
        graphics.drawString(font, TITLE, (width - font.width(TITLE)) / 2, (bar - font.lineHeight) / 2,
            Vignette.alpha(Palette.MUTED, fade), false)

        val caption = playing.captions.lastOrNull { it.at <= now } ?: return
        val shown = ((now - caption.at) / CAPTION_FADE_MILLIS).coerceIn(0f, 1f) * fade
        val headline = "#${caption.place + 1}  ${caption.name}"
        val centreY = height - bar / 2f

        graphics.pose().pushMatrix()
        graphics.pose().translate(width / 2f, centreY - font.lineHeight * 0.9f)
        graphics.pose().scale(CAPTION_SCALE, CAPTION_SCALE)
        graphics.drawString(font, headline, -font.width(headline) / 2, -font.lineHeight / 2,
            Vignette.alpha(COLOURS[caption.place], shown), true)
        graphics.pose().popMatrix()

        if (caption.detail.isNotEmpty()) {
            graphics.drawString(font, caption.detail, (width - font.width(caption.detail)) / 2,
                (centreY + font.lineHeight * 0.6f).toInt(), Vignette.alpha(Palette.TEXT, shown), true)
        }
    }

    private fun smooth(value: Double): Double = value * value * (3.0 - 2.0 * value)

    private fun smoothF(value: Float): Float = value * value * (3f - 2f * value)
}
