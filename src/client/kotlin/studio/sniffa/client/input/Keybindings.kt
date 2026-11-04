package studio.sniffa.client.input

import studio.sniffa.Hideandseek
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

    private val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(Hideandseek.id("main"))

    private lateinit var openShop: KeyMapping
    private lateinit var openWatch: KeyMapping

    fun install() {
        openShop = KeyBindingHelper.registerKeyBinding(
            KeyMapping("key.hideandseek.open_shop", GLFW.GLFW_KEY_Z, CATEGORY)
        )

        openWatch = KeyBindingHelper.registerKeyBinding(
            KeyMapping("key.hideandseek.open_watch", GLFW.GLFW_KEY_R, CATEGORY)
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (openWatch.consumeClick()) {
                watch(client)
            }
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
        RoundState.clock == RoundState.Clock.PAUSED ->
            "Die Runde ist pausiert."
        AbilityCatalog.forSide(RoundState.side).isEmpty() ->
            if (RoundState.running) "Du schaust nur zu." else "Es läuft keine Runde."
        else -> null
    }

    fun shopKey(): Component = openShop.translatedKeyMessage

    fun watchKey(): Component = openWatch.translatedKeyMessage

    private fun watch(client: Minecraft) {
        val player = client.player ?: return
        if (client.screen != null) return
        if (RoundState.role != RoundState.Role.STAFF || !RoundState.running) {
            player.displayClientMessage(Component.literal("Der Teleporter ist nur für die Regie, während eine Runde läuft."), true)
            return
        }
        player.connection.sendCommand("hswatch")
    }

    private fun open(client: Minecraft, screen: Screen) {
        if (client.screen != null) return
        client.setScreen(screen)
    }
}
