package studio.sniffa.client.ui.inventory

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import studio.sniffa.client.net.Handshake

object FlatHeads {

    private const val SIZE = 16

    fun overdraw(graphics: GuiGraphics, slot: Slot) {
        if (Handshake.state != Handshake.State.REGISTERED) return

        val stack = slot.item
        if (!stack.`is`(Items.PLAYER_HEAD)) return

        val profile = stack.get(DataComponents.PROFILE) ?: return
        val skin = Minecraft.getInstance().skinManager
            .createLookup(profile.partialProfile(), false)
            .get() ?: return

        PlayerFaceRenderer.draw(graphics, skin, slot.x, slot.y, SIZE)
    }
}
