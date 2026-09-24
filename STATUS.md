# Hide & Seek — where the build stands

Written 2026-08-17, rewritten 2026-08-19 at the end of a long day on it. It covers both halves: this
mod and `../sniffa-hideandseek-server`. Read it top to bottom before picking the work back up; it is
written to be the only thing that needs reading.

**What changed on 2026-08-19**, since it was most of the game: the hiders got a shop and an economy;
the wheel learned about cooldowns; placements, a podium and a proper ending; the Höhen-Scanner was cut
and the Kompass built; Radar now outlines what it counts and its prop glows; the world was locked down
to running and looking; every message was rewritten with one voice, one palette and one name per
ability; the round gained titles and eleven synthesised sounds; and the round now pauses rather than
hanging when the seeker disconnects. Four proposed mechanics were turned down — see the bottom.

**What changed up to 2026-09-24:** the seeker gained an eleventh ability, the Gefahrenzone; the
teleport map was redrawn as a paper sheet that opens on the whole arena and ignores clicks for three
seconds; all fourteen abilities wear their own sprite; the mod bundles everything it needs, is marked
client-only and is open source under MIT; the title screen ping can be switched off; and the
mod is being prepared for Modrinth (see `MODRINTH.md`).

## The shape of the thing

Two programs that only ever meet over custom payloads.

- **`sniffa-hideandseek`** — Fabric client mod, MC 1.21.11, Kotlin, owo-lib, GeckoLib. Everything the
  seeker *sees*: shaders, HUD, the ability wheel, the teleport map, the props, the title screen.
- **`sniffa-hideandseek-server`** — Paper plugin, Java 21, running on the event server.
  Everything that is *true*: points, purchases, cooldowns, round phases, the border,
  who is caught, every teleport, and where a prop is put down.

The split is deliberate and worth defending: **the client renders, the server decides.** No ability
takes effect because a client asked nicely — a purchase is a request, the server charges and
answers. The map click is the clearest case: the client sends a wish, the server validates the
grant, the border and the landing spot, and only then teleports.

`Protocol.VERSION` is **1** and both sides check it at handshake. Adding a channel is not a change of
shape — a side that does not know one never sends on it and ignores what arrives on it — so `role`,
`spotted`, `bearing` and `cooldown` all cost nothing. Only changing an *existing* payload earns a
bump. It went to 2 for an afternoon when Glow tried carrying positions instead of entity ids, and back
to 1 when that was reverted; **2 is burned, the next bump is 3.** Channels: `handshake`, `border`,
`round`, `seeker`, `role`, `radar`, `spotted`, `scan`, `heat`, `hotcold`, `range`, `bearing`, `glow`,
`tag`, `cooldown`, `buy`, `map_meta`, `map_data`, `map_need`, `tp`, `prop`, `zone_arm`, `zone`,
`zone_pick`.

`AbilityCatalog` exists **twice** — `src/main/kotlin/studio/sniffa/ability/AbilityCatalog.kt` here
and `.../ability/AbilityCatalog.java` there. Paper and Fabric cannot share code, so ids, names,
descriptions and prices are mirrored by hand. They are in sync as of now. A mismatch shows up as a
wheel where clicking does nothing — it already happened once, when the two lists had entirely
different ids.

## The fourteen abilities

Eleven for the seeker, three for the hiders. All built. The Höhen-Scanner was cut rather than designed —
height on its own is a line of text, and everything else in the catalogue is something you look at.

### The seeker's eleven

| Price | Ability | State | What it does |
|---|---|---|---|
| 3 | Radar | ✅ | Puts a device down, sweeps from it, counts what it finds **and outlines them** — see below. `RANGE = 50`. |
| 4 | Tempo | ✅ | 8 s Speed II. Cooldown 45 s, far longer than the effect, so it saves one chase rather than becoming travel. |
| 5 | Wärmebild | ✅ | Thermal heatmap of a hider, through walls, with a deliberate random offset. `RADIUS 26`, `OFFSET 10`, `REACHABLE 90`. |
| 6 | Heiß / Kalt | ✅ | Screen-edge frame: ice when moving away, embers when closing. 15 s, `COLD_AT 60`. |
| 7 | Entfernungsmesser | ✅ | Custom SDF rangefinder bar where the bossbar sits. 10 s, 4 Hz, `MAX_BLOCKS 200`. |
| 8 | Kompass | ✅ | A dial on the ground around the seeker with a needle on the nearest hider. 14 s, 4 Hz. A direction and never a distance — see below. |
| 9 | Aufleuchten | ✅ | Every living hider outlined for 4 s. Reaches only as far as the client has been sent players — see below. |
| 10 | Elytra | ✅ | Elytra + 2 rockets, removed on landing. |
| 10 | Teleport | ✅ | Opens the world map, click to choose — see below. Random spot as fallback. |
| 12 | Tausch | ✅ | Position swap with a random hider. Refused while gliding. |
| 14 | Gefahrenzone | ✅ | The seeker places a circle on the map; every hider still inside when the two minutes run out is out. Cooldown 120 s. The client draws it as a plain red wall (`zonewall.fsh`), previews it at true size on the map and gives the hiders standing in it a red screen edge with a countdown. Channels `zone_arm`, `zone`, `zone_pick`. |

