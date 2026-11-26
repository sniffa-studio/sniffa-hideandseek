package studio.sniffa.client.ui.sound

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object UiSound {

    fun wedgeFocused(affordable: Boolean) {
        val now = System.nanoTime()
        if (now - lastPlayed < MINIMUM_GAP_NANOS) return
        lastPlayed = now

        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(),
                if (affordable) PITCH_AFFORDABLE else PITCH_DENIED,
                VOLUME,
            )
        )
    }

    fun purchased() {
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, PITCH_PURCHASE, VOLUME_PURCHASE)
        )
    }

    fun hotColdBlip(strength: Float) {
        val heat = strength.coerceIn(0f, 1f)

        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(
                SoundEvents.NOTE_BLOCK_BIT.value(),
                PITCH_BLIP_COOL + (PITCH_BLIP_HOT - PITCH_BLIP_COOL) * heat,
                VOLUME_BLIP,
            )
        )
    }

    fun rangeLocked() {
        Minecraft.getInstance().soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BIT.value(), PITCH_LOCK, VOLUME_LOCK)
        )
    }

    private var lastPlayed = 0L

    private const val VOLUME = 0.25f

    private const val PITCH_AFFORDABLE = 1.35f

    private const val PITCH_DENIED = 0.9f

    private const val MINIMUM_GAP_NANOS = 40_000_000L

    private const val VOLUME_PURCHASE = 0.45f
    private const val PITCH_PURCHASE = 1.3f

    private const val VOLUME_BLIP = 0.30f
    private const val PITCH_BLIP_COOL = 0.8f
    private const val PITCH_BLIP_HOT = 1.9f

    private const val VOLUME_LOCK = 0.35f
    private const val PITCH_LOCK = 1.6f
}
