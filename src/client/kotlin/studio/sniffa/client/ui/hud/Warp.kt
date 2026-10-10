package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import kotlin.math.pow

object Warp {

    private const val JUMP_BLOCKS = 12.0

    private const val HOLD_MILLIS = 120L
    private const val OPEN_MILLIS = 380L

    private const val SOFT_EDGE = 8

    private const val BLACK = 0xFF000000.toInt()
    private const val CLEAR = 0x00000000

    private var last: Vec3? = null
    private var lastLevel: ClientLevel? = null
    private var lastPlayer = -1

    private var startedAt = 0L

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }

        HudRenderCallback.EVENT.register { graphics, _ ->
            if (startedAt == 0L) return@register

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= HOLD_MILLIS + OPEN_MILLIS) {
                startedAt = 0L
                return@register
            }

            val opened = ((elapsed - HOLD_MILLIS).toFloat() / OPEN_MILLIS).coerceIn(0f, 1f)
            draw(graphics, 1f - (1f - opened).pow(3))
        }
    }

    private fun draw(graphics: GuiGraphics, opened: Float) {
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        val cover = ((height + 1) / 2 * (1f - opened)).toInt()
        if (cover <= 0) return

        graphics.fill(0, 0, width, cover, BLACK)
        graphics.fill(0, height - cover, width, height, BLACK)

        graphics.fillGradient(0, cover, width, cover + SOFT_EDGE, BLACK, CLEAR)
        graphics.fillGradient(0, height - cover - SOFT_EDGE, width, height - cover, CLEAR, BLACK)
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