### The hiders' three

Until 2026-08-19 `Role.HIDER` appeared exactly once in the whole plugin, to put them in Adventure
mode. Ninety-nine people had a key that opened nothing while one person had ten tools; the shop was
a toolbox for the streamer rather than a game between two sides.

| Price | Ability | State | What it does |
|---|---|---|---|
| 3 | Sprint | ✅ | The seeker's Speed effect, reused as it stands. Cooldown 60 s rather than 45, so it saves one escape instead of outrunning the round. |
| 4 | Störsender | ✅ | 20 s during which a radar sweep does not count you. The first ability that is about another ability. |
| 6 | Frühwarnung | ✅ | 6 s of the seeker's distance, on the seeker's own rangefinder bar and payload. A distance and never a direction. |

**They earn by surviving**: nothing to start with, one point every two minutes of the hunt
(`SECONDS_PER_HIDER_POINT`). The seeker is paid twice as often, and the two clocks are separate on
purpose — the seeker's trickle stops a dry spell being a standstill, while a hider is being paid for
something they would go on doing for nothing.

At that rate the cheapest thing on their wheel is six minutes of hunt away, the jammer eight, the
warning twelve, and every purchase starts the wait again. **Most of a hundred hiders will be caught
before any of it**, so their half of the shop is a reward for surviving rather than a tool everybody
gets to hold. That is known and chosen, not an oversight — if it should reach further, the levers are
a starting balance and the interval, both single numbers in `PointRules`.

**The jammer never makes the radar lie.** A jammed hider is left out of the count rather than the
count being falsified — the number the seeker reads is always exactly how many people the sweep
detected. A radar that reported invented figures would make every reading worthless, the honest ones
included.

**Being counted is now told to the person counted.** `hideandseek:spotted` goes to each hider a sweep
found, carrying the reach and nothing else, and lands at the same moment the seeker's own number does
— earlier would be a quarter-second of prophecy about their own position. Jammed hiders get nothing,
because they were not counted.

**Which shop opens is sent, not inferred.** `hideandseek:role` carries the role; the client used to
deduce "I am the seeker" from having been sent points, which held exactly as long as the hiders had
none. Sent at the round's start, at a catch, at a join and at the handshake.

**The rangefinder and the compass are deliberate opposites.** Entfernungsmesser (7) says how far without
saying where; Kompass (8) says where without saying how far. Neither may leak the other's
half — which is why `CompassRose` draws the same dial at ten blocks as at three hundred, and why
`PlayerCompassEffect` sends nothing that could be turned into a range. A seeker who has bought both
has a fix, and that is what makes two abilities worth more than one at fifteen points.

The one thing that does change with distance is how fast the needle swings while the seeker walks.
That is parallax rather than a leak: it costs movement and attention, and whoever works it out has
earned it.

**Radar marks people out now, and that was a decision.** It answered only with a number until
2026-08-19, and the restraint was the design — a count sends the seeker somewhere without telling them
what to look at, which is a chase. It outlines the counted hiders as the wave reaches each of them.
Three points therefore buy most of what nine used to.

**Balance is unreviewed and there are three known overlaps**: Radar (3) now outlines everyone within
fifty blocks through walls, which is what Aufleuchten (9) sells at longer reach; Wärmebild (5) reveals
through walls, which is most of what Aufleuchten sells too; Heiß/Kalt (6) gives proximity, which is what
Entfernungsmesser (7) gives more precisely. None has been played enough to know which should move.

**Aufleuchten reaches as far as the seeker's client can see, and no further.** An outline is drawn on the
player's own model, so the client has to have one — Paper sends players within ~128 blocks and hands
out six chunks of world, on a map a thousand blocks across. It is an endgame tool by construction,
not a way of finding the last hider on a big map. Raising `view-distance` is the only lever, and it
costs bandwidth for everyone.

