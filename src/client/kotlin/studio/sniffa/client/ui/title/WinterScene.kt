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
    private val BACKGROUND: Identifier = Hideandseek.id("textures/ui/xmas/background.png")
    private const val BACKGROUND_W = 1280
    private const val BACKGROUND_H = 720
    private val TREE: Identifier = Hideandseek.id("textures/ui/xmas/tree.png")
    private val GIFTS: Identifier = Hideandseek.id("textures/ui/xmas/gifts.png")
    private val HAT: Identifier = Hideandseek.id("textures/ui/xmas/santahat.png")

    private const val DRIFT_W = 64
    private const val DRIFT_H = 18
    private const val TREE_W = 46
    private const val TREE_H = 58
    private const val GIFTS_W = 56
    private const val GIFTS_H = 30
    private const val HAT_W = 29
    private const val HAT_H = 17

    private const val GIFT_MIN_WIDTH = 60
    private const val RIBBON_INSET = 14

    private class Wrap(val base: Int, val light: Int, val dark: Int, val outline: Int)

    private val WRAPS = arrayOf(
        Wrap(0xFFB3122F.toInt(), 0xFFD62A47.toInt(), 0xFF7E0A20.toInt(), 0xFF3A0610.toInt()),
        Wrap(0xFF1E7A3F.toInt(), 0xFF2E9E55.toInt(), 0xFF125128.toInt(), 0xFF07240F.toInt()),
    )
    private const val RIBBON = 0xFFE8B33A.toInt()
    private const val RIBBON_DARK = 0xFFA8761C.toInt()
    private const val RIBBON_LIGHT = 0xFFFFE07A.toInt()

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
        backdrop(graphics, width, height, now)
        graphics.fill(0, 0, width, height, argb(0x30, NIGHT))
        graphics.fillGradient(0, 0, width, height * 2 / 5, argb(0x96, NIGHT), argb(0x00, NIGHT))
        graphics.fillGradient(0, height * 2 / 5, width, height, argb(0x00, NIGHT), argb(0xB4, NIGHT))
        aurora(graphics, width, height, now)
        stars(graphics, width, height, now)
        snow(graphics, width, height, now, FAR)
        snow(graphics, width, height, now, MID)
        ground(graphics, width, height)
    }

    fun front(graphics: GuiGraphics, width: Int, height: Int, widgets: List<AbstractWidget>) {
        val now = Util.getMillis()
        val gifts = widgets.filter { it.visible && it is AbstractButton && it.width >= GIFT_MIN_WIDTH }
        gifts.forEachIndexed { i, button -> wrap(graphics, button, WRAPS[i % WRAPS.size]) }
        widgets.filter { it.visible && it is AbstractButton }.forEach { cap(graphics, it) }
        gifts.forEach { bow(graphics, it, now) }
        logoSnow(graphics, width)
        garland(graphics, width, now)
        hat(graphics, width, now)
        edition(graphics, width)
        snow(graphics, width, height, now, NEAR)
    }

    private fun backdrop(graphics: GuiGraphics, width: Int, height: Int, now: Long) {
        val breathe = 1.06 + 0.03 * sin(now / 9000.0)
        val scale = maxOf(width / BACKGROUND_W.toDouble(), height / BACKGROUND_H.toDouble()) * breathe
        val w = (BACKGROUND_W * scale).toInt()
        val h = (BACKGROUND_H * scale).toInt()
        val x = (width - w) / 2 + (6 * sin(now / 13000.0)).toInt()
        val y = (height - h) / 2
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 0f, 0f, w, h,
            BACKGROUND_W, BACKGROUND_H, BACKGROUND_W, BACKGROUND_H)
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, GIFTS, treeX + treeW - 16, height - GIFTS_H - 6, 0f, 0f,
            GIFTS_W, GIFTS_H, GIFTS_W, GIFTS_H, GIFTS_W, GIFTS_H)
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
        val ray = 4 + (4 * glow).toInt()
        val shine = argb((90 + 120 * glow).toInt(), 0xFFE07A)
        graphics.fill(starX - 6 - ray, starY, starX - 5, starY + 1, shine)
        graphics.fill(starX + 6, starY, starX + 7 + ray, starY + 1, shine)
        graphics.fill(starX, starY - 6 - ray, starX + 1, starY - 5, shine)
        graphics.fill(starX, starY + 6, starX + 1, starY + 7 + ray / 2, shine)
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

    private fun hat(graphics: GuiGraphics, width: Int, now: Long) {
        val w = HAT_W * 3 / 2
        val h = HAT_H * 3 / 2
        val centreX = width / 2 - LOGO_HALF_WIDTH + 22
        val centreY = LOGO_TOP + 6 - h / 2
        val sway = -0.14 + 0.06 * sin(now / 650.0)
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(centreX.toFloat(), (centreY + h / 2).toFloat())
        pose.rotate(sway.toFloat())
        graphics.blit(RenderPipelines.GUI_TEXTURED, HAT, -w / 2, -h, 0f, 0f, w, h, HAT_W, HAT_H, HAT_W, HAT_H)
        pose.popMatrix()
    }

    private fun logoSnow(graphics: GuiGraphics, width: Int) {
        val left = width / 2 - LOGO_HALF_WIDTH + 34
        val right = width / 2 + LOGO_HALF_WIDTH - 6
        val top = LOGO_TOP + 1
        var x = left
        while (x < right) {
            val bump = hash(x / 2, 91) % 3
            if (hash(x / 5, 17) % 9 != 0) {
                graphics.fill(x, top - bump, x + 2, top + 2, SNOW)
                graphics.fill(x, top + 2, x + 2, top + 3, SNOW_SHADE)
            }
            x += 2
        }
    }

    private fun aurora(graphics: GuiGraphics, width: Int, height: Int, now: Long) {
        val t = now / 1000.0
        val base = height * 0.18
        var x = 0
        while (x < width) {
            val centre = (base + 9 * sin(x / 70.0 + t * 0.25) + 5 * sin(x / 27.0 - t * 0.45)).toInt()
            val strength = 0.5 + 0.5 * sin(x / 95.0 + t * 0.3)
            val alpha = (22 + 74 * strength).toInt()
            val reach = (18 + 30 * strength).toInt()
            val mix = (0.5 + 0.5 * sin(x / 160.0 + t * 0.12))
            val colour = blend(0x3BFF9A, 0x8A6BFF, mix)
            graphics.fillGradient(x, centre - reach, x + 2, centre, argb(0, colour), argb(alpha, colour))
            graphics.fillGradient(x, centre, x + 2, centre + 5, argb(alpha, colour), argb(0, colour))
            x += 2
        }
    }

    private fun wrap(graphics: GuiGraphics, button: AbstractWidget, paper: Wrap) {
        val x = button.x
        val y = button.y
        val r = x + button.width
        val b = y + button.height
        val hot = button.isHoveredOrFocused
        if (hot) {
            graphics.fill(x - 2, y - 2, r + 2, b + 2, argb(0x70, RIBBON_LIGHT))
        }
        graphics.fill(x, y, r, b, paper.outline)
        graphics.fill(x + 1, y + 1, r - 1, b - 1, if (hot) paper.light else paper.base)
        graphics.fill(x + 1, y + 1, r - 1, y + 2, paper.light)
        graphics.fill(x + 1, b - 3, r - 1, b - 1, paper.dark)
        var row = 0
        var dy = y + 5
        while (dy < b - 4) {
            var dx = x + 5 + (row % 2) * 4
            while (dx < r - 3) {
                graphics.fill(dx, dy, dx + 1, dy + 1, argb(0x60, 0xFFFFFF))
                dx += 8
            }
            dy += 6
            row++
        }
        val rx = x + RIBBON_INSET
        graphics.fill(rx, y + 1, rx + 4, b - 1, RIBBON)
        graphics.fill(rx, y + 1, rx + 1, b - 1, RIBBON_DARK)
        graphics.fill(rx + 3, y + 1, rx + 4, b - 1, RIBBON_DARK)
        graphics.fill(rx + 1, y + 1, rx + 2, b - 1, RIBBON_LIGHT)
        val font = Minecraft.getInstance().font
        val colour = when {
            !button.active -> 0xFFA0A0A0.toInt()
            hot -> RIBBON_LIGHT
            else -> 0xFFFFFFFF.toInt()
        }
        val textX = rx + 4 + (r - rx - 4) / 2
        graphics.drawCenteredString(font, button.message, textX, y + (button.height - 8) / 2, colour)
    }

    private fun bow(graphics: GuiGraphics, button: AbstractWidget, now: Long) {
        val rx = button.x + RIBBON_INSET
        val top = button.y
        val lift = if (button.isHoveredOrFocused) (2 * abs(sin(now / 120.0))).toInt() else 0
        val y = top - 4 - lift
        graphics.fill(rx - 5, y - 1, rx, y + 4, RIBBON_DARK)
        graphics.fill(rx - 4, y, rx, y + 3, RIBBON)
        graphics.fill(rx + 4, y - 1, rx + 9, y + 4, RIBBON_DARK)
        graphics.fill(rx + 4, y, rx + 8, y + 3, RIBBON)
        graphics.fill(rx, y + 1, rx + 4, y + 5, RIBBON_DARK)
        graphics.fill(rx + 1, y + 2, rx + 3, y + 4, RIBBON_LIGHT)
        graphics.fill(rx - 3, y + 4, rx - 1, y + 7, RIBBON)
        graphics.fill(rx + 5, y + 4, rx + 7, y + 7, RIBBON)
    }

    private fun blend(a: Int, b: Int, t: Double): Int {
        fun ch(shift: Int) = ((a shr shift and 0xFF) * (1 - t) + (b shr shift and 0xFF) * t).toInt() and 0xFF
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun edition(graphics: GuiGraphics, width: Int) {
        val font = Minecraft.getInstance().font
        val y = EDITION_BOTTOM + 4
        plate(graphics, font.width(EDITION_LINE), width / 2, y)
        graphics.drawCenteredString(font, EDITION_LINE, width / 2, y, GOLD)
    }

    fun plate(graphics: GuiGraphics, textWidth: Int, centreX: Int, textY: Int) {
        val left = centreX - textWidth / 2 - 7
        val right = centreX + (textWidth + 1) / 2 + 7
        val top = textY - 3
        val bottom = textY + 11
        graphics.fill(left + 1, top, right - 1, bottom, argb(0xB8, NIGHT))
        graphics.fill(left, top + 1, right, bottom - 1, argb(0xB8, NIGHT))
        graphics.fill(left + 2, top, right - 2, top + 1, argb(0xC0, 0xE8B33A))
        graphics.fill(left + 2, bottom - 1, right - 2, bottom, argb(0xC0, 0xE8B33A))
    }

    private fun argb(alpha: Int, rgb: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

    private fun floorMod(value: Double, mod: Double): Double = value - floor(value / mod) * mod

    private fun hash(a: Int, b: Int): Int {
        var h = a * 374761393 + b * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7FFFFFFF
    }
}
