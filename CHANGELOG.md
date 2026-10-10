# Changelog

## 1.0.1

### Added

- The crew opens the teleporter to the hiders with R (Teleporter öffnen in the controls).
- A small panel in the top left shows your role and which key opens your wheel, or the teleporter for the crew.
- The crew and spectators see every hider glowing green and every seeker glowing red, with their names.
- The top left panel shows the phase, how much hiding time is left or how long the hunt runs, a progress bar,
  how many hiders and seekers are left, and your points.
- The ability wheel shows your points in its middle, also while you point at an ability.

### Changed

- The "Hide & Seek · connected" line in the top left is gone.
- The boss bar and the bottom right readout are gone, everything is in the top left panel.
- Radar marks keep glowing for eight seconds after the wave reaches them.
- The border is a glowing blue hex wall with smaller cells that twinkle and a light that sweeps up it.
  It turns pink while it shrinks and green while it grows, on the map as well.
- The border builds itself up cell by cell when a round starts or when it first comes into view.
- The border lights up the closer you get to it, and sends a wave through its cells where anyone touches it.
- While the border shrinks, its light runs calmly down the wall and the whole wall breathes slowly.
- The border shows only within about 96 blocks of you and fades out from 48 blocks, instead of across the whole map.
- A glowing line runs along the ground where the border meets the terrain.

### Fixed

- The map opens again after a spot the server refuses, without the three second wait, and the pick
  circle on it now matches what the server accepts for a teleport.
- The title screen no longer stalls while it looks up the event server.
- The danger zone screen edge shows only for hiders.
- The border wall is no longer rebuilt every frame while it moves.
- The border turns blue again once it has finished shrinking or growing.
- Scan marks fade with their wave, and cooldowns start fresh with every round.
- The map texture is released when you leave the server.

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
