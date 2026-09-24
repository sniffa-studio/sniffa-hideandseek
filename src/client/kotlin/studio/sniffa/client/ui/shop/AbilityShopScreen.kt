package studio.sniffa.client.ui.shop

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.lwjgl.glfw.GLFW
import studio.sniffa.ability.Ability
import studio.sniffa.ability.AbilityCatalog
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.shop.component.AbilityWheel
import studio.sniffa.client.ui.shop.component.PageDots
import studio.sniffa.client.ui.shop.component.WheelDetail
import studio.sniffa.client.ui.sound.UiSound
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics

class AbilityShopScreen : BaseOwoScreen<FlowLayout>() {

    private val pages = WheelPages.split(AbilityCatalog.forSide(RoundState.side))
    private val dots = PageDots(pages.size)
    private val detail = WheelDetail()

    private var wheels: List<AbilityWheel> = emptyList()
    private lateinit var metrics: WheelMetrics
    private lateinit var stack: StackLayout

    private var page = 0

    override fun createAdapter(): OwoUIAdapter<FlowLayout> =
        OwoUIAdapter.create(this, UIContainers::verticalFlow)

    override fun build(root: FlowLayout) {
        metrics = WheelMetrics.forViewport(width, height, Spacing.BELOW_WHEEL)
        wheels = pages.map { AbilityWheel(it, metrics) }

        stack = UIContainers.stack(
            Sizing.fixed(metrics.size),
            Sizing.fixed(metrics.size),
        ).apply {
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)

            wheels.forEach { child(it) }
        }

        val column = UIContainers.verticalFlow(Sizing.content(), Sizing.content()).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(Spacing.SECTION_GAP)

            child(stack)
            child(dots)
            child(detail)

            child(studioLockup())
        }

        root.apply {
            surface(Surface.blur(BLUR_QUALITY, BLUR_SIZE).and(Surface.flat(Palette.SCRIM)))
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            child(column)
        }

        showPage(page)
    }

    private fun buy(ability: Ability) {
        if (!ShopSession.available(ability)) return
        if (!ShopSession.buy(ability)) return
        UiSound.purchased()

        wheels.getOrNull(page)?.clearFocus()
    }

    private fun studioLockup(): FlowLayout =
        UIContainers.horizontalFlow(Sizing.content(), Sizing.content()).apply {
            verticalAlignment(VerticalAlignment.CENTER)
            gap(Spacing.MARK_GAP)

            child(
                UIComponents.texture(
                    Textures.SNIFFA,
                    0, 0,
                    Textures.MARK_SOURCE_WIDTH, Textures.MARK_SOURCE_HEIGHT,
                    Textures.MARK_SOURCE_WIDTH, Textures.MARK_SOURCE_HEIGHT,
                ).apply {
                    horizontalSizing(Sizing.fixed(Spacing.MARK_WIDTH))
                    verticalSizing(Sizing.fixed(Spacing.LOCKUP_HEIGHT))
                    blend(true)
                }
            )

            child(
                UIComponents.texture(
                    Textures.SNIFFA_TEXT,
                    0, 0,
                    Textures.WORDMARK_SOURCE_WIDTH, Textures.WORDMARK_SOURCE_HEIGHT,
                    Textures.WORDMARK_SOURCE_WIDTH, Textures.WORDMARK_SOURCE_HEIGHT,
                ).apply {
                    horizontalSizing(Sizing.fixed(Spacing.WORDMARK_WIDTH))
                    verticalSizing(Sizing.fixed(Spacing.LOCKUP_HEIGHT))
                    blend(true)
                }
            )
        }

    private fun showPage(index: Int) {
        if (wheels.isEmpty()) return

        page = ((index % wheels.size) + wheels.size) % wheels.size
        wheels.forEachIndexed { at, wheel ->
            val size = if (at == page) metrics.size else 0
            wheel.horizontalSizing(Sizing.fixed(size))
            wheel.verticalSizing(Sizing.fixed(size))

            if (at != page) wheel.clearFocus()
        }
        dots.select(page)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        wheels.getOrNull(page)?.let { wheel ->
            val centerX = wheel.x() + metrics.size / 2.0
            val centerY = wheel.y() + metrics.size / 2.0
            wheel.refresh()
            wheel.pointAt(mouseX - centerX, mouseY - centerY)
            detail.show(wheel.focused)
        }

        super.render(graphics, mouseX, mouseY, delta)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        when (event.key()) {
            GLFW.GLFW_KEY_Q -> {
                showPage(page - 1)
                return true
            }

            GLFW.GLFW_KEY_E -> {
                showPage(page + 1)
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            val chosen = wheels.getOrNull(page)?.focused
            if (chosen != null && ShopSession.available(chosen)) {
                buy(chosen)
                return true
            }
        }

        return super.mouseClicked(event, doubled)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        if (verticalAmount != 0.0) {
            showPage(if (verticalAmount < 0) page + 1 else page - 1)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) = Unit

    private companion object {
        const val BLUR_QUALITY = 4f

        const val BLUR_SIZE = 9f
    }
}
