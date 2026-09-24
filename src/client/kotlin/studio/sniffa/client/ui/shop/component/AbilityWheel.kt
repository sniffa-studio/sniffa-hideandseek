package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.StackLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.world.item.ItemStack
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.sound.UiSound
import studio.sniffa.client.ui.theme.Spacing
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class AbilityWheel(
    private val abilities: List<Ability>,
    private val metrics: WheelMetrics,
) : StackLayout(Sizing.fixed(metrics.size), Sizing.fixed(metrics.size)) {

    private val wheelSurface = WheelSurface(Spacing.SLOTS_PER_PAGE, metrics)

    var focus: Int? = null
        private set

    init {
        surface(wheelSurface)

        val middle = metrics.size / 2

        abilities.forEachIndexed { index, ability ->
            val angle = angleOf(index)

            val slot = if (ability.sprite != null) metrics.iconSize else Spacing.SLOT_SIZE
            val x = middle + (cos(angle) * metrics.iconRadius).roundToInt() - slot / 2
            val y = middle - (sin(angle) * metrics.iconRadius).roundToInt() - slot / 2

            child(
                UIContainers.stack(
                    Sizing.fixed(slot),
                    Sizing.fixed(slot),
                ).apply {
                    alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
                    positioning(Positioning.absolute(x, y))
                    child(iconOf(ability))
                }
            )
        }

        child(
            UIComponents.texture(
                Textures.MARGUHL,
                0, 0,
                Textures.MARGUHL_SOURCE_SIZE, Textures.MARGUHL_SOURCE_SIZE,
                Textures.MARGUHL_SOURCE_SIZE, Textures.MARGUHL_SOURCE_SIZE,
            ).apply {
                horizontalSizing(Sizing.fixed(metrics.hubSize))
                verticalSizing(Sizing.fixed(metrics.hubSize))
                blend(true)
                positioning(
                    Positioning.absolute(
                        middle - metrics.hubSize / 2,
                        middle - metrics.hubSize / 2,
                    )
                )
            }
        )
    }

    fun pointAt(dx: Double, dy: Double) {
        val picked = when {
            hypot(dx, dy) < metrics.deadZone -> null
            else -> nearestWedge(dx, dy).takeIf { it < abilities.size }
        }
        if (picked == focus) return

        focus = picked
        wheelSurface.update(picked)

        if (picked != null) UiSound.wedgeFocused(ShopSession.available(abilities[picked]))
    }

    val focused: Ability? get() = focus?.let { abilities.getOrNull(it) }

    fun clearFocus() {
        if (focus == null) return
        focus = null
        wheelSurface.update(null)
    }

    fun refresh() {
        abilities.forEachIndexed { index, ability ->
            wheelSurface.describe(
                index,
                locked = !ShopSession.available(ability),
                cooling = ShopSession.cooldownShare(ability),
            )
        }
    }

    private fun iconOf(ability: Ability): UIComponent =
        ability.sprite?.let { AbilityIcon(it, metrics.iconSize) }
            ?: UIComponents.item(ItemStack(ability.icon)).apply {
                setTooltipFromStack(false)
            }

    private fun angleOf(index: Int): Double =
        Math.PI / 2 - TAU * index / Spacing.SLOTS_PER_PAGE

    private fun nearestWedge(dx: Double, dy: Double): Int {
        val pointer = atan2(-dy, dx)

        return (0 until Spacing.SLOTS_PER_PAGE).minByOrNull { index ->
            Math.abs(wrapToPi(pointer - angleOf(index)))
        } ?: 0
    }

    private fun wrapToPi(angle: Double): Double {
        var result = angle
        while (result > Math.PI) result -= TAU
        while (result < -Math.PI) result += TAU
        return result
    }

    private companion object {
        const val TAU = 2 * Math.PI
    }
}
