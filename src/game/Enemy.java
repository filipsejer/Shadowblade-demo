package game;

import java.awt.Color;

final class Enemy {
    /**
     * radius, hp, speed, damage, xp, windup time, knockback/stun resistance, colour, armored, shadowy.
     * Armored enemies (the boss) can't be stunned or interrupted, so they have to be dodged, not stun-locked.
     * Shadowy enemies (the Shade) flip between solid and shadow every {@link #SHADOW_PERIOD} seconds: in shadow they
     * can't attack you and you can't touch them.
     */
    enum Type {
        GRUNT(14, 30, 95, 9, 8, 0.45, 0.0, new Color(222, 52, 52), false, false),
        RUNNER(10, 16, 175, 6, 6, 0.28, 0.0, new Color(255, 96, 72), false, false),
        SHOOTER(13, 24, 70, 9, 12, 0.55, 0.0, new Color(210, 44, 120), false, false),
        SHADE(15, 35, 115, 10, 16, 0.4, 0.0, new Color(150, 40, 120), false, true),
        BRUTE(26, 130, 62, 20, 30, 0.65, 0.6, new Color(150, 22, 22), false, false),
        BOSS(46, 800, 75, 22, 250, 0.85, 0.95, new Color(110, 10, 40), true, false);

        final double radius, hp, speed, damage, windup, resist;
        final int xp;
        final Color color;
        final boolean armored, shadowy;

        /** Grunts, runners, shooters and shades: everything smaller than a brute. */
        boolean small() { return radius < 20; }

        Type(double radius, double hp, double speed, double damage, int xp, double windup, double resist, Color color,
             boolean armored, boolean shadowy) {
            this.armored = armored;
            this.shadowy = shadowy;
            this.radius = radius;
            this.hp = hp;
            this.speed = speed;
            this.damage = damage;
            this.xp = xp;
            this.windup = windup;
            this.resist = resist;
            this.color = color;
        }
    }

    enum State { CHASE, WINDUP, RECOVER }

    /** What the boss is currently winding up. */
    enum Attack { NONE, SLAM, BURST, BOMBS, CHARGE }

    /** Seconds a Shade spends in each mode. */
    static final double SHADOW_PERIOD = 5.0;
    /** How fast the final boss's stage-two charge attack travels. */
    static final double CHARGE_SPEED = 620;
    /** How fast a launched enemy falls back down. */
    static final double GRAVITY = 1500;
    /** The little upward nudge any hit gives an already-airborne enemy, so a combo keeps it up a while longer. */
    static final double JUGGLE_VZ = 110;

    private static final Color BURN_TEXT = new Color(255, 160, 60);

    final Type type;
    final double radius;
    final double damage;
    double x, y;
    double lastX, lastY;        // where it stood at the start of the frame
    double hp, maxHp;
    double kx, ky;              // knockback velocity
    double z, vz;                // height off the ground and vertical velocity: launched into the air by a finisher, pulled back down by gravity

    State state = State.CHASE;
    double stateTimer;
    double windupTotal;         // length of the wind-up in progress (for drawing its telegraph)
    Attack attack = Attack.NONE;
    double attackCd;            // boss: time until it may fire its next bullet burst
    boolean phase2;             // boss: below half health it gets faster and fires more bullets
    double summonCd = 5;        // the mad scientist, in his second phase: time until he brings more of his creations to life
    double bombCd = 6;          // the mad scientist: time until he next lobs flask bombs
    double stageTimer;          // the mad scientist changing stage: invulnerable and busy for this long
    int waves;                  // how many batches of creations he has summoned
    double chargeCd = 5;        // the mad scientist, in his second phase only: time until he may charge again
    double chargeTimer;         // >0 while mid-charge: a fast, committed dash
    double chargeDx, chargeDy;  // direction locked in the moment he launches
    boolean chargeHit;          // whether this charge has already landed its hit
    boolean summoned;           // a creation of the boss: worth no XP, and it falls apart when he does
    boolean noXp;               // worth no XP (the tutorial's enemy)
    boolean shellOnHit;         // the tutorial's enemy: the first sword hit makes it curl into a thorny shell
    boolean shelled;            // ...which it only does once
    boolean shielded;           // curled up in the shell: swords bounce off it, magic breaks it open
    int swordHits, magicHits;   // blows that landed, by kind (the tutorial counts them)
    double shootTimer;
    double strafeDir;

