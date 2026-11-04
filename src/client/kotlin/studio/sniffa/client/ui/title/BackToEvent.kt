package studio.sniffa.client.ui.title

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.Component
import studio.sniffa.client.net.Handshake

object BackToEvent {

    private const val WIDTH = 200
    private const val HEIGHT = 20
    private const val GAP = 4

    fun install() {
        ScreenEvents.AFTER_INIT.register { _, screen, width, height ->
            if (screen !is DisconnectedScreen) return@register
            val address = Handshake.eventAddress ?: return@register

            val buttons = Screens.getButtons(screen)
            val below = buttons.maxOfOrNull { it.y + it.height }?.plus(GAP) ?: (height / 2 + 40)
            buttons.add(
                Button.builder(Component.literal("Zurück zum Event")) { connect(address) }
                    .bounds(width / 2 - WIDTH / 2, below, WIDTH, HEIGHT)
                    .build()
            )
        }
    }

    private fun connect(address: String) {
        val client = Minecraft.getInstance()
        val parent: Screen = TitleScreen()
        ConnectScreen.startConnecting(
            parent,
            client,
            ServerAddress.parseString(address),
            ServerData("Sniffa Event", address, ServerData.Type.OTHER),
            false,
            null,
        )
    }
}
