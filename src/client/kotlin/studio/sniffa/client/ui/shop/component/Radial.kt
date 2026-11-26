package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.Color
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

object Radial {

    private const val FLATNESS = 0.12

    fun x(centerX: Number, degrees: Double, radius: Double): Double =
        centerX.toDouble() - cos(Math.toRadians(degrees)) * radius

    fun y(centerY: Number, degrees: Double, radius: Double): Double =
        centerY.toDouble() - sin(Math.toRadians(degrees)) * radius

    fun degreesOf(dx: Double, dy: Double): Double = Math.toDegrees(Math.atan2(-dy, -dx))

    fun segmentsFor(spanDegrees: Double, radius: Double): Int {
        val widest = 2 * acos((1.0 - FLATNESS / radius.coerceAtLeast(1.0)).coerceIn(-1.0, 1.0))
        return ceil(Math.toRadians(spanDegrees) / widest).toInt().coerceAtLeast(1)
    }

    fun ink(argb: Int, alpha: Float = 1f): Color = Color.ofArgb(fade(argb, alpha))

    fun fade(argb: Int, alpha: Float): Int {
        val a = ((argb ushr 24) * alpha.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }

    fun blend(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return (channel(from, to, t, 24) shl 24) or
            (channel(from, to, t, 16) shl 16) or
            (channel(from, to, t, 8) shl 8) or
            channel(from, to, t, 0)
    }

    private fun channel(from: Int, to: Int, t: Float, shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }
}
