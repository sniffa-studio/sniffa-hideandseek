package studio.sniffa.client.ui.deko

import com.google.gson.JsonParser
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import studio.sniffa.Hideandseek
import java.io.InputStreamReader

class DekoScreen : Screen(Component.literal("Deko")) {

    data class Entry(val art: String, val title: String, val category: String, val icon: ItemStack)

    private data class Tool(val label: String, val command: String)

    private data class Hit(val x: Int, val y: Int, val w: Int, val h: Int, val action: () -> Unit, val tip: String?)

    private val entries = load()
    private val categories = listOf(ALL) + entries.map { it.category }.distinct()
    private var category = remembered
    private var query = ""
    private var scroll = 0
    private val hits = ArrayList<Hit>()
    private lateinit var search: EditBox

    private val shown: List<Entry>
        get() = entries.filter { (category == ALL || it.category == category) && (query.isBlank() || it.title.contains(query, true) || it.art.contains(query, true)) }

    override fun init() {
        search = EditBox(font, width - PAD - SEARCH_W, PAD, SEARCH_W, 16, Component.literal("Suchen"))
        search.setHint(Component.literal("Suchen …"))
        search.setValue(query)
        search.setResponder { value ->
            query = value
            scroll = 0
        }
        addRenderableWidget(search)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        graphics.fill(0, 0, width, height, BACKDROP)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        hits.clear()
        graphics.text(font, "Deko setzen", PAD, PAD + 4, GOLD, true)
        val hint = "Erst auf den Boden schauen, dann wählen"
        val hintX = PAD + font.width("Deko setzen") + 10
        if (hintX + font.width(hint) < width - PAD - SEARCH_W - 8) graphics.text(font, hint, hintX, PAD + 4, MUTED, false)

        var x = PAD
        var tabY = PAD + 24
        for (name in categories) {
            val w = font.width(name) + 12
            if (x + w > width - PAD) {
                x = PAD
                tabY += 20
            }
            val active = name == category
            graphics.fill(x, tabY, x + w, tabY + 16, if (active) TAB_ACTIVE else TAB)
            graphics.text(font, name, x + 6, tabY + 4, if (active) INK_DARK else INK, false)
            hits += Hit(x, tabY, w, 16, {
                category = name
                remembered = name
                scroll = 0
            }, null)
            x += w + 4
        }

        val toolRows = ArrayList<MutableList<Pair<Tool, Int>>>()
        var rowWidth = 0
        for (tool in TOOLS) {
            val w = font.width(tool.label) + 12
            if (toolRows.isEmpty() || rowWidth + w > width - 2 * PAD) {
                toolRows += ArrayList<Pair<Tool, Int>>()
                rowWidth = 0
            }
            toolRows.last() += tool to w
            rowWidth += w + 4
        }
        val toolTop = height - PAD - toolRows.size * 20 + 4
        for ((rowIndex, row) in toolRows.withIndex()) {
            x = PAD
            for ((tool, w) in row) {
                drawTool(graphics, tool, x, toolTop + rowIndex * 20, w, mouseX, mouseY)
                x += w + 4
            }
        }

        val top = tabY + 24
        val bottom = toolTop - 8
        val columns = ((width - 2 * PAD) / CELL).coerceAtLeast(1)
        val left = (width - columns * CELL) / 2
        val list = shown
        val rows = (list.size + columns - 1) / columns
        val visibleRows = ((bottom - top) / CELL).coerceAtLeast(1)
        scroll = scroll.coerceIn(0, (rows - visibleRows).coerceAtLeast(0))
        if (list.isEmpty()) {
            graphics.centeredText(font, "Nichts gefunden", width / 2, top + 20, MUTED)
        }
        for ((index, entry) in list.withIndex()) {
            val row = index / columns - scroll
            if (row < 0 || row >= visibleRows) continue
            val cx = left + (index % columns) * CELL
            val cy = top + row * CELL
            val hover = mouseX in cx until cx + CELL - 4 && mouseY in cy until cy + CELL - 4
            graphics.fill(cx, cy, cx + CELL - 4, cy + CELL - 4, if (hover) CELL_HOVER else CELL_BG)
            graphics.pose().pushMatrix()
            graphics.pose().translate((cx + (CELL - 4) / 2 - 16).toFloat(), (cy + 3).toFloat())
            graphics.pose().scale(2f, 2f)
            graphics.item(entry.icon, 0, 0)
            graphics.pose().popMatrix()
            val label = font.plainSubstrByWidth(entry.title, ((CELL - 6) / LABEL).toInt())
            graphics.pose().pushMatrix()
            graphics.pose().translate((cx + (CELL - 4) / 2).toFloat(), (cy + CELL - 13).toFloat())
            graphics.pose().scale(LABEL, LABEL)
            graphics.centeredText(font, label, 0, 0, INK)
            graphics.pose().popMatrix()
            hits += Hit(cx, cy, CELL - 4, CELL - 4, { run(entry.art) }, "${entry.title}  ·  /function sniffa:${entry.art}")
        }
        if (rows > visibleRows) {
            graphics.text(font, "${scroll + 1}–${(scroll + visibleRows).coerceAtMost(rows)} von $rows Reihen · Mausrad", PAD, bottom - 2, MUTED, false)
        }
        hits.firstOrNull { mouseX in it.x until it.x + it.w && mouseY in it.y until it.y + it.h && it.tip != null }?.let {
            graphics.setTooltipForNextFrame(Component.literal(it.tip!!), mouseX, mouseY)
        }
    }

