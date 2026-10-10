package studio.sniffa.client.border

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import studio.sniffa.client.state.RoundState
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

object BorderEffects {

    private data class Ripple(val angle: Double, val height: Double, val startedAt: Long)

    private const val TOUCH_BLOCKS = 1.5
    private const val TOUCH_COOLDOWN_MILLIS = 1200L

    private const val RIPPLES = 3
    private const val RIPPLE_MILLIS = 1600L

    private const val REVEAL_MILLIS = 2600L

    private const val CLOSE_FROM = 2.0
    private const val CLOSE_TO = 24.0

    private const val TAU = 2.0 * Math.PI

    private val ripples = ArrayDeque<Ripple>()
    private val touchedAt = HashMap<UUID, Long>()

    private var visible = false
    private var revealFrom = 0L
    private var phase = RoundState.Phase.IDLE

    fun install() {
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
    }

    private fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        ripples.removeAll { now - it.startedAt > RIPPLE_MILLIS }
        touchedAt.values.removeIf { now - it > TOUCH_COOLDOWN_MILLIS }

        if (RoundState.phase != phase) {
            phase = RoundState.phase
            if (phase == RoundState.Phase.HIDING) revealFrom = now
        }

        val level = client.level
        val ring = BorderZone.current()
        if (level == null || ring == null) {
            visible = false
            ripples.clear()
            return
        }

        if (!visible) {
            visible = true
            revealFrom = now
        }

        for (player in level.players()) {
            if (player.isSpectator) continue

            val dx = player.x - ring.centerX
            val dz = player.z - ring.centerZ
            if (abs(ring.radius - hypot(dx, dz)) > TOUCH_BLOCKS) continue
            if (touchedAt.containsKey(player.uuid)) continue

            touchedAt[player.uuid] = now
            ripples.addLast(Ripple((atan2(dz, dx) + TAU) % TAU, player.y + player.bbHeight / 2.0, now))
            if (ripples.size > RIPPLES) ripples.removeFirst()
        }
    }

    fun uniforms(ring: BorderZone.Ring, columns: Int, camera: Vec3): Matrix4f {
        val now = System.currentTimeMillis()
        val values = FloatArray(16)

        for (slot in 0 until RIPPLES) {
            values[slot * 4 + 2] = -1f
        }

        ripples.forEachIndexed { slot, ripple ->
            values[slot * 4] = (ripple.angle / TAU * columns * BorderMesh.PERIOD_CELLS).toFloat()
            values[slot * 4 + 1] = (ripple.height / BorderMesh.CELL_BLOCKS).toFloat()
            values[slot * 4 + 2] = ((now - ripple.startedAt) / 1000.0).toFloat()
        }

        val wall = abs(ring.radius - hypot(camera.x - ring.centerX, camera.z - ring.centerZ))

        values[12] = columns.toFloat()
        values[13] = (1.0 - ((wall - CLOSE_FROM) / (CLOSE_TO - CLOSE_FROM)).coerceIn(0.0, 1.0)).toFloat()
        values[14] = ((now - revealFrom).toDouble() / REVEAL_MILLIS).coerceIn(0.0, 1.0).toFloat()
        values[15] = BorderMesh.CELL_BLOCKS.toFloat()

        return Matrix4f().set(values)
    }
}
