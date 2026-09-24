package studio.sniffa.ability

import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item

data class Ability(
    val id: String,
    val displayName: String,
    val description: String,
    val side: Side,
    val cost: Int,
    val cooldownSeconds: Int,
    val icon: Item,
    val plannedModel: String,
    val sprite: Identifier? = null,
)
