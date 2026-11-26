package studio.sniffa.client.ui.shop.component

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import kotlin.math.ceil
import kotlin.math.floor

class WedgeMesh(
    val radius: Double,
    val start: Double,
    val end: Double,
)

object WedgeMeshes {

    fun draw(graphics: GuiGraphicsExtractor, centerX: Int, centerY: Int, rows: List<WedgeMesh>, segments: Int, argb: Int) {
        if (rows.size < 2 || segments < 1 || (argb ushr 24) == 0) return
        val reach = rows.maxOf { it.radius }
        val left = floor(centerX - reach).toInt() - 1
        val top = floor(centerY - reach).toInt() - 1
        val size = ceil(reach * 2).toInt() + 2
        val bounds = ScreenRectangle(left, top, size, size).transformMaxBounds(graphics.pose())
        graphics.guiRenderState.addGuiElement(
            Element(Matrix3x2f(graphics.pose()), bounds, centerX.toDouble(), centerY.toDouble(), rows, segments, argb)
        )
    }

    private class Element(
        private val pose: Matrix3x2fc,
        private val area: ScreenRectangle,
        private val centerX: Double,
        private val centerY: Double,
        private val rows: List<WedgeMesh>,
        private val segments: Int,
        private val argb: Int,
    ) : GuiElementRenderState {

        override fun buildVertices(consumer: VertexConsumer) {
            for (row in 0 until rows.size - 1) {
                val near = rows[row]
                val far = rows[row + 1]
                for (step in 0 until segments) {
                    vertex(consumer, near, step)
                    vertex(consumer, far, step)
                    vertex(consumer, far, step + 1)
                    vertex(consumer, near, step + 1)
                }
            }
        }

        private fun vertex(consumer: VertexConsumer, row: WedgeMesh, step: Int) {
            val degrees = row.start + (row.end - row.start) * step / segments
            consumer.addVertexWith2DPose(
                pose,
                Radial.x(centerX, degrees, row.radius).toFloat(),
                Radial.y(centerY, degrees, row.radius).toFloat(),
            ).setColor(argb)
        }

        override fun pipeline(): RenderPipeline = RenderPipelines.GUI

        override fun textureSetup(): TextureSetup = TextureSetup.noTexture()

        override fun scissorArea(): ScreenRectangle? = null

        override fun bounds(): ScreenRectangle = area
    }
}