**A third balance question, found by reading rather than playing:** Radar's wave is sent to everyone
within `VISIBLE_MULTIPLE = 3` times its range — 150 blocks — while it only counts within 50. Somebody
a hundred blocks away was never in danger and still learns exactly where the seeker is. The trade is
named in the code and defended there; the multiplier is the number to argue about.

## The round, end to end

The host calls every beat; nothing is on a schedule. `Round` owns the phases and the one-second
clock, and the boss bar is only touched when its text would actually differ.

**Starting.** `/hsround start <Spieler>` — and it is **refused without a border**. Starting without
one is not broken, which is exactly the danger: everything runs, nobody is warned, and the hiders walk
off the map while Radar's fifty blocks, the compass and Aufleuchten quietly stop being worth anything.
So the order is always `/hsborder here <radius>` and then the round.

At the start: roles are handed out and sent, the seeker is armed and blinded, hiders go to Adventure
with an account opened at nought, the standings are opened, and everybody gets the title, the chat
line and the cue.

**Hiding**, `hiding-seconds` in the config (720). The seeker is blind and slow rather than boxed in.
The last ten seconds tick and the last three climb. `/hsskip` — or `/hsround skip` — cuts it short and
tells the host how much was skipped. With nobody to hide, it skips itself.

**The hunt** has no deadline. It ends when the last hider is found, or when the host says so. Points
trickle: the seeker one a minute, hiders one every two.

**A catch** is a hit, and only the seeker's counts. The damage is always cancelled — nobody is hurt,
they are found — and the catch is a direct kill so nothing between a damage number and a death gets a
say. Announced before the killing blow, because killing them fires the death that may end the round.

**Going out.** Every death takes a player out of the round, however it happened: sword, fall, void.
They are kicked and kept out until the next round starts, with their place on the kick screen —
`Elimination` is memory-only and lasts one round, not a ban. **The last hider is the exception**: they
are respawned as a spectator and stay to watch the ending.

**A place is worked out the moment somebody goes out and never changes.** Eighty-seven still hidden
means eighty-eighth. That is what lets the number travel with the kick, on a screen they will actually
read, and it means nothing has to be recomputed at the end.

**The seeker leaving pauses everything** for ninety seconds — clock, points and shop — and says so on
the bar. They come back and it resumes; they do not and the round ends properly rather than hanging.
Roles are held by UUID, so a reconnect needs no repair. See `SeekerWatch` and `Round.seekerLeft`.

**Ending.** All found, or `/hsround stop`. Either way: a title, the result, and the podium — the last
three found by place, or the names of whoever was never found, who share the top and are not ranked
against each other because nothing in the round measured one hidden player against another.

## What the round says, and how

**One voice, and it wears the prefix.** Vanilla's join, quit and death messages are all suppressed;
everything a player reads comes through `Messages` with `<prefix>` in it. Two announcers saying the
same thing differently is how a chat stops being read.

**The palette is not decoration.** `Palette` in the plugin is the whole of it, and three entries were
wrong until 2026-08-19 and are worth not re-breaking:

- **The mark is a cold cyan gradient** (`#35D6F0` → `#A8F0FF`) across "HIDE & SEEK", and the studio
  purple is gone from everything over the world. Purple is right on a white website and wrong over
  Minecraft: it sits in the night sky and in every shadow, fights the seeker's red instead of standing
  apart from it, and goes murky at stream bitrate. The title screen splash moved with it.
- **Quiet is `#8B9C92`, not a dark grey.** It was `#515151`, which over grass or sky is not quiet text
  but text somebody has to lean in to read. Forty-seven call sites hang off that one constant.
- **Success is moss `#4EC471`, not neon.** `#03F720` is a colour nothing in the world is, and it was
  going on every ability that fired.

**Titles are for the four beats and nothing else** — the round opening, the hunt beginning, the
ending, and the last hider's own result. A title is the one thing Minecraft has that cannot be missed,
which is exactly why it has to stay rare; one for a purchase would train people to look away from the
middle of the screen, which is where they are trying to find somebody. The chat line still goes out
for each, because a title cannot be scrolled back to.

**The boss bar carries two facts.** `Verstecken 07:42` while people hide, `Jagd 04:12 · Verstecker
88/100` during the hunt, and `Pausiert · Sucher weg · 1:23` in yellow when the seeker has gone. It
grew a FINALE word, a sentence about how many were still finding a spot, and twenty notches, and all
three came back out: a bar at the top of the screen has to be scanned, not studied.

