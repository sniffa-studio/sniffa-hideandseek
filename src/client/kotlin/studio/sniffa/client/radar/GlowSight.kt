package studio.sniffa.client.radar

import net.minecraft.world.entity.Entity

object GlowSight {

    @Volatile
    private var lit: Set<Int> = emptySet()

    @Volatile
    private var until = 0L

    fun accept(entityIds: List<Int>, seconds: Int) {
        lit = entityIds.toSet()
        until = System.currentTimeMillis() + seconds * 1000L
    }

    fun forget() {
        lit = emptySet()
        until = 0L
    }

    fun active(): Boolean = until != 0L && System.currentTimeMillis() < until

    fun glowing(entity: Entity): Boolean = entity.id in lit
}
