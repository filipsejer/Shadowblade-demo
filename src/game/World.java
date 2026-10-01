package game;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** All game state and rules. No drawing here (see {@link Renderer}) and no window, so it can run headless. */
final class World {
    /**
     * TITLE is the main menu. PLAYING is either walking around an explorable world ({@link #run} null) or a fight
     * ({@link #run} set). TRAINER, SHOP and BRIEFING are the world's screens (Ranger Ash, Bramble's table, the card
     * before a challenge); LEVEL_UP is a fight's choice of perks, RESULTS its end; ARMORY the equipment screen.
     */
    enum State { TITLE, PLAYING, PAUSE, LEVEL_UP, RESULTS, ARMORY, TRAINER, SHOP, BRIEFING }

    final Random rng = new Random();

    State state = State.TITLE;
    Level level;
    double fade;                    // 1 -> 0 after travelling somewhere: a black screen fading in
    Player player;
    final List<Enemy> enemies = new ArrayList<>();
    final List<Projectile> projectiles = new ArrayList<>();
    final List<Zone> zones = new ArrayList<>();
    final List<Effect> effects = new ArrayList<>();

    /** A sound the game wants played. The sound engine turns these into audio (see {@link GameAudio}); the world just says what happened. */
    record Cue(Snd snd, double x, double y, boolean positional, double delay, double gain, double rate) {}

    private final List<Cue> sounds = new ArrayList<>();
    final List<Blast> blasts = new ArrayList<>();          // the mad scientist's flask bombs, waiting to burst
    private final List<Enemy> arrivals = new ArrayList<>();   // summoned mid-frame, added once the enemy loop is done
    /** Volume choices; they belong to the player, not to a game, so they survive everything. */
    AudioSettings audio = new AudioSettings();
    int pauseCursor;

    /** The fight in progress, or null (walking around the world, or the menus). */
    Run run;
    /** The story so far, or null on the main menu before a game is started or loaded. */
    Adventure adventure;
    /** Gold and equipment. Saved with the adventure. */
    Profile profile = new Profile();
    /** Set by QUIT on the main menu; the window closes itself when it sees it. */
    boolean quitRequested;
    /** The main menu's line about the saved game, or null when there's nothing to continue. */
    String savedGame;
    /** NEW GAME pressed once over an existing save: the next press confirms throwing it away. */
    boolean confirmNew;
    /** The Armory: which row of the bag is selected, a line of feedback, a salvage waiting for its confirming press, and where ESC goes back to. */
    int armoryCursor;
    String armoryMessage = "";
    boolean confirmSalvage;
    State armoryReturn = State.TITLE;
    /** A fight's pause menu: a retreat waiting for its confirming press. */
    boolean confirmRetreat;
    /** Counts down on the results screen, which ignores every key until it runs out. */
    double overTimer;
    static final double OVER_GUARD = 1.0;
    /** The trainer's and the merchant's screens: the selected row, and a line of feedback. */
    int trainerCursor, shopCursor;
    String screenMessage = "";
    /** The challenge whose briefing card is up. */
    Challenge briefing;

    Enemy lockTarget;
    double camX, camY;
    /** Where the camera looks instead of at the player (NaN = follow the player): the menus' backdrop. */
    double focusX = Double.NaN, focusY = Double.NaN;
    /** Which main menu row is highlighted. */
    int menuCursor;
    /** Somebody talking: a box at the bottom of the screen. */
    final Dialogue dialogue = new Dialogue();
    double shake;
    double hitStop;
    double time;
    int kills;
    /** The area of the world you're standing in (for its name across the screen when you walk into it). */
    private Level.Room area;

    String banner = "";
    double bannerTimer;
    /** A line of news over the bottom of the screen ("NEST DESTROYED"), with a smaller line under it. */
    String notice = "";
    String noticeHint = "";
    double noticeTimer;

    /** How close you must stand to talk to someone, open a chest or step through a gate. */
    static final double INTERACT_RANGE = 95;

    World() {
        profile = Profile.load(Profile.file());
        showTitle();
        fade = 1.6;                                   // the very first screen fades in from black
    }

    /** Clears everything that belongs to one place (enemies, effects...), ready for a new one. */
    private void clearField() {
        enemies.clear();
        arrivals.clear();
        blasts.clear();
        projectiles.clear();
        zones.clear();
        effects.clear();
        lockTarget = null;
        dialogue.clear();
        banner = notice = noticeHint = "";
        bannerTimer = noticeTimer = 0;
        shake = hitStop = 0;
    }

    // ------------------------------------------------------------------ sound

    /** Plays a sound with no place in the world (menus, jingles). */
    void sound(Snd s) { sound(s, 0); }

    void sound(Snd s, double delay) { queue(new Cue(s, 0, 0, false, delay, 1, 1)); }

    /** Plays a sound at a place in the world: louder and more central the closer it is to the player. */
    void soundAt(Snd s, double x, double y) { soundAt(s, x, y, 0, 1, 1); }

    void soundAt(Snd s, double x, double y, double delay, double gain, double rate) { queue(new Cue(s, x, y, true, delay, gain, rate)); }

    private void queue(Cue c) {
        sounds.add(c);
        if (sounds.size() > 256) sounds.remove(0);     // nobody is listening (a test, or no sound device): don't pile up
    }

    /** The cues since the last call, oldest first. */
    List<Cue> drainSounds() {
        List<Cue> out = new ArrayList<>(sounds);
        sounds.clear();
        return out;
    }

    /** The sounds queued and not yet drained (for tests). */
    List<Cue> pendingSounds() { return sounds; }

    boolean forest() { return level.theme == Theme.FOREST; }

    /** Picks the version of a sound that belongs to the current level's theme. */
    Snd themed(Snd forest, Snd city, Snd lab) {
        return switch (level.theme) { case FOREST -> forest; case CITY -> city; case LAB -> lab; };
    }

    // ------------------------------------------------------------------ update

