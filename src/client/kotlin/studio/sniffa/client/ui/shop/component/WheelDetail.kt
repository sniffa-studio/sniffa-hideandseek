package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.VerticalAlignment
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Spacing
import net.minecraft.network.chat.Component as McText

class WheelDetail : FlowLayout(
    Sizing.fixed(Spacing.DETAIL_WIDTH),
    Sizing.fixed(Spacing.DETAIL_HEIGHT),
    Algorithm.VERTICAL,
) {

    private val title: LabelComponent = UIComponents.label(McText.empty()).apply {
        shadow(true)
        horizontalTextAlignment(HorizontalAlignment.CENTER)
        horizontalSizing(Sizing.fill(100))
    }

    private val body: LabelComponent = UIComponents.label(McText.empty()).apply {
        color(Color.ofArgb(Palette.MUTED))
        shadow(true)
        maxWidth(Spacing.DETAIL_WIDTH)
        horizontalTextAlignment(HorizontalAlignment.CENTER)
        horizontalSizing(Sizing.fill(100))
    }

    init {
        alignment(HorizontalAlignment.CENTER, VerticalAlignment.TOP)
        gap(Spacing.LINE_GAP)
        child(title)
        child(body)
        show(null)
    }

    fun show(ability: Ability?) {
        if (ability == null) {
            title.text(McText.literal("${ShopSession.points} Punkte"))
            title.color(Color.ofArgb(if (ShopSession.known) Palette.BRAND else Palette.ERROR))
            body.text(
                McText.literal(
                    if (ShopSession.known) "Zeig nach außen  ·  Q / E blättert  ·  Esc schließt"
                    else "Keine Runde  ·  der Server hat noch keine Punkte geschickt"
                )
            )
            return
        }

        val cooling = ShopSession.cooldownLeft(ability)
        val available = ShopSession.available(ability)

        title.text(McText.literal("${ability.displayName}  —  ${ability.cost} Punkte"))
        title.color(Color.ofArgb(if (available) Palette.BRAND else Palette.ERROR))
        body.text(
            McText.literal(
                when {
                    cooling > 0 -> "${ability.description}  (noch ${cooling}s)"
                    !ShopSession.canAfford(ability) ->
                        "${ability.description}  (es fehlen ${ability.cost - ShopSession.points})"
                    else -> ability.description
                }
            )
        )
    }
}
