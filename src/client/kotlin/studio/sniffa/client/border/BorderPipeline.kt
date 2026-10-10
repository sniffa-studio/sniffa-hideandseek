package studio.sniffa.client.border

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.Hideandseek

object BorderPipeline {

    val RING: RenderPipeline = wall("border", "core/border", animated = true)

    val ZONE: RenderPipeline = wall("zone", "core/zonewall", animated = true)

    val SEAM: RenderPipeline = wall("seam", "core/borderseam", animated = true)

    private fun wall(name: String, fragment: String, animated: Boolean = false): RenderPipeline {
        val builder = RenderPipeline.builder()
            .withLocation(Hideandseek.id("pipeline/$name"))
            .withVertexShader(Hideandseek.id("core/border"))
            .withFragmentShader(Hideandseek.id(fragment))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)

        if (animated) {
            builder.withUniform("Globals", UniformType.UNIFORM_BUFFER)
        }

        return builder
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
            .build()
    }

    fun install() {
        RenderPipelines.register(RING)
        RenderPipelines.register(ZONE)
        RenderPipelines.register(SEAM)
    }
}
