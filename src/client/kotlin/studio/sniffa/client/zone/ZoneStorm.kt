package studio.sniffa.client.zone

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import studio.sniffa.Hideandseek
import kotlin.math.sqrt

object ZoneStorm {

    private val WIND: SoundEvent = SoundEvent.createVariableRangeEvent(Hideandseek.id("zone.wind"))

    private const val HEARD_FROM = 28.0
    private const val INSIDE_VOLUME = 0.6f
    private const val EDGE_VOLUME = 0.45f
    private const val VOLUME_EASE = 0.06f

    private const val FLURRY_FROM = 18.0
    private const val FLAKES_INSIDE = 16
    private const val FLAKES_AT_EDGE = 8
    private const val FLURRY_REACH = 14.0
    private const val SWIRL = 0.42
    private const val PULL = 0.05
    private const val FALL = -0.07

    @Volatile
    var presence = 0f
        private set

    private var wantedVolume = 0f
    private var wind: Wind? = null

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun tick(client: Minecraft) {
        val player = client.player
        val level = client.level
        val zone = DangerZone.current()

        if (player == null || level == null || zone == null) {
            wantedVolume = 0f
            presence = (presence - 0.08f).coerceAtLeast(0f)
            return
        }

        val dx = player.x - zone.centerX
        val dz = player.z - zone.centerZ
        val fromCentre = sqrt(dx * dx + dz * dz)
        val past = fromCentre - zone.radius

        val inside = past <= 0.0
        presence = if (inside) (presence + 0.05f).coerceAtMost(1f) else (presence - 0.08f).coerceAtLeast(0f)

        val edge = (1.0 - past.coerceAtLeast(0.0) / HEARD_FROM).coerceIn(0.0, 1.0).toFloat()
        wantedVolume = if (inside) INSIDE_VOLUME else EDGE_VOLUME * edge * edge
        keepWindGoing(client)

        if (past < FLURRY_FROM) flurry(client, zone, inside, past)
    }

    private fun keepWindGoing(client: Minecraft) {
        val playing = wind
        if (wantedVolume > 0.01f && (playing == null || playing.isStopped)) {
            wind = Wind().also { client.soundManager.play(it) }
        }
    }

    private fun flurry(client: Minecraft, zone: DangerZone.Ring, inside: Boolean, past: Double) {
        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera().position()
        val random = level.random
        val count = if (inside) FLAKES_INSIDE else (FLAKES_AT_EDGE * (1.0 - past / FLURRY_FROM)).toInt()

        repeat(count) {
            val x = camera.x + (random.nextDouble() * 2.0 - 1.0) * FLURRY_REACH
            val z = camera.z + (random.nextDouble() * 2.0 - 1.0) * FLURRY_REACH
            val y = camera.y - 2.0 + random.nextDouble() * 10.0

            val ox = x - zone.centerX
            val oz = z - zone.centerZ
            val reach = sqrt(ox * ox + oz * oz)
            if (reach > zone.radius || reach < 0.5) return@repeat

            val swirlX = -oz / reach * SWIRL - ox / reach * PULL
            val swirlZ = ox / reach * SWIRL - oz / reach * PULL
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, swirlX, FALL, swirlZ)
        }
    }

    private class Wind : AbstractTickableSoundInstance(WIND, SoundSource.AMBIENT, RandomSource.create()) {

        init {
            looping = true
            delay = 0
            relative = true
            attenuation = SoundInstance.Attenuation.NONE
            volume = 0.01f
            pitch = 1f
        }

        override fun tick() {
            volume += (wantedVolume - volume) * VOLUME_EASE
            val urgent = DangerZone.current()?.let { DangerZone.secondsLeft() <= 10 } ?: false
            pitch += ((if (urgent) 1.15f else 1f) - pitch) * 0.05f
            if (wantedVolume <= 0.01f && volume < 0.012f) stop()
        }
    }
}