    void update(double dt, Input in) {
        shake *= Math.exp(-9 * dt);
        overTimer = Math.max(0, overTimer - dt);
        fade = Math.max(0, fade - dt / 0.9);
        if (in.pressed(KeyEvent.VK_M)) audio.toggleMute();
        switch (state) {
            case TITLE -> { animateTitleScene(dt); updateMainMenu(in); }
            case PLAYING -> updatePlaying(dt, in);
            case PAUSE -> updatePause(in);
            case LEVEL_UP -> run.updateChoices(this, in, dt);
            case ARMORY -> { animateTitleScene(dt); updateArmory(in); }
            case TRAINER -> updateTrainer(in);
            case SHOP -> updateShop(in);
            case BRIEFING -> updateBriefing(in);
            case RESULTS -> {
                updateEffects(dt);
                if (overTimer <= 0 && (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_ESCAPE) || in.pressed(KeyEvent.VK_SPACE) || in.pressed(KeyEvent.VK_E))) {
                    in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_E);
                    returnFromFight();
                }
            }
        }
        double lookX = Double.isNaN(focusX) ? player.x : focusX, lookY = Double.isNaN(focusY) ? player.y : focusY;
        camX += (lookX - camX) * Math.min(1, 9 * dt);
        camY += (lookY - camY) * Math.min(1, 9 * dt);
    }

    // ------------------------------------------------------------------ the main menu

    /** The main menu's rows, top to bottom. */
    static final String[] MAIN_MENU = {"NEW GAME", "CONTINUE", "ARMORY", "QUIT"};

    /** W/S choose, ENTER picks. NEW GAME over an existing save asks for a second press first. */
    private void updateMainMenu(Input in) {
        int rows = MAIN_MENU.length;
        if (in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) { menuCursor = (menuCursor + 1) % rows; confirmNew = false; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) { menuCursor = (menuCursor + rows - 1) % rows; confirmNew = false; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_ESCAPE)) confirmNew = false;
        if (!(in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_SPACE))) return;
        in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        switch (menuCursor) {
            case 0 -> {
                if (savedGame != null && !confirmNew) {
                    confirmNew = true;
                    sound(Snd.MENU_DENY);
                } else {
                    newGame();
                }
            }
            case 1 -> {
                if (savedGame == null) sound(Snd.MENU_DENY);
                else continueGame();
            }
            case 2 -> openArmory(State.TITLE);
            default -> quitRequested = true;
        }
    }

    /** True while the menus' backdrop is up: the hero in a forest clearing with the horde circling (see {@link #showTitle}). */
    boolean titleScene;
    /** Each circling monster's orbit: start angle, radius, and speed (radians per second; negative goes the other way). */
    private final List<double[]> orbits = new ArrayList<>();

    /**
     * Back to the main menu, with its backdrop: the hero standing alone in a forest clearing at dusk while monsters
     * prowl round in slow circles. Nothing here fights; {@link #animateTitleScene} just walks them round.
     */
    void showTitle() {
        clearField();
        run = null;
        adventure = null;
        state = State.TITLE;
        confirmNew = confirmRetreat = confirmSalvage = false;
        savedGame = Adventure.describeSaved();
        menuCursor = savedGame != null ? 1 : 0;
        titleScene = true;
        time = 0;
        kills = 0;
        level = Worlds.clearing();
        player = new Player(level.spawnX, level.spawnY);
        player.facing = Math.PI / 2;                                  // facing the camera
        Random r = new Random(11);
        Enemy.Type[] kinds = {Enemy.Type.GRUNT, Enemy.Type.RUNNER, Enemy.Type.GRUNT, Enemy.Type.SHOOTER, Enemy.Type.GRUNT,
            Enemy.Type.RUNNER, Enemy.Type.BRUTE, Enemy.Type.GRUNT, Enemy.Type.RUNNER, Enemy.Type.GRUNT, Enemy.Type.SHOOTER, Enemy.Type.GRUNT};
        orbits.clear();
        for (int i = 0; i < kinds.length; i++) {
            boolean inner = i % 2 == 0;
            double a = i * Math.PI * 2 / kinds.length + r.nextDouble() * 0.3;
            double radius = (inner ? 165 : 235) + r.nextDouble() * 25;
            double speed = (inner ? 0.16 : -0.11) * (0.85 + r.nextDouble() * 0.3);
            Enemy e = new Enemy(kinds[i], player.x, player.y, 1, 1, r);
            e.spawnIn = 0;
            enemies.add(e);
            orbits.add(new double[]{a, radius, speed});
        }
        animateTitleScene(0);
        focusX = player.x - 150;                                      // the hero stands right of centre, clear of the menu
        focusY = player.y - 12;
        camX = focusX;
        camY = focusY;
    }

    /** Walks the backdrop's monsters round their circles (and keeps the clock running so everything animates). */
    private void animateTitleScene(double dt) {
        if (!titleScene) return;
        time += dt;
        for (int i = 0; i < enemies.size() && i < orbits.size(); i++) {
            Enemy e = enemies.get(i);
            double[] o = orbits.get(i);
            double a = o[0] + time * o[2];
            double nx = player.x + Math.cos(a) * o[1], ny = player.y + Math.sin(a) * o[1] * 0.62;
            if (Math.abs(nx - e.x) > 0.01) e.faceLeft = nx < e.x;
            e.x = nx;
            e.y = ny;
            e.state = Enemy.State.CHASE;
        }
    }

    /** A brand-new adventure: no gold, no gear, at the edge of Mossbrook. Throws away any saved game. */
    void newGame() {
        Adventure.delete();
        Run.delete(Profile.home().resolve("run.properties"));          // (a save from before the adventure, if there is one)
        profile = new Profile();
        adventure = new Adventure();
        adventure.restock();
        enterWorld();
        Story.intro(this);
        saveGame();
        sound(Snd.TITLE_START);
    }

    /** Picks the saved game back up, where you stood when it was saved. */
    void continueGame() {
        Adventure a = Adventure.load(Adventure.file());
        if (a == null) {
            savedGame = null;
            sound(Snd.MENU_DENY);
            return;
        }
        adventure = a;
        profile = Profile.load(Profile.file());
        enterWorld();
        notice = "WELCOME BACK";
        noticeHint = Story.objective(adventure);
        noticeTimer = 4;
        sound(Snd.TITLE_START);
    }

    /** Into the explorable world, wherever the adventure says you were standing (or the start). */
    private void enterWorld() {
        clearField();
        titleScene = false;
        focusX = focusY = Double.NaN;
        run = null;
        level = Worlds.forest();
        double x = Double.isNaN(adventure.x) ? level.spawnX : adventure.x, y = Double.isNaN(adventure.y) ? level.spawnY : adventure.y;
        Util.Vec v = level.clamp(x, y, 15);
        player = worldPlayer(v.x(), v.y());
        camX = player.x;
        camY = player.y;
        area = level.roomAt(player.x, player.y, 0);
        if (area != null) area.visited = true;
        banner = level.name;
        bannerTimer = 3;
        fade = 1;
        time = 0;
        state = State.PLAYING;
    }

    /** The hero as they walk around between fights: the sword and the roll, nothing more. */
    private static Player worldPlayer(double x, double y) {
        Player p = new Player(x, y);
        p.rollUnlocked = true;
        return p;
    }

    /** Writes the game: where you stand (the gate you went through, during a fight), the story, gold and gear. */
    void saveGame() {
        if (adventure == null) return;
        if (run == null && level != null && level.explorable && player != null) {
            adventure.x = player.x;
            adventure.y = player.y;
        }
        adventure.save(Adventure.file());
        profile.save(Profile.file());
    }

    // ------------------------------------------------------------------ playing

    private void updatePlaying(double dt, Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.PAUSE;
            pauseCursor = 0;
            confirmRetreat = false;
            sound(Snd.PAUSE_IN);
            return;
        }
        if (run != null) {
            if (in.pressed(KeyEvent.VK_TAB)) {
                cycleLock();
                if (lockTarget != null) sound(Snd.LOCK_ON);
            }
            if (in.pressed(KeyEvent.VK_Q)) {
                if (lockTarget != null) sound(Snd.LOCK_OFF);
                lockTarget = null;
            }
            if (in.pressed(KeyEvent.VK_E)) run.interact(this);
        } else {
            dialogue.update(this, in, dt);                                // somebody talking
            if (dialogue.stopsWorld()) {
                updateEffects(dt);
                return;
            }
            if (in.pressed(KeyEvent.VK_E) && interact()) return;
        }
        if (hitStop > 0) {           // brief freeze on impact
            hitStop -= dt;
            in.carryOver(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, KeyEvent.VK_J);   // presses made during the freeze are kept for when it ends
            return;
        }

        time += dt;
        bannerTimer = Math.max(0, bannerTimer - dt);
        noticeTimer = Math.max(0, noticeTimer - dt);
        player.update(this, in, dt);
        for (Enemy e : enemies) e.update(this, dt);
        enemies.addAll(arrivals);                       // creatures the boss summoned this frame join the fight now that the loop is over
        arrivals.clear();
        resolveCollisions();
        updateProjectiles(dt);
        updateBlasts(dt);
        updateZones(dt);
        updateEffects(dt);
        collectDead();
        if (run == null) {
            noticeArea();
            return;
        }
        run.update(this, dt);
        if (state != State.PLAYING) return;                  // the fight ended (through the portal home)
        if (player.hp <= 0 && run.tryRevive(this)) return;
        if (player.hp <= 0) {
            for (int i = 0; i < 24; i++) {
                effects.add(Effect.spark(player.x, player.y, rng.nextDouble() * Math.PI * 2, 80 + rng.nextDouble() * 260, 4, 0.8, Player.COLOR));
            }
            run.finish(this, Run.Outcome.DEFEAT);
            return;
        }
        run.maybeOpenChoices(this);
    }

    /** Walking into a new area of the world: its name across the screen, and the minimap learns it. */
    private void noticeArea() {
        Level.Room here = level.roomAt(player.x, player.y, 40);
        if (here == null || here == area) return;
        area = here;
        if (!here.visited) {
            banner = here.name;
            bannerTimer = 2.2;
        }
        here.visited = true;
    }

    /** The area of the world you're in (for the HUD), or null in a fight. */
    Level.Room area() { return run == null ? area : null; }

    private void updateEffects(double dt) {
        effects.removeIf(e -> !e.update(dt));
    }

    // ------------------------------------------------------------------ the explorable world: people, chests, gates

    /** E in the world: talk to whoever's there, open the chest, or step up to the gate. True if something happened. */
    private boolean interact() {
        Level.Npc npc = npcNearby();
        if (npc != null) {
            player.facing = Util.angleTo(player.x, player.y, npc.x(), npc.y());
            dialogue.clearShouts();                                   // a line called out on the way is cut short
            Story.talk(this, npc);
            saveGame();
            return true;
        }
        Level.Treasure t = treasureNearby();
        if (t != null) {
            openTreasure(t);
            return true;
        }
        Level.Gate g = gateNearby();
        if (g != null) {
            if (!gateOpen(g.challenge())) {
                dialogue.say("YOU", "hero.down.idle", Snd.TOWN_TALK, g.challenge() == Challenge.HOLLOW
                    ? "Thorns as thick as my arm, knotted across the way, and something beating behind them. Someone in the camp must know what this is."
                    : "Fresh tracks, and a sour smell on the wind. Whoever hunts around here would know what's going on.");
                return true;
            }
            briefing = g.challenge();
            state = State.BRIEFING;
            sound(Snd.MENU_OPEN);
            return true;
        }
        return false;
    }

    /** Whether you've been asked to take a challenge on yet (its gate stays thorny until then). */
    boolean gateOpen(Challenge c) {
        return adventure != null && (c == Challenge.HOLLOW ? adventure.has("quest.hollow") : adventure.has("quest.dens"));
    }

    Level.Npc npcNearby() {
        if (run != null || dialogue.stopsWorld()) return null;
        for (Level.Npc n : level.npcs) if (Util.dist(player.x, player.y, n.x(), n.y()) <= INTERACT_RANGE) return n;
        return null;
    }

    Level.Treasure treasureNearby() {
        if (run != null || adventure == null) return null;
        for (Level.Treasure t : level.treasures) {
            if (!adventure.opened.contains(t.id()) && Util.dist(player.x, player.y, t.x(), t.y()) <= INTERACT_RANGE) return t;
        }
        return null;
    }

    Level.Gate gateNearby() {
        if (run != null) return null;
        for (Level.Gate g : level.gates) if (Util.dist(player.x, player.y, g.x(), g.y()) <= INTERACT_RANGE + 20) return g;
        return null;
    }

    private void openTreasure(Level.Treasure t) {
        adventure.opened.add(t.id());
        Story.Find f = Story.treasure(t.id());
        StringBuilder got = new StringBuilder();
        if (f.gold() > 0) {
            profile.gold += f.gold();
            got.append("+").append(f.gold()).append(" gold");
        }
        if (f.rarity() != null) {
            Item it = Item.roll(rng, f.rarity(), 0);
            profile.add(List.of(it));
            if (got.length() > 0) got.append(MenuStyle.DOT);
            got.append(it.name).append(" (").append(it.rarity.label.toLowerCase()).append(")");
        }
        if (f.skillPoints() > 0) {
            adventure.skillPoints += f.skillPoints();
            if (got.length() > 0) got.append(MenuStyle.DOT);
            got.append("+").append(f.skillPoints()).append(" skill point").append(f.skillPoints() == 1 ? "" : "s");
        }
        notice = got.toString().toUpperCase();
        noticeHint = f.note();
        noticeTimer = 5;
        sound(Snd.CHEST_OPEN);
        Color c = t.big() ? new Color(255, 150, 60) : new Color(255, 214, 90);
        effects.add(Effect.ring(t.x(), t.y(), 10, 110, 0.5, c, true));
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14;
            effects.add(Effect.particle(t.x(), t.y() - 20, Math.cos(a) * 170, Math.sin(a) * 170 - 60, 0.8, "fx.star", -1, 0.03, 30, 3));
        }
        saveGame();
    }

    // ------------------------------------------------------------------ the trainer and the merchant

    void openTrainer() {
        trainerCursor = 0;
        screenMessage = "";
        state = State.TRAINER;
        sound(Snd.MENU_OPEN);
    }

    void openShop() {
        shopCursor = 0;
        screenMessage = "";
        state = State.SHOP;
        sound(Snd.MENU_OPEN);
    }

    /** W / S choose a mastery, ENTER learns its next rank, ESC leaves. */
    private void updateTrainer(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) { state = State.PLAYING; sound(Snd.MENU_BACK); return; }
        int rows = Mastery.values().length;
        if (in.pressed(KeyEvent.VK_S) || in.pressed(KeyEvent.VK_DOWN)) { trainerCursor = (trainerCursor + 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_W) || in.pressed(KeyEvent.VK_UP)) { trainerCursor = (trainerCursor + rows - 1) % rows; sound(Snd.MENU_MOVE); }
        if (!(in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E))) return;
        in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_E);
        Mastery m = Mastery.values()[trainerCursor];
        int now = adventure.rank(m);
        if (now >= m.maxRank) {
            screenMessage = m.label + " is already mastered.";
            sound(Snd.MENU_DENY);
        } else if (adventure.skillPoints < m.cost(now)) {
            screenMessage = "Not enough skill points: " + m.label + " costs " + m.cost(now) + ".";
            sound(Snd.MENU_DENY);
        } else {
            adventure.skillPoints -= m.cost(now);
            adventure.mastery[m.ordinal()]++;
            screenMessage = "Learned " + m.label + (m.maxRank > 1 ? " " + adventure.rank(m) : "") + "!";
            sound(Snd.LEVEL_UP);
            saveGame();
        }
    }

    /** W / S choose an item, ENTER buys it, ESC leaves. */
    private void updateShop(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) { state = State.PLAYING; sound(Snd.MENU_BACK); return; }
        List<Item> stock = adventure.stock;
        if (stock.isEmpty()) return;
        int rows = stock.size();
        shopCursor = Math.min(shopCursor, rows - 1);
        if (in.pressed(KeyEvent.VK_S) || in.pressed(KeyEvent.VK_DOWN)) { shopCursor = (shopCursor + 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_W) || in.pressed(KeyEvent.VK_UP)) { shopCursor = (shopCursor + rows - 1) % rows; sound(Snd.MENU_MOVE); }
        if (!(in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E))) return;
        in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_E);
        Item it = stock.get(shopCursor);
        int price = Adventure.price(it);
        if (profile.gold < price) {
            screenMessage = "Not enough gold: " + it.name + " costs " + price + ".";
            sound(Snd.MENU_DENY);
            return;
        }
        profile.gold -= price;
        profile.add(List.of(it));
        stock.remove(it);
        shopCursor = Math.max(0, Math.min(shopCursor, stock.size() - 1));
        screenMessage = "Bought " + it.name + ". Wear it from the Equipment screen (ESC).";
        sound(Snd.COIN_PICKUP);
        saveGame();
    }

    /** The card before a challenge: ENTER (or E) goes in, ESC steps back. */
    private void updateBriefing(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) { state = State.PLAYING; sound(Snd.MENU_BACK); return; }
        if (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E)) {
            in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_E);
            startChallenge(briefing);
        }
    }

    // ------------------------------------------------------------------ fights

    /** Through the gate: a battlefield made fresh, a fighter made from your gear and masteries. */
    void startChallenge(Challenge c) {
        Level.Gate gate = null;
        for (Level.Gate g : level.gates) if (g.challenge() == c) gate = g;
        if (gate != null) {                                            // you'll come back out standing just in front of it
            adventure.x = gate.x();
            adventure.y = gate.y() + 110;
        }
        adventure.fights++;
        saveGame();
        clearField();
        sound(Snd.TRAVEL);
        level = Battlefield.make(c, System.nanoTime());
        run = Run.start(this, c);
        state = State.PLAYING;
    }

    /** The results have been seen: back out of the gate into the world. */
    private void returnFromFight() {
        Run done = run;
        enterWorld();
        if (done != null) Story.afterFight(this, done.challenge, done.outcome == Run.Outcome.VICTORY);
        saveGame();
        sound(Snd.MENU_BACK);
    }

    // ------------------------------------------------------------------ the pause menu and the Armory

    /** The pause menu's rows: in the world, and in a fight. */
    static final String[] WORLD_PAUSE = {"RESUME", "EQUIPMENT", "MUSIC", "EFFECTS", "SAVE & QUIT"};
    static final String[] FIGHT_PAUSE = {"RESUME", "MUSIC", "EFFECTS", "RETREAT"};

    String[] pauseRows() { return run != null ? FIGHT_PAUSE : WORLD_PAUSE; }

    /** W/S choose; LEFT/RIGHT change a volume; ENTER does the rest (a retreat needs a second press). ESC resumes. */
    private void updatePause(Input in) {
        String[] rows = pauseRows();
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.PLAYING;
            confirmRetreat = false;
            sound(Snd.PAUSE_OUT);
            return;
        }
        if (in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) { pauseCursor = (pauseCursor + 1) % rows.length; confirmRetreat = false; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) { pauseCursor = (pauseCursor + rows.length - 1) % rows.length; confirmRetreat = false; sound(Snd.MENU_MOVE); }
        String row = rows[Math.min(pauseCursor, rows.length - 1)];
        int delta = (in.pressed(KeyEvent.VK_RIGHT) || in.pressed(KeyEvent.VK_D) ? 1 : 0) - (in.pressed(KeyEvent.VK_LEFT) || in.pressed(KeyEvent.VK_A) ? 1 : 0);
        if (delta != 0 && row.equals("MUSIC") && audio.adjustMusic(delta)) sound(Snd.MENU_MOVE);
        if (delta != 0 && row.equals("EFFECTS") && audio.adjustSfx(delta)) sound(Snd.MENU_MOVE);
        if (!in.pressed(KeyEvent.VK_ENTER)) return;
        in.consume(KeyEvent.VK_ENTER);
        switch (row) {
            case "RESUME" -> { state = State.PLAYING; sound(Snd.PAUSE_OUT); }
            case "EQUIPMENT" -> openArmory(State.PAUSE);
            case "SAVE & QUIT" -> {
                saveGame();
                showTitle();
                sound(Snd.MENU_SELECT);
            }
            case "RETREAT" -> {
                if (!confirmRetreat) { confirmRetreat = true; sound(Snd.MENU_DENY); }
                else run.finish(this, Run.Outcome.RETREAT);
            }
            default -> { }
        }
    }

    private void openArmory(State back) {
        armoryReturn = back;
        armoryCursor = 0;
        armoryMessage = "";
        confirmSalvage = false;
        state = State.ARMORY;
        sound(Snd.MENU_OPEN);
    }

    /** The Armory: W/S choose an item, ENTER wears (or takes off) it, U upgrades it with gold, X (twice) salvages it, ESC leaves. */
    private void updateArmory(Input in) {
        List<Item> bag = profile.sorted();
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            if (adventure != null) saveGame();
            else profile.save(Profile.file());
            state = armoryReturn;
            sound(Snd.MENU_BACK);
            return;
        }
        if (bag.isEmpty()) return;
        int rows = bag.size();
        armoryCursor = Math.min(armoryCursor, rows - 1);
        if (in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) { armoryCursor = (armoryCursor + 1) % rows; confirmSalvage = false; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) { armoryCursor = (armoryCursor + rows - 1) % rows; confirmSalvage = false; sound(Snd.MENU_MOVE); }
        Item it = bag.get(armoryCursor);
        if (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E)) {
            boolean was = profile.isEquipped(it);
            profile.toggleEquip(it);
            armoryMessage = (was ? "Took off " : "Equipped ") + it.name + ".";
            sound(Snd.MENU_SELECT);
            armoryCursor = profile.sorted().indexOf(it);
        }
        if (in.pressed(KeyEvent.VK_U)) {
            if (it.upgrade >= Item.MAX_UPGRADE) { armoryMessage = it.name + " is fully upgraded."; sound(Snd.MENU_DENY); }
            else if (!profile.upgrade(it)) { armoryMessage = "Not enough gold: the next upgrade costs " + it.upgradeCost() + "."; sound(Snd.MENU_DENY); }
            else { armoryMessage = it.name + " upgraded to +" + it.upgrade + "."; sound(Snd.LEVEL_UP); }
        }
        if (in.pressed(KeyEvent.VK_X)) {
            if (!confirmSalvage) {
                confirmSalvage = true;
                armoryMessage = "Press X again to salvage " + it.name + " for " + it.salvageValue() + " gold.";
                sound(Snd.MENU_DENY);
            } else {
                confirmSalvage = false;
                profile.salvage(it);
                armoryMessage = "Salvaged " + it.name + " for " + it.salvageValue() + " gold.";
                sound(Snd.COIN_PICKUP);
                armoryCursor = Math.max(0, Math.min(armoryCursor, profile.items.size() - 1));
            }
        }
    }

    /** Saves the game when the window closes. */
    void saveIfAny() {
        if (adventure != null) saveGame();
    }

    // ------------------------------------------------------------------ collisions

    private void resolveCollisions() {
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (a.intangible() || b.intangible() || a.airborne() || b.airborne()) continue;   // floating above everything, they don't jostle or get jostled
                double dx = b.x - a.x, dy = b.y - a.y;
                double d = Math.hypot(dx, dy);
                double min = a.radius + b.radius;
                if (d >= min) continue;
                if (d < 0.001) { dx = rng.nextDouble() - 0.5; dy = rng.nextDouble() - 0.5; d = Math.hypot(dx, dy); }
                double ux = dx / d, uy = dy / d;
                double wa = a.rooted() ? 0 : b.rooted() ? 1 : 0.5;          // a nest doesn't budge: whoever bumps it moves all the way
                double push = (min - d) * 0.6;
                a.x -= ux * push * wa;
                a.y -= uy * push * wa;
                b.x += ux * push * (1 - wa);
                b.y += uy * push * (1 - wa);
            }
        }
        if (!player.dodging() && !player.sliding()) {   // rolling and attack-dashes pass through enemies
            for (Enemy e : enemies) {
                if (e.intangible() || e.airborne()) continue;
                double dx = e.x - player.x, dy = e.y - player.y;
                double d = Math.hypot(dx, dy);
                double min = e.radius + player.radius;
                if (d >= min || d < 0.001) continue;
                double overlap = min - d;
                double ux = dx / d, uy = dy / d;
                double share = e.rooted() ? 0 : 0.7;
                e.x += ux * overlap * share;
                e.y += uy * overlap * share;
                player.x -= ux * overlap * (1 - share);
                player.y -= uy * overlap * (1 - share);
            }
        }
        for (Level.Landmark l : level.landmarks) {                 // a tree trunk is solid
            if (l.radius() <= 0) continue;
            Util.Vec out = around(l.x(), l.y() - 12, l.radius(), player.x, player.y, player.radius, player.x - player.lastX, player.y - player.lastY);   // (the trunk's base, as far as a body's centre is concerned)
            if (out != null) { player.x = out.x(); player.y = out.y(); }
            for (Enemy e : enemies) {
                if (e.airborne()) continue;                        // clear over the top of a tree trunk or a crate while it's up there
                out = around(l.x(), l.y() - 12, l.radius(), e.x, e.y, e.radius, e.x - e.lastX, e.y - e.lastY);
                if (out != null) { e.x = out.x(); e.y = out.y(); }
            }
        }
        for (Breakable c : level.breakables) {                     // crates and barrels are solid until they are smashed
            if (c.broken) continue;
            Util.Vec out = around(c.x, c.y - 8, c.radius, player.x, player.y, player.radius, player.x - player.lastX, player.y - player.lastY);
            if (out != null) { player.x = out.x(); player.y = out.y(); }
            for (Enemy e : enemies) {
                if (e.airborne()) continue;
                out = around(c.x, c.y - 8, c.radius, e.x, e.y, e.radius, e.x - e.lastX, e.y - e.lastY);
                if (out != null) { e.x = out.x(); e.y = out.y(); }
            }
        }
        for (Level.Npc npc : level.npcs) {                         // people (and their stalls) are solid too
            boolean stall = npc.role() != Level.Role.TALK;
            Util.Vec out = around(npc.x(), npc.y() + (stall ? 20 : 8), stall ? 46 : 16, player.x, player.y, player.radius, player.x - player.lastX, player.y - player.lastY);
            if (out != null) { player.x = out.x(); player.y = out.y(); }
        }
        for (Rectangle2D.Double g : level.grassPatches) {           // solid ground you can't walk on, e.g. a flower bed
            Util.Vec out = aroundRect(g, player.x, player.y, player.radius);
            if (out != null) { player.x = out.x(); player.y = out.y(); }
            for (Enemy e : enemies) {
                out = aroundRect(g, e.x, e.y, e.radius);
                if (out != null) { e.x = out.x(); e.y = out.y(); }
            }
        }
        for (Enemy e : enemies) {
            Util.Vec v = level.clamp(e.x, e.y, e.radius);
            e.x = v.x();
            e.y = v.y();
        }
        Util.Vec v = level.clamp(player.x, player.y, player.radius);
        player.x = v.x();
        player.y = v.y();
        if (run != null) run.confine(this);                        // a boss fight's ring
    }

    /**
     * Where a body of radius r at (x, y) ends up when it is pushed out of a solid circle at (cx, cy), or null if it is already clear of it.
     * (mx, my) is how far the body moved this frame. The push is straight away from the middle, so any sideways part of that movement carries
     * on (it slides round); and a body heading dead into the middle is drifted to whichever side of its line of travel it is already on,
     * so it clears the obstacle instead of standing there stuck against it.
     */
    private static Util.Vec around(double cx, double cy, double cr, double x, double y, double r, double mx, double my) {
        double dx = x - cx, dy = y - cy, d = Math.hypot(dx, dy), min = r + cr;
        if (d >= min) return null;
        if (d < 0.001) { dx = 0; dy = 1; d = 1; }
        double px = cx + dx / d * min, py = cy + dy / d * min;
        double len = Math.hypot(mx, my);
        if (len > 0.01 && len < 30) {                                // (a huge jump is a teleport, not a walk)
            double perpX = -my / len, perpY = mx / len;
            double side = perpX * dx + perpY * dy >= 0 ? 1 : -1;
            double nudge = Math.min(2.5, (min - d) * 0.8);
            px += perpX * side * nudge;
            py += perpY * side * nudge;
        }
        return new Util.Vec(px, py);
    }

    /** Where a body of radius r at (x, y) ends up when it is pushed out of a solid rectangle, or null if it is already clear of it. */
    private static Util.Vec aroundRect(Rectangle2D.Double rect, double x, double y, double r) {
        double minX = rect.x - r, maxX = rect.getMaxX() + r, minY = rect.y - r, maxY = rect.getMaxY() + r;
        if (x <= minX || x >= maxX || y <= minY || y >= maxY) return null;
        double left = x - minX, right = maxX - x, top = y - minY, bottom = maxY - y;
        double m = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (m == left) return new Util.Vec(minX, y);
        if (m == right) return new Util.Vec(maxX, y);
        if (m == top) return new Util.Vec(x, minY);
        return new Util.Vec(x, maxY);
    }

    // ------------------------------------------------------------------ crates

    /** Breaks a crate or barrel: splinters, a puff, a sound, and what was inside. */
    void smash(Breakable b) {
        if (b.broken) return;
        b.broken = true;
        boolean crate = b.kind == Breakable.Kind.CRATE;
        soundAt(crate ? Snd.CRATE_SMASH : Snd.BARREL_SMASH, b.x, b.y);
        String bits = switch (level.theme) { case FOREST -> "fx.plank"; case CITY -> "fx.scrap"; case LAB -> crate ? "fx.scrap" : "fx.goo"; };
        effects.add(Effect.particle(b.x, b.y - 14, 0, 0, 0.42, "fx.puff", -1, 1, 0, 4));
        for (int i = 0; i < 9; i++) {
            double a = rng.nextDouble() * Math.PI * 2, sp = 80 + rng.nextDouble() * 170;
            effects.add(Effect.particle(b.x, b.y - 14, Math.cos(a) * sp, Math.sin(a) * sp - 90, 0.6 + rng.nextDouble() * 0.4, bits, rng.nextInt(3), 0.05, -110, 3));
        }
        shake = Math.max(shake, 2);
        if (run != null) {                                          // in a fight, crates hold pickups
            run.onSmash(this, b);
            return;
        }
        int g = 3 + rng.nextInt(6);                                 // in the world, a few coins
        profile.gold += g;
        effects.add(Effect.text(b.x, b.y - 46, "+" + g + " gold", new Color(255, 214, 80), false));
        sound(Snd.COIN_PICKUP, 0.1);
    }

    /** Breaks every crate and barrel whose centre is within {@code radius} of the point (blasts and bolts). */
    void smashNear(double x, double y, double radius) {
        for (Breakable b : level.breakables) if (!b.broken && Util.dist(x, y, b.x, b.y) <= radius + b.radius) smash(b);
    }

    // ------------------------------------------------------------------ attacks in flight

    private void updateProjectiles(double dt) {
        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext(); ) {
            Projectile p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            boolean outside = !level.contains(p.x, p.y);   // hit a wall
            boolean remove = false;

            if (p.friendly && p.wave) {                                // Crescent Wave: cuts through enemies instead of exploding
                double ang = Math.atan2(p.vy, p.vx);
                for (Enemy e : enemies) {
                    if (!e.targetable() || p.struck.contains(e)) continue;
                    if (Util.dist(p.x, p.y, e.x, e.y - e.z) > p.radius + e.radius) continue;
                    p.struck.add(e);
                    e.hurt(this, p.damage * (0.92 + rng.nextDouble() * 0.16), Math.cos(ang) * 150, Math.sin(ang) * 150, 0.15, WAVE_TEXT, false, false);
                    soundAt(Snd.HIT_LIGHT, e.x, e.y, 0, 0.6, 1.2);
                    if (p.struck.size() >= p.pierce) { remove = true; break; }
                }
                smashNear(p.x, p.y, p.radius * 0.5);
                if (outside || p.life <= 0) remove = true;
            } else if (p.friendly) {
                if (rng.nextInt(2) == 0) {
                    effects.add(Effect.spark(p.x, p.y, rng.nextDouble() * Math.PI * 2, 30, 4, 0.25, new Color(255, 190, 60)));
                }
                for (Enemy e : enemies) {
                    if (e.targetable() && Util.dist(p.x, p.y, e.x, e.y) < p.radius + e.radius) { remove = true; break; }
                }
                if (remove || outside || p.life <= 0) {
                    explode(p);
                    remove = true;
                }
            } else {
                if (player.invuln <= 0 && Util.dist(p.x, p.y, player.x, player.y) < p.radius + player.radius) {
                    player.hurt(this, p.damage, p.x - p.vx, p.y - p.vy);
                    remove = true;
                }
                if (outside || p.life <= 0) remove = true;
            }
            if (remove) it.remove();
        }
    }

    private static final Color WAVE_TEXT = new Color(190, 220, 255);
    static final Color FIRE = new Color(255, 140, 30);

    private void explode(Projectile p) {
        effects.add(Effect.explosion(p.x, p.y, p.aoe));
        soundAt(Snd.FIRE_EXPLODE, p.x, p.y, 0, 1, 1.22 - 0.3 * Util.clamp(p.aoe / 90.0, 0, 1));   // a bigger blast is deeper
        for (int i = 0; i < 8; i++) {
            effects.add(Effect.spark(p.x, p.y, rng.nextDouble() * Math.PI * 2, 80 + rng.nextDouble() * 200, 4, 0.35, FIRE));
        }
        smashNear(p.x, p.y, p.aoe);
        for (Enemy e : enemies) {
            if (!e.targetable()) continue;
            double d = Util.dist(p.x, p.y, e.x, e.y);
            if (d > p.aoe + e.radius) continue;
            double ang = Util.angleTo(p.x, p.y, e.x, e.y);
            e.hurt(this, p.damage, Math.cos(ang) * 200, Math.sin(ang) * 200, 0.2, FIRE);
            if (p.burnDps > 0) e.ignite(p.burnDps, 3.0);
        }
        shake = Math.max(shake, 3);
    }

    /** Counts the flask bombs down; a burst one hurts the player if they are standing in it. */
    private void updateBlasts(double dt) {
        for (Iterator<Blast> it = blasts.iterator(); it.hasNext(); ) {
            Blast b = it.next();
            if (!b.burst) {
                b.delay -= dt;
                if (b.delay > 0) continue;
                b.burst = true;
                effects.add(Effect.ring(b.x, b.y, 10, b.radius, 0.35, new Color(120, 255, 150), true));
                for (int i = 0; i < 9; i++) {
                    double a = rng.nextDouble() * Math.PI * 2, sp = 60 + rng.nextDouble() * 150;
                    effects.add(Effect.particle(b.x, b.y, Math.cos(a) * sp, Math.sin(a) * sp - 50, 0.6 + rng.nextDouble() * 0.3, "fx.goo", rng.nextInt(3), 0.05, -50, 3));
                }
                shake = Math.max(shake, 4);
                soundAt(Snd.BOSS_BOMB_LAB, b.x, b.y);
                if (Util.dist(b.x, b.y, player.x, player.y) < b.radius + player.radius * 0.5) player.hurt(this, b.damage, b.x, b.y);
            } else {
                b.after -= dt;
                if (b.after <= 0) it.remove();
            }
        }
    }

    private void updateZones(double dt) {
        for (Iterator<Zone> it = zones.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            z.life -= dt;
            z.tick -= dt;
            if (z.tick <= 0) {
                z.tick += Zone.TICK;
                boolean any = false;
                for (Enemy e : enemies) {
                    if (!e.targetable() || Util.dist(z.x, z.y, e.x, e.y) > z.radius + e.radius * 0.5) continue;
                    e.hurt(this, z.damage, 0, 0, 0, Perk.ICE_STORM.color);
                    e.slow(z.slowMul, 1.0);
                    any = true;
                }
                if (any) soundAt(Snd.ICE_TICK, z.x, z.y);
            }
            if (z.life <= 0) it.remove();
        }
    }

    private void collectDead() {
        for (Iterator<Enemy> it = enemies.iterator(); it.hasNext(); ) {
            Enemy e = it.next();
            if (e.hp > 0) continue;
            if (e.type == Enemy.Type.BOSS && !e.phase2 && level.theme == Theme.LAB) {
                e.enterFinalStage(this);        // the final boss: this wasn't the killing blow, it was the end of stage one
                continue;
            }
            it.remove();
            if (e == lockTarget) lockTarget = null;
            kills++;
            deathSound(e);
            deathBurst(e);
            if (run != null) run.onKill(this, e);
            if (e.type == Enemy.Type.BOSS) {
                for (Enemy o : enemies) if (o.summoned) o.hp = 0;                                  // his creations collapse with him
                blasts.clear();
            }
        }
    }

    private void deathSound(Enemy e) {
        if (e.type == Enemy.Type.BOSS || e.type == Enemy.Type.NEST) soundAt(Snd.BOSS_DIE, e.x, e.y);
        else if (e.radius > 20) soundAt(themed(Snd.DIE_BIG_FOREST, Snd.DIE_BIG_CITY, Snd.DIE_BIG_LAB), e.x, e.y);
        else soundAt(themed(Snd.DIE_FOREST, Snd.DIE_CITY, Snd.DIE_LAB), e.x, e.y);
    }

    /** A puff of smoke and a scatter of bits: leaves in the forest, scrap in the city. */
    private void deathBurst(Enemy e) {
        boolean big = e.radius > 20;
        double ey = e.y - e.radius * 0.3 - e.z;                    // dying mid-juggle, the burst happens where they actually are
        effects.add(Effect.particle(e.x, ey, 0, 0, 0.42, "fx.puff", -1, 1, 0, big ? 5 : 3));
        String bits = switch (level.theme) { case FOREST -> "fx.leaf"; case CITY -> "fx.scrap"; case LAB -> "fx.goo"; };
        int n = big ? 12 : 7;
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2, sp = 70 + rng.nextDouble() * 170;
            effects.add(Effect.particle(e.x, ey, Math.cos(a) * sp, Math.sin(a) * sp - 60, 0.7 + rng.nextDouble() * 0.4,
                bits, rng.nextInt(3), 0.06, -70, 3));
        }
    }

    /** The mad scientist's creations (and a nest's guards): an enemy brought in mid-fight. It dies when the boss does. */
    void summon(Enemy.Type type, double x, double y) {
        Util.Vec v = level.clamp(x, y, type.radius);
        Enemy e = new Enemy(type, v.x(), v.y(), level.hpMult(type), level.damageMult(type), rng);
        e.summoned = true;
        arrivals.add(e);
        effects.add(Effect.ring(v.x(), v.y(), type.radius * 2.5, type.radius, 0.5, new Color(120, 255, 160), false));
        soundAt(Snd.BOSS_SUMMON_LAB, v.x(), v.y());
    }

    // ------------------------------------------------------------------ queries

    /**
     * TAB: lock onto the nearest enemy, or if one is already locked, move to the next-nearest.
     * After the farthest enemy it wraps back around to the nearest.
     */
    private void cycleLock() {
        List<Enemy> alive = new ArrayList<>();
        for (Enemy e : enemies) if (e.targetable()) alive.add(e);
        if (alive.isEmpty()) {
            lockTarget = null;
            return;
        }
        alive.sort(Comparator.comparingDouble(e -> Util.dist(player.x, player.y, e.x, e.y)));
        int current = alive.indexOf(lockedTarget());   // -1 when nothing is locked, so we start at the nearest
        lockTarget = alive.get((current + 1) % alive.size());
    }

    /** The enemy currently locked onto, or null (also null once it has died). */
    Enemy lockedTarget() {
        return lockTarget != null && lockTarget.targetable() ? lockTarget : null;   // a locked shade in shadow mode is on hold
    }

    /**
     * What attacks should aim at: the locked enemy (at any distance — callers decide whether it's in reach),
     * otherwise the nearest enemy within {@code range}.
     */
    Enemy target(double x, double y, double range) {
        Enemy locked = lockedTarget();
        return locked != null ? locked : nearestEnemy(x, y, range);
    }

    /** The boss while it's alive, or null. */
    Enemy boss() {
        for (Enemy e : enemies) if (e.type == Enemy.Type.BOSS && e.hp > 0) return e;
        return null;
    }

    Enemy nearestEnemy(double x, double y, double range) {
        Enemy best = null;
        double bestDist = range;
        for (Enemy e : enemies) {
            if (!e.targetable()) continue;
            double d = Util.dist(x, y, e.x, e.y);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return best;
    }
}
