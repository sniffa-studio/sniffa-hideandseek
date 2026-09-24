package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.component.BoxComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing

class PageDots(
    private val pages: Int,
) : FlowLayout(Sizing.content(), Sizing.content(), Algorithm.HORIZONTAL) {

    private val dots = mutableListOf<BoxComponent>()

    init {
        alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
        gap(Spacing.DOT_GAP)

        if (pages > 1) {
            repeat(pages) {
                val dot = UIComponents.box(
                    Sizing.fixed(Spacing.DOT_SIZE),
                    Sizing.fixed(Spacing.DOT_SIZE),
                ).fill(true)
                dots += dot
                child(dot)
            }
        }
        select(0)
    }

    fun select(page: Int) {
        dots.forEachIndexed { index, dot ->
            dot.color(Color.ofArgb(if (index == page) Palette.DOT_ACTIVE else Palette.DOT_IDLE))
        }
    }
}
