package game;

import java.awt.Color;
import java.util.List;

/**
 * Something you protect in a Stormcliff fight: Copper the surveyor robot, rolling along a route across the East Wing
 * ({@link Challenge.Goal#ESCORT}), or Ilse's stasis engine, charging up in the Observatory
 * ({@link Challenge.Goal#DEFEND}). Some of the horde goes for it instead of you (until you hit them). Knocked down to
 * nothing it isn't lost, just broken: it stops, and you have to stand by it a few seconds to get it going again,
 * while the danger clock keeps ticking. {@link Run} directs all of that; this is its state, and its route.
 */
final class Ward {
    enum Kind { ROBOT, ENGINE }

    /** Copper's pace, and how close you must be for him to roll on. */
    static final double SPEED = 78, FOLLOW = 380;
    /** How long he takes to cut through a wall of vines. */
    static final double WORK_TIME = 13;
    /** How long the engine takes to charge, unbroken. */
    static final double CHARGE_TIME = 115;
    /** Standing this close to a broken ward for {@link #REPAIR_TIME} seconds gets it going again (at half health). */
    static final double REPAIR_RANGE = 140, REPAIR_TIME = 4;
    /** After this long without being hit, it mends itself a little each second. */
    static final double MEND_AFTER = 5, MEND = 0.02;
    /** The engine's cold: monsters this close to it are slowed. */
    static final double CHILL = 230;
    static final Color COPPER_LIGHT = new Color(255, 190, 110), FROST = new Color(150, 220, 255);

    final Kind kind;
    final double radius;
    double x, y;
    double hp, maxHp;
    /** Knocked out: no longer a target, and being repaired. */
    boolean broken;
    /** How far through the repair, 0..1. */
    double repair;
    /** Seconds since it was last hit, and a hit flash. */
    double sinceHit = 99, flash;
    boolean faceLeft;

    // the robot: his route, how far along it he is, and the walls of vines on the way (distances along the route)
    final List<Util.Vec> route;
    final double length;
    final double[] stops;
    double along;
    int nextStop;
    /** Seconds left cutting through the vines in front of him (0 when not cutting). */
    double work;
    /** Rolling this frame (you're with him). */
    boolean moving;

    // the engine
    double charge;
    int surges;

    private Ward(Kind kind, double x, double y, double hp, List<Util.Vec> route, double[] stops) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.maxHp = this.hp = hp;
        this.radius = kind == Kind.ROBOT ? 22 : 44;
        this.route = route;
        this.stops = stops;
        double len = 0;
        for (int i = 1; i < route.size(); i++) len += Util.dist(route.get(i - 1).x(), route.get(i - 1).y(), route.get(i).x(), route.get(i).y());
        this.length = len;
    }

    /** Copper at the start of his route, with {@code walls} walls of vines spread evenly along it. */
    static Ward robot(List<Util.Vec> route, int walls, double hp) {
        double[] stops = new double[walls];
        Ward w = new Ward(Kind.ROBOT, route.get(0).x(), route.get(0).y(), hp, route, stops);
        for (int i = 0; i < walls; i++) stops[i] = w.length * (i + 1) / (walls + 1);
        return w;
    }

    static Ward engine(double x, double y, double hp) {
        return new Ward(Kind.ENGINE, x, y, hp, List.of(new Util.Vec(x, y)), new double[0]);
    }

    /** Standing and whole: the horde can go for it. */
    boolean targetable() { return !broken && hp > 0; }

    /** How far the goal has got, 0..1: along the route, or charged. */
    double progress() { return kind == Kind.ROBOT ? (length <= 0 ? 1 : Math.min(1, along / length)) : Math.min(1, charge); }

    /** Where a wall of vines stands (distance {@code d} along the route). */
    Util.Vec pointAt(double d) {
        double left = d;
        for (int i = 1; i < route.size(); i++) {
            Util.Vec a = route.get(i - 1), b = route.get(i);
            double seg = Util.dist(a.x(), a.y(), b.x(), b.y());
            if (left <= seg) {
                double t = seg <= 0 ? 0 : left / seg;
                return new Util.Vec(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t);
            }
            left -= seg;
        }
        return route.get(route.size() - 1);
    }

    /** Rolls {@code d} further along the route. */
    void advance(double d) {
        along = Math.min(length, along + d);
        Util.Vec v = pointAt(along);
        if (Math.abs(v.x() - x) > 0.01) faceLeft = v.x() < x;
        x = v.x();
        y = v.y();
    }

    /** A blow from a monster. True if it was the one that broke it. */
    boolean hurt(World w, double dmg) {
        if (!targetable()) return false;
        hp -= dmg;
        flash = 0.12;
        sinceHit = 0;
        w.effects.add(Effect.text(x + (w.rng.nextDouble() - 0.5) * 20, y - (kind == Kind.ROBOT ? 70 : 120),
            String.valueOf(Math.max(1, (int) Math.round(dmg))), new Color(255, 150, 110), false));
        if (hp > 0) return false;
        hp = 0;
        broken = true;
        repair = 0;
        return true;
    }
}