**One name per ability.** The wheel and the chat used to disagree — the catalogue said "Nearest
Player" while the ability announced itself as "Entfernungsmesser" — and the same people were "Spieler"
in the descriptions and "Verstecker" everywhere else. All German now, one word each, and both
catalogues checked entry for entry.

**Refusals carry the number.** "Kompass kostet 8 Punkte, du hast 5" and "Radar ist noch nicht bereit —
noch 12s", rather than "zu teuer" and a balance on the other side of the screen.

## The sounds are ours

Eleven cues, **synthesised rather than recorded**: `tools/SoundForge.java` writes them out of sine
waves and envelopes, `tools/sounds.sh` encodes them with ffmpeg, and all eleven together are 160 KB —
less than one of the mod's textures. A cue that is four lines of arithmetic can be tuned in a minute
and reviewed in a diff, where a sample can only be replaced.

**A plugin cannot ship audio and a mod can**, which is the whole shape of it: the mod registers them
under `hideandseek:cue.*` (`Cues.kt`), the server plays them by name (`Cues.java`), and the decision
about *when* a sound happens stays on the side that knows. Vanilla stays layered underneath wherever
the game already has something better — there is no vanilla sound for "a sweep counted you", but there
is one for a critical hit, and a player reads that faster than anything written here.

**Three ranges, and choosing between them is most of the design.** To one player at their own position
for anything private; into the world for anything that is an event somewhere, so a radar unfolding is
heard two streets away; to everybody only for the beats.

The one worth reading twice is the hunt: a riser cut off dead, a tenth of a second of silence, then the
impact. A riser that fades out is tension leaking away.

## The world is locked down

`Protection` is a list of noes rather than a mode. Adventure stops blocks being broken and placed and
stops nothing else: a chest still opens, an apple is still eaten, and hunger runs down until somebody
who has hidden well for fifteen minutes starts taking damage for it.

Nothing may be broken, placed, opened, eaten, dropped, picked up, swapped, slept in or reordered, and
almost nothing may hurt anybody. **Two exceptions**, both deliberate:

- **A hider can take fall damage.** Height is a real choice, and a hider who can drop off anything
  unhurt has a free escape from every chase. The seeker cannot fall at all — not symmetry, but the
  show: a chase that ends with the seeker dying on a two-block drop is a chase nobody gets back.
- **The void kills everybody.** Nothing else can end it. A player under the world with damage off falls
  for ever, cannot be caught, and the round cannot finish.

Firework rockets keep their right-click, because the Elytra ability is made of them. Player against
player is left entirely to `CatchService`, which already cancels it whoever swung — two copies of that
rule is how the two come to disagree.

## The radar, in full

The one ability that has had a proper pass. It is worth reading as the pattern the other ten should
follow.

**A device, not a light effect.** `Props.placeInFrontOf` finds a spot two blocks ahead — searched
with `Landing.standable`, walking downwards from the player's own feet rather than using the world
height map, because on a map made of buildings the height map answers "the roof above you". Half a
second later, once the model has finished unfolding, the sweep is cast **from the dish** and the
count goes to the seeker. Nowhere to stand it is not a failure: the sweep still happens, from the
seeker, exactly as it did before there was a model.

**The order is the point.** Deploy (0.5 s) → sweep (about 4 s) → the machine stands there → retract
(the last 0.5 s of its six). `DEPLOY_TICKS` in `RadarEffect` and the animation length in
`radar.animation.json` are the same number by intent, not by dependency, and both say so.

**The readout names the reach and the thing counted**: "3 Verstecker in 50 Blöcken". Both halves were
missing at different times and each made the line useless on its own — *near* means nothing without a
figure, and a figure means nothing without a noun. `RadarPayload` had been carrying the range all
along with a comment saying what it was for, and nothing unpacked it.

**Its display and status lamp glow**, drawn over the model at full brightness whatever the light is
where it stands. A machine mid-sweep with a dark screen reads as switched off.

## The props

`PropKind` (shared) + `Prop`, `PropModel`, `Props` (client) + `prop/Props.java` (server). GeckoLib
5.4.2, bundled in the jar so participants download nothing extra.

**Adding the twelfth model is one entry in `PropKind` and no code.** The server sends a name, a
place, a facing and a duration on one channel; the client draws it until its time is up. There is no
entity, no id and no removal packet — a disconnect at the wrong moment cannot leave a radar dish
standing in a field for the rest of the evening.

