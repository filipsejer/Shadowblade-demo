package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A fight's skills: every {@link Perk.Kind#SKILL} you've picked fires on its own, on its own cooldown, at
 * whatever is nearest — the sword stays yours to swing. Numbers per rank are the arrays below, indexed by rank (index 0
 * unused, 6 = evolved); the text in {@link Perk} describes them, so keep the two in step.
 */
final class Arsenal {
    private Arsenal() {}

    //                                              rank:  -   1    2    3    4    5   evolved
    static final double[] FIRE_CD =     {0, 1.8, 1.7, 1.7, 1.6, 1.5, 1.0};
    static final double[] FIRE_DMG =    {0, 22, 30, 30, 34, 40, 55};
    static final int[] FIRE_SHOTS =     {0, 1, 1, 2, 2, 3, 5};
    static final double[] FIRE_AOE =    {0, 55, 58, 62, 66, 80, 110};
    static final double[] FIRE_BURN =   {0, 0, 0, 0, 8, 10, 16};

    static final double[] BOLT_CD =     {0, 2.6, 2.5, 2.4, 2.0, 1.9, 1.2};
    static final double[] BOLT_DMG =    {0, 28, 32, 40, 44, 50, 60};
    static final int[] BOLT_CHAIN =     {0, 2, 3, 4, 4, 6, 6};
    static final double[] BOLT_STUN =   {0, 0.25, 0.25, 0.3, 0.5, 0.5, 0.5};

    static final double[] ICE_CD =      {0, 7, 6.5, 6.5, 6, 5.5, 4.5};
    static final double[] ICE_RADIUS =  {0, 90, 115, 120, 125, 160, 190};
    static final double[] ICE_DMG =     {0, 5, 7, 8, 9, 12, 16};
    static final double[] ICE_TIME =    {0, 3, 3, 4, 4, 4.5, 5};
    static final double[] ICE_SLOW =    {0, 0.6, 0.55, 0.4, 0.4, 0.35, 0.25};
    static final int[] ICE_COUNT =      {0, 1, 1, 1, 2, 2, 3};
    static final double[] ICE_FREEZE =  {0, 0, 0, 0, 0, 0.8, 1.2};

    static final int[] ORBIT_COUNT =    {0, 2, 3, 3, 4, 5, 8};
    static final double[] ORBIT_RADIUS ={0, 72, 76, 80, 92, 96, 110};
    static final double[] ORBIT_DMG =   {0, 10, 12, 17, 19, 26, 30};
    static final double[] ORBIT_SPEED = {0, 3.0, 3.2, 3.9, 4.1, 4.4, 7.0};
    static final double ORBIT_HIT_GAP = 0.4;

    static final double[] AURA_RADIUS = {0, 75, 95, 100, 125, 130, 175};
    static final double[] AURA_DMG =    {0, 4, 5, 8, 9, 13, 16};           // per tick
    static final double AURA_TICK = 0.5;

    static final double[] HEAL_CD =     {0, 16, 13, 10};
    static final double[] HEAL_FRAC =   {0, 0.20, 0.28, 0.36};

    static final double[] WAVE_DMG =    {0, 0.6, 0.8, 0.8, 0.9, 1.1, 1.4};  // times the sword's damage
    static final int[] WAVE_PIERCE =    {0, 2, 3, 3, 4, 99, 99};
    static final double[] WAVE_SIZE =   {0, 1, 1, 1, 1.3, 1.3, 1.8};
    static final double[] WAVE_LIFE =   {0, 0.45, 0.5, 0.5, 0.65, 0.7, 0.8};
    static final double WAVE_SPEED = 640, WAVE_GAP = 0.22;

    static final Color CRIT = new Color(255, 214, 60);

    static int rank(Player p, Perk k) { return p.perk[k.ordinal()]; }

    /** The base cooldown of a skill at its current rank (before Quick Casting), for the HUD's recharge sweep. */
    static double baseCooldown(Player p, Perk k) {
        int r = rank(p, k);
        if (r == 0) return 0;
        return switch (k) {
            case FIREBALL -> FIRE_CD[r];
            case LIGHTNING -> BOLT_CD[r];
            case ICE_STORM -> ICE_CD[r];
            case HEALING -> HEAL_CD[r];
            default -> 0;
        };
    }

