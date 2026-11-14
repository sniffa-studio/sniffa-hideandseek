package studio.sniffa.ability

import net.minecraft.world.item.Items
import studio.sniffa.Hideandseek

object AbilityCatalog {

    val RADAR = Ability(
        id = "radar",
        displayName = "Rentier-Nase",
        description = "Rudolph wittert, wie viele Verstecker in 50 Blöcken sind, und markiert sie kurz. Geschenkpapier schützt.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 45,
        icon = Items.COMPASS,
        plannedModel = "Kleines Gerät mit Display und Radar-Anzeige",
        sprite = Hideandseek.id("ability/radar"),
    )

    val FOOTPRINTS = Ability(
        id = "footprints",
        displayName = "Fußspuren im Schnee",
        description = "20 Sekunden lang leuchten die Spuren aller Verstecker, die sich bewegen. Filzpantoffeln schützen.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 60,
        icon = Items.LEATHER_BOOTS,
        plannedModel = "Leuchtende Abdrücke im Schnee",
        sprite = Hideandseek.id("ability/fussspuren"),
    )

    val ELYTRA = Ability(
        id = "elytra",
        displayName = "Rentier-Schlitten",
        description = "Flügel für die ganze Runde und zwei Knallbonbons. Danach nur Knallbonbons nachkaufen.",
        side = Side.SEEKER,
        cost = 15,
        cooldownSeconds = 60,
        icon = Items.ELYTRA,
        plannedModel = "Eigene Wings oder kleiner Jetpack",
        sprite = Hideandseek.id("ability/elytra"),
        restockCost = 4,
    )

    val SWAP = Ability(
        id = "swap",
        displayName = "Wichteln",
        description = "Nur für den Hauptsucher: Tauscht deine Position mit einem zufälligen Verstecker. Geschenkpapier schützt.",
        side = Side.SEEKER,
        cost = 15,
        cooldownSeconds = 180,
        icon = Items.ENDER_EYE,
        plannedModel = "Kugel oder Orb mit Glitch-Effekt",
        sprite = Hideandseek.id("ability/swap"),
    )

    val DANGER_ZONE = Ability(
        id = "danger_zone",
        displayName = "Schneesturm",
        description = "Nur für den Hauptsucher: Ein Schneesturm auf der Karte. Wer nach zwei Minuten noch drin steht, ist raus. Nicht mehr im Endspiel.",
        side = Side.SEEKER,
        cost = 20,
        cooldownSeconds = 600,
        icon = Items.REDSTONE_BLOCK,
        plannedModel = "Markierungsgerät oder Leuchtfeuer",
        sprite = Hideandseek.id("ability/dangerzone"),
    )

    val JAMMER = Ability(
        id = "jammer",
        displayName = "Geschenkpapier",
        description = "Gut eingepackt: 25 Sekunden lang finden dich Rentier-Nase, Schlittenhund, Wichteln und Grinch nicht.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 75,
        icon = Items.REDSTONE_TORCH,
        plannedModel = "Kleiner Sender mit Antenne",
        sprite = Hideandseek.id("ability/stoersender"),
    )

    val SNOWMAN_DISGUISE = Ability(
        id = "snowman_disguise",
        displayName = "Schneemann-Verkleidung",
        description = "60 Sekunden lang bist du ein Schneemann. Wer stillsteht, fällt nicht auf.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 120,
        icon = Items.SNOW_BLOCK,
        plannedModel = "Schneemann-Kostüm",
        sprite = Hideandseek.id("ability/schneemann_verkleidung"),
    )

    val SNOWBALL = Ability(
        id = "snowball",
        displayName = "Schneeball ins Gesicht",
        description = "Der nächste Sucher in 12 Blöcken, den du siehst, bekommt einen Schneeball ins Gesicht und sieht 2 Sekunden nichts.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 60,
        icon = Items.SNOWBALL,
        plannedModel = "Schneeball mit Flugbahn",
        sprite = Hideandseek.id("ability/schneeball"),
    )

