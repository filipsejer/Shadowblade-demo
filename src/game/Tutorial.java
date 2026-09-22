package game;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The opening of the game, told as a short story instead of a list of controls. You wake up dazed beside a tree; a squirrel
 * asks whether you have forgotten how to move, and one lesson leads into the next, each one a little scene:
 * <ol>
 *   <li>MOVE: the squirrel hops away and waits for you to walk over.</li>
 *   <li>ROLL: it tells you that you rolled into that tree, asks if you remember how, then throws acorns for you to roll through.</li>
 *   <li>ATTACK: a monster appears, the squirrel bolts, and calls advice from behind the brambles while you fight it.</li>
 *   <li>MAGIC: the monster curls up in a thorny shell that swords bounce off; only a spell gets through.</li>
 *   <li>Then you look for the squirrel through three rooms. It is scared of you now, and when you finally corner it, it says you
 *       are dangerous and must go and find your own people. You arrive in level 1.</li>
 * </ol>
 * This class holds the rules of the script (no drawing); {@link World} runs it and {@link Renderer} / {@link WorldRenderer} draw it.
 * Controls are only ever named in the squirrel's dialogue, and, if you stall, as key caps that fade in beside your hero.
 */
final class Tutorial {
    enum Step { WAKE, GREET, MOVE, TREE, ROLL, ACORNS, ALARM, ATTACK, MAGIC, SEARCH, FAREWELL, LEAVE }

    /** What the HUD should draw attention to (a pulsing outline). */
    enum Focus { NONE, ROLL, ATTACK, MAGIC }

    static final int DODGES_NEEDED = 2;
    static final double SQUIRREL_SPEED = 380;
    static final double SEARCH_FLEE_DISTANCE = 300, FOUND_DISTANCE = 170;
    private static final String NAME = "SQUIRREL";
    private static final String CALM = "squirrel.idle", SCARED = "squirrel.scared";

    /** The squirrel: it runs along a route of waypoints and otherwise sits, looking at you. */
    static final class Squirrel {
        enum Pose { SIT, RUN, SCARED, THROW }

        double x, y;
        boolean faceLeft, hidden;
        double alarm;                    // > 0: a "!" over its head
        double scared;                   // > 0: standing up frightened (when it isn't running)
        int throwFrame = -1;             // 0 winding up, 1 just let go, -1 not throwing
        private final Deque<double[]> route = new ArrayDeque<>();

        void run(double... xy) {
            route.clear();
            for (int i = 0; i + 1 < xy.length; i += 2) route.add(new double[]{xy[i], xy[i + 1]});
        }

        boolean running() { return !route.isEmpty(); }

        Pose pose() {
            if (running()) return Pose.RUN;
            if (throwFrame >= 0) return Pose.THROW;
            return scared > 0 ? Pose.SCARED : Pose.SIT;
        }

        void update(double dt) {
            alarm = Math.max(0, alarm - dt);
            scared = Math.max(0, scared - dt);
            if (route.isEmpty()) return;
            double[] to = route.peek();
            double dx = to[0] - x, dy = to[1] - y, d = Math.hypot(dx, dy), step = SQUIRREL_SPEED * dt;
            if (Math.abs(dx) > 1) faceLeft = dx < 0;
            if (d <= step) {
                x = to[0];
                y = to[1];
                route.poll();
            } else {
                x += dx / d * step;
                y += dy / d * step;
            }
        }
    }

    /** An acorn in flight. It is harmless (a bonk and no damage); the point is to roll through it. */
    static final class Acorn {
        double x, y, vx, vy, spin;
        boolean rolledThrough;
    }

    final Dialogue dialogue = new Dialogue();
    final Squirrel squirrel = new Squirrel();
    final List<Acorn> acorns = new ArrayList<>();
    Enemy foe;                                              // the monster of the ATTACK / MAGIC scenes
    Step step = Step.WAKE;
    double t;                                               // seconds in this step
    int dodges, bonks;

