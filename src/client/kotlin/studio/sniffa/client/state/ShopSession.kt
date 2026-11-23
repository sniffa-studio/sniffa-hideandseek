package studio.sniffa.client.state

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import studio.sniffa.ability.Ability
import studio.sniffa.net.payload.PurchaseRequestPayload

object ShopSession {

    val points: Int get() = RoundState.points ?: 0

    val known: Boolean get() = RoundState.points != null

    val remainingHiders: Int get() = RoundState.remainingHiders

    fun restocking(ability: Ability): Boolean =
        ServerPrices.restockCost(ability) > 0 && AbilityCooldowns.owned(ability.id)

    fun price(ability: Ability): Int = when {
        restocking(ability) -> ServerPrices.restockCost(ability)
        AbilityCooldowns.payPerUse -> ServerPrices.cost(ability)
        AbilityCooldowns.owned(ability.id) -> 0
        else -> ServerPrices.cost(ability)
    }

    fun canAfford(ability: Ability): Boolean = points >= price(ability)

    fun available(ability: Ability): Boolean =
        RoundState.clock != RoundState.Clock.PAUSED && canAfford(ability) && AbilityCooldowns.ready(ability.id)

    fun cooldownLeft(ability: Ability): Int = AbilityCooldowns.remaining(ability.id)

    fun cooldownShare(ability: Ability): Float =
        AbilityCooldowns.fraction(ability.id, ServerPrices.cooldownSeconds(ability))

    fun buy(ability: Ability): Boolean {
        if (!ClientPlayNetworking.canSend(PurchaseRequestPayload.TYPE)) {
            return false
        }
        ClientPlayNetworking.send(PurchaseRequestPayload(ability.id))
        return true
    }
}
