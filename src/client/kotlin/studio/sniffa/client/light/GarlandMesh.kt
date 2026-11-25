package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import org.joml.Vector3f
import kotlin.math.floor

class GarlandMesh(
    val garland: Garland,
    val path: List<Vector3f>,
    val wire: GpuBuffer,
    val wireIndexCount: Int,
    val bulbs: GpuBuffer?,
    val bulbIndexCount: Int,
    val fir: FirRope.Built?,
) : AutoCloseable {

    override fun close() {
        wire.close()
        bulbs?.close()
        fir?.close()
    }

    fun sameAs(other: List<Vector3f>): Boolean =
        other.size == path.size && path.indices.all { path[it].distanceSquared(other[it]) < MOVED_SQUARED }

    companion object {

        private const val BULB_SPACING = 0.42f
        private const val BULB_DROP = 0.07f
        private const val MOVED_SQUARED = 0.0025f
        private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

        fun build(garland: Garland, path: List<Vector3f>, light: Float): GarlandMesh {
            val segments = path.size - 1
            val wire = wire(path)
            val fir = if (garland.palette == Garland.Palette.TANNE) FirRope.build(path, light) else null
            var spots = spots(path)
            if (fir != null) {
                spots = spots.filterIndexed { index, _ -> index % 2 == 0 }.filter { FirRope.avoidsBows(it, path) }
                    .mapIndexed { index, spot -> Vector3f(spot).add(0f, 0.04f + (phaseOf(index + 7) - 0.5f) * 0.1f, 0f) }
            }
            val bulbs = if (spots.isNotEmpty()) bulbs(garland, spots) else null
            return GarlandMesh(garland, path, wire, segments * 6, bulbs, spots.size * 6, fir)
        }

        private fun wire(path: List<Vector3f>): GpuBuffer {
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            val segments = path.size - 1
            val slopes = List(path.size) { index ->
                val before = path[(index - 1).coerceAtLeast(0)]
                val after = path[(index + 1).coerceAtMost(path.size - 1)]
                Vector3f(after).sub(before).let { if (it.lengthSquared() < 1.0e-8f) Vector3f(0f, 1f, 0f) else it.normalize() }
            }
            ByteBufferBuilder(format.vertexSize * 4 * segments).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                for (segment in 0 until segments) {
                    wireVertex(builder, path[segment], slopes[segment], -1f)
                    wireVertex(builder, path[segment], slopes[segment], 1f)
                    wireVertex(builder, path[segment + 1], slopes[segment + 1], 1f)
                    wireVertex(builder, path[segment + 1], slopes[segment + 1], -1f)
                }
                builder.buildOrThrow().use { mesh ->
                    return RenderSystem.getDevice().createBuffer({ "sniffa garland wire" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                }
            }
        }

        private fun wireVertex(builder: BufferBuilder, point: Vector3f, slope: Vector3f, side: Float) {
            builder.addVertex(point.x, point.y, point.z)
                .setUv(side, 0f)
                .setColor(slope.x * 0.5f + 0.5f, slope.y * 0.5f + 0.5f, slope.z * 0.5f + 0.5f, 1f)
        }

        fun spots(path: List<Vector3f>, spacing: Float = BULB_SPACING): List<Vector3f> {
            var total = 0f
            for (index in 1 until path.size) total += path[index].distance(path[index - 1])
            val count = floor(total / spacing).toInt()
            if (count <= 0) return emptyList()
            val gap = total / count
            val spots = ArrayList<Vector3f>(count)
            var wanted = gap * 0.5f
            var walked = 0f
            for (index in 1 until path.size) {
                val from = path[index - 1]
                val to = path[index]
                val piece = from.distance(to)
                while (piece > 0f && wanted <= walked + piece && spots.size < count) {
                    spots += Vector3f(from).lerp(to, (wanted - walked) / piece)
                    wanted += gap
                }
                walked += piece
            }
            return spots
        }

        private fun bulbs(garland: Garland, spots: List<Vector3f>): GpuBuffer {
            val format = DefaultVertexFormat.POSITION_TEX_COLOR
            val colours = garland.palette.colours
            ByteBufferBuilder(format.vertexSize * 4 * spots.size).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                spots.forEachIndexed { index, point ->
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