    private final Level level;
    private final Rectangle2D.Double clearing, trail, stream, hollow;
    private final Level.Room clearingRoom;
    private final Level.Landmark tree;
    private final double[][] perches;                       // where the squirrel hides while you search, one per room
    private final double[][] routes;                        // how it runs from one hiding place to the next (x, y pairs, flattened)
    private int perch;
    private double hintTimer;
    private boolean wasRolling, movedOn, castSeen, walkedShout, treeShout, fled, greeted, rolledOnce;
    private double throwCd = 1.0, windup, recover, doneAt = -1, bonkCd, leafCd, blackout, nagCd, rollPause;
    private int throwPhase;
    private final boolean[] held = new boolean[5];          // W A S D SPACE... see HELD_KEYS
    private static final int[] HELD_KEYS = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_SPACE};
    private boolean enterDown, shiftDown;

    Tutorial(World w) {
        level = w.level;
        clearingRoom = level.rooms.get(0);
        clearing = clearingRoom.bounds;
        trail = level.rooms.get(1).bounds;
        stream = level.rooms.get(2).bounds;
        hollow = level.rooms.get(3).bounds;
        tree = level.landmarks.get(0);
        perches = new double[][]{
            {trail.getMaxX() - 170, trail.getMaxY() - 118},
            {stream.getMaxX() - 160, stream.getMaxY() - 118},
            {hollow.getMaxX() - 130, hollow.getCenterY() + 22},
        };
        routes = new double[][]{
            {trail.getCenterX(), trail.getMaxY() - 30, trail.getCenterX(), stream.y + 70, perches[1][0], perches[1][1]},
            {stream.getMaxX() - 40, stream.getCenterY(), hollow.x + 90, hollow.getCenterY(), perches[2][0], perches[2][1]},
        };
        squirrel.x = clearing.getMaxX() + 60;                // it will come in through the east door
        squirrel.y = clearing.getCenterY();
        w.sound(Snd.WAKE_UP, 1.2);                          // after the title jingle has had its moment
    }

    // ------------------------------------------------------------------ what the HUD and the renderers ask

    /** The world stands still: waking up, a line that waits for a key, or the final fade. */
    boolean frozen() { return step == Step.WAKE || step == Step.LEAVE || dialogue.stopsWorld(); }

    boolean dazed() { return step == Step.WAKE; }

    /** 0 = eyes open, 1 = shut: two black lids that open, squint, blink and open again as you wake up. */
    double eyelids() {
        if (step != Step.WAKE) return 0;
        if (t < 0.9) return 1;
        if (t < 1.7) return lerp(1, 0.55, (t - 0.9) / 0.8);
        if (t < 2.15) return lerp(0.55, 0.95, (t - 1.7) / 0.45);
        if (t < 3.2) return lerp(0.95, 0, (t - 2.15) / 1.05);
        return 0;
    }

    private static double lerp(double a, double b, double f) {
        f = Util.clamp(f, 0, 1);
        return a + (b - a) * f * f * (3 - 2 * f);
    }

    /** 0..1: the screen going black at the very end. */
    double blackout() { return blackout; }

    /** The controls appear on the HUD as they are taught: first the roll button, then the command menu, then magic. */
    boolean showRoll() { return step.ordinal() >= Step.ROLL.ordinal(); }

    boolean showMenu() { return step.ordinal() >= Step.ATTACK.ordinal(); }

    boolean showMagic() { return step.ordinal() >= Step.MAGIC.ordinal(); }

    Focus focus() {
        return switch (step) {
            case ROLL -> Focus.ROLL;
            case ACORNS -> dodges < DODGES_NEEDED && !dialogue.stopsWorld() ? Focus.ROLL : Focus.NONE;
            case ATTACK -> Focus.ATTACK;
            case MAGIC -> castSeen ? Focus.NONE : Focus.MAGIC;
            default -> Focus.NONE;
        };
    }

    String objective() { return step == Step.SEARCH ? "Find the squirrel" : ""; }

    /** The key caps to show beside the hero if you seem stuck (fading in), or null. */
    String[] hintKeys() {
        return switch (step) {
            case MOVE -> new String[]{"W", "A", "S", "D"};
            case ROLL, ACORNS -> new String[]{"SPACE"};
            case ATTACK -> new String[]{"ENTER"};
            case MAGIC -> castSeen ? null : new String[]{"SHIFT", "ENTER"};
            default -> null;
        };
    }

    double hintAlpha() {
        if (hintKeys() == null || dialogue.stopsWorld()) return 0;
        return Util.clamp((hintTimer - hintDelay()) / 0.8, 0, 1);
    }

    private double hintDelay() {
        return switch (step) { case MOVE -> 5; case ROLL -> 6; case ACORNS -> 4.5; case ATTACK -> 9; default -> 10; };
    }

    /** Is this key (one of W A S D SPACE, ENTER, SHIFT) held right now? The key caps light up while you press them. */
    boolean held(String key) {
        return switch (key) {
            case "W" -> held[0]; case "A" -> held[1]; case "S" -> held[2]; case "D" -> held[3]; case "SPACE" -> held[4];
            case "ENTER" -> enterDown; case "SHIFT" -> shiftDown;
            default -> false;
        };
    }

    Level.Landmark tree() { return tree; }

    // ------------------------------------------------------------------ the script

    void update(World w, Input in, double dt) {
        t += dt;
        Player p = w.player;
        for (int i = 0; i < held.length; i++) held[i] = in.down(HELD_KEYS[i]);
        enterDown = in.down(KeyEvent.VK_ENTER);
        shiftDown = in.down(KeyEvent.VK_SHIFT);
        squirrel.update(dt);
        if (!squirrel.running() && !squirrel.hidden && step != Step.WAKE) squirrel.faceLeft = p.x < squirrel.x;
        boolean rollStarted = p.dodging() && !wasRolling;
        wasRolling = p.dodging();
        if (rollStarted) hintTimer = 0;
        else if (p.moving && step == Step.MOVE) hintTimer = 0;
        else hintTimer += dt;
        updateAcorns(w, dt);
        dialogue.update(w, in, dt);
        nagIfStuck();
        fallingLeaves(w, dt);
        bonkOnTree(w, p, dt);

        switch (step) {
            case WAKE -> wake(w, p);
            case GREET -> { if (!dialogue.active()) go(w, Step.MOVE); }
            case MOVE -> move(w, p);
            case TREE -> { if (!dialogue.active()) go(w, Step.ROLL); }
            case ROLL -> rollScene(w, p, rollStarted, dt);
            case ACORNS -> acornScene(w, p, dt);
            case ALARM -> alarm(w, p);
            case ATTACK -> attack(w, p);
            case MAGIC -> magic(w, p);
            case SEARCH -> search(w, p);
            case FAREWELL -> { if (!dialogue.active()) go(w, Step.LEAVE); }
            case LEAVE -> leave(w);
        }
        if (step != Step.TREE) w.focusX = w.focusY = Double.NaN;
    }

    /** What the squirrel repeats now and then if you have not done the thing yet, or null. */
    private String nagText() {
        return switch (step) {
            case MOVE -> squirrel.running() ? null : "Use W, A, S and D to walk over to me!";
            case ATTACK -> "Get close and press ENTER!";
            case MAGIC -> castSeen ? null : "Hold SHIFT, then press ENTER!";
            default -> null;
        };
    }

    private void nagIfStuck() {
        String text = nagText();
        if (text == null || dialogue.active()) return;
        nagCd -= 1.0 / 60;
        if (nagCd <= 0) {
            nagCd = 11;
            shout(text);
        }
    }

    private void go(World w, Step s) {
        step = s;
        t = 0;
        hintTimer = 0;
        nagCd = 11;
        switch (s) {
            case GREET -> {
                say("Oh! You're awake!");
                say("You've been lying there for ages, staring at nothing. Are you alright? You look dazed.");
                say("Wait... don't tell me. Did you forget how to MOVE?");
                say("Your legs listen to W, A, S and D. Go on, try it. Come over here!");
            }
            case MOVE -> {
                squirrel.run(clearing.x + 800, clearing.getCenterY() - 60);
                clearingRoom.state = Level.Room.State.COMBAT;          // the way out grows over with brambles until you have learned your lessons
                level.refresh();
                w.soundAt(Snd.LOCK_FOREST, clearing.getMaxX() + 90, clearing.getCenterY());
            }
            case TREE -> {
                say("See? Your legs remember. That's a start!");
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "So what happened to you? Oh! I saw the whole thing.", () -> lookAtTree(w));
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "You were tumbling around out here and - BONK! - you rolled right into that tree. Head first.", () -> lookAtTree(w));
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "It looked like it hurt. A lot.", () -> lookAtTree(w));
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "Do you remember how to roll? Tap SPACE while you're moving and you'll tumble that way.", () -> w.focusX = w.focusY = Double.NaN);
            }
            case ACORNS -> {
                say("Good! A roll makes you untouchable for a split second. Nothing can hurt you while you tumble.");
                say("Let's see you use it. I'll throw acorns at you, and you roll THROUGH them. Ready?");
                double px = w.player.x;
                double sx = px < clearing.getCenterX() ? Math.min(px + 430, clearing.getMaxX() - 130) : Math.max(px - 430, clearing.x + 130);
                squirrel.run(sx, Util.clamp(w.player.y, clearing.y + 140, clearing.getMaxY() - 140));
                throwCd = 1.2;
            }
            case ALARM -> {
                squirrel.throwFrame = -1;
                squirrel.scared = 3;
                squirrel.alarm = 1.1;
                w.sound(Snd.SQUIRREL_ALARM);
                dialogue.clearShouts();
                shout("Did you hear that? ...AAAH! A MONSTER!");
                double[] spot = monsterSpot(w);
                for (int i = 0; i < 9; i++) {
                    double a = w.rng.nextDouble() * Math.PI * 2, sp = 50 + w.rng.nextDouble() * 110;
                    w.effects.add(Effect.particle(spot[0], spot[1] - 10, Math.cos(a) * sp, Math.sin(a) * sp - 50, 0.8, "fx.leaf", w.rng.nextInt(3), 0.05, -30, 3));
                }
                w.effects.add(Effect.ring(spot[0], spot[1], 12, 60, 0.5, new Color(120, 200, 100), false));
                Enemy e = new Enemy(Enemy.Type.GRUNT, spot[0], spot[1], 1.0, 0.6, w.rng);   // the small enemy of level 1, a little gentler
                e.noXp = true;
                e.shellOnHit = true;
                e.spawnIn = 1e6;                                     // stays a ghost of itself until the fight begins
                w.enemies.add(e);
                foe = e;
                w.soundAt(Snd.WINDUP_SMALL_FOREST, spot[0], spot[1]);
            }
            case ATTACK -> w.player.mp = w.player.maxMp;
            case MAGIC -> {
                w.player.mp = w.player.maxMp;
                shoutNow("Its thorny shell blocks swords! Use MAGIC: hold SHIFT, then press ENTER!");
            }
            case FAREWELL -> {
                squirrel.scared = 1e6;
                squirrel.alarm = 0;
                dialogue.clear();
                dialogue.say(NAME, SCARED, Snd.SQUIRREL_TALK, "Oh no. You found me. P-please don't swing that sword at me!");
                dialogue.say(NAME, SCARED, Snd.SQUIRREL_TALK, "You wake up, and a few minutes later you're throwing fire at things. You're... dangerous.");
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "You can't stay here with a poor little squirrel. You need to find your own people. Ones who know about swords and spells and all that.");
                dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, "Go on. And thank you for not hitting me. Look out for the trees!");
            }
            default -> { }
        }
    }

    private void say(String text) { dialogue.say(NAME, CALM, Snd.SQUIRREL_TALK, text); }

    /** Called out while you play. Said from far away (with a note saying so) when the squirrel is out of sight. */
    private void shout(String text) { dialogue.shout(NAME, farNote(), squirrel.scared > 0 ? SCARED : CALM, Snd.SQUIRREL_TALK, text); }

    private void shoutNow(String text) {
        dialogue.clearShouts();
        shout(text);
    }

    private void shoutFar(String text) { dialogue.shout(NAME, "(shouting from far away)", CALM, Snd.SQUIRREL_TALK, text); }

    private String farNote() { return squirrel.hidden ? "(from behind the brambles)" : ""; }

    private void lookAtTree(World w) {
        w.focusX = tree.x();
        w.focusY = tree.y() - 60;
    }

    // ------------------------------------------------------------------ scenes

    private void wake(World w, Player p) {
        if (t >= 2.9 && !greeted) {
            greeted = true;
            squirrel.run(p.x + 190, p.y - 10);
            w.sound(Snd.SQUIRREL_SCURRY);
        }
        if (greeted && !squirrel.running()) go(w, Step.GREET);
    }

    private void move(World w, Player p) {
        if (squirrel.running()) return;
        if (!movedOn && t > 0.4) {
            movedOn = true;
            shout("Over here!");
        }
        if (Util.dist(p.x, p.y, squirrel.x, squirrel.y) < 120) {
            lessonDone(w, p);
            go(w, Step.TREE);
        }
    }

    /** The first roll is praised at once; the scene moves on once the roll is over, so the world never freezes with you in mid-air. */
    private void rollScene(World w, Player p, boolean rollStarted, double dt) {
        if (rollStarted && !rolledOnce) {
            rolledOnce = true;
            rollPause = 0.8;
            lessonDone(w, p);
            shoutNow("That's it! You remember!");
        }
        if (rolledOnce && !p.dodging() && (rollPause -= dt) <= 0) go(w, Step.ACORNS);
    }

    /** A little chime and a spark of stars: one thing learned. */
    private void lessonDone(World w, Player p) {
        w.sound(Snd.TUT_DONE);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI * 2 / 6;
            w.effects.add(Effect.particle(p.x, p.y - 10, Math.cos(a) * 110, Math.sin(a) * 110, 0.6, "fx.star", -1, 0.03, 20, 3));
        }
    }

    // ---- the acorn game

    private void acornScene(World w, Player p, double dt) {
        if (doneAt >= 0) {
            if (t >= doneAt) go(w, Step.ALARM);
            return;
        }
        if (dialogue.stopsWorld() || squirrel.running()) return;
        if (throwPhase == 0) {
            throwCd -= dt;
            if (throwCd <= 0) {
                throwPhase = 1;
                windup = 0.65;
                squirrel.throwFrame = 0;
            }
        } else if (throwPhase == 1) {
            windup -= dt;
            if (windup <= 0) {
                Acorn a = new Acorn();
                double dir = squirrel.faceLeft ? -1 : 1;
                a.x = squirrel.x + dir * 22;
                a.y = squirrel.y;
                double d = Math.max(1, Util.dist(a.x, a.y, p.x, p.y));
                a.vx = (p.x - a.x) / d * 330;
                a.vy = (p.y - a.y) / d * 330;
                acorns.add(a);
                w.soundAt(Snd.ACORN_THROW, a.x, a.y);
                squirrel.throwFrame = 1;
                recover = 0.35;
                throwPhase = 2;
            }
        } else {
            recover -= dt;
            if (recover <= 0) {
                squirrel.throwFrame = -1;
                throwPhase = 0;
                throwCd = 1.7;
            }
        }
    }

    private void updateAcorns(World w, double dt) {
        Player p = w.player;
        for (int i = acorns.size() - 1; i >= 0; i--) {
            Acorn a = acorns.get(i);
            a.x += a.vx * dt;
            a.y += a.vy * dt;
            a.spin += 14 * dt;
            double d = Util.dist(a.x, a.y, p.x, p.y);
            boolean touching = d < p.radius + 16;
            boolean passed = (p.x - a.x) * a.vx + (p.y - a.y) * a.vy < 0 && !touching;
            if (touching) {
                if (p.dodging()) a.rolledThrough = true;                       // it sails through a rolling hero
                else if (!a.rolledThrough) {
                    bonk(w, p, a);
                    acorns.remove(i);
                    continue;
                }
            }
            if (passed || !level.contains(a.x, a.y)) {
                acorns.remove(i);
                if (dodges >= DODGES_NEEDED) continue;
                if (a.rolledThrough) dodged(w, p);
                else walkedAround();
            }
        }
    }

    private void bonk(World w, Player p, Acorn a) {
        bonks++;
        w.soundAt(Snd.ACORN_BONK, p.x, p.y);
        w.shake = Math.max(w.shake, 3);
        w.effects.add(Effect.text(p.x, p.y - 34, "Bonk!", new Color(255, 214, 110), true));
        for (int i = 0; i < 4; i++) {
            double ang = i * Math.PI / 2 + w.time;
            w.effects.add(Effect.particle(p.x + Math.cos(ang) * 12, p.y - 26, Math.cos(ang) * 40, Math.sin(ang) * 40 - 30, 0.5, "fx.star", -1, 0.05, 0, 2));
        }
        hintTimer = Math.max(hintTimer, hintDelay());
        String[] lines = {"Bonk! Wait until it reaches you, then roll: SPACE!", "Too slow! Roll right as it arrives!", "Roll THROUGH it. Tap SPACE just before it hits!"};
        shoutNow(lines[Math.min(bonks - 1, lines.length - 1)]);
    }

    private void dodged(World w, Player p) {
        dodges++;
        w.sound(Snd.ROLL_DODGE);
        w.effects.add(Effect.text(p.x, p.y - 34, "Dodged!", new Color(150, 235, 255), true));
        if (dodges >= DODGES_NEEDED) {
            shoutNow("You're a natural! Nothing gets past those legs!");
            doneAt = t + 2.2;
            lessonDone(w, p);
        } else {
            shoutNow("Ha! Right through it! Once more!");
        }
    }

    private void walkedAround() {
        if (walkedShout && bonks + dodges > 1) return;
        walkedShout = true;
        shoutNow("You just walked around it! Roll THROUGH it: SPACE!");
    }

    // ---- the monster

    /** A place for the monster to appear: some way from you, inside the clearing, away from the tree and from the door the squirrel runs to. */
    private double[] monsterSpot(World w) {
        Player p = w.player;
        for (int k = 0; k < 64; k++) {
            double a = k * 0.83, r = 300 + (k % 5) * 30;
            double x = p.x + Math.cos(a) * r, y = p.y + Math.sin(a) * r;
            boolean inside = x > clearing.x + 130 && x < clearing.getMaxX() - 280 && y > clearing.y + 130 && y < clearing.getMaxY() - 130;
            boolean seen = y < p.y + 60 && y > p.y - 280;                       // on screen, and not hidden behind the dialogue box at the bottom
            if (inside && seen && Util.dist(x, y, tree.x(), tree.y()) > 130) return new double[]{x, y};
        }
        return new double[]{clearing.x + 180, clearing.getCenterY() - 200};
    }

    private void alarm(World w, Player p) {
        Enemy e = foe;
        if (t > 1.6 && !fled) {
            fled = true;
            squirrel.scared = 0;
            squirrel.run(clearing.getMaxX() - 40, clearing.getCenterY(), clearing.getMaxX() + 70, clearing.getCenterY());
            w.sound(Snd.SQUIRREL_SCURRY);
            shoutNow("Hit it with your sword! Press ENTER! I'm... going over there. Far away!");
        }
        if (!fled) {
            squirrel.faceLeft = e.x < squirrel.x;
            return;
        }
        if (!squirrel.running()) {
            squirrel.hidden = true;                                // it has dived into the brambles across the door, and the fight begins
            w.activeRoom = clearingRoom;
            w.sound(Snd.LOCK_FOREST);
            e.spawnIn = 1.2;
            go(w, Step.ATTACK);
        }
    }

    private void attack(World w, Player p) {
        Enemy e = foe;
        if (e.hp <= 0) { fightOver(w); return; }
        if (e.swordHits > 0 || e.magicHits > 0 || e.shielded) {
            lessonDone(w, p);
            go(w, Step.MAGIC);
            return;
        }
    }

    private void magic(World w, Player p) {
        Enemy e = foe;
        if (!castSeen && p.cooldown[Ability.FIREBALL.ordinal()] > 0) {
            castSeen = true;
            lessonDone(w, p);
        }
        if (e.hp <= 0 || w.enemies.isEmpty()) {
            if (!castSeen) lessonDone(w, p);
            fightOver(w);
            return;
        }
    }

    /** The monster is dead: the doors open, you are patched up, and the squirrel calls out from its hiding place. */
    private void fightOver(World w) {
        Player p = w.player;
        clearingRoom.state = Level.Room.State.CLEARED;
        w.activeRoom = null;
        level.refresh();
        w.sound(Snd.ROOM_CLEAR);
        p.hp = Math.min(p.maxHp, p.hp + p.maxHp * 0.5);
        p.mp = p.maxMp;
        w.effects.add(Effect.ring(p.x, p.y, 10, 90, 0.5, Ability.HEAL.color, true));
        squirrel.hidden = false;
        squirrel.x = perches[0][0];
        squirrel.y = perches[0][1];
        squirrel.faceLeft = true;
        squirrel.scared = 0;
        dialogue.clearShouts();
        shoutFar("...Is it gone? You did that?! Wow. Wow.");
        shoutFar("I'm hiding somewhere along the trail. Don't come looking for me!");
        go(w, Step.SEARCH);
    }

    // ---- looking for the squirrel

    private void search(World w, Player p) {
        double d = Util.dist(p.x, p.y, squirrel.x, squirrel.y);
        if (squirrel.running()) return;
        if (perch < 2 && d < SEARCH_FLEE_DISTANCE) {
            squirrel.alarm = 0.8;
            squirrel.scared = 0.3;
            w.sound(Snd.SQUIRREL_ALARM);
            w.sound(Snd.SQUIRREL_SCURRY, 0.2);
            dialogue.clearShouts();
            dialogue.shout(NAME, "", SCARED, Snd.SQUIRREL_TALK, perch == 0 ? "Eek! It's you! Please don't hit me!" : "Stop following me! You're scary!");
            squirrel.run(routes[perch]);
            perch++;
        } else if (perch == 2 && d < FOUND_DISTANCE) {
            go(w, Step.FAREWELL);
        }
    }

    private void leave(World w) {
        blackout = Util.clamp(t / 1.3, 0, 1);
        if (t > 0.3) squirrel.hidden = true;                       // it has slipped away into the log
        if (t >= 1.6) w.finishTutorial();
    }

    // ---- scenery

    private void fallingLeaves(World w, double dt) {
        if (level.roomAt(w.player.x, w.player.y, 0) != clearingRoom) return;
        leafCd -= dt;
        if (leafCd > 0) return;
        leafCd = 0.9 + w.rng.nextDouble() * 1.2;
        w.effects.add(Effect.particle(tree.x() + (w.rng.nextDouble() - 0.5) * 100, tree.y() - 120 - w.rng.nextDouble() * 30,
            (w.rng.nextDouble() - 0.5) * 24, 34, 2.8, "fx.leaf", w.rng.nextInt(3), 1, 0, 3));
    }

    /** Roll into the tree and you'll know how it felt. */
    private void bonkOnTree(World w, Player p, double dt) {
        bonkCd = Math.max(0, bonkCd - dt);
        if (step == Step.WAKE || !p.dodging() || bonkCd > 0) return;
        if (Util.dist(p.x, p.y, tree.x(), tree.y()) > p.radius + tree.radius() + 10) return;
        bonkCd = 1.2;
        p.stopRoll();
        w.soundAt(Snd.TREE_BONK, p.x, p.y);
        w.shake = Math.max(w.shake, 6);
        w.effects.add(Effect.text(p.x, p.y - 40, "BONK!", new Color(255, 214, 110), true));
        for (int i = 0; i < 5; i++) {
            double ang = i * Math.PI * 2 / 5;
            w.effects.add(Effect.particle(p.x, p.y - 30, Math.cos(ang) * 70, Math.sin(ang) * 40 - 30, 0.7, "fx.star", -1, 0.04, 10, 3));
        }
        for (int i = 0; i < 7; i++) {
            w.effects.add(Effect.particle(tree.x() + (w.rng.nextDouble() - 0.5) * 90, tree.y() - 110, (w.rng.nextDouble() - 0.5) * 60, 20, 1.6, "fx.leaf", w.rng.nextInt(3), 1, 0, 3));
        }
        if (!treeShout && (step == Step.ROLL || step == Step.ACORNS)) {
            treeShout = true;
            shoutNow("Yep! Just like that! That's exactly what happened to you!");
        }
    }
}
