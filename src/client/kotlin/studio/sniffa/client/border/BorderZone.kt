package studio.sniffa.client.border

import net.minecraft.client.Minecraft
import net.minecraft.world.level.border.BorderStatus

object BorderZone {

    data class Ring(val centerX: Double, val centerZ: Double, val radius: Double)

    private var move: Move? = null

    private data class Move(
        val centerX: Double,
        val centerZ: Double,
        val from: Double,
        val to: Double,
        val durationMillis: Int,
        val startedAt: Long,
    )

    fun accept(centerX: Double, centerZ: Double, from: Double, to: Double, durationMillis: Int) {
        move = Move(centerX, centerZ, from, to, durationMillis, System.currentTimeMillis())
    }

    fun forget() {
        move = null
    }

    fun current(): Ring? {
        move?.let { return Ring(it.centerX, it.centerZ, radiusOf(it)) }
        return fromVanilla()
    }

    private fun radiusOf(move: Move): Double {
        if (move.durationMillis <= 0 || move.from == move.to) return move.to

        val elapsed = System.currentTimeMillis() - move.startedAt
        val progress = (elapsed.toDouble() / move.durationMillis).coerceIn(0.0, 1.0)
        return move.from + (move.to - move.from) * progress
    }

    private fun fromVanilla(): Ring? {
        val level = Minecraft.getInstance().level ?: return null
        val border = level.worldBorder

        val radius = border.size / 2.0
        if (radius > VISIBLE_LIMIT) return null

        return Ring(border.centerX, border.centerZ, radius)
    }

    enum class Motion { STATIONARY, SHRINKING, GROWING }

    fun motion(): Motion {
        move?.let {
            val running = System.currentTimeMillis() - it.startedAt < it.durationMillis
            return when {
                !running -> Motion.STATIONARY
                it.to < it.from -> Motion.SHRINKING
                it.to > it.from -> Motion.GROWING
                else -> Motion.STATIONARY
            }
        }

        val level = Minecraft.getInstance().level ?: return Motion.STATIONARY
        return when (level.worldBorder.status) {
            BorderStatus.SHRINKING -> Motion.SHRINKING
            BorderStatus.GROWING -> Motion.GROWING
            else -> Motion.STATIONARY
        }
    }

    fun tint(): Int = when (motion()) {
        Motion.SHRINKING -> SHRINKING
        Motion.GROWING -> GROWING
        Motion.STATIONARY -> STATIONARY
    }

    private const val STATIONARY = 0x3A8DFF
    private const val SHRINKING = 0xFF4F7A
    private const val GROWING = 0x3FE0B0

    const val BOTTOM = -64.0
    const val TOP = 320.0

    private const val VISIBLE_LIMIT = 4096.0
}
