package studio.sniffa.client.ui.title

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import studio.sniffa.Hideandseek
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

object WinterScene {

    private val DRIFT: Identifier = Hideandseek.id("textures/ui/xmas/snowdrift.png")
    private val TREE: Identifier = Hideandseek.id("textures/ui/xmas/tree.png")
    private val GIFTS: Identifier = Hideandseek.id("textures/ui/xmas/gifts.png")
    private val HAT: Identifier = Hideandseek.id("textures/ui/xmas/santahat.png")

    private const val DRIFT_W = 64
    private const val DRIFT_H = 18
    private const val TREE_W = 46
    private const val TREE_H = 58
    private const val GIFTS_W = 34
    private const val GIFTS_H = 16
    private const val HAT_W = 29
    private const val HAT_H = 17

    private const val LOGO_HALF_WIDTH = 128
    private const val LOGO_TOP = 30
    private const val EDITION_BOTTOM = 81

    private const val NIGHT = 0x081430
    private const val SNOW = 0xFFF2F6FF.toInt()
    private const val SNOW_SHADE = 0xFFDCE7F6.toInt()
    private const val ICE = 0xFFA9DEFF.toInt()
    private const val WIRE = 0xFF16301C.toInt()
    private const val GOLD = 0xFFE8B33A.toInt()

    private val BULBS = intArrayOf(0xC8102E, 0xE8B33A, 0x2FA85A, 0x3D9CFF)

    private val TREE_LIGHTS = arrayOf(
        intArrayOf(22, 15), intArrayOf(15, 22), intArrayOf(30, 23), intArrayOf(11, 31),
        intArrayOf(35, 32), intArrayOf(23, 37), intArrayOf(7, 41), intArrayOf(39, 42),
        intArrayOf(17, 47), intArrayOf(31, 48), intArrayOf(4, 50), intArrayOf(42, 50),
    )

    const val SIGN_LIFT = 22

    private val EDITION_LINE: Component = Component.literal("❄ Weihnachts-Edition ❄")

    private data class Layer(val count: Int, val size: Int, val minSpeed: Float, val spread: Float,
                             val sway: Float, val alpha: Int, val salt: Int)

    private val FAR = Layer(110, 1, 9f, 7f, 6f, 0x90, 11)
    private val MID = Layer(55, 2, 17f, 9f, 10f, 0xC0, 23)
    private val NEAR = Layer(16, 3, 30f, 12f, 16f, 0xE6, 37)

    @JvmStatic
    fun back(graphics: GuiGraphics, width: Int, height: Int) {
        val now = Util.getMillis()
        graphics.fillGradient(0, 0, width, height / 2, argb(0xB0, NIGHT), argb(0x00, NIGHT))
        graphics.fillGradient(0, height * 11 / 20, width, height, argb(0x00, NIGHT), argb(0x9A, NIGHT))
        stars(graphics, width, height, now)
        snow(graphics, width, height, now, FAR)
        snow(graphics, width, height, now, MID)
        ground(graphics, width, height)
    }

    fun front(graphics: GuiGraphics, width: Int, height: Int, widgets: List<AbstractWidget>) {
        val now = Util.getMillis()
        widgets.filter { it.visible && it is AbstractButton }.forEach { cap(graphics, it) }
        garland(graphics, width, now)
        hat(graphics, width)
        edition(graphics, width)
        snow(graphics, width, height, now, NEAR)
    }

    private fun stars(graphics: GuiGraphics, width: Int, height: Int, now: Long) {
        val sky = height * 2 / 5
        for (i in 0 until 48) {
            val x = hash(i, 3) % width
            val y = 4 + hash(i, 5) % sky.coerceAtLeast(1)
            val twinkle = 0.5 + 0.5 * sin(now / 700.0 + i * 1.9)
            val alpha = (60 + 170 * twinkle).toInt()
            graphics.fill(x, y, x + 1, y + 1, argb(alpha, 0xFFFFFF))
            if (i % 9 == 0 && twinkle > 0.8) {
                graphics.fill(x - 1, y, x + 2, y + 1, argb(alpha / 3, 0xFFFFFF))
                graphics.fill(x, y - 1, x + 1, y + 2, argb(alpha / 3, 0xFFFFFF))
            }
        }
    }

    private fun snow(graphics: GuiGraphics, width: Int, height: Int, now: Long, layer: Layer) {
        val seconds = now / 1000.0
        val fall = height + 12
        for (i in 0 until layer.count) {
            val seed = hash(i, layer.salt)
            val speed = layer.minSpeed + (seed % 1000) / 1000f * layer.spread
            val y = floorMod(seconds * speed + seed % fall, fall.toDouble()) - 6
            val drift = sin(seconds * 0.7 + i * 0.37) * layer.sway + seconds * layer.sway * 0.35
            val x = floorMod((seed / 7 % width) + drift, width.toDouble()).toInt()
            val top = y.toInt()
            val colour = argb(layer.alpha, 0xFFFFFF)
            when (layer.size) {
                1 -> graphics.fill(x, top, x + 1, top + 1, colour)
                2 -> graphics.fill(x, top, x + 2, top + 2, colour)
                else -> {
                    graphics.fill(x - 1, top, x + 2, top + 1, colour)
                    graphics.fill(x, top - 1, x + 1, top + 2, colour)
                    graphics.fill(x - 2, top - 2, x - 1, top - 1, argb(layer.alpha / 2, 0xFFFFFF))
                    graphics.fill(x + 2, top + 2, x + 3, top + 3, argb(layer.alpha / 2, 0xFFFFFF))
                }
            }
        }
    }

