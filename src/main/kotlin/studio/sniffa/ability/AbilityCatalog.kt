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
        cooldownSeconds = 45,
        icon = Items.COMPASS,
        plannedModel = "Kleines Gerät mit Display und Radar-Anzeige",
        sprite = Hideandseek.id("ability/radar"),
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
        cost = 6,
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

    val JAMMER = Ability(
        id = "jammer",
        displayName = "Geschenkpapier",
        description = "Gut eingepackt: 20 Sekunden lang findet dich keine Ortung des Suchers.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 75,
        icon = Items.REDSTONE_TORCH,
        plannedModel = "Kleiner Sender mit Antenne",
        sprite = Hideandseek.id("ability/stoersender"),
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
        cost = 4,
        cooldownSeconds = 75,
        icon = Items.POWDER_SNOW_BUCKET,
        plannedModel = "Pulverschnee-Wolke",
        sprite = Hideandseek.id("ability/schneewolke"),
    )


    val SLED_DOG = Ability(
        id = "sled_dog",
        displayName = "Schlittenhund",
        description = "Ein Husky nimmt die Spur des nächsten Versteckers auf, läuft hin und bellt ihn an.",
        side = Side.SEEKER,
        cost = 10,
        cooldownSeconds = 90,
        icon = Items.BONE,
        plannedModel = "Husky mit roter Schleife",
        sprite = Hideandseek.id("ability/schlittenhund"),
    )

    val MISTLETOE = Ability(
        id = "mistletoe",
        displayName = "Mistelzweig-Falle",
        description = "Eine Falle, die nur Sucher sehen. Tritt ein Verstecker drauf, bimmelt sie laut.",
        side = Side.SEEKER,
        cost = 4,
        cooldownSeconds = 30,
        icon = Items.AZALEA_LEAVES,
        plannedModel = "Mistelzweig mit Beeren",
        sprite = Hideandseek.id("ability/mistelzweig"),
    )

    val FAKE_STEPS = Ability(
        id = "fake_steps",
        displayName = "Wichtelschritte",
        description = "Der Sucher hört 4 Sekunden lang Schritte direkt hinter sich.",
        side = Side.HIDER,
        cost = 3,
        cooldownSeconds = 45,
        icon = Items.LEATHER_BOOTS,
        plannedModel = "Kleine Wichtelstiefel",
        sprite = Hideandseek.id("ability/wichtelschritte"),
    )

    val DECOY = Ability(
        id = "decoy",
        displayName = "Lebkuchen-Doppelgänger",
        description = "Ein Doppelgänger mit deinem Skin bleibt 45 Sekunden stehen. Schlägt der Sucher drauf, platzt er.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 90,
        icon = Items.ARMOR_STAND,
        plannedModel = "Lebkuchenmann",
        sprite = Hideandseek.id("ability/lebkuchen"),
    )

    val BLACK_ICE = Ability(
        id = "black_ice",
        displayName = "Glatteis",
        description = "Der nächste Sucher in 16 Blöcken, den du siehst, rutscht 4 Sekunden lang und kommt kaum vom Fleck.",
        side = Side.HIDER,
        cost = 6,
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
        cost = 7,
        cooldownSeconds = 120,
        icon = Items.ENDER_PEARL,
        plannedModel = "Sternschnuppe mit Schweif",
        sprite = Hideandseek.id("ability/sternschnuppe"),
    )

    val SHRINK = Ability(
        id = "shrink",
        displayName = "Wichtelgröße",
        description = "8 Sekunden lang bist du winzig klein und passt durch jede Lücke.",
        side = Side.HIDER,
        cost = 8,
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

    val NAUGHTY_LIST = Ability(
        id = "naughty_list",
        displayName = "Die Liste",
        description = "Wer war unartig? Der Verstecker, der sich am längsten nicht bewegt hat, leuchtet 5 Sekunden lang auf.",
        side = Side.SEEKER,
        cost = 8,
        cooldownSeconds = 90,
        icon = Items.WRITABLE_BOOK,
        plannedModel = "Lange Pergamentrolle",
        sprite = Hideandseek.id("ability/liste"),
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
        description = "Dein Lachen schallt über den Schnee. Wer in 25 Blöcken steckt, muss kichern, und du hörst, wo.",
        side = Side.SEEKER,
        cost = 4,
        cooldownSeconds = 45,
        icon = Items.GOAT_HORN,
        plannedModel = "Weihnachtsmann-Lachen",
        sprite = Hideandseek.id("ability/hohoho"),
    )

    val GRINCH = Ability(
        id = "grinch",
        displayName = "Grinch",
        description = "Klaut den Verstecker in 20 Blöcken ihre Fähigkeiten. 30 Sekunden lang geht bei ihnen nichts.",
        side = Side.SEEKER,
        cost = 7,
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
        cost = 4,
        cooldownSeconds = 40,
        icon = Items.SUGAR,
        plannedModel = "Rentier-Hufe mit Schneestaub",
        sprite = Hideandseek.id("ability/rentiersprint"),
    )

    val SEEKER_CAM = Ability(
        id = "seeker_cam",
        displayName = "Sucher-Cam",
        description = "8 Sekunden lang siehst du durch die Augen des nächsten Suchers. Dein Ebenbild hält so lange deinen Platz.",
        side = Side.HIDER,
        cost = 5,
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
        cost = 4,
        cooldownSeconds = 90,
        icon = Items.LEATHER_BOOTS,
        plannedModel = "Rote Filzpantoffeln",
        sprite = Hideandseek.id("ability/filzpantoffeln"),
    )

    val EGGNOG = Ability(
        id = "eggnog",
        displayName = "Eierpunsch",
        description = "Der nächste Sucher in 16 Blöcken, den du siehst, torkelt 5 Sekunden lang angetrunken herum.",
        side = Side.HIDER,
        cost = 5,
        cooldownSeconds = 75,
        icon = Items.MILK_BUCKET,
        plannedModel = "Tasse Eierpunsch",
        sprite = Hideandseek.id("ability/eierpunsch"),
    )

    val SEEKER_ABILITIES: List<Ability> = listOf(
        RADAR,
        HO_HO_HO,
        FOOTPRINTS,
        NAUGHTY_LIST,
        SLED_DOG,
        SNOWMAN_SPY,
        MISTLETOE,
        ICE_BALL,
        GRINCH,
        REINDEER_SPRINT,
        SWAP,
        ELYTRA,
        DANGER_ZONE,
    )

    val HIDER_ABILITIES: List<Ability> = listOf(
        SNOWBALL,
        FAKE_STEPS,
        FELT_SLIPPERS,
        SNOW_CLOUD,
        JAMMER,
        SNOWMAN_DISGUISE,
        DECOY,
        SEEKER_CAM,
        EGGNOG,
        BLACK_ICE,
        STAR_PEARL,
        SHRINK,
        ANGEL,
    )

    fun forSide(side: Side?): List<Ability> = when (side) {
        Side.SEEKER -> SEEKER_ABILITIES
        Side.HIDER -> HIDER_ABILITIES
        null -> emptyList()
    }
}