Four things that cost an hour each and will not be obvious again:

1. **GeckoLib scans its own folders.** Assets go in `assets/hideandseek/geckolib/models/` and
   `.../geckolib/animations/`, and the id handed to it is the **bare name** — it puts the folder and
   the extension back itself. A full path fails at the moment of drawing.
2. **`GeoObjectRenderer` is written for block entities** and nudges everything half a block sideways
   and half a block up. `Props.PropRenderer` overrides `adjustRenderPose` to do nothing.
3. **Glowing parts are a second texture, found by name.** `<id>_glowmask.png` beside the texture,
   drawn over the model at full brightness by `AutoGlowingGeoLayer`. Nothing points at it — the name
   is the wiring — which is why `PropKind.glows` gates the layer: a prop that says yes without a mask
   would render the missing-texture chequer as a light. The radar's mask comes out of the same
   generator pass as its texture, so the two cannot drift apart.
4. **Arriving and leaving belong in the animation, not in the renderer.** Scaling a model about its
   own feet does not fold it away, it shrinks it into the floor.

The radar's model, texture and glowmask all come out of **one script** — `tools/radar-prop.ps1`, in
the repo, and rerunning it reproduces the committed files byte for byte. It defines the cubes once,
packs the UV layout itself and paints the texture to match, so a face can never wear another face's
paint, and the glowmask is painted in the same pass so it cannot slip out of register. The texture is
painted in ramp indices — eight rungs per material, hue-shifted, whole numbers everywhere so only
gradients dither. It is a stand-in until the designer delivers, and it opens in Blockbench as a
finished project with named bones.

## The shaders

All in `src/main/resources/assets/hideandseek/shaders/core/`.

- **`scan`** — the sonar. Reconstructs world position from the depth buffer, so the wave lights up
  real geometry. Asymmetric wave front, edge detection via `fwidth`, three echoes. `TRAVEL_MILLIS
  2600`. Now also draws **the instrument**: range rings every 10 blocks chalked onto the terrain as
  the wave passes, a brighter ring at the ability's reach that flares as the front arrives, a faint
  hold over everything already swept, and range speckle on the returns.
- **`heat`** — thermal bodies, blotchy rather than circular, through walls (`NO_DEPTH_TEST`).
- **`hotcold`** — the frame. Binds vanilla's `powder_snow_outline.png` for the frost.
- **`range`** — the rangefinder bar, an SDF capsule with rounded caps and a glow.
- **`border`** — the honeycomb wall in the world.
- **`mapoverlay` + `mapborder` + `mappick`** — the map's overlays, new. See below.

**Two rules that cost a crash each and are easy to break again:**

1. **Everything must be prepared before `createRenderPass`.** Uniform writes, and especially
   `textureManager.getTexture` — the first call uploads, which is a command. Doing it inside the
   pass throws `Close the existing render pass before performing additional commands`.
2. **Never call `renderBackground` from a `Screen.render`.** In 1.21.11 the framework calls it
   itself, on its own stratum, before `render`. A second call throws
   `Can only blur once per frame`. `MapScreen` carries a comment saying so.

**And a third, learned since:** a screen cannot simply run a shader. `GuiGraphics` records draws and
hands the list over at the end of the frame, so a render pass opened inside `Screen.render` paints
*under* everything the screen is about to submit. `MapOverlay` submits a `GuiElementRenderState`
instead; the one line in the access widener is what makes that reachable.

## The teleport map

The largest single piece of work here, and finished.

**Server renders, client displays, hash decides.** `/hsmap render [radius]` walks the world one chunk
*row* at a time — async loads, snapshots read off-thread — into a `MapImage` of two bytes per pixel
(palette index + shade), deflated, CRC32-hashed, and kept in a gzipped `map.bin` beside the plugin.
This is a preparation step run roughly once per arena, not something a round does.

On teleport the server sends only the **meta** (origin, size, palette, hash). A client that already
has that hash opens instantly; otherwise it asks with `map_need` and the server streams 64 KB
slices. `MAX_SIZE = 3072`.

**Colours come from the real block textures**, not the vanilla map palette. `MapTexture` averages
each block's actual texture file, derives shaped blocks back to their source block, and applies the
plains tints to grass, foliage and water.

