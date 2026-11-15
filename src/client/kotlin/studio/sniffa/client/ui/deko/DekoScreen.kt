package studio.sniffa.client.ui.deko

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import studio.sniffa.Hideandseek
import java.io.InputStreamReader

class DekoScreen : Screen(Component.literal("Deko")) {

    class Entry(
        val command: String,
        val title: String,
        val category: String,
        val icon: ItemStack,
        val text: String,
        val size: String?,
        val place: String?,
        val features: List<String>,
    )

    private class Hit(val x: Int, val y: Int, val w: Int, val h: Int, val action: (Boolean) -> Unit, val lore: List<FormattedCharSequence>?)

    private val entries = load() + TOOLS
    private val categories = entries.map { it.category }.distinct()
    private var category: String? = remembered
    private var page = 0
    private var query = ""
    private val hits = ArrayList<Hit>()
    private lateinit var search: EditBox

    private val shown: List<Entry>
        get() = when {
            query.isNotBlank() -> entries.filter { it.title.contains(query, true) || it.text.contains(query, true) || it.command.contains(query, true) }
            category != null -> entries.filter { it.category == category }
            else -> emptyList()
        }

    override fun init() {
        search = EditBox(font, 0, 0, SEARCH_W, 14, Component.literal("Suchen"))
        search.setHint(Component.literal("Suchen …"))
        search.setValue(query)
        search.setResponder { value ->
            query = value
            page = 0
        }
        addRenderableWidget(search)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, BACKDROP)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        hits.clear()
        val columns = ((width - 2 * MARGIN - 2 * INNER) / SLOT).coerceIn(4, 15)
        val panelW = columns * SLOT + 2 * INNER
        val panelX = (width - panelW) / 2
        val panelY = MARGIN
        val panelH = height - 2 * MARGIN
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, FRAME)
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL)
        graphics.fill(panelX, panelY, panelX + panelW, panelY + HEADER, PANEL_HEAD)

        search.setX(panelX + panelW - INNER - SEARCH_W)
        search.setY(panelY + 5)
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        val gridTop = panelY + HEADER + 24
        val gridBottom = panelY + panelH - 22
        val rows = ((gridBottom - gridTop) / SLOT).coerceAtLeast(1)
        val gridX = panelX + INNER

        if (query.isBlank() && category == null) {
            graphics.text(font, "Deko-Katalog", panelX + INNER, panelY + 8, GOLD, true)
            graphics.text(font, "Kategorie wählen", panelX + INNER, panelY + HEADER + 8, MUTED, false)
            home(graphics, gridX, gridTop, columns, mouseX, mouseY)
            footer(graphics, panelX, panelY + panelH - 18, panelW, "Erst auf den Boden schauen, dann wählen. Esc schließt.")
        } else {
            val title = if (query.isNotBlank()) "Suche: $query" else category!!
            graphics.text(font, font.plainSubstrByWidth(title, panelW - SEARCH_W - 3 * INNER), panelX + INNER, panelY + 8, GOLD, true)
            button(graphics, "‹ Kategorien", panelX + INNER, panelY + HEADER + 4, mouseX, mouseY) {
                category = null
                remembered = null
                query = ""
                search.setValue("")
                page = 0
            }
            val list = shown
            val perPage = rows * columns
            val pages = ((list.size + perPage - 1) / perPage).coerceAtLeast(1)
            page = page.coerceIn(0, pages - 1)
            val label = "Seite ${page + 1} von $pages"
            val nextX = panelX + panelW - INNER - 16
            val labelX = nextX - 6 - font.width(label)
            button(graphics, "‹", labelX - 6 - 16, panelY + HEADER + 4, mouseX, mouseY, page > 0) { page-- }
            graphics.text(font, label, labelX, panelY + HEADER + 8, INK, false)
            button(graphics, "›", nextX, panelY + HEADER + 4, mouseX, mouseY, page < pages - 1) { page++ }
            if (list.isEmpty()) graphics.centeredText(font, "Nichts gefunden", panelX + panelW / 2, gridTop + 20, MUTED)
            for ((slot, entry) in list.drop(page * perPage).take(perPage).withIndex()) {
                val sx = gridX + (slot % columns) * SLOT
                val sy = gridTop + (slot / columns) * SLOT
                slot(graphics, entry.icon, sx, sy, mouseX, mouseY)
                hits += Hit(sx, sy, SLOT - 2, SLOT - 2, { shift -> pick(entry, shift) }, lore(entry))
            }
            footer(graphics, panelX, panelY + panelH - 18, panelW, "${list.size} Teile · Klick: ins Inventar · Shift + Klick: direkt setzen · Mausrad blättert")
        }

        hits.firstOrNull { it.lore != null && mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h }?.let {
            graphics.setTooltipForNextFrame(it.lore!!, mouseX, mouseY)
        }
    }

    private fun home(graphics: GuiGraphicsExtractor, x0: Int, y0: Int, columns: Int, mouseX: Int, mouseY: Int) {
        val perRow = (columns / 3).coerceAtLeast(1)
        val tileW = columns * SLOT / perRow
        for ((index, name) in categories.withIndex()) {
            val tx = x0 + (index % perRow) * tileW
            val ty = y0 + (index / perRow) * (SLOT + 4)
            val hover = mouseX in tx until tx + tileW - 4 && mouseY in ty until ty + SLOT
            graphics.fill(tx, ty, tx + tileW - 4, ty + SLOT, if (hover) SLOT_HOVER else SLOT_BG)
            val icon = entries.firstOrNull { it.command == CATEGORY_ICON[name] }?.icon ?: entries.first { it.category == name }.icon
            drawIcon(graphics, icon, tx + 3, ty + 3)
            val count = entries.count { it.category == name }
            graphics.text(font, name, tx + SLOT + 2, ty + 10, if (hover) INK_DARK else INK, false)
            graphics.text(font, "$count Teile", tx + SLOT + 2, ty + 22, if (hover) INK_DARK else MUTED, false)
            hits += Hit(tx, ty, tileW - 4, SLOT, { _ ->
                category = name
                remembered = name
                page = 0
            }, null)
        }
    }

    private fun slot(graphics: GuiGraphicsExtractor, icon: ItemStack, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        val hover = mouseX in x until x + SLOT - 2 && mouseY in y until y + SLOT - 2
        graphics.fill(x, y, x + SLOT - 2, y + SLOT - 2, if (hover) SLOT_HOVER else SLOT_BG)
        graphics.fill(x, y, x + SLOT - 2, y + 1, SLOT_EDGE)
        graphics.fill(x, y, x + 1, y + SLOT - 2, SLOT_EDGE)
        drawIcon(graphics, icon, x + 2, y + 2)
    }

    private fun drawIcon(graphics: GuiGraphicsExtractor, icon: ItemStack, x: Int, y: Int) {
        if (icon.get(DataComponents.ITEM_MODEL)?.path?.contains("/katalog/") == true) {
            graphics.item(icon, x + 8, y + 8)
            return
        }
        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(2f, 2f)
        graphics.item(icon, 0, 0)
        graphics.pose().popMatrix()
    }

    private fun button(graphics: GuiGraphicsExtractor, label: String, x: Int, y: Int, mouseX: Int, mouseY: Int, active: Boolean = true, action: () -> Unit) {
        val w = (font.width(label) + 10).coerceAtLeast(16)
        val hover = active && mouseX in x until x + w && mouseY in y until y + 16
        graphics.fill(x, y, x + w, y + 16, if (!active) BUTTON_OFF else if (hover) SLOT_HOVER else BUTTON)
        graphics.text(font, label, x + (w - font.width(label)) / 2, y + 4, if (!active) MUTED else if (hover) INK_DARK else INK, false)
        if (active) hits += Hit(x, y, w, 16, { _ -> action() }, null)
    }

    private fun footer(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, text: String) {
        graphics.text(font, font.plainSubstrByWidth(text, w - 2 * INNER), x + INNER, y + 5, MUTED, false)
    }

    private fun lore(entry: Entry): List<FormattedCharSequence> {
        val lines = ArrayList<Component>()
        lines += Component.literal(entry.title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        if (entry.text.isNotBlank()) lines += Component.literal(entry.text).withStyle(ChatFormatting.GRAY)
        lines += Component.empty()
        entry.size?.let { lines += Component.literal("Größe: ").withStyle(ChatFormatting.DARK_AQUA).append(Component.literal(it).withStyle(ChatFormatting.WHITE)) }
        entry.place?.let { lines += Component.literal("Platz: ").withStyle(ChatFormatting.DARK_AQUA).append(Component.literal(it).withStyle(ChatFormatting.WHITE)) }
        for (feature in entry.features) lines += Component.literal("✦ $feature").withStyle(ChatFormatting.YELLOW)
        if (entry.size != null || entry.place != null || entry.features.isNotEmpty()) lines += Component.empty()
        if (entry.category == TOOLS_CATEGORY) {
            lines += Component.literal("Klick: benutzen").withStyle(ChatFormatting.GREEN)
        } else {
            lines += Component.literal("Klick: ins Inventar").withStyle(ChatFormatting.GREEN)
            lines += Component.literal("Shift + Klick: direkt setzen").withStyle(ChatFormatting.GREEN)
        }
        lines += Component.literal("/function sniffa:${entry.command}").withStyle(ChatFormatting.DARK_GRAY)
        return lines.flatMap { line -> font.split(line, LORE_WIDTH).ifEmpty { listOf(FormattedCharSequence.EMPTY) } }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            val x = event.x().toInt()
            val y = event.y().toInt()
            hits.firstOrNull { x in it.x until it.x + it.w && y in it.y until it.y + it.h }?.let {
                it.action(event.hasShiftDown())
                return true
            }
        }
        return super.mouseClicked(event, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount != 0.0 && (category != null || query.isNotBlank())) {
            page = (page + if (verticalAmount < 0) 1 else -1).coerceAtLeast(0)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isFocused && category != null) {
            category = null
            remembered = null
            return true
        }
        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false

    private fun pick(entry: Entry, shift: Boolean) {
        if (entry.category == TOOLS_CATEGORY || shift) {
            run(entry.command)
            return
        }
        val player = Minecraft.getInstance().player ?: return
        player.connection.sendCommand("function sniffa:item/${entry.command}")
        player.sendOverlayMessage(Component.literal("${entry.title} liegt im Inventar"))
    }

    private fun run(command: String) {
        val client = Minecraft.getInstance()
        onClose()
        client.player?.connection?.sendCommand("function sniffa:$command")
    }

    companion object {
        private const val MARGIN = 16
        private const val INNER = 10
        private const val HEADER = 24
        private const val SLOT = 38
        private const val SEARCH_W = 120
        private const val LORE_WIDTH = 220
        private const val BACKDROP = 0xB0080B12.toInt()
        private const val FRAME = 0xFF8C6A2E.toInt()
        private const val PANEL = 0xF01A1F2B.toInt()
        private const val PANEL_HEAD = 0xFF262D3D.toInt()
        private const val SLOT_BG = 0xFF2C3446.toInt()
        private const val SLOT_EDGE = 0xFF151A24.toInt()
        private const val SLOT_HOVER = 0xFFF2C14E.toInt()
        private const val BUTTON = 0xFF34303C.toInt()
        private const val BUTTON_OFF = 0xFF23212A.toInt()
        private const val GOLD = 0xFFF2C14E.toInt()
        private const val INK = 0xFFF1EDE4.toInt()
        private const val INK_DARK = 0xFF1A1410.toInt()
        private const val MUTED = 0xFF9AA3B5.toInt()
        private const val TOOLS_CATEGORY = "Werkzeuge"
        private val ALLOWED = setOf("jakubxyz", "corvinderayri", "p2pe")
        private var remembered: String? = null

        private val CATEGORY_ICON = mapOf(
            "Stände" to "marktstand",
            "Markt" to "karussell",
            "Licht" to "laterne",
            "Bauteile" to "holzzaun",
            "Geschenke" to "geschenkstapel",
            "Fabrik" to "riesen_verpackung",
            "Wand/Decke" to "tuerkranz",
            TOOLS_CATEGORY to "deko_weg",
        )

        private fun tool(command: String, title: String, text: String, icon: String): Entry {
            val stack = ItemStack(Items.STICK)
            stack.set(DataComponents.ITEM_MODEL, Identifier.parse(icon))
            return Entry(command, title, TOOLS_CATEGORY, stack, text, null, null, emptyList())
        }

        private val TOOLS: List<Entry>
            get() = listOf(
                tool("deko_weg", "Entfernen", "Entfernt das angeschaute Teil. Zäune und Banden verbinden sich danach neu.", "minecraft:barrier"),
                tool("deko_farbe", "Geschenk umfärben", "Gibt dem angeschauten Geschenk die nächste Farbe.", "minecraft:red_dye"),
                tool("eislaeufer_klein", "Eisläufer, kleiner Kreis", "Läufer zieht Kreise mit 3 Blöcken Radius um die angeschaute Stelle.", "hideandseek:props/deko/eislaeufer_2"),
                tool("eislaeufer_gross", "Eisläufer, großer Kreis", "Läufer zieht Kreise mit 6 Blöcken Radius um die angeschaute Stelle.", "hideandseek:props/deko/eislaeufer_3"),
                tool("lichterkette_start", "Lichterkette: Anfang", "Setzt den Anfang einer Lichterkette an den angeschauten Punkt.", "minecraft:glowstone_dust"),
                tool("lichterkette_ende", "Lichterkette: nächster Punkt", "Spannt die Kette zum angeschauten Punkt. Mehrmals für lange Ketten.", "minecraft:glowstone_dust"),
                tool("lichterkette_baum", "Baum-Lichterkette", "Schmückt den angeschauten Baum mit einer Lichterspirale.", "minecraft:spruce_sapling"),
                tool("eiszapfen_start", "Eiszapfen: Anfang", "Anfang einer Eiszapfen-Reihe an einer Dachkante.", "minecraft:ice"),
                tool("eiszapfen_ende", "Eiszapfen: Ende", "Ende der Eiszapfen-Reihe.", "minecraft:ice"),
                tool("rauch", "Schornsteinrauch", "Rauch aus dem angeschauten Schornstein.", "minecraft:campfire"),
                tool("scheinwerfer", "Scheinwerfer", "Setzt einen Scheinwerfer auf den angeschauten Block.", "hideandseek:props/scheinwerfer_kopf"),
            )

        fun allowed(client: Minecraft): Boolean =
            FabricLoader.getInstance().isDevelopmentEnvironment || client.user.name.lowercase() in ALLOWED

        private fun load(): List<Entry> {
            val resource = Minecraft.getInstance().resourceManager.getResource(Hideandseek.id("deko_katalog.json")).orElse(null) ?: return emptyList()
            val root = resource.open().use { JsonParser.parseReader(InputStreamReader(it, Charsets.UTF_8)).asJsonObject }
            return root.getAsJsonArray("props").map { element ->
                val item = element.asJsonObject
                val stack = ItemStack(Items.STICK)
                stack.set(DataComponents.ITEM_MODEL, Identifier.parse(item.get("icon").asString))
                Entry(
                    item.get("art").asString,
                    item.get("title").asString,
                    item.get("category").asString,
                    stack,
                    item.get("text")?.asString ?: "",
                    item.get("size")?.asString,
                    item.get("place")?.asString,
                    item.getAsJsonArray("features")?.map { it.asString } ?: emptyList(),
                )
            }
        }
    }
}
