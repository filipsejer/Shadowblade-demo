# Spellblade

Top-down action roguelike with a Kingdom Hearts-style combat feel, played room by room. Plain Java (Swing / Java2D) — no dependencies.
All the art is pixel art painted by code at startup (there are no image files): see **Graphics engine** below. All the music and sound effects are
synthesised by code too (there are no audio files): see **Sound engine** below. The game opens with a short story that teaches the controls (see **The opening story**). Level 1 is **The Whispering Forest**, level 2 is **The Neon City**, and level 3 is **The Mad Scientist's Laboratory**.

## Just want to play it?

Download [`dist/Spellblade.jar`](dist/Spellblade.jar) and double-click it (or run `java -jar Spellblade.jar` in a terminal).
You need [Java](https://adoptium.net/) 17 or newer installed — no other setup, no install. See **Controls** below for how to play.

**On a Mac**, the first time you open it Gatekeeper will say it can't verify the jar is free of malware — that's just
because it isn't signed by a paid Apple Developer account, not a sign anything's wrong. Instead of double-clicking:
**Control-click `Spellblade.jar` → Open → Open** (a dialog with an actual Open button shows up this time). That only
needs doing once; after that it opens normally. If that doesn't work, **System Settings → Privacy & Security** has an
**Open Anyway** button for it near the bottom of the page.

## Run from source

```sh
./run.sh              # normal
./run.sh safe         # software rendering, memory capped, log written to game.log
./run.sh silent       # no sound at all (the sound engine is never started); can be combined: ./run.sh safe silent
./run.sh notutorial   # the title screen starts with the opening story switched off (T on the title screen switches it back on)
```

Needs a JDK 17+.

## Controls

| Key | Action |
| --- | --- |
| W A S D | Move |
| Space | Roll (invincible while rolling). It carries you about 45% further from character level 5, and about 90% further from level 10 |
| Up / Down | Choose a command in the menu (bottom-left): **Attack**, **Magic**, **Items** |
| Enter | On **Attack**: attack. One press is one attack (holding it does nothing), so tap it to chain combos. You dash through the nearest enemy. A press during a swing is remembered for a moment, so the next hit follows straight on |
| Right or Enter | On **Magic**: open the spell list. There, Up / Down pick a spell, **Enter casts it**, **Left** (or Backspace) goes back |
| Hold Shift | Opens the spell list straight away from anywhere, and **keeps it open** after each cast so you can keep casting. Up / Down pick a spell, Enter casts. Letting go of Shift closes it and puts the cursor back where it was. (Shift + Enter casts the spell you used last right away.) |
| Items | Greyed out for now ("coming soon"); the cursor skips it |
| Tab | Lock on to the nearest enemy; press again to cycle to the next-nearest (wraps around). Attacks and spells all go to the locked enemy. The lock drops when it dies |
| Q | Release the lock |
| E | Talk to a trainer or the guide in the hub (when you stand next to them). In the opening story, E (or Enter) turns the page of a conversation |
| Esc | Pause. Also leaves a trainer's menu. On the pause screen **Up / Down** choose *Music* or *Effects* and **Left / Right** change the volume |
| M | Mute / unmute all sound (anywhere, including the title screen). Your volume settings are remembered between runs |
| R | Restart after dying (in the opening story, that starts the story over) |
| T | On the title screen: switch the opening story on or off |

After a successful cast (the normal way) the cursor drops back to **Attack**, so casting again means going back down to Magic; holding **Shift** skips all of that. Spells cost
a lot of MP (a full bar is about four Fireballs), MP only trickles back on its own (5 per second) and from landing melee
hits, and each spell has its own cooldown. A failed cast (not enough MP, still cooling down, nothing to hit) leaves the
list open and costs nothing.

Two more indicators live on the character rather than in a HUD panel, so they stay wherever you're standing: a row of
small dots under your feet lights up hit by hit to show how far through the attack chain you are, and a ring just to
the right of you sweeps shut as your roll's cooldown charges back up — it isn't drawn at all while the roll is ready,
and reappears the moment you use it. Both are drawn in `WorldRenderer` (`drawComboDots` / `drawRollCharge`), not
`Renderer`'s screen-space HUD, so they scroll with the world instead of sitting fixed on screen.

## Main menu

The game opens on a menu with two rows, **W / S** (or the arrows) to choose and **Enter** to confirm:

- **Play** starts the opening story from the very beginning, exactly like pressing Enter always has (`T` still switches the story off first, so Play drops straight into level 1 instead — `./run.sh notutorial` starts with it off).
- **Select Chapter** opens a second screen listing the three levels (name, boss, theme). Choosing one skips straight to that level's hub with a level-15 character: `World.CHAPTER_SELECT_LEVEL` skill-ups' worth of skill points (28, at 2 per level — see `World.SKILL_POINTS_PER_LEVEL`), all unspent, nothing bought at the trainers yet. It's a quick way to try a level, or the trainers' late-game upgrades, without the grind. `Esc` backs out to the main menu.

Both screens are built in `World.updateTitle` / `World.updateChapterSelect` / `World.beginChapter`, and drawn in `Renderer.drawTitle` / `Renderer.drawChapterSelect`.

Select Chapter has one extra row below the three real levels: **PROTOTYPE**, a hand-drawn layout being tried out (`Level.protoSketch()`, `World.beginPrototype()`). It's not one of the three real levels — it doesn't count towards `Level.COUNT`, has no enemies, and can't be "cleared" — just a level built with the room-shape system (see **Room shapes**) to try against something someone actually sketched, before committing to it as a real room in a real level. Real rooms with real walls between them, a doorway pushed off-centre on two of them to match the sketch's zigzag (see **Room shapes**), and a couple of placeholders for systems that don't exist yet: the barricade across the deck is a row of plain, walk-through (`radius` 0) log landmarks standing in for real barricade art and the mission-flag system that would one day remove it; the two doors marked as exits to somewhere not yet designed lead to small, empty stub rooms rather than faking content that isn't there.

## Layout

| File | What it does |
| --- | --- |
| `Level.java` | The maps: rooms (each one or more glued-together rectangles — see **Room shapes** below), corridors (doors), the trainers, the guide's spot, the sealed boss door, and walkable-area collision. `Level.create(n)` builds level n (`levelOne()`, `levelTwo()`), `Level.tutorial()` the opening story's map, `Level.town()` Transit Town |
| `Tutorial.java` / `Dialogue.java` | The opening story: the script (each lesson is a scene with the squirrel) and the speech box's rules (typewriter text, lines that wait for a key or are called out, the voice's chirps); `Dialogue` is reused as-is for Transit Town's locals |
| `TownArt.java` | Transit Town's three locals and the train station, shuttered and lit |
| `Breakable.java` / `BreakableArt.java` | The crates and barrels: their rules (solid, one hit, XP) and their sprites in a forest, city and laboratory look |
| `SquirrelArt.java` | The squirrel's sprites (sitting, running, frightened, throwing) and its acorn |
| `World.java` | Game state and rules: entering rooms spawns enemies and locks doors, collisions, XP / level-ups, menus |
| `Player.java` | Movement, combo state machine, dash-through attacks, roll, spell casting |
| `CommandMenu.java` | The Attack / Magic / Items menu: turns arrow keys and Enter into "attack" or "cast this spell" |
| `Enemy.java` | Enemy types (`Type` enum holds the stats) and their AI |
| `Spells.java` / `Ability.java` | Spell behaviour and per-level numbers / descriptions |
| `UpgradePool.java` / `Upgrade.java` | Everything the trainers sell (combo length, spells, stats) and what it costs |
| `Renderer.java` | HUD, menus, pause / title screens, camera, screen shake. `World` has no drawing code |
| `WorldRenderer.java` | Draws the world itself: level, shadows, depth-sorted sprites, telegraphs, projectiles, effects, lock ring, health bars |
| `LevelView.java` | The level's background: bakes floors, walls, props and scenery into cached image chunks; draws barriers over closed doors, and glows |
| `PixelCanvas.java` / `Sprite.java` / `Art.java` | The graphics engine: a pixel painting canvas, an anchored sprite, and the sprite atlas |
| `PeopleArt.java` | Sprites for the hero, the guide and the three shops |
| `CreatureArt.java` | Sprites for the enemies and bosses, in forest, city and laboratory versions, plus the shade |
| `FxArt.java` / `PixelFont.java` | Attack and effect sprites (slashes, fire, lightning, frost, particles, projectiles) and the digit font for damage numbers |
| `Theme.java` / `ThemeArt.java` | The forest, city and laboratory looks: floor tiles, walls, barriers, colours |
| `ForestProps.java` / `CityProps.java` / `LabProps.java` | Trees, bushes, mushrooms; lamps, cars, crates; tanks, tesla coils, server racks, lab benches, monitors: the scenery |
| `Minimap.java` | The round radar in the top-right corner |
| `Effect.java` | Slashes, lightning, damage numbers, sparks |
| `Dsp.java` | Sound toolbox: filters, envelopes, band-limited oscillators, reverb, echo, limiter, A-weighting |
| `Instruments.java` | The band: flute, harp (plucked string), pads, basses, strings, brass, e-piano, synth leads, three drum kits |
| `Song.java` / `Songs.java` | The music: notation parser, chord voicing, and the six pieces (forest, city, lab and each level's boss) written out bar by bar |
| `Music.java` | The live orchestra: plays a song sample-accurately, fades its parts in and out with the mood, crossfades between songs |
| `Sx.java` / `SfxSynth.java` / `SfxBank.java` / `Snd.java` | The sound effects: a painter, the recipe for each of the ~70 effects, the bank they are painted into at start-up, and the catalogue with each one's loudness |
| `AudioEngine.java` | The mixer and the audio thread (music + effects + ambience, ducking, limiter, output) |
| `GameAudio.java` / `MusicDirector.java` / `AudioSettings.java` | The link to the game: turns world events into positioned sounds, picks the music for the situation, remembers your volumes |

## Crates and barrels

Every room except the boss's has two or three crates and barrels standing in it (about 28 per level; the tutorial has none). They are solid, so you and the enemies walk around them,
and each goes in **one hit** from a sword swing (anything in the arc), a Fireball blast, or a Lightning bolt (the crates next to the enemy it strikes). Smashing one pays a little XP,
which floats up over the spot: a crate is worth 4 XP and a barrel 6 in level 1, and 1.5x and 2x that in levels 2 and 3, about 140 XP in all in level 1. They never respawn, and
do not count towards clearing a room. They are placed by `Level.scatterBreakables()` (deterministically, away from walls, doorways, trainers and the spawn point), worth `Breakable.Kind.baseXp`
and broken by `World.smash()`.

## The opening story

A new game begins with a short story that teaches the controls without a single "press W to move" box. You wake up dazed beside a tree
(the screen is black, then your eyelids open, squint and blink), and a squirrel runs up and asks if you have forgotten how to move.
Each lesson leads into the next, and the HUD only grows as things are taught (health at first, then the roll button, the Attack command, and
finally Magic and the mana bar). The controls are only ever named in the squirrel's own words, and if you stall, as key caps that fade in beside
your hero (W A S D, SPACE, ENTER, SHIFT + ENTER) and light up under the keys you press.

1. **Move.** The squirrel hops across the clearing and waits; you walk over to it. (The way out of the clearing is shut with brambles until you have learned your lessons.)
2. **Roll.** It says you rolled into that tree and hit your head (the camera turns to look at the tree; roll into the trunk yourself and you go BONK), and asks if you remember how to roll. Then it throws acorns at you: roll *through* two of them. An acorn does no damage; a hit is only a bonk.
3. **Attack.** A small forest enemy rustles out of the bushes and the squirrel bolts into the brambles, shouting advice from behind them. The doors lock and the fight music starts. The first sword hit makes the monster curl into a thorny shell.
4. **Magic.** Swords bounce off the shell, and the monster stops fighting, so the only way on is a spell: the Magic command appears, pulsing, and a Fireball cracks the shell open.
5. **The search.** The squirrel is scared of you now and hides in the next rooms (the trail, the stream); it bolts when you get within about 300 pixels, and you finally corner it in the hollow.
6. **Goodbye.** It says you are dangerous and must find your own people. The screen fades out and you arrive in **Transit Town** (see below), not level 1 just yet — the tutorial gives no XP, so you are still level 1.

Dying in the story starts it over; dying later goes to level 1's hub directly (skipping Transit Town). `T` on the title screen switches the story off (`./run.sh notutorial` starts with it off). Things to tune:
the lines of dialogue and the timings are in `Tutorial.go()` and the scene methods below it; the monster's strength is in `Tutorial.go(ALARM)`; how many acorns you must dodge is `Tutorial.DODGES_NEEDED`;
how close you may get before the squirrel runs is `Tutorial.SEARCH_FLEE_DISTANCE`; the shell is `Enemy.curlUp()` / `Enemy.breakShell()` (`shellOnHit` switches it on for one enemy).

## Transit Town

A friendly hub (in the spirit of Traverse Town) between the opening story and level 1: no enemies anywhere, five open areas to wander —
the town square you arrive in, an old quarter to the east with the locals, a back alley off that, a quiet fountain square to the north,
and the edge of the woods to the west — all built by `Level.town()`. There is no minimap "rooms cleared" count and no trainers here; it
exists purely to give the story somewhere to land you, and later to be the way back into the run.

- **Three chatty locals** (an old traveller, a merchant, a child — `TownArt.java`) stand around town. Walk up to one and press **E** for
  one short, unimportant line — its own small dialogue box (`World.dialogue`, the same `Dialogue` class the opening story uses, just with
  its own blip and its own accent colour). Nothing about them matters mechanically; they're flavour.
- **The path into the forest** is a marked spot at the western edge of town (`Level.forestX/forestY`, with a couple of trees crowding in
  around it). Stand on it and press **E** to head into level 1 — this is the *only* way into level 1 the first time; there is no guide for it.
- **The train station** stands in the square from the start, shuttered and dark (`station.closed`), with a "Closed for now" label. It's the
  same spot every other level's guide would appear at (`Level.guideX/guideY`, `Level.guideAppeared` — repurposed here rather than duplicated),
  so it already gets a green marker on the minimap for free once it opens.
- **Beating level 1's boss** does *not* bring a guide to level 1's own hub the way every other level's boss does. Instead, the station opens
  (`World.stationOpen`), a short beat plays out (`World.townReturnTimer`, about three seconds) and you're brought straight back to Transit
  Town, station lit up (`station.open`) and ready. Stand by it and press **E** — exactly like any other guide — to travel on to level 2.
- **Levels 2 and 3 are unchanged**: their bosses still bring a guide to their own hub as always, with no detour through Transit Town.
  Transit Town is only ever visited twice: once after the story, once after level 1.

## Graphics engine

Everything you see is drawn by `game/*Art.java` code into small pixel images (`PixelCanvas`), scaled up 3x with
nearest-neighbour filtering so the pixels stay crisp (`Art.SCALE`). The images are built once when the game starts.

- **`PixelCanvas`** is a grid of ARGB pixels with drawing helpers: rectangles, ellipses, lines, polygons, `outline`
  (a dark border around a shape), `bevel` (light top-left, dark bottom-right shading), mirroring and colour mixing.
- **`Sprite`** is a frame plus an *anchor* (usually the feet). It draws scaled, mirrored, rotated or as a flat-colour
  silhouette (hit flashes, ghosts, status tints), and caches the silhouettes.
- **`Art`** is the atlas: name to animation frames, e.g. `hero.side.walk`, `forest.brute.windup`, `city.boss2.slam`.
  `Art.frame(name, seconds, fps)` picks the current frame; asking for a name that doesn't exist throws.
- **Characters** face right and are mirrored to face left. The hero has front, back and side views (idle, walk, attack)
  and a roll; enemies have a walk cycle and a wind-up pose that shows before an attack. Bosses have idle, walk, slam
  and burst poses, and a second, angrier look under half health.
- **Levels** are baked into 1024x1024 image chunks the first time you look at them (and one neighbouring chunk is
  prepared each frame while you play), so drawing the map costs a few image copies per frame instead of thousands of
  tile draws. Only the closed-door barriers, props that animate, glows and the entities are drawn live.
- **Depth:** characters, shops and the guide are sorted by their feet and each gets a ground shadow.
- **Lighting:** a colour wash for the mood (warm dappled light in the forest, blue dusk in the city), glows for
  lamps / neon / fire / lightning, and a vignette at the edges.
- **Themes:** `Level.theme` picks the art set. The forest has grass, flagstones, dirt paths, hedges, bramble barriers
  and tree canopy; the city has asphalt, sidewalks, brick, rolling shutters and rooftops; the laboratory has pale tile, a checkerboard lobby, teal panel walls with hazard bands, red laser gates over closed doors and a steel blast door on the sealed one. Enemies change with the
  theme too: toadstools, foxes, snap-blooms, stump golems and the Treant in the forest; rats, cats, drones, dumpsters and
  the Warden robot in the city; green oozes, wind-up mice, acid flasks, stitched mutants and the Mad Scientist in the lab. The shade is the same in all three.

To add a sprite: paint it in the matching `*Art` class, register it under a name, and ask the atlas for that name where
it's drawn. To add a theme: add it to the `Theme` enum, give it tiles and props in `ThemeArt`, and draw the enemies
in `CreatureArt`.

## Sound engine

Everything you hear is made by code, in real time or when the game starts, with `javax.sound` (part of the JDK) only used to
send the finished audio to the speakers. **Sound never slows the game down**: the game thread only drops requests in a queue,
and a separate audio thread mixes 44.1 kHz stereo in 512-frame blocks (about 11 ms). If there is no sound device (or the device
fails) the game just runs silent; if something goes wrong in one block the audio thread plays silence for it and carries on.
The sound effects are painted in about half a second on the audio thread, in parallel with opening the speakers, so the window
never waits (on a Mac the very first open of the speakers can take a few seconds; music and sounds start when it's ready).

- **The music** is four pieces, each 16 bars, written by hand as notes over a chord progression, played live on the synthesised
  instruments. Every piece is split into *layers* (pad, bass, melody, drums...) and each layer has a volume for each mood, so the
  score changes with the action without ever restarting or losing the beat:
  - **Forest** (G major, 96 bpm): a flute tune over harp and a warm pad. In a fight, frame drums, shakers, fiddles, a driving bass
    and a lower echo of the tune join in.
  - **City** (A minor, 100 bpm): an electric-piano tune over a synth pad and echoing arpeggios; the fight adds a four-on-the-floor
    beat, a driving bass and a lead.
  - **Laboratory** (D dorian, 108 bpm): a theremin over a synth pad and a ticking clock, with a fight beat and lead.
  - **Forest boss** (E minor, 120 bpm, brass and war drums), **city boss** (D minor, 128 bpm, saw lead and electronic beat) and the **Mad Scientist** (A harmonic minor, 144 bpm, a frantic organ toccata over growling bass).
    Below half health the boss music brings in a second wave of parts (`Mood.PEAK`).
  - `MusicDirector` picks the piece and mood from the game state: calm while exploring, the fight mood while a room is locked, the
    boss piece for the boss, silence after death; pausing muffles and lowers the music.
- **Sound effects** (`Snd` lists them all): swings, hits, spells, enemy tells and attacks, deaths, boss events, rooms locking,
  jingles, menu blips, footsteps (soft on forest grass, hard in the city and the hub). Each has several variants that are taken
  in turn, plus a little random pitch, so nothing ever repeats exactly. Enemy sounds are placed in the stereo field and get quieter
  with distance. Sounds have a minimum gap and a voice limit so a crowd can't machine-gun one sound, and a voice that is taken over
  fades out in 10 ms rather than being cut.
- **Ambience** is made live from filtered noise: wind and the odd bird in the forest, a low city hum and distant traffic and
  the odd horn in the city, and in the lab a fluorescent hum, ventilation and now and then a computer beep or a bubbling tank. It ducks itself during fights.
- **Mixing:** music and effects are balanced by *A-weighted loudness* (how loud a sound seems to an ear, not how much energy it has),
  so a swish, a thump and a jingle of the same number seem equally loud. Big effects duck the music by up to about 3 dB; a
  look-ahead limiter keeps the output under 0.995 whatever happens; DC offset and sub-bass rumble are filtered out; deep booms
  get overtones added so they still work on laptop speakers.
- **Key rule:** every jingle and interface blip uses only the notes C D E G A, which belong to all four keys, so an effect can
  never clash with the music underneath it.

To change the sound: melodies and chords are in `Songs.java` (each bar of a melody must add up to 16 sixteenths, checked when the
song is built); a part's volume per mood is the three numbers after its name, and its balance is in the `TRIM` table (re-measure
with the sound tests after changing an instrument). Sound effect recipes are in `SfxSynth.java`; how loud each is meant to be is the
first number in `Snd.java`. Default volumes are in `AudioSettings.java`; the saved settings are in `~/.spellblade/audio.properties`.

## Where to tune things

- **Enemy stats:** the `Type` enum in `Enemy.java`.
- **Which enemies are in a room (and how many waves):** the `Roster` passed to each room in `Level.levelOne()` / `levelTwo()`; chain `.nextWave()` calls onto it for a room with reinforcements.
- **Room layout:** the `b.attach(...)` calls in `Level.levelOne()` / `levelTwo()`. Each one hangs a room off a side of another room; the corridor between them is made for you, and an overlap check stops you placing rooms on top of each other.
- **Combo timing and damage:** constants at the top of `Player.java`, plus `startAttack()` / `doHit()`.
- **Spell numbers:** the arrays at the top of `Spells.java` (damage etc.) and the cost / cooldown in `Ability.java`. Keep the descriptions in `Ability.java` in sync. Melee gives back MP per hit: `mpPerHit` in `Player.java`.
- **New upgrades:** add a `Stat` to the list in `UpgradePool.java`. Costs: spells and combo hits in `UpgradePool`, stats are 1 point each.
- **Skill points per level-up:** `SKILL_POINTS_PER_LEVEL` in `World.java`.
- **The roll bonuses (levels 5 and 10):** `ROLL_BONUS_LEVEL` / `ROLL_BONUS` and `ROLL_BONUS_LEVEL_2` / `ROLL_BONUS_2` at the top of `Player.java`.
- **Shade timing:** `SHADOW_PERIOD` in `Enemy.java` (seconds per mode).
- **Level difficulty:** the multipliers passed to `finish(...)` at the end of each level method in `Level.java`: health and damage for big enemies (brutes, the boss), then separately for small ones (grunts, runners, shooters, shades).
- **The boss:** the `BOSS` entry in `Enemy.Type` for its stats, and `updateBoss()` / `fireBurst()` in `Enemy.java` for its attacks. The final boss's two-stage fight is `Enemy.enterFinalStage()` (triggered from `World.collectDead()`, which is what turns his "killing" blow into a stage change instead) plus the `phase2`/`chargeCd` handling in `updateBoss()`; `Enemy.CHARGE_SPEED` tunes his stage-two dash.
- **Transit Town:** its layout and furniture are all in `Level.town()`; the locals' lines are right there too. How long the beat lasts between level 1's boss dying and landing back in town is `World.townReturnTimer`'s starting value, set in `World.endCombat()`.

## Levels, skill points and the trainers

The whole level is one continuous map (no scene switching) made of rectangular rooms joined by corridors. From the hub
there is a branch of rooms going north, east and south, and the boss room is to the west (level 1, the forest):

```
                    [FERN HOLLOW]
                          |
                     [MOSSY TRAIL] -- [MUSHROOM GROVE]
                          |
  [BOSS] - - - - -  [   HUB   ] -- [BRAMBLE GATE] -- [SUNLIT GLADE]
                          |                               |
                      [ BURROW ]                     [OLD SHRINE]
                          |
      [TREASURE GROVE]-[DEEP WOODS]
```

- Walking into an unvisited room spawns its enemies and locks all of its doors (red bars). Kill everything to unlock them.
- **Some rooms have a second wave:** a `Roster` can be built with `.nextWave()` (e.g. `new Roster().add(GRUNT, 4).nextWave().add(BRUTE, 2)`),
  which spawns the first batch as normal but only sends in the second once the first is wiped out (after a 1.2s pause,
  `World.NEXT_WAVE_DELAY`) — the room stays locked and "in combat" the whole time. In level 1 this is the three leaf
  rooms at the end of a branch (**Fern Hollow**, **Old Shrine**, **Treasure Grove**); every other room is still a
  single wave.
- Clearing a room restores 25% HP and 50 MP. Cleared rooms stay empty.
- **The boss door is sealed** (purple bars, "SEALED") until all 9 other rooms are cleared. The level is cleared when the
  boss dies.
- **Levelling up gives skill points** (2 per level). There are no pop-up choices: go back to the hub and talk to a
  trainer (stand next to them and press **E**). **W / S** choose, **Enter** buys, **Esc** leaves. The trainers pulse when
  you have points to spend.
  - **Swordsmaster** (a knight behind a counter of swords): combo length, melee damage, attack speed, reach, HP / MP on hit.
  - **Wizard** (a robed mage with a crystal ball): learn spells, level them up, spell power, cooldowns.
  - **Survivalist** (a ranger with potions and supplies): max HP, move speed, max MP, roll cooldown.
- Costs: a new spell is 2 points, spell level 2 / 3 / 4 cost 2 / 3 / 4, combo hit 3 / 4 / 5 cost 2 / 3 / 4, and stat
  ranks cost 1 each.
- **The boss** is armored: it can't be stunned, interrupted or knocked back, so you have to dodge. It slams the ground
  when you're close (big red ring, roll out of it) and fires a ring of bullets from range. Below half health it speeds
  up and fires more bullets.
- **Levels.** Beating a level's boss makes a **guide** (a friendly cloaked traveller with a lantern) appear in the hub. Stand next to them and
  press **E** to travel to the next level. You keep everything: character level, skill points, spells, stats. The hub
  comes with you (same size, same three trainers in the same places); only the rooms around it change, and you arrive
  fully healed. Level 1 is the exception — its boss sends you back to **Transit Town** instead (see above), and the
  train station there takes you on to level 2. Level 2 has a different shape (longer trails, a 4-room tower to the north, the boss room to the **east**),
  tougher enemies and its own boss, the Warden. It is the city: Market, Workshop, Storeroom, Library, Observatory, Archive, Greenhouse, Kitchen, Pantry and Cold Store. Brutes and the boss have 1.35x health and 1.2x damage there; the small
  enemies (grunts, runners, shooters) have 1.9x health and 1.5x damage. After the last level's boss the game says
  GAME CLEARED. Dying restarts the whole run from level 1.
- **Level 3: the laboratory.** The boss room is to the **north** of the hub. West: the Chem Lab, with the Mutant Pens above it
  and the Gene Lab beyond; east: the Power Room, the Reactor and the Coolant Plant; south: the Specimen Vault, the Cryo
  Chamber and the Observation Deck. Brutes and the boss have 1.7x health and 1.4x damage, the small enemies 2.6x and 1.9x,
  with Shades in most rooms. The creatures: green **oozes** (grunts), wind-up **mice** (runners), **acid flasks** that spit
  globs (shooters) and stitched **mutants** (brutes). Closed doors are red laser gates; the sealed boss door is a blast door.
  - **The Mad Scientist** is the last boss, and unlike the other two he has **two whole health bars**, one per stage (the HUD
    bar says which stage he's in). Like the other bosses he is armoured: no stunning.
    - **Stage 1** is his entire first bar, fought alone: he slams his giant wrench when you are close, flings a ring of acid
      from range, and **lobs flask bombs** — four green circles are marked on the floor (the first right where you are
      standing), a flask falls onto each and they burst after about a second. Anyone in a circle when it bursts is hurt; roll
      through or step out.
    - **The hit that would finish stage one doesn't kill him.** He goes untouchable for about two seconds — a **STAGE 2**
      banner shows and he cackles while four regular enemies spring up around him — and comes back with **his health bar
      completely refilled**. From there he is **50% faster**, lobs seven bombs at a time more often, fires bullets more
      often, **keeps summoning waves of regular enemies** (oozes, mice and flasks, three at a time every 8 seconds, up to
      about ten enemies at once), and now also **charges you down**: a telegraphed green line down the lane he's about to
      take, then a fast committed dash that hurts on contact. *This* is the hit that can finally kill him. Creatures he
      summons are worth no XP, so they can't be farmed, and **they and any bombs vanish when he dies**.
- **Roll bonus:** when your character reaches level 5 the roll travels about 45% further (164 to 237 pixels), and at
  level 10 about 90% further (311 pixels). The roll lasts the same time, so it is just faster. The bonuses stay for the
  rest of the run. The pause screen shows your roll distance.
- **Shades (levels 2 and 3).** A floating ghost that flips between **solid** and **shadow** every 5 seconds. In shadow
  it turns into a dark see-through silhouette with a violet edge, and it **can't attack you and you can't attack it**:
  melee, fireballs, lightning, ice storms and burning all pass through it, the auto-target and Tab skip it, and nothing
  pushes it (or gets pushed by it). A ring around it counts down the time left in its current mode (pink = solid,
  lilac = shadow) and it flickers for the last second. When it turns solid it needs half a second to materialise
  before it can strike, and going into shadow cancels any attack it was winding up. Shades start in a random mode, so
  a group isn't in step. A locked-on shade that goes into shadow keeps its lock on hold and gets it back when solid.
- To add a room: call `b.attach(existingRoom, Dir.NORTH / EAST / SOUTH / WEST, "NAME", width, height, roster)` in
  `Level.levelOne()` / `levelTwo()` (or a new level method, then add it to `Level.create` and raise `Level.COUNT`). To add a trainer, add a `Station` to the list at the bottom of it.

## Room shapes

A room isn't only ever a single rectangle — `Builder.extend(room, dx, dy, w, h)` glues another rectangle onto one, at
an offset from its first piece's top-left corner, so a room can be an L, a cross, a wide chamber with a little alcove
off it, anything built out of boxes (see `Level.testShapes()` for a worked synthetic example, and `Level.protoSketch()`
— reachable in-game from Select Chapter's extra **PROTOTYPE** row — for the small plaza's kid-mission corridor, a
long extra piece glued flush onto its east wall). It's still no corridor and no new connected room — just growing
that one room's own footprint — and every piece of it (collision, the floor bake, the minimap, prop scattering, where
enemies spawn) treats the whole cluster as one seamless room, not several.

A doorway between two separate rooms doesn't have to sit centred on the shared wall either: `Builder.attach(...)`
takes an optional corridor width and length, and beyond that an optional `doorOffset` — how far off centre the
doorway itself sits (positive east for a north/south doorway, positive south for an east/west one), while the two
rooms stay placed centred on each other exactly as `attach()` always has. That's how `Level.protoSketch()`'s zigzag
staircase works: one doorway biased west, the next biased east, both still ordinary centred rooms. See
`Level.testOffCentreDoors()` for a minimal worked example.

**Placing pieces:** give `extend()` a piece flush against the one it's meant to join — no need to fudge a gap or an
overlap yourself. It grows a little way past that seam on its own (`Builder.bridge()`, the same idea as a door's own
walkable area reaching a little way into the rooms on either side of it): two rectangles that only ever touch at a
single line would otherwise leave a body-radius-wide gap neither one claims, where you'd get stuck standing right at
the seam.

**What it can't do:** every piece is still an axis-aligned rectangle — no diagonal walls, no curves. That's a
deliberate trade-off: real polygon collision would mean rewriting how a body finds its way out of a wall (right now
just "clamp to the nearest edge of the nearest rectangle"), and touching the minimap and every existing level along
with it. Rectangles glued together, plus an off-centre doorway where a corridor needs one, get most of the way to a
hand-drawn map's variety — wide chambers, alcoves, jogged corridors, zigzag staircases — for a much smaller, much
safer change.

**Grass patches** (`level.grassPatches`, a plain list of rectangles) are solid ground sitting on top of a room's
floor rather than cut out of it — real grass tiling (always forest tiling, whatever the level's own theme, the same
idea as `Landmark`'s forest art), but you can't stand on it at all, the same as a wall, rather than a prop you walk
around. `Level.protoSketch()` uses them for the grass either side of its entrance doorway and the bed in its plaza.

## Minimap

A Kingdom Hearts 2 style radar sits in the top-right corner: a round map with a gold bezel, centred on you. The arrow in
the middle points where you're facing, the map scrolls under you, and north is always up (the **N** at the top).

- **Rooms** appear once you've been inside them (fog of war). Blue = the hub, red = a fight in progress, green = cleared.
  Set `REVEAL_ALL = true` in `Minimap.java` to show the whole map from the start.
- **Corridors** are pale blue when open, red while locked for a fight, and purple while sealed (the boss door).
- **Dots:** red = enemies (the boss is bigger with a ring), gold / purple / teal squares = the three trainers. The enemy
  you've locked onto gets a white ring.
- Size and zoom are `RADIUS` and `SCALE` at the top of `Minimap.java`.
