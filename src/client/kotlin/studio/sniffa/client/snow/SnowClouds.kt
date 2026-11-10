package studio.sniffa.client.snow

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.particles.ParticleTypes
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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object SnowClouds {

    private const val PUFFS = 110
    private const val SMALLEST = 1.4f
    private const val LARGEST = 3.0f
    private const val SWAY = 0.35
    private const val FLAKES_PER_TICK = 6
    private const val GROW_MILLIS = 600L
    private const val THIN_MILLIS = 1500L
    private const val INSIDE_ALPHA = 0.94f
    private const val EDGE_SHARE = 0.35f
    private const val DRIFT = 0.02

    private class Puff(val dx: Double, val dy: Double, val dz: Double, val size: Float, val seed: Float, val light: Float, val phase: Double)

    private class Cloud(val x: Double, val y: Double, val z: Double, val radius: Double, val bornAt: Long, val endsAt: Long,
                        val puffs: List<Puff>)

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/snowcloud"))
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

    private val clouds = CopyOnWriteArrayList<Cloud>()

    fun install() {
        RenderPipelines.register(PIPELINE)
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { drawPuffs() })
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Hideandseek.id("snow_cloud")) { graphics, _ ->
            draw(graphics)
        }
    }

    fun accept(x: Double, y: Double, z: Double, radius: Float, seconds: Int) {
        val now = System.currentTimeMillis()
        val random = Random(now)
        val puffs = List(PUFFS) {
            val u = random.nextDouble()
            val theta = random.nextDouble() * Math.PI * 2.0
            val phi = Math.acos(2.0 * random.nextDouble() - 1.0)
            val r = radius * 0.78 * cbrt(u)
            Puff(r * sin(phi) * cos(theta), r * cos(phi) * 0.7, r * sin(phi) * sin(theta),
                SMALLEST + random.nextFloat() * (LARGEST - SMALLEST), random.nextFloat(), 0.86f + random.nextFloat() * 0.14f,
                random.nextDouble() * Math.PI * 2.0)
        }
        clouds.add(Cloud(x, y, z, radius.toDouble(), now, now + seconds * 1000L, puffs))
    }

    fun forget() {
        clouds.clear()
    }

    private fun strength(cloud: Cloud, now: Long): Float {
        val grow = ((now - cloud.bornAt).toFloat() / GROW_MILLIS).coerceIn(0f, 1f)
        val thin = ((cloud.endsAt - now).toFloat() / THIN_MILLIS).coerceIn(0f, 1f)
        return minOf(grow, thin)
    }

    private fun tick(client: Minecraft) {
        val now = System.currentTimeMillis()
        clouds.removeIf { now >= it.endsAt }
        val level = client.level ?: return
        val random = level.random
        for (cloud in clouds) {
            val strength = strength(cloud, now)
            repeat((FLAKES_PER_TICK * strength).toInt()) {
                val x = cloud.x + (random.nextDouble() * 2.0 - 1.0) * cloud.radius
                val y = cloud.y + (random.nextDouble() * 2.0 - 1.0) * cloud.radius * 0.7
                val z = cloud.z + (random.nextDouble() * 2.0 - 1.0) * cloud.radius
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (random.nextDouble() - 0.5) * DRIFT, -DRIFT, (random.nextDouble() - 0.5) * DRIFT)
            }
        }
    }

    private fun drawPuffs() {
        if (clouds.isEmpty()) return
        val client = Minecraft.getInstance()
        val view = client.gameRenderer.mainCamera()
        val camera = view.position()
        val left = view.leftVector()
        val up = view.upVector()
        val now = System.currentTimeMillis()
        val seconds = now / 1000.0

        class Placed(val x: Float, val y: Float, val z: Float, val size: Float, val seed: Float, val light: Float, val fade: Float, val far: Float)
        val placed = ArrayList<Placed>()
        for (cloud in clouds) {
            val strength = strength(cloud, now)
            val grow = 0.45f + 0.55f * strength
            for (puff in cloud.puffs) {
                val sway = sin(seconds * 0.6 + puff.phase) * SWAY
                val x = (cloud.x + puff.dx * grow + sway - camera.x).toFloat()
                val y = (cloud.y + puff.dy * grow + sway * 0.4 - camera.y).toFloat()
                val z = (cloud.z + puff.dz * grow + cos(seconds * 0.5 + puff.phase) * SWAY - camera.z).toFloat()
                placed.add(Placed(x, y, z, puff.size * grow, puff.seed, puff.light, strength, x * x + y * y + z * z))
            }
        }
        placed.sortByDescending { it.far }

        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        ByteBufferBuilder(format.vertexSize * 4 * placed.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (p in placed) {
                val h = p.size / 2f
                val lx = left.x() * h; val ly = left.y() * h; val lz = left.z() * h
                val ux = up.x() * h; val uy = up.y() * h; val uz = up.z() * h
                builder.addVertex(p.x + lx - ux, p.y + ly - uy, p.z + lz - uz).setUv(0f, 1f).setColor(p.seed, p.light, 0f, p.fade)
                builder.addVertex(p.x - lx - ux, p.y - ly - uy, p.z - lz - uz).setUv(1f, 1f).setColor(p.seed, p.light, 0f, p.fade)
                builder.addVertex(p.x - lx + ux, p.y - ly + uy, p.z - lz + uz).setUv(1f, 0f).setColor(p.seed, p.light, 0f, p.fade)
                builder.addVertex(p.x + lx + ux, p.y + ly + uy, p.z + lz + uz).setUv(0f, 0f).setColor(p.seed, p.light, 0f, p.fade)
            }
            builder.buildOrThrow().use { mesh ->
                RenderSystem.getDevice().createBuffer({ "sniffa snow cloud" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { vertices ->
                    pass(vertices, placed.size * 6)
                }
            }
        }
    }

    private fun pass(vertices: GpuBuffer, indexCount: Int) {
        val client = Minecraft.getInstance()
        val target = client.levelRenderer.weatherTarget() ?: client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrixCopy(), Vector4f(1f, 1f, 1f, 1f), Vector3f(), Matrix4f())
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "sniffa snow cloud" }, colour, Optional.empty(), depth, OptionalDouble.empty(),
        ).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.setIndexBuffer(indices.getBuffer(indexCount), indices.type())
            pass.setVertexBuffer(0, vertices.slice())
            pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }

    private fun draw(graphics: GuiGraphicsExtractor) {
        if (clouds.isEmpty()) return
        val client = Minecraft.getInstance()
        val eye = client.gameRenderer.mainCamera().position()
        val now = System.currentTimeMillis()
        var thickest = 0f
        for (cloud in clouds) {
            val dx = eye.x - cloud.x
            val dy = (eye.y - cloud.y) / 0.7
            val dz = eye.z - cloud.z
            val depth = 1.0 - sqrt(dx * dx + dy * dy + dz * dz) / cloud.radius
            if (depth <= 0.0) continue
            val inside = (depth / EDGE_SHARE).coerceIn(0.0, 1.0).toFloat()
            thickest = maxOf(thickest, inside * strength(cloud, now))
        }
        if (thickest <= 0f) return
        val alpha = (thickest * INSIDE_ALPHA * 255f).toInt().coerceIn(0, 255)
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha shl 24) or 0xE8F0FA)
    }
}