    val SNOW_CLOUD = Ability(
        id = "snow_cloud",
        displayName = "Schneewolke",
        description = "Eine dichte Schneewolke um dich herum. 8 Sekunden lang sieht niemand hinein oder hindurch.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 60,
        icon = Items.POWDER_SNOW_BUCKET,
        plannedModel = "Pulverschnee-Wolke",
        sprite = Hideandseek.id("ability/schneewolke"),
    )


    val SLED_DOG = Ability(
        id = "sled_dog",
        displayName = "Schlittenhund",
        description = "Ein Husky nimmt die Spur des nächsten Versteckers auf, läuft hin und bellt ihn an. Geschenkpapier schützt.",
        side = Side.SEEKER,
        cost = 8,
        cooldownSeconds = 90,
        icon = Items.BONE,
        plannedModel = "Husky mit roter Schleife",
        sprite = Hideandseek.id("ability/schlittenhund"),
    )

    val MISTLETOE = Ability(
        id = "mistletoe",
        displayName = "Mistelzweig-Falle",
        description = "Eine Falle, die nur Sucher sehen, 4 Minuten lang. Läuft ein Verstecker drüber, bimmelt sie laut. Filzpantoffeln schützen.",
        side = Side.SEEKER,
        cost = 3,
        cooldownSeconds = 30,
        icon = Items.AZALEA_LEAVES,
        plannedModel = "Mistelzweig mit Beeren",
        sprite = Hideandseek.id("ability/mistelzweig"),
    )

    val DECOY = Ability(
        id = "decoy",
        displayName = "Lebkuchen-Doppelgänger",
        description = "Ein Doppelgänger mit deinem Skin bleibt 60 Sekunden stehen. Schlägt der Sucher drauf, platzt er.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 120,
        icon = Items.ARMOR_STAND,
        plannedModel = "Lebkuchenmann",
        sprite = Hideandseek.id("ability/lebkuchen"),
    )

    val BLACK_ICE = Ability(
        id = "black_ice",
        displayName = "Glatteis",
        description = "Der nächste Sucher in 16 Blöcken, den du siehst, rutscht 4 Sekunden lang und kommt kaum vom Fleck.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 90,
        icon = Items.BLUE_ICE,
        plannedModel = "Eisfläche",
        sprite = Hideandseek.id("ability/glatteis"),
    )

    val STAR_PEARL = Ability(
        id = "star_pearl",
        displayName = "Sternschnuppe",
        description = "Eine Enderperle zum Werfen. Du landest ohne Schaden dort, wo sie aufschlägt.",
        side = Side.HIDER,
        cost = 8,
        cooldownSeconds = 120,
        icon = Items.ENDER_PEARL,
        plannedModel = "Sternschnuppe mit Schweif",
        sprite = Hideandseek.id("ability/sternschnuppe"),
    )

    val SHRINK = Ability(
        id = "shrink",
        displayName = "Wichtelgröße",
        description = "12 Sekunden lang bist du winzig klein und passt durch jede Lücke.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 120,
        icon = Items.RED_MUSHROOM,
        plannedModel = "Wichtelmütze",
        sprite = Hideandseek.id("ability/wichtelgroesse"),
    )

    val ICE_BALL = Ability(
        id = "ice_ball",
        displayName = "Eisball",
        description = "Ein Schneeball zum Werfen. Wen er trifft, der friert 2 Sekunden ein und leuchtet 5 Sekunden lang.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 45,
        icon = Items.SNOWBALL,
        plannedModel = "Eisblauer Schneeball",
        sprite = Hideandseek.id("ability/eisball"),
    )

    val ANGEL = Ability(
        id = "angel",
        displayName = "Schutzengel",
        description = "Einmal pro Runde: Der nächste Treffer fängt dich nicht. Dein Engel schubst dich weg und du rennst 2 Sekunden schneller.",
        side = Side.HIDER,
        cost = 15,
        cooldownSeconds = 300,
        icon = Items.TOTEM_OF_UNDYING,
        plannedModel = "Engel mit Heiligenschein",
        sprite = Hideandseek.id("ability/schutzengel"),
    )

