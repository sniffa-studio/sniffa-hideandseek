package studio.sniffa.client.ui.shop

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component as McText
import org.lwjgl.glfw.GLFW
import studio.sniffa.ability.Ability
import studio.sniffa.ability.AbilityCatalog
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.shop.component.AbilityWheel
import studio.sniffa.client.ui.sound.UiSound
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics

class AbilityShopScreen : BaseOwoScreen<FlowLayout>() {

    private val abilities = AbilityCatalog.forSide(RoundState.side)

    private var wheel: AbilityWheel? = null
    private lateinit var metrics: WheelMetrics

    override fun createAdapter(): OwoUIAdapter<FlowLayout> =
        OwoUIAdapter.create(this, UIContainers::verticalFlow)

    override fun build(root: FlowLayout) {
        metrics = WheelMetrics.forViewport(width, height, Spacing.BELOW_WHEEL)
        val built = AbilityWheel(abilities, metrics)
        wheel = built

        val column = UIContainers.verticalFlow(Sizing.content(), Sizing.content()).apply {
            horizontalAlignment(HorizontalAlignment.CENTER)
            gap(Spacing.SECTION_GAP)

            child(built)
            child(
                UIComponents.label(McText.literal(HINT)).apply {
                    color(Color.ofArgb(Palette.MUTED))
                    shadow(true)
                }
            )

            child(studioLockup())
        }

        root.apply {
            surface(Surface.blur(BLUR_QUALITY, BLUR_SIZE).and(Surface.flat(Palette.SCRIM)))
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            child(column)
        }
    }

    private fun buy(ability: Ability) {
        if (!ShopSession.available(ability)) return
        if (!ShopSession.buy(ability)) return
        UiSound.purchased()

        wheel?.purchased()
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

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        wheel?.let {
            val centerX = it.x() + metrics.size / 2.0
            val centerY = it.y() + metrics.size / 2.0
            it.pointAt(mouseX - centerX, mouseY - centerY)
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            val chosen = wheel?.focused
            if (chosen != null && ShopSession.available(chosen)) {
                buy(chosen)
                return true
            }
        }

        return super.mouseClicked(event, doubled)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) = Unit

    private companion object {
        const val BLUR_QUALITY = 4f

        const val BLUR_SIZE = 9f

        const val HINT = "Maus auf eine Fähigkeit  ·  Klick kauft  ·  Esc schließt"
    }
}
