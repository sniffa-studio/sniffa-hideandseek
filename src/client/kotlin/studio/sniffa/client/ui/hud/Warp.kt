package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

object Warp {

    private const val JUMP_BLOCKS = 12.0
    private const val DURATION_MILLIS = 900L

    private const val TINT = 0xB8C8FF
    private const val OVERLAY_FROM = 0.85f
    private const val OVERLAY_EASE = 2.2f

    private const val PULL = 0.07f
    private const val PULL_AT = 0.1f
    private const val SPREAD = 0.15f

    private const val PORTAL_PARTICLES = 64
    private const val BURST_PARTICLES = 24
    private const val TAU = 2.0 * Math.PI

    private var last: Vec3? = null
    private var lastLevel: ClientLevel? = null
    private var lastPlayer = -1

    private var startedAt = 0L

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }

        HudRenderCallback.EVENT.register { graphics, _ ->
            val t = progress() ?: return@register
            val client = Minecraft.getInstance()
            if (client.options.hideGui) return@register

            val strength = OVERLAY_FROM * (1f - t).pow(OVERLAY_EASE)
            if (strength <= 0.01f) return@register

            val portal = client.blockRenderer.blockModelShaper.getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState())
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                portal,
                0, 0,
                graphics.guiWidth(), graphics.guiHeight(),
                ARGB.color((strength * 255f).toInt(), TINT),
            )
        }
    }

    fun fovScale(): Float {
        val t = progress() ?: return 1f
        val off = (t - PULL_AT) / SPREAD
        return 1f - PULL * exp(-off * off)
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
            sparkle(level, player)
        }
    }

    private fun sparkle(level: ClientLevel, player: LocalPlayer) {
        val random = player.random

        repeat(PORTAL_PARTICLES) {
            level.addParticle(
                ParticleTypes.PORTAL,
                player.getRandomX(0.8),
                player.getRandomY() - 0.25,
                player.getRandomZ(0.8),
                (random.nextDouble() - 0.5) * 2.0,
                -random.nextDouble(),
                (random.nextDouble() - 0.5) * 2.0,
            )
        }

        repeat(BURST_PARTICLES) { index ->
            val angle = index * TAU / BURST_PARTICLES
            level.addParticle(
                ParticleTypes.REVERSE_PORTAL,
                player.x + cos(angle) * 0.6,
                player.y + 0.1,
                player.z + sin(angle) * 0.6,
                cos(angle) * 0.15,
                0.05,
                sin(angle) * 0.15,
            )
        }
    }
}