    val HO_HO_HO = Ability(
        id = "ho_ho_ho",
        displayName = "Ho Ho Ho!",
        description = "Dein Lachen schallt über den Schnee. Die bis zu 5 nächsten Verstecker in 25 Blöcken müssen kichern. Filzpantoffeln schützen.",
        side = Side.SEEKER,
        cost = 3,
        cooldownSeconds = 45,
        icon = Items.GOAT_HORN,
        plannedModel = "Weihnachtsmann-Lachen",
        sprite = Hideandseek.id("ability/hohoho"),
    )

    val GRINCH = Ability(
        id = "grinch",
        displayName = "Grinch",
        description = "Klaut den Verstecker in 16 Blöcken ihre Fähigkeiten. 20 Sekunden lang geht bei ihnen nichts. Geschenkpapier schützt.",
        side = Side.SEEKER,
        cost = 5,
        cooldownSeconds = 90,
        icon = Items.SLIME_BALL,
        plannedModel = "Grüne Grinch-Hand",
        sprite = Hideandseek.id("ability/grinch"),
    )

    val REINDEER_SPRINT = Ability(
        id = "reindeer_sprint",
        displayName = "Rentier-Sprint",
        description = "6 Sekunden lang rennst du wie ein Rentier, mit Schneestaub hinter dir.",
        side = Side.SEEKER,
        cost = 3,
        cooldownSeconds = 40,
        icon = Items.SUGAR,
        plannedModel = "Rentier-Hufe mit Schneestaub",
        sprite = Hideandseek.id("ability/rentiersprint"),
    )

    val SEEKER_CAM = Ability(
        id = "seeker_cam",
        displayName = "Sucher-Cam",
        description = "10 Sekunden lang siehst du durch die Augen des nächsten Suchers. Dein Ebenbild hält deinen Platz. Wird es erwischt, bist du es auch.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 90,
        icon = Items.SPYGLASS,
        plannedModel = "Kleine Kamera mit rotem Licht",
        sprite = Hideandseek.id("ability/suchercam"),
    )

    val FELT_SLIPPERS = Ability(
        id = "felt_slippers",
        displayName = "Filzpantoffeln",
        description = "30 Sekunden lang machst du kein Geräusch und hinterlässt keine Fußspuren. Ideal zum Umziehen.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 60,
        icon = Items.LEATHER_BOOTS,
        plannedModel = "Rote Filzpantoffeln",
        sprite = Hideandseek.id("ability/filzpantoffeln"),
    )

    val EGGNOG = Ability(
        id = "eggnog",
        displayName = "Eierpunsch",
        description = "Der nächste Sucher in 16 Blöcken, den du siehst, torkelt 3 Sekunden lang angetrunken herum.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 60,
        icon = Items.MILK_BUCKET,
        plannedModel = "Tasse Eierpunsch",
        sprite = Hideandseek.id("ability/eierpunsch"),
    )

    val SEEKER_ABILITIES: List<Ability> = listOf(
        RADAR,
        HO_HO_HO,
        FOOTPRINTS,
        SLED_DOG,
        MISTLETOE,
        ICE_BALL,
        GRINCH,
        REINDEER_SPRINT,
        SWAP,
        ELYTRA,
        DANGER_ZONE,
    )

    val HIDER_ABILITIES: List<Ability> = listOf(
        JAMMER,
        SNOWMAN_DISGUISE,
        FELT_SLIPPERS,
        SEEKER_CAM,
        SNOWBALL,
        EGGNOG,
        BLACK_ICE,
        SNOW_CLOUD,
        DECOY,
        SHRINK,
        STAR_PEARL,
        ANGEL,
    )

    fun forSide(side: Side?): List<Ability> = when (side) {
        Side.SEEKER -> SEEKER_ABILITIES
        Side.HIDER -> HIDER_ABILITIES
        null -> emptyList()
    }
}
