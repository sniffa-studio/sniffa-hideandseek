package studio.sniffa.client.zone

import net.minecraft.client.Minecraft

object DangerZone {

    data class Ring(val centerX: Double, val centerZ: Double, val radius: Double)

    @Volatile
    private var ring: Ring? = null

    @Volatile
    private var endsAt = 0L

    fun accept(x: Int, z: Int, radius: Int, secondsLeft: Int) {
        if (radius <= 0) {
            forget()
            return
        }
        ring = Ring(x.toDouble(), z.toDouble(), radius.toDouble())
        endsAt = System.currentTimeMillis() + secondsLeft * 1000L
    }

    fun forget() {
        ring = null
        endsAt = 0L
    }

    fun current(): Ring? {
        val standing = ring ?: return null
        if (System.currentTimeMillis() >= endsAt) {
            forget()
            return null
        }
        return standing
    }

    fun secondsLeft(): Int {
        val left = endsAt - System.currentTimeMillis()
        return if (left <= 0L) 0 else ((left + 999L) / 1000L).toInt()
    }

    fun caughtInside(): Boolean {
        val standing = current() ?: return false
        val player = Minecraft.getInstance().player ?: return false

        val dx = player.x - standing.centerX
        val dz = player.z - standing.centerZ
        return dx * dx + dz * dz <= standing.radius * standing.radius
    }
}
