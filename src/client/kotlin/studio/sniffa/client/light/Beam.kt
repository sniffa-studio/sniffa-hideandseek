package studio.sniffa.client.light

import net.minecraft.world.entity.Display
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

data class Beam(val x: Double, val y: Double, val z: Double, val axis: Vector3f) {

    fun across(): Pair<Vector3f, Vector3f> {
        val helper = if (kotlin.math.abs(axis.y) > 0.9f) Vector3f(1f, 0f, 0f) else Vector3f(0f, 1f, 0f)
        val side = Vector3f(axis).cross(helper).normalize()
        val up = Vector3f(side).cross(axis).normalize()
        return side to up
    }

    fun within(px: Double, py: Double, pz: Double, distance: Double): Boolean {
        val dx = x - px
        val dy = y - py
        val dz = z - pz
        return dx * dx + dy * dy + dz * dz <= distance * distance
    }

    fun movedFrom(other: Beam): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz > MOVE_SQUARED || axis.dot(other.axis) < TURN_COSINE
    }

    companion object {

        private const val LENS = 0.26f
        private const val MOVE_SQUARED = 0.01
        private const val TURN_COSINE = 0.9998f
        private const val DEGREES = (Math.PI / 180.0).toFloat()

        fun of(display: Display): Beam? {
            val state = display.renderState() ?: return null
            val matrix = Matrix4f(state.transformation().get(1f).matrix)
            val turn = Quaternionf().rotationYXZ(-display.yRot * DEGREES, display.xRot * DEGREES, 0f)

            val axis = matrix.transformDirection(Vector3f(0f, 1f, 0f))
            turn.transform(axis)
            if (axis.lengthSquared() < 1.0e-6f) return null
            axis.normalize()

            val lens = matrix.transformPosition(Vector3f(0f, LENS, 0f))
            turn.transform(lens)

            val at = display.position()
            return Beam(at.x + lens.x, at.y + lens.y, at.z + lens.z, axis)
        }
    }
}
