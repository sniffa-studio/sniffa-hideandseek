package studio.sniffa.client.radar

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

object ScanTags {

    private const val HOLD_MILLIS = 2800f

    @Volatile
    private var marked: Set<Int> = emptySet()

    fun accept(entityIds: List<Int>) {
        marked = entityIds.toSet()
    }

    fun forget() {
        marked = emptySet()
    }

    fun glowing(entity: Entity): Boolean {
        val ids = marked
        if (ids.isEmpty() || entity.id !in ids) {
            return false
        }

        val wave = ScanPulse.inFlight() ?: return false

        val at = entity.position().add(0.0, entity.bbHeight / 2.0, 0.0)
        val distance = at.distanceTo(wave.origin).toFloat()

        if (distance > wave.radius) {
            return false
        }

        val since = (wave.radius - distance) / wave.speed * 1000f
        return since <= HOLD_MILLIS
    }

    fun any(): Boolean = marked.isNotEmpty()

    fun ready(): Boolean = Minecraft.getInstance().level != null
}
