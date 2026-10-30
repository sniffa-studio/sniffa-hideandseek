package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

class AbilityIcon(
    private val sprite: Identifier,
    private val size: Int,
    private val glow: () -> Float,
) : BaseUIComponent() {

    override fun determineHorizontalContentSize(sizing: Sizing): Int = size

    override fun determineVerticalContentSize(sizing: Sizing): Int = size

    override fun draw(
        graphics: OwoUIGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        delta: Float,
    ) {
        val grown = (width * (1f + GROW * glow())).roundToInt()
        val offset = (grown - width) / 2
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x - offset, y - offset, grown, grown)
    }

    private companion object {
        const val GROW = 0.18f
    }
}
