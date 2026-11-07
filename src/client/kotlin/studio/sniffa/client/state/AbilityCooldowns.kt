package studio.sniffa.client.state

object AbilityCooldowns {

    private var readyAt: Map<String, Long> = emptyMap()

    private val used = mutableSetOf<String>()

    private const val PAY_PER_USE = "@pay"

    var payPerUse: Boolean = false
        private set

    fun accept(secondsLeft: Map<String, Int>) {
        val now = System.currentTimeMillis()
        payPerUse = PAY_PER_USE in secondsLeft
        val abilities = secondsLeft - PAY_PER_USE
        readyAt = abilities.mapValues { now + it.value * 1000L }
        if (!payPerUse) used += abilities.keys
    }

    fun forget() {
        readyAt = emptyMap()
        used.clear()
        payPerUse = false
    }

    fun owned(abilityId: String): Boolean = abilityId in used

    fun remaining(abilityId: String): Int {
        val left = leftMillis(abilityId)
        return if (left <= 0L) 0 else ((left + 999) / 1000).toInt()
    }

    fun fraction(abilityId: String, totalSeconds: Int): Float {
        if (totalSeconds <= 0) return 0f
        val left = leftMillis(abilityId)
        if (left <= 0L) return 0f
        return (left.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f)
    }

    private fun leftMillis(abilityId: String): Long {
        val until = readyAt[abilityId] ?: return 0L
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun ready(abilityId: String): Boolean = remaining(abilityId) <= 0
}
