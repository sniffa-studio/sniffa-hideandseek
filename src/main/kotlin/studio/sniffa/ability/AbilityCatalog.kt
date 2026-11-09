package studio.sniffa.ability

import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek

object AbilityCatalog {

    val RADAR = Ability(
        id = "radar",
        displayName = "Rentier-Nase",
        description = "Rudolph wittert, wie viele Verstecker in der Nähe sind — und markiert sie kurz.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 30,
        icon = Items.COMPASS,
        plannedModel = "Kleines Gerät mit Display und Radar-Anzeige",
        sprite = Hideandseek.id("ability/radar"),
    )

    val SPEED = Ability(
        id = "speed",
        displayName = "Zuckerstange",
        description = "Zuckerschock: kurzer Geschwindigkeitsschub.",
        side = Side.SEEKER,
        cost = 1,
        cooldownSeconds = 45,
        icon = Items.SUGAR,
        plannedModel = "Energy-Drink, Spritze oder Booster",
        sprite = Hideandseek.id("ability/tempo"),
    )




    val PLAYER_COMPASS = Ability(
        id = "player_compass",
        displayName = "Polarstern",
        description = "Der Stern zeigt dir die Richtung zum nächsten Verstecker.",
        side = Side.SEEKER,
        cost = 8,
        cooldownSeconds = 45,
        icon = Items.RECOVERY_COMPASS,
        plannedModel = "Eigener Tracker mit Pfeil auf dem Display",
        sprite = Hideandseek.id("ability/kompass"),
    )


    val FOOTPRINTS = Ability(
        id = "footprints",
        displayName = "Fußspuren im Schnee",
        description = "20 Sekunden lang leuchten die Fußspuren aller Verstecker im Schnee.",
        side = Side.SEEKER,
        cost = 6,
        cooldownSeconds = 60,
        icon = Items.LEATHER_BOOTS,
        plannedModel = "Leuchtende Abdrücke im Schnee",
        sprite = Hideandseek.id("ability/fussspuren"),
    )

    val SNOWMAN_SPY = Ability(
        id = "snowman_spy",
        displayName = "Schneemann-Spion",
        description = "Stellt einen Schneemann auf, der 60 Sekunden lang 10 Blöcke im Blick behält und Alarm schlägt.",
        side = Side.SEEKER,
        cost = 7,
        cooldownSeconds = 90,
        icon = Items.CARVED_PUMPKIN,
        plannedModel = "Schneemann mit Zylinder und Schal",
        sprite = Hideandseek.id("ability/schneemann_spion"),
    )

    val ELYTRA = Ability(
        id = "elytra",
        displayName = "Rentier-Schlitten",
        description = "Flügel für die ganze Runde und zwei Knallbonbons. Danach nur Knallbonbons nachkaufen.",
        side = Side.SEEKER,
        cost = 20,
        cooldownSeconds = 60,
        icon = Items.ELYTRA,
        plannedModel = "Eigene Wings oder kleiner Jetpack",
        sprite = Hideandseek.id("ability/elytra"),
        restockCost = 4,
    )


    val SWAP = Ability(
        id = "swap",
        displayName = "Wichteln",
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
        displayName = "Schneesturm",
        description = "Lässt einen Schneesturm auf der Karte los. Wer nach zwei Minuten noch drin steht, ist raus.",
        side = Side.SEEKER,
        cost = 30,
        cooldownSeconds = 300,
        icon = Items.REDSTONE_BLOCK,
        plannedModel = "Markierungsgerät oder Leuchtfeuer",
        sprite = Hideandseek.id("ability/dangerzone"),
    )

    val SPRINT = Ability(
        id = "sprint",
        displayName = "Schlittschuhe",
        description = "Auf Kufen: kurzer Geschwindigkeitsschub.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 60,
        icon = Items.FEATHER,
        plannedModel = "Turnschuh oder kleiner Booster",
        sprite = Hideandseek.id("ability/sprint"),
    )

    val JAMMER = Ability(
        id = "jammer",
        displayName = "Geschenkpapier",
        description = "Gut eingepackt: Die nächste Rentier-Nase zählt dich nicht mit.",
        side = Side.HIDER,
        cost = 4,
        cooldownSeconds = 75,
        icon = Items.REDSTONE_TORCH,
        plannedModel = "Kleiner Sender mit Antenne",
        sprite = Hideandseek.id("ability/stoersender"),
    )

    val EARLY_WARNING = Ability(
        id = "early_warning",
        displayName = "Weihnachtsglöckchen",
        description = "Bimmelt dir kurz, wie weit der Sucher weg ist.",
        side = Side.HIDER,
        cost = 6,
        cooldownSeconds = 60,
        icon = Items.BELL,
        plannedModel = "Wanduhr oder Alarmglocke",
        sprite = Hideandseek.id("ability/fruehwarnung"),
    )

    val SNOWMAN_DISGUISE = Ability(
        id = "snowman_disguise",
        displayName = "Schneemann-Verkleidung",
        description = "30 Sekunden lang bist du ein Schneemann. Wer stillsteht, fällt nicht auf.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 90,
        icon = Items.SNOW_BLOCK,
        plannedModel = "Schneemann-Kostüm",
        sprite = Hideandseek.id("ability/schneemann_verkleidung"),
    )

    val SEEKER_ABILITIES: List<Ability> = listOf(
        RADAR,
        SPEED,
        PLAYER_COMPASS,
        FOOTPRINTS,
        SNOWMAN_SPY,
        ELYTRA,
        SWAP,
        DANGER_ZONE,
    )

    val HIDER_ABILITIES: List<Ability> = listOf(
        SPRINT,
        JAMMER,
        EARLY_WARNING,
        SNOWMAN_DISGUISE,
    )

    fun forSide(side: Side?): List<Ability> = when (side) {
        Side.SEEKER -> SEEKER_ABILITIES
        Side.HIDER -> HIDER_ABILITIES
        null -> emptyList()
    }
}
