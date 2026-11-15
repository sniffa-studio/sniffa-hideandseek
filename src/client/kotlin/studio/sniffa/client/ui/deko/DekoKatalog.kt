package studio.sniffa.client.ui.deko

import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import studio.sniffa.Hideandseek

object DekoKatalog {

    private val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(Hideandseek.id("deko"))

    private lateinit var key: KeyMapping
    private var pending = false

    fun install() {
        key = KeyMappingHelper.registerKeyMapping(KeyMapping("key.hideandseek.deko", GLFW.GLFW_KEY_J, CATEGORY))
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClientCommands.literal("deko").executes {
                pending = true
                1
            })
        }
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (key.consumeClick()) pending = true
            if (pending && client.gui.screen() == null) {
                pending = false
                open(client)
            }
        }
    }

    private fun open(client: Minecraft) {
        val player = client.player ?: return
        if (!DekoScreen.allowed(client)) {
            player.sendOverlayMessage(Component.literal("Der Deko-Katalog ist nur für das Bau-Team."))
            return
        }
        client.gui.setScreen(DekoScreen())
    }
}