    private fun ground(graphics: GuiGraphics, width: Int, height: Int) {
        val tileW = DRIFT_W * 2
        val tileH = DRIFT_H * 2
        val top = height - tileH + 6
        var x = 0
        while (x < width) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, DRIFT, x, top, 0f, 0f, tileW, tileH,
                DRIFT_W, DRIFT_H, DRIFT_W, DRIFT_H)
            x += tileW
        }
        val margin = (width - 240) / 2
        if (margin < TREE_W * 3 / 2 + 12) {
            return
        }
        val treeW = TREE_W * 3 / 2
        val treeH = TREE_H * 3 / 2
        val treeX = 10
        val treeY = height - treeH - 8
        graphics.blit(RenderPipelines.GUI_TEXTURED, TREE, treeX, treeY, 0f, 0f, treeW, treeH,
            TREE_W, TREE_H, TREE_W, TREE_H)
        val giftsW = GIFTS_W * 3 / 2
        val giftsH = GIFTS_H * 3 / 2
        graphics.blit(RenderPipelines.GUI_TEXTURED, GIFTS, treeX + treeW - 18, height - giftsH - 9, 0f, 0f,
            giftsW, giftsH, GIFTS_W, GIFTS_H, GIFTS_W, GIFTS_H)
        val now = Util.getMillis()
        TREE_LIGHTS.forEachIndexed { i, (lx, ly) ->
            val phase = sin(now / 520.0 + i * 2.1)
            if (phase > -0.2) {
                val px = treeX + lx * 3 / 2
                val py = treeY + ly * 3 / 2
                val colour = BULBS[i % BULBS.size]
                graphics.fill(px - 1, py - 1, px + 3, py + 3, argb((30 + 40 * phase).toInt(), colour))
                graphics.fill(px, py, px + 2, py + 2, argb(0xFF, colour))
            }
        }
        val glow = 0.5 + 0.5 * sin(now / 380.0)
        val starX = treeX + treeW / 2
        val starY = treeY + 6
        graphics.fill(starX - 5, starY - 5, starX + 6, starY + 6, argb((25 + 45 * glow).toInt(), 0xFFE07A))
        graphics.fill(starX - 3, starY - 3, starX + 4, starY + 4, argb((30 + 60 * glow).toInt(), 0xFFE07A))
    }

    private fun cap(graphics: GuiGraphics, button: AbstractWidget) {
        val left = button.x
        val right = button.x + button.width
        val top = button.y
        graphics.fill(left, top - 1, right, top + 2, SNOW)
        graphics.fill(left + 1, top + 2, right - 1, top + 3, SNOW_SHADE)
        var x = left
        while (x < right) {
            val bump = hash(x / 3, button.y) % 4
            if (bump > 0) {
                val w = minOf(3, right - x)
                graphics.fill(x, top - 1 - bump, x + w, top - 1, SNOW)
            }
            if (hash(x, button.y + 7) % 11 == 0 && x > left + 2 && x < right - 3) {
                val drip = 1 + hash(x, 3) % 3
                graphics.fill(x, top + 3, x + 1, top + 3 + drip, ICE)
            }
            x += 3
        }
    }

    private fun garland(graphics: GuiGraphics, width: Int, now: Long) {
        val spans = maxOf(2, width / 140)
        val span = width / spans.toFloat()
        val sag = 9f
        val top = 2
        fun wireY(x: Float): Float {
            val t = (x % span) / span * 2f - 1f
            return top + sag * (1f - t * t)
        }
        for (x in 0 until width) {
            val y = wireY(x.toFloat()).toInt()
            graphics.fill(x, y, x + 1, y + 1, WIRE)
        }
        var i = 0
        var x = 6f
        while (x < width - 2) {
            val y = wireY(x).toInt()
            val colour = BULBS[i % BULBS.size]
            val phase = sin(now / 450.0 + i * 1.3)
            val lit = phase > -0.35
            val bulbX = x.toInt()
            graphics.fill(bulbX, y + 1, bulbX + 2, y + 2, 0xFF2A2A2A.toInt())
            if (lit) {
                val glow = (40 + 50 * abs(phase)).toInt()
                graphics.fill(bulbX - 2, y, bulbX + 4, y + 7, argb(glow / 2, colour))
                graphics.fill(bulbX - 1, y + 1, bulbX + 3, y + 6, argb(glow, colour))
            }
            graphics.fill(bulbX, y + 2, bulbX + 2, y + 5, argb(if (lit) 0xFF else 0x90, colour))
            if (lit) {
                graphics.fill(bulbX, y + 2, bulbX + 1, y + 3, argb(0xC0, 0xFFFFFF))
            }
            x += 13f
            i++
        }
    }

    private fun hat(graphics: GuiGraphics, width: Int) {
        val x = width / 2 - LOGO_HALF_WIDTH - 6
        val y = LOGO_TOP - 12
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(x.toFloat() + HAT_W, y.toFloat() + HAT_H)
        pose.rotate((-PI / 9).toFloat())
        graphics.blit(RenderPipelines.GUI_TEXTURED, HAT, -HAT_W * 3 / 2, -HAT_H * 3 / 2, 0f, 0f,
            HAT_W * 3 / 2, HAT_H * 3 / 2, HAT_W, HAT_H, HAT_W, HAT_H)
        pose.popMatrix()
    }

    private fun edition(graphics: GuiGraphics, width: Int) {
        val font = Minecraft.getInstance().font
        graphics.drawCenteredString(font, EDITION_LINE, width / 2, EDITION_BOTTOM + 4, GOLD)
    }

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    private fun floorMod(value: Double, mod: Double): Double = value - floor(value / mod) * mod

    private fun hash(a: Int, b: Int): Int {
        var h = a * 374761393 + b * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7FFFFFFF
    }
}
