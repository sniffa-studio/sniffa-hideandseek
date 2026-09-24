package studio.sniffa.client.state

import studio.sniffa.ability.Side

object RoundState {

    enum class Phase { IDLE, HIDING, HUNT, ENDED }

    enum class Role { SEEKER, HIDER, SPECTATOR }

    @Volatile
    var phase: Phase = Phase.IDLE
        private set

    @Volatile
    var elapsedSeconds: Int = 0
        private set

    @Volatile
    var remainingHiders: Int = 0
        private set

    @Volatile
    var points: Int? = null
        private set

    @Volatile
    var role: Role = Role.SPECTATOR
        private set

    val side: Side?
        get() = when (role) {
            Role.SEEKER -> Side.SEEKER
            Role.HIDER -> Side.HIDER
            Role.SPECTATOR -> null
        }

    val running: Boolean get() = phase == Phase.HIDING || phase == Phase.HUNT

    fun accept(phase: Int, elapsedSeconds: Int, remainingHiders: Int) {
        this.phase = Phase.entries.getOrElse(phase) { Phase.IDLE }
        this.elapsedSeconds = elapsedSeconds
        this.remainingHiders = remainingHiders
    }

    fun acceptPoints(points: Int) {
        this.points = points
    }

    fun acceptRole(ordinal: Int) {
        role = Role.entries.getOrElse(ordinal) { Role.SPECTATOR }
    }

    fun forget() {
        phase = Phase.IDLE
        elapsedSeconds = 0
        remainingHiders = 0
        points = null
        role = Role.SPECTATOR
    }
}
