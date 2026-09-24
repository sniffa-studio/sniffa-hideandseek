package studio.sniffa.client.border

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.Hideandseek

object BorderPipeline {

    val RING: RenderPipeline = wall("border", "core/border")

    val ZONE: RenderPipeline = wall("zone", "core/zonewall")

    private fun wall(name: String, fragment: String): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/$name"))
        .withVertexShader(Hideandseek.id("core/border"))
        .withFragmentShader(Hideandseek.id(fragment))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthWrite(false)
        .build()

    fun install() {
        RenderPipelines.register(RING)
        RenderPipelines.register(ZONE)
    }
}
