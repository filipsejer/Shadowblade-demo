package game;

import java.awt.Color;

/**
 * The permanent upgrades Ranger Ash teaches at Mossbrook's training post, bought with skill points. Unlike a fight's
 * level-up {@link Perk}s (which start again from nothing every fight) these stay with you, and every fight starts with
 * them in place: {@link #applyAll} runs on the fresh hero {@link Run} makes.
 *
 * <p>Rank {@code r} costs {@code r} skill points: the first steps are cheap, mastering one takes a while.
 */
enum Mastery {
    VITALITY("Vitality", new Color(110, 220, 120), 5, "+15 max HP per rank.", "Hardier in every fight."),
    MIGHT("Might", new Color(255, 170, 90), 5, "+8% melee damage per rank.", "Your sword hits harder."),
    FOCUS("Focus", new Color(190, 120, 255), 5, "+8% skill damage per rank.", "Your skills hit harder."),
    TOUGHNESS("Toughness", new Color(170, 180, 200), 3, "Take 4% less damage per rank.", "Shrug off blows."),
    SWIFTNESS("Swiftness", new Color(120, 230, 200), 3, "+5% move speed per rank.", "Outrun the horde."),
    REACH("Gatherer", new Color(120, 170, 255), 3, "+20% pickup range per rank.", "Gems and gold come to you from further away."),
    INSIGHT("Insight", new Color(120, 200, 255), 3, "+10% XP gained per rank.", "Level up faster in a fight."),
    FORTUNE("Fortune", new Color(255, 214, 80), 3, "+12% gold found per rank.", "More gold from every fight."),
    TUMBLER("Tumbler", new Color(150, 230, 255), 1, "Every fight starts with Evasive Roll.", "No waiting for a level-up to dodge."),
    HEAD_START("Head Start", new Color(255, 230, 140), 2, "Every fight starts with one free level-up choice per rank.", "A skill before the first monster arrives."),
    SECOND_WIND("Second Wind", new Color(255, 150, 60), 1, "Once per fight, rise again at half health.", "One mistake won't end it."),
    REROLLS("Second Thoughts", new Color(200, 210, 255), 2, "+1 reroll of the level-up cards per fight, per rank.", "Don't like the cards? Draw again.");

    final String label;
    final Color color;
    final int maxRank;
    /** What one rank does, and a short line of flavour. */
    final String effect, flavor;

    Mastery(String label, Color color, int maxRank, String effect, String flavor) {
        this.label = label;
        this.color = color;
        this.maxRank = maxRank;
        this.effect = effect;
        this.flavor = flavor;
    }

    /** Skill points the next rank costs, from rank {@code now}. */
    int cost(int now) { return now + 1; }

    /** Puts every rank you hold onto a fresh fighter (stats only; the starting perks and extras are read by {@link Run}). */
    static void applyAll(Player p, int[] ranks) {
        for (Mastery m : values()) {
            int r = ranks[m.ordinal()];
            if (r == 0) continue;
            switch (m) {
                case VITALITY -> { p.maxHp += 15 * r; p.hp = p.maxHp; }
                case MIGHT -> p.meleeMult += 0.08 * r;
                case FOCUS -> p.spellPower += 0.08 * r;
                case TOUGHNESS -> p.armor = Math.min(0.6, p.armor + 0.04 * r);
                case SWIFTNESS -> p.moveSpeed *= 1 + 0.05 * r;
                case REACH -> p.magnet *= 1 + 0.2 * r;
                case INSIGHT -> p.xpMult += 0.1 * r;
                case FORTUNE -> p.goldMult += 0.12 * r;
                default -> { }
            }
        }
    }
}
