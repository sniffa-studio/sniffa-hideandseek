package studio.sniffa.client.snow

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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble

object Breath {

    private const val WALK_EVERY_TICKS = 50
    private const val SPRINT_EVERY_TICKS = 30
    private const val PUFFS_PER_BREATH = 3
    private const val TICKS_BETWEEN_PUFFS = 2
    private const val LIFE_TICKS = 28f
    private const val MOVING = 0.035
    private const val SHOWN_WITHIN = 48.0
    private const val STRENGTH = 0.8f

    private class Puff(var x: Double, var y: Double, var z: Double, val dx: Double, val dy: Double, val dz: Double, val seed: Float, val light: Float) {
        var age = 0f
    }

    private val puffs = ArrayList<Puff>()
    private val pending = HashMap<Int, Int>()

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/breath"))
        .withVertexShader(Hideandseek.id("core/snowcloud"))
        .withFragmentShader(Hideandseek.id("core/snowcloud"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    fun install() {
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    fun breathes(player: Player): Boolean {
        if (player.isSpectator || player.isInvisible || player.isCrouching || player.isPassenger) return false
        val dx = player.x - player.xo
        val dz = player.z - player.zo
        return dx * dx + dz * dz > MOVING * MOVING
    }

    private fun tick(client: Minecraft) {
        val level = client.level
        if (level == null || client.isPaused) {
            if (level == null) {
                puffs.clear()
                pending.clear()
            }
            return
        }
        puffs.forEach { puff ->
            puff.age += 1f
            puff.x += puff.dx
            puff.y += puff.dy
            puff.z += puff.dz
        }
        puffs.removeIf { it.age >= LIFE_TICKS }

        val camera = client.gameRenderer.mainCamera().position()
        val firstPerson = client.options.cameraType.isFirstPerson
        val time = level.gameTime
        for (player in level.players()) {
            if (player == client.player && firstPerson) continue
            if (player.distanceToSqr(camera) > SHOWN_WITHIN * SHOWN_WITHIN) continue
            val every = if (player.isSprinting) SPRINT_EVERY_TICKS else WALK_EVERY_TICKS
            val left = pending[player.id]
            if (left == null || left <= 0) {
                if (((time + player.id * 7L) % every) == 0L && breathes(player)) pending[player.id] = PUFFS_PER_BREATH * TICKS_BETWEEN_PUFFS
                else pending.remove(player.id)
                continue
            }
            if (left % TICKS_BETWEEN_PUFFS == 0) exhale(client, player)
            pending[player.id] = left - 1
        }
    }

    private fun exhale(client: Minecraft, player: Player) {
        val level = client.level ?: return
        val eyes = player.getEyePosition(1f)
        val look = player.lookAngle
        val moveX = player.x - player.xo
        val moveZ = player.z - player.zo
        val random = level.random
        val x = eyes.x + look.x * 0.32
        val y = eyes.y - 0.12 + look.y * 0.32
        val z = eyes.z + look.z * 0.32
        val light = 0.62f + 0.38f * (level.getMaxLocalRawBrightness(BlockPos.containing(x, y, z)) / 15f)
        puffs += Puff(
            x, y, z,
            look.x * 0.03 + moveX * 0.6 + (random.nextDouble() - 0.5) * 0.01,
            0.006 + look.y * 0.02,
            look.z * 0.03 + moveZ * 0.6 + (random.nextDouble() - 0.5) * 0.01,
            random.nextFloat(),
            light,
        )
    }

    private fun draw() {
        if (puffs.isEmpty()) return
        val client = Minecraft.getInstance()
        val view = client.gameRenderer.mainCamera()
        val camera = view.position()
        val left = view.leftVector()
        val up = view.upVector()
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val sorted = puffs.sortedByDescending { it.x.minus(camera.x).let { dx -> dx * dx } + (it.y - camera.y).let { dy -> dy * dy } + (it.z - camera.z).let { dz -> dz * dz } }
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * sorted.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (puff in sorted) {
                val age = ((puff.age + partial) / LIFE_TICKS).coerceIn(0f, 1f)
                val size = 0.16f + 0.5f * age
                val fade = (1f - age).let { it * it } * STRENGTH * (age * 6f).coerceAtMost(1f)
                val px = (puff.x + puff.dx * partial - camera.x).toFloat()
                val py = (puff.y + puff.dy * partial - camera.y).toFloat()
                val pz = (puff.z + puff.dz * partial - camera.z).toFloat()
                val h = size / 2f
                val lx = left.x() * h; val ly = left.y() * h; val lz = left.z() * h
                val ux = up.x() * h; val uy = up.y() * h; val uz = up.z() * h
                builder.addVertex(px + lx - ux, py + ly - uy, pz + lz - uz).setUv(0f, 1f).setColor(puff.seed, puff.light, 0f, fade)
                builder.addVertex(px - lx - ux, py - ly - uy, pz - lz - uz).setUv(1f, 1f).setColor(puff.seed, puff.light, 0f, fade)
                builder.addVertex(px - lx + ux, py - ly + uy, pz - lz + uz).setUv(1f, 0f).setColor(puff.seed, puff.light, 0f, fade)
                builder.addVertex(px + lx + ux, py + ly + uy, pz + lz + uz).setUv(0f, 0f).setColor(puff.seed, puff.light, 0f, fade)
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa breath" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(client, vertices, sorted.size * 6)
                }
            }
        }
    }

    private fun pass(client: Minecraft, vertices: GpuBuffer, indexCount: Int) {
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), Vector4f(1f), Vector3f(), Matrix4f())
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "sniffa breath" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }
}
