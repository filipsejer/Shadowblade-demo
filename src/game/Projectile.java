package game;

final class Projectile {
    final boolean friendly;
    double x, y, vx, vy;
    double radius, life, damage;
    /** Fireball only: explosion radius and burn damage per second (0 = no burn). */
    double aoe, burnDps;

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
