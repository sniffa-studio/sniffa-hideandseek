package studio.sniffa.client.light

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Display
import studio.sniffa.Hideandseek
import kotlin.math.cos
import kotlin.math.sin

object Chimneys {

    val SMOKE = Hideandseek.id("props/rauch")

    private const val SCAN_EVERY_TICKS = 20
    private const val SHOWN_WITHIN = 128.0
    private const val TICKS_PER_PUFF = 3.5f
    private const val SPREAD = 0.22
    private const val RISE = 0.055
    private const val WIND = 0.012
    private const val WIND_TURN_SECONDS = 90.0

    private class Chimney(val x: Double, val y: Double, val z: Double, val strength: Float) {
        var owed = 0f
    }

    private val chimneys = HashMap<Int, Chimney>()
    private var ticks = 0

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null) {
            chimneys.clear()
            return
        }
        if (client.isPaused) return
        if (++ticks % SCAN_EVERY_TICKS == 0) scan(level)
        if (chimneys.isEmpty()) return

        val camera = client.gameRenderer.mainCamera().position()
        val random = level.random
        val heading = (System.currentTimeMillis() / 1000.0) / WIND_TURN_SECONDS
        val windX = cos(heading) * WIND
        val windZ = sin(heading) * WIND
        for (chimney in chimneys.values) {
            if (camera.distanceToSqr(chimney.x, chimney.y, chimney.z) > SHOWN_WITHIN * SHOWN_WITHIN) continue
            chimney.owed += chimney.strength / TICKS_PER_PUFF
            while (chimney.owed >= 1f) {
                chimney.owed -= 1f
                level.addAlwaysVisibleParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    true,
                    chimney.x + (random.nextDouble() - 0.5) * 2.0 * SPREAD,
                    chimney.y + random.nextDouble() * 0.3,
                    chimney.z + (random.nextDouble() - 0.5) * 2.0 * SPREAD,
                    windX,
                    RISE * (0.8 + random.nextDouble() * 0.4),
                    windZ,
                )
            }
        }
    }

    private fun scan(level: ClientLevel) {
        val seen = HashSet<Int>()
        for (entity in level.entitiesForRendering()) {
            if (entity !is Display.ItemDisplay) continue
            val stack = entity.itemRenderState()?.itemStack() ?: continue
            if (stack.get(DataComponents.ITEM_MODEL) != SMOKE) continue
            val strength = (stack.get(DataComponents.CUSTOM_MODEL_DATA)?.floats()?.firstOrNull() ?: 1f).coerceIn(0.1f, 4f)
            seen += entity.id
            val at = entity.position()
            val known = chimneys[entity.id]
            if (known == null || known.strength != strength || known.x != at.x || known.y != at.y || known.z != at.z) {
                chimneys[entity.id] = Chimney(at.x, at.y, at.z, strength)
            }
        }
        chimneys.keys.retainAll(seen)
    }
}
