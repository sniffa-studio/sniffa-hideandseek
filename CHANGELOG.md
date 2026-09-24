# Changelog

## 1.0.0 - 2026-09-24

First release, for Minecraft 1.21.11 on Fabric.

### Added

- Handshake with the event server. Without the mod the server does not let you in.
- Ability wheel and shop for seekers and hiders, with points and cooldowns.
- Eleven seeker abilities: Radar, Tempo, Wärmebild, Heiß / Kalt, Entfernungsmesser, Kompass,
  Aufleuchten, Elytra, Teleport, Tausch and Gefahrenzone.
- Three hider abilities: Sprint, Störsender and Frühwarnung.
- Teleport map of the whole arena, drawn as a paper sheet with a compass rose. Clicks are ignored for
  the first three seconds, so a held wheel click cannot carry through.
- Danger zone: the seeker places a circle on the map, and hiders still inside when the countdown ends
  are out. The zone is drawn as a red wall, previewed at true size on the map, and shown to the hiders
  standing in it as a red screen edge with a countdown.
- Arena border drawn as a honeycomb wall that is blue at rest, red while closing and green while
  opening.
- Round HUD, titles and eleven event sounds.
- Title screen with a "Join Event" button and a live player count for the event server.
- `config/hideandseek.properties` with `ping_event_server`, to stop the title screen from
  contacting the event server.

### Bundled

- Fabric API 0.141.6+1.21.11
- Fabric Language Kotlin 1.13.13+kotlin.2.4.10
- GeckoLib 5.4.2
- owo-lib 0.13.0+1.21.11
