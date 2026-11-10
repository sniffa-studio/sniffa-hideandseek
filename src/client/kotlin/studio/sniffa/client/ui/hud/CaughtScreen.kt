package studio.sniffa.client.ui.hud

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import studio.sniffa.Hideandseek
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import studio.sniffa.client.ui.theme.Palette
import studio.sniffa.net.payload.CaughtPayload
import kotlin.math.sin
import kotlin.random.Random

object CaughtScreen {

    private const val FADE_MILLIS = 250f
    private const val SHOW_MILLIS = 5000L

    private const val SHADE = 0xD80B1530.toInt()
    private const val RED = 0xFFE8394A.toInt()
    private const val GOLD = 0xFFF2C65A.toInt()
    private const val SNOW = 0xFFF4F8FF.toInt()

    private val GIFT: Identifier = Hideandseek.id("hud/gift")
    private const val GIFT_SIZE = 36
    private const val GIFT_LIFT = 74
    private const val POP_MILLIS = 420f
    private const val LINE_WIDTH = 120

    private const val FLAKES = 90
    private const val FALL_PER_SECOND = 0.12f

    private class Flake(val x: Float, val y: Float, val size: Int, val speed: Float, val sway: Float, val alpha: Float)

    private var flakes: List<Flake> = emptyList()

    private const val HEADLINE_SCALE = 4f
    private const val HEADLINE_LIFT = 26
    private const val DETAIL_DROP = 18

    @Volatile
    private var caught: CaughtPayload? = null

    @Volatile
    private var startedAt = 0L

    fun install() {
        ClientPlayNetworking.registerGlobalReceiver(CaughtPayload.TYPE) { payload, _ ->
            caught = payload
            startedAt = System.currentTimeMillis()
            val random = Random(startedAt)
            flakes = List(FLAKES) {
                Flake(random.nextFloat(), random.nextFloat(), 1 + random.nextInt(2), 0.6f + random.nextFloat() * 0.8f,
                    random.nextFloat() * 6.28f, 0.35f + random.nextFloat() * 0.65f)
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> caught = null }

        HudElementRegistry.addLast(Hideandseek.id("caught_screen")) { graphics, _ ->
            val shown = caught ?: return@addLast

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed > SHOW_MILLIS) {
                caught = null
                return@addLast
            }

            draw(graphics, shown, (elapsed / FADE_MILLIS).coerceIn(0f, 1f), elapsed)
        }
    }

    private fun draw(graphics: GuiGraphicsExtractor, shown: CaughtPayload, fade: Float, elapsed: Long) {
        val font = Minecraft.getInstance().font
        val width = graphics.guiWidth()
        val height = graphics.guiHeight()

        graphics.fill(0, 0, width, height, Vignette.alpha(SHADE, fade))

        val seconds = elapsed / 1000f
        for (flake in flakes) {
            val y = ((flake.y + seconds * FALL_PER_SECOND * flake.speed) % 1f) * height
            val x = flake.x * width + sin(seconds * 1.4f + flake.sway) * 6f
            graphics.fill(x.toInt(), y.toInt(), x.toInt() + flake.size, y.toInt() + flake.size, Vignette.alpha(SNOW, fade * flake.alpha))
        }

        val pop = (elapsed / POP_MILLIS).coerceIn(0f, 1f)
        val bounce = 1f + 0.35f * sin(pop * Math.PI.toFloat()) * (1f - pop)
        val gift = (GIFT_SIZE * bounce).toInt()
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GIFT, width / 2 - gift / 2, height / 2 - GIFT_LIFT - gift / 2, gift, gift,
            Vignette.alpha(0xFFFFFFFF.toInt(), fade))

        val headline = "GEFUNDEN"
        graphics.pose().pushMatrix()
        graphics.pose().translate(width / 2f, height / 2f - HEADLINE_LIFT)
        graphics.pose().scale(HEADLINE_SCALE, HEADLINE_SCALE)
        graphics.text(font, headline, -font.width(headline) / 2, -font.lineHeight / 2, Vignette.alpha(RED, fade), true)
        graphics.pose().popMatrix()

        val lineY = height / 2 + 4
        graphics.fill(width / 2 - LINE_WIDTH / 2, lineY, width / 2 + LINE_WIDTH / 2, lineY + 1, Vignette.alpha(GOLD, fade))
        graphics.fill(width / 2 - 2, lineY - 2, width / 2 + 2, lineY + 3, Vignette.alpha(GOLD, fade))

        val by = "von "
        val name = shown.seeker
        val place = "  ·  Platz #${shown.place} von ${shown.total}"
        val total = font.width(by) + font.width(name) + font.width(place)
        var x = (width - total) / 2
        val y = height / 2 + DETAIL_DROP
        graphics.text(font, by, x, y, Vignette.alpha(Palette.TEXT, fade), true)
        x += font.width(by)
        graphics.text(font, name, x, y, Vignette.alpha(RED, fade), true)
        x += font.width(name)
        graphics.text(font, place, x, y, Vignette.alpha(GOLD, fade), true)
    }
}
