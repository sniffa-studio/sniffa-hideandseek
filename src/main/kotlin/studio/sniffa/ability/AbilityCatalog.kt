package studio.sniffa.ability

import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek

object AbilityCatalog {

    val RADAR = Ability(
        id = "radar",
        displayName = "Radar",
        description = "Zeigt, wie viele Verstecker in der Nähe sind — und markiert sie kurz.",
        side = Side.SEEKER,
        cost = 3,
        cooldownSeconds = 30,
        icon = Items.COMPASS,
        plannedModel = "Kleines Gerät mit Display und Radar-Anzeige",
        sprite = Hideandseek.id("ability/radar"),
    )

    val SPEED = Ability(
        id = "speed",
        displayName = "Tempo",
        description = "Kurzer Geschwindigkeitsschub.",
        side = Side.SEEKER,
        cost = 4,
        cooldownSeconds = 45,
        icon = Items.SUGAR,
        plannedModel = "Energy-Drink, Spritze oder Booster",
        sprite = Hideandseek.id("ability/tempo"),
    )

    val RANDOM_PING = Ability(
        id = "random_ping",
        displayName = "Wärmebild",
        description = "Zeigt ungefähr, wo ein zufälliger Verstecker ist.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 40,
        icon = Items.FIREWORK_ROCKET,
        plannedModel = "Kleine Signalpistole oder eigenes Ping-Gerät",
        sprite = Hideandseek.id("ability/signalpistole"),
    )

    val HOT_COLD = Ability(
        id = "hot_cold",
        displayName = "Heiß / Kalt",
        description = "Zeigt, ob du näher kommst oder dich entfernst.",
        side = Side.SEEKER,
        cost = 6,
        cooldownSeconds = 30,
        icon = Items.BLAZE_POWDER,
        plannedModel = "Thermometer oder Scanner",
        sprite = Hideandseek.id("ability/hotcold"),
    )

    val NEAREST_PLAYER = Ability(
        id = "nearest_player",
        displayName = "Entfernungsmesser",
        description = "Zeigt, wie weit der nächste Verstecker weg ist.",
        side = Side.SEEKER,
        cost = 7,
        cooldownSeconds = 30,
        icon = Items.TARGET,
        plannedModel = "Anzeige mit Entfernung",
        sprite = Hideandseek.id("ability/entfernungsmesser"),
    )

    val PLAYER_COMPASS = Ability(
        id = "player_compass",
        displayName = "Kompass",
        description = "Zeigt die Richtung zum nächsten Verstecker.",
        side = Side.SEEKER,
        cost = 8,
        cooldownSeconds = 45,
        icon = Items.RECOVERY_COMPASS,
        plannedModel = "Eigener Tracker mit Pfeil auf dem Display",
        sprite = Hideandseek.id("ability/kompass"),
    )

    val GLOW = Ability(
        id = "glow",
        displayName = "Aufleuchten",
        description = "Alle Verstecker leuchten kurz auf.",
        side = Side.SEEKER,
        cost = 9,
        cooldownSeconds = 60,
        icon = Items.GLOWSTONE_DUST,
        plannedModel = "Leuchtender Scanner oder UV-Gerät",
        sprite = Hideandseek.id("ability/uv_geraet"),
    )

    val ELYTRA = Ability(
        id = "elytra",
        displayName = "Elytra",
        description = "Elytra und zwei Raketen, bis du landest.",
        side = Side.SEEKER,
        cost = 10,
        cooldownSeconds = 60,
        icon = Items.ELYTRA,
        plannedModel = "Eigene Wings oder kleiner Jetpack",
        sprite = Hideandseek.id("ability/elytra"),
    )

    val TELEPORT = Ability(
        id = "teleport",
        displayName = "Teleport",
        description = "Karte öffnen und hinspringen.",
        side = Side.SEEKER,
        cost = 10,
        cooldownSeconds = 60,
        icon = Items.ENDER_PEARL,
        plannedModel = "Kleines Teleport-Gerät",
        sprite = Hideandseek.id("ability/teleport"),
    )

    val SWAP = Ability(
        id = "swap",
        displayName = "Tausch",
        description = "Tauscht deine Position mit einem zufälligen Verstecker.",
        side = Side.SEEKER,
        cost = 12,
        cooldownSeconds = 90,
        icon = Items.ENDER_EYE,
        plannedModel = "Kugel oder Orb mit Glitch-Effekt",
        sprite = Hideandseek.id("ability/swap"),
    )

    val DANGER_ZONE = Ability(
        id = "danger_zone",
        displayName = "Gefahrenzone",
        description = "Setzt einen Kreis auf der Karte. Wer nach zwei Minuten noch drin steht, ist raus.",
        side = Side.SEEKER,
        cost = 14,
        cooldownSeconds = 120,
        icon = Items.REDSTONE_BLOCK,
        plannedModel = "Markierungsgerät oder Leuchtfeuer",
        sprite = Hideandseek.id("ability/dangerzone"),
    )

    val SPRINT = Ability(
        id = "sprint",
        displayName = "Sprint",
        description = "Kurzer Geschwindigkeitsschub.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 60,
        icon = Items.FEATHER,
        plannedModel = "Turnschuh oder kleiner Booster",
        sprite = Hideandseek.id("ability/sprint"),
    )

    val JAMMER = Ability(
        id = "jammer",
        displayName = "Störsender",
        description = "Das nächste Radar zählt dich nicht mit.",
        side = Side.HIDER,
        cost = 4,
        cooldownSeconds = 75,
        icon = Items.REDSTONE_TORCH,
        plannedModel = "Kleiner Sender mit Antenne",
        sprite = Hideandseek.id("ability/stoersender"),
    )

    val EARLY_WARNING = Ability(
        id = "early_warning",
        displayName = "Frühwarnung",
        description = "Zeigt kurz, wie weit der Sucher weg ist.",
        side = Side.HIDER,
        cost = 6,
        cooldownSeconds = 60,
        icon = Items.BELL,
        plannedModel = "Wanduhr oder Alarmglocke",
        sprite = Hideandseek.id("ability/fruehwarnung"),
    )

    val SEEKER_ABILITIES: List<Ability> = listOf(
        RADAR,
        SPEED,
        RANDOM_PING,
        HOT_COLD,
        NEAREST_PLAYER,
        PLAYER_COMPASS,
        GLOW,
        ELYTRA,
        TELEPORT,
        SWAP,
        DANGER_ZONE,
    )

    val HIDER_ABILITIES: List<Ability> = listOf(
        SPRINT,
        JAMMER,
        EARLY_WARNING,
    )

    fun forSide(side: Side?): List<Ability> = when (side) {
        Side.SEEKER -> SEEKER_ABILITIES
        Side.HIDER -> HIDER_ABILITIES
        null -> emptyList()
    }
}
