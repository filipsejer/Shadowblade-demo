package game;

import java.awt.Color;
import java.awt.event.KeyEvent;

final class Player {
    static final Color COLOR = new Color(58, 118, 255);
    static final double LUNGE_RANGE = 260;         // how far away an enemy can be and still be dashed through
    static final double DASH_OVERSHOOT = 26;       // how far past the enemy's far edge you land
    static final double DODGE_TIME = 0.26;
    static final double DODGE_SPEED = 640;
    static final double COMBO_WINDOW = 0.45;       // time to press ENTER again to continue a chain
    static final double INPUT_BUFFER = 0.22;
    static final double LAUNCH_VZ = 560;           // a combo's finishing hit launches enemies into the air by this much (see Enemy.launch)

    double x, y;
    double lastX, lastY;                          // where it stood at the start of the frame (to tell which way it is moving)
    final double radius = 15;
    double facing;

    double hp = 100, maxHp = 100;
    double moveSpeed = 230;

    // a fight's level (it starts again from 1 every fight)
    int level = 1, xp = 0, xpNext = Run.xpFor(1);

    // melee stats
    double meleeDamage = 10, meleeMult = 1, attackSpeed = 1, reachMult = 1;
    int comboMax = 2;
    double hpPerHit = 0;

    // spell stats
    double spellPower = 1, cooldownMult = 1;

    // a fight's picks (see Run / Perk / Arsenal), and the stats picks, gear and masteries raise
    final int[] perk = new int[Perk.values().length];
    final double[] skillCd = new double[Perk.values().length];
    /** False at the start of a fight: the roll is a level-up pick there (or a mastery). Always true in the world. */
    boolean rollUnlocked = true;
    double rollBonus = 1;
    double critChance = 0, critMult = 1.8;
    /** Fraction of incoming damage ignored. */
    double armor = 0;
    double regen = 0;
    /** How close XP gems and gold have to be before they fly to you. */
    double magnet = 110;
    double xpMult = 1, goldMult = 1;
    double orbitAngle, auraTick, waveCd;

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
    }

    boolean dodging() { return dodgeTimer > 0; }

    /** How much further than usual a roll carries you (Evasive Roll's last rank). */
    double rollDistanceMult() { return rollBonus; }

    /** How far a roll carries you, in pixels. */
    double rollDistance() { return DODGE_SPEED * DODGE_TIME * rollDistanceMult(); }

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

        // ENTER (or J) attacks, and holding it keeps swinging; skills fire on their own. The level-up cards are taken
        // with the number keys, so a held attack can never pick one by accident
        if (in.pressed(KeyEvent.VK_ENTER) || in.down(KeyEvent.VK_ENTER) || in.pressed(KeyEvent.VK_J) || in.down(KeyEvent.VK_J)) attackBuffer = INPUT_BUFFER;
        if (in.pressed(KeyEvent.VK_SPACE) && rollUnlocked) dodgeBuffer = INPUT_BUFFER;

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
                boolean stone = !w.forest() || here == null || here.state == Level.Room.State.SAFE;     // hard ground: city streets, the camp's paving
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
        waveQueued = w.run != null;                              // Crescent Wave (a run skill) fires once we know which way we're facing
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
        if (waveQueued) {
            waveQueued = false;
            Arsenal.onSwing(w, this, hitFinisher);
        }
    }

    private boolean waveQueued;

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
            boolean crit = w.rng.nextDouble() < critChance;
            double dmg = base * (0.92 + w.rng.nextDouble() * 0.16) * (crit ? critMult : 1);
            double kb = hitFinisher ? 430 : 150;
            e.hurt(w, dmg, Math.cos(ang) * kb, Math.sin(ang) * kb, hitFinisher ? 0.45 : 0.2, crit ? Arsenal.CRIT : Color.WHITE, false, crit);
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

    void heal(World w, double amount) {
        double before = hp;
        hp = Math.min(maxHp, hp + amount);
        w.effects.add(Effect.text(x, y - 26, "+" + Math.round(hp - before), new Color(120, 240, 140), true));
    }

    void hurt(World w, double dmg, double fromX, double fromY) {
        if (invuln > 0 || hp <= 0) return;
        dmg *= 1 - armor;
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