    private fun drawTool(graphics: GuiGraphicsExtractor, tool: Tool, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val hover = mouseX in x until x + w && mouseY in y until y + 16
        graphics.fill(x, y, x + w, y + 16, if (hover) TOOL_HOVER else TOOL)
        graphics.text(font, tool.label, x + 6, y + 4, INK, false)
        hits += Hit(x, y, w, 16, { run(tool.command) }, "/function sniffa:${tool.command}")
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            val x = event.x().toInt()
            val y = event.y().toInt()
            hits.firstOrNull { x in it.x until it.x + it.w && y in it.y until it.y + it.h }?.let {
                it.action()
                return true
            }
        }
        return super.mouseClicked(event, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (verticalAmount != 0.0) {
            scroll += if (verticalAmount < 0) 1 else -1
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun isPauseScreen(): Boolean = false

    private fun run(command: String) {
        val client = Minecraft.getInstance()
        onClose()
        client.player?.connection?.sendCommand("function sniffa:$command")
    }

    companion object {
        private const val ALL = "Alle"
        private const val PAD = 12
        private const val CELL = 56
        private const val SEARCH_W = 140
        private const val LABEL = 0.75f
        private const val BACKDROP = 0xD0101521.toInt()
        private const val TAB = 0xFF2A3245.toInt()
        private const val TAB_ACTIVE = 0xFFF2C14E.toInt()
        private const val TOOL = 0xFF34303C.toInt()
        private const val TOOL_HOVER = 0xFF4A4352.toInt()
        private const val CELL_BG = 0x60FFFFFF
        private const val CELL_HOVER = 0xA0F2C14E.toInt()
        private const val GOLD = 0xFFF2C14E.toInt()
        private const val INK = 0xFFF1EDE4.toInt()
        private const val INK_DARK = 0xFF1A1410.toInt()
        private const val MUTED = 0xFFA9B1C2.toInt()
        private val ALLOWED = setOf("jakubxyz", "corvinderayri", "p2pe")
        private var remembered = ALL

        private val TOOLS = listOf(
            Tool("Entfernen", "deko_weg"),
            Tool("Geschenk umfärben", "deko_farbe"),
            Tool("Eisläufer klein", "eislaeufer_klein"),
            Tool("Eisläufer groß", "eislaeufer_gross"),
            Tool("Lichterkette Anfang", "lichterkette_start"),
            Tool("Lichterkette Ende", "lichterkette_ende"),
            Tool("Baum-Lichterkette", "lichterkette_baum"),
            Tool("Eiszapfen Anfang", "eiszapfen_start"),
            Tool("Eiszapfen Ende", "eiszapfen_ende"),
            Tool("Rauch", "rauch"),
            Tool("Scheinwerfer", "scheinwerfer"),
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
                Entry(item.get("art").asString, item.get("title").asString, item.get("category").asString, stack)
            }
        }
    }
}
