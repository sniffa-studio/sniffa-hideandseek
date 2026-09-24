package studio.sniffa.client.radar

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.Hideandseek

object ScanPipeline {

    val PING: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/scan"))
        .withVertexShader(Hideandseek.id("core/scan"))
        .withFragmentShader(Hideandseek.id("core/scan"))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withSampler("DepthSampler")
        .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.ADDITIVE)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build()

    fun install() {
        RenderPipelines.register(PING)
    }
}
