package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class ShaftMesh(val beam: Beam, val vertices: GpuBuffer, val indexCount: Int) : AutoCloseable {

    override fun close() {
        vertices.close()
    }

    companion object {

        private const val SEGMENTS = 40
        private const val RINGS = 16
        private const val LENS_RADIUS = 0.2f
        private const val SHORTEST = 3.0
        private const val OPEN_SKY_SHARE = 0.6

        fun length(level: Level, beam: Beam, reach: Double): Double {
            val from = Vec3(beam.x, beam.y, beam.z)
            val to = from.add(beam.axis.x * reach, beam.axis.y * reach, beam.axis.z * reach)
            val hit = level.clip(ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
            val length = if (hit.type == HitResult.Type.MISS) reach * OPEN_SKY_SHARE else hit.location.distanceTo(from)
            return length.coerceAtLeast(SHORTEST)
        }

        fun build(beam: Beam, length: Double, degrees: Double): ShaftMesh {
            val (side, up) = beam.across()
            val axis = beam.axis
            val spread = tan(Math.toRadians(degrees)).toFloat()
            val slope = Math.atan(spread.toDouble()).toFloat()
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            val quads = SEGMENTS * RINGS

            ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                for (ring in 0 until RINGS) {
                    val t0 = ring / RINGS.toFloat()
                    val t1 = (ring + 1) / RINGS.toFloat()
                    for (segment in 0 until SEGMENTS) {
                        val a0 = (segment / SEGMENTS.toDouble() * 2.0 * PI)
                        val a1 = ((segment + 1) / SEGMENTS.toDouble() * 2.0 * PI)
                        vertex(builder, axis, side, up, length.toFloat(), spread, slope, t0, a0)
                        vertex(builder, axis, side, up, length.toFloat(), spread, slope, t0, a1)
                        vertex(builder, axis, side, up, length.toFloat(), spread, slope, t1, a1)
                        vertex(builder, axis, side, up, length.toFloat(), spread, slope, t1, a0)
                    }
                }
                builder.buildOrThrow().use { mesh ->
                    val buffer = RenderSystem.getDevice().createBuffer({ "sniffa spotlight shaft" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                    return ShaftMesh(beam, buffer, quads * 6)
                }
            }
        }

        private fun vertex(
            builder: BufferBuilder,
            axis: Vector3f,
            side: Vector3f,
            up: Vector3f,
            length: Float,
            spread: Float,
            slope: Float,
            t: Float,
            angle: Double,
        ) {
            val distance = t * length
            val radius = LENS_RADIUS + distance * spread
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            val rx = side.x * c + up.x * s
            val ry = side.y * c + up.y * s
            val rz = side.z * c + up.z * s
            val nc = cos(slope)
            val ns = sin(slope)
            val nx = rx * nc - axis.x * ns
            val ny = ry * nc - axis.y * ns
            val nz = rz * nc - axis.z * ns
            builder.addVertex(axis.x * distance + rx * radius, axis.y * distance + ry * radius, axis.z * distance + rz * radius)
                .setUv(t, 0f)
                .setColor(nx * 0.5f + 0.5f, ny * 0.5f + 0.5f, nz * 0.5f + 0.5f, 1f)
        }
    }
}