    /** Hurts an enemy with a skill: skill power applied, and a chance to crit for extra damage in gold. */
    static void strike(World w, Player p, Enemy e, double base, double kbx, double kby, double stun, Color c) {
        boolean crit = w.rng.nextDouble() < p.critChance;
        double dmg = base * p.spellPower * (crit ? p.critMult : 1) * (0.92 + w.rng.nextDouble() * 0.16);
        e.hurt(w, dmg, kbx, kby, stun, crit ? CRIT : c, true, crit);
    }

    /** Called every frame of a run: counts cooldowns down and fires whatever is ready. */
    static void update(World w, Player p, double dt) {
        p.waveCd = Math.max(0, p.waveCd - dt);
        for (Perk k : Perk.values()) {
            if (k.kind != Perk.Kind.SKILL || rank(p, k) == 0) continue;
            int i = k.ordinal();
            p.skillCd[i] = Math.max(0, p.skillCd[i] - dt);
            if (p.skillCd[i] > 0) continue;
            boolean fired = switch (k) {
                case FIREBALL -> fireball(w, p, rank(p, k));
                case LIGHTNING -> lightning(w, p, rank(p, k));
                case ICE_STORM -> iceStorm(w, p, rank(p, k));
                case HEALING -> heal(w, p, rank(p, k));
                default -> false;
            };
            // nothing to aim at yet: try again shortly rather than wasting a whole cooldown
            p.skillCd[i] = fired ? baseCooldown(p, k) * p.cooldownMult : 0.25;
        }
        if (rank(p, Perk.ORBIT_BLADES) > 0) orbit(w, p, rank(p, Perk.ORBIT_BLADES), dt);
        if (rank(p, Perk.HOLY_AURA) > 0) aura(w, p, rank(p, Perk.HOLY_AURA), dt);
    }

    // ------------------------------------------------------------------ fireball

    private static boolean fireball(World w, Player p, int r) {
        Enemy target = w.target(p.x, p.y, 650);
        if (target == null) return false;
        double base = Util.angleTo(p.x, p.y, target.x, target.y);
        int shots = FIRE_SHOTS[r];
        double spread = shots >= 5 ? 0.2 : 0.24;
        for (int s = 0; s < shots; s++) {
            double ang = base + (s - (shots - 1) / 2.0) * spread;
            Projectile f = new Projectile(true, p.x + Math.cos(ang) * 22, p.y + Math.sin(ang) * 22, ang, 540, 10,
                FIRE_DMG[r] * p.spellPower * critRoll(w, p), 1.4);
            f.aoe = FIRE_AOE[r];
            f.burnDps = FIRE_BURN[r] * p.spellPower;
            f.scale = r >= Perk.EVOLVED ? 1.6 : 1;
            w.projectiles.add(f);
        }
        w.sound(Snd.CAST_FIRE);
        return true;
    }

    private static double critRoll(World w, Player p) {
        return w.rng.nextDouble() < p.critChance ? p.critMult : 1;
    }

    // ------------------------------------------------------------------ lightning

    private static boolean lightning(World w, Player p, int r) {
        List<Enemy> used = new ArrayList<>();
        int chains = r >= Perk.EVOLVED ? 3 : 1;
        boolean any = false;
        for (int c = 0; c < chains; c++) {
            Enemy first = c == 0 ? w.target(p.x, p.y, 520) : randomNear(w, p.x, p.y, 520, used);
            if (first == null || Util.dist(p.x, p.y, first.x, first.y) > 520 || used.contains(first)) break;
            chain(w, p, r, first, used, c);
            any = true;
        }
        if (any) {
            w.sound(Snd.CAST_BOLT);
            w.shake = Math.max(w.shake, 3);
        }
        return any;
    }

