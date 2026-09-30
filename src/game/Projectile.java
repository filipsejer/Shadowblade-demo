package game;

final class Projectile {
    final boolean friendly;
    double x, y, vx, vy;
    double radius, life, damage;
    /** Fireball only: explosion radius and burn damage per second (0 = no burn). */
    double aoe, burnDps;
    /** Drawing size (1 = normal): a run's evolved skills throw bigger ones. */
    double scale = 1;
    /** A run's Crescent Wave: passes through enemies (up to {@link #pierce} of them) instead of exploding, hitting each once. */
    boolean wave;
    int pierce;
    final java.util.List<Enemy> struck = new java.util.ArrayList<>();

    Projectile(boolean friendly, double x, double y, double angle, double speed, double radius, double damage, double life) {
        this.friendly = friendly;
        this.x = x;
        this.y = y;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.radius = radius;
        this.damage = damage;
        this.life = life;
    }
}
