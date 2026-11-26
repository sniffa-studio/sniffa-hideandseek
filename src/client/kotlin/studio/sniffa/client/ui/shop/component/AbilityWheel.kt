package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.item.ItemStack
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.sound.UiSound
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.hypot

class AbilityWheel(
    private val abilities: List<Ability>,
    private val metrics: WheelMetrics,
) : BaseUIComponent() {

    private val painter = RadialPainter(abilities.size.coerceAtLeast(1), metrics)
    private val centre = WheelCentre(metrics)

    private var openedAt = 0L

    var focus: Int? = null
        private set

    val focused: Ability? get() = focus?.let { abilities.getOrNull(it) }

    override fun determineHorizontalContentSize(sizing: Sizing): Int = metrics.size

    override fun determineVerticalContentSize(sizing: Sizing): Int = metrics.size

    fun pointAt(dx: Double, dy: Double) {
        val picked = when {
            abilities.isEmpty() -> null
            hypot(dx, dy) < metrics.deadZone -> null
            else -> painter.indexAt(dx, dy).takeIf { it < abilities.size }
        }
        if (picked == focus) return

        focus = picked
        if (picked != null) UiSound.wedgeFocused(ShopSession.available(abilities[picked]))
    }

    fun purchased() {
        focus?.let { painter.flash(it) }
    }

    override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        val now = System.nanoTime()
        if (openedAt == 0L) openedAt = now
        val open = (((now - openedAt) / NANOS_PER_MILLI) / OPEN_MILLIS).toFloat().coerceIn(0f, 1f)
        val ease = 1f - (1f - open) * (1f - open)
        val alpha = ease
        val scale = OPEN_SCALE + (1f - OPEN_SCALE) * ease
        painter.advance(focus)

        val centerX = x + width / 2
        val centerY = y + height / 2

        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX.toFloat(), centerY.toFloat())
        graphics.pose().scale(scale, scale)
        graphics.pose().translate(-centerX.toFloat(), -centerY.toFloat())

        abilities.indices.forEach { painter.drawWedge(graphics, centerX, centerY, it, alpha) }

        abilities.forEachIndexed { index, ability ->
            val cooling = ShopSession.cooldownShare(ability)
            val left = ShopSession.cooldownLeft(ability)
            val coolingDown = left > 0 && cooling > 0f
            if (coolingDown) painter.drawCooldown(graphics, centerX, centerY, index, 1f - cooling, alpha)
            drawIcon(graphics, centerX, centerY, index, ability, coolingDown, alpha)
            drawLabel(graphics, centerX, centerY, index, ability, if (coolingDown) left else 0, alpha)
        }

        centre.draw(graphics, centerX, centerY, focused, alpha)

        graphics.pose().popMatrix()
    }

    private fun drawIcon(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        ability: Ability,
        coolingDown: Boolean,
        alpha: Float,
    ) {
        val degrees = painter.centreOf(index)
        val iconX = Radial.x(centerX, degrees, metrics.slotRadius).toFloat()
        val iconY = (Radial.y(centerY, degrees, metrics.slotRadius) - metrics.iconLift).toFloat()
        val size = metrics.iconSize
        val scale = 1f + HOVER_SCALE * painter.burn(index)
        val tint = when {
            coolingDown -> Radial.fade(Palette.ICON_COOLING, alpha * COOLING_ALPHA)
            !ShopSession.canAfford(ability) -> Radial.fade(WHITE, alpha * MUTED_ALPHA)
            else -> Radial.fade(WHITE, alpha)
        }

        graphics.pose().pushMatrix()
        graphics.pose().translate(iconX, iconY)
        graphics.pose().scale(scale, scale)
        val sprite = ability.sprite
        if (sprite != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, -size / 2, -size / 2, size, size, tint)
        } else {
            graphics.item(ItemStack(ability.icon), -8, -8)
        }
        graphics.pose().popMatrix()
    }

    private fun drawLabel(
        graphics: OwoUIGraphics,
        centerX: Int,
        centerY: Int,
        index: Int,
        ability: Ability,
        secondsLeft: Int,
        alpha: Float,
    ) {
        val font = Minecraft.getInstance().font
        val price = ShopSession.price(ability)
        val owned = price == 0 && secondsLeft == 0
        val text = when {
            secondsLeft > 0 -> readyIn(secondsLeft)
            owned -> CHECK
            else -> "$price"
        }
        val showPoint = secondsLeft == 0 && !owned
        val fade = if (secondsLeft == 0 && !owned && !ShopSession.canAfford(ability)) MUTED_ALPHA else 1f
        val colour = Radial.fade(Palette.LABEL, alpha * fade)

        val scale = metrics.textScale
        val degrees = painter.centreOf(index)
        val labelX = Radial.x(centerX, degrees, metrics.slotRadius).toFloat()
        val labelY = (Radial.y(centerY, degrees, metrics.slotRadius) + metrics.labelDrop).toFloat()
        val textWidth = font.width(text)
        val pointWidth = if (showPoint) POINT_GAP + POINT_SIZE else 0
        val total = textWidth + pointWidth

        graphics.pose().pushMatrix()
        graphics.pose().translate(labelX, labelY)
        graphics.pose().scale(scale, scale)
        val left = -total / 2
        graphics.text(font, text, left, -4, colour, false)
        if (showPoint) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, Textures.POINT,
                left + textWidth + POINT_GAP, -4, POINT_SIZE, POINT_SIZE, Radial.fade(WHITE, alpha * fade),
            )
        }
        graphics.pose().popMatrix()
    }

    private fun readyIn(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000.0
        const val OPEN_MILLIS = 120.0
        const val OPEN_SCALE = 0.94f
        const val HOVER_SCALE = 0.1f
        const val MUTED_ALPHA = 0.4f
        const val COOLING_ALPHA = 0.7f
        const val WHITE = 0xFFFFFFFF.toInt()
        const val CHECK = "✔"
        const val POINT_GAP = 1
        const val POINT_SIZE = 7
    }
}
