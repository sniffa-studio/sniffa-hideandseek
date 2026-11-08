package studio.sniffa.client.ui.title

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.fabricmc.fabric.api.event.Event
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ConfirmLinkScreen
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.Identifier
import studio.sniffa.Hideandseek
import studio.sniffa.client.Settings
import studio.sniffa.client.ui.theme.Textures

object TitleScreenSkin {

    private const val EVENT_ADDRESS = "event.sniffa.studio"

    private const val DISCORD = "https://discord.sniffa.studio"

    private const val OPTIONS = "menu.options"
    private const val QUIT = "menu.quit"

    private val REPLAY_BUTTONS = setOf("flashback.open_replays", "replaymod.gui.replayviewer")

    private const val FULL_WIDTH = 240
    private const val GAP = 6
    private const val HALF_WIDTH = (FULL_WIDTH - GAP) / 2
    private const val ROW_HEIGHT = 28
    private const val ROW_GAP = 6

    private const val LOCKUP_HEIGHT = 14
    private const val LOCKUP_GAP = 4
    private const val MARGIN = 8

    private const val STATUS_GAP = 10

    private val AFTER_OTHER_MODS = Hideandseek.id("after_other_mods")

    private var statusY = 0

    fun install() {
        ScreenEvents.AFTER_INIT.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_OTHER_MODS)
        ScreenEvents.AFTER_INIT.register(AFTER_OTHER_MODS) { _, screen, _, _ ->
            if (screen !is TitleScreen) return@register
            rebuild(screen)
            if (Settings.pingEventServer) EventStatus.refresh(EVENT_ADDRESS)

            ScreenEvents.afterRender(screen).register { _, graphics, _, _, _ ->
                WinterScene.front(graphics, screen.width, screen.height, Screens.getButtons(screen))
                EventStatus.tick()
                status(graphics, screen)
                sign(graphics)
            }
            ScreenEvents.remove(screen).register { EventStatus.stop() }
        }
    }

    private fun status(graphics: GuiGraphics, screen: TitleScreen) {
        val line = EventStatus.line() ?: return
        WinterScene.plate(graphics, screen.font.width(line), screen.width / 2, statusY)
        graphics.drawCenteredString(screen.font, line, screen.width / 2, statusY, -1)
    }

    private fun rebuild(screen: TitleScreen) {
        val buttons = Screens.getButtons(screen)

        val anchorX = screen.width / 2 - FULL_WIDTH / 2
        val anchorY = screen.height / 4 + 48
        var options: AbstractWidget? = null
        var quit: AbstractWidget? = null
        val replays = mutableListOf<AbstractWidget>()

        for (widget in buttons) {
            val key = keyOf(widget)
            when (key) {
                OPTIONS -> options = widget
                QUIT -> quit = widget
                in REPLAY_BUTTONS -> replays += widget
            }
            widget.visible = key in REPLAY_BUTTONS
        }

        buttons.add(
            Button.builder(Component.translatable("menu.hideandseek.join_event")) { connect(screen) }
                .bounds(anchorX, anchorY, HALF_WIDTH, ROW_HEIGHT)
                .build()
        )
        place(quit, anchorX + HALF_WIDTH + GAP, anchorY, HALF_WIDTH)
        place(options, anchorX, anchorY + ROW_HEIGHT + ROW_GAP, HALF_WIDTH)
        buttons.add(
            Button.builder(Component.translatable("menu.hideandseek.discord")) {
                ConfirmLinkScreen.confirmLinkNow(screen, DISCORD, true)
            }
                .bounds(anchorX + HALF_WIDTH + GAP, anchorY + ROW_HEIGHT + ROW_GAP, HALF_WIDTH, ROW_HEIGHT)
                .build()
        )

        if (replays.isNotEmpty()) {
            val pinReplays = {
                replays.forEachIndexed { row, widget ->
                    widget.setX(anchorX + FULL_WIDTH + GAP)
                    widget.setY(anchorY + row * (ROW_HEIGHT + ROW_GAP) + (ROW_HEIGHT - widget.height) / 2)
                    widget.visible = true
                }
            }
            pinReplays()
            ScreenEvents.afterTick(screen).register { pinReplays() }
            ScreenEvents.beforeRender(screen).register { _, _, _, _, _ -> pinReplays() }
        }

        statusY = anchorY + 2 * ROW_HEIGHT + ROW_GAP + STATUS_GAP
    }

    private fun connect(screen: Screen) {
        val client = Minecraft.getInstance()
        val server = ServerData("Sniffa Event", EVENT_ADDRESS, ServerData.Type.OTHER)
        ConnectScreen.startConnecting(
            screen,
            client,
            ServerAddress.parseString(EVENT_ADDRESS),
            server,
            false,
            null,
        )
    }

    private fun place(widget: AbstractWidget?, x: Int, y: Int, width: Int) {
        if (widget == null) {
            return
        }
        widget.setX(x)
        widget.setY(y)
        widget.setWidth(width)
        widget.setHeight(ROW_HEIGHT)
        widget.visible = true
    }

    private fun keyOf(widget: AbstractWidget): String? =
        (widget.message.contents as? TranslatableContents)?.key

    private fun sign(graphics: GuiGraphics) {
        val markWidth = widthFor(Textures.MARK_SOURCE_WIDTH, Textures.MARK_SOURCE_HEIGHT)
        val wordWidth = widthFor(Textures.WORDMARK_SOURCE_WIDTH, Textures.WORDMARK_SOURCE_HEIGHT)

        val y = graphics.guiHeight() - LOCKUP_HEIGHT - MARGIN - WinterScene.SIGN_LIFT
        val left = graphics.guiWidth() - MARGIN - wordWidth - LOCKUP_GAP - markWidth

        draw(graphics, Textures.SNIFFA, left, y, markWidth,
            Textures.MARK_SOURCE_WIDTH, Textures.MARK_SOURCE_HEIGHT)

        draw(graphics, Textures.SNIFFA_TEXT, left + markWidth + LOCKUP_GAP, y, wordWidth,
            Textures.WORDMARK_SOURCE_WIDTH, Textures.WORDMARK_SOURCE_HEIGHT)
    }

    private fun widthFor(sourceWidth: Int, sourceHeight: Int): Int =
        (LOCKUP_HEIGHT * sourceWidth + sourceHeight / 2) / sourceHeight

    private fun draw(
        graphics: GuiGraphics,
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            0f, 0f,
            width, LOCKUP_HEIGHT,
            sourceWidth, sourceHeight,
            sourceWidth, sourceHeight,
        )
    }
}
