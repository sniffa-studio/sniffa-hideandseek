package studio.sniffa.client.light

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LitFaces(private val beam: Beam, private val reach: Double, private val cosine: Double) {

    val faces = HashMap<Long, Int>()

    private var next = 0

    val done: Boolean get() = next >= RAYS

    private val side: Vector3f
    private val up: Vector3f

    init {
        val helper = if (kotlin.math.abs(beam.axis.y) > 0.9f) Vector3f(1f, 0f, 0f) else Vector3f(0f, 1f, 0f)
        side = Vector3f(beam.axis).cross(helper).normalize()
        up = Vector3f(side).cross(beam.axis).normalize()
    }

    fun trace(level: Level, budget: Int): Int {
        val from = Vec3(beam.x, beam.y, beam.z)
        var used = 0
        while (used < budget && next < RAYS) {
            val ray = direction(next++)
            val to = from.add(ray.x * reach, ray.y * reach, ray.z * reach)
            val hit = level.clip(ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
            if (hit.type == HitResult.Type.BLOCK) {
                val block = hit as BlockHitResult
                val key = block.blockPos.asLong()
                faces[key] = (faces[key] ?: 0) or (1 shl block.direction.ordinal)
            }
            used++
        }
        return used
    }

    private fun direction(index: Int): Vec3 {
        val cosTheta = 1.0 - (index + 0.5) / RAYS * (1.0 - cosine)
        val sinTheta = sqrt(maxOf(0.0, 1.0 - cosTheta * cosTheta))
        val phi = index * GOLDEN_ANGLE
        val a = cos(phi) * sinTheta
        val b = sin(phi) * sinTheta
        return Vec3(
            beam.axis.x * cosTheta + side.x * a + up.x * b,
            beam.axis.y * cosTheta + side.y * a + up.y * b,
            beam.axis.z * cosTheta + side.z * a + up.z * b,
        )
    }

    companion object {
        const val RAYS = 16_000
        private val GOLDEN_ANGLE = PI * (3.0 - sqrt(5.0))

        fun directionOf(bit: Int): Direction = Direction.entries[bit]

        fun posOf(key: Long): BlockPos = BlockPos.of(key)
    }
}
