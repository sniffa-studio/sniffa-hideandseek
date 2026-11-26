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
import net.minecraft.world.entity.HumanoidArm
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

    private const val RANGE = 12.0
    private const val OWN_RANGE = 12.0
    private const val AHEAD = 16f
    private const val REBUILD_TICKS = 8
    private const val MOST_FACES = 12_000
    private const val FADE_IN_MILLIS = 400L
    private const val FADE_OUT_MILLIS = 900L
    private const val LIFT_GAIN = 2.0f
    private const val GLOW_GAIN = 0.54f
    private const val HALO_SIZE = 0.42f
    private const val HALO_GAIN = 0.85f
    private const val SOURCE_LIFT = 0.9f
    private const val SHADOW_LIFT = 0.5
    private const val FACE_LIFT = 0.004
    private const val HAND_SIDE = 0.36
    private const val HAND_AHEAD = 0.08
    private const val HAND_LOW = 0.38
    private val FULL = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    private val WARM = Vector3f(1f, 0.76f, 0.44f)
    private val CANDLE = Vector3f(1f, 0.67f, 0.3f)

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
    private val HALO: RenderPipeline = pipeline("lantern_halo", "core/lanternhalo", ADDITIVE, DefaultVertexFormat.POSITION_TEX_COLOR)

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
        val side = if (player.mainArm == HumanoidArm.RIGHT) 1.0 else -1.0
        val handX = cos(yaw) * side
        val handZ = sin(yaw) * side
        val forwardX = -sin(yaw)
        val forwardZ = cos(yaw)
        return Vec3(at.x + handX * HAND_SIDE + forwardX * HAND_AHEAD, at.y + HAND_LOW, at.z + handZ * HAND_SIDE + forwardZ * HAND_AHEAD)
    }

    private class Face(val box: AABB, val side: Direction, val seen: Float)

    private fun rebuild(level: ClientLevel, glow: Glow, lamp: Vec3, centre: BlockPos, range: Double) {
        glow.close()
        glow.origin = centre
        glow.builtAt = centre
        val eye = Vec3(lamp.x, lamp.y + SHADOW_LIFT, lamp.z)
        val reach = range.toInt()
        val faces = ArrayList<Face>()
        val corners = HashMap<Long, FloatArray>()
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
                val towards = (eye.x - faceX) * side.stepX + (eye.y - faceY) * side.stepY + (eye.z - faceZ) * side.stepZ
                if (towards <= 0.0) continue
                val seen = if (seen(level, eye, pos, side, faceX, faceY, faceZ)) 1f else 0f
                for (part in shape.toAabbs().ifEmpty { listOf(FULL) }) {
                    val box = part.move(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                    faces += Face(box, side, seen)
                    eachCorner(box, side) { x, y, z ->
                        val sum = corners.getOrPut(cornerKey(centre, x, y, z, side)) { FloatArray(2) }
                        sum[0] += seen
                        sum[1] += 1f
                    }
                }
                if (faces.size >= MOST_FACES) break@outer
            }
        }
        val lit = faces.filter { face ->
            var any = false
            eachCorner(face.box, face.side) { x, y, z -> if (shade(corners, centre, x, y, z, face.side) > 0f) any = true }
            any
        }
        if (lit.isEmpty()) return
        val format = DefaultVertexFormat.POSITION_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * lit.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (face in lit) face(builder, centre, face.box, face.side, corners)
            builder.buildOrThrow().use { mesh ->
                glow.mesh = RenderSystem.getDevice().createBuffer({ "sniffa lantern" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())
                glow.indexCount = lit.size * 6
            }
        }
    }

    private inline fun eachCorner(box: AABB, side: Direction, action: (Double, Double, Double) -> Unit) {
        when (side.axis) {
            Direction.Axis.Y -> {
                val y = if (side == Direction.UP) box.maxY else box.minY
                action(box.minX, y, box.minZ); action(box.maxX, y, box.minZ); action(box.maxX, y, box.maxZ); action(box.minX, y, box.maxZ)
            }
            Direction.Axis.Z -> {
                val z = if (side == Direction.SOUTH) box.maxZ else box.minZ
                action(box.minX, box.minY, z); action(box.maxX, box.minY, z); action(box.maxX, box.maxY, z); action(box.minX, box.maxY, z)
            }
            Direction.Axis.X -> {
                val x = if (side == Direction.EAST) box.maxX else box.minX
                action(x, box.minY, box.minZ); action(x, box.minY, box.maxZ); action(x, box.maxY, box.maxZ); action(x, box.maxY, box.minZ)
            }
        }
    }

    private fun cornerKey(origin: BlockPos, x: Double, y: Double, z: Double, side: Direction): Long {
        val qx = Math.round((x - origin.x) * 16.0) + 512
        val qy = Math.round((y - origin.y) * 16.0) + 512
        val qz = Math.round((z - origin.z) * 16.0) + 512
        return (qx shl 23) or (qy shl 13) or (qz shl 3) or side.ordinal.toLong()
    }

    private fun shade(corners: HashMap<Long, FloatArray>, origin: BlockPos, x: Double, y: Double, z: Double, side: Direction): Float {
        val sum = corners[cornerKey(origin, x, y, z, side)] ?: return 0f
        return if (sum[1] > 0f) sum[0] / sum[1] else 0f
    }

    private fun seen(level: ClientLevel, lamp: Vec3, pos: BlockPos, side: Direction, x: Double, y: Double, z: Double): Boolean {
        val target = Vec3(x + side.stepX * 0.02, y + side.stepY * 0.02, z + side.stepZ * 0.02)
        val hit = level.clip(ClipContext(lamp, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()))
        if (hit.type == HitResult.Type.MISS) return true
        return (hit as BlockHitResult).blockPos == pos
    }

    private fun face(builder: BufferBuilder, origin: BlockPos, box: AABB, side: Direction, corners: HashMap<Long, FloatArray>) {
        val r = side.stepX * 0.5f + 0.5f
        val g = side.stepY * 0.5f + 0.5f
        val b = side.stepZ * 0.5f + 0.5f
        val l = FACE_LIFT
        val px = side.stepX * l
        val py = side.stepY * l
        val pz = side.stepZ * l
        val points = ArrayList<DoubleArray>(4)
        eachCorner(box, side) { x, y, z -> points += doubleArrayOf(x, y, z) }
        for (p in points) {
            builder.addVertex((p[0] - origin.x + px).toFloat(), (p[1] - origin.y + py).toFloat(), (p[2] - origin.z + pz).toFloat())
                .setColor(r, g, b, shade(corners, origin, p[0], p[1], p[2], side))
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
                values.m21(SOURCE_LIFT)
                if (mine && firstPerson) {
                    val look = camera.forwardVector()
                    values.m10(look.x())
                    values.m11(look.y())
                    values.m12(look.z())
                    values.m20(AHEAD)
                }
                val tint = Vector4f(CANDLE.x * fade, CANDLE.y * fade, CANDLE.z * fade, 1f)
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
                builder.addVertex(x, y, z).setUv(corner[0] * HALO_SIZE, corner[1] * HALO_SIZE).setColor(WARM.x * fade * HALO_GAIN, WARM.y * fade * HALO_GAIN, WARM.z * fade * HALO_GAIN, 0.3f)
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
