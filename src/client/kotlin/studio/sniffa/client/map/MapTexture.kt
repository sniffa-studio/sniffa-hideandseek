package studio.sniffa.client.map

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.material.MapColor
import studio.sniffa.Hideandseek

object MapTexture {

    private var builtFor = 0
    private var identifier: Identifier? = null

    private const val NOTHING_ARGB = 0x00000000

    private const val NOTHING_INDEX = 255

    private val SHADE = floatArrayOf(180f / 255f, 220f / 255f, 1.0f, 135f / 255f)

    private const val GRASS_TINT = 0xFF91BD59.toInt()
    private const val FOLIAGE_TINT = 0xFF77AB2F.toInt()
    private const val WATER_TINT = 0xFF3F76E4.toInt()
    private const val SPRUCE_TINT = 0xFF619961.toInt()
    private const val BIRCH_TINT = 0xFF80A755.toInt()

    private val GRASS_TINTED = setOf(
        "grass_block", "short_grass", "tall_grass", "fern", "large_fern", "vine", "sugar_cane",
        "lily_pad"
    )

    private val SELF_COLOURED_LEAVES = setOf(
        "cherry_leaves", "azalea_leaves", "flowering_azalea_leaves", "pale_oak_leaves"
    )

    fun of(map: MapState.ReadyMap): Identifier {
        val existing = identifier
        if (existing != null && builtFor == map.meta.hash) {
            return existing
        }
        existing?.let { Minecraft.getInstance().textureManager.release(it) }

        val id = Hideandseek.id("map/" + Integer.toUnsignedString(map.meta.hash, 16))
        val size = map.meta.size

        val shades = shadeTable(map.meta.palette)

        val image = NativeImage(size, size, false)
        for (z in 0 until size) {
            for (x in 0 until size) {
                val at = (z * size + x) * 2
                val material = map.pixels[at].toInt() and 0xFF
                val shade = (map.pixels[at + 1].toInt() and 0xFF).coerceIn(0, 3)

                val argb = if (material == NOTHING_INDEX || material >= map.meta.palette.size) {
                    NOTHING_ARGB
                } else {
                    shades[material * 4 + shade]
                }
                image.setPixel(x, z, argb)
            }
        }

        val texture = DynamicTexture({ "sniffa map" }, image)
        Minecraft.getInstance().textureManager.register(id, texture)

        builtFor = map.meta.hash
        identifier = id
        return id
    }

    private fun shadeTable(palette: List<String>): IntArray {
        val table = IntArray(palette.size * 4)
        palette.forEachIndexed { index, key ->
            val base = baseColour(key)
            for (shade in 0 until 4) {
                table[index * 4 + shade] = darken(base, SHADE[shade])
            }
        }
        return table
    }

    private fun baseColour(key: String): Int {
        val id = Identifier.tryParse(key) ?: return mapPaletteColour(key)
        val path = id.path

        for (candidate in textureCandidates(path)) {
            val averaged = averageTexture(Identifier.fromNamespaceAndPath(id.namespace, "textures/block/$candidate.png"))
                ?: continue
            return tint(path, averaged)
        }

        return mapPaletteColour(key)
    }

    private fun textureCandidates(path: String): List<String> {
        val direct = listOf("${path}_top", path, "${path}_still")

        val suffixes = listOf("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_pressure_plate", "_button")
        val suffix = suffixes.firstOrNull { path.endsWith(it) } ?: return direct
        val base = path.removeSuffix(suffix)

        return direct + listOf("${base}s", "${base}_planks", base, "${base}_top")
    }

    private fun tint(path: String, argb: Int): Int = when {
        path == "water" -> multiply(argb, WATER_TINT)
        path == "spruce_leaves" -> multiply(argb, SPRUCE_TINT)
        path == "birch_leaves" -> multiply(argb, BIRCH_TINT)
        path in GRASS_TINTED -> multiply(argb, GRASS_TINT)
        path.endsWith("_leaves") && path !in SELF_COLOURED_LEAVES -> multiply(argb, FOLIAGE_TINT)
        else -> argb
    }

    private fun averageTexture(at: Identifier): Int? {
        val resource = Minecraft.getInstance().resourceManager.getResource(at).orElse(null) ?: return null

        return try {
            resource.open().use { stream ->
                NativeImage.read(stream).use { image ->
                    var red = 0L
                    var green = 0L
                    var blue = 0L
                    var counted = 0L

                    for (y in 0 until image.height) {
                        for (x in 0 until image.width) {
                            val pixel = image.getPixel(x, y)
                            if (pixel ushr 24 and 0xFF < 128) continue
                            red += pixel ushr 16 and 0xFF
                            green += pixel ushr 8 and 0xFF
                            blue += pixel and 0xFF
                            counted++
                        }
                    }

                    if (counted == 0L) null
                    else (0xFF shl 24) or ((red / counted).toInt() shl 16) or
                            ((green / counted).toInt() shl 8) or (blue / counted).toInt()
                }
            }
        } catch (unreadable: Exception) {
            null
        }
    }

    private fun mapPaletteColour(key: String): Int {
        val block = BuiltInRegistries.BLOCK.getValue(
            Identifier.tryParse(key) ?: Identifier.withDefaultNamespace("air")
        )
        val colour = block.defaultMapColor()
        return if (colour == MapColor.NONE) 0xFF7F7F7F.toInt()
        else colour.calculateARGBColor(MapColor.Brightness.HIGH)
    }

    private fun darken(argb: Int, by: Float): Int {
        val red = ((argb ushr 16 and 0xFF) * by).toInt()
        val green = ((argb ushr 8 and 0xFF) * by).toInt()
        val blue = ((argb and 0xFF) * by).toInt()
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun multiply(argb: Int, tint: Int): Int {
        val red = (argb ushr 16 and 0xFF) * (tint ushr 16 and 0xFF) / 255
        val green = (argb ushr 8 and 0xFF) * (tint ushr 8 and 0xFF) / 255
        val blue = (argb and 0xFF) * (tint and 0xFF) / 255
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }
}
