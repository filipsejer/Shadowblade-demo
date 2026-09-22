package game;

import java.util.ArrayList;
import java.util.List;

/** The behaviour and per-level numbers of every {@link Ability}. Arrays are indexed by (level - 1). */
final class Spells {
    private Spells() {}

    static final double[] FIRE_DAMAGE = {25, 32, 32, 38};
    static final double[] FIRE_RADIUS = {55, 60, 78, 90};
    static final int[] FIRE_SHOTS = {1, 1, 2, 3};
    static final double[] FIRE_BURN = {0, 7, 10, 12};      // damage per second for 3s

    static final double[] ICE_DAMAGE = {6, 9, 11, 14};    // per tick (0.4s)
    static final double[] ICE_RADIUS = {110, 130, 150, 175};
    static final double[] ICE_TIME = {3.0, 3.5, 4.0, 4.5};
    static final double[] ICE_SLOW = {0.55, 0.5, 0.4, 0.3}; // enemy speed multiplier
    static final double[] ICE_FREEZE = {0, 0, 0, 1.0};

    static final double[] BOLT_DAMAGE = {35, 42, 50, 62};
    static final int[] BOLT_TARGETS = {2, 3, 4, 5};
    static final double[] BOLT_STUN = {0.25, 0.3, 0.4, 0.5};

    static final double[] HEAL = {40, 58, 75, 95};

    /** @return false if the spell could not be cast (no target / nothing to heal), so no mana is spent. */
    static boolean cast(World w, Player p, Ability a, int level) {
        int i = level - 1;
        return switch (a) {
            case FIREBALL -> fireball(w, p, i);
            case ICE_STORM -> iceStorm(w, p, i);
            case LIGHTNING -> lightning(w, p, i);
            case HEAL -> heal(w, p, i);
        };
    }

    private static boolean fireball(World w, Player p, int i) {
        Enemy target = w.target(p.x, p.y, 900);   // the locked enemy at any distance, else the nearest
        double base = target != null ? Util.angleTo(p.x, p.y, target.x, target.y) : p.facing;
        p.facing = base;
        w.sound(Snd.CAST_FIRE);
        int shots = FIRE_SHOTS[i];
        for (int s = 0; s < shots; s++) {
            double ang = base + (s - (shots - 1) / 2.0) * 0.24;
            Projectile f = new Projectile(true, p.x + Math.cos(ang) * 22, p.y + Math.sin(ang) * 22,
                ang, 520, 10, FIRE_DAMAGE[i] * p.spellPower, 1.5);
            f.aoe = FIRE_RADIUS[i];
            f.burnDps = FIRE_BURN[i] * p.spellPower;
            w.projectiles.add(f);
        }
        return true;
    }

    private static boolean iceStorm(World w, Player p, int i) {
        Enemy target = w.target(p.x, p.y, 520);
        double cx, cy;
        if (target != null) {
            // a locked enemy beyond casting range gets the storm placed as far toward it as we can reach
            double d = Util.dist(p.x, p.y, target.x, target.y);
            double reach = Math.min(1, 520 / Math.max(1, d));
            cx = p.x + (target.x - p.x) * reach;
            cy = p.y + (target.y - p.y) * reach;
        } else {
            cx = p.x + Math.cos(p.facing) * 150;
            cy = p.y + Math.sin(p.facing) * 150;
        }
        Util.Vec inside = w.level.clamp(cx, cy, 0);   // keep the storm on walkable ground
        cx = inside.x();
        cy = inside.y();
        double radius = ICE_RADIUS[i];
        w.zones.add(new Zone(cx, cy, radius, ICE_TIME[i], ICE_DAMAGE[i] * p.spellPower, ICE_SLOW[i]));
        w.soundAt(Snd.CAST_ICE, cx, cy);
        w.effects.add(Effect.ring(cx, cy, 10, radius, 0.35, Ability.ICE_STORM.color, true));
        if (ICE_FREEZE[i] > 0) {
            for (Enemy e : w.enemies) {
                if (e.targetable() && !e.type.armored && Util.dist(cx, cy, e.x, e.y) <= radius + e.radius) e.stun = Math.max(e.stun, ICE_FREEZE[i]);
            }
        }
        return true;
    }

    private static boolean lightning(World w, Player p, int i) {
        Enemy current = w.target(p.x, p.y, 520);
        if (current == null || Util.dist(p.x, p.y, current.x, current.y) > 520) return false; // locked target out of range
        p.facing = Util.angleTo(p.x, p.y, current.x, current.y);

        List<Enemy> struck = new ArrayList<>();
        List<double[]> points = new ArrayList<>();
        points.add(new double[]{p.x, p.y});
        double dmg = BOLT_DAMAGE[i] * p.spellPower;
        for (int n = 0; n < BOLT_TARGETS[i] && current != null; n++) {
            struck.add(current);
            points.add(new double[]{current.x, current.y});
            double ang = Util.angleTo(points.get(points.size() - 2)[0], points.get(points.size() - 2)[1], current.x, current.y);
            current.hurt(w, dmg, Math.cos(ang) * 90, Math.sin(ang) * 90, BOLT_STUN[i], Ability.LIGHTNING.color);
            w.effects.add(Effect.particle(current.x, current.y, 0, 0, 0.3, "fx.impact", -1, 1, 0, 3));
            w.soundAt(Snd.ZAP_HIT, current.x, current.y, 0.04 * n, 1, 1);     // the zap jumps from one enemy to the next
            w.smashNear(current.x, current.y, 60);                              // the bolt splinters any crate beside the enemy it strikes
            dmg *= 0.8;

            Enemy next = null;
            double best = 200;
            for (Enemy e : w.enemies) {
                if (!e.targetable() || struck.contains(e)) continue;
                double d = Util.dist(current.x, current.y, e.x, e.y);
                if (d < best) { best = d; next = e; }
            }
            current = next;
        }

        double[] xs = new double[points.size()];
        double[] ys = new double[points.size()];
        for (int k = 0; k < xs.length; k++) {
            xs[k] = points.get(k)[0];
            ys[k] = points.get(k)[1];
        }
        w.effects.add(Effect.bolt(xs, ys, Ability.LIGHTNING.color, w.rng));
        w.sound(Snd.CAST_BOLT);
        w.shake = Math.max(w.shake, 3);
        return true;
    }

    private static boolean heal(World w, Player p, int i) {
        if (p.hp >= p.maxHp) return false;
        p.heal(w, HEAL[i]);
        w.sound(Snd.CAST_HEAL);
        w.effects.add(Effect.ring(p.x, p.y, 10, 60, 0.45, Ability.HEAL.color, true));
        for (int k = 0; k < 9; k++) {                               // plus signs rising around you
            double a = k * Math.PI * 2 / 9 + w.rng.nextDouble();
            w.effects.add(Effect.particle(p.x + Math.cos(a) * 26, p.y + 6 + Math.sin(a) * 10, 0, 0, 0.7 + w.rng.nextDouble() * 0.4, "fx.plus", -1, 0.4, 70 + w.rng.nextDouble() * 40, 3));
        }
        return true;
    }
}
