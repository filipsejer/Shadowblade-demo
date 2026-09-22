package game;

import java.awt.Color;

/** The spells in the Magic menu. Numbers per level live in {@link Spells}; text here must match them. */
enum Ability {
    FIREBALL("Fireball", new Color(255, 140, 30),
        new double[]{25, 25, 28, 32}, 0.8,
        "Hurls an exploding fireball at the nearest enemy.",
        new String[]{
            "Fireballs hit harder and set enemies ablaze.",
            "Bigger explosions and two fireballs per cast.",
            "Three fireballs in a spread, even bigger blasts."}),
    ICE_STORM("Ice Storm", new Color(120, 200, 255),
        new double[]{45, 45, 50, 55}, 6.0,
        "Summons a freezing storm that damages and slows enemies.",
        new String[]{
            "Wider storm and more damage.",
            "Lasts longer and slows enemies harder.",
            "Enormous storm that flash-freezes enemies on cast."}),
    LIGHTNING("Lightning Bolt", new Color(255, 240, 90),
        new double[]{35, 35, 38, 42}, 1.8,
        "Stuns the nearest enemy with a bolt that chains to others nearby.",
        new String[]{
            "Chains to 3 targets with more damage.",
            "Chains to 4 targets and stuns them longer.",
            "Chains to 5 targets for devastating damage."}),
    HEAL("Healing", new Color(110, 230, 130),
        new double[]{50, 46, 42, 38}, 9.0,
        "Restores a chunk of your health.",
        new String[]{
            "Heals more.",
            "Heals even more and costs less.",
            "Greatly increased healing for a low cost."});

    static final int MAX_LEVEL = 4;

    final String label;
    final Color color;
    private final double[] cost;
    final double cooldown;
    final String blurb;
    /** upgrades[i] describes what level i+2 adds. */
    final String[] upgrades;

    Ability(String label, Color color, double[] cost, double cooldown, String blurb, String[] upgrades) {
        this.label = label;
        this.color = color;
        this.cost = cost;
        this.cooldown = cooldown;
        this.blurb = blurb;
        this.upgrades = upgrades;
    }

    double cost(int level) { return cost[Math.min(level, MAX_LEVEL) - 1]; }
}
