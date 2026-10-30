package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

class AbilityIcon(private val sprite: Identifier, private val size: Int) : BaseUIComponent() {

    override fun determineHorizontalContentSize(sizing: Sizing): Int = size

    override fun determineVerticalContentSize(sizing: Sizing): Int = size

    override fun draw(
        graphics: OwoUIGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
        delta: Float,
    ) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
    }
}
