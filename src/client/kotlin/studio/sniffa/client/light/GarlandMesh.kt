package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import kotlin.math.ceil
import kotlin.math.floor

class GarlandMesh(
    val garland: Garland,
    val wire: GpuBuffer,
    val wireIndexCount: Int,
    val bulbs: GpuBuffer?,
    val bulbIndexCount: Int,
) : AutoCloseable {

    override fun close() {
        wire.close()
        bulbs?.close()
    }

    companion object {

        private const val SEGMENTS_PER_BLOCK = 3f
        private const val FEWEST_SEGMENTS = 6
        private const val MOST_SEGMENTS = 192
        private const val BULB_SPACING = 0.42f
        private const val BULB_DROP = 0.07f
        private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

        fun build(garland: Garland): GarlandMesh {
            val segments = ceil(garland.length * SEGMENTS_PER_BLOCK).toInt().coerceIn(FEWEST_SEGMENTS, MOST_SEGMENTS)
            val wire = wire(garland, segments)
            val count = floor(garland.length / BULB_SPACING).toInt()
            val bulbs = if (count > 0) bulbs(garland, count) else null
            return GarlandMesh(garland, wire, segments * 6, bulbs, count * 6)
        }

        private fun wire(garland: Garland, segments: Int): GpuBuffer {
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            ByteBufferBuilder(format.vertexSize * 4 * segments).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                for (segment in 0 until segments) {
                    val t0 = segment / segments.toFloat()
                    val t1 = (segment + 1) / segments.toFloat()
                    wireVertex(builder, garland, t0, -1f)
                    wireVertex(builder, garland, t0, 1f)
                    wireVertex(builder, garland, t1, 1f)
                    wireVertex(builder, garland, t1, -1f)
                }
                builder.buildOrThrow().use { mesh ->
                    return RenderSystem.getDevice().createBuffer({ "sniffa garland wire" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                }
            }
        }

        private fun wireVertex(builder: BufferBuilder, garland: Garland, t: Float, side: Float) {
            val point = garland.at(t)
            val slope = garland.slope(t)
            builder.addVertex(point.x, point.y, point.z)
                .setUv(side, t)
                .setColor(slope.x * 0.5f + 0.5f, slope.y * 0.5f + 0.5f, slope.z * 0.5f + 0.5f, 1f)
        }

        private fun bulbs(garland: Garland, count: Int): GpuBuffer {
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            val colours = garland.palette.colours
            ByteBufferBuilder(format.vertexSize * 4 * count).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                for (index in 0 until count) {
                    val point = garland.at((index + 0.5f) / count)
                    val colour = colours[index % colours.size]
                    val phase = phaseOf(index)
                    for (corner in CORNERS) {
                        builder.addVertex(point.x, point.y - BULB_DROP, point.z)
                            .setUv(corner[0], corner[1])
                            .setColor(colour.x, colour.y, colour.z, phase)
                    }
                }
                builder.buildOrThrow().use { mesh ->
                    return RenderSystem.getDevice().createBuffer({ "sniffa garland bulbs" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                }
            }
        }

        fun phaseOf(index: Int): Float {
            var h = index * -0x61c88647
            h = h xor (h ushr 15)
            h *= 0x2c1b3c6d
            h = h xor (h ushr 12)
            return (h and 0xffff) / 65535f
        }
    }
}
