package game;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class Player {
    static final Color COLOR = new Color(58, 118, 255);
    static final double LUNGE_RANGE = 260;         // how far away an enemy can be and still be dashed through
    static final double DASH_OVERSHOOT = 26;       // how far past the enemy's far edge you land
    static final double DODGE_TIME = 0.26;
    static final double DODGE_SPEED = 640;
    /**
     * Roll distance bonuses (same duration, so the roll is faster): {@link #ROLL_BONUS} times as far from character
     * level 5, and {@link #ROLL_BONUS_2} times as far from level 10.
     */
    static final int ROLL_BONUS_LEVEL = 5, ROLL_BONUS_LEVEL_2 = 10;
    static final double ROLL_BONUS = 1.45, ROLL_BONUS_2 = 1.9;
    static final double COMBO_WINDOW = 0.45;       // time to press ENTER again to continue a chain
    static final double INPUT_BUFFER = 0.22;
    static final double LAUNCH_VZ = 560;           // a combo's finishing hit launches enemies into the air by this much (see Enemy.launch)

    double x, y;
    double lastX, lastY;                          // where it stood at the start of the frame (to tell which way it is moving)
    final double radius = 15;
    double facing;

    double hp = 100, maxHp = 100;
    double mp = 100, maxMp = 100, mpRegen = 5;
    double moveSpeed = 230;

    // progression
    int level = 1, xp = 0, xpNext = 40;
    int skillPoints = 0;                          // earned by levelling up, spent at the upgrade station
    final Map<String, Integer> ranks = new HashMap<>();

    // melee stats
    double meleeDamage = 10, meleeMult = 1, attackSpeed = 1, reachMult = 1;
    int comboMax = 2;
    double hpPerHit = 0, mpPerHit = 3;

    // spell stats
    double spellPower = 1, cooldownMult = 1;
    final CommandMenu menu = new CommandMenu();
    final int[] spellLevel = new int[Ability.values().length];   // 0 = not learned
    final double[] cooldown = new double[Ability.values().length];

    // dodge
    double dodgeCooldown = 0.75;
    double dodgeCd, dodgeTimer;
    private double dodgeDx, dodgeDy, ghostTimer;

    // attack state
    private int hitIndex, nextCombo;
    private boolean hitFinisher, hitDone;
    /** True while the player is pushing a movement key (for the walk animation). */
    boolean moving;
    private double stepTimer;                       // counts down to the next footstep sound
    private double swingTimer, swingDur, hitAt, comboTimer, attackLock;
    private double slideTimer, slideVx, slideVy;
    private double attackBuffer, dodgeBuffer;

    // damage state
    double invuln, hurtTimer;
    private double kx, ky;

    Player(double x, double y) {
        this.x = x;
        this.y = y;
        unlockSpell(Ability.FIREBALL);
    }

    int rank(String id) { return ranks.getOrDefault(id, 0); }

    boolean dodging() { return dodgeTimer > 0; }

    /** 1 normally; {@link #ROLL_BONUS} from level 5; {@link #ROLL_BONUS_2} from level 10. */
    double rollDistanceMult() {
        return level >= ROLL_BONUS_LEVEL_2 ? ROLL_BONUS_2 : level >= ROLL_BONUS_LEVEL ? ROLL_BONUS : 1.0;
    }

    /** How far a roll carries you, in pixels. */
    double rollDistance() { return DODGE_SPEED * DODGE_TIME * rollDistanceMult(); }

    /** Cuts a roll short (the tutorial's tree). */
    void stopRoll() {
        dodgeTimer = 0;
        invuln = 0;
    }

    boolean swinging() { return swingTimer > 0; }

    /** How far through the current swing we are, 0 to 1 (for picking the sword pose). */
    double swingPhase() { return swingDur <= 0 ? 0 : Util.clamp((swingDur - swingTimer) / swingDur, 0, 1); }

    /** How far through the current roll we are, 0 to 1. */
    double dodgeProgress() { return Util.clamp(1 - dodgeTimer / DODGE_TIME, 0, 1); }

    /** True while dashing through an enemy: nothing blocks you. */
    boolean sliding() { return slideTimer > 0; }

    /** Hits landed so far in the chain currently in progress (0 when no chain is active). */
    int comboProgress() {
        if (swingTimer > 0) return hitIndex + 1;
        return comboTimer > 0 ? nextCombo : 0;
    }

    /** Learns a spell; it then shows up in the Magic menu. */
    void unlockSpell(Ability a) {
        spellLevel[a.ordinal()] = 1;
    }

    /** The spells you know, in the order they appear in the Magic menu. */
    java.util.List<Ability> learnedSpells() {
        java.util.List<Ability> out = new java.util.ArrayList<>();
        for (Ability a : Ability.values()) if (spellLevel[a.ordinal()] > 0) out.add(a);
        return out;
    }

    void update(World w, Input in, double dt) {
        lastX = x;
        lastY = y;
        // timers
        invuln = Math.max(0, invuln - dt);
        hurtTimer = Math.max(0, hurtTimer - dt);
        dodgeCd = Math.max(0, dodgeCd - dt);
        attackLock = Math.max(0, attackLock - dt);
        attackBuffer -= dt;
        dodgeBuffer -= dt;
        for (int i = 0; i < cooldown.length; i++) cooldown[i] = Math.max(0, cooldown[i] - dt);
        mp = Math.min(maxMp, mp + mpRegen * dt);

        // the command menu turns ENTER / arrow presses into "attack" or "cast this spell"
        CommandMenu.Item cursorBefore = menu.cursor;
        boolean openBefore = menu.magicOpen, shiftBefore = menu.shiftOpened;
        int spellBefore = menu.spellCursor;
        CommandMenu.Result command = menu.update(in, learnedSpells());
        if (menu.magicOpen != openBefore) {
            if (menu.magicOpen) w.sound(Snd.MENU_OPEN);
            else if (!shiftBefore) w.sound(Snd.MENU_BACK);          // letting go of Shift closes the list quietly
        } else if (menu.cursor != cursorBefore || menu.spellCursor != spellBefore) {
            w.sound(Snd.MENU_MOVE);
        }
        if (command.attackPressed()) attackBuffer = INPUT_BUFFER;
        if (in.pressed(KeyEvent.VK_SPACE)) dodgeBuffer = INPUT_BUFFER;

        double ix = (in.down(KeyEvent.VK_D) ? 1 : 0) - (in.down(KeyEvent.VK_A) ? 1 : 0);
        double iy = (in.down(KeyEvent.VK_S) ? 1 : 0) - (in.down(KeyEvent.VK_W) ? 1 : 0);
        boolean moving = ix != 0 || iy != 0;
        this.moving = moving;
        if (moving) {
            double len = Math.hypot(ix, iy);
            ix /= len;
            iy /= len;
        }

        // knockback from being hit
        x += kx * dt;
        y += ky * dt;
        double drag = Math.exp(-10 * dt);
        kx *= drag;
        ky *= drag;

        if (command.cast() != null) menu.castResult(command.cast(), !dodging() && tryCast(w, command.cast()));

        if (dodgeBuffer > 0 && dodgeCd <= 0 && !dodging()) startDodge(w, ix, iy, moving);

        if (dodging()) {
            dodgeTimer -= dt;
            double t = Util.clamp(dodgeTimer / DODGE_TIME, 0, 1);
            double speed = DODGE_SPEED * rollDistanceMult() * (0.55 + 0.9 * t);
            x += dodgeDx * speed * dt;
            y += dodgeDy * speed * dt;
            ghostTimer -= dt;
            if (ghostTimer <= 0) {
                ghostTimer = 0.045;
                w.effects.add(Effect.ghost(x, y, facing, COLOR));
            }
        } else {
            updateMelee(w, in, dt, ix, iy, moving);
        }

        Util.Vec inside = w.level.clamp(x, y, radius);
        x = inside.x();
        y = inside.y();
    }

    private void updateMelee(World w, Input in, double dt, double ix, double iy, boolean moving) {
        if (swingTimer > 0) {
            swingTimer -= dt;
            if (!hitDone && swingDur - swingTimer >= hitAt) {
                hitDone = true;
                doHit(w);
            }
            if (swingTimer <= 0) {
                if (hitFinisher) {
                    attackLock = 0.2;
                    comboTimer = 0;
                    nextCombo = 0;
                } else {
                    comboTimer = COMBO_WINDOW;
                }
            }
        } else if (comboTimer > 0) {
            comboTimer -= dt;
            if (comboTimer <= 0) nextCombo = 0;
        }

        double moveMul = swingTimer > 0 ? 0.3 : 1.0;
        double vx = ix * moveSpeed * moveMul;
        double vy = iy * moveSpeed * moveMul;
        if (slideTimer > 0) {
            slideTimer -= dt;
            vx += slideVx;
            vy += slideVy;
            w.effects.add(Effect.ghost(x, y, facing, COLOR));   // dash trail
        }
        x += vx * dt;
        y += vy * dt;

        if (moving && swingTimer <= 0 && slideTimer <= 0) {
            stepTimer -= dt;
            if (stepTimer <= 0) {
                stepTimer += 0.31;
                Level.Room here = w.level.roomAt(x, y, 0);
                boolean stone = !w.forest() || here == null || here.name.equals("HUB");     // hard ground: city streets and the hub plaza
                w.soundAt(stone ? Snd.STEP_STONE : Snd.STEP_GRASS, x, y);
            }
        } else {
            stepTimer = Math.min(stepTimer, 0.08);      // the first step lands soon after you start walking
        }

        if (swingTimer <= 0) {
            Enemy lock = w.lockedTarget();
            if (lock != null) facing = Util.turnToward(facing, Util.angleTo(x, y, lock.x, lock.y), 16 * dt); // keep facing the lock
            else if (moving) facing = Util.turnToward(facing, Math.atan2(iy, ix), 16 * dt);
        }

        boolean wantAttack = attackBuffer > 0;          // a fresh ENTER press (buffered for a moment if you press mid-swing)
        if (swingTimer <= 0 && attackLock <= 0 && wantAttack) startAttack(w, ix, iy, moving);
    }

    private void startAttack(World w, double ix, double iy, boolean moving) {
        if (nextCombo >= comboMax) nextCombo = 0;
        hitIndex = nextCombo;
        hitFinisher = hitIndex >= comboMax - 1;
        nextCombo = hitFinisher ? 0 : hitIndex + 1;

        swingDur = (hitFinisher ? 0.44 : 0.27) / attackSpeed;
        hitAt = swingDur * 0.38;
        swingTimer = swingDur;
        hitDone = false;
        attackBuffer = 0;
        comboTimer = 0;

        // Kingdom Hearts style: face the nearest enemy and dash straight through it, landing on the far side.
        // The hit connects at the moment we cross the enemy; the rest of the swing is recovery on the other side.
        // With a target lock, the locked enemy is always the one we go for.
        Enemy target = w.target(x, y, LUNGE_RANGE);
        boolean inRange = target != null && Util.dist(x, y, target.x, target.y) <= LUNGE_RANGE;
        slideVx = slideVy = 0;
        if (hitFinisher) w.sound(Snd.SWING_HEAVY);
        else w.sound(inRange ? Snd.DASH : Snd.SWING_LIGHT);      // a light attack that dashes through someone whooshes; one at thin air swishes
        if (inRange) {
            facing = Util.angleTo(x, y, target.x, target.y);
            double toCenter = Util.dist(x, y, target.x, target.y);
            double total = toCenter + target.radius + radius + DASH_OVERSHOOT;
            slideTimer = Math.min(hitFinisher ? 0.15 : 0.12, swingDur * 0.6);
            double speed = total / slideTimer;
            slideVx = Math.cos(facing) * speed;
            slideVy = Math.sin(facing) * speed;
            hitAt = Math.max(0.03, slideTimer * toCenter / total);
        } else {
            // A locked target that's too far away to dash at: face it and step toward it.
            if (target != null) facing = Util.angleTo(x, y, target.x, target.y);
            else if (moving) facing = Math.atan2(iy, ix);
            slideTimer = 0.09;                     // small step forward even with nothing to hit
            slideVx = Math.cos(facing) * 300;
            slideVy = Math.sin(facing) * 300;
        }
    }

    private void doHit(World w) {
        double reach = (hitFinisher ? 76 : 62) * reachMult;
        double half = Math.toRadians(hitFinisher ? 115 : 70);
        double base = meleeDamage * meleeMult * (1 + 0.15 * hitIndex) * (hitFinisher ? 1.7 : 1);
        int hits = 0;

        for (Enemy e : w.enemies) {
            if (!e.targetable()) continue;
            double d = Util.dist(x, y, e.x, e.y);
            if (d - e.radius > reach) continue;
            // right on top of us (we're mid-pass): send them the way we're travelling
            double ang = d > e.radius * 0.6 ? Util.angleTo(x, y, e.x, e.y) : facing;
            if (d > e.radius) {
                double tolerance = half + Math.asin(Math.min(1, e.radius / d)); // fat enemies are easier to clip
                if (Math.abs(Util.angleDiff(facing, ang)) > tolerance) continue;
            }
            if (e.shielded) { e.bounce(w); continue; }                     // a thorny shell: the blow glances off
            double dmg = base * (0.92 + w.rng.nextDouble() * 0.16);
            double kb = hitFinisher ? 430 : 150;
            e.hurt(w, dmg, Math.cos(ang) * kb, Math.sin(ang) * kb, hitFinisher ? 0.45 : 0.2);
            if (hitFinisher) e.launch(LAUNCH_VZ);                              // the combo's last hit pops them into the air
            w.soundAt(e.type.armored ? Snd.HIT_ARMOR : hitFinisher ? Snd.HIT_HEAVY : Snd.HIT_LIGHT, e.x, e.y);
            w.effects.add(Effect.particle(e.x, e.y - e.radius * 0.3 - e.z, 0, 0, 0.24, "fx.spark", -1, 1, 0, 3));   // impact star
            for (int i = 0; i < 3; i++) {
                w.effects.add(Effect.spark(e.x, e.y - e.z, ang + (w.rng.nextDouble() - 0.5) * 2.4,
                    120 + w.rng.nextDouble() * 160, 3, 0.25, Color.WHITE));
            }
            hits++;
        }

        boolean smashed = false;                                        // crates and barrels in the arc break too
        for (Breakable b : w.level.breakables) {
            if (b.broken) continue;
            double d = Util.dist(x, y, b.x, b.y);
            if (d - b.radius > reach) continue;
            if (d > b.radius) {
                double ang = Util.angleTo(x, y, b.x, b.y);
                if (Math.abs(Util.angleDiff(facing, ang)) > half + Math.asin(Math.min(1, b.radius / d))) continue;
            }
            w.smash(b);
            smashed = true;
        }
        if (smashed) w.hitStop = Math.max(w.hitStop, 0.03);

        w.effects.add(Effect.slash(x, y, facing, half, reach + 10, hitFinisher ? new Color(255, 220, 120) : COLOR));
        if (hits > 0) {
            mp = Math.min(maxMp, mp + mpPerHit);
            hp = Math.min(maxHp, hp + hpPerHit);
            w.hitStop = Math.max(w.hitStop, hitFinisher ? 0.07 : 0.035);
            w.shake = Math.max(w.shake, hitFinisher ? 6 : 2);
        }
    }

    private void startDodge(World w, double ix, double iy, boolean moving) {
        if (moving) {
            dodgeDx = ix;
            dodgeDy = iy;
            facing = Math.atan2(iy, ix);
        } else {
            dodgeDx = Math.cos(facing);
            dodgeDy = Math.sin(facing);
        }
        w.sound(Snd.ROLL);
        dodgeTimer = DODGE_TIME;
        dodgeCd = dodgeCooldown;
        dodgeBuffer = 0;
        invuln = Math.max(invuln, DODGE_TIME + 0.06);
        ghostTimer = 0;
        // rolling cancels whatever you were doing
        swingTimer = 0;
        slideTimer = 0;
        comboTimer = 0;
        nextCombo = 0;
        attackBuffer = 0;
    }

    /** Casts a spell if it's off cooldown, you can afford it and it has something to do. Costs MP only on success. */
    private boolean tryCast(World w, Ability a) {
        int lv = spellLevel[a.ordinal()];
        if (lv == 0) return false;
        double cost = a.cost(lv);
        if (cooldown[a.ordinal()] > 0) {
            w.effects.add(Effect.text(x, y - 30, "Cooling down", new Color(200, 200, 210), false));
            w.sound(Snd.SPELL_FAIL);
            return false;
        }
        if (mp < cost) {
            w.effects.add(Effect.text(x, y - 30, "Not enough MP", new Color(110, 160, 255), false));
            w.sound(Snd.SPELL_FAIL);
            return false;
        }
        if (!Spells.cast(w, this, a, lv)) {
            w.sound(Snd.SPELL_FAIL);
            return false;
        }
        mp -= cost;
        cooldown[a.ordinal()] = a.cooldown * cooldownMult;
        return true;
    }

    void heal(World w, double amount) {
        double before = hp;
        hp = Math.min(maxHp, hp + amount);
        w.effects.add(Effect.text(x, y - 26, "+" + Math.round(hp - before), new Color(120, 240, 140), true));
    }

    void hurt(World w, double dmg, double fromX, double fromY) {
        if (invuln > 0 || hp <= 0) return;
        hp -= dmg;
        invuln = 0.6;
        hurtTimer = 0.6;
        double ang = Util.angleTo(fromX, fromY, x, y);
        kx = Math.cos(ang) * 260;
        ky = Math.sin(ang) * 260;
        w.shake = Math.max(w.shake, 8);
        w.effects.add(Effect.text(x, y - 26, "-" + Math.round(dmg), new Color(255, 90, 90), true));
        w.sound(Snd.PLAYER_HURT);
    }
}
