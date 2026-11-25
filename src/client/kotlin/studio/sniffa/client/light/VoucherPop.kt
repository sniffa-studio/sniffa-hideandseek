package studio.sniffa.client.light

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek
import studio.sniffa.client.ui.text.ShineText
import studio.sniffa.client.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

object VoucherPop {

    private const val BURST = 40
    private const val TITLE_AFTER_MILLIS = 1300L
    private const val TITLE_FOR_MILLIS = 3800L
    private const val TITLE_FADE_IN = 250f
    private const val TITLE_FADE_OUT = 600f
    private const val SUPPRESS_FOR_MILLIS = 7000L
    private const val HEADLINE = "Gutschein gefunden!"
    private const val TEAM = "das Team meldet sich bei dir"
    private const val HEADLINE_SCALE = 4f
    private const val DETAIL_SCALE = 2f

    private class Shown(val card: VoucherGlow.Card, val practice: Boolean, val at: Long)

    @Volatile
    private var shown: Shown? = null

    fun install() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> shown = null }
        HudElementRegistry.addLast(Hideandseek.id("voucher_title")) { graphics, _ -> drawTitle(graphics) }
    }

    @JvmStatic
    fun drawsOwnTitle(text: String): Boolean {
        val popped = shown ?: return false
        if (System.currentTimeMillis() - popped.at > SUPPRESS_FOR_MILLIS) return false
        return text.contains(HEADLINE) || text.contains(TEAM)
    }

    private fun drawTitle(graphics: GuiGraphicsExtractor) {
        val popped = shown ?: return
        val since = System.currentTimeMillis() - popped.at - TITLE_AFTER_MILLIS
        if (since < 0) return
        if (since > TITLE_FOR_MILLIS) {
            if (since > SUPPRESS_FOR_MILLIS) shown = null
            return
        }
        val fade = minOf(since / TITLE_FADE_IN, (TITLE_FOR_MILLIS - since) / TITLE_FADE_OUT).coerceIn(0f, 1f)
        val font = Minecraft.getInstance().font
        val centreX = graphics.guiWidth() / 2f
        val centreY = graphics.guiHeight() / 2f
        val style = if (popped.card == VoucherGlow.Card.FIFTEEN) ShineText.Style.RED else ShineText.Style.BLUE

        graphics.pose().pushMatrix()
        graphics.pose().translate(centreX, centreY)
        graphics.pose().scale(HEADLINE_SCALE, HEADLINE_SCALE)
        ShineText.drawCentred(graphics, font, HEADLINE, 0f, -10f, style, fade, since, stagger = true, wave = true)
        graphics.pose().popMatrix()

        val value = "${popped.card.euros} €-Gutschein"
        val rest = " · $TEAM" + if (popped.practice) " (Debug)" else ""
        val width = font.width(value) + font.width(rest)
        val alpha = ((fade * 255f).toInt().coerceIn(0, 255)) shl 24
        graphics.pose().pushMatrix()
        graphics.pose().translate(centreX, centreY)
        graphics.pose().scale(DETAIL_SCALE, DETAIL_SCALE)
        val x = -width / 2
        graphics.text(font, value, x, 5, alpha or (ShineText.SHINE_GOLD and 0xFFFFFF), true)
        graphics.text(font, rest, x + font.width(value), 5, alpha or (Palette.TEXT and 0xFFFFFF), true)
        graphics.pose().popMatrix()
    }

    fun show(euros: Int, practice: Boolean = false) {
        val card = VoucherGlow.Card.entries.firstOrNull { it.euros == euros } ?: return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return
        val stack = ItemStack(Items.PAPER)
        stack.set(DataComponents.ITEM_MODEL, card.model)
        client.gameRenderer.displayItemActivation(stack)
        shown = Shown(card, practice, System.currentTimeMillis())
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
