package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import studio.sniffa.client.map.MapOverlay
import kotlin.math.exp

object Warp {

    private const val JUMP_BLOCKS = 12.0
    private const val DURATION_MILLIS = 700L

    private const val TINT = 0x3A8DFF

    private const val PULL = 0.2f
    private const val PULL_AT = 0.2f
    private const val SPRING = 0.05f
    private const val SPRING_AT = 0.55f
    private const val SPREAD = 0.12f

    private var last: Vec3? = null
    private var lastLevel: ClientLevel? = null
    private var lastPlayer = -1

    private var startedAt = 0L

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }

        HudRenderCallback.EVENT.register { graphics, _ ->
            val progress = progress() ?: return@register
            if (Minecraft.getInstance().options.hideGui) return@register

            val step = (progress * 255f).toInt().coerceIn(0, 255)
            MapOverlay.panel(
                graphics,
                MapOverlay.WARP,
                MapOverlay.wholeScreen(graphics.guiWidth(), graphics.guiHeight()),
                (step shl 24) or TINT,
            )
        }
    }

    fun fovScale(): Float {
        val t = progress() ?: return 1f
        return 1f - PULL * bump(t, PULL_AT) + SPRING * bump(t, SPRING_AT)
    }

    private fun bump(t: Float, at: Float): Float {
        val off = (t - at) / SPREAD
        return exp(-off * off)
    }

    private fun progress(): Float? {
        if (startedAt == 0L) return null
        val t = (System.currentTimeMillis() - startedAt).toFloat() / DURATION_MILLIS
        if (t >= 1f) {
            startedAt = 0L
            return null
        }
        return t
    }

    private fun tick(client: Minecraft) {
        val player = client.player
        val level = client.level

        if (player == null || level == null || level !== lastLevel || player.id != lastPlayer) {
            last = player?.position()
            lastLevel = level
            lastPlayer = player?.id ?: -1
            return
        }

        val now = player.position()
        val before = last
        last = now

        if (before != null && now.distanceTo(before) > JUMP_BLOCKS) {
            startedAt = System.currentTimeMillis()
        }
    }
}