`MapScreen` is a paper sheet (`mapsheet.fsh`) with a compass rose (`maprose.fsh`). It opens on the
whole arena and cannot be zoomed out past that, so at full view the map sits still; the wheel zooms
towards the cursor up to `MOST_ZOOM = 8`, dragging pans once zoomed in, and a press that travelled
under `CLICK_SLOP = 4` px is a click, not a pan. Clicks are ignored for the first `ARM_MILLIS = 3000`
ms, because a wheel click held a moment too long otherwise lands on the map. When `zone_arm` arrives
first, the same screen places a danger zone instead of teleporting. A refused
landing **keeps the grant** (`GRANT_MILLIS 60_000`).

**The border and the reticle are shaders now**, and both were wrong before in ways worth remembering.
The border was 160 little squares in the interface's green: it fell apart into dots when zoomed in,
clotted into a band when zoomed out, and could not warn anybody because the wall's own colour — blue
standing still, red closing in, green opening out — was thrown away. It is a distance field taking
its tint from `BorderZone.tint()`, with the honeycomb running along it. Nothing is drawn outside it:
a grey wash and a purple storm were both tried and both fought the sheet. The reticle gained bearing marks and, more usefully, **says whether the click will be
accepted**: the border is on this client already, so green means it will land and red means the
server will refuse.

## What else is in

- **The wheel** — eight wedges a page, selected by angle rather than by hover. All fourteen abilities
  wear their own animated sprite in the interface atlas. Sprite files are named after the drawing,
  not the ability id (`dangerzone`, `stoersender`, `uv_geraet`); `AbilityWheel.iconOf` falls back to
  the stand-in item when `Ability.sprite` is null.
- **The wheel knows about cooldowns**, which is what lets it answer "what can I buy" while nothing
  is being pointed at. `hideandseek:cooldown` carries only what is still cooling; the client runs
  the countdown itself, because the one thing it cannot work out is when a wait *began*. `CooldownFeed`
  sends it after every use, at the handshake — a seeker who relogs mid-round otherwise holds no
  timers at all — and when a round starts, where the timers are cancelled rather than expiring.
- **Title screen** — Singleplayer and Multiplayer are hidden; what is left is **Join Event**, Options
  and Quit, with a live player count for `event.sniffa.studio` underneath. `ping_event_server=false`
  in `config/hideandseek.properties` stops that ping. The splash is replaced with `twitch.tv/marguhl`
  in the show blue (`0x3D9CFF`); both Sniffa PNGs sit bottom right.
- **World rules** — peaceful, permanent noon (time 6000, daylight cycle off).
- **Presence** — joins and departures in the show's voice with a head count, and vanilla's own lines
  suppressed. A hider who closes the game mid-round is announced as a result with their place instead,
  by `CatchService`; `Presence` asks first (at `LOWEST`) so it can tell the two apart while the role is
  still readable.
- **Commands** — `/hsround`, `/hsskip`, `/hsborder`, `/hsability`, `/hspoints`, `/hsdebug`, `/hsmap`.
  `/hsdebug <ability>` fires any ability free of cost and outside a round. `/hsskip` is `/hsround skip`
  in seven characters — it is the one call made under time pressure with an audience watching, and it
  reports how much time it cut.

## Open, in the order I would do it

**Almost none of it has been played with more than two people.** Everything below that is not marked
as blocked on somebody else is really blocked on that.

1. **The hiders' half has never been played.** Two accounts and a round: `/hsborder here 200`,
   `/hsround start <du>`, `/hsskip`, then `G` on the second client. What to watch for — the wheel
   shows three wedges, points climb by one every two minutes, `/hsdebug radar` on the seeker throws
   the red ERFASST warning on the hider, and a jammer bought first makes that warning stop arriving.
2. **The mod cannot reach anybody yet.** `enforce: true` is live and the kick message points at
   sniffa.studio. The plan is Modrinth: `MODRINTH.md` holds every field and the description, the
   jar is ready, and what is left is creating the project and submitting it for review. This blocks
   the event itself rather than the game, and it is the only item with a deadline attached.
3. **Two things the host has to remember that the plugin could carry.** A round does not put anybody
   anywhere — it starts where people are standing — and between rounds every caught player has to
   reconnect by hand, which at a hundred players is its own event. Neither is broken; both are
   unmanaged.
4. Decide `VISIBLE_MULTIPLE`. 150 blocks is a lot of leaflet for three points.
5. The props behind the icons. Every ability has its glyph now; only Radar has a model in the
   world.
6. Balance, which needs a played round more than it needs an opinion. The sharpest question is what
   Aufleuchten (9) still sells now that Radar (3) outlines everything within fifty blocks.

## Decided on 2026-08-19 — do not re-open

