package game;

/**
 * A crate or a barrel standing in a room. It is solid (you and the enemies walk around it), goes in one hit from a sword swing, a
 * Fireball blast or a Lightning bolt, and pays a little XP when it breaks.
 */
final class Breakable {
    enum Kind {
        CRATE(4, 22), BARREL(6, 19);

        final int baseXp;
        final double radius;

        Kind(int baseXp, double radius) {
            this.baseXp = baseXp;
            this.radius = radius;
        }
    }

    final Kind kind;
    final double x, y;           // where it stands (its base)
    final double radius;
    final int xp;
    boolean broken;

    /** {@code xpScale}: 1 in level 1, more in the later levels, like the enemies' rewards. */
    Breakable(Kind kind, double x, double y, double xpScale) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = kind.radius;
        this.xp = (int) Math.round(kind.baseXp * xpScale);
    }
}
