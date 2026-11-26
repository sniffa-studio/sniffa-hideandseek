package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.base.BaseUIComponent
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.world.item.ItemStack
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.sound.UiSound
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.hypot
import kotlin.math.roundToInt

class AbilityWheel(
    private val abilities: List<Ability>,
    private val metrics: WheelMetrics,
) : BaseUIComponent() {

    private val painter = RadialPainter(abilities.size.coerceAtLeast(1), metrics)
    private val wreath = Wreath(metrics)
    private val centre = WheelCentre(metrics)

    private var openedAt = 0L
    private var announced = 0

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
        val opened = ((now - openedAt) / NANOS_PER_MILLI).toFloat()
        val seconds = now / NANOS_PER_SECOND
        announce(opened)
        painter.advance(focus)

        val centerX = x + width / 2
        val centerY = y + height / 2

        wreath.draw(graphics, centerX, centerY, opened, seconds)

        abilities.forEachIndexed { index, ability ->
            val unfold = painter.unfold(index, opened)
            if (unfold <= 0f) return@forEachIndexed
            val style = if (ShopSession.available(ability)) Palette.WEDGE else Palette.WEDGE_DENIED
            painter.drawWedge(graphics, centerX, centerY, index, style, unfold, seconds)
        }

        abilities.forEachIndexed { index, ability ->
            val unfold = painter.unfold(index, opened)
            if (unfold <= 0f) return@forEachIndexed
            drawIcon(graphics, centerX, centerY, index, ability, unfold)
            drawBadge(graphics, centerX, centerY, index, ability, unfold)
        }

        centre.draw(graphics, centerX, centerY, focused, opened)
    }

    private fun announce(opened: Float) {
        while (announced < abilities.size && opened >= painter.startOf(announced)) {
            UiSound.wedgeUnfolded(announced, abilities.size)
            announced++
        }
    }

    private fun drawIcon(graphics: OwoUIGraphics, centerX: Int, centerY: Int, index: Int, ability: Ability, unfold: Float) {
        val burn = painter.burn(index)
        val reach = painter.reach(unfold)
        val degrees = painter.centreOf(index)
        val radius = metrics.inner + (metrics.iconRadius - metrics.inner) * reach + metrics.pop * burn * 0.6
        val iconX = Radial.x(centerX, degrees, radius).toFloat()
        val iconY = Radial.y(centerY, degrees, radius).toFloat()
        val size = metrics.iconSize
        val scale = (1f + HOVER_SCALE * burn) * (ENTRY_SCALE + (1f - ENTRY_SCALE) * unfold)

        if (burn > 0.02f) {
            graphics.drawRing(
                iconX.roundToInt(), iconY.roundToInt(), 0.0, 360.0, GLOW_SEGMENTS, 0.0, size * GLOW_REACH * scale,
                Color.ofArgb(Radial.opaque(GLOW, GLOW_ALPHA * burn * unfold)), Radial.CLEAR,
            )
        }

        val available = ShopSession.available(ability)
        val tint = if (available) Radial.opaque(0xFFFFFF, unfold) else Radial.fade(Palette.ICON_DIMMED, unfold)

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

        val cooling = ShopSession.cooldownShare(ability)
        val left = ShopSession.cooldownLeft(ability)
        if (cooling > 0.005f && left > 0) {
            val wipe = size * WIPE_REACH * scale
            val cx = iconX.roundToInt()
            val cy = iconY.roundToInt()
            val covered = 360.0 * cooling.coerceIn(0f, 1f)
            graphics.drawRing(
                cx, cy, 90.0, 90.0 + covered, Radial.segmentsFor(covered, wipe), 0.0, wipe,
                Color.ofArgb(Radial.fade(Palette.COOLDOWN_SHADE, unfold)), Color.ofArgb(Radial.fade(Palette.COOLDOWN_SHADE, unfold)),
            )
            val font = Minecraft.getInstance().font
            val text = if (left < 60) "$left" else "${left / 60}:${(left % 60).toString().padStart(2, '0')}"
            val textScale = metrics.badgeScale
            graphics.pose().pushMatrix()
            graphics.pose().translate(iconX, iconY - font.lineHeight * textScale / 2f + 0.5f)
            graphics.pose().scale(textScale, textScale)
            graphics.text(font, text, -font.width(text) / 2, 0, Radial.fade(Palette.TEXT, unfold), true)
            graphics.pose().popMatrix()
        }
    }

    private fun drawBadge(graphics: OwoUIGraphics, centerX: Int, centerY: Int, index: Int, ability: Ability, unfold: Float) {
        val font = Minecraft.getInstance().font
        val price = ShopSession.price(ability)
        val text = if (price == 0) "✔" else "$price"
        val colour = when {
            price == 0 -> Palette.BRAND
            ShopSession.canAfford(ability) -> Palette.GOLD
            else -> Palette.SOFT_RED
        }
        val scale = metrics.badgeScale
        val outer = painter.outerOf(index, unfold)
        val degrees = painter.centreOf(index)
        val bx = Radial.x(centerX, degrees, outer - metrics.badgeInset).toFloat()
        val by = Radial.y(centerY, degrees, outer - metrics.badgeInset).toFloat()

        val w = font.width(text) * scale
        val h = (font.lineHeight - 1) * scale
        val left = (bx - w / 2f - BADGE_PAD).roundToInt()
        val right = (bx + w / 2f + BADGE_PAD).roundToInt()
        val top = (by - h / 2f - 1f).roundToInt()
        val bottom = (by + h / 2f + 1f).roundToInt()
        val back = Radial.fade(Palette.BADGE, unfold)
        graphics.fill(left + 1, top, right - 1, bottom, back)
        graphics.fill(left, top + 1, left + 1, bottom - 1, back)
        graphics.fill(right - 1, top + 1, right, bottom - 1, back)

        graphics.pose().pushMatrix()
        graphics.pose().translate(bx - w / 2f, by - h / 2f)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, 0, 0, Radial.fade(colour, unfold), false)
        graphics.pose().popMatrix()
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000.0
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val HOVER_SCALE = 0.25f
        const val ENTRY_SCALE = 0.6f
        const val GLOW = 0xF2C65A
        const val GLOW_ALPHA = 0.32f
        const val GLOW_REACH = 0.85
        const val GLOW_SEGMENTS = 20
        const val WIPE_REACH = 0.62
        const val BADGE_PAD = 2f
    }
}