Four mechanics were proposed, read against the code, and turned down. They are written here with
what follows from each, so nobody spends an afternoon rediscovering them.

**No clock on the hunt.** A round ends when the last hider is found, or when the host types
`/hsround stop`. It follows that a single good hider can keep a round going indefinitely, and that
stopping by hand is the *ordinary* way for the hiders to win rather than an abort — which is why
`Round.stop()` says "Runde beendet" and the final table names whoever was never found.

**No automatic phases.** `GamePhase` described five beats — Eröffnung, Es wird eng, Druck, Endphase,
Letzte Phase — with the behaviour each was supposed to bring, and not one of them was ever built. It
has been **deleted**, along with the `ShopSession.phase` that was the only thing reading it. That is
the honest state of it: a file whose own documentation described a timed, catch-scored round was
going to keep reading as unfinished work rather than as a design that was considered and dropped, and
the next person through would have built it. The entry is in the git history if it is ever wanted
back.

**The border closes on command, not on a timer.** `RoundBorder` can already walk from one radius to
another over a duration, the mod draws the wall and colours it by what it is doing, and
`BorderEnforcer` teleports people back inside rather than hurting them. All of that is driven by
`/hsborder set <radius> <seconds>` and by nothing else, on purpose: the beats are called, not
scheduled.

**Caught players are removed from the server.** Not spectators. The one exception is the last hider,
who stays to watch the ending — see `CatchService.watchTheEnd`. Everybody else is kicked and kept out
until the next round starts.

## The version split is deliberate — do not "fix" it

The network runs two Minecraft versions, and it looks like something nobody got round to tidying.
It is not. Checked on 2026-08-19, and every direction out of it is closed:

```
lobby-1         Paper 26.2      ten plugins: Nexo, ModelEngine, MythicMobs, FAWE, ImageFrame, …
bau-1           Paper 1.21.11   holds the worlds `hideandseek` and `hideandseek2`
hideandseek-1   Paper 1.21.11   one plugin: this one
```

**Nothing can move up.** owo-lib's newest release is 26.1.2 and supports 26.1, 1.21.11 and 1.20.6 —
there is no 26.2 build. This mod is pinned to 1.21.11 because of it, which is written down in
`gradle.properties` and is the reason the whole project targets that version.

**lobby-1 cannot move down.** World data only goes forwards, and its plugin stack is the expensive
one on the network.

**And bau-1 must not move up**, which is the one that would be easy to get wrong. It is the *build
server for this game's arena* — the `hideandseek` worlds live there and are copied to
`hideandseek-1`. Raise it to 26.2 and nothing built on it can ever be copied down again.

So `bau-1` and `hideandseek-1` are correctly paired, and `lobby-1` sits alone for good reasons.

**What it costs:** the seamless server switch cannot work across versions — different versions mean
different registries, so the client must go through the configuration phase whatever the proxy does.
That is what `sniffa-velocity` was forked to shorten. Measured, that switch is 152–452 ms and the
patch is worth 60–200 ms of it, so this is a small price and not a reason to migrate anything. The
three seconds players actually complained about came from a resource-pack setting on the lobby.

**What would change it:** owo-lib for 26.2. When that exists, the mod, `hideandseek-1` and `bau-1`
move together, in that order, and the seamless switch becomes worth building.

## Building and deploying

- Mod: `./gradlew build` → `build/libs/hideandseek-1.0.0.jar`. Bundles Fabric API,
  fabric-language-kotlin, GeckoLib and owo-lib, so players need nothing but the Fabric Loader.
  Releases go to Modrinth (`MODRINTH.md`) and are listed in `CHANGELOG.md`.
- Sounds: `tools/sounds.sh` regenerates all eleven from `tools/SoundForge.java` and needs ffmpeg.
  `tools/` is kept outside the repository; only needed when a cue changes, and the ogg files are
  committed.
- The radar's model, texture and glowmask: `tools/radar-prop.ps1`, and rerunning it reproduces the
  committed files byte for byte.
- Server: built and deployed from the `sniffa-hideandseek-server` repository.
- **Rebuilding changes the jar's checksum without changing its content** — Gradle stamps timestamps
  into it. Compare sizes and dates, not hashes, when working out whether a deploy is current.

## Pause, 21.08.2026 — was heute dazukam und was offen ist

Alles hier ist deployt auf `hideandseek-1` und startet mit 0 Fehlerzeilen. Was **niemand im Spiel
gesehen hat**, steht unten unter „Ungeprüft" — das ist die ehrliche Liste.

