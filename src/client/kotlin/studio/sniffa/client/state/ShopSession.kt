package studio.sniffa.client.state

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import studio.sniffa.ability.Ability
import studio.sniffa.net.payload.PurchaseRequestPayload

object ShopSession {

    val points: Int get() = RoundState.points ?: 0

    val known: Boolean get() = RoundState.points != null

    val remainingHiders: Int get() = RoundState.remainingHiders

    fun canAfford(ability: Ability): Boolean = points >= ability.cost

    fun available(ability: Ability): Boolean =
        canAfford(ability) && AbilityCooldowns.ready(ability.id)

    fun cooldownLeft(ability: Ability): Int = AbilityCooldowns.remaining(ability.id)

    fun cooldownShare(ability: Ability): Float =
        AbilityCooldowns.fraction(ability.id, ability.cooldownSeconds)

    fun buy(ability: Ability): Boolean {
        if (!ClientPlayNetworking.canSend(PurchaseRequestPayload.TYPE)) {
            return false
        }
        ClientPlayNetworking.send(PurchaseRequestPayload(ability.id))
        return true
    }
}
