package studio.sniffa.client.border

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import kotlin.math.cos
import kotlin.math.sin

class BorderMesh private constructor(
    val vertices: GpuBuffer,
    val indexCount: Int,
    private val ring: BorderZone.Ring,
) : AutoCloseable {

    fun matches(other: BorderZone.Ring): Boolean = ring == other

    override fun close() {
        vertices.close()
    }

    companion object {

        private const val SEGMENTS = 192

        private const val TAU = 2.0 * Math.PI

        private const val CELL_BLOCKS = 8.0

        private const val PERIOD_CELLS = 1.7320508

        fun build(ring: BorderZone.Ring): BorderMesh {
            val format = DefaultVertexFormat.POSITION_TEX
            val quads = SEGMENTS
            val bytes = format.vertexSize * 4 * quads

            ByteBufferBuilder(bytes).use { storage ->
                val builder = BufferBuilder(storage, VertexFormat.Mode.QUADS, format)

                val step = TAU / SEGMENTS

                val circumferenceCells = TAU * ring.radius / CELL_BLOCKS
                val tiles = Math.round(circumferenceCells / PERIOD_CELLS).coerceAtLeast(1L)
                val uPerSegment = tiles * PERIOD_CELLS / SEGMENTS

                val bottom = BorderZone.BOTTOM.toFloat()
                val top = BorderZone.TOP.toFloat()
                val vBottom = (BorderZone.BOTTOM / CELL_BLOCKS).toFloat()
                val vTop = (BorderZone.TOP / CELL_BLOCKS).toFloat()

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
                }

                val mesh = builder.buildOrThrow()
                mesh.use {
                    val buffer = RenderSystem.getDevice().createBuffer(
                        { "sniffa border ring" },
                        GpuBuffer.USAGE_VERTEX,
                        it.vertexBuffer(),
                    )
                    return BorderMesh(buffer, quads * 6, ring)
                }
            }
        }
    }
}
