package studio.sniffa.client.light

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.attribute.EnvironmentAttributes
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import studio.sniffa.client.compat.ShaderGuard
import java.util.EnumMap
import java.util.Optional
import java.util.OptionalDouble
import kotlin.math.floor

object WarmLight {

    private const val NEAR = 40.0
    private const val FADE_FROM = 28f
    private const val MOST = 64
    private const val MERGE = 2.0
    private const val MOVE_REBUILD = 4.0
    private const val COLOUR_RANGE = 2f
    private const val SHOWN_FROM = 0.03f
    private const val GAIN = 0.34f

    private val disabled = java.lang.Boolean.getBoolean("hideandseek.noWarmLight")

    enum class Owner { LAMPS, CHAINS }

    class Source(val x: Double, val y: Double, val z: Double, val colour: Vector3f, val reach: Float, val flame: Flame, val phase: Float)

    private class Batch(val originX: Double, val originY: Double, val originZ: Double, val vertices: GpuBuffer, val indexCount: Int) : AutoCloseable {
        override fun close() = vertices.close()
    }

    private val published = EnumMap<Owner, List<Source>>(Owner::class.java)
    private var dirty = false
    private var batch: Batch? = null
    private var sampler: GpuSampler? = null
    private var depthCopy: GpuTexture? = null
    private var depthCopyView: GpuTextureView? = null

