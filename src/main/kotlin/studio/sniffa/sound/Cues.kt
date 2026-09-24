package studio.sniffa.sound

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import studio.sniffa.Hideandseek

object Cues {

    val ROUND_OPENS: SoundEvent = register("cue.round_opens")
    val HUNT_BEGINS: SoundEvent = register("cue.hunt_begins")
    val SEEKER_WINS: SoundEvent = register("cue.seeker_wins")
    val HIDERS_WIN: SoundEvent = register("cue.hiders_win")
    val CAUGHT: SoundEvent = register("cue.caught")
    val SPOTTED: SoundEvent = register("cue.spotted")
    val RADAR_PING: SoundEvent = register("cue.radar_ping")
    val TICK: SoundEvent = register("cue.tick")
    val TICK_LAST: SoundEvent = register("cue.tick_last")
    val JAM: SoundEvent = register("cue.jam")
    val INSTRUMENT: SoundEvent = register("cue.instrument")

    fun install() {
    }

    private fun register(path: String): SoundEvent {
        val id: Identifier = Hideandseek.id(path)
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))
    }
}
