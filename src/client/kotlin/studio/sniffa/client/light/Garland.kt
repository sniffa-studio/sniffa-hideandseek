package studio.sniffa.client.light

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import org.joml.Vector3f
import kotlin.math.sqrt

data class Garland(
    val x: Double,
    val y: Double,
    val z: Double,
    val span: Vector3f,
    val sag: Float,
    val palette: Palette,
) {

    enum class Palette(val colours: List<Vector3f>) {
        BUNT(listOf(Vector3f(1f, 0.16f, 0.1f), Vector3f(0.2f, 1f, 0.3f), Vector3f(1f, 0.7f, 0.18f), Vector3f(0.25f, 0.45f, 1f))),
        WARM(listOf(Vector3f(1f, 0.74f, 0.38f))),
        KALT(listOf(Vector3f(0.7f, 0.86f, 1f))),
        ROT_GRUEN(listOf(Vector3f(1f, 0.16f, 0.1f), Vector3f(0.2f, 1f, 0.3f))),
    }

    val length: Float get() = span.length()

    fun at(t: Float): Vector3f {
        val drop = sag * 4f * t * (1f - t)
        return Vector3f(span.x * t, span.y * t - drop, span.z * t)
    }

    fun slope(t: Float): Vector3f {
        val drop = sag * 4f * (1f - 2f * t)
        return Vector3f(span.x, span.y - drop, span.z).normalize()
    }

    fun within(px: Double, py: Double, pz: Double, distance: Double): Boolean {
        val dx = x + span.x * 0.5 - px
        val dy = y + span.y * 0.5 - py
        val dz = z + span.z * 0.5 - pz
        val reach = distance + length * 0.5
        return dx * dx + dy * dy + dz * dz <= reach * reach
    }

    companion object {

        private const val LONGEST = 64f
        private const val SHORTEST = 0.5f
        private const val SAG_BASE = 0.12f
        private const val SAG_SHARE = 0.045f

        fun of(display: Display.ItemDisplay, stack: net.minecraft.world.item.ItemStack): Garland? {
            val floats = stack.get(DataComponents.CUSTOM_MODEL_DATA)?.floats() ?: return null
            if (floats.size < 3) return null
            val span = Vector3f(floats[0], floats[1], floats[2])
            val length = span.length()
            if (!length.isFinite() || length < SHORTEST || length > LONGEST) return null
            val asked = floats.getOrNull(3) ?: 0f
            val sag = if (asked > 0f) asked.coerceAtMost(length * 0.5f) else sagFor(span)
            val palette = Palette.entries.getOrElse((floats.getOrNull(4) ?: 0f).toInt()) { Palette.BUNT }
            val at = display.position()
            return Garland(at.x, at.y, at.z, span, sag, palette)
        }

        fun sagFor(span: Vector3f): Float {
            val flat = sqrt(span.x * span.x + span.z * span.z)
            return SAG_BASE + flat * SAG_SHARE
        }
    }
}
