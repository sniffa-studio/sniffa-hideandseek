package studio.sniffa.client.light

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.cos
import kotlin.math.sin

object VoucherPop {

    private const val BURST = 40

    fun show(euros: Int) {
        val card = VoucherGlow.Card.entries.firstOrNull { it.euros == euros } ?: return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return
        val stack = ItemStack(Items.PAPER)
        stack.set(DataComponents.ITEM_MODEL, card.model)
        client.gameRenderer.displayItemActivation(stack)
        val random = level.random
        val dust = DustParticleOptions(card.glow, 0.9f)
        val gold = DustParticleOptions(0xF2C65A, 0.6f)
        for (i in 0 until BURST) {
            val angle = random.nextDouble() * Math.PI * 2
            val reach = 1.0 + random.nextDouble() * 0.5
            val lift = random.nextDouble() * 1.1
            val speed = 0.06 + random.nextDouble() * 0.1
            level.addParticle(
                if (i % 4 == 0) gold else dust,
                player.x + cos(angle) * reach, player.y + 0.1 + lift, player.z + sin(angle) * reach,
                cos(angle) * speed, 0.04 + random.nextDouble() * 0.08, sin(angle) * speed,
            )
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.END_ROD, player.x + cos(angle) * 0.9, player.y + 0.3 + lift * 0.5, player.z + sin(angle) * 0.9,
                    cos(angle) * speed, random.nextDouble() * 0.1, sin(angle) * speed)
            }
        }
    }
}
