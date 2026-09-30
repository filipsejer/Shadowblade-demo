package game;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * One roguelike run, in the style of Survivor.io / Megabonk: three stages (the forest, the city, the laboratory), each
 * an open arena where monsters keep pouring in from off-screen for {@value #STAGE_TIME} seconds. They get tougher the
 * longer you last and the higher your level. Two elites crash in partway through each stage, and when the clock runs
 * out the stage's boss arrives inside a ring you can't leave. Beat it, open its chest, step through the portal.
 *
 * <p>Every enemy drops an XP gem; enough XP levels you up, and each level-up offers a choice of {@link Perk}s. You start
 * with nothing but the sword. Chests hold {@link Item}s, kept after the run (win or lose) along with the gold you
 * picked up. {@link World} runs the fight itself; this class is the director on top: the clock, spawning, drops,
 * pickups, the ring, choices, and the end of the run.
 */
final class Run {
    static final int STAGES = 3;
    /** Seconds of survival before a stage's boss arrives. */
    static final double STAGE_TIME = 300;
    static final double[] ELITE_TIMES = {100, 200};
    static final double[] SWARM_TIMES = {60, 150, 240};
    static final double BOSS_WARNING = 10;
    static final double RING_RADIUS = 600;
    static final int MAX_ENEMIES = 110;
    static final int CRATES = 34;

    final Random rng;
    final long seed;
    int stage;
    /** Seconds into the current stage (the clock on the HUD), and into the whole run. */
    double stageTime, runTime;
    int gold;
    /** Items found this run: yours to keep when it ends, however it ends. */
    final List<Item> loot = new ArrayList<>();
    /** What the character was wearing when the run began (a loaded run keeps its own gear, whatever the Armory says now). */
    final List<Item> gear = new ArrayList<>();
    final List<Pickup> pickups = new ArrayList<>();

    int elitesDone, swarmsDone, elitesKilled, bossesKilled;
    boolean bossSpawned, bossDead;
    boolean ringActive;
    double ringX, ringY;
    private double spawnTimer, crateTimer, retuneTimer, autosaveTimer = 30;
    private double xpCarry;

    // level-up / chest choices
    int pendingLevels, pendingChests;
    List<Perk.Choice> choices = List.of();
    int choiceCursor;
    String choiceTitle = "";
    int rerolls = 1;
    int cards = 3;
    /** Seconds before the cards on screen can be taken: a key already on its way down when they appeared can't pick one. */
    double choiceArm;
    /** How long the cards ignore ENTER after appearing. */
    static final double CHOICE_ARM_TIME = 0.45;
    boolean reviveAvailable, reviveUsed;

    // the end
    boolean over, victory;
    List<String> records = List.of();
    List<Item> bonusLoot = List.of();

    Run(long seed) {
        this.seed = seed;
        this.rng = new Random(seed);
    }

    // ------------------------------------------------------------------ starting, stages

    /** A brand-new run with whatever the profile is wearing. */
    static Run start(World w, Profile profile) {
        Run r = new Run(System.nanoTime());
        r.gear.addAll(profile.worn());
        w.level = Level.arena(0);
        w.player = r.makePlayer(w.level, true);
        r.enterStage(w, 0, true);
        return r;
    }

    /** A fresh character for a run: just the sword, plus whatever the gear gives. */
    Player makePlayer(Level level, boolean startPerks) {
        Player p = new Player(level.spawnX, level.spawnY);
        java.util.Arrays.fill(p.spellLevel, 0);          // no Magic menu in a run: skills fire on their own
        p.rollUnlocked = false;
        p.mpPerHit = 0;
        p.critChance = 0.05;
        for (Item it : gear) {
            for (Item.Stat s : it.stats.keySet()) applyStat(p, s, it.value(s));
            if (it.unique == null) continue;
            switch (it.unique) {
                case REROLL -> rerolls += 2;
                case REVIVE -> reviveAvailable = true;
                case FOURTH_CARD -> cards = 4;
                case WAVE_START -> { if (startPerks) Perk.CRESCENT_WAVE.take(p); }
                case COMBO_START -> { if (startPerks) Perk.COMBO.take(p); }
                case ROLL_START -> { if (startPerks) Perk.ROLL.take(p); }
            }
        }
        p.hp = p.maxHp;
        return p;
    }

    static void applyStat(Player p, Item.Stat s, double v) {
        switch (s) {
            case MELEE_DMG -> p.meleeMult += v / 100;
            case SPELL_DMG -> p.spellPower += v / 100;
            case MAX_HP -> p.maxHp += v;
            case ARMOR -> p.armor = Math.min(0.6, p.armor + v / 100);
            case ATTACK_SPEED -> p.attackSpeed += v / 100;
            case MOVE_SPEED -> p.moveSpeed *= 1 + v / 100;
            case CRIT -> p.critChance += v / 100;
            case REGEN -> p.regen += v;
            case MAGNET -> p.magnet *= 1 + v / 100;
            case XP -> p.xpMult += v / 100;
            case COOLDOWN -> p.cooldownMult *= Math.max(0.4, 1 - v / 100);
            case GOLD -> p.goldMult += v / 100;
        }
    }

    /** Arrives in stage {@code s}: a clean arena, full health, the clock at zero. Saved, so a crash never costs a stage. */
    void enterStage(World w, int s, boolean save) {
        stage = s;
        stageTime = 0;
        elitesDone = swarmsDone = 0;
        bossSpawned = bossDead = ringActive = false;
        spawnTimer = 1.0;
        crateTimer = 0;
        pickups.clear();
        if (w.level == null || w.level.arenaStage != s) w.level = Level.arena(s);
        w.clearField();
        Player p = w.player;
        p.x = p.lastX = w.level.spawnX;
        p.y = p.lastY = w.level.spawnY;
        p.hp = p.maxHp;
        w.camX = p.x;
        w.camY = p.y;
        w.banner = "STAGE " + (s + 1);
        w.bannerTimer = 3;
        w.notice = w.level.name;
        w.noticeHint = (s == 0 ? "Hold J to swing your sword.   " : "") + "Survive " + (int) (STAGE_TIME / 60) + " minutes. Then "
            + w.level.bossName + " comes for you.";
        w.noticeTimer = 5;
        w.fade = 1;
        retune(w);
        if (save) save(w, file());
    }

    // ------------------------------------------------------------------ difficulty

    /** How far into the run the monsters think you are: 5 per stage, plus the minutes into this one. */
    double threat() { return stage * 5 + Math.min(stageTime, STAGE_TIME) / 60; }

    double hpMult(Player p, Enemy.Type t) {
        double m = threat();
        double base = t.small() ? 0.52 : 0.8;
        return base * (1 + 0.30 * m + 0.014 * m * m) * (1 + 0.03 * (p.level - 1));
    }

    double dmgMult(Player p) {
        return (0.8 + 0.11 * threat()) * (1 + 0.012 * (p.level - 1));
    }

    /** How many monsters the director keeps around you. */
    int population(Player p) {
        double m = Math.min(stageTime, STAGE_TIME) / 60;
        return (int) Math.min(MAX_ENEMIES, 12 + (stage == 0 ? 5.2 : 6.5) * m + 6 * stage + 0.4 * p.level);
    }

    /** Keeps the level's multipliers (used by the final boss's summons) in step with the clock. */
    private void retune(World w) {
        Player p = w.player;
        w.level.smallEnemyHpMult = hpMult(p, Enemy.Type.GRUNT) * 0.6;
        w.level.enemyHpMult = hpMult(p, Enemy.Type.BRUTE);
        w.level.smallEnemyDamageMult = w.level.enemyDamageMult = dmgMult(p);
    }

    /** What the director sends at you, and how often, by stage and by how far into it you are. */
    private Enemy.Type pickType() {
        double m = stageTime / 60;
        double[] wts = new double[Enemy.Type.values().length];
        wts[Enemy.Type.GRUNT.ordinal()] = 1.0;
        switch (stage) {
            case 0 -> {
                wts[Enemy.Type.RUNNER.ordinal()] = m >= 0.5 ? 0.6 : 0;
                wts[Enemy.Type.SHOOTER.ordinal()] = m >= 1.5 ? 0.3 : 0;
                wts[Enemy.Type.BRUTE.ordinal()] = m >= 2.5 ? 0.1 + 0.03 * m : 0;
            }
            case 1 -> {
                wts[Enemy.Type.RUNNER.ordinal()] = 0.7;
                wts[Enemy.Type.SHOOTER.ordinal()] = m >= 0.5 ? 0.35 : 0;
                wts[Enemy.Type.SHADE.ordinal()] = m >= 1 ? 0.3 : 0;
                wts[Enemy.Type.BRUTE.ordinal()] = m >= 1.5 ? 0.15 : 0;
            }
            default -> {
                wts[Enemy.Type.GRUNT.ordinal()] = 0.9;
                wts[Enemy.Type.RUNNER.ordinal()] = 0.8;
                wts[Enemy.Type.SHOOTER.ordinal()] = 0.4;
                wts[Enemy.Type.SHADE.ordinal()] = 0.35;
                wts[Enemy.Type.BRUTE.ordinal()] = m >= 1 ? 0.22 : 0.08;
            }
        }
        double total = 0;
        for (double v : wts) total += v;
        double x = rng.nextDouble() * total;
        for (Enemy.Type t : Enemy.Type.values()) {
            x -= wts[t.ordinal()];
            if (x < 0) return t;
        }
        return Enemy.Type.GRUNT;
    }

    // ------------------------------------------------------------------ the frame

    void update(World w, double dt) {
        Player p = w.player;
        stageTime += dt;
        runTime += dt;
        if (p.regen > 0 && p.hp > 0) p.hp = Math.min(p.maxHp, p.hp + p.regen * dt);
        Arsenal.update(w, p, dt);

        retuneTimer -= dt;
        if (retuneTimer <= 0) { retuneTimer = 1; retune(w); }
        autosaveTimer -= dt;
        if (autosaveTimer <= 0) { autosaveTimer = 30; save(w, file()); }

        boolean calm = bossSpawned || bossDead;
        if (!calm) {
            direct(w, dt);
            if (elitesDone < ELITE_TIMES.length && stageTime >= ELITE_TIMES[elitesDone]) spawnElite(w);
            if (swarmsDone < SWARM_TIMES.length && stageTime >= SWARM_TIMES[swarmsDone]) swarm(w);
            if (stageTime >= STAGE_TIME) spawnBoss(w);
        }
        relocateStragglers(w);
        updateCrates(w, dt);
        updatePickups(w, dt);
    }

    /** Keeps the arena topped up: a few more monsters every third of a second while there are fewer than the target. */
    private void direct(World w, double dt) {
        spawnTimer -= dt;
        if (spawnTimer > 0) return;
        spawnTimer = 0.33;
        int want = population(w.player) - w.enemies.size();
        int shooters = 0;
        for (Enemy e : w.enemies) if (e.type == Enemy.Type.SHOOTER) shooters++;
        for (int i = 0; i < Math.min(3, want); i++) {
            Enemy.Type t = pickType();
            if (t == Enemy.Type.SHOOTER && shooters >= 8 + 2 * stage) t = Enemy.Type.GRUNT;
            if (t == Enemy.Type.SHOOTER) shooters++;
            Util.Vec at = spawnPoint(w, 760, 960);
            spawn(w, t, at.x(), at.y(), 1, 1);
        }
    }

    /** A spot {@code min}..{@code max} away from the player (just off-screen), inside the arena. */
    Util.Vec spawnPoint(World w, double min, double max) {
        Player p = w.player;
        double margin = 70;
        for (int tries = 0; tries < 24; tries++) {
            double a = rng.nextDouble() * Math.PI * 2, d = min + rng.nextDouble() * (max - min);
            double x = p.x + Math.cos(a) * d, y = p.y + Math.sin(a) * d;
            if (x > margin && y > margin && x < w.level.width - margin && y < w.level.height - margin) return new Util.Vec(x, y);
        }
        double a = rng.nextDouble() * Math.PI * 2;
        return w.level.clamp(p.x + Math.cos(a) * min, p.y + Math.sin(a) * min, 30);
    }

    Enemy spawn(World w, Enemy.Type t, double x, double y, double hpBoost, double dmgBoost) {
        Enemy e = new Enemy(t, x, y, hpMult(w.player, t) * hpBoost, dmgMult(w.player) * dmgBoost, w.rng);
        e.spawnIn = 0.35;
        w.enemies.add(e);
        return e;
    }

    /** Monsters left far behind are brought back round in front of you, so the pressure never just trails off. */
    private void relocateStragglers(World w) {
        for (Enemy e : w.enemies) {
            if (e.type == Enemy.Type.BOSS || e.elite || e.summoned) continue;
            if (Util.dist(e.x, e.y, w.player.x, w.player.y) < 1500) continue;
            Util.Vec at = spawnPoint(w, 760, 900);
            e.x = e.lastX = at.x();
            e.y = e.lastY = at.y();
            e.kx = e.ky = 0;
        }
    }

    /** A ring of monsters closing in from every side at once. */
    private void swarm(World w) {
        swarmsDone++;
        int n = 16 + 4 * stage + swarmsDone * 2;
        Enemy.Type t = stage == 0 && swarmsDone == 1 ? Enemy.Type.GRUNT : Enemy.Type.RUNNER;
        Player p = w.player;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            Util.Vec at = w.level.clamp(p.x + Math.cos(a) * 560, p.y + Math.sin(a) * 560, 20);
            spawn(w, t, at.x(), at.y(), 0.8, 1);
        }
        w.banner = "SWARM!";
        w.bannerTimer = 1.6;
        w.sound(w.themed(Snd.LOCK_FOREST, Snd.LOCK_CITY, Snd.LOCK_LAB));
    }

    private void spawnElite(World w) {
        elitesDone++;
        Enemy.Type t = switch (stage) {
            case 0 -> Enemy.Type.BRUTE;
            case 1 -> elitesDone == 1 ? Enemy.Type.BRUTE : Enemy.Type.SHADE;
            default -> elitesDone == 1 ? Enemy.Type.SHOOTER : Enemy.Type.BRUTE;
        };
        Util.Vec at = spawnPoint(w, 520, 640);
        Enemy e = spawn(w, t, at.x(), at.y(), t.small() ? 22 : 12, 1.5);
        e.elite = true;
        e.spawnIn = 0.9;
        w.banner = "ELITE!";
        w.bannerTimer = 2;
        w.notice = "An elite " + eliteName(t) + " has appeared";
        w.noticeHint = "It drops a treasure chest.";
        w.noticeTimer = 3.5;
        w.sound(Snd.BOSS_INTRO);
        w.effects.add(Effect.ring(at.x(), at.y(), 10, 120, 0.6, new Color(255, 210, 80), true));
    }

    private String eliteName(Enemy.Type t) {
        return switch (stage) {
            case 0 -> t == Enemy.Type.BRUTE ? "stump golem" : "beast";
            case 1 -> t == Enemy.Type.BRUTE ? "dumpster" : "shade";
            default -> t == Enemy.Type.SHOOTER ? "acid flask" : "mutant";
        };
    }

    /** The clock has run out: every monster still standing vanishes (leaving its gem), and the boss arrives in a ring. */
    private void spawnBoss(World w) {
        bossSpawned = true;
        Player p = w.player;
        for (Enemy e : new ArrayList<>(w.enemies)) {
            dropFor(w, e);
            w.effects.add(Effect.particle(e.x, e.y - e.radius * 0.3, 0, 0, 0.42, "fx.puff", -1, 1, 0, 3));
        }
        w.enemies.clear();
        w.projectiles.clear();
        w.lockTarget = null;
        double m = RING_RADIUS + 90;
        ringX = Util.clamp(p.x, m, w.level.width - m);
        ringY = Util.clamp(p.y, m, w.level.height - m);
        ringActive = true;
        double a = rng.nextDouble() * Math.PI * 2;
        double bx = ringX + Math.cos(a) * 360, by = ringY + Math.sin(a) * 360;
        double[] hp = {0.95, 2.0, 1.7};
        double[] dmg = {1.0, 1.3, 1.6};
        Enemy boss = new Enemy(Enemy.Type.BOSS, bx, by, hp[stage] * (1 + 0.05 * (p.level - 1)), dmg[stage], w.rng);
        w.enemies.add(boss);
        w.banner = w.level.bossName;
        w.bannerTimer = 3;
        w.notice = "BOSS FIGHT";
        w.noticeHint = "The ring holds you both in. Only one of you leaves.";
        w.noticeTimer = 3.5;
        w.sound(Snd.BOSS_INTRO);
        w.shake = Math.max(w.shake, 10);
        w.effects.add(Effect.ring(ringX, ringY, 40, RING_RADIUS, 0.7, new Color(255, 90, 90), false));
    }

    /**
     * The nearest spot to (x, y) where something {@code r} across can stand and be walked up to: out of any grass bed
     * and clear of trees and rocks; crates in the way are simply swept aside.
     */
    Util.Vec clearSpot(World w, double x, double y, double r) {
        for (int pass = 0; pass < 4; pass++) {
            for (java.awt.geom.Rectangle2D.Double g : w.level.grassPatches) {
                double minX = g.x - r, maxX = g.getMaxX() + r, minY = g.y - r, maxY = g.getMaxY() + r;
                if (x <= minX || x >= maxX || y <= minY || y >= maxY) continue;
                double left = x - minX, right = maxX - x, top = y - minY, bottom = maxY - y;
                double m = Math.min(Math.min(left, right), Math.min(top, bottom));
                if (m == left) x = minX; else if (m == right) x = maxX; else if (m == top) y = minY; else y = maxY;
            }
            for (Level.Landmark l : w.level.landmarks) {
                if (l.radius() <= 0) continue;
                double d = Util.dist(x, y, l.x(), l.y() - 12), min = l.radius() + r;
                if (d >= min) continue;
                double a = d < 0.01 ? 0 : Math.atan2(y - (l.y() - 12), x - l.x());
                x = l.x() + Math.cos(a) * min;
                y = l.y() - 12 + Math.sin(a) * min;
            }
            Util.Vec v = w.level.clamp(x, y, r);
            x = v.x();
            y = v.y();
        }
        for (Breakable b : w.level.breakables) if (!b.broken && Util.dist(x, y, b.x, b.y) < r + b.radius + 20) b.broken = true;
        return new Util.Vec(x, y);
    }

    /** Inside the boss ring, nobody gets out. Called after every collision pass. */
    void confine(World w) {
        if (!ringActive) return;
        Player p = w.player;
        Util.Vec v = inRing(p.x, p.y, p.radius);
        p.x = v.x();
        p.y = v.y();
        for (Enemy e : w.enemies) {
            v = inRing(e.x, e.y, e.radius);
            e.x = v.x();
            e.y = v.y();
        }
    }

    private Util.Vec inRing(double x, double y, double r) {
        double dx = x - ringX, dy = y - ringY, d = Math.hypot(dx, dy), max = RING_RADIUS - r;
        if (d <= max) return new Util.Vec(x, y);
        return new Util.Vec(ringX + dx / d * max, ringY + dy / d * max);
    }

    /** Keeps the arena stocked with crates (they break, the director quietly replaces them out of sight). */
    private void updateCrates(World w, double dt) {
        crateTimer -= dt;
        if (crateTimer > 0) return;
        crateTimer = 12;
        int alive = 0;
        for (Breakable b : w.level.breakables) if (!b.broken) alive++;
        w.level.breakables.removeIf(b -> b.broken);
        for (int i = alive; i < CRATES && i < alive + 4; i++) {
            Util.Vec at = spawnPoint(w, 900, 1500);
            w.level.breakables.add(new Breakable(rng.nextBoolean() ? Breakable.Kind.CRATE : Breakable.Kind.BARREL, at.x(), at.y(), 1));
        }
    }

    // ------------------------------------------------------------------ drops

    /** An enemy died (for real — the final boss changing stage doesn't count). */
    void onKill(World w, Enemy e) {
        if (e.elite) elitesKilled++;
        dropFor(w, e);
        if (e.type == Enemy.Type.BOSS) bossDown(w, e);
    }

    /** Its gem, maybe a coin or a heart, and a chest for an elite. */
    private void dropFor(World w, Enemy e) {
        if (e.summoned) return;
        int xp = (int) Math.round(e.type.xp * (1 + 0.2 * stage) * (e.elite ? 6 : 1) * (e.type == Enemy.Type.BOSS ? 1 + stage : 1));
        drop(w, new Pickup(Pickup.Kind.GEM, e.x, e.y, xp));
        if (rng.nextDouble() < 0.035 || e.elite) drop(w, new Pickup(Pickup.Kind.COIN, e.x, e.y, (1 + rng.nextInt(3)) * (1 + stage) * (e.elite ? 8 : 1)));
        if (rng.nextDouble() < 0.004) drop(w, new Pickup(Pickup.Kind.HEART, e.x, e.y, 0));
        if (e.elite) drop(w, new Pickup(Pickup.Kind.ELITE_CHEST, e.x, e.y, 0));
    }

    void drop(World w, Pickup pk) {
        if (pk.kind == Pickup.Kind.GEM) {
            int gems = 0;
            Pickup far = null;
            for (Pickup o : pickups) {
                if (o.kind != Pickup.Kind.GEM) continue;
                gems++;
                if (!o.attracted && Util.dist(o.x, o.y, w.player.x, w.player.y) > 900) far = o;
            }
            if (gems > 320 && far != null) { far.value += pk.value; return; }      // too many lying about: pool it into a distant one
        }
        if (pk.kind != Pickup.Kind.PORTAL && pk.kind != Pickup.Kind.BOSS_CHEST) {  // things pop out of what dropped them
            double a = rng.nextDouble() * Math.PI * 2, sp = pk.kind.magnetic() ? 60 + rng.nextDouble() * 90 : 120;
            pk.vx = Math.cos(a) * sp;
            pk.vy = Math.sin(a) * sp;
        }
        pickups.add(pk);
    }

    /** Crates hold the good stuff: gold, hearts, magnets and bombs. */
    void onSmash(World w, Breakable b) {
        double x = rng.nextDouble();
        Pickup.Kind k = x < 0.38 ? Pickup.Kind.COIN : x < 0.58 ? Pickup.Kind.HEART : x < 0.70 ? Pickup.Kind.MAGNET
            : x < 0.80 ? Pickup.Kind.BOMB : Pickup.Kind.GEM;
        int value = k == Pickup.Kind.COIN ? (3 + rng.nextInt(6)) * (1 + stage) : k == Pickup.Kind.GEM ? 12 + 6 * stage : 0;
        drop(w, new Pickup(k, b.x, b.y - 6, value));
    }

    /** The boss is down: the ring falls, every gem flies to you, its chest drops and a portal opens. */
    private void bossDown(World w, Enemy boss) {
        bossDead = true;
        ringActive = false;
        bossesKilled++;
        for (Pickup pk : pickups) if (pk.kind == Pickup.Kind.GEM || pk.kind == Pickup.Kind.COIN) pk.attracted = true;
        Util.Vec chest = clearSpot(w, boss.x, boss.y, 40);
        drop(w, new Pickup(Pickup.Kind.BOSS_CHEST, chest.x(), chest.y(), 0));
        Util.Vec portal = w.level.clamp(ringX, ringY - 120, 60);
        if (Util.dist(portal.x(), portal.y(), chest.x(), chest.y()) < 140) portal = w.level.clamp(ringX + 200, ringY - 160, 60);
        portal = clearSpot(w, portal.x(), portal.y(), 60);
        drop(w, new Pickup(Pickup.Kind.PORTAL, portal.x(), portal.y(), 0));
        boolean last = stage + 1 >= STAGES;
        w.banner = last ? "VICTORY!" : "STAGE CLEARED";
        w.bannerTimer = 3;
        w.notice = "Open the chest, then step into the portal";
        w.noticeHint = last ? "The portal leads home." : "It leads on to stage " + (stage + 2) + ".";
        w.noticeTimer = 6;
        w.sound(Snd.GUIDE_APPEAR, 2.4);
    }

    // ------------------------------------------------------------------ pickups

    private void updatePickups(World w, double dt) {
        Player p = w.player;
        boolean chestLeft = false;
        for (Pickup pk : pickups) if (pk.kind == Pickup.Kind.BOSS_CHEST) chestLeft = true;
        List<Pickup> taken = new ArrayList<>();
        for (Pickup pk : pickups) {
            pk.age += dt;
            pk.x += pk.vx * dt;
            pk.y += pk.vy * dt;
            double drag = Math.exp(-5 * dt);
            pk.vx *= drag;
            pk.vy *= drag;
            double d = Util.dist(pk.x, pk.y, p.x, p.y);
            if (pk.kind.magnetic() && pk.age > 0.25) {
                if (!pk.attracted && d < p.magnet) pk.attracted = true;
                if (pk.attracted) {
                    pk.speed = Math.min(1300, Math.max(pk.speed, 260) + 1800 * dt);
                    double step = Math.min(d, pk.speed * dt);
                    pk.x += (p.x - pk.x) / Math.max(d, 1e-6) * step;
                    pk.y += (p.y - 6 - pk.y) / Math.max(d, 1e-6) * step;
                    d = Util.dist(pk.x, pk.y, p.x, p.y);
                }
                if (d < p.radius + 12) taken.add(pk);
            } else if (pk.kind == Pickup.Kind.PORTAL) {
                if (!chestLeft && d < pk.radius()) taken.add(pk);
            } else if (d < p.radius + pk.radius() && pk.age > 0.3) {
                taken.add(pk);
            }
        }
        for (Pickup pk : taken) {
            if (!pickups.remove(pk)) continue;
            collect(w, pk);
            if (w.run != this || w.state != World.State.PLAYING && pk.kind == Pickup.Kind.PORTAL) return;   // the stage (or run) just ended
        }
    }

    private void collect(World w, Pickup pk) {
        Player p = w.player;
        switch (pk.kind) {
            case GEM -> {
                addXp(w, pk.value);
                w.soundAt(Snd.XP_PICKUP, pk.x, pk.y, 0, 0.8 + 0.2 * pk.gemTier(), 1);
            }
            case COIN -> {
                int g = (int) Math.round(pk.value * p.goldMult);
                gold += g;
                w.soundAt(Snd.COIN_PICKUP, pk.x, pk.y);
                w.effects.add(Effect.text(pk.x, pk.y - 20, "+" + g, new Color(255, 214, 80), false));
            }
            case HEART -> {
                p.heal(w, p.maxHp * 0.3);
                w.sound(Snd.POWERUP);
                w.effects.add(Effect.ring(p.x, p.y, 10, 60, 0.4, Ability.HEAL.color, true));
            }
            case MAGNET -> {
                for (Pickup o : pickups) if (o.kind.magnetic()) o.attracted = true;
                w.sound(Snd.POWERUP);
                w.effects.add(Effect.ring(p.x, p.y, 20, 900, 0.6, new Color(120, 170, 255), false));
                w.effects.add(Effect.text(p.x, p.y - 60, "MAGNET!", new Color(140, 190, 255), true));
            }
            case BOMB -> bomb(w, pk);
            case ELITE_CHEST -> openEliteChest(w, pk);
            case BOSS_CHEST -> openBossChest(w, pk);
            case PORTAL -> enterPortal(w);
        }
    }

    /** Wipes out every ordinary monster on screen (elites and bosses just take a heavy hit). */
    private void bomb(World w, Pickup pk) {
        Player p = w.player;
        w.sound(Snd.FIRE_EXPLODE);
        w.shake = Math.max(w.shake, 12);
        w.effects.add(Effect.explosion(p.x, p.y, 160));
        w.effects.add(Effect.ring(p.x, p.y, 30, 760, 0.5, new Color(255, 170, 70), true));
        for (Enemy e : w.enemies) {
            if (!e.targetable() || Util.dist(p.x, p.y, e.x, e.y) > 760) continue;
            double big = e.type == Enemy.Type.BOSS || e.elite ? e.maxHp * 0.08 : e.hp + 1;
            e.hurt(w, big, 0, 0, 0.3, new Color(255, 170, 70), true, false);
        }
    }

    private static final double[][] ELITE_RARITY = {{60, 30, 10, 0, 0}, {35, 40, 20, 5, 0}, {15, 40, 33, 10, 2}};
    private static final double[][] BOSS_RARITY = {{20, 45, 28, 7, 0}, {0, 30, 45, 22, 3}, {0, 10, 40, 40, 10}};

    private void openEliteChest(World w, Pickup pk) {
        int g = 20 + 15 * stage;
        gold += g;
        w.sound(Snd.CHEST_OPEN);
        chestBurst(w, pk.x, pk.y, new Color(255, 214, 90));
        w.effects.add(Effect.text(pk.x, pk.y - 50, "+" + g + " GOLD", new Color(255, 214, 80), true));
        if (rng.nextDouble() < 0.4) findItem(w, Item.roll(rng, Item.rarity(rng, ELITE_RARITY[stage]), stage));
        pendingChests++;
    }

    private void openBossChest(World w, Pickup pk) {
        int g = 60 * (stage + 1);
        gold += g;
        w.sound(Snd.CHEST_OPEN);
        chestBurst(w, pk.x, pk.y, new Color(255, 150, 60));
        w.effects.add(Effect.text(pk.x, pk.y - 50, "+" + g + " GOLD", new Color(255, 214, 80), true));
        findItem(w, Item.roll(rng, Item.rarity(rng, BOSS_RARITY[stage]), stage));
        pendingChests++;
    }

    private void findItem(World w, Item it) {
        loot.add(it);
        w.notice = "FOUND: " + it.name;
        w.noticeHint = it.rarity.label + " " + it.slot.label.toLowerCase() + "  -  yours to keep, equip it in the Armory";
        w.noticeTimer = 5;
        w.sound(Snd.GAME_CLEARED, 0.3);
    }

    private void chestBurst(World w, double x, double y, Color c) {
        w.effects.add(Effect.ring(x, y, 10, 110, 0.5, c, true));
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14;
            w.effects.add(Effect.particle(x, y - 20, Math.cos(a) * 170, Math.sin(a) * 170 - 60, 0.8, "fx.star", -1, 0.03, 30, 3));
        }
        w.shake = Math.max(w.shake, 4);
    }

    private void enterPortal(World w) {
        w.sound(Snd.TRAVEL);
        if (stage + 1 >= STAGES) finish(w, true);
        else enterStage(w, stage + 1, true);
    }

    // ------------------------------------------------------------------ XP and level-ups

    /** XP needed to go from level {@code lv} to the next. */
    static int xpFor(int lv) {
        int n = lv - 1;
        return 30 + 16 * n + (int) (1.8 * n * n);
    }

    void addXp(World w, double amount) {
        Player p = w.player;
        xpCarry += amount * p.xpMult;
        int whole = (int) xpCarry;
        xpCarry -= whole;
        p.xp += whole;
        int levels = 0;
        while (p.xp >= p.xpNext) {
            p.xp -= p.xpNext;
            p.level++;
            p.xpNext = xpFor(p.level);
            levels++;
        }
        if (levels == 0) return;
        pendingLevels += levels;
        Color gold = new Color(255, 220, 90);
        w.effects.add(Effect.text(p.x, p.y - 60, "LEVEL UP!", gold, true));
        w.effects.add(Effect.ring(p.x, p.y, 10, 80, 0.5, gold, true));
        w.sound(Snd.LEVEL_UP);
    }

    /** After the frame: if a level-up or a chest is waiting, stop the action and show the choices. */
    void maybeOpenChoices(World w) {
        if (w.state != World.State.PLAYING || w.player.hp <= 0) return;
        if (pendingLevels > 0) openChoices(w, "LEVEL UP!");
        else if (pendingChests > 0) openChoices(w, "TREASURE!");
    }

    private void openChoices(World w, String title) {
        choiceTitle = title;
        choices = Perk.offer(w.player, rng, cards, !title.startsWith("LEVEL"));
        if (choices.isEmpty()) choices = List.of(new Perk.Choice(null, -1), new Perk.Choice(null, -2));   // everything is maxed out
        choiceCursor = 0;
        choiceArm = CHOICE_ARM_TIME;
        w.state = World.State.LEVEL_UP;
        w.sound(Snd.MENU_OPEN);
    }

    /**
     * A/D (or the arrows) choose, ENTER (or E) takes, R rerolls the cards (a few times a run). The cards can't be taken
     * for the first {@value #CHOICE_ARM_TIME} seconds.
     */
    void updateChoices(World w, Input in, double dt) {
        choiceArm = Math.max(0, choiceArm - dt);
        int n = choices.size();
        if (in.pressed(KeyEvent.VK_RIGHT) || in.pressed(KeyEvent.VK_D) || in.pressed(KeyEvent.VK_DOWN) || in.pressed(KeyEvent.VK_S)) {
            choiceCursor = (choiceCursor + 1) % n;
            w.sound(Snd.MENU_MOVE);
        }
        if (in.pressed(KeyEvent.VK_LEFT) || in.pressed(KeyEvent.VK_A) || in.pressed(KeyEvent.VK_UP) || in.pressed(KeyEvent.VK_W)) {
            choiceCursor = (choiceCursor + n - 1) % n;
            w.sound(Snd.MENU_MOVE);
        }
        if (in.pressed(KeyEvent.VK_R)) {
            if (rerolls > 0 && choices.get(0).perk() != null) {
                rerolls--;
                choices = Perk.offer(w.player, rng, cards, !choiceTitle.startsWith("LEVEL"));
                choiceCursor = 0;
                choiceArm = CHOICE_ARM_TIME;
                w.sound(Snd.MENU_OPEN);
            } else {
                w.sound(Snd.MENU_DENY);
            }
        }
        if ((in.pressed(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_E)) && choiceArm <= 0) {
            take(w, choices.get(choiceCursor));
            in.consume(KeyEvent.VK_ENTER, KeyEvent.VK_E);
        }
    }

    void take(World w, Perk.Choice c) {
        Player p = w.player;
        if (c.perk() == null) {
            if (c.rank() == -1) gold += 40;
            else p.hp = Math.min(p.maxHp, p.hp + p.maxHp * 0.4);
        } else if (c.evolution()) {
            p.perk[c.perk().ordinal()] = Perk.EVOLVED;
            w.effects.add(Effect.text(p.x, p.y - 70, c.perk().evoName.toUpperCase() + "!", c.perk().color, true));
        } else {
            c.perk().take(p);
        }
        w.sound(Snd.MENU_SELECT);
        if (choiceTitle.equals("LEVEL UP!")) pendingLevels--;
        else pendingChests--;
        p.invuln = Math.max(p.invuln, 0.4);
        w.state = World.State.PLAYING;
        maybeOpenChoices(w);
    }

    // ------------------------------------------------------------------ the end

    /**
     * The run is over, won or lost. Everything found is added to the profile (plus a bonus item for a win, or a
     * consolation one for a long run that found nothing), records are updated, and the saved run is deleted.
     */
    void finish(World w, boolean won) {
        if (over) return;
        over = true;
        victory = won;
        Profile prof = w.profile;
        List<Item> bonus = new ArrayList<>();
        if (won) bonus.add(Item.roll(rng, Item.rarity(rng, new double[]{0, 0, 20, 55, 25}), 3));
        else if (loot.isEmpty() && runTime > 150) bonus.add(Item.roll(rng, Item.rarity(rng, new double[]{50, 40, 10, 0, 0}), stage));
        bonusLoot = bonus;
        List<Item> all = new ArrayList<>(loot);
        all.addAll(bonus);
        List<String> rec = new ArrayList<>();
        int reached = stage + (won ? 1 : 0);
        if (reached > prof.bestStage) { prof.bestStage = reached; rec.add("Furthest stage"); }
        if (w.kills > prof.bestKills) { prof.bestKills = w.kills; rec.add("Most kills"); }
        if (w.player.level > prof.bestLevel) { prof.bestLevel = w.player.level; rec.add("Highest level"); }
        if (runTime > prof.bestTime) { prof.bestTime = runTime; rec.add("Longest run"); }
        records = rec;
        prof.runs++;
        if (won) prof.victories++;
        prof.gold += gold;
        prof.add(all);
        prof.save(Profile.file());
        delete(file());
        w.state = World.State.RUN_END;
        w.sound(won ? Snd.GAME_CLEARED : Snd.GAME_OVER);
    }

    /** Legendary armour: the first death of the run isn't the end. */
    boolean tryRevive(World w) {
        if (!reviveAvailable || reviveUsed) return false;
        reviveUsed = true;
        Player p = w.player;
        p.hp = p.maxHp * 0.5;
        p.invuln = 2.5;
        w.banner = "REVIVED!";
        w.bannerTimer = 2;
        w.sound(Snd.GUIDE_APPEAR);
        w.effects.add(Effect.ring(p.x, p.y, 10, 260, 0.6, new Color(255, 150, 60), true));
        for (Enemy e : w.enemies) {
            double d = Util.dist(p.x, p.y, e.x, e.y);
            if (d > 300 || e.type.armored) continue;
            double a = Util.angleTo(p.x, p.y, e.x, e.y);
            e.kx += Math.cos(a) * 900;
            e.ky += Math.sin(a) * 900;
            e.stun = Math.max(e.stun, 1);
        }
        return true;
    }

    // ------------------------------------------------------------------ saving

    static Path file() { return Profile.home().resolve("run.properties"); }

    static boolean saved() { return Files.exists(file()); }

    static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }

    /** Writes the run as it stands. Enemies aren't saved: a loaded stage refills with fresh ones at the same clock time. */
    void save(World w, Path path) {
        if (over) return;
        Properties p = new Properties();
        Player pl = w.player;
        p.setProperty("version", "1");
        p.setProperty("seed", String.valueOf(seed));
        p.setProperty("stage", String.valueOf(stage));
        p.setProperty("stageTime", String.valueOf(stageTime));
        p.setProperty("runTime", String.valueOf(runTime));
        p.setProperty("gold", String.valueOf(gold));
        p.setProperty("kills", String.valueOf(w.kills));
        p.setProperty("level", String.valueOf(pl.level));
        p.setProperty("xp", String.valueOf(pl.xp));
        p.setProperty("hp", String.valueOf(pl.hp));
        p.setProperty("rerolls", String.valueOf(rerolls));
        p.setProperty("reviveUsed", reviveUsed ? "1" : "0");
        p.setProperty("elitesDone", String.valueOf(elitesDone));
        p.setProperty("swarmsDone", String.valueOf(swarmsDone));
        p.setProperty("elitesKilled", String.valueOf(elitesKilled));
        p.setProperty("bossesKilled", String.valueOf(bossesKilled));
        p.setProperty("bossDead", bossDead ? "1" : "0");
        p.setProperty("chestLeft", find(Pickup.Kind.BOSS_CHEST) != null ? "1" : "0");
        p.setProperty("pendingLevels", String.valueOf(pendingLevels));
        p.setProperty("pendingChests", String.valueOf(pendingChests));
        for (Perk k : Perk.values()) if (pl.perk[k.ordinal()] > 0) p.setProperty("perk." + k.name(), String.valueOf(pl.perk[k.ordinal()]));
        p.setProperty("loot", String.valueOf(loot.size()));
        for (int i = 0; i < loot.size(); i++) p.setProperty("loot." + i, loot.get(i).encode());
        p.setProperty("gear", String.valueOf(gear.size()));
        for (int i = 0; i < gear.size(); i++) p.setProperty("gear." + i, gear.get(i).encode());
        try {
            Files.createDirectories(path.getParent());
            try (Writer out = Files.newBufferedWriter(path)) {
                p.store(out, "Spellblade roguelike run in progress");
            }
        } catch (IOException e) {
            System.err.println("Could not save the run: " + e.getMessage());
        }
    }

    /**
     * Picks a saved run back up: same gear, same picks, same level and gold, back at the same point on the stage's
     * clock (a boss fight in progress restarts from the boss's warning). Null if there's no readable save.
     */
    static Run load(World w) {
        Properties p = readSaved();
        if (p == null || p.getProperty("stage") == null) return null;
        long seed;
        try { seed = Long.parseLong(p.getProperty("seed", "1")); } catch (NumberFormatException e) { seed = 1; }
        Run r = new Run(seed + 7919);
        for (int i = 0; i < Profile.intOf(p, "gear"); i++) {
            Item it = Item.decode(p.getProperty("gear." + i, ""));
            if (it != null) r.gear.add(it);
        }
        for (int i = 0; i < Profile.intOf(p, "loot"); i++) {
            Item it = Item.decode(p.getProperty("loot." + i, ""));
            if (it != null) r.loot.add(it);
        }
        int stage = Math.max(0, Math.min(STAGES - 1, Profile.intOf(p, "stage")));
        w.level = Level.arena(stage);
        Player pl = r.makePlayer(w.level, false);
        int[] ranks = new int[Perk.values().length];
        for (Perk k : Perk.values()) ranks[k.ordinal()] = Math.min(Perk.EVOLVED, Profile.intOf(p, "perk." + k.name()));
        Perk.reapply(pl, ranks);
        pl.level = Math.max(1, Profile.intOf(p, "level"));
        pl.xpNext = xpFor(pl.level);
        pl.xp = Math.min(pl.xpNext - 1, Profile.intOf(p, "xp"));
        w.player = pl;
        w.kills = Profile.intOf(p, "kills");
        r.gold = Profile.intOf(p, "gold");
        r.runTime = parse(p, "runTime");
        r.enterStage(w, stage, false);
        r.rerolls = Profile.intOf(p, "rerolls");
        r.reviveUsed = "1".equals(p.getProperty("reviveUsed"));
        r.elitesDone = Math.min(ELITE_TIMES.length, Profile.intOf(p, "elitesDone"));
        r.swarmsDone = Math.min(SWARM_TIMES.length, Profile.intOf(p, "swarmsDone"));
        r.elitesKilled = Profile.intOf(p, "elitesKilled");
        r.bossesKilled = Profile.intOf(p, "bossesKilled");
        r.pendingLevels = Profile.intOf(p, "pendingLevels");
        r.pendingChests = Profile.intOf(p, "pendingChests");
        r.stageTime = Math.min(parse(p, "stageTime"), STAGE_TIME - BOSS_WARNING);
        if ("1".equals(p.getProperty("bossDead"))) {           // saved after the boss: the portal (and the chest, if unopened) are waiting
            r.bossDead = r.bossSpawned = true;
            r.stageTime = STAGE_TIME;
            double cx = w.level.spawnX, cy = w.level.spawnY;
            Util.Vec chest = r.clearSpot(w, cx + 160, cy + 40, 40), portal = r.clearSpot(w, cx, cy - 200, 60);
            if ("1".equals(p.getProperty("chestLeft"))) r.pickups.add(new Pickup(Pickup.Kind.BOSS_CHEST, chest.x(), chest.y(), 0));
            r.pickups.add(new Pickup(Pickup.Kind.PORTAL, portal.x(), portal.y(), 0));
        }
        double hp = parse(p, "hp");
        pl.hp = hp > 0 ? Math.min(pl.maxHp, hp) : pl.maxHp;
        w.notice = "Run loaded";
        w.noticeHint = "Stage " + (stage + 1) + " at " + clock(r.stageTime) + ".";
        w.noticeTimer = 4;
        return r;
    }

    /** A short description of the saved run for the main menu ("Stage 2  -  Lv 14  -  3:12"), or null if there isn't one. */
    static String describeSaved() {
        Properties p = readSaved();
        if (p == null) return null;
        int st = Profile.intOf(p, "stage"), lv = Profile.intOf(p, "level");
        double t = parse(p, "stageTime");
        return "Stage " + (st + 1) + "   -   Lv " + lv + "   -   " + clock(t);
    }

    static Properties readSaved() {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file())) {
            p.load(r);
            return p;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    static double parse(Properties p, String key) {
        try {
            return Double.parseDouble(p.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String clock(double seconds) {
        int s = (int) seconds;
        return s / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", s % 60);
    }

    /** Pickups of a kind, for the HUD's arrows. */
    Pickup find(Pickup.Kind k) {
        for (Pickup pk : pickups) if (pk.kind == k) return pk;
        return null;
    }

    /** Iterator over pickups (the renderer draws them). */
    Iterator<Pickup> pickupIterator() { return pickups.iterator(); }
}
