package studio.sniffa.client.state

import studio.sniffa.ability.Side

object RoundState {

    enum class Phase { IDLE, HIDING, HUNT, ENDED }

    enum class Role { SEEKER, HIDER, SPECTATOR, STAFF }

    enum class Clock { HIDING, HUNT, PAUSED }

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

    @Volatile
    var clock: Clock = Clock.HIDING
        private set

    @Volatile
    var clockSeconds: Int = 0
        private set

    @Volatile
    var progress: Float = 1f
        private set

    @Volatile
    var hidersAtStart: Int = 0
        private set

    @Volatile
    var seekers: Int = 0
        private set

    val side: Side?
        get() = when (role) {
            Role.SEEKER -> Side.SEEKER
            Role.HIDER -> Side.HIDER
            Role.SPECTATOR, Role.STAFF -> null
        }

    val running: Boolean get() = phase == Phase.HIDING || phase == Phase.HUNT

    fun accept(phase: Int, elapsedSeconds: Int, remainingHiders: Int) {
        this.phase = Phase.entries.getOrElse(phase) { Phase.IDLE }
        this.elapsedSeconds = elapsedSeconds
        this.remainingHiders = remainingHiders
    }

    fun acceptClock(state: Int, seconds: Int, progress: Float, hidersAtStart: Int, seekers: Int) {
        clock = Clock.entries.getOrElse(state) { Clock.HIDING }
        clockSeconds = seconds
        this.progress = progress.coerceIn(0f, 1f)
        this.hidersAtStart = hidersAtStart
        this.seekers = seekers
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
        clock = Clock.HIDING
        clockSeconds = 0
        progress = 1f
        hidersAtStart = 0
        seekers = 0
    }
}
