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
    enum State { TITLE, CHAPTER_SELECT, PLAYING, UPGRADE, PAUSE, GAME_OVER }

    final Random rng = new Random();

    State state = State.TITLE;
    Level level;
    int stage;                      // which level we're on (0 = the first)
    double fade;                    // 1 -> 0 after travelling to a new level: a black screen fading in
    Level.Room activeRoom;          // the room currently in combat, or null
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
    /** Volume choices; they belong to the player, not to a run, so {@link #reset()} leaves them alone. */
    AudioSettings audio = new AudioSettings();
    int pauseCursor;                // which sound setting is selected on the pause screen
    private boolean sealedNear;     // standing next to the sealed boss door

    Enemy lockTarget;
    double camX, camY;
    /** Where the camera looks instead of at the player (NaN = follow the player): the tutorial pans to the tree. */
    double focusX = Double.NaN, focusY = Double.NaN;
    /** The story that opens the game, or null (after it, and in a game started without it). */
    Tutorial tutorial;
    /** Whether pressing ENTER on the title screen starts the tutorial (T toggles it there). */
    boolean tutorialOn;
    /** Which row is highlighted: the title screen's PLAY / SELECT CHAPTER menu, and the chapter list beneath it. */
    int menuCursor;
    int chapterCursor;
    /** Character level Select Chapter starts you at, fully skilled up for that stage of the game. */
    static final int CHAPTER_SELECT_LEVEL = 15;
    /** True once level 1 is cleared: Transit Town's train station opens, and its guide field means the station instead. */
    boolean stationOpen;
    /** Counts down after level 1's boss dies; at zero you're back in Transit Town. */
    double townReturnTimer;
    /** A short line from a friendly face in Transit Town — its own little dialogue box, separate from the tutorial's. */
    final Dialogue dialogue = new Dialogue();
    double shake;
    double hitStop;
    double time;
    int kills;

    String banner = "";
    double bannerTimer;

    static final int SKILL_POINTS_PER_LEVEL = 2;
    static final double STATION_RANGE = 90;       // how close you must be to use the upgrade station

    // upgrade station
    Level.Station activeStation;                  // the trainer whose menu is open
    List<Upgrade> catalogue = List.of();
    int stationCursor;
    String stationMessage = "";

    // HUD notice, e.g. "LEVEL 3!  +2 skill points"
    String notice = "";
    String noticeHint = "";
    double noticeTimer;

    World() {
        this(false);
    }

    /** {@code withTutorial}: ENTER on the title screen starts with the tutorial (the player can switch it off with T). */
    World(boolean withTutorial) {
        tutorialOn = withTutorial;
        reset();
    }

    void reset() {
        stage = 0;
        tutorial = null;
        focusX = focusY = Double.NaN;
        level = Level.create(stage);
        fade = 0;
        activeRoom = null;
        player = new Player(level.spawnX, level.spawnY);
        markVisited();
        enemies.clear();
        arrivals.clear();
        blasts.clear();
        projectiles.clear();
        zones.clear();
        effects.clear();
        lockTarget = null;
        camX = player.x;
        camY = player.y;
        shake = hitStop = time = 0;
        kills = 0;
        banner = "";
        bannerTimer = 0;
        activeStation = null;
        stationCursor = 0;
        stationMessage = notice = noticeHint = "";
        noticeTimer = 0;
        menuCursor = 0;
        chapterCursor = 0;
        stationOpen = false;
        townReturnTimer = 0;
        dialogue.clear();
        state = State.TITLE;
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
        fade = Math.max(0, fade - dt / 0.9);
        if (in.pressed(KeyEvent.VK_M)) audio.toggleMute();
        switch (state) {
            case TITLE -> updateTitle(in);
            case CHAPTER_SELECT -> updateChapterSelect(in);
            case PLAYING -> updatePlaying(dt, in);
            case UPGRADE -> updateStation(in);
            case PAUSE -> updatePause(in);
            case GAME_OVER -> {
                updateEffects(dt);
                if (in.pressed(KeyEvent.VK_R) || in.pressed(KeyEvent.VK_ENTER)) {
                    boolean again = tutorial != null;          // dying in the tutorial starts the tutorial over; later deaths go to level 1
                    reset();
                    if (again) beginTutorial();
                    state = State.PLAYING;
                }
            }
        }
        double lookX = Double.isNaN(focusX) ? player.x : focusX, lookY = Double.isNaN(focusY) ? player.y : focusY;
        camX += (lookX - camX) * Math.min(1, 9 * dt);
        camY += (lookY - camY) * Math.min(1, 9 * dt);
    }

    /** W/S (or the arrows) move between PLAY and SELECT CHAPTER; ENTER/SPACE picks the one that's lit up. T still toggles the opening story for PLAY. */
    private void updateTitle(Input in) {
        if (in.pressed(KeyEvent.VK_T)) {
            tutorialOn = !tutorialOn;
            sound(Snd.MENU_MOVE);
        }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_W) || in.pressed(KeyEvent.VK_S)) {
            menuCursor = 1 - menuCursor;
            sound(Snd.MENU_MOVE);
        }
        if (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_SPACE)) {
            if (menuCursor == 0) {
                if (tutorialOn) beginTutorial();
                else state = State.PLAYING;
                sound(Snd.TITLE_START);
            } else {
                chapterCursor = 0;
                state = State.CHAPTER_SELECT;
                sound(Snd.MENU_OPEN);
            }
        }
    }

    /** W/S choose a level, ENTER starts it (character level {@value #CHAPTER_SELECT_LEVEL}, skill points unspent), ESC goes back. */
    private void updateChapterSelect(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.TITLE;
            sound(Snd.MENU_BACK);
            return;
        }
        // one extra row beyond the real Level.COUNT levels: a hand-sketched prototype layout, not part of the actual game
        int rows = Level.COUNT + 1;
        if (in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) { chapterCursor = (chapterCursor + 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) { chapterCursor = (chapterCursor + rows - 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E)) {
            if (chapterCursor == Level.COUNT) beginPrototype();
            else beginChapter(chapterCursor);
        }
    }

    /**
     * Select Chapter: jump straight into level {@code index}'s hub, skipping the story and everything before it. The
     * character arrives at {@value #CHAPTER_SELECT_LEVEL}, with all the skill points that come with it and none spent,
     * so every level can be tried with a build worth the name.
     */
    private void beginChapter(int index) {
        reset();
        stage = index;
        level = Level.create(stage);
        player = new Player(level.spawnX, level.spawnY);
        grantLevels(player, CHAPTER_SELECT_LEVEL);
        camX = player.x;
        camY = player.y;
        markVisited();
        banner = level.name;
        bannerTimer = 3;
        sound(Snd.TITLE_START);
        state = State.PLAYING;
    }

    /**
     * Select Chapter's extra row: {@link Level#protoSketch()}, a hand-drawn layout being tried out. Deliberately
     * separate from {@link #beginChapter} — it doesn't touch {@link #stage}, so it can't interact with real level
     * progression (there's no combat here for {@code endCombat()} to ever act on anyway).
     */
    private void beginPrototype() {
        reset();
        level = Level.protoSketch();
        player = new Player(level.spawnX, level.spawnY);
        grantLevels(player, CHAPTER_SELECT_LEVEL);
        camX = player.x;
        camY = player.y;
        markVisited();
        banner = level.name;
        bannerTimer = 3;
        sound(Snd.TITLE_START);
        state = State.PLAYING;
    }

    /** Levels a fresh player up exactly as ordinary play would (same skill points, same XP curve) — just without the XP grind. */
    private static void grantLevels(Player p, int level) {
        while (p.level < level) {
            p.level++;
            p.skillPoints += SKILL_POINTS_PER_LEVEL;
            p.xpNext = 35 + 20 * (p.level - 1) + 3 * (p.level - 1) * (p.level - 1);
        }
    }

    private void updatePlaying(double dt, Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.PAUSE;
            sound(Snd.PAUSE_IN);
            return;
        }
        if (in.pressed(KeyEvent.VK_TAB)) {
            cycleLock();
            if (lockTarget != null) sound(Snd.LOCK_ON);
        }
        if (in.pressed(KeyEvent.VK_Q)) {
            if (lockTarget != null) sound(Snd.LOCK_OFF);
            lockTarget = null;
        }
        Level.Station trainer = stationNearby();
        if (in.pressed(KeyEvent.VK_E) && trainer != null) {
            openStation(trainer);
            return;
        }
        if (in.pressed(KeyEvent.VK_E) && guideNearby()) {
            nextLevel();
            return;
        }
        if (tutorial == null) {
            dialogue.update(this, in, dt);                          // a townsfolk's line, if one is showing
            if (dialogue.stopsWorld()) {
                updateEffects(dt);
                return;
            }
            Level.Npc npc = npcNearby();
            if (in.pressed(KeyEvent.VK_E) && npc != null) dialogue.say(npc.name(), npc.portrait(), Snd.TOWN_TALK, npc.line());
            if (in.pressed(KeyEvent.VK_E) && forestPathNearby()) {
                stage = 0;
                enterLevel();
                return;
            }
        }
        if (hitStop > 0) {           // brief freeze on impact
            hitStop -= dt;
            // attack / menu / roll presses made during the freeze must not be lost: keep them for when it ends
            in.carryOver(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT, KeyEvent.VK_BACK_SPACE);
            return;
        }
        if (townReturnTimer > 0) {   // the beat after level 1's boss dies, before you're whisked back to Transit Town
            townReturnTimer -= dt;
            if (townReturnTimer <= 0) {
                enterTown();
                return;
            }
        }

        time += dt;
        markVisited();
        updateSealedHum();
        bannerTimer = Math.max(0, bannerTimer - dt);
        noticeTimer = Math.max(0, noticeTimer - dt);

        if (tutorial != null) {
            tutorial.update(this, in, dt);
            if (tutorial == null) return;                       // it has just handed over to level 1
            if (tutorial.frozen()) {                            // waking up, or a line of dialogue waiting for a key: the world stands still
                updateEffects(dt);
                return;
            }
        }
        player.update(this, in, dt);
        for (Enemy e : enemies) e.update(this, dt);
        enemies.addAll(arrivals);                       // creatures the boss summoned this frame join the fight now that the loop is over
        arrivals.clear();
        resolveCollisions();
        updateProjectiles(dt);
        updateBlasts(dt);
        updateZones(dt);
        updateEffects(dt);
        if (tutorial == null) updateRooms();                     // the tutorial opens and closes its own doors
        collectDead();

        if (player.hp <= 0) {
            state = State.GAME_OVER;
            sound(Snd.GAME_OVER);
            for (int i = 0; i < 24; i++) {
                effects.add(Effect.spark(player.x, player.y, rng.nextDouble() * Math.PI * 2,
                    80 + rng.nextDouble() * 260, 4, 0.8, Player.COLOR));
            }
            return;
        }
    }

    private void updateEffects(double dt) {
        effects.removeIf(e -> !e.update(dt));
    }

    private void resolveCollisions() {
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (a.intangible() || b.intangible()) continue;
                double dx = b.x - a.x, dy = b.y - a.y;
                double d = Math.hypot(dx, dy);
                double min = a.radius + b.radius;
                if (d >= min) continue;
                if (d < 0.001) { dx = rng.nextDouble() - 0.5; dy = rng.nextDouble() - 0.5; d = Math.hypot(dx, dy); }
                double push = (min - d) * 0.3;
                double ux = dx / d, uy = dy / d;
                a.x -= ux * push;
                a.y -= uy * push;
                b.x += ux * push;
                b.y += uy * push;
            }
        }
        if (!player.dodging() && !player.sliding()) {   // rolling and attack-dashes pass through enemies
            for (Enemy e : enemies) {
                if (e.intangible()) continue;
                double dx = e.x - player.x, dy = e.y - player.y;
                double d = Math.hypot(dx, dy);
                double min = e.radius + player.radius;
                if (d >= min || d < 0.001) continue;
                double overlap = min - d;
                double ux = dx / d, uy = dy / d;
                e.x += ux * overlap * 0.7;
                e.y += uy * overlap * 0.7;
                player.x -= ux * overlap * 0.3;
                player.y -= uy * overlap * 0.3;
            }
        }
        for (Level.Landmark l : level.landmarks) {                 // a tree trunk is solid
            if (l.radius() <= 0) continue;
            Util.Vec out = around(l.x(), l.y() - 12, l.radius(), player.x, player.y, player.radius, player.x - player.lastX, player.y - player.lastY);   // (the trunk's base, as far as a body's centre is concerned)
            if (out != null) { player.x = out.x(); player.y = out.y(); }
            for (Enemy e : enemies) {
                out = around(l.x(), l.y() - 12, l.radius(), e.x, e.y, e.radius, e.x - e.lastX, e.y - e.lastY);
                if (out != null) { e.x = out.x(); e.y = out.y(); }
            }
        }
        for (Breakable c : level.breakables) {                     // crates and barrels are solid until they are smashed
            if (c.broken) continue;
            Util.Vec out = around(c.x, c.y - 8, c.radius, player.x, player.y, player.radius, player.x - player.lastX, player.y - player.lastY);
            if (out != null) { player.x = out.x(); player.y = out.y(); }
            for (Enemy e : enemies) {
                out = around(c.x, c.y - 8, c.radius, e.x, e.y, e.radius, e.x - e.lastX, e.y - e.lastY);
                if (out != null) { e.x = out.x(); e.y = out.y(); }
            }
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

    /** Where a body of radius r at (x, y) ends up when it is pushed out of a solid rectangle, or null if it is already clear of it (with r's worth of clearance all round). Pushed straight out to whichever edge is nearest. */
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

    /** Breaks a crate or barrel: splinters, a puff, a sound, and its XP floating up. */
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
        effects.add(Effect.text(b.x, b.y - 46, "+" + b.xp + " XP", new Color(255, 225, 110), false));
        shake = Math.max(shake, 2);
        gainXp(b.xp);
    }

    /** Breaks every crate and barrel whose centre is within {@code radius} of the point (blasts and bolts). */
    void smashNear(double x, double y, double radius) {
        for (Breakable b : level.breakables) if (!b.broken && Util.dist(x, y, b.x, b.y) <= radius + b.radius) smash(b);
    }

    private void updateProjectiles(double dt) {
        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext(); ) {
            Projectile p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            boolean outside = !level.contains(p.x, p.y);   // hit a wall
            boolean remove = false;

            if (p.friendly) {
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

    private void explode(Projectile p) {
        Color c = Ability.FIREBALL.color;
        effects.add(Effect.explosion(p.x, p.y, p.aoe));
        soundAt(Snd.FIRE_EXPLODE, p.x, p.y, 0, 1, 1.22 - 0.3 * Util.clamp(p.aoe / 90.0, 0, 1));   // a bigger blast is deeper
        for (int i = 0; i < 8; i++) {
            effects.add(Effect.spark(p.x, p.y, rng.nextDouble() * Math.PI * 2, 80 + rng.nextDouble() * 200, 4, 0.35, c));
        }
        smashNear(p.x, p.y, p.aoe);
        for (Enemy e : enemies) {
            if (!e.targetable()) continue;
            double d = Util.dist(p.x, p.y, e.x, e.y);
            if (d > p.aoe + e.radius) continue;
            double ang = Util.angleTo(p.x, p.y, e.x, e.y);
            e.hurt(this, p.damage, Math.cos(ang) * 200, Math.sin(ang) * 200, 0.2, c);
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
                    e.hurt(this, z.damage, 0, 0, 0, Ability.ICE_STORM.color);
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
            gainXp(e.summoned || e.noXp ? 0 : e.type.xp);
            if (e.type == Enemy.Type.BOSS) {
                for (Enemy o : enemies) if (o.summoned) o.hp = 0;                                  // his creations collapse with him
                blasts.clear();
            }
        }
    }

    private void deathSound(Enemy e) {
        if (e.type == Enemy.Type.BOSS) soundAt(Snd.BOSS_DIE, e.x, e.y);
        else if (e.radius > 20) soundAt(themed(Snd.DIE_BIG_FOREST, Snd.DIE_BIG_CITY, Snd.DIE_BIG_LAB), e.x, e.y);
        else soundAt(themed(Snd.DIE_FOREST, Snd.DIE_CITY, Snd.DIE_LAB), e.x, e.y);
    }

    /** Standing near the sealed boss door: a low warning hum, once each time you walk up to it. */
    private void updateSealedHum() {
        boolean near = false;
        for (Level.Door d : level.doors) {
            if (!d.sealed) continue;
            double cx = d.gap.getCenterX(), cy = d.gap.getCenterY();
            if (Util.dist(player.x, player.y, cx, cy) < 150) {
                near = true;
                if (!sealedNear) soundAt(Snd.SEALED, cx, cy);
            }
        }
        sealedNear = near;
    }

    /** A puff of smoke and a scatter of bits: leaves in the forest, scrap in the city. */
    private void deathBurst(Enemy e) {
        boolean big = e.radius > 20;
        effects.add(Effect.particle(e.x, e.y - e.radius * 0.3, 0, 0, 0.42, "fx.puff", -1, 1, 0, big ? 5 : 3));
        String bits = switch (level.theme) { case FOREST -> "fx.leaf"; case CITY -> "fx.scrap"; case LAB -> "fx.goo"; };
        int n = big ? 12 : 7;
        for (int i = 0; i < n; i++) {
            double a = rng.nextDouble() * Math.PI * 2, sp = 70 + rng.nextDouble() * 170;
            effects.add(Effect.particle(e.x, e.y - e.radius * 0.3, Math.cos(a) * sp, Math.sin(a) * sp - 60, 0.7 + rng.nextDouble() * 0.4,
                bits, rng.nextInt(3), 0.06, -70, 3));
        }
    }

    private void gainXp(int amount) {
        player.xp += amount;
        double rollBefore = player.rollDistanceMult();
        int levels = 0;
        while (player.xp >= player.xpNext) {
            player.xp -= player.xpNext;
            player.level++;
            player.xpNext = 35 + 20 * (player.level - 1) + 3 * (player.level - 1) * (player.level - 1);
            levels++;
        }
        if (levels > 0) {
            int points = levels * SKILL_POINTS_PER_LEVEL;
            player.skillPoints += points;
            notice = "LEVEL " + player.level + "!   +" + points + " skill points";
            noticeHint = "Spend your skill points with the trainers in the hub.";
            noticeTimer = 4;
            Color gold = new Color(255, 220, 90);
            if (player.rollDistanceMult() > rollBefore) {
                noticeHint = player.level >= Player.ROLL_BONUS_LEVEL_2 ? "Your roll now goes even further!" : "Your roll now goes further!";
                noticeTimer = 5;
                effects.add(Effect.text(player.x, player.y - 68, "ROLL DISTANCE UP!", new Color(150, 230, 255), true));
            }
            effects.add(Effect.text(player.x, player.y - 60, "LEVEL UP!", gold, true));
            sound(Snd.LEVEL_UP);
            effects.add(Effect.ring(player.x, player.y, 10, 80, 0.5, gold, true));
            for (int i = 0; i < 9; i++) {                       // a burst of stars
                double a = i * Math.PI * 2 / 9;
                effects.add(Effect.particle(player.x, player.y - 10, Math.cos(a) * 130, Math.sin(a) * 130, 0.7, "fx.star", -1, 0.02, 30, 3));
            }
        }
    }

    // ------------------------------------------------------------------ rooms

    /** Walking into an unvisited room spawns its enemies and locks its doors; killing them all opens the doors again. */
    private void updateRooms() {
        if (activeRoom == null) {
            Level.Room room = level.roomAt(player.x, player.y, Level.TRIGGER_INSET);
            if (room != null && room.state == Level.Room.State.UNVISITED) startCombat(room);
        } else if (enemies.isEmpty()) {
            endCombat();
        }
    }

    private void startCombat(Level.Room room) {
        activeRoom = room;
        room.state = Level.Room.State.COMBAT;
        level.refresh();                       // closes the doors
        for (Enemy.Type type : room.spawns) spawnEnemy(type, room);
        sound(themed(Snd.LOCK_FOREST, Snd.LOCK_CITY, Snd.LOCK_LAB));
        if (room.spawns.contains(Enemy.Type.BOSS)) sound(Snd.BOSS_INTRO);
        banner = room.name;
        bannerTimer = 2.2;
    }

    private void endCombat() {
        boolean bossWasSealed = !level.bossUnlocked();
        activeRoom.state = Level.Room.State.CLEARED;
        activeRoom = null;
        level.refresh();                       // opens the doors (and the boss door, if that was the last room)
        if (level.clearedRoomCount() == level.combatRoomCount()) {
            boolean more = stage + 1 < Level.COUNT;
            banner = more ? "LEVEL CLEARED" : "GAME CLEARED";
            if (more && stage == 0) {                              // the whispering forest: back to Transit Town, not a guide in the hub
                stationOpen = true;
                notice = "THE TRAIN STATION IS NOW OPEN";
                noticeHint = "Back to Transit Town.";
                townReturnTimer = 3.0;
                sound(Snd.GUIDE_APPEAR, 2.8);
            } else if (more) {
                level.guideAppeared = true;
                notice = "A GUIDE HAS APPEARED IN THE HUB";
                noticeHint = "Talk to them to travel to the next level.";
                sound(Snd.GUIDE_APPEAR, 2.8);                       // after the boss has finished dying
            } else {
                notice = "YOU'VE CLEARED EVERY LEVEL!";
                noticeHint = "";
                sound(Snd.GAME_CLEARED, 2.8);
            }
            noticeTimer = 7;
        }
        else if (bossWasSealed && level.bossUnlocked()) {
            banner = "BOSS DOOR UNLOCKED";
            sound(Snd.ROOM_CLEAR);
            sound(Snd.BOSS_UNSEAL, 0.9);
        }
        else {
            banner = "ROOM CLEARED";
            sound(Snd.ROOM_CLEAR);
        }
        bannerTimer = 2.5;
        player.hp = Math.min(player.maxHp, player.hp + player.maxHp * 0.25);
        player.mp = Math.min(player.maxMp, player.mp + 50);
        effects.add(Effect.ring(player.x, player.y, 10, 90, 0.5, Ability.HEAL.color, true));
    }

    /** The mad scientist's creations: an enemy brought in mid-fight. It is worth no XP and dies when the boss does. */
    void summon(Enemy.Type type, double x, double y) {
        Util.Vec v = level.clamp(x, y, type.radius);
        Enemy e = new Enemy(type, v.x(), v.y(), level.hpMult(type), level.damageMult(type), rng);
        e.summoned = true;
        arrivals.add(e);
        effects.add(Effect.ring(v.x(), v.y(), type.radius * 2.5, type.radius, 0.5, new Color(120, 255, 160), false));
        soundAt(Snd.BOSS_SUMMON_LAB, v.x(), v.y());
    }

    /** Drops an enemy at a random spot in the room, away from the player. */
    private void spawnEnemy(Enemy.Type type, Level.Room room) {
        final double margin = 90;
        double x = 0, y = 0;
        for (int tries = 0; tries < 30; tries++) {
            Rectangle2D.Double part = room.parts.get(rng.nextInt(room.parts.size()));   // multi-part rooms spawn into any of their pieces
            double mx = Math.min(margin, part.width / 2 - 1), my = Math.min(margin, part.height / 2 - 1);
            x = part.x + mx + rng.nextDouble() * (part.width - 2 * mx);
            y = part.y + my + rng.nextDouble() * (part.height - 2 * my);
            if (Util.dist(x, y, player.x, player.y) > 320) break;
        }
        enemies.add(new Enemy(type, x, y, level.hpMult(type), level.damageMult(type), rng));
        effects.add(Effect.ring(x, y, type.radius * 2.5, type.radius, 0.5, type.color, false));
    }

    /** Text for the top of the HUD: the room you're in, and how the fight is going. */
    String roomStatus() {
        if (activeRoom != null) return activeRoom.name + "   -   " + enemies.size() + " left";
        Level.Room room = level.roomAt(player.x, player.y, 0);
        if (room == null) return "";
        return room.state == Level.Room.State.CLEARED ? room.name + "   -   cleared" : room.name;
    }

    // ------------------------------------------------------------------ menus

    /** True when the guide (in Transit Town, the train station) has appeared and you're standing next to them. */
    boolean guideNearby() {
        return level.guideAppeared && activeRoom == null
            && Util.dist(player.x, player.y, level.guideX, level.guideY) <= STATION_RANGE;
    }

    /** The friendly local within reach in Transit Town, or null. */
    Level.Npc npcNearby() {
        if (activeRoom != null) return null;
        for (Level.Npc n : level.npcs) if (Util.dist(player.x, player.y, n.x(), n.y()) <= STATION_RANGE) return n;
        return null;
    }

    /** Standing at the path out of Transit Town into the forest (level 1). */
    boolean forestPathNearby() {
        return level.town && activeRoom == null && Util.dist(player.x, player.y, level.forestX, level.forestY) <= STATION_RANGE;
    }

    /**
     * Travel to the next level. The player keeps everything (level, skill points, spells, stats) and arrives in the new
     * level's hub, healed. The hub itself looks the same; the rooms around it are different.
     */
    void nextLevel() {
        sound(Snd.TRAVEL);
        stage++;
        enterLevel();
    }

    /** Arrive in the current {@link #stage}'s level: healed, in its hub, with the level's name across the screen. */
    private void enterLevel() {
        level = Level.create(stage);
        arrive();
    }

    /** Back to (or into, from the tutorial) Transit Town: healed, at the town square, the station open if level 1 is behind you. */
    private void enterTown() {
        level = Level.town();
        level.guideAppeared = stationOpen;
        arrive();
    }

    /** The part every arrival shares, wherever {@link #level} now points: a clean slate, healed, at the spawn point. */
    private void arrive() {
        activeRoom = null;
        enemies.clear();
        arrivals.clear();
        blasts.clear();
        projectiles.clear();
        zones.clear();
        effects.clear();
        lockTarget = null;
        dialogue.clear();
        player.x = level.spawnX;
        player.y = level.spawnY;
        player.hp = player.maxHp;
        player.mp = player.maxMp;
        camX = player.x;
        camY = player.y;
        markVisited();
        banner = level.name;
        bannerTimer = 3;
        notice = "";
        noticeTimer = 0;
        fade = 1;
    }

    // ------------------------------------------------------------------ the tutorial

    /** Starts the game with the opening story: you wake up in a clearing beside a tree. */
    void beginTutorial() {
        reset();
        level = Level.tutorial();
        player = new Player(level.spawnX, level.spawnY);
        player.facing = Math.PI / 2;
        camX = player.x;
        camY = player.y;
        markVisited();
        tutorial = new Tutorial(this);
        state = State.PLAYING;
    }

    /** The story is over: not level 1 just yet — Transit Town first, where the path into the forest is waiting. */
    void finishTutorial() {
        tutorial = null;
        focusX = focusY = Double.NaN;
        sound(Snd.TRAVEL);
        enterTown();
    }

    /** The trainer you're standing next to (they only exist in the safe hub), or null. */
    Level.Station stationNearby() {
        return activeRoom == null ? level.stationNear(player.x, player.y, STATION_RANGE) : null;
    }

    private void openStation(Level.Station station) {
        activeStation = station;
        catalogue = UpgradePool.catalogue(player);
        stationCursor = 0;
        stationMessage = "";
        state = State.UPGRADE;
        sound(Snd.MENU_OPEN);
    }

    /** The rows of the open trainer's menu: only their own category of upgrades. */
    List<Upgrade> stationItems() {
        return activeStation == null ? List.of() : UpgradePool.inCategory(catalogue, activeStation.category());
    }

    /** W / S choose an upgrade, Enter or E buys it, Esc leaves. */
    private void updateStation(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.PLAYING;
            sound(Snd.MENU_BACK);
            return;
        }
        int rows = stationItems().size();
        if (in.pressed(KeyEvent.VK_S) || in.pressed(KeyEvent.VK_DOWN)) { stationCursor = (stationCursor + 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_W) || in.pressed(KeyEvent.VK_UP)) { stationCursor = (stationCursor + rows - 1) % rows; sound(Snd.MENU_MOVE); }
        if (in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E)) buy(stationItems().get(stationCursor));
    }

    void buy(Upgrade u) {
        if (u.maxed()) {
            stationMessage = u.title() + " is already at its maximum.";
            sound(Snd.MENU_DENY);
        } else if (player.skillPoints < u.cost()) {
            stationMessage = "Not enough skill points - " + u.title() + " costs " + u.cost() + ".";
            sound(Snd.MENU_DENY);
        } else {
            sound(Snd.MENU_SELECT);
            player.skillPoints -= u.cost();
            u.apply(player);
            catalogue = UpgradePool.catalogue(player);   // refresh ranks and costs
            stationMessage = "Bought " + u.title() + "!";
        }
    }

    /** Esc resumes. Up / Down choose a sound setting (music or effects volume) and Left / Right change it. */
    private void updatePause(Input in) {
        if (in.pressed(KeyEvent.VK_ESCAPE)) {
            state = State.PLAYING;
            sound(Snd.PAUSE_OUT);
            return;
        }
        if (in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) {
            pauseCursor = (pauseCursor + 1) % 2;
            sound(Snd.MENU_MOVE);
        }
        if (in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) {
            pauseCursor = (pauseCursor + 1) % 2;
            sound(Snd.MENU_MOVE);
        }
        int delta = (in.pressed(KeyEvent.VK_RIGHT) || in.pressed(KeyEvent.VK_D) ? 1 : 0) - (in.pressed(KeyEvent.VK_LEFT) || in.pressed(KeyEvent.VK_A) ? 1 : 0);
        if (delta != 0 && (pauseCursor == 0 ? audio.adjustMusic(delta) : audio.adjustSfx(delta))) sound(Snd.MENU_MOVE);
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
     * What attacks and spells should aim at: the locked enemy (at any distance — callers decide whether it's in
     * reach), otherwise the nearest enemy within {@code range}.
     */
    Enemy target(double x, double y, double range) {
        Enemy locked = lockedTarget();
        return locked != null ? locked : nearestEnemy(x, y, range);
    }

    /** Remembers the room the player is standing in, so the minimap can reveal it. */
    private void markVisited() {
        Level.Room room = level.roomAt(player.x, player.y, 0);
        if (room != null) room.visited = true;
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