    boolean faceLeft;           // sprites face right; this mirrors them
    final double animOffset;    // desynchronises the walk cycles of a crowd
    boolean shadow;             // shade: currently in shadow mode
    double shadowTimer;         // shade: seconds left in the current mode
    double flash;               // white hit flash
    double spawnIn = 0.5;       // spawn-in delay: harmless and idle
    double stun;
    double slowTimer, slowMul = 1;
    double burnTimer, burnDps, burnTick;

    Enemy(Type type, double x, double y, double hpMult, double dmgMult, java.util.Random rng) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.radius = type.radius;
        this.maxHp = this.hp = type.hp * hpMult;
        this.damage = type.damage * dmgMult;
        this.shootTimer = 1.0 + rng.nextDouble() * 1.5;
        this.strafeDir = rng.nextBoolean() ? 1 : -1;
        this.animOffset = rng.nextDouble() * 10;
        if (type == Type.BOSS) {
            spawnIn = 1.0;         // a beat to take in the boss before it acts
            attackCd = 2.5;
        }
        if (type.shadowy) {        // shades start in a random mode, so a group isn't all solid or all shadow together
            shadow = rng.nextBoolean();
            shadowTimer = SHADOW_PERIOD;
        }
    }

    /** In shadow mode: can't attack, can't be attacked, can't be pushed around. */
    boolean intangible() { return type.shadowy && shadow; }

    /** Off the ground: launched by a finisher, or still hanging in the air from one. No AI while it's up here, and nothing shoves it around. */
    boolean airborne() { return z > 0.5; }

    /** Alive and solid: the only kind of enemy attacks, spells and the lock-on can pick. */
    boolean targetable() { return hp > 0 && !intangible() && stageTimer <= 0; }

    /**
     * A finisher's launcher: pops the enemy into the air to start a juggle. Heavier enemies (higher
     * {@link Type#resist}) go up less; armored enemies (the boss) not at all — juggling him would trivialise the fight.
     */
    void launch(double vz0) {
        if (type.armored) return;
        vz = Math.max(vz, vz0 * (1 - type.resist));
    }

    /** A sword blow. */
    void hurt(World w, double dmg, double kbx, double kby, double stunTime) {
        damage(w, dmg, kbx, kby, stunTime, Color.WHITE, false);
    }

    /** Magic (a spell or its burning): the colour is the damage number's. */
    void hurt(World w, double dmg, double kbx, double kby, double stunTime, Color textColor) {
        damage(w, dmg, kbx, kby, stunTime, textColor, true);
    }

    private void damage(World w, double dmg, double kbx, double kby, double stunTime, Color textColor, boolean magic) {
        if (hp <= 0 || intangible() || stageTimer > 0) return;        // nothing touches a shade in shadow mode, or a boss changing stage
        if (shielded) {
            if (!magic) { bounce(w); return; }                          // swords glance off the shell...
            breakShell(w);                                              // ...but magic cracks it open, and goes on to hurt what is inside
        }
        if (magic) magicHits++; else swordHits++;
        hp -= dmg;
        flash = 0.1;
        double k = 1 - type.resist;
        kx += kbx * k;
        ky += kby * k;
        if (airborne()) vz = Math.max(vz, JUGGLE_VZ * k);   // a hit while they're up keeps them up a little longer: that's the juggle
        if (stunTime > 0 && !type.armored) {
            // Any real hit stuns the enemy and cancels an attack it was winding up.
            // (Burn and Ice Storm ticks pass 0 so they never stun.)
            stun = Math.max(stun, stunTime * k);
            if (state == State.WINDUP) interrupt();
        }
        w.effects.add(Effect.text(x + (w.rng.nextDouble() - 0.5) * 14, y - radius - 6 - z,
            String.valueOf(Math.max(1, (int) Math.round(dmg))), textColor, false));
        if (shellOnHit && !shelled && !magic && hp > 0) curlUp(w);
    }

    /** The tutorial's enemy rolls itself into a ball of thorns after its first sword hit: it stops fighting, and only magic gets through. */
    private void curlUp(World w) {
        shelled = shielded = true;
        stun = 1e6;
        state = State.CHASE;
        w.soundAt(Snd.SHELL_CURL, x, y);
        w.effects.add(Effect.ring(x, y, radius, radius * 2.6, 0.4, new Color(120, 220, 110), false));
        w.effects.add(Effect.text(x, y - radius - 26, "It curled up!", new Color(170, 240, 140), true));
    }

    /** A sword hit on the shell: a clink and a spark, no damage. */
    void bounce(World w) {
        flash = 0.06;
        w.soundAt(Snd.SHELL_CLINK, x, y);
        w.effects.add(Effect.particle(x, y - radius * 0.3, 0, 0, 0.24, "fx.spark", -1, 1, 0, 3));
        w.effects.add(Effect.text(x + (w.rng.nextDouble() - 0.5) * 14, y - radius - 6, "Blocked!", new Color(200, 210, 200), false));
        w.shake = Math.max(w.shake, 2);
    }

    private void breakShell(World w) {
        shielded = false;
        stun = 0.5;
        w.soundAt(Snd.SHELL_BREAK, x, y);
        w.effects.add(Effect.ring(x, y, radius, radius * 3.2, 0.35, new Color(190, 255, 160), true));
        for (int i = 0; i < 10; i++) {
            double a = w.rng.nextDouble() * Math.PI * 2, sp = 90 + w.rng.nextDouble() * 170;
            w.effects.add(Effect.particle(x, y - radius * 0.4, Math.cos(a) * sp, Math.sin(a) * sp - 60, 0.7 + w.rng.nextDouble() * 0.4, "fx.leaf", w.rng.nextInt(3), 0.06, -70, 3));
        }
        w.effects.add(Effect.text(x, y - radius - 26, "Shell broken!", new Color(255, 230, 120), true));
    }

    /**
     * Counts down the current mode and flips it when it runs out. Going into shadow cancels any wind-up (it can't attack
     * from there); coming back out, it needs half a second to materialise before it can strike.
     */
    private void updateShadow(World w, double dt) {
        shadowTimer -= dt;
        if (shadowTimer > 0) return;
        shadow = !shadow;
        shadowTimer += SHADOW_PERIOD;
        if (shadow) {
            state = State.CHASE;
        } else {
            state = State.RECOVER;
            stateTimer = 0.5;
        }
        w.effects.add(Effect.ring(x, y, radius, radius * 2.4, 0.3, shadow ? new Color(190, 150, 255) : new Color(255, 110, 140), false));
        w.soundAt(shadow ? Snd.SHADE_OUT : Snd.SHADE_IN, x, y);
    }

    /** Cancels a wind-up: the enemy has to start over once it recovers. */
    private void interrupt() {
        if (type == Type.SHOOTER) {
            state = State.CHASE;
            shootTimer = Math.max(shootTimer, 0.8);    // the shot is lost; it needs a moment before trying again
        } else {
            state = State.RECOVER;
            stateTimer = 0.3;
        }
    }

    void slow(double mul, double duration) {
        slowMul = slowTimer > 0 ? Math.min(slowMul, mul) : mul;
        slowTimer = Math.max(slowTimer, duration);
    }

    void ignite(double dps, double duration) {
        burnDps = Math.max(burnDps, dps);
        burnTimer = Math.max(burnTimer, duration);
    }

    void update(World w, double dt) {
        lastX = x;
        lastY = y;
        flash = Math.max(0, flash - dt);

        // knockback always applies, even while stunned
        x += kx * dt;
        y += ky * dt;
        double drag = Math.exp(-9 * dt);
        kx *= drag;
        ky *= drag;

        if (z > 0 || vz != 0) {          // gravity always applies too, even mid-stun: a launched enemy falls regardless
            z += vz * dt;
            vz -= GRAVITY * dt;
            if (z <= 0) {
                z = 0;
                vz = 0;
                land(w);
            }
        }

        if (burnTimer > 0) {
            burnTimer -= dt;
            burnTick -= dt;
            if (burnTick <= 0) {
                burnTick = 0.5;
                hurt(w, burnDps * 0.5, 0, 0, 0, BURN_TEXT);
            }
            if (burnTimer <= 0) burnDps = 0;
        }
        if (slowTimer > 0) slowTimer -= dt;

        if (spawnIn > 0) { spawnIn -= dt; return; }
        if (type.shadowy) updateShadow(w, dt);
        if (stun > 0) { stun -= dt; return; }

        if (!airborne()) {                                        // up in the air: no AI, just falling and drifting on whatever knockback it has left
            Player p = w.player;
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.max(0.001, Math.hypot(dx, dy));
            double ux = dx / dist, uy = dy / dist;
            if (Math.abs(dx) > 3) faceLeft = dx < 0;                 // look toward the player
            double speed = type.speed * (slowTimer > 0 ? slowMul : 1) * (intangible() ? 1.2 : 1);

            if (type == Type.BOSS) updateBoss(w, dt, p, ux, uy, dist, speed);
            else if (type == Type.SHOOTER) updateShooter(w, dt, ux, uy, dist, speed);
            else updateMelee(w, dt, p, ux, uy, dist, speed);
        }

        Util.Vec inside = w.level.clamp(x, y, radius);
        x = inside.x();
        y = inside.y();
    }

    /** Hits the ground after being launched: a little dust, a thump, and a brief stagger before it can act again. */
    private void land(World w) {
        state = State.RECOVER;
        stateTimer = 0.4;
        w.shake = Math.max(w.shake, 2);
        w.soundAt(Snd.ENEMY_LAND, x, y);
        w.effects.add(Effect.particle(x, y - radius * 0.2, 0, 0, 0.3, "fx.puff", -1, 1, 0, 3));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI * 2 / 6;
            w.effects.add(Effect.particle(x, y, Math.cos(a) * 60, Math.sin(a) * 28 - 6, 0.3, "fx.spark", -1, 0.04, 40, 2));
        }
    }

    private void updateMelee(World w, double dt, Player p, double ux, double uy, double dist, double speed) {
        switch (state) {
            case CHASE -> {
                x += ux * speed * dt;
                y += uy * speed * dt;
                if (dist - radius - p.radius < 12 && !intangible()) {
                    state = State.WINDUP;
                    stateTimer = windupTotal = type.windup;
                    windupSound(w);
                }
            }
            case WINDUP -> {
                stateTimer -= dt;
                if (stateTimer <= 0) {
                    w.soundAt(Snd.ENEMY_STRIKE, x, y);
                    if (dist - radius - p.radius < strikeReach()) p.hurt(w, damage, x, y);
                    state = State.RECOVER;
                    stateTimer = 0.6;
                }
            }
            case RECOVER -> {
                stateTimer -= dt;
                if (stateTimer <= 0) state = State.CHASE;
            }
        }
    }

    /** The tell before an attack: a small creak or whine for the little ones, a groan for the big ones. */
    private void windupSound(World w) {
        boolean big = radius > 20;
        w.soundAt(big ? w.themed(Snd.WINDUP_BIG_FOREST, Snd.WINDUP_BIG_CITY, Snd.WINDUP_BIG_LAB)
                      : w.themed(Snd.WINDUP_SMALL_FOREST, Snd.WINDUP_SMALL_CITY, Snd.WINDUP_SMALL_LAB), x, y);
    }

    /** How far past its own body a melee strike reaches — also the size of the telegraph ring. */
    double strikeReach() { return type == Type.BOSS ? 90 : 34; }

    private void updateShooter(World w, double dt, double ux, double uy, double dist, double speed) {
        switch (state) {
            case CHASE, RECOVER -> {
                double mx = 0, my = 0;
                if (dist > 340) { mx = ux; my = uy; }
                else if (dist < 230) { mx = -ux; my = -uy; }
                else { mx = -uy * strafeDir * 0.6; my = ux * strafeDir * 0.6; }
                x += mx * speed * dt;
                y += my * speed * dt;
                shootTimer -= dt;
                if (shootTimer <= 0 && dist < 520) {
                    state = State.WINDUP;
                    stateTimer = windupTotal = type.windup;
                    windupSound(w);
                }
            }
            case WINDUP -> {
                stateTimer -= dt;
                if (stateTimer <= 0) {
                    double ang = Math.atan2(w.player.y - y, w.player.x - x);
                    Projectile b = new Projectile(false, x, y, ang, 270, 7, damage, 4);
                    w.projectiles.add(b);
                    w.soundAt(w.themed(Snd.SHOOT_FOREST, Snd.SHOOT_CITY, Snd.SHOOT_LAB), x, y);
                    state = State.CHASE;
                    shootTimer = 1.8 + w.rng.nextDouble() * 0.8;
                    if (w.rng.nextInt(3) == 0) strafeDir = -strafeDir;
                }
            }
        }
    }

    /**
     * The boss: walks at you and slams the ground when you're close (a big telegraphed ring), and from range fires a
     * ring of bullets. The forest and city bosses have one health bar and speed up past halfway through it. The final
     * boss instead fights his whole bar alone as stage one; the hit that would finish him off doesn't (see
     * {@link World#collectDead}) — he goes untouchable for a beat while four of his creations spring up, then comes
     * back with a fresh bar, faster, lobbing more bombs, still summoning waves, and now also charging you down. It
     * can't be stunned or interrupted.
     */
    private void updateBoss(World w, double dt, Player p, double ux, double uy, double dist, double speed) {
        boolean lab = w.level.theme == Theme.LAB;
        if (!lab && !phase2 && hp < maxHp * 0.5) {                    // the forest and city bosses: one bar, faster past halfway
            phase2 = true;
            w.effects.add(Effect.ring(x, y, radius, radius * 3.5, 0.6, new Color(255, 90, 60), true));
            w.shake = Math.max(w.shake, 10);
            w.soundAt(w.themed(Snd.BOSS_PHASE2_FOREST, Snd.BOSS_PHASE2_CITY, Snd.BOSS_PHASE2_LAB), x, y);
        }
        if (stageTimer > 0) {                                        // the final boss, changing stage: he laughs, untouchable, while the room fills up
            stageTimer -= dt;
            attack = Attack.NONE;
            state = State.RECOVER;
            stateTimer = 0.4;
            if (stageTimer <= 0) {                                   // ...and comes back swinging, with a whole fresh bar
                hp = maxHp;
                w.effects.add(Effect.ring(x, y, radius, radius * 3.5, 0.6, new Color(150, 255, 120), true));
                w.shake = Math.max(w.shake, 10);
                w.soundAt(Snd.BOSS_PHASE2_LAB, x, y);
            }
            return;
        }
        if (chargeTimer > 0) {                                       // mid-charge: a fast, committed dash (stage two only)
            chargeTimer -= dt;
            x += chargeDx * CHARGE_SPEED * dt;
            y += chargeDy * CHARGE_SPEED * dt;
            if (!chargeHit && Util.dist(x, y, p.x, p.y) < radius + p.radius + 10) {
                p.hurt(w, damage * 0.7, chargeDx * 220, chargeDy * 220);
                chargeHit = true;
            }
            if (chargeTimer <= 0) {
                w.effects.add(Effect.ring(x, y, radius, radius + 50, 0.3, new Color(150, 255, 120), true));
                w.shake = Math.max(w.shake, 6);
                w.soundAt(Snd.BOSS_SLAM_LAB, x, y);
                state = State.RECOVER;
                stateTimer = 0.5;
            }
            return;
        }
        double pace = phase2 ? (lab ? 1.5 : 1.3) : 1.0;
        attackCd -= dt;
        bombCd -= dt;
        chargeCd -= dt;
        if (phase2 && lab) {                                         // stage two: a steady stream of regular enemies
            summonCd -= dt;
            if (summonCd <= 0 && state == State.CHASE && w.enemies.size() < 9) {
                Type[] wave = waves++ % 2 == 0 ? new Type[]{Type.GRUNT, Type.RUNNER, Type.SHOOTER} : new Type[]{Type.GRUNT, Type.GRUNT, Type.SHOOTER};
                for (int i = 0; i < wave.length; i++) {
                    double a = Math.PI * 2 * (i + waves * 0.3) / wave.length;
                    w.summon(wave[i], x + Math.cos(a) * 100, y + Math.sin(a) * 100);
                }
                summonCd = 8;
            }
        }

        switch (state) {
            case CHASE -> {
                x += ux * speed * pace * dt;
                y += uy * speed * pace * dt;
                if (lab && bombCd <= 0) {                                    // his bombs come first, wherever you are
                    attack = Attack.BOMBS;
                    state = State.WINDUP;
                    stateTimer = windupTotal = 0.8 / pace;
                    windupSound(w);
                } else if (dist - radius - p.radius < 70) {
                    attack = Attack.SLAM;
                    state = State.WINDUP;
                    stateTimer = windupTotal = type.windup / pace;
                    windupSound(w);
                } else if (phase2 && lab && chargeCd <= 0 && dist > 200) {    // stage two only: close the distance fast
                    attack = Attack.CHARGE;
                    state = State.WINDUP;
                    stateTimer = windupTotal = 0.5;
                    windupSound(w);
                } else if (attackCd <= 0 && dist > 170) {
                    attack = Attack.BURST;
                    state = State.WINDUP;
                    stateTimer = windupTotal = 0.9 / pace;
                    windupSound(w);
                }
            }
            case WINDUP -> {
                stateTimer -= dt;
                if (stateTimer <= 0) {
                    if (attack == Attack.SLAM) {
                        if (dist - radius - p.radius < strikeReach()) p.hurt(w, damage, x, y);
                        w.effects.add(Effect.ring(x, y, radius, radius + p.radius + strikeReach(), 0.3, new Color(255, 140, 90), true));
                        w.shake = Math.max(w.shake, 9);
                        w.soundAt(w.themed(Snd.BOSS_SLAM_FOREST, Snd.BOSS_SLAM_CITY, Snd.BOSS_SLAM_LAB), x, y);
                        stateTimer = 0.9;
                    } else if (attack == Attack.BOMBS) {
                        lobBombs(w, p);
                        bombCd = phase2 ? 6.0 : 9.0;
                        stateTimer = 0.7;
                    } else if (attack == Attack.CHARGE) {
                        double ang = Util.angleTo(x, y, p.x, p.y);
                        chargeDx = Math.cos(ang);
                        chargeDy = Math.sin(ang);
                        chargeHit = false;
                        chargeTimer = 0.35;
                        chargeCd = 6.5;
                        w.soundAt(w.themed(Snd.WINDUP_BIG_FOREST, Snd.WINDUP_BIG_CITY, Snd.WINDUP_BIG_LAB), x, y);
                        stateTimer = 0.1;                              // the chargeTimer branch above takes over next frame
                    } else {
                        fireBurst(w);
                        attackCd = phase2 ? (lab ? 2.5 : 3.0) : 4.5;
                        stateTimer = 0.7;
                    }
                    attack = Attack.NONE;
                    state = State.RECOVER;
                }
            }
            case RECOVER -> {
                stateTimer -= dt;
                if (stateTimer <= 0) state = State.CHASE;
            }
        }
    }

    /**
     * The mad scientist's stage-one bar has run out: not really dead (see {@link World#collectDead}). The bar reads
     * empty and he goes untouchable for a beat while four of his creations spring up around him; {@link #updateBoss}
     * refills it to a whole fresh {@link #maxHp} once that passes, and stage two begins.
     */
    void enterFinalStage(World w) {
        hp = 0.001;
        phase2 = true;
        stageTimer = 1.8;
        attack = Attack.NONE;
        state = State.RECOVER;
        w.banner = "STAGE 2";
        w.bannerTimer = 2.4;
        Type[] first = {Type.GRUNT, Type.RUNNER, Type.GRUNT, Type.SHOOTER};
        for (int i = 0; i < first.length; i++) {
            double a = Math.PI * 2 * i / first.length + 0.6;
            w.summon(first[i], x + Math.cos(a) * 120, y + Math.sin(a) * 120);
        }
        summonCd = 7;
        chargeCd = 4;
        bombCd = Math.min(bombCd, 3.5);
        attackCd = Math.max(attackCd, 2);
    }

    /** Flask bombs: one lands where you are standing, the rest around it, each marked on the floor for a moment first. */
    private void lobBombs(World w, Player p) {
        int count = phase2 ? 7 : 4;
        double delay = phase2 ? 0.95 : 1.1;
        for (int i = 0; i < count; i++) {
            double px = p.x, py = p.y;
            if (i > 0) {
                double a = w.rng.nextDouble() * Math.PI * 2, d = 70 + w.rng.nextDouble() * 190;
                Util.Vec v = w.level.clamp(p.x + Math.cos(a) * d, p.y + Math.sin(a) * d, 40);
                px = v.x();
                py = v.y();
            }
            w.blasts.add(new Blast(px, py, 72, delay + i * 0.09, damage * 0.8));
        }
        w.soundAt(Snd.BOSS_LOB_LAB, x, y);
    }

    /** A ring of slow bullets, aimed so that one of them heads straight for the player. */
    private void fireBurst(World w) {
        int count = phase2 ? 16 : 10;
        double start = Util.angleTo(x, y, w.player.x, w.player.y);
        for (int i = 0; i < count; i++) {
            double ang = start + i * Math.PI * 2 / count;
            w.projectiles.add(new Projectile(false, x + Math.cos(ang) * radius, y + Math.sin(ang) * radius,
                ang, 240, 8, damage * 0.5, 5));
        }
        w.effects.add(Effect.ring(x, y, radius, radius + 40, 0.3, new Color(255, 80, 120), false));
        w.soundAt(w.themed(Snd.BOSS_BURST_FOREST, Snd.BOSS_BURST_CITY, Snd.BOSS_BURST_LAB), x, y);
    }
}
