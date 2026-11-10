package studio.sniffa.client.snow

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek
import kotlin.random.Random

object SnowSplat {

    private val SPRITES: List<Identifier> = (0..3).map { Hideandseek.id("hud/snow_splat_$it") }

    private const val CLUMPS = 14
    private const val FADE_MILLIS = 900L
    private const val WASH_MILLIS = 500L
    private const val WASH_ALPHA = 0.85f
    private const val SLIDE_PER_SECOND = 0.035f
    private const val WOBBLE_MILLIS = 160L

    private class Clump(val sprite: Identifier, val x: Float, val y: Float, val size: Float, val slide: Float)

    @Volatile
    private var clumps: List<Clump> = emptyList()
    private var hitAt = 0L
    private var clearAt = 0L
    private val random = Random(System.nanoTime())

    fun install() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Hideandseek.id("snow_splat")) { graphics, _ ->
            draw(graphics)
        }
    }

    fun hit(ticks: Int) {
        val now = System.currentTimeMillis()
        hitAt = now
        clearAt = now + ticks * 50L
        clumps = List(CLUMPS) { index ->
            val spread = index.toFloat() / CLUMPS
            Clump(
                SPRITES[random.nextInt(SPRITES.size)],
                x = random.nextFloat() * 1.1f - 0.05f,
                y = random.nextFloat() * 0.9f - 0.05f,
                size = 0.32f + random.nextFloat() * 0.3f + (1f - spread) * 0.12f,
                slide = SLIDE_PER_SECOND * (0.4f + random.nextFloat()),
            )
        }
    }

    fun forget() {
        clumps = emptyList()
        clearAt = 0L
    }

    private fun draw(graphics: GuiGraphicsExtractor) {
        val now = System.currentTimeMillis()
        val showing = clumps
        if (showing.isEmpty()) return
        if (now >= clearAt + FADE_MILLIS) {
            clumps = emptyList()
            return
        }

        val width = graphics.guiWidth().toFloat()
        val height = graphics.guiHeight().toFloat()
        val fade = if (now <= clearAt) 1f else 1f - (now - clearAt).toFloat() / FADE_MILLIS
        val wash = (1f - (now - hitAt).toFloat() / WASH_MILLIS).coerceIn(0f, 1f) * WASH_ALPHA
        if (wash > 0f) {
            graphics.fill(0, 0, width.toInt(), height.toInt(), argb(wash * fade, 0xF0F6FF))
        }

        val seconds = (now - hitAt) / 1000f
        val wobble = if (now - hitAt < WOBBLE_MILLIS) (1f - (now - hitAt).toFloat() / WOBBLE_MILLIS) * 6f else 0f
        val short = minOf(width, height)
        for (clump in showing) {
            val size = (clump.size * short).toInt()
            val x = (clump.x * width - size / 2f).toInt()
            val y = (clump.y * height - size / 2f + clump.slide * height * seconds + wobble).toInt()
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, clump.sprite, x, y, size, size, argb(fade, 0xFFFFFF))
        }
    }

    private fun argb(alpha: Float, rgb: Int): Int = ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xFFFFFF)
}
