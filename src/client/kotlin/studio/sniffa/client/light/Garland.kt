package studio.sniffa.client.light

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import org.joml.Vector3f
import kotlin.math.sqrt

data class Garland(
    val x: Double,
    val y: Double,
    val z: Double,
    val shape: Shape,
    val palette: Palette,
) {

    enum class Palette(val colours: List<Vector3f>) {
        BUNT(listOf(Vector3f(1f, 0.16f, 0.1f), Vector3f(0.2f, 1f, 0.3f), Vector3f(1f, 0.7f, 0.18f), Vector3f(0.25f, 0.45f, 1f))),
        WARM(listOf(Vector3f(1f, 0.74f, 0.38f))),
        KALT(listOf(Vector3f(0.7f, 0.86f, 1f))),
        ROT_GRUEN(listOf(Vector3f(1f, 0.16f, 0.1f), Vector3f(0.2f, 1f, 0.3f))),
    }

    sealed interface Shape {
        val centre: Vector3f
        val reach: Float
        val fitsTree: Boolean
        fun path(level: BlockGetter?, x: Double, y: Double, z: Double): List<Vector3f>
    }

    data class Span(val span: Vector3f, val sag: Float) : Shape {

        override val centre: Vector3f get() = Vector3f(span).mul(0.5f)
        override val reach: Float get() = span.length() * 0.5f
        override val fitsTree: Boolean get() = false

        override fun path(level: BlockGetter?, x: Double, y: Double, z: Double): List<Vector3f> {
            val steps = (span.length() * 4f).toInt().coerceIn(8, 256)
            return List(steps + 1) { at(it / steps.toFloat()) }
        }

        fun at(t: Float): Vector3f {
            val drop = sag * 4f * t * (1f - t)
            return Vector3f(span.x * t, span.y * t - drop, span.z * t)
        }
    }

    data class Spiral(val height: Float, val radius: Float, val turns: Float) : Shape {

        override val centre: Vector3f get() = Vector3f(0f, TreeFit.TALLEST * 0.5f, 0f)
        override val reach: Float get() = TreeFit.TALLEST * 0.5f + radius
        override val fitsTree: Boolean get() = true

        override fun path(level: BlockGetter?, x: Double, y: Double, z: Double): List<Vector3f> =
            TreeFit.spiral(level, x, y, z, height, radius, turns)
    }

    fun within(px: Double, py: Double, pz: Double, distance: Double): Boolean {
        val centre = shape.centre
        val dx = x + centre.x - px
        val dy = y + centre.y - py
        val dz = z + centre.z - pz
        val reach = distance + shape.reach
        return dx * dx + dy * dy + dz * dz <= reach * reach
    }

    companion object {

        private const val LONGEST = 64f
        private const val SHORTEST = 0.5f
        private const val SAG_BASE = 0.12f
        private const val SAG_SHARE = 0.045f
        private const val WIDEST = 12f

        fun of(display: Display.ItemDisplay, stack: ItemStack, tree: Boolean): Garland? {
            val floats = stack.get(DataComponents.CUSTOM_MODEL_DATA)?.floats() ?: return null
            if (floats.size < 3) return null
            val shape = if (tree) spiral(floats) else span(floats)
            shape ?: return null
            val palette = Palette.entries.getOrElse((floats.getOrNull(4) ?: 0f).toInt()) { Palette.BUNT }
            val at = display.position()
            return Garland(at.x, at.y, at.z, shape, palette)
        }

        private fun span(floats: List<Float>): Span? {
            val span = Vector3f(floats[0], floats[1], floats[2])
            val length = span.length()
            if (!length.isFinite() || length < SHORTEST || length > LONGEST) return null
            val asked = floats.getOrNull(3) ?: 0f
            val sag = if (asked > 0f) asked.coerceAtMost(length * 0.5f) else sagFor(span)
            return Span(span, sag)
        }

        private fun spiral(floats: List<Float>): Spiral? {
            val height = floats[0]
            val radius = floats[1]
            val turns = floats[2]
            if (!height.isFinite() || !radius.isFinite() || !turns.isFinite()) return null
            if (height < 0f || height > TreeFit.TALLEST || radius < 0.5f || radius > WIDEST || turns < 0f || turns > 30f) return null
            return Spiral(height, radius, turns)
        }

        fun sagFor(span: Vector3f): Float {
            val flat = sqrt(span.x * span.x + span.z * span.z)
            return SAG_BASE + flat * SAG_SHARE
        }
    }
}