    private static void chain(World w, Player p, int r, Enemy current, List<Enemy> used, int n0) {
        List<double[]> points = new ArrayList<>();
        points.add(new double[]{p.x, p.y - 20});
        double dmg = BOLT_DMG[r];
        for (int n = 0; n < BOLT_CHAIN[r] && current != null; n++) {
            used.add(current);
            double[] prev = points.get(points.size() - 1);
            points.add(new double[]{current.x, current.y - current.z});
            double ang = Util.angleTo(prev[0], prev[1], current.x, current.y);
            strike(w, p, current, dmg, Math.cos(ang) * 90, Math.sin(ang) * 90, BOLT_STUN[r], Perk.LIGHTNING.color);
            w.effects.add(Effect.particle(current.x, current.y, 0, 0, 0.3, "fx.impact", -1, 1, 0, 3));
            w.soundAt(Snd.ZAP_HIT, current.x, current.y, 0.04 * (n + n0), 1, 1);
            w.smashNear(current.x, current.y, 50);
            dmg *= 0.85;
            Enemy next = null;
            double best = 220;
            for (Enemy e : w.enemies) {
                if (!e.targetable() || used.contains(e)) continue;
                double d = Util.dist(current.x, current.y, e.x, e.y);
                if (d < best) { best = d; next = e; }
            }
            current = next;
        }
        double[] xs = new double[points.size()], ys = new double[points.size()];
        for (int k = 0; k < xs.length; k++) { xs[k] = points.get(k)[0]; ys[k] = points.get(k)[1]; }
        w.effects.add(Effect.bolt(xs, ys, Perk.LIGHTNING.color, w.rng));
    }

    /** One of the (up to) eight nearest solid enemies within range, not in {@code skip}. */
    private static Enemy randomNear(World w, double x, double y, double range, List<Enemy> skip) {
        List<Enemy> near = new ArrayList<>();
        for (Enemy e : w.enemies) if (e.targetable() && !skip.contains(e) && Util.dist(x, y, e.x, e.y) <= range) near.add(e);
        if (near.isEmpty()) return null;
        near.sort((a, b) -> Double.compare(Util.dist(x, y, a.x, a.y), Util.dist(x, y, b.x, b.y)));
        return near.get(w.rng.nextInt(Math.min(8, near.size())));
    }

    // ------------------------------------------------------------------ ice storm

    private static boolean iceStorm(World w, Player p, int r) {
        List<Enemy> used = new ArrayList<>();
        boolean any = false;
        for (int s = 0; s < ICE_COUNT[r]; s++) {
            Enemy t = randomNear(w, p.x, p.y, 520, used);
            if (t == null) break;
            used.add(t);
            Util.Vec at = w.level.clamp(t.x, t.y, 0);
            double radius = ICE_RADIUS[r];
            w.zones.add(new Zone(at.x(), at.y(), radius, ICE_TIME[r], ICE_DMG[r] * p.spellPower, ICE_SLOW[r]));
            w.effects.add(Effect.ring(at.x(), at.y(), 10, radius, 0.35, Perk.ICE_STORM.color, true));
            w.soundAt(Snd.CAST_ICE, at.x(), at.y());
            if (ICE_FREEZE[r] > 0) {
                for (Enemy e : w.enemies) {
                    if (e.targetable() && !e.type.armored && Util.dist(at.x(), at.y(), e.x, e.y) <= radius + e.radius) e.stun = Math.max(e.stun, ICE_FREEZE[r]);
                }
            }
            any = true;
        }
        return any;
    }

    // ------------------------------------------------------------------ healing

    private static boolean heal(World w, Player p, int r) {
        if (p.hp > p.maxHp * 0.6 || p.hp <= 0) return false;
        p.heal(w, p.maxHp * HEAL_FRAC[r]);
        w.sound(Snd.CAST_HEAL);
        w.effects.add(Effect.ring(p.x, p.y, 10, 60, 0.45, Perk.HEALING.color, true));
        for (int k = 0; k < 9; k++) {
            double a = k * Math.PI * 2 / 9 + w.rng.nextDouble();
            w.effects.add(Effect.particle(p.x + Math.cos(a) * 26, p.y + 6 + Math.sin(a) * 10, 0, 0, 0.7 + w.rng.nextDouble() * 0.4, "fx.plus", -1, 0.4, 70 + w.rng.nextDouble() * 40, 3));
        }
        return true;
    }

