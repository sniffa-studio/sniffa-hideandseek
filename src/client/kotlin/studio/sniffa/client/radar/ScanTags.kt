package studio.sniffa.client.radar

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import java.util.concurrent.ConcurrentHashMap

object ScanTags {

    private const val HOLD_MILLIS = 8_000L

    private const val LIFETIME_MILLIS = HOLD_MILLIS + 10_000L

    @Volatile
    private var marked: Set<Int> = emptySet()

    @Volatile
    private var markedAt = 0L

    private val reachedAt = ConcurrentHashMap<Int, Long>()

    fun accept(entityIds: List<Int>) {
        reachedAt.clear()
        marked = entityIds.toSet()
        markedAt = System.currentTimeMillis()
    }

    fun forget() {
        marked = emptySet()
        reachedAt.clear()
    }

    fun glowing(entity: Entity): Boolean {
        val ids = marked
        if (ids.isEmpty() || entity.id !in ids) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - markedAt > LIFETIME_MILLIS) {
            forget()
            return false
        }

        reachedAt[entity.id]?.let { return now - it <= HOLD_MILLIS }

        val wave = ScanPulse.inFlight() ?: return false

        val at = entity.position().add(0.0, entity.bbHeight / 2.0, 0.0)
        val distance = at.distanceTo(wave.origin).toFloat()

        if (distance > wave.radius) {
            return false
        }

        val reached = now - ((wave.radius - distance) / wave.speed * 1000f).toLong()
        reachedAt[entity.id] = reached
        return now - reached <= HOLD_MILLIS
    }

    fun any(): Boolean = marked.isNotEmpty()

    fun ready(): Boolean = Minecraft.getInstance().level != null
}
