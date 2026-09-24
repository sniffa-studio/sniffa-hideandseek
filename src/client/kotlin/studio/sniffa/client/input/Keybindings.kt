package studio.sniffa.client.input

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.Screen
import studio.sniffa.ability.AbilityCatalog
import studio.sniffa.client.state.RoundState
import studio.sniffa.client.ui.shop.AbilityShopScreen

object Keybindings {

    private lateinit var openShop: KeyMapping

    fun install() {
        openShop = KeyBindingHelper.registerKeyBinding(
            KeyMapping("key.hideandseek.open_shop", GLFW.GLFW_KEY_G, KeyMapping.Category.GAMEPLAY)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openShop.consumeClick()) {
                val refusal = why(client) ?: run {
                    open(client, AbilityShopScreen())
                    continue
                }
                client.player?.displayClientMessage(Component.literal(refusal), true)
            }
        }
    }

    private fun why(client: Minecraft): String? = when {
        client.player == null -> ""
        RoundState.phase == RoundState.Phase.HIDING ->
            "Solange versteckt wird, kauft niemand."
        AbilityCatalog.forSide(RoundState.side).isEmpty() ->
            if (RoundState.running) "Du schaust nur zu." else "Es läuft keine Runde."
        else -> null
    }

    private fun open(client: Minecraft, screen: Screen) {
        if (client.screen != null) return
        client.setScreen(screen)
    }
}
