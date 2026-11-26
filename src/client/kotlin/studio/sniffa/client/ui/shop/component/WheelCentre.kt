package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.OwoUIGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.text.ShineText
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.roundToInt

class WheelCentre(private val metrics: WheelMetrics) {

    private val segments = Radial.segmentsFor(360.0, metrics.hubRadius)

    private var shown: Ability? = null
    private var shownSince = 0L

    fun draw(graphics: OwoUIGraphics, centerX: Int, centerY: Int, focused: Ability?, openedMillis: Float) {
        val alpha = (openedMillis / FADE_MILLIS).coerceIn(0f, 1f)

        graphics.drawRing(
            centerX, centerY, 0.0, 360.0, segments, 0.0, metrics.hubRadius,
            Color.ofArgb(Radial.fade(Palette.HUB_INNER, alpha)), Color.ofArgb(Radial.fade(Palette.HUB_OUTER, alpha)),
        )
        graphics.drawRing(
            centerX, centerY, 0.0, 360.0, segments, metrics.hubRadius - 1, metrics.hubRadius,
            Color.ofArgb(Radial.fade(Palette.HUB_RIM, alpha)), Color.ofArgb(Radial.fade(Palette.HUB_RIM, alpha)),
        )

        if (focused != shown) {
            shown = focused
            shownSince = System.currentTimeMillis()
        }
        val since = System.currentTimeMillis() - shownSince
        val appear = (since / APPEAR_MILLIS).coerceIn(0f, 1f) * alpha

        if (focused == null) idle(graphics, centerX, centerY, appear) else describe(graphics, centerX, centerY, focused, appear, since)
    }

    private fun idle(graphics: OwoUIGraphics, centerX: Int, centerY: Int, alpha: Float) {
        val font = Minecraft.getInstance().font
        val image = metrics.hubImage
        val pointsScale = if (metrics.size >= LARGE_WHEEL) LARGE_SCALE else 1f
        val textHeight = font.lineHeight * pointsScale
        val top = centerY - ((image + POINTS_GAP + textHeight) / 2f).roundToInt()

        graphics.blit(
            RenderPipelines.GUI_TEXTURED, Textures.MARGUHL,
            centerX - image / 2, top, 0f, 0f, image, image,
            Textures.MARGUHL_SOURCE_SIZE, Textures.MARGUHL_SOURCE_SIZE,
            Textures.MARGUHL_SOURCE_SIZE, Textures.MARGUHL_SOURCE_SIZE,
            Radial.opaque(0xFFFFFF, alpha),
        )

        val text = if (ShopSession.known) "${ShopSession.points}" else "Noch keine Punkte"
        val colour = if (ShopSession.known) Palette.GOLD else Palette.MUTED
        val scale = if (ShopSession.known) pointsScale else metrics.textScale
        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX.toFloat(), top + image + POINTS_GAP)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, -font.width(text) / 2, 0, Radial.fade(colour, alpha), true)
        graphics.pose().popMatrix()
    }

    private fun describe(graphics: OwoUIGraphics, centerX: Int, centerY: Int, ability: Ability, alpha: Float, since: Long) {
        val font = Minecraft.getInstance().font
        val scale = metrics.textScale
        val width = (metrics.hubRadius * TEXT_WIDTH_SHARE).toInt()

        val price = ShopSession.price(ability)
        val cooling = ShopSession.cooldownLeft(ability)
        val affordable = ShopSession.canAfford(ability)
        val available = ShopSession.available(ability)

        val priceLine = when {
            ShopSession.restocking(ability) -> "Knallbonbons  ·  $price Punkte"
            price == 0 -> "gekauft"
            !affordable -> "$price Punkte  ·  es fehlen ${price - ShopSession.points}"
            else -> "$price Punkte"
        }
        val priceColour = if (affordable) Palette.GOLD else Palette.SOFT_RED
        val coolLine = if (cooling > 0) "wieder bereit in ${readyIn(cooling)}" else null

        val lines: List<FormattedCharSequence> = font.split(Component.literal(ability.description), (width / scale).toInt())
        val lineHeight = font.lineHeight * scale
        val room = metrics.hubRadius * 2 * ROOM_SHARE - font.lineHeight - lineHeight * (if (coolLine != null) 2 else 1) - GAP * 2
        val shownLines = lines.take(maxOf(1, (room / (lineHeight + 0.5f)).toInt()))

        val total = font.lineHeight + GAP + lineHeight * (1 + (if (coolLine != null) 1 else 0)) + GAP + shownLines.size * lineHeight
        var y = centerY - total / 2f

        if (available) {
            ShineText.drawCentred(graphics, font, ability.displayName, centerX.toFloat(), y, ShineText.Style.SHINE, alpha, since, stagger = false)
        } else {
            val name = ability.displayName
            graphics.text(font, name, centerX - font.width(name) / 2, y.roundToInt(), Radial.fade(Palette.TEXT, alpha), true)
        }
        y += font.lineHeight + GAP

        y = scaledLine(graphics, priceLine, centerX, y, scale, Radial.fade(priceColour, alpha))
        if (coolLine != null) y = scaledLine(graphics, coolLine, centerX, y, scale, Radial.fade(Palette.ICE, alpha))
        y += GAP

        for (line in shownLines) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(centerX.toFloat(), y)
            graphics.pose().scale(scale, scale)
            graphics.text(font, line, -font.width(line) / 2, 0, Radial.fade(Palette.MUTED, alpha), true)
            graphics.pose().popMatrix()
            y += lineHeight
        }
    }

    private fun scaledLine(graphics: OwoUIGraphics, text: String, centerX: Int, y: Float, scale: Float, colour: Int): Float {
        val font = Minecraft.getInstance().font
        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX.toFloat(), y)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, -font.width(text) / 2, 0, colour, true)
        graphics.pose().popMatrix()
        return y + font.lineHeight * scale
    }

    private fun readyIn(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

    private companion object {
        const val FADE_MILLIS = 250f
        const val APPEAR_MILLIS = 120f
        const val LARGE_WHEEL = 220
        const val LARGE_SCALE = 1.5f
        const val POINTS_GAP = 3f
        const val TEXT_WIDTH_SHARE = 1.62
        const val ROOM_SHARE = 0.8
        const val GAP = 2f
    }
}
