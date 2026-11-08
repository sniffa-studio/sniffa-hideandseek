package studio.sniffa.client.radar

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import studio.sniffa.Hideandseek

object ScanPipeline {

    val PING: RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/scan"))
        .withVertexShader(Hideandseek.id("core/scan"))
        .withFragmentShader(Hideandseek.id("core/scan"))
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("DepthSampler").build())
        .withVertexBinding(0, DefaultVertexFormat.POSITION)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .withColorTargetState(ColorTargetState(BlendFunction.ADDITIVE))
        .withCull(false)
        .build()

    fun install() {
        RenderPipelines.register(PING)
    }
}
