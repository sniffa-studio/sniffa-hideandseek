package studio.sniffa.client.border

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

object BorderSeam {

    class Mesh(val vertices: GpuBuffer, val indexCount: Int, val reach: Double)

    private class Sample(val x: Float, val z: Float, val inner: Vec3, val outer: Vec3, val ground: Float, val along: Float)

    private const val STEP_BLOCKS = 1.0
    private const val HEIGHT_BLOCKS = 1.6
    private const val HALF_WIDTH = 1.2
    private const val LIFT = 0.02f
    private const val ACROSS = 2f

    private const val REBUILD_MILLIS = 1000L
    private const val MOVE_BLOCKS = 6.0
    private const val RADIUS_BLOCKS = 0.5
    private const val LEAF_DEPTH = 48
    private const val REACH_BLOCKS = 96.0

    private var mesh: Mesh? = null
    private var builtFor: BorderZone.Ring? = null
    private var builtFrom: Vec3 = Vec3.ZERO
    private var builtAt = 0L

    fun meshFor(ring: BorderZone.Ring, camera: Vec3): Mesh? {
        val now = System.currentTimeMillis()
        val built = builtFor

        val stale = built == null ||
            built.centerX != ring.centerX ||
            built.centerZ != ring.centerZ ||
            abs(built.radius - ring.radius) >= RADIUS_BLOCKS ||
            hypot(camera.x - builtFrom.x, camera.z - builtFrom.z) >= MOVE_BLOCKS ||
            now - builtAt >= REBUILD_MILLIS

        if (stale) {
            drop()
            mesh = build(ring, camera)
            builtFor = ring
            builtFrom = camera
            builtAt = now
        }

        return mesh
    }

    fun drop() {
        mesh?.vertices?.close()
        mesh = null
        builtFor = null
    }

    fun uniforms(reach: Double): Matrix4f {
        val values = FloatArray(16)
        values[3] = when (BorderZone.motion()) {
            BorderZone.Motion.SHRINKING -> -1f
            BorderZone.Motion.GROWING -> 1f
            BorderZone.Motion.STATIONARY -> 0f
        }
        values[12] = reach.toFloat()
        return Matrix4f().set(values)
    }

    private fun build(ring: BorderZone.Ring, camera: Vec3): Mesh? {
        val client = Minecraft.getInstance()
        val level = client.level ?: return null

        val reach = minOf(client.options.renderDistance().get() * 16.0, REACH_BLOCKS)
        val samples = sample(level, ring, camera, reach)

        val pairs = samples.zipWithNext().count { (a, b) -> a != null && b != null }
        if (pairs == 0) return null

        val format = DefaultVertexFormat.POSITION_TEX
        val quads = pairs * 2

        ByteBufferBuilder(format.vertexSize * 4 * quads).use { storage ->
            val builder = BufferBuilder(storage, VertexFormat.Mode.QUADS, format)

            for ((from, to) in samples.zipWithNext()) {
                if (from == null || to == null) continue

                builder.addVertex(from.x, from.ground, from.z).setUv(from.along, 0f)
                builder.addVertex(to.x, to.ground, to.z).setUv(to.along, 0f)
                builder.addVertex(to.x, to.ground + HEIGHT_BLOCKS.toFloat(), to.z).setUv(to.along, ACROSS)
                builder.addVertex(from.x, from.ground + HEIGHT_BLOCKS.toFloat(), from.z).setUv(from.along, ACROSS)

                builder.addVertex(from.inner.x.toFloat(), from.ground + LIFT, from.inner.z.toFloat()).setUv(from.along, -ACROSS)
                builder.addVertex(to.inner.x.toFloat(), to.ground + LIFT, to.inner.z.toFloat()).setUv(to.along, -ACROSS)
                builder.addVertex(to.outer.x.toFloat(), to.ground + LIFT, to.outer.z.toFloat()).setUv(to.along, ACROSS)
                builder.addVertex(from.outer.x.toFloat(), from.ground + LIFT, from.outer.z.toFloat()).setUv(from.along, ACROSS)
            }

            builder.buildOrThrow().use {
                val buffer = RenderSystem.getDevice().createBuffer(
                    { "sniffa border seam" },
                    GpuBuffer.USAGE_VERTEX,
                    it.vertexBuffer(),
                )
                return Mesh(buffer, quads * 6, reach)
            }
        }
    }

    private fun sample(level: ClientLevel, ring: BorderZone.Ring, camera: Vec3, reach: Double): List<Sample?> {
        val radius = ring.radius
        if (radius <= 0.0) return emptyList()

        val dx = camera.x - ring.centerX
        val dz = camera.z - ring.centerZ
        val away = hypot(dx, dz)
        val facing = atan2(dz, dx)

        val spread = if (away < 1.0e-6) {
            Math.PI
        } else {
            val cosine = (radius * radius + away * away - reach * reach) / (2.0 * radius * away)
            if (cosine > 1.0) return emptyList()
            acos(cosine.coerceAtLeast(-1.0))
        }

        val step = STEP_BLOCKS / radius
        val first = floor((facing - spread) / step).toLong()
        val last = ceil((facing + spread) / step).toLong()
        val pos = BlockPos.MutableBlockPos()

        return (first..last).map { index ->
            val angle = index * step
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            val ground = ground(level, floor(ring.centerX + x).toInt(), floor(ring.centerZ + z).toInt(), pos)

            ground?.let {
                Sample(
                    x.toFloat(),
                    z.toFloat(),
                    Vec3(cos(angle) * (radius - HALF_WIDTH), 0.0, sin(angle) * (radius - HALF_WIDTH)),
                    Vec3(cos(angle) * (radius + HALF_WIDTH), 0.0, sin(angle) * (radius + HALF_WIDTH)),
                    it.toFloat(),
                    (angle * radius).toFloat(),
                )
            }
        }
    }

    private fun ground(level: ClientLevel, x: Int, z: Int, pos: BlockPos.MutableBlockPos): Int? {
        if (!level.hasChunk(x shr 4, z shr 4)) return null

        var y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
        if (y <= level.minY) return null

        var depth = 0
        while (y > level.minY && depth < LEAF_DEPTH) {
            val below = level.getBlockState(pos.set(x, y - 1, z))
            if (!below.isAir && !below.`is`(BlockTags.LEAVES)) break
            y--
            depth++
        }

        return y
    }
}