    private val SCREEN = BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_COLOR, BlendFactor.ZERO, BlendFactor.ONE)

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/warm_light"))
        .withVertexShader(Hideandseek.id("core/warmlight"))
        .withFragmentShader(Hideandseek.id("core/warmlight"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("DepthSampler").build())
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(SCREEN))
        .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN, false))
        .withCull(false)
        .build()

    fun install() {
        if (disabled) return
        RenderPipelines.register(PIPELINE)
        ClientTickEvents.END_CLIENT_TICK.register { client -> tick(client) }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.execute { forget() } }
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    fun publish(owner: Owner, sources: List<Source>) {
        if (disabled) return
        published[owner] = sources
        dirty = true
    }

    private fun forget() {
        batch?.close()
        batch = null
        published.clear()
        dirty = false
    }

    private fun tick(client: Minecraft) {
        if (client.level == null) {
            if (batch != null || published.isNotEmpty()) forget()
            return
        }
        val camera = client.gameRenderer.mainCamera().position()
        val current = batch
        val moved = current != null && (camera.x - current.originX).let { it * it } + (camera.y - current.originY).let { it * it } + (camera.z - current.originZ).let { it * it } > MOVE_REBUILD * MOVE_REBUILD
        if (!dirty && !moved) return
        dirty = false
        batch?.close()
        batch = build(camera.x, camera.y, camera.z)
    }

    private class Merged {
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var weight = 0f
        val colour = Vector3f()
        var reach = 0f
        var flame = Flame.STEADY
        var strongest = 0f
        var phase = 0f
    }

    private fun build(cx: Double, cy: Double, cz: Double): Batch? {
        val cells = HashMap<Long, Merged>()
        for (list in published.values) for (source in list) {
            val dx = source.x - cx
            val dy = source.y - cy
            val dz = source.z - cz
            val far = NEAR + source.reach
            if (dx * dx + dy * dy + dz * dz > far * far) continue
            val key = (floor(source.x / MERGE).toLong() and 0x1fffff) or ((floor(source.y / MERGE).toLong() and 0x1fffff) shl 21) or ((floor(source.z / MERGE).toLong() and 0x1fffff) shl 42)
            val cell = cells.getOrPut(key) { Merged() }
            val weight = source.colour.x * 0.3f + source.colour.y * 0.55f + source.colour.z * 0.15f + 1.0e-3f
            cell.x += source.x * weight
            cell.y += source.y * weight
            cell.z += source.z * weight
            cell.weight += weight
            cell.colour.add(source.colour)
            if (source.reach > cell.reach) cell.reach = source.reach
            if (weight > cell.strongest) {
                cell.strongest = weight
                cell.flame = source.flame
                cell.phase = source.phase
            }
        }
        if (cells.isEmpty()) return null
        val chosen = cells.values.sortedBy {
            val dx = it.x / it.weight - cx
            val dy = it.y / it.weight - cy
            val dz = it.z / it.weight - cz
            dx * dx + dy * dy + dz * dz
        }.take(MOST)
        val format = DefaultVertexFormat.POSITION_TEX_COLOR
        val buffer = ByteBufferBuilder(format.vertexSize * 4 * chosen.size).use { storage ->
            val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, format)
            for (light in chosen) {
                val x = (light.x / light.weight - cx).toFloat()
                val y = (light.y / light.weight - cy).toFloat()
                val z = (light.z / light.weight - cz).toFloat()
                val peak = maxOf(light.colour.x, light.colour.y, light.colour.z)
                val scale = if (peak > COLOUR_RANGE) 1f / peak else 1f / COLOUR_RANGE
                val code = light.flame.ordinal + 1f + light.phase * 0.98f
                for (corner in CORNERS) {
                    builder.addVertex(x, y, z)
                        .setUv(corner[0] * light.reach, corner[1] * code)
                        .setColor(light.colour.x * scale, light.colour.y * scale, light.colour.z * scale, 1f)
                }
            }
            builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa warm light" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
        }
        return Batch(cx, cy, cz, buffer, chosen.size * 6)
    }

    private val CORNERS = arrayOf(floatArrayOf(-1f, -1f), floatArrayOf(1f, -1f), floatArrayOf(1f, 1f), floatArrayOf(-1f, 1f))

    private fun sampler(): GpuSampler = sampler ?: RenderSystem.getDevice().createSampler(
        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty(),
    ).also { sampler = it }

    private fun copyOf(source: GpuTexture): GpuTextureView {
        val current = depthCopy
        if (current != null && current.getWidth(0) == source.getWidth(0) && current.getHeight(0) == source.getHeight(0) && current.format == source.format) {
            return depthCopyView!!
        }
        depthCopyView?.close()
        current?.close()
        val device = RenderSystem.getDevice()
        val fresh = device.createTexture({ "sniffa warm light depth" }, GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING, source.format, source.getWidth(0), source.getHeight(0), 1, 1)
        depthCopy = fresh
        return device.createTextureView(fresh).also { depthCopyView = it }
    }

    fun darkness(client: Minecraft): Float {
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val sky = client.gameRenderer.mainCamera().attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partial) as Float
        val night = ((0.95f - sky) / 0.6f).coerceIn(0f, 1f)
        val smooth = night * night * (3f - 2f * night)
        return smooth
    }

    private fun draw() {
        val batch = batch ?: return
        if (ShaderGuard.shaderPackActive) return
        val client = Minecraft.getInstance()
        if (client.level == null) return
        val target = client.gameRenderer.mainRenderTarget()
        val colour = target.colorTextureView ?: return
        val depth = target.depthTextureView ?: return
        val depthTexture = target.depthTexture ?: return
        val strength = darkness(client)
        if (strength < SHOWN_FROM) return
        val sceneDepth = copyOf(depthTexture)
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.copyTextureToTexture(depthTexture, depthCopy!!, 0, 0, 0, 0, 0, depthTexture.getWidth(0), depthTexture.getHeight(0))
        val camera = client.gameRenderer.mainCamera().position()
        val offset = Vector3f((batch.originX - camera.x).toFloat(), (batch.originY - camera.y).toFloat(), (batch.originZ - camera.z).toFloat())
        val modelView = RenderSystem.getModelViewMatrixCopy()
        val tint = Vector4f(1f, 1f, 1f, strength)
        val transform = RenderSystem.getDynamicUniforms().writeTransform(modelView, tint, offset, Matrix4f().zero().m00(FADE_FROM).m01(NEAR.toFloat()).m02(GAIN))
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        val indexBuffer = indices.getBuffer(batch.indexCount)
        encoder.createRenderPass({ "sniffa warm light" }, colour, Optional.empty(), depth, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.bindTexture("DepthSampler", sceneDepth, sampler())
            pass.setIndexBuffer(indexBuffer, indices.type())
            pass.setVertexBuffer(0, batch.vertices.slice())
            pass.setUniform("DynamicTransforms", transform)
            pass.drawIndexed(batch.indexCount, 1, 0, 0, 0)
        }
    }
}
