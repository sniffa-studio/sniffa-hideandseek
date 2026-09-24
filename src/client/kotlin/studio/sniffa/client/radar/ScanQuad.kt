package studio.sniffa.client.radar

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat

class ScanQuad private constructor(
    val vertices: GpuBuffer,
    val indexCount: Int,
) : AutoCloseable {

    override fun close() {
        vertices.close()
    }

    companion object {

        fun build(): ScanQuad {
            val format = DefaultVertexFormat.POSITION
            val bytes = format.vertexSize * 4

            ByteBufferBuilder(bytes).use { storage ->
                val builder = BufferBuilder(storage, VertexFormat.Mode.QUADS, format)

                builder.addVertex(-1f, -1f, 0f)
                builder.addVertex(1f, -1f, 0f)
                builder.addVertex(1f, 1f, 0f)
                builder.addVertex(-1f, 1f, 0f)

                val mesh = builder.buildOrThrow()
                mesh.use {
                    val buffer = RenderSystem.getDevice().createBuffer(
                        { "sniffa scan quad" },
                        GpuBuffer.USAGE_VERTEX,
                        it.vertexBuffer(),
                    )
                    return ScanQuad(buffer, 6)
                }
            }
        }
    }
}
