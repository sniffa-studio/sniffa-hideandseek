package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Vector3f
import studio.sniffa.Hideandseek
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object FirRope {

    private const val RADIUS = 0.14f
    private const val RING = 7
    private const val BOW_EVERY = 2.6f
    private const val BOW_WIDTH = 0.38f
    private const val BOW_HEIGHT = 0.32f

    val ROPE: RenderPipeline = pipeline("fir_rope", "core/firgarland")
    val BOW: RenderPipeline = pipeline("fir_bow", "core/garlandbow")

    private fun pipeline(name: String, shader: String): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/$name"))
        .withVertexShader(Hideandseek.id(shader))
        .withFragmentShader(Hideandseek.id(shader))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(ROPE)
        RenderPipelines.register(BOW)
    }

    class Built(val rope: GpuBuffer, val ropeCount: Int, val bows: GpuBuffer?, val bowCount: Int) : AutoCloseable {
        override fun close() {
            rope.close()
            bows?.close()
        }
    }

    fun build(points: List<Vector3f>, light: Float): Built {
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val quads = (points.size - 1) * RING
        val rope = ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            rope(builder, points, light)
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa fir rope" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        val spots = bowSpots(points)
        val bows = if (spots.isEmpty()) null else ByteBufferBuilder(format.vertexSize * 4 * spots.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            spots.forEach { (point, slope) -> bow(builder, point, slope, light) }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa fir bows" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        return Built(rope, quads * 6, bows, spots.size * 6)
    }

    fun avoidsBows(point: Vector3f, points: List<Vector3f>): Boolean =
        bowSpots(points).none { it.first.distance(point) < BOW_WIDTH }

    private fun bowSpots(points: List<Vector3f>): List<Pair<Vector3f, Vector3f>> {
        var total = 0f
        for (index in 1 until points.size) total += points[index].distance(points[index - 1])
        val count = (total / BOW_EVERY).toInt()
        if (count <= 0) return emptyList()
        val gap = total / count
        val spots = ArrayList<Pair<Vector3f, Vector3f>>()
        var wanted = gap * 0.5f
        var walked = 0f
        for (index in 1 until points.size) {
            val from = points[index - 1]
            val to = points[index]
            val piece = from.distance(to)
            while (piece > 0f && wanted <= walked + piece && spots.size < count) {
                spots += Vector3f(from).lerp(to, (wanted - walked) / piece) to Vector3f(to).sub(from).normalize()
                wanted += gap
            }
            walked += piece
        }
        return spots
    }

    private fun rope(builder: BufferBuilder, points: List<Vector3f>, light: Float) {
        var along = 0f
        val lengths = FloatArray(points.size)
        for (index in 1 until points.size) {
            along += points[index].distance(points[index - 1])
            lengths[index] = along
        }
        val rings = points.indices.map { index ->
            val before = points[(index - 1).coerceAtLeast(0)]
            val after = points[(index + 1).coerceAtMost(points.size - 1)]
            val slope = Vector3f(after).sub(before).normalize()
            val helper = if (abs(slope.y) > 0.9f) Vector3f(1f, 0f, 0f) else Vector3f(0f, 1f, 0f)
            val a = Vector3f(slope).cross(helper).normalize()
            val b = Vector3f(a).cross(slope).normalize()
            List(RING) { step ->
                val angle = step / RING.toDouble() * 2.0 * PI
                val normal = Vector3f(a).mul(cos(angle).toFloat()).add(Vector3f(b).mul(sin(angle).toFloat()))
                val puff = RADIUS * (0.85f + 0.3f * fuzz(index * 31 + step))
                Vector3f(points[index]).add(Vector3f(normal).mul(puff)) to normal
            }
        }
        for (index in 0 until points.size - 1) {
            for (step in 0 until RING) {
                val next = (step + 1) % RING
                corner(builder, rings[index][step], lengths[index], step, light)
                corner(builder, rings[index][next], lengths[index], step + 1, light)
                corner(builder, rings[index + 1][next], lengths[index + 1], step + 1, light)
                corner(builder, rings[index + 1][step], lengths[index + 1], step, light)
            }
        }
    }

    private fun corner(builder: BufferBuilder, ring: Pair<Vector3f, Vector3f>, along: Float, step: Int, light: Float) {
        val (point, normal) = ring
        builder.addVertex(point.x, point.y, point.z)
            .setUv(along, step / RING.toFloat())
            .setColor(normal.x * 0.5f + 0.5f, normal.y * 0.5f + 0.5f, normal.z * 0.5f + 0.5f, light)
    }

    private fun bow(builder: BufferBuilder, point: Vector3f, slope: Vector3f, light: Float) {
        val side = Vector3f(slope.x, 0f, slope.z).let { if (it.lengthSquared() < 1.0e-4f) Vector3f(1f, 0f, 0f) else it.normalize() }
        val out = Vector3f(-side.z, 0f, side.x)
        val centre = Vector3f(point).add(Vector3f(out).mul(RADIUS + 0.03f)).add(0f, 0.02f, 0f)
        val corners = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))
        for (corner in corners) {
            val at = Vector3f(centre).add(Vector3f(side).mul(corner[0] * BOW_WIDTH)).add(0f, corner[1] * BOW_HEIGHT, 0f)
            builder.addVertex(at.x, at.y, at.z).setUv(corner[0], corner[1]).setColor(0.5f, 0.5f, 0.5f, light)
        }
    }

    private fun fuzz(seed: Int): Float {
        var h = seed * -0x61c88647
        h = h xor (h ushr 15)
        h *= 0x2c1b3c6d
        h = h xor (h ushr 12)
        return (h and 0xffff) / 65535f
    }
}
