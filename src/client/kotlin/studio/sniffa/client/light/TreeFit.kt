package studio.sniffa.client.light

import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.BlockGetter
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

object TreeFit {

    const val TALLEST = 40f

    private const val POINTS_PER_TURN = 56
    private const val STEP = 0.125f
    private const val CLEARANCE = 0.3f
    private const val TIP_RADIUS = 0.35f
    private const val CORE = 0.6f
    private const val SMOOTHING = 6
    private const val DRAPE = 0.14f
    private const val DRAPE_WAVES = 5.0
    private const val TURNS_PER_BLOCK = 0.75f
    private const val LONGEST_GAP = 2
    private const val TRUNK_SEARCH = 6
    private const val TRUNK_BELOW = 3
    private const val TRUNK_ABOVE = 8

    fun spiral(level: BlockGetter?, anchorX: Double, y: Double, anchorZ: Double, asked: Float, radius: Float, turnsAsked: Float): List<Vector3f> {
        val trunk = level?.let { trunkOf(it, anchorX, y, anchorZ, radius) }
        val x = trunk?.first ?: anchorX
        val z = trunk?.second ?: anchorZ
        val shiftX = (x - anchorX).toFloat()
        val shiftZ = (z - anchorZ).toFloat()
        val height = if (asked > 0f) asked else level?.let { heightOf(it, x, y, z, radius) } ?: radius * 2f
        val turns = if (turnsAsked > 0f) turnsAsked else max(2f, height * TURNS_PER_BLOCK)
        val count = (turns * POINTS_PER_TURN).toInt().coerceIn(16, 2048)
        val pos = BlockPos.MutableBlockPos()

        val radii = FloatArray(count + 1) { index ->
            val t = index / count.toFloat()
            val angle = t * turns * 2.0 * PI
            val cone = radius * (1f - t) + TIP_RADIUS * t
            level?.let { surface(it, pos, x, y + t * height, z, angle, radius) } ?: cone
        }
        val smooth = smoothed(radii)

        return List(count + 1) { index ->
            val t = index / count.toFloat()
            val angle = t * turns * 2.0 * PI
            val r = max(smooth[index], TIP_RADIUS)
            val drape = DRAPE * (0.5f - 0.5f * cos(angle * DRAPE_WAVES).toFloat())
            Vector3f((cos(angle) * r).toFloat() + shiftX, t * height - drape, (sin(angle) * r).toFloat() + shiftZ)
        }
    }

    fun trunkOf(level: BlockGetter, x: Double, y: Double, z: Double, radius: Float): Pair<Double, Double>? {
        val pos = BlockPos.MutableBlockPos()
        val bx = floor(x).toInt()
        val by = floor(y).toInt()
        val bz = floor(z).toInt()
        val reach = radius.toInt().coerceAtMost(TRUNK_SEARCH)
        val counts = HashMap<Long, Int>()
        for (dx in -reach..reach) for (dz in -reach..reach) {
            var logs = 0
            for (dy in -TRUNK_BELOW..TRUNK_ABOVE) {
                if (level.getBlockState(pos.set(bx + dx, by + dy, bz + dz)).`is`(BlockTags.LOGS)) logs++
            }
            if (logs > 0) counts[BlockPos.asLong(bx + dx, 0, bz + dz)] = logs
        }
        val most = counts.values.maxOrNull() ?: return null
        val trunk = counts.filterValues { it * 10 >= most * 6 }.keys
        val cx = trunk.map { BlockPos.getX(it) + 0.5 }.average()
        val cz = trunk.map { BlockPos.getZ(it) + 0.5 }.average()
        return cx to cz
    }

    fun heightOf(level: BlockGetter, x: Double, y: Double, z: Double, radius: Float): Float {
        val pos = BlockPos.MutableBlockPos()
        val base = floor(y).toInt()
        var top = base
        var gap = 0
        var at = base
        while (at - base < TALLEST.toInt()) {
            if (columnFilled(level, pos, x, at, z)) {
                top = at
                gap = 0
            } else if (++gap > LONGEST_GAP) {
                break
            }
            at++
        }
        val height = (top + 1 - y).toFloat()
        return if (height < 1f) radius * 2f else height
    }

    private fun columnFilled(level: BlockGetter, pos: BlockPos.MutableBlockPos, x: Double, y: Int, z: Double): Boolean {
        for (dx in -1..1) for (dz in -1..1) {
            if (!level.getBlockState(pos.set(floor(x).toInt() + dx, y, floor(z).toInt() + dz)).isAir) return true
        }
        return false
    }

    private fun surface(level: BlockGetter, pos: BlockPos.MutableBlockPos, x: Double, y: Double, z: Double, angle: Double, radius: Float): Float? {
        val cx = cos(angle)
        val cz = sin(angle)
        var r = radius
        val by = floor(y).toInt()
        while (r > CORE) {
            val state = level.getBlockState(pos.set(floor(x + cx * r).toInt(), by, floor(z + cz * r).toInt()))
            if (!state.isAir) return r + CLEARANCE
            r -= STEP
        }
        return null
    }

    private fun smoothed(radii: FloatArray): FloatArray {
        return FloatArray(radii.size) { index ->
            var sum = 0f
            var weight = 0f
            for (offset in -SMOOTHING..SMOOTHING) {
                val value = radii.getOrNull(index + offset) ?: continue
                sum += value
                weight += 1f
            }
            sum / weight
        }
    }
}
