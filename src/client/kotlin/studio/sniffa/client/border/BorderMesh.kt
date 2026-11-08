package studio.sniffa.client.border

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import kotlin.math.cos
import kotlin.math.sin

class BorderMesh private constructor(
    val vertices: GpuBuffer,
    val indexCount: Int,
    val columns: Int,
    val ring: BorderZone.Ring,
    val dome: Boolean,
) : AutoCloseable {

    fun matches(other: BorderZone.Ring): Boolean = ring == other

    fun scaleFor(other: BorderZone.Ring): Double? {
        if (ring.centerX != other.centerX || ring.centerZ != other.centerZ || ring.radius <= 0.0) return null
        val scale = other.radius / ring.radius
        return if (scale in LEAST_SCALE..MOST_SCALE) scale else null
    }

    override fun close() {
        vertices.close()
    }

    companion object {

        private const val SEGMENTS = 192

        private const val DOME_RINGS = 40

        const val DOME_BASE = 110.0

        private const val LEAST_SCALE = 0.45

        private const val MOST_SCALE = 2.2

        private const val TAU = 2.0 * Math.PI

        const val CELL_BLOCKS = 2.0

        const val PERIOD_CELLS = 1.7320508

        fun build(ring: BorderZone.Ring, dome: Boolean): BorderMesh {
            val format = DefaultVertexFormat.POSITION_TEX
            val quads = SEGMENTS * (if (dome) 1 + DOME_RINGS else 1)
            val bytes = format.vertexSize * 4 * quads

            ByteBufferBuilder(bytes).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)

                val step = TAU / SEGMENTS

                val circumferenceCells = TAU * ring.radius / CELL_BLOCKS
                val tiles = Math.round(circumferenceCells / PERIOD_CELLS).coerceAtLeast(1L)
                val uPerSegment = tiles * PERIOD_CELLS / SEGMENTS

                val bottom = BorderZone.BOTTOM.toFloat()
                val top = (if (dome) DOME_BASE else BorderZone.TOP).toFloat()
                val vBottom = (BorderZone.BOTTOM / CELL_BLOCKS).toFloat()
                val vTop = (top / CELL_BLOCKS).toFloat()

                for (segment in 0 until SEGMENTS) {
                    val from = segment * step
                    val to = from + step

                    val x0 = (cos(from) * ring.radius).toFloat()
                    val z0 = (sin(from) * ring.radius).toFloat()
                    val x1 = (cos(to) * ring.radius).toFloat()
                    val z1 = (sin(to) * ring.radius).toFloat()

                    val u0 = (segment * uPerSegment).toFloat()
                    val u1 = ((segment + 1) * uPerSegment).toFloat()

                    builder.addVertex(x0, bottom, z0).setUv(u0, vBottom)
                    builder.addVertex(x1, bottom, z1).setUv(u1, vBottom)
                    builder.addVertex(x1, top, z1).setUv(u1, vTop)
                    builder.addVertex(x0, top, z0).setUv(u0, vTop)

                    if (dome) {
                        for (band in 0 until DOME_RINGS) {
                            val low = band * (Math.PI / 2.0) / DOME_RINGS
                            val high = (band + 1) * (Math.PI / 2.0) / DOME_RINGS

                            val rLow = cos(low) * ring.radius
                            val rHigh = cos(high) * ring.radius
                            val yLow = (DOME_BASE + sin(low) * ring.radius).toFloat()
                            val yHigh = (DOME_BASE + sin(high) * ring.radius).toFloat()
                            val vLow = vTop + (low * ring.radius / CELL_BLOCKS).toFloat()
                            val vHigh = vTop + (high * ring.radius / CELL_BLOCKS).toFloat()

                            builder.addVertex((cos(from) * rLow).toFloat(), yLow, (sin(from) * rLow).toFloat()).setUv(u0, vLow)
                            builder.addVertex((cos(to) * rLow).toFloat(), yLow, (sin(to) * rLow).toFloat()).setUv(u1, vLow)
                            builder.addVertex((cos(to) * rHigh).toFloat(), yHigh, (sin(to) * rHigh).toFloat()).setUv(u1, vHigh)
                            builder.addVertex((cos(from) * rHigh).toFloat(), yHigh, (sin(from) * rHigh).toFloat()).setUv(u0, vHigh)
                        }
                    }
                }

                val mesh = builder.buildOrThrow()
                mesh.use {
                    val buffer = RenderSystem.getDevice().createBuffer(
                        { "sniffa border ring" },
                        GpuBuffer.USAGE_VERTEX,
                        it.vertexBuffer(),
                    )
                    return BorderMesh(buffer, quads * 6, tiles.toInt(), ring, dome)
                }
            }
        }
    }
}
