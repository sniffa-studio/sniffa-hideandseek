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

    var paging: Boolean = false

    private var shown: List<Any?>? = null

    fun show(ability: Ability?) {
        val now = listOf(
            ability?.id, ShopSession.points, ShopSession.known, paging,
            ability?.let { ShopSession.cooldownLeft(it) },
            ability?.let { ShopSession.available(it) },
            ability?.let { ShopSession.price(it) },
        )
        if (now == shown) return
        shown = now

        if (ability == null) {
            title.text(McText.literal("${ShopSession.points} Punkte"))
            title.color(Color.ofArgb(if (ShopSession.known) Palette.BRAND else Palette.ERROR))
            body.text(
                McText.literal(
                    if (!ShopSession.known) "Noch keine Punkte  ·  warte auf die Runde"
                    else if (paging) "Maus auf eine Fähigkeit  ·  Q / E blättert  ·  Esc schließt"
                    else "Maus auf eine Fähigkeit  ·  Esc schließt"
                )
            )
            return
        }

        val cooling = ShopSession.cooldownLeft(ability)
        val available = ShopSession.available(ability)

        val price = ShopSession.price(ability)
        val label = when {
            ShopSession.restocking(ability) -> "Knallbonbons  —  $price Punkte"
            price == 0 -> "gekauft"
            else -> "$price Punkte"
        }
        title.text(McText.literal("${ability.displayName}  —  $label"))
        title.color(Color.ofArgb(if (available) Palette.BRAND else Palette.ERROR))
        body.text(
            McText.literal(
                when {
                    cooling > 0 -> "${ability.description}  (wieder bereit in ${readyIn(cooling)})"
                    !ShopSession.canAfford(ability) ->
                        "${ability.description}  (es fehlen ${price - ShopSession.points})"
                    else -> ability.description
                }
            )
        )
    }

    private fun readyIn(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}
