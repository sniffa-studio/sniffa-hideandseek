package studio.sniffa.client.radar

import net.minecraft.world.entity.Entity

object CrewView {

    private const val HIDER_COLOUR = 0x4EC471
    private const val SEEKER_COLOUR = 0xFC301E

    private const val STALE_MILLIS = 3_000L

    @Volatile
    private var hiders: Set<Int> = emptySet()

    @Volatile
    private var seekers: Set<Int> = emptySet()

    @Volatile
    private var receivedAt = 0L

    fun accept(hiderIds: List<Int>, seekerIds: List<Int>) {
        hiders = hiderIds.toSet()
        seekers = seekerIds.toSet()
        receivedAt = System.currentTimeMillis()
    }

    fun forget() {
        hiders = emptySet()
        seekers = emptySet()
        receivedAt = 0L
    }

    fun colourOf(entity: Entity): Int? {
        if (System.currentTimeMillis() - receivedAt > STALE_MILLIS) return null
        return when (entity.id) {
            in hiders -> HIDER_COLOUR
            in seekers -> SEEKER_COLOUR
            else -> null
        }
    }

    fun knows(entity: Entity): Boolean = colourOf(entity) != null
}
