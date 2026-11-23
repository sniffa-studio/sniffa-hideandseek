package studio.sniffa.client.state

import studio.sniffa.ability.Ability
import studio.sniffa.net.payload.PricesPayload

object ServerPrices {

    private var prices: Map<String, PricesPayload.Price> = emptyMap()

    fun accept(prices: Map<String, PricesPayload.Price>) {
        this.prices = prices
    }

    fun forget() {
        prices = emptyMap()
    }

    fun cost(ability: Ability): Int = prices[ability.id]?.cost ?: ability.cost

    fun restockCost(ability: Ability): Int = prices[ability.id]?.restockCost ?: ability.restockCost

    fun cooldownSeconds(ability: Ability): Int = prices[ability.id]?.cooldownSeconds ?: ability.cooldownSeconds
}
