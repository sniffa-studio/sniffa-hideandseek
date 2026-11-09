package studio.sniffa.client.border

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class AuroraMesh private constructor(
    val vertices: GpuBuffer,
    val indexCount: Int,
    val ring: BorderZone.Ring,
) : AutoCloseable {

    fun scaleFor(other: BorderZone.Ring): Double? {
        if (ring.centerX != other.centerX || ring.centerZ != other.centerZ || ring.radius <= 0.0) return null
        val scale = other.radius / ring.radius
        return if (scale in LEAST_SCALE..MOST_SCALE) scale else null
    }

    override fun close() {
        vertices.close()
    }

    private class Band(val angle: Double, val offset: Double, val bottom: Double, val height: Double)

    companion object {

        private const val SEGMENTS = 160
        private const val LENGTH_SHARE = 1.55
        private const val INSIDE_SHARE = 0.9
        private const val FADE_FROM_SHARE = 0.72
        private const val SWAY_SHARE = 0.09
        private const val RIPPLE_SHARE = 0.03
        private const val DOME_CLEARANCE = 14.0
        private const val SEED_SPACING = 10000.0

        private const val LEAST_SCALE = 0.45
        private const val MOST_SCALE = 2.2

        private const val TAU = 2.0 * Math.PI

        private val BANDS = listOf(
            Band(0.35, -0.38, 168.0, 64.0),
            Band(1.15, -0.08, 178.0, 78.0),
            Band(0.62, 0.2, 172.0, 70.0),
            Band(1.9, 0.44, 184.0, 58.0),
        )

        fun build(ring: BorderZone.Ring): AuroraMesh {
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            val radius = ring.radius
            val quads = BANDS.size * SEGMENTS

            ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                var written = 0

                BANDS.forEachIndexed { index, band ->
                    val dirX = cos(band.angle)
                    val dirZ = sin(band.angle)
                    val normalX = -dirZ
                    val normalZ = dirX
                    val centreX = normalX * band.offset * radius
                    val centreZ = normalZ * band.offset * radius
                    val length = LENGTH_SHARE * radius

                    fun at(step: Int): DoubleArray {
                        val s = step.toDouble() / SEGMENTS
                        val along = (s - 0.5) * length
                        val sway = sin(s * TAU * 1.3 + index) * SWAY_SHARE * radius +
                            sin(s * TAU * 3.1 + index * 2.0) * RIPPLE_SHARE * radius
                        val x = centreX + dirX * along + normalX * sway
                        val z = centreZ + dirZ * along + normalZ * sway
                        return doubleArrayOf(x, z, s, along + length / 2.0)
                    }

                    for (step in 0 until SEGMENTS) {
                        val from = at(step)
                        val to = at(step + 1)
                        val reachFrom = hypot(from[0], from[1])
                        val reachTo = hypot(to[0], to[1])
                        if (reachFrom > INSIDE_SHARE * radius || reachTo > INSIDE_SHARE * radius) continue

                        val seed = index * SEED_SPACING
                        val fadeFrom = edgeFade(reachFrom, radius)
                        val fadeTo = edgeFade(reachTo, radius)
                        val topFrom = top(band, reachFrom, radius)
                        val topTo = top(band, reachTo, radius)

                        val nx = (normalX * 0.5 + 0.5).toFloat()
                        val nz = (normalZ * 0.5 + 0.5).toFloat()

                        builder.addVertex(from[0].toFloat(), band.bottom.toFloat(), from[1].toFloat())
                            .setUv((seed + from[3]).toFloat(), 0f).setColor(nx, nz, fadeFrom, from[2].toFloat())
                        builder.addVertex(to[0].toFloat(), band.bottom.toFloat(), to[1].toFloat())
                            .setUv((seed + to[3]).toFloat(), 0f).setColor(nx, nz, fadeTo, to[2].toFloat())
                        builder.addVertex(to[0].toFloat(), topTo.toFloat(), to[1].toFloat())
                            .setUv((seed + to[3]).toFloat(), 1f).setColor(nx, nz, fadeTo, to[2].toFloat())
                        builder.addVertex(from[0].toFloat(), topFrom.toFloat(), from[1].toFloat())
                            .setUv((seed + from[3]).toFloat(), 1f).setColor(nx, nz, fadeFrom, from[2].toFloat())
                        written++
                    }
                }

                builder.buildOrThrow().use {
                    val buffer = RenderSystem.getDevice().createBuffer(
                        { "sniffa aurora" },
                        GpuBuffer.USAGE_VERTEX,
                        it.vertexBuffer(),
                    )
                    return AuroraMesh(buffer, written * 6, ring)
                }
            }
        }

        private fun edgeFade(reach: Double, radius: Double): Float {
            val from = FADE_FROM_SHARE * radius
            val to = INSIDE_SHARE * radius
            val t = ((reach - from) / (to - from)).coerceIn(0.0, 1.0)
            return (1.0 - t * t * (3.0 - 2.0 * t)).toFloat()
        }

        private fun top(band: Band, reach: Double, radius: Double): Double {
            val dome = BorderMesh.DOME_BASE + sqrt((radius * radius - reach * reach).coerceAtLeast(0.0))
            return minOf(band.bottom + band.height, dome - DOME_CLEARANCE).coerceAtLeast(band.bottom + 8.0)
        }
    }
}