    // ------------------------------------------------------------------ orbit blades

    /** Where blade {@code i} of {@code n} is right now. */
    static Util.Vec bladeAt(Player p, int i, int n, double radius) {
        double a = p.orbitAngle + i * Math.PI * 2 / n;
        return new Util.Vec(p.x + Math.cos(a) * radius, p.y - 10 + Math.sin(a) * radius * 0.8);
    }

    private static void orbit(World w, Player p, int r, double dt) {
        p.orbitAngle += ORBIT_SPEED[r] * dt;
        int n = ORBIT_COUNT[r];
        for (Enemy e : w.enemies) e.orbitCd = Math.max(0, e.orbitCd - dt);
        for (int i = 0; i < n; i++) {
            Util.Vec b = bladeAt(p, i, n, ORBIT_RADIUS[r]);
            for (Enemy e : w.enemies) {
                if (e.orbitCd > 0 || !e.targetable()) continue;
                if (Util.dist(b.x(), b.y(), e.x, e.y - e.z) > e.radius + 18) continue;
                double ang = Util.angleTo(p.x, p.y, e.x, e.y);
                strike(w, p, e, ORBIT_DMG[r], Math.cos(ang) * 160, Math.sin(ang) * 160, 0.1, ORBIT_TEXT);
                e.orbitCd = ORBIT_HIT_GAP;
                w.soundAt(Snd.HIT_LIGHT, e.x, e.y, 0, 0.5, 1.3);
                w.effects.add(Effect.particle(e.x, e.y - e.radius * 0.3 - e.z, 0, 0, 0.2, "fx.spark", -1, 1, 0, 2));
            }
            for (Breakable c : w.level.breakables) if (!c.broken && Util.dist(b.x(), b.y(), c.x, c.y - 8) < c.radius + 14) w.smash(c);
        }
    }

    private static final Color ORBIT_TEXT = new Color(200, 220, 255);

    // ------------------------------------------------------------------ holy aura

    private static void aura(World w, Player p, int r, double dt) {
        p.auraTick -= dt;
        if (p.auraTick > 0) return;
        p.auraTick += AURA_TICK;
        double radius = AURA_RADIUS[r];
        int healed = 0;
        for (Enemy e : w.enemies) {
            if (!e.targetable() || Util.dist(p.x, p.y, e.x, e.y) > radius + e.radius * 0.5) continue;
            strike(w, p, e, AURA_DMG[r], 0, 0, 0, Perk.HOLY_AURA.color);
            if (r >= 3) e.slow(0.7, 0.6);
            if (r >= Perk.EVOLVED && healed < 3 && p.hp < p.maxHp) { p.hp = Math.min(p.maxHp, p.hp + 0.5); healed++; }
        }
    }

    static double auraRadius(Player p) {
        int r = rank(p, Perk.HOLY_AURA);
        return r == 0 ? 0 : AURA_RADIUS[r];
    }

    // ------------------------------------------------------------------ crescent wave

    /** A sword swing just started: with Crescent Wave, it sends a piercing wave (or a fan of three) the way you face. */
    static void onSwing(World w, Player p, boolean finisher) {
        int r = rank(p, Perk.CRESCENT_WAVE);
        if (r == 0 || p.waveCd > 0) return;
        p.waveCd = WAVE_GAP / p.attackSpeed;
        int count = r >= Perk.EVOLVED || (finisher && r >= 3) ? 3 : 1;
        double dmg = p.meleeDamage * p.meleeMult * WAVE_DMG[r];
        for (int i = 0; i < count; i++) {
            double ang = p.facing + (i - (count - 1) / 2.0) * 0.32;
            Projectile wave = new Projectile(true, p.x + Math.cos(ang) * 20, p.y - 8 + Math.sin(ang) * 20, ang, WAVE_SPEED,
                22 * WAVE_SIZE[r], dmg * critRoll(w, p), WAVE_LIFE[r]);
            wave.wave = true;
            wave.pierce = WAVE_PIERCE[r];
            wave.scale = WAVE_SIZE[r];
            w.projectiles.add(wave);
        }
    }
}
