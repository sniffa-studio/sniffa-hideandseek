package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

object Lanterns {

    private const val RANGE = 9.0
    private const val OWN_RANGE = 11.0
    private const val AHEAD = 16f
    private const val REBUILD_TICKS = 6
    private const val MOST_FACES = 8_000
    private const val FADE_IN_MILLIS = 400L
    private const val FADE_OUT_MILLIS = 900L
    private const val LIFT_GAIN = 3.0f
    private const val GLOW_GAIN = 0.2f
    private const val HALO_SIZE = 0.55f
    private const val FACE_LIFT = 0.004
    private val FULL = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    private val WARM = Vector3f(1f, 0.66f, 0.32f)

    private class Glow(val until: Long, val started: Long) {
        var mesh: GpuBuffer? = null
        var indexCount = 0
        var origin: BlockPos = BlockPos.ZERO
        var builtAt: BlockPos? = null
        var age = 0

        fun close() {
            mesh?.close()
            mesh = null
            builtAt = null
        }
    }

    private val glows = HashMap<UUID, Glow>()
    private var seenLevel: ClientLevel? = null

    private val ADDITIVE = BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE)
    private val LIFT: RenderPipeline = pipeline("lantern_lift", "core/lanternlight", BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE), DefaultVertexFormat.POSITION_COLOR)
    private val GLOW: RenderPipeline = pipeline("lantern_glow", "core/lanternlight", ADDITIVE, DefaultVertexFormat.POSITION_COLOR)
    private val HALO: RenderPipeline = pipeline("lantern_halo", "core/lamphalo", ADDITIVE, DefaultVertexFormat.POSITION_TEX_COLOR)

    private fun pipeline(name: String, shader: String, blend: BlendFunction, format: VertexFormat): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/$name"))
        .withVertexShader(Hideandseek.id(shader))
        .withFragmentShader(Hideandseek.id(shader))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, format)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(blend))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(LIFT)
        RenderPipelines.register(GLOW)
        RenderPipelines.register(HALO)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    fun accept(uuid: String, millis: Int) {
        val id = runCatching { UUID.fromString(uuid) }.getOrNull() ?: return
        watchLevel(Minecraft.getInstance())
        glows.remove(id)?.close()
        if (millis <= 0) return
        val now = System.currentTimeMillis()
        glows[id] = Glow(now + millis, now)
    }

    fun forget() {
        glows.values.forEach { it.close() }
        glows.clear()
        seenLevel = null
    }

    private fun watchLevel(client: Minecraft) {
        val level = client.level
        if (level === seenLevel) return
        glows.values.forEach { it.close() }
        glows.clear()
        seenLevel = level
    }

    private fun tick(client: Minecraft) {
        watchLevel(client)
        val level = client.level
        if (level == null || glows.isEmpty()) return
        val now = System.currentTimeMillis()
        val over = glows.filterValues { now >= it.until }.keys
        for (id in over) glows.remove(id)?.close()
        for ((id, glow) in glows) {
            val player = level.getPlayerByUUID(id)
            if (player == null) {
                glow.close()
                continue
            }
            glow.age++
            val at = player.blockPosition().above()
            if (glow.builtAt != at || glow.age % REBUILD_TICKS == 0) rebuild(level, glow, lampOf(player, 1f), at, rangeOf(player))
        }
    }

    private fun own(player: Player): Boolean = player === Minecraft.getInstance().player

    private fun rangeOf(player: Player): Double = if (own(player)) OWN_RANGE else RANGE

    private fun lampOf(player: Player, partial: Float): Vec3 {
        val at = player.getPosition(partial)
        val yaw = Math.toRadians(player.getPreciseBodyRotation(partial).toDouble())
        val rightX = -cos(yaw)
        val rightZ = -sin(yaw)
        val forwardX = -sin(yaw)
        val forwardZ = cos(yaw)
        return Vec3(at.x + rightX * 0.42 + forwardX * 0.15, at.y + 0.95, at.z + rightZ * 0.42 + forwardZ * 0.15)
    }

    private fun rebuild(level: ClientLevel, glow: Glow, lamp: Vec3, centre: BlockPos, range: Double) {
        glow.close()
        glow.origin = centre
        glow.builtAt = centre
        val reach = range.toInt()
        val boxes = ArrayList<Pair<AABB, Direction>>()
        val pos = BlockPos.MutableBlockPos()
        val next = BlockPos.MutableBlockPos()
        val limit = range * range
        outer@ for (dx in -reach..reach) for (dy in -reach..reach) for (dz in -reach..reach) {
            if (dx * dx + dy * dy + dz * dz > limit) continue
            pos.set(centre.x + dx, centre.y + dy, centre.z + dz)
            val state = level.getBlockState(pos)
            if (state.isAir || state.renderShape == RenderShape.INVISIBLE) continue
            val shape = state.getCollisionShape(level, pos)
            if (shape.isEmpty) continue
            for (side in Direction.entries) {
                next.setWithOffset(pos, side)
                val neighbour = level.getBlockState(next)
                if (neighbour.isSolidRender) continue
                val faceX = pos.x + 0.5 + side.stepX * 0.5
                val faceY = pos.y + 0.5 + side.stepY * 0.5
                val faceZ = pos.z + 0.5 + side.stepZ * 0.5
                val towards = (lamp.x - faceX) * side.stepX + (lamp.y - faceY) * side.stepY + (lamp.z - faceZ) * side.stepZ
                if (towards <= 0.0) continue
                if (!seen(level, lamp, pos, side, faceX, faceY, faceZ)) continue
                for (part in shape.toAabbs().ifEmpty { listOf(FULL) }) {
                    boxes += part.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()) to side
                }
                if (boxes.size >= MOST_FACES) break@outer
            }
        }
        if (boxes.isEmpty()) return
        val format = DefaultVertexFormat.POSITION_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * boxes.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for ((box, side) in boxes) face(builder, centre, box, side)
            builder.buildOrThrow().use { mesh ->
                glow.mesh = RenderSystem.getDevice().createBuffer({ "sniffa lantern" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                glow.indexCount = boxes.size * 6
            }
        }
    }

    private fun seen(level: ClientLevel, lamp: Vec3, pos: BlockPos, side: Direction, x: Double, y: Double, z: Double): Boolean {
        val target = Vec3(x + side.stepX * 0.02, y + side.stepY * 0.02, z + side.stepZ * 0.02)
        val hit = level.clip(ClipContext(lamp, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
        if (hit.type == HitResult.Type.MISS) return true
        return (hit as BlockHitResult).blockPos == pos
    }

    private fun face(builder: BufferBuilder, origin: BlockPos, box: AABB, side: Direction) {
        val r = side.stepX * 0.5f + 0.5f
        val g = side.stepY * 0.5f + 0.5f
        val b = side.stepZ * 0.5f + 0.5f
        val x0 = (box.minX - origin.x).toFloat()
        val y0 = (box.minY - origin.y).toFloat()
        val z0 = (box.minZ - origin.z).toFloat()
        val x1 = (box.maxX - origin.x).toFloat()
        val y1 = (box.maxY - origin.y).toFloat()
        val z1 = (box.maxZ - origin.z).toFloat()
        val l = FACE_LIFT.toFloat()
        fun v(x: Float, y: Float, z: Float) = builder.addVertex(x, y, z).setColor(r, g, b, 1f)
        when (side) {
            Direction.UP -> { v(x0, y1 + l, z0); v(x1, y1 + l, z0); v(x1, y1 + l, z1); v(x0, y1 + l, z1) }
            Direction.DOWN -> { v(x0, y0 - l, z0); v(x0, y0 - l, z1); v(x1, y0 - l, z1); v(x1, y0 - l, z0) }
            Direction.NORTH -> { v(x0, y0, z0 - l); v(x1, y0, z0 - l); v(x1, y1, z0 - l); v(x0, y1, z0 - l) }
            Direction.SOUTH -> { v(x0, y0, z1 + l); v(x0, y1, z1 + l); v(x1, y1, z1 + l); v(x1, y0, z1 + l) }
            Direction.WEST -> { v(x0 - l, y0, z0); v(x0 - l, y1, z0); v(x0 - l, y1, z1); v(x0 - l, y0, z1) }
            Direction.EAST -> { v(x1 + l, y0, z0); v(x1 + l, y0, z1); v(x1 + l, y1, z1); v(x1 + l, y1, z0) }
        }
    }

    private fun strength(glow: Glow, now: Long): Float {
        val rise = ((now - glow.started) / FADE_IN_MILLIS.toFloat()).coerceIn(0f, 1f)
        val fall = ((glow.until - now) / FADE_OUT_MILLIS.toFloat()).coerceIn(0f, 1f)
        return rise * fall
    }

    private fun draw() {
        if (glows.isEmpty()) return
        val client = Minecraft.getInstance()
        val level = client.level ?: return
        val camera = client.gameRenderer.mainCamera()
        val eye = camera.position()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val now = System.currentTimeMillis()
        val firstPerson = client.options.cameraType.isFirstPerson

        for ((id, glow) in glows) {
            val player = level.getPlayerByUUID(id) ?: continue
            val fade = strength(glow, now)
            if (fade <= 0.01f) continue
            val lamp = lampOf(player, partial)
            val mine = own(player)
            val mesh = glow.mesh
            if (mesh != null) {
                val offset = Vector3f((glow.origin.x - eye.x).toFloat(), (glow.origin.y - eye.y).toFloat(), (glow.origin.z - eye.z).toFloat())
                val values = Matrix4f().zero()
                values.m00((lamp.x - glow.origin.x).toFloat())
                values.m01((lamp.y - glow.origin.y).toFloat())
                values.m02((lamp.z - glow.origin.z).toFloat())
                values.m03(rangeOf(player).toFloat())
                if (mine && firstPerson) {
                    val look = camera.forwardVector()
                    values.m10(look.x())
                    values.m11(look.y())
                    values.m12(look.z())
                    values.m20(AHEAD)
                }
                val tint = Vector4f(WARM.x * fade, WARM.y * fade, WARM.z * fade, 1f)
                pass(LIFT, mesh, glow.indexCount, offset, tint, Matrix4f(values).m13(LIFT_GAIN), colour, depth)
                pass(GLOW, mesh, glow.indexCount, offset, tint, Matrix4f(values).m13(GLOW_GAIN), colour, depth)
            }
            if (!(mine && firstPerson)) halo(lamp, eye, fade, colour, depth)
        }
    }

    private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

    private fun halo(lamp: Vec3, eye: Vec3, fade: Float, colour: GpuTextureView, depth: GpuTextureView) {
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            val x = (lamp.x - eye.x).toFloat()
            val y = (lamp.y - eye.y).toFloat()
            val z = (lamp.z - eye.z).toFloat()
            for (corner in CORNERS) {
                builder.addVertex(x, y, z).setUv(corner[0] * HALO_SIZE, corner[1] * HALO_SIZE).setColor(WARM.x * fade, WARM.y * fade, WARM.z * fade, 0.3f)
            }
            builder.buildOrThrow().use { built ->
                RenderSystem.getDevice().createBuffer({ "sniffa lantern halo" }, GpuBuffer.USAGE_VERTEX, built.vertexBuffer()).use { vertices ->
                    pass(HALO, vertices, 6, Vector3f(), Vector4f(1f), Matrix4f().zero().m00(48f).m01(64f), colour, depth)
                }
            }
        }
    }

    private fun pass(
        pipeline: RenderPipeline,
        vertices: GpuBuffer,
        indexCount: Int,
        offset: Vector3f,
        tint: Vector4f,
        values: Matrix4f,
        colour: GpuTextureView,
        depth: GpuTextureView,
    ) {
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), tint, offset, values)
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa lantern" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
