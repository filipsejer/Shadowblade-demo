package game;

/**
 * A flask bomb the mad scientist lobs: a marked spot on the floor that, after {@code delay} seconds, bursts and hurts anyone
 * standing in it. Rolling through it (or just being elsewhere) is safe. Once burst it lingers for a moment so the flash can
 * be drawn.
 */
final class Blast {
    static final double LINGER = 0.35;

    final double x, y, radius, total, damage;
    double delay;               // seconds until it bursts
    double after = LINGER;      // seconds left to be drawn once burst
    boolean burst;

    Blast(double x, double y, double radius, double delay, double damage) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.delay = delay;
        this.total = delay;
        this.damage = damage;
    }
}
