package studio.sniffa.client.border

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.Hideandseek

object BorderPipeline {

    val RING: RenderPipeline = wall("border", "core/border", animated = true)

    val ZONE: RenderPipeline = wall("zone", "core/zonewall", animated = true)

    val SEAM: RenderPipeline = wall("seam", "core/borderseam", animated = true)

    val AURORA: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/aurora"))
        .withVertexShader(Hideandseek.id("core/aurora"))
        .withFragmentShader(Hideandseek.id("core/aurora"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.LIGHTNING))
        .withCull(false)
        .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build()

    private fun wall(name: String, fragment: String, animated: Boolean = false): RenderPipeline {
        val builder = RenderPipeline.builder()
            .withLocation(Hideandseek.id("pipeline/$name"))
            .withVertexShader(Hideandseek.id("core/border"))
            .withFragmentShader(Hideandseek.id(fragment))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)

        if (animated) {
            builder.withBindGroupLayout(BindGroupLayouts.GLOBALS)
        }

        return builder
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .build()
    }

    fun install() {
        RenderPipelines.register(RING)
        RenderPipelines.register(ZONE)
        RenderPipelines.register(SEAM)
        RenderPipelines.register(AURORA)
    }
}
