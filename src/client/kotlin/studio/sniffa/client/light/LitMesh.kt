package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

class LitMesh(val beam: Beam, val vertices: GpuBuffer, val indexCount: Int) : AutoCloseable {

    override fun close() {
        vertices.close()
    }

    companion object {

        private const val LIFT = 0.004
        private val FULL = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        fun build(level: Level, beam: Beam, faces: Map<Long, Int>): LitMesh? {
            val boxes = ArrayList<Pair<AABB, Direction>>()
            for ((key, mask) in faces) {
                val pos = LitFaces.posOf(key)
                val state = level.getBlockState(pos)
                if (state.isAir) continue
                val shape = state.getShape(level, pos)
                val parts = if (shape.isEmpty) listOf(FULL) else shape.toAabbs()
                for (bit in 0 until 6) {
                    if (mask and (1 shl bit) == 0) continue
                    val side = LitFaces.directionOf(bit)
                    for (part in parts) {
                        boxes += part.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) to side
                    }
                }
            }
            if (boxes.isEmpty()) return null

            val format = DefaultVertexFormat.POSITION_COLOR
            ByteBufferBuilder(format.vertexSize * 4 * boxes.size).use { storage ->
                val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
                for ((box, side) in boxes) face(builder, beam, box, side)
                builder.buildOrThrow().use { mesh ->
                    val buffer = RenderSystem.getDevice().createBuffer({ "sniffa spotlight" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                    return LitMesh(beam, buffer, boxes.size * 6)
                }
            }
        }

        private fun face(builder: BufferBuilder, beam: Beam, box: AABB, side: Direction) {
            val nx = side.stepX
            val ny = side.stepY
            val nz = side.stepZ
            val r = (nx * 0.5f + 0.5f)
            val g = (ny * 0.5f + 0.5f)
            val b = (nz * 0.5f + 0.5f)

            val x0 = (box.minX - beam.x).toFloat()
            val y0 = (box.minY - beam.y).toFloat()
            val z0 = (box.minZ - beam.z).toFloat()
            val x1 = (box.maxX - beam.x).toFloat()
            val y1 = (box.maxY - beam.y).toFloat()
            val z1 = (box.maxZ - beam.z).toFloat()
            val lift = LIFT.toFloat()

            when (side) {
                Direction.UP -> quad(builder, r, g, b, x0, y1 + lift, z0, x1, y1 + lift, z0, x1, y1 + lift, z1, x0, y1 + lift, z1)
                Direction.DOWN -> quad(builder, r, g, b, x0, y0 - lift, z0, x0, y0 - lift, z1, x1, y0 - lift, z1, x1, y0 - lift, z0)
                Direction.NORTH -> quad(builder, r, g, b, x0, y0, z0 - lift, x1, y0, z0 - lift, x1, y1, z0 - lift, x0, y1, z0 - lift)
                Direction.SOUTH -> quad(builder, r, g, b, x0, y0, z1 + lift, x0, y1, z1 + lift, x1, y1, z1 + lift, x1, y0, z1 + lift)
                Direction.WEST -> quad(builder, r, g, b, x0 - lift, y0, z0, x0 - lift, y1, z0, x0 - lift, y1, z1, x0 - lift, y0, z1)
                Direction.EAST -> quad(builder, r, g, b, x1 + lift, y0, z0, x1 + lift, y0, z1, x1 + lift, y1, z1, x1 + lift, y1, z0)
            }
        }

        private fun quad(
            builder: BufferBuilder, r: Float, g: Float, b: Float,
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float,
        ) {
            builder.addVertex(ax, ay, az).setColor(r, g, b, 1f)
            builder.addVertex(bx, by, bz).setColor(r, g, b, 1f)
            builder.addVertex(cx, cy, cz).setColor(r, g, b, 1f)
            builder.addVertex(dx, dy, dz).setColor(r, g, b, 1f)
        }
    }
}