### Ränge und Tablist
- LuckPerms liest die geteilte Mongo-DB; Ränge gelten netzwerkweit.
- Rang-Badges brauchen zwei Dinge: das Zeichen **und** den Font `nexo:default`. Ohne den zeichnet
  der Client irgendetwas aus der Vanilla-Schrift. `NexoGlyphs` macht beides.
- Während einer Runde ersetzt die **Rolle** den Rang im Eintrag, sie steht nicht davor.
- Neue Nexo-Glyphen für die Rollen, von Nexo selbst vergeben: **SUCHER U+A413**, **VERSTECKER
  U+A415**, definiert in `glyphs/hideandseek/rollen.yml` auf der Lobby.
- Tablist im Netzwerkstil: Kapitälchen über `SmallCaps`, Farben `#4595FF` / `#AAAAAA` / `#555555`.
  Die Palette gilt inzwischen für **alle** Oberflächen, auch Chat-Prefix.

### Regie-Werkzeug
- `/hswatch` (oder `/hs watch`) öffnet eine Kiste mit einem Kopf pro Verstecker; Klick teleportiert.
- **Serverseitig gebaut, deshalb gibt es nichts abzusichern.** Die frühere Fassung schickte die
  Liste an den Client — Kanal, Payload, HUD und Debug-Vorschau sind gelöscht.
- Der Client zeichnet die Köpfe flach statt als 3D-Würfel (`FlatHeadMixin`), aus dem Profil, das im
  Item ohnehin steckt. Kein zusätzlicher Kanal.
- `/hsround start <Sucher> [Regie...]` — Namen nach dem Sucher ersetzen die Op-Vorgabe komplett.

### Befehle
- Alle acht haben Tab-Completion; `/hs` ist der Wurzelbefehl und leitet weiter (prüft die Rechte
  selbst, weil Bukkit nur die des getippten Befehls prüft).
- `/hsdebug watch` zeigt dieselbe Kiste aus allen Online-Spielern — liest die Spielerliste, **nie**
  die Runde.

### Fallen, die je eine Stunde gekostet haben
- `Protection` bricht **jedes** `InventoryOpenEvent` ab. Eigene Menüs müssen ausgenommen werden
  (`WatchMenu.owns`), sonst passiert wortlos nichts — kein Fehler im Log.
- `GuiGraphics.drawString` kehrt bei **Alpha 0** sofort zurück. `0xFFFFFF` ist Alpha 0; `-1` nehmen.
- Adventure vererbt Stile an Kinder: `glyph.append(name)` gibt dem Namen den Glyphen-Font, und der
  hat keine Buchstaben → Kästchen. Aus leerer Wurzel bauen, dann sind es Geschwister.
- Bukkit verträgt kein `openInventory`/`teleport` **innerhalb** eines Klick-Events — einen Tick
  später einplanen.
- `SkullMeta.setOwningPlayer` holt die Textur erst bei Mojang; bei Online-Spielern
  `setPlayerProfile(player.getPlayerProfile())` nehmen, das ist sofort da.
- `ranks.refreshAll()` stand **vor** dem Phasenwechsel — dadurch behielten alle Verstecker ihr
  Namensschild die ganze Runde. Das Neuzeichnen wohnt jetzt in `enter()`.

### Ungeprüft (braucht Spieler)
- Tablist, Rollen-Badges, Namensschild-Unterdrückung bei Versteckten.
- Das Regie-Menü mit echten Versteckten. **Braucht drei Leute gleichzeitig**: Sucher, Regie,
  Verstecker. Mit zweien geht nur `/hsdebug watch`.
- Titelbildschirm des Mods: Buttongrößen, Statuszeile, ob „Join Event" verbindet.
- Die Border ist nach jedem Neustart weg — `/hsborder here 200` von etwa -605,-599.

### Offene Entscheidung für später
Weitere Prop-Modelle. Kandidaten in dieser Reihenfolge: **Störsender** (heißt nach einem Gerät, hat
heute gar keine Sichtbarkeit), dann die drei Messgeräte des Suchers (Entfernungsmesser, Kompass,
Wärmebild) als eine Formfamilie. Tempo/Sprint/Elytra brauchen keins. Aufleuchten/Teleport/Tausch
wollen Partikel, kein Mesh.

**Vorher zu klären:** `Props.place` schickt an *jeden* im Umkreis. Ein Verstecker-Prop steht damit
sichtbar für den Sucher da. Entweder bewusster Handel — oder `Props.place` braucht eine
Empfängerliste, und die gehört vor die Modelle.
