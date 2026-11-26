package studio.sniffa.client.ui.shop.component

import io.wispforest.owo.ui.core.OwoUIGraphics
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import studio.sniffa.ability.Ability
import studio.sniffa.client.state.ShopSession
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.client.ui.theme.Textures
import studio.sniffa.client.ui.theme.WheelMetrics
import kotlin.math.roundToInt

class WheelCentre(private val metrics: WheelMetrics) {

    private val segments = Radial.segmentsFor(360.0, metrics.hubRadius)

    fun draw(graphics: OwoUIGraphics, centerX: Int, centerY: Int, focused: Ability?, alpha: Float) {
        graphics.drawRing(
            centerX, centerY, 0.0, 360.0, segments, 0.0, metrics.hubRadius,
            Radial.ink(Palette.HUB, alpha), Radial.ink(Palette.HUB, alpha),
        )

        if (focused == null) idle(graphics, centerX, centerY, alpha) else describe(graphics, centerX, centerY, focused, alpha)
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
            Radial.fade(WHITE, alpha),
        )

        val text = if (ShopSession.known) "${ShopSession.points}" else "Noch keine Punkte"
        val colour = if (ShopSession.known) Palette.GOLD else Palette.DETAIL
        val scale = if (ShopSession.known) pointsScale else metrics.textScale
        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX.toFloat(), top + image + POINTS_GAP)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, -font.width(text) / 2, 0, Radial.fade(colour, alpha), true)
        graphics.pose().popMatrix()
    }

    private fun describe(graphics: OwoUIGraphics, centerX: Int, centerY: Int, ability: Ability, alpha: Float) {
        val font = Minecraft.getInstance().font
        val small = metrics.textScale * DESCRIPTION_SCALE
        val width = metrics.hubRadius * TEXT_WIDTH_SHARE

        val status = status(ability)
        val lines = font.split(Component.literal(ability.description), (width / small).toInt())
        val lineHeight = (font.lineHeight + LINE_SPACING) * small
        val titleHeight = font.lineHeight * metrics.textScale
        val statusHeight = font.lineHeight * small
        val room = metrics.hubRadius * 2 * ROOM_SHARE - titleHeight - statusHeight - BLOCK_GAP * 2
        val shown = lines.take(maxOf(1, (room / lineHeight).toInt()))
        val total = titleHeight + BLOCK_GAP + statusHeight + BLOCK_GAP + shown.size * lineHeight

        var y = centerY - total / 2f
        y = line(graphics, ability.displayName, centerX, y, metrics.textScale, Radial.fade(Palette.TEXT, alpha)) + BLOCK_GAP
        y = line(graphics, status, centerX, y, small, Radial.fade(Palette.LABEL, alpha)) + BLOCK_GAP
        for (text in shown) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(centerX.toFloat(), y)
            graphics.pose().scale(small, small)
            graphics.text(font, text, -font.width(text) / 2, 0, Radial.fade(Palette.DETAIL, alpha), false)
            graphics.pose().popMatrix()
            y += lineHeight
        }
    }

    private fun status(ability: Ability): String {
        val price = ShopSession.price(ability)
        val cooling = ShopSession.cooldownLeft(ability)
        return when {
            cooling > 0 -> "wieder bereit in ${readyIn(cooling)}"
            ShopSession.restocking(ability) -> "Knallbonbons  ·  $price Punkte"
            price == 0 -> "gekauft"
            !ShopSession.canAfford(ability) -> "$price Punkte  ·  dir fehlen ${price - ShopSession.points}"
            else -> "$price Punkte"
        }
    }

    private fun line(graphics: OwoUIGraphics, text: String, centerX: Int, y: Float, scale: Float, colour: Int): Float {
        val font = Minecraft.getInstance().font
        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX.toFloat(), y)
        graphics.pose().scale(scale, scale)
        graphics.text(font, text, -font.width(text) / 2, 0, colour, false)
        graphics.pose().popMatrix()
        return y + font.lineHeight * scale
    }

    private fun readyIn(seconds: Int): String =
        if (seconds < 60) "${seconds}s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val LARGE_WHEEL = 220
        const val LARGE_SCALE = 1.5f
        const val POINTS_GAP = 3f
        const val TEXT_WIDTH_SHARE = 1.55
        const val ROOM_SHARE = 0.78
        const val DESCRIPTION_SCALE = 0.85f
        const val LINE_SPACING = 2f
        const val BLOCK_GAP = 4f
    }
}
