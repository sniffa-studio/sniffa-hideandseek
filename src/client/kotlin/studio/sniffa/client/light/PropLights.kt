package studio.sniffa.client.light

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.geom.builders.UVPair
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Display
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import studio.sniffa.Hideandseek
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PropLights {

    private const val PROP_PREFIX = "props/"
    private const val CELL = 1f
    private const val BRIGHT_ENOUGH = 7
    private const val TEXELS_PER_QUAD = 64
    private val WARM_WHITE = Vector3f(1f, 0.72f, 0.42f)
    private val FIRE_WORDS = listOf("feuer", "fire", "kamin", "esse", "ofen", "flamme", "glut", "forge", "schmiede")
    private val CANDLE_WORDS = listOf("kerze", "adventskranz", "schwibbogen", "candle", "pyramide")
    private val STALL_WORDS = listOf("stand_", "marktstand", "markthuette", "christbaumverkauf")

    class Emitter(val at: Vector3f, val colour: Vector3f, val area: Float)

    class Kind(val emitters: List<Emitter>, val flame: Flame, val stall: Boolean)

    private val NONE = Kind(emptyList(), Flame.STEADY, false)

    private val kinds = HashMap<Pair<Identifier, ItemDisplayContext>, Kind>()
    private var imageName: Identifier? = null
    private var image: NativeImage? = null
    private var atlas: Any? = null

    fun forget() {
        kinds.clear()
        image?.close()
        image = null
        imageName = null
    }

    fun of(client: Minecraft, display: Display.ItemDisplay): Kind {
        val item = display.itemRenderState() ?: return NONE
        val stack = item.itemStack()
        val model = stack.get(DataComponents.ITEM_MODEL) ?: return NONE
        if (model.namespace != Hideandseek.MOD_ID || !model.path.startsWith(PROP_PREFIX)) return NONE
        val texture = client.textureManager.getTexture(TextureAtlas.LOCATION_ITEMS).texture
        if (texture !== atlas) {
            forget()
            atlas = texture
        }
        return kinds.getOrPut(model to item.itemTransform()) {
            val renderState = ItemStackRenderState()
            client.itemModelResolver.updateForNonLiving(renderState, stack, item.itemTransform(), display)
            val captured = ItemQuads.capture(renderState, PoseStack(), 0) ?: return@getOrPut NONE
            val emitters = cluster(client, captured)
            if (emitters.isEmpty()) NONE else Kind(emitters, flameOf(model.path), STALL_WORDS.any { model.path.contains(it) })
        }
    }

    fun placement(display: Display.ItemDisplay, ox: Double, oy: Double, oz: Double): Matrix4f? {
        val state = display.renderState() ?: return null
        val pose = PoseStack()
        pose.translate(display.x - ox, display.y - oy, display.z - oz)
        pose.mulPose(Quaternionf().rotationYXZ((-Math.PI / 180.0).toFloat() * display.yRot, (Math.PI / 180.0).toFloat() * display.xRot, 0f))
        pose.mulPose(state.transformation().get(1f))
        pose.mulPose(Axis.YP.rotation(Math.PI.toFloat()))
        return Matrix4f(pose.last().pose())
    }

    private fun flameOf(path: String): Flame = when {
        FIRE_WORDS.any { path.contains(it) } -> Flame.FIRE
        CANDLE_WORDS.any { path.contains(it) } -> Flame.CANDLE
        path.contains("laterne") || path.contains("lampe") || path.contains("lantern") -> Flame.LANTERN
        else -> Flame.STEADY
    }

    private class Cell {
        val at = Vector3f()
        val colour = Vector3f()
        var area = 0f
    }

    private fun cluster(client: Minecraft, captured: List<ItemQuads.Captured>): List<Emitter> {
        val cells = HashMap<Long, Cell>()
        val corner = Vector3f()
        for (part in captured) {
            val matrix = part.pose.pose()
            for (quad in part.quads) {
                if (quad.materialInfo().lightEmission() < BRIGHT_ENOUGH) continue
                val centre = Vector3f()
                val points = Array(4) { index ->
                    matrix.transformPosition(quad.position(index), corner)
                    Vector3f(corner).also { centre.add(it) }
                }
                centre.mul(0.25f)
                val area = Vector3f(points[2]).sub(points[0]).cross(Vector3f(points[3]).sub(points[1])).length() * 0.5f
                if (area <= 1.0e-6f) continue
                val key = (floor(centre.x / CELL).toLong() and 0x1fffff) or ((floor(centre.y / CELL).toLong() and 0x1fffff) shl 21) or ((floor(centre.z / CELL).toLong() and 0x1fffff) shl 42)
                val cell = cells.getOrPut(key) { Cell() }
                cell.at.add(Vector3f(centre).mul(area))
                cell.colour.add(Vector3f(colourOf(client, quad.materialInfo().sprite(), quad)).mul(area))
                cell.area += area
            }
        }
        return cells.values.map { cell ->
            val colour = Vector3f(cell.colour).div(cell.area)
            val peak = max(colour.x, max(colour.y, colour.z))
            if (peak > 1.0e-3f) colour.div(peak) else colour.set(WARM_WHITE)
            Emitter(Vector3f(cell.at).div(cell.area), colour.lerp(WARM_WHITE, 0.4f), cell.area)
        }
    }

    private fun colourOf(client: Minecraft, sprite: TextureAtlasSprite, quad: net.minecraft.client.resources.model.geometry.BakedQuad): Vector3f {
        val image = imageOf(client, sprite.contents().name()) ?: return WARM_WHITE
        var u0 = 1f
        var v0 = 1f
        var u1 = 0f
        var v1 = 0f
        for (index in 0 until 4) {
            val uv = quad.packedUV(index)
            val u = (UVPair.unpackU(uv) - sprite.u0) / (sprite.u1 - sprite.u0)
            val v = (UVPair.unpackV(uv) - sprite.v0) / (sprite.v1 - sprite.v0)
            u0 = min(u0, u); u1 = max(u1, u)
            v0 = min(v0, v); v1 = max(v1, v)
        }
        val width = image.width
        val height = image.height / max(1, image.height / width)
        val x0 = (u0 * width).toInt().coerceIn(0, width - 1)
        val x1 = (u1 * width).toInt().coerceIn(x0 + 1, width)
        val y0 = (v0 * height).toInt().coerceIn(0, height - 1)
        val y1 = (v1 * height).toInt().coerceIn(y0 + 1, height)
        val stepX = max(1, ((x1 - x0) * (y1 - y0) / TEXELS_PER_QUAD).let { sqrt(it.toFloat()).toInt() })
        val sum = Vector3f()
        var weight = 0f
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val pixel = image.getPixel(x, y)
                val alpha = ARGB.alpha(pixel) / 255f
                if (alpha > 0.1f) {
                    sum.add(ARGB.red(pixel) / 255f * alpha, ARGB.green(pixel) / 255f * alpha, ARGB.blue(pixel) / 255f * alpha)
                    weight += alpha
                }
                x += stepX
            }
            y += stepX
        }
        return if (weight > 0f) sum.div(weight) else WARM_WHITE
    }

    private fun imageOf(client: Minecraft, name: Identifier): NativeImage? {
        if (name == imageName) return image
        image?.close()
        imageName = name
        val file = Identifier.fromNamespaceAndPath(name.namespace, "textures/${name.path}.png")
        image = try {
            client.resourceManager.getResource(file).orElse(null)?.open()?.use { NativeImage.read(it) }
        } catch (failure: Exception) {
            null
        }
        return image
    }
}
