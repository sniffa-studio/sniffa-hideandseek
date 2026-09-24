package studio.sniffa.client.map

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.renderer.RenderPipelines
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import studio.sniffa.Hideandseek
import kotlin.math.ceil
import kotlin.math.floor

object MapOverlay {

    val BORDER: RenderPipeline = pipeline("border", "core/mapborder")

    val PICK: RenderPipeline = pipeline("pick", "core/mappick")

    val SHEET: RenderPipeline = pipeline("sheet", "core/mapsheet")

    val ROSE: RenderPipeline = pipeline("rose", "core/maprose")

    private fun pipeline(name: String, fragment: String): RenderPipeline = RenderPipeline.builder()
        .withLocation(Hideandseek.id("pipeline/map_$name"))
        .withVertexShader(Hideandseek.id("core/mapoverlay"))
        .withFragmentShader(Hideandseek.id(fragment))
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build()

    fun install() {
        RenderPipelines.register(BORDER)
        RenderPipelines.register(PICK)
        RenderPipelines.register(SHEET)
        RenderPipelines.register(ROSE)
    }

    fun circle(
        graphics: GuiGraphics,
        pipeline: RenderPipeline,
        bounds: ScreenRectangle,
        centreX: Double,
        centreY: Double,
        radius: Double,
        argb: Int,
        clip: ScreenRectangle? = null,
    ) {
        if (radius < 1.0 || bounds.width() <= 0 || bounds.height() <= 0) return

        graphics.guiRenderState.submitGuiElement(
            Shape(pipeline, Matrix3x2f(graphics.pose()), bounds, centreX, centreY, radius, argb, clip)
        )
    }

    fun panel(
        graphics: GuiGraphics,
        pipeline: RenderPipeline,
        bounds: ScreenRectangle,
        argb: Int,
    ) {
        if (bounds.width() <= 0 || bounds.height() <= 0) return

        graphics.guiRenderState.submitGuiElement(
            Panel(pipeline, Matrix3x2f(graphics.pose()), bounds, argb)
        )
    }

    private class Panel(
        private val pipeline: RenderPipeline,
        private val pose: Matrix3x2fc,
        private val area: ScreenRectangle,
        private val argb: Int,
    ) : GuiElementRenderState {

        override fun buildVertices(consumer: VertexConsumer) {
            corner(consumer, area.left().toFloat(), area.top().toFloat(), 0f, 0f)
            corner(consumer, area.left().toFloat(), area.bottom().toFloat(), 0f, 1f)
            corner(consumer, area.right().toFloat(), area.bottom().toFloat(), 1f, 1f)
            corner(consumer, area.right().toFloat(), area.top().toFloat(), 1f, 0f)
        }

        private fun corner(consumer: VertexConsumer, x: Float, y: Float, u: Float, v: Float) {
            consumer.addVertexWith2DPose(pose, x, y).setUv(u, v).setColor(argb)
        }

        override fun pipeline(): RenderPipeline = pipeline

        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

        override fun scissorArea(): ScreenRectangle? = null

        override fun bounds(): ScreenRectangle = area
    }

    private class Shape(
        private val pipeline: RenderPipeline,
        private val pose: Matrix3x2fc,
        private val area: ScreenRectangle,
        private val centreX: Double,
        private val centreY: Double,
        private val radius: Double,
        private val argb: Int,
        private val clip: ScreenRectangle?,
    ) : GuiElementRenderState {

        override fun buildVertices(consumer: VertexConsumer) {
            val left = area.left().toFloat()
            val top = area.top().toFloat()
            val right = area.right().toFloat()
            val bottom = area.bottom().toFloat()

            corner(consumer, left, top)
            corner(consumer, left, bottom)
            corner(consumer, right, bottom)
            corner(consumer, right, top)
        }

        private fun corner(consumer: VertexConsumer, x: Float, y: Float) {
            consumer.addVertexWith2DPose(pose, x, y)
                .setUv(
                    ((x - centreX) / radius).toFloat(),
                    ((y - centreY) / radius).toFloat(),
                )
                .setColor(argb)
        }

        override fun pipeline(): RenderPipeline = pipeline

        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

        override fun scissorArea(): ScreenRectangle? = clip

        override fun bounds(): ScreenRectangle = area
    }

    fun wholeScreen(width: Int, height: Int): ScreenRectangle = ScreenRectangle(0, 0, width, height)

    fun boxAround(centreX: Double, centreY: Double, reach: Double): ScreenRectangle {
        val left = floor(centreX - reach).toInt()
        val top = floor(centreY - reach).toInt()
        return ScreenRectangle(
            left,
            top,
            ceil(centreX + reach).toInt() - left,
            ceil(centreY + reach).toInt() - top,
        )
    }
}
