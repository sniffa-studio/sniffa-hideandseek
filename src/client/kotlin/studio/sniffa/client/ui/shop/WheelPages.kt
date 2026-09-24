package studio.sniffa.client.ui.shop

import studio.sniffa.ability.Ability
import studio.sniffa.client.ui.theme.Spacing

object WheelPages {

    fun split(abilities: List<Ability>): List<List<Ability>> =
        if (abilities.isEmpty()) emptyList() else abilities.chunked(Spacing.SLOTS_PER_PAGE)
}
