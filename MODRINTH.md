# Modrinth submission

Everything needed to create the project on modrinth.com and submit it for review.

## Project

| Field | Value |
|---|---|
| Type | Mod |
| Name | Sniffa Hide & Seek |
| URL slug | `sniffa-hide-and-seek` |
| Summary | Required client mod for Marguhl's Hide & Seek event. Useless anywhere else. |
| Icon | `src/main/resources/assets/hideandseek/icon.png` |
| Client side | Required |
| Server side | Unsupported |
| Primary category | Minigame |
| Additional categories | Game Mechanics |
| License | MIT |
| Homepage link | https://sniffa.studio |
| Source link | https://github.com/sniffa-studio/sniffa-hideandseek |
| Issues link | https://github.com/sniffa-studio/sniffa-hideandseek/issues |
| Content disclosure: AI-generated content | Parts of the code were written with Claude. Rule 6.1 requires this disclosure when that is a substantial portion of the code. |
| Gallery | 2-3 real in-game screenshots: ability wheel, radar or map, round HUD |

## Version 1.0.0

| Field | Value |
|---|---|
| File | `build/libs/hideandseek-1.0.0.jar` (build with `./gradlew build`) |
| Additional files | none |
| Version number | 1.0.0 |
| Version title | 1.0.0 |
| Release channel | Release |
| Loaders | Fabric |
| Game versions | 1.21.11 |
| Changelog | The 1.0.0 section of `CHANGELOG.md`, without its heading |

Dependencies, all type **Embedded** (they are bundled inside the jar):

- Fabric API 0.141.6+1.21.11
- Fabric Language Kotlin 1.13.13+kotlin.2.4.10
- GeckoLib 5.4.2
- owo-lib 0.13.0+1.21.11

## Description

Paste everything below the line into the description field.

---

**This mod is only for [Marguhl](https://twitch.tv/marguhl)'s Hide & Seek event, organised by Sniffa Studio.** It does nothing on any other server, and the event server will not let you in without it.

Everybody taking part installs it. It draws everything the event needs on your screen, while the event server decides everything that happens in the round.

## What it adds

- An ability wheel and shop for seekers and hiders, with points and cooldowns
- The seeker's tools: radar, thermal view, hot / cold frame, rangefinder, compass, glow, teleport map and danger zone
- The hiders' tools: sprint, jammer and early warning
- The arena border, the round HUD, titles and event sounds

## Before you install

- **It replaces your title screen.** Singleplayer and Multiplayer are hidden and a "Join Event" button connects you straight to `event.sniffa.studio`. Remove the mod after the event to get your normal menu back.
- **The title screen pings `event.sniffa.studio`** to show whether the event is online and how many players are on. To turn this off, set `ping_event_server=false` in `config/hideandseek.properties`.
- **Every ability is granted and driven by the event server.** The mod only draws what the server sends it and works nothing out on its own, so it gives no advantage anywhere else.
- **No other downloads needed.** Fabric API, Fabric Language Kotlin, GeckoLib and owo-lib are bundled. You only need the Fabric Loader for Minecraft 1.21.11.
- The in-game text is in German.

## Deutsch

**Diese Mod ist nur für das Hide & Seek Event von [Marguhl](https://twitch.tv/marguhl), organisiert von Sniffa Studio.** Auf anderen Servern macht sie nichts, und ohne sie lässt dich der Event-Server nicht rein.

Alle, die mitspielen, installieren sie. Sie zeichnet alles, was das Event auf deinem Bildschirm braucht. Was in der Runde passiert, entscheidet der Event-Server.

- **Sie ersetzt dein Hauptmenü.** Einzelspieler und Mehrspieler sind ausgeblendet, ein „Join Event“-Button verbindet dich direkt mit `event.sniffa.studio`. Entferne die Mod nach dem Event, dann ist dein normales Menü zurück.
- **Das Hauptmenü pingt `event.sniffa.studio`**, um anzuzeigen, ob das Event läuft. Abschalten mit `ping_event_server=false` in `config/hideandseek.properties`.
- **Jede Fähigkeit wird vom Event-Server vergeben und gesteuert.** Die Mod zeigt nur an, was der Server schickt, und bringt dir anderswo keinen Vorteil.
- **Keine weiteren Downloads nötig.** Fabric API, Fabric Language Kotlin, GeckoLib und owo-lib sind enthalten. Du brauchst nur den Fabric Loader für Minecraft 1.21.11.
