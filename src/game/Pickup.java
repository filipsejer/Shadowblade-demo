package game;

/**
 * Something lying on the ground in a roguelike run, waiting to be walked over: XP gems (dropped by every enemy), gold,
 * and the rarer things crates hold. Gems and gold are pulled in once you're within your pickup range; the rest you have
 * to actually step on. Chests are dropped by elites and bosses; the portal takes you to the next stage.
 */
final class Pickup {
    enum Kind {
        GEM, COIN, HEART, MAGNET, BOMB, ELITE_CHEST, BOSS_CHEST, PORTAL;

        /** Pulled toward you by your pickup range (the others need to be touched). */
        boolean magnetic() { return this == GEM || this == COIN; }
    }

    final Kind kind;
    double x, y;
    /** XP for a gem, gold for a coin. */
    int value;
    boolean attracted;
    double speed;
    /** Seconds since it appeared: things pop out of the enemy that dropped them, and bob. */
    double age;
    double vx, vy;

    Pickup(Kind kind, double x, double y, int value) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.value = value;
    }

    /** Gem size (0 small blue, 1 green, 2 big red) from how much XP it holds — also which sprite it wears. */
    int gemTier() { return value >= 60 ? 2 : value >= 15 ? 1 : 0; }

    double radius() {
        return switch (kind) {
            case ELITE_CHEST, BOSS_CHEST -> 30;
            case PORTAL -> 46;
            default -> 18;
        };
    }
}
