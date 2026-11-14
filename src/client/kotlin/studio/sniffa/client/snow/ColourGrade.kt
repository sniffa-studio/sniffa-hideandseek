package studio.sniffa.client.snow

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.attribute.EnvironmentAttributes
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import studio.sniffa.Hideandseek
import java.util.Optional
import java.util.OptionalDouble

object ColourGrade {

    private const val DAY_STRENGTH = 0.35f

    var enabled = true

    private val PIPELINE: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/colour_grade"))
        .withVertexShader(Hideandseek.id("core/colourgrade"))
        .withFragmentShader(Hideandseek.id("core/colourgrade"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("SceneSampler").build())
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState.DEFAULT)
        .withCull(false)
        .build()

    private var copy: GpuTexture? = null
    private var copyView: GpuTextureView? = null
    private var sampler: GpuSampler? = null
    private var quad: GpuBuffer? = null

    fun install() {
        RenderPipelines.register(PIPELINE)
        LevelRenderEvents.END_MAIN.register(LevelRenderEvents.EndMain { draw() })
    }

    private fun quad(): GpuBuffer = quad ?: ByteBufferBuilder(DefaultVertexFormat.POSITION.vertexSize * 4).use { storage ->
        val builder = BufferBuilder(storage, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION)
        builder.addVertex(-1f, -1f, 0f)
        builder.addVertex(1f, -1f, 0f)
        builder.addVertex(1f, 1f, 0f)
        builder.addVertex(-1f, 1f, 0f)
        builder.buildOrThrow().use { RenderSystem.getDevice().createBuffer({ "sniffa grade quad" }, GpuBuffer.USAGE_VERTEX, it.vertexBuffer()) }
    }.also { quad = it }

    private fun sampler(): GpuSampler = sampler ?: RenderSystem.getDevice().createSampler(
        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty(),
    ).also { sampler = it }

    private fun copyOf(source: GpuTexture): GpuTextureView {
        val current = copy
        if (current != null && current.getWidth(0) == source.getWidth(0) && current.getHeight(0) == source.getHeight(0) && current.format == source.format) {
            return copyView!!
        }
        copyView?.close()
        current?.close()
        val device = RenderSystem.getDevice()
        val fresh = device.createTexture({ "sniffa grade copy" }, GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING, source.format, source.getWidth(0), source.getHeight(0), 1, 1)
        copy = fresh
        return device.createTextureView(fresh).also { copyView = it }
    }

    private fun draw() {
        if (!enabled) return
        val client = Minecraft.getInstance()
        if (client.level == null) return
        val target = client.gameRenderer.mainRenderTarget()
        val source = target.colorTexture ?: return
        val colour = target.colorTextureView ?: return
        val partial = client.deltaTracker.getGameTimeDeltaPartialTick(false)
        val stars = client.gameRenderer.mainCamera().attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, partial) as Float
        val night = (stars * 2.2f).coerceIn(0f, 1f)
        val strength = DAY_STRENGTH + (1f - DAY_STRENGTH) * night

        val view = copyOf(source)
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.copyTextureToTexture(source, copy!!, 0, 0, 0, 0, 0, source.getWidth(0), source.getHeight(0))

        val transform = RenderSystem.getDynamicUniforms().writeTransform(Matrix4f(), Vector4f(1f, 1f, 1f, strength), Vector3f(), Matrix4f())
        val indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
        encoder.createRenderPass({ "sniffa colour grade" }, colour, Optional.empty()).use { pass ->
            pass.setPipeline(PIPELINE)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)
            pass.bindTexture("SceneSampler", view, sampler())
            pass.setIndexBuffer(indices.getBuffer(6), indices.type())
            pass.setVertexBuffer(0, quad().slice())
            pass.drawIndexed(6, 1, 0, 0, 0)
        }
    }
}
