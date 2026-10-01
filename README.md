# Spellblade

A top-down action adventure in plain Java (Swing / Java2D), with no dependencies. It's a mashup of three games:

- **Kingdom Hearts**: an explorable world between fights, with people to talk to, a story, hidden chests, and skill
  points you spend on permanent upgrades.
- **Survivor.io**: the fighting. Hordes pour in, every kill drops XP, and each level-up offers a choice of skills that
  fire on their own. Equipment with stats comes with you into every fight.
- **Risk of Rain**: each fight takes place on a battlefield generated fresh every time, with a danger clock that makes
  the monsters tougher the longer you take, caches to buy with the gold you pick up, and loops that make a cleared
  challenge harder (and better paid) every time you go back.

Chapter 1 is **The Whispering Forest**. The monsters, bosses, the forest's trees and rocks, the nests, the crates and
the chests are drawn pixel art made with [PixelLab](https://pixellab.ai) (the PNGs in `res/art`); everything else is
pixel art painted by code at startup (see **Graphics engine**). All the music and sound effects are synthesised by code
(there are no audio files; see **Sound engine**).

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
```

Needs a JDK 17+. The drawn sprites are loaded from `res/` on the classpath (`run.sh` adds it). Without it, the game
still runs, with code-painted sprites in their place.

To rebuild `dist/Spellblade.jar` (compiled for Java 17, with the images inside):

```sh
rm -rf out_dist && mkdir out_dist && javac --release 17 -d out_dist src/game/*.java && (cd out_dist && printf 'Main-Class: game.Main\n' > ../manifest.tmp && jar cfm ../dist/Spellblade.jar ../manifest.tmp game -C ../res art) && rm manifest.tmp && rm -rf out_dist
```

## Controls

| Key | In the world | In a fight |
| --- | --- | --- |
| W A S D | Move | Move |
| E | Talk, open a chest, step up to a gate | Open a cache (if you have the gold) |
| Enter (or J) | Swing your sword (break crates for a few coins); confirm on screens | Attack. **Hold it** to keep swinging; you dash through the nearest enemy |
| Space | Roll | Roll, once you've picked **Evasive Roll** (or learned **Tumbler**) |
| 1 / 2 / 3, R | | On a level-up or chest: take that card (4 with the Ring of Fortune); R rerolls |
| Tab / Q | | Lock on (cycle) / release |
| Esc | Pause: resume, **Equipment**, volumes, **Save & Quit** | Pause: resume, volumes, **Retreat** |
| M | Mute / unmute | Mute / unmute |

On the trainer's, the merchant's and the Armory's screens: **W / S** choose, **Enter** confirms, **Esc** leaves.

## The adventure

**New Game** starts the story; **Continue** picks it up where you saved (the main menu also has the **Armory** and
**Quit**). You arrive at **Mossbrook**, a camp of forest folk, and walk around freely: no monsters live in the world
itself (`Worlds.forest`).

```
                       [ HOLLOW'S EDGE ]          (the gate to the Blighted Hollow)
                               |
   [ MUSHROOM RING ] -- [  OLD SHRINE  ]
                               |
   [ WEST THICKET ] -- [ MOSSBROOK CAMP ] -- [ SUNLIT PATH ] -- [ HUNTER'S CLEARING ]   (the gate to the fox dens)
                               |
                         [ RIVERBANK ]
```

- **People** (`Story.talk`): **Elder Rowan** (the story and the main quest), **Ranger Ash** (the trainer),
  **Bramble** (the merchant), **Wren** (a child with hints), **the Hermit** at the Old Shrine (lore) and **Hunter Fenn**
  (a side job). Walk up and press **E**. What they say changes as the story moves on; a gold **!** over someone means
  they have something new for you (or, over Ash, that you can afford an upgrade).
- **Chests** are hidden around the forest: gold, items, and a glowing seed worth a skill point (`Story.treasure`).
  Crates and barrels break for a few coins.
- **Ranger Ash** turns skill points into **masteries** (`Mastery`): permanent upgrades that apply in every fight.
  Vitality, Might, Focus, Toughness, Swiftness, Gatherer (pickup range), Insight (XP), Fortune (gold), Tumbler (start
  every fight with the roll), Head Start (free level-up picks at the start), Second Wind (revive once per fight) and
  Second Thoughts (extra rerolls). Rank *r* costs *r* skill points.
- **Bramble** sells four items for gold, priced by rarity (`Adventure.price`); the stock changes every time you clear
  a challenge.
- The top of the screen names the area you're in and what to do next (`Story.objective`); the radar in the corner
  shows the places you've been, the people (blue; the trainer and merchant in gold), unopened chests (orange) and the
  gates (purple).

### The story so far (Chapter 1)

The stars fell, and three nights later a sickness crept out of the Hollow north of Mossbrook: the **Blight**. Thorns
that walk, flowers that bite, growing from nests. Elder Rowan asks you to go into the Hollow and tear the nests out.
Clear it and the camp thanks you with a gift (an epic ring, gold and skill points) and a warning: the Blight didn't
start in the forest. The road south leads to the city, and that's where the story continues next.

## Challenges

A challenge (`Challenge`) is a fight you take on from the world: someone asks for help, and its **gate** (a swirl of
light, knotted with thorns until you've been asked) takes you to a battlefield. Before you go in, a card shows what's
waiting: how many nests, whether there's a guardian, the danger at the start, and the reward.

| Challenge | From | Nests | Guardian | Starting danger | First clear | After that |
| --- | --- | --- | --- | --- | --- | --- |
| **The Blighted Hollow** | Elder Rowan | 4 | the Blighted Guardian | Easy (a step above the dens) | 160 gold, 5 skill points | 70 gold, 1 skill point |
| **Fox Den Raid** | Hunter Fenn | 3 | none | Easy | 70 gold, 2 skill points | 45 gold |

Challenges can be fought again. Each clear is a **loop**: the next attempt starts more dangerous
(`Challenge.LOOP_DANGER`), its guardian is tougher, and it pays a little more.

## Fights

A fight (`Run`) happens on a **battlefield** made fresh every time (`Battlefield.make`): a big irregular stretch of
ground grown cell by cell on a 7x7 grid, so it has open stretches, narrow necks, loops and dead ends, with trees and
rocks to weave round. You start in a cell on its edge.

- **You start from nothing:** level 1, only your sword. Your **equipment** and Ash's **masteries** come with you.
  Every monster drops an XP gem; each level-up pauses the action and offers three cards (`Perk.offer`).
  Your level and cards are gone when the fight ends; the next one starts from nothing again.
- **The goal: destroy the nests.** The Blight's nests (`Enemy.Type.NEST`) sit in the cells furthest from where you
  start and from each other, so clearing them means crossing the map. Arrows at the screen's edge point to the ones
  you can't see. A nest doesn't fight itself, but when you come near it springs a pack of guards and keeps sending
  more while you're close. A destroyed nest bursts into gems, gold and a heart.
- **The danger clock.** The horde never stops coming, and it gets tougher the longer you take: the HUD shows the
  clock and the danger (Easy, Medium, Hard, Very Hard, Insane, Impossible). Monster health, damage and numbers grow with
  it (`Run.threat`), and with your level. Elites arrive every 80 seconds (they drop a chest), swarms every 115.
- **Caches** stand in a few cells: press **E** to open one with the gold you've picked up (a free card and maybe an
  item). Each one costs more than the last, Risk of Rain style.
- **The guardian.** With the last nest down, the ground shakes, everything ordinary vanishes (leaving its gems), and
  the challenge's guardian arrives inside a red **ring** you can't leave until it's beaten. A challenge without one
  just opens the way home.
- **The way home:** a chest (gold and an item, always) and a portal back to the gate you came in by.
- **The end.** Win, lose or **Retreat** (from the pause menu), you keep the gold you picked up and every item you
  found. A win also pays the challenge's reward of gold and skill points (one more skill point if you reached level 12).

**Skills** fire by themselves, up to 5 of them, 5 ranks each: **Crescent Wave** (your sword swings send piercing
waves), **Fireball**, **Lightning**, **Ice Storm**, **Orbit Blades**, **Holy Aura**, and **Healing**. **Passives**, up
to 6: **Evasive Roll**, **Combo Extension**, **Blade Mastery**, **Quick Strikes**, **Long Reach**, **Vampiric Strikes**,
**Arcane Power**, **Quick Casting**, **Vitality**, **Fleet Foot**, **Magnetism**, **Wisdom**, **Iron Skin**,
**Regeneration** and **Precision**. A skill at rank 5 plus its partner passive can **evolve**: Crescent Wave + Quick
Strikes = *Moonlit Crescent*, Fireball + Arcane Power = *Inferno Comet*, Lightning + Quick Casting = *Storm Lord*, Ice
Storm + Iron Skin = *Absolute Zero*, Orbit Blades + Blade Mastery = *Blade Tempest*, Holy Aura + Vitality = *Sanctuary*.
Chests always lead with an evolution when one is ready.

**Crates** in a fight hold gold, hearts (heal 30%), magnets (pull in every gem on the map) and bombs (wipe out
ordinary monsters on screen).

## Air combos

The game is still drawn top-down, but combat has a height axis: a combo's **finisher** (the last hit of a chain) launches
whatever it connects with into the air, and you can keep juggling it there.

- **Launching.** Only the finisher hit calls `Enemy.launch()`; the regular hits earlier in a combo never do. Heavier
  enemies (a higher `Type.resist`, the same stat that already softens knockback) go up proportionally less — a Brute
  barely leaves the ground. Armored enemies (**the guardian**, and the nests) can't be launched at all, so a boss fight is never trivialised
  into a juggle.
- **Falling.** Gravity (`Enemy.GRAVITY`) pulls it back down; the sprite draws offset above its shadow, which stays on
  the ground the whole time, so you can always see where it's about to land.
- **Juggling.** Any hit that lands on an airborne enemy — your sword or a skill, it doesn't matter — gives it a small
  upward nudge (`Enemy.JUGGLE_VZ`), so a follow-up combo (or a Fireball) keeps it up rather than letting it drop.
  Damage numbers and impact sparks float up with it.
- **While it's airborne** an enemy has no AI at all (it can't move, attack, or wind anything up), and it doesn't push
  or get pushed by anything solid (other enemies, the player, trees, crates) — it's simply out of the way until it
  comes back down. It's still a completely valid target: lock-on, the sword and skills all work on it as normal.
- **Landing** plays a thump and a puff of dust, and leaves it staggered (briefly unable to act) before its AI resumes.

Tune the height and weight of it all via `Player.LAUNCH_VZ` (how hard a finisher launches), `Enemy.GRAVITY` (how fast
it falls) and `Enemy.JUGGLE_VZ` (how much each juggle hit adds) in `Player.java` / `Enemy.java`.

## Items and the Armory

Items come from chests (the world's, elites', the guardian's, and caches), from Bramble's stall, and from Rowan's gift.

- **Six slots:** weapon, helm, armor, gloves, boots, ring. Each slot has a main stat (melee damage, skill damage, max HP,
  attack speed, move speed, crit chance) plus more random lines — damage taken, regeneration, pickup range, XP gain,
  skill cooldowns, gold found.
- **Rarity:** Common, Uncommon, Rare, Epic, Legendary — more stat lines and bigger numbers. Each **Legendary** also has
  a power of its own: *Dawnbreaker* (fights start with Crescent Wave), *Crown of Insight* (+2 rerolls), *Phoenix Mail*
  (revive once per fight), *Tempest Gauntlets* (start with a 3-hit combo), *Windwalkers* (start with the roll), *Ring of
  Fortune* (4 cards per level-up).
- **The Armory** (the main menu, or **Equipment** on the pause menu): **Enter** equips or takes off, **U** spends gold
  to upgrade an item (+12% of its stats per level, up to +10), **X** twice salvages it for gold. The bag holds 60.

## Saving

The game saves itself whenever something changes in the world (talking, chests, training, buying), when you go into a
fight and come back out, on **Save & Quit**, and when the window closes. A fight in progress isn't saved: closing the
window mid-fight puts you back at its gate next time. Two files in `~/.spellblade` make up the save:
`adventure.properties` (the story, chests opened, challenges cleared, skill points, masteries, Bramble's stock, where you
stood) and `profile.properties` (gold and equipment). `-Dspellblade.home=<folder>` moves them. **New Game** over a save
asks for a second press, then starts fresh.

## Layout

| File | What it does |
| --- | --- |
| `World.java` | Game state and rules: the main menu, walking around the world (talking, chests, gates), the trainer's and merchant's screens, starting and ending fights, collisions, the pause menu, the Armory, saving. `World` has no drawing code |
| `Adventure.java` | The story so far (flags), chests opened, challenges cleared, skill points, masteries, Bramble's stock, where you stood; saving and loading it |
| `Story.java` | All the words: what everyone says as the story moves on, what the chests hold, the objective line, the intro and Rowan's gift |
| `Worlds.java` | The explorable world (`forest()`: the camp and the forest around it, its people, chests and gates) and the main menu's clearing |
| `Challenge.java` | The challenges: who asks, how many nests, the guardian, the danger, the rewards |
| `Battlefield.java` | Generates a challenge's battlefield: the shape, the nests, the scenery, the caches |
| `Run.java` | One fight: the director (spawning, the danger clock, elites, swarms, the nests' guards), drops, pickups and caches, the guardian's ring, level-up choices, and the end of the fight |
| `Mastery.java` | Ranger Ash's permanent upgrades |
| `Perk.java` / `Arsenal.java` | Everything a level-up can offer (skills, passives, evolutions) and the self-firing skills' numbers per rank |
| `Pickup.java` | Gems, gold, hearts, magnets, bombs, chests, caches and the portal |
| `Item.java` / `Profile.java` | Equipment (slots, rarities, stats, legendaries) and the profile (gold, the bag, what's worn) |
| `Level.java` | A map: rooms (each one or more rectangles), corridors, people, chests, gates, scenery, and walkable-area collision |
| `Player.java` | Movement, the combo, dash-through attacks, the roll |
| `Enemy.java` | Enemy types (`Type` holds the stats), their AI, the guardian, and the nests |
| `Renderer.java` | Draws a frame: the world, then the HUD and whichever screen is up, and the speech box |
| `WorldRenderer.java` | Draws the world itself: level, shadows, depth-sorted sprites, people, chests, gates, prompts, telegraphs, projectiles, effects, health bars |
| `LevelView.java` | The level's background: bakes floors, walls, props and scenery into cached image chunks, and glows |
| `WorldHud.java` | The world's HUD, pause menu, the trainer's and merchant's screens, and the card before a challenge |
| `RunHud.java` | A fight's HUD, the level-up cards, the fight's pause menu, the results screen, and the perk icons |
| `TitleScreen.java` / `ArmoryScreen.java` | The main menu (on its dusk backdrop) and the Armory |
| `MenuStyle.java` | The look the screens share: gold headings, gliding selection rows, key caps, glass cards, the serif font |
| `Minimap.java` | The round radar in the top-right corner |
| `Dialogue.java` | The speech box's rules: typewriter text, lines that wait for a key or are called out, the voice's chirps |
| `PixelCanvas.java` / `Sprite.java` / `Art.java` | The graphics engine: a pixel painting canvas, an anchored sprite, and the sprite atlas |
| `ImportedArt.java` / `res/art/` | The drawn (PixelLab) sprites and the poses made from them |
| `PeopleArt.java` / `TownArt.java` | Painted sprites for the hero (1.5x finer than the rest), the Hermit, Ash's and Bramble's stalls, Rowan, Fenn and Wren |
| `CreatureArt.java` / `FxArt.java` / `RunArt.java` / `BreakableArt.java` | Painted enemies (the fallback for the drawn ones), effects, pickups and item icons, crates |
| `Theme.java` / `ThemeArt.java` / `ForestProps.java` / `CityProps.java` / `LabProps.java` | The forest, city and laboratory looks: floor tiles, walls, colours, scenery |
| `Music*.java`, `Song*.java`, `Instruments.java`, `Sfx*.java`, `Audio*.java`, `Dsp.java`, `Snd.java` | The sound engine (below) |

## Graphics engine

Apart from the drawn art (see **Drawn art** below), everything you see is painted by `game/*Art.java` code into small pixel images (`PixelCanvas`), scaled up 3x with
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
- **Maps** are baked into 1024x1024 image chunks the first time you look at them, so drawing the map costs a few
  image copies per frame instead of thousands of tile draws. Only the props that animate, glows and everything that
  moves are drawn live.
- **Depth:** characters, people, chests and gates are sorted by their feet and each gets a ground shadow.
- **Lighting:** a colour wash for the mood (warm dappled light in the forest, blue dusk in the city), glows for
  lamps / neon / fire / lightning, and a vignette at the edges.
- **Themes:** `Level.theme` picks the art set. The forest has grass, flagstones, dirt paths, hedges and tree canopy;
  the city (asphalt, sidewalks, brick, rooftops) and the laboratory (pale tile, teal panel walls with hazard bands) are
  ready for the chapters to come. Enemies change with the theme too: toadstools, foxes, snap-blooms, stump golems,
  the Blight's nests and the Treant in the forest; rats, cats, drones, dumpsters and the Warden robot in the city; green
  oozes, wind-up mice, acid flasks, hulking green mutants and the Mad Scientist in the lab. The shade is the same in all three.

### Drawn art

The monsters and the props you fight among are drawn pixel art, made with PixelLab and loaded by `ImportedArt` from
`res/art`. Each PNG is a horizontal strip of frames. `res/art/anchors.properties` gives each strip's anchor (the feet,
in the frame's pixels) and its frame count. A strip replaces the painted sprite of the same name, with `_` for `.`
(`crate_forest.png` becomes `crate.forest`). Any sprite without a PNG keeps its painted look.

- **Finer pixels.** One pixel of drawn art is 2 world units (`ImportedArt.PIXEL`), against 3 for the painted art.
  `Sprite.k` holds that ratio, so code keeps drawing everything at `Art.SCALE` and both kinds come out the right size.
  An elite is drawn 1.5x so its pixels stay even.
- **The monsters** have one drawn pose each. Their walk (a squash and a bob) and their wind-up (rearing back) are made
  from it in code.
- **The bosses** have one drawn pose each too. Idle, walk, slam and burst are made from it, and so is the second
  phase under half health: the Treant's leaves turn autumn orange, the Warden runs red, and the Mad Scientist's
  coat and hair go a sickly green. A `<theme>_boss2.png` would replace that recolour.
- **Scenery:** the forest's oak, bush, boulder and stump (`landmark_*`), the Blight's nest (`forest_nest`, which
  pulses in code), the crates and barrels of all three themes, and the chests. The city and lab crates are recolours of the forest's.
- The images were cleaned up before they were added: baked-in drop shadows and stray specks removed, each cropped to
  its pixels, and the alley cat's all-black body lightened so it shows on the dark streets.

The hero, the floors, walls, effects, pickups, the forest's people and the city and lab scenery are still painted in code. The hero is
painted on a grid 1.5x finer than the other painted art (`PeopleArt.Fine`), so their pixels are the same size as the
monsters' around them.

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
  - `MusicDirector` picks the piece and mood from the game state: calm while exploring, the fight mood once a fight gets going,
    the boss piece for the guardian, silence after a defeat; pausing (and the world's screens) muffles and lowers the music.
- **Sound effects** (`Snd` lists them all): swings, hits, skills, enemy tells and attacks, deaths, boss events, nests,
  jingles, menu blips, footsteps (soft on forest grass, hard on the camp's paving and in the city). Each has several variants that are taken
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

- **The world:** areas, people, chests and gates are placed in `Worlds.forest()`; what everyone says, what the chests
  hold and the objectives are in `Story.java`.
- **Challenges:** nests, danger, rewards, the favoured monster and the battlefield's size are the arguments in
  `Challenge.java`; `Challenge.LOOP_DANGER` is what each clear adds.
- **Battlefield shape:** `Battlefield.GRID` / `CELL`, the passage widths and the scenery counts in `Battlefield.make`.
- **Fight difficulty:** `Run.threat` (the danger clock), `Run.hpMult` / `dmgMult` / `population`, `Run.pickType`,
  `ELITE_EVERY` / `SWARM_EVERY`, the guardian's multipliers in `Run.spawnBoss`; nests: `Enemy.Type.NEST` (health),
  `Run.NEST_RANGE` and the guard counts in `Run.updateNests`.
- **Caches:** prices in `Run.priceCaches`.
- **Masteries:** `Mastery.java` (effects in `applyAll`; costs in `cost`).
- **Bramble:** stock odds in `Adventure.restock`, prices in `Adventure.price`.
- **Level-up curve:** `Run.xpFor`; gem values in `Run.dropFor`.
- **Skills:** the per-rank arrays at the top of `Arsenal.java` (keep the text in `Perk.java` in step); passives' effects
  in `Perk.apply`.
- **Loot:** the rarity weights in `Run.openEliteChest` / `openBossChest` / `interact`; stat ranges in `Item.Stat`;
  upgrade and salvage prices in `Item.upgradeCost` / `salvageValue`.
- **Enemy stats:** the `Type` enum in `Enemy.java`; the guardian's attacks are `updateBoss()` / `fireBurst()`.
- **Combo timing and damage:** constants at the top of `Player.java`, plus `startAttack()` / `doHit()`.
- **Air combos:** `Player.LAUNCH_VZ` / `Enemy.GRAVITY` / `Enemy.JUGGLE_VZ`.
