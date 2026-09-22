package game;

/** A lingering ground area (Ice Storm) that ticks damage and slows enemies inside it. */
final class Zone {
    static final double TICK = 0.4;

    final double x, y, radius, maxLife, damage, slowMul;
    double life;
    double tick = 0;

    Zone(double x, double y, double radius, double life, double damage, double slowMul) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.life = life;
        this.maxLife = life;
        this.damage = damage;
        this.slowMul = slowMul;
    }
}
