package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Everything a level-up can offer in a fight. {@link Kind#SKILL}s are weapons that fire on their own (see
 * {@link Arsenal}), up to {@value #SKILL_SLOTS} of them, each with five ranks — and a sixth, its <em>evolution</em>,
 * offered once the skill is at rank 5 and you also own its {@link #partner} passive. {@link Kind#PASSIVE}s are stat
 * boosts, up to {@value #PASSIVE_SLOTS} different ones. A run starts with nothing but the sword: even the roll is a pick.
 */
enum Perk {
    // ---------------------------------------------------------------- skills (fire by themselves)
    CRESCENT_WAVE(Kind.SKILL, "Crescent Wave", new Color(150, 200, 255), 5, "Moonlit Crescent",
        "Every sword swing also sends a piercing wave of light ahead of you.",
        new String[]{
            "Swings fire a piercing wave (60% of melee damage).",
            "Waves hit harder (80%) and pierce more.",
            "Finishers fire three waves in a fan.",
            "Waves are bigger and travel further.",
            "Waves deal 110% melee damage and pierce everything."},
        "Every swing fires a fan of three huge waves that pierce everything."),
    FIREBALL(Kind.SKILL, "Fireball", new Color(255, 140, 30), 5, "Inferno Comet",
        "Hurls an exploding fireball at the nearest enemy.",
        new String[]{
            "Throws a fireball every 1.8s.",
            "Harder-hitting fireballs.",
            "Two fireballs per throw.",
            "Fireballs set enemies ablaze.",
            "Three fireballs, bigger blasts."},
        "Five comets per throw, enormous burning blasts, much faster."),
    LIGHTNING(Kind.SKILL, "Lightning", new Color(255, 240, 90), 5, "Storm Lord",
        "Calls a bolt that stuns an enemy and chains to others.",
        new String[]{
            "A bolt every 2.6s, chaining to 2 enemies.",
            "Chains to 3 enemies.",
            "More damage, chains to 4.",
            "Stuns longer and strikes faster.",
            "Chains to 6 enemies."},
        "Three chains of lightning at once, every 1.2 seconds."),
    ICE_STORM(Kind.SKILL, "Ice Storm", new Color(120, 200, 255), 5, "Absolute Zero",
        "Drops a freezing storm on a crowd: damage over time and a slow.",
        new String[]{
            "A small storm every 7s.",
            "Wider storm, more damage.",
            "Lasts longer and slows harder.",
            "Two storms at once.",
            "Huge storms that flash-freeze on landing."},
        "Three vast storms that freeze everything they land on."),
    ORBIT_BLADES(Kind.SKILL, "Orbit Blades", new Color(210, 225, 255), 5, "Blade Tempest",
        "Spectral swords circle you, cutting anything they touch.",
        new String[]{
            "Two blades circle you.",
            "Three blades.",
            "Blades hit harder and spin faster.",
            "Four blades, a wider circle.",
            "Five blades that hit very hard."},
        "Eight blades whirling at double speed."),
    HOLY_AURA(Kind.SKILL, "Holy Aura", new Color(255, 225, 120), 5, "Sanctuary",
        "A ring of holy light that burns enemies standing near you.",
        new String[]{
            "A small aura that burns nearby enemies.",
            "Wider aura.",
            "Burns harder and slows enemies inside.",
            "Wider still.",
            "Burns very hard."},
        "A vast aura; every enemy it burns heals you a little."),
    HEALING(Kind.SKILL, "Healing", new Color(110, 230, 130), 3, null,
        "Heals you automatically when your health drops below 60%.",
        new String[]{
            "Heals 20% of max HP (every 16s at most).",
            "Heals 28%, every 13s at most.",
            "Heals 36%, every 10s at most."},
        null),

    // ---------------------------------------------------------------- passives (stats)
    ROLL(Kind.PASSIVE, "Evasive Roll", new Color(150, 230, 255), 3, null,
        "Learn to roll (SPACE): a quick, invincible dodge.",
        new String[]{"Unlocks the roll (SPACE).", "Roll recharges 25% faster.", "Roll recharges faster and goes further."}, null),
    COMBO(Kind.PASSIVE, "Combo Extension", new Color(255, 200, 70), 3, null,
        "One more hit in your sword chain; the finisher launches.",
        new String[]{"3-hit combo.", "4-hit combo.", "5-hit combo."}, null),
    BLADE(Kind.PASSIVE, "Blade Mastery", new Color(255, 170, 90), 5, null, "+20% melee damage.", null, null),
    HASTE(Kind.PASSIVE, "Quick Strikes", new Color(255, 230, 140), 5, null, "+12% attack speed.", null, null),
    REACH(Kind.PASSIVE, "Long Reach", new Color(240, 200, 160), 3, null, "+15% sword reach.", null, null),
    VAMPIRE(Kind.PASSIVE, "Vampiric Strikes", new Color(220, 70, 90), 3, null, "Each sword hit heals 1 HP.", null, null),
    POWER(Kind.PASSIVE, "Arcane Power", new Color(190, 120, 255), 5, null, "+15% skill damage.", null, null),
    CASTING(Kind.PASSIVE, "Quick Casting", new Color(160, 140, 255), 4, null, "Skill cooldowns 10% shorter.", null, null),
    VITALITY(Kind.PASSIVE, "Vitality", new Color(110, 220, 120), 5, null, "+25 max HP and heal 25.", null, null),
    SWIFT(Kind.PASSIVE, "Fleet Foot", new Color(120, 230, 200), 4, null, "+8% move speed.", null, null),
    MAGNET(Kind.PASSIVE, "Magnetism", new Color(120, 170, 255), 4, null, "+40% pickup range.", null, null),
    WISDOM(Kind.PASSIVE, "Wisdom", new Color(120, 200, 255), 3, null, "+15% XP gained.", null, null),
    ARMOR(Kind.PASSIVE, "Iron Skin", new Color(170, 180, 200), 4, null, "Take 7% less damage.", null, null),
    REGEN(Kind.PASSIVE, "Regeneration", new Color(140, 240, 150), 4, null, "Regenerate 0.6 HP per second.", null, null),
    CRIT(Kind.PASSIVE, "Precision", new Color(255, 120, 120), 4, null, "+7% critical hit chance.", null, null);

    enum Kind { SKILL, PASSIVE }

    static final int SKILL_SLOTS = 5, PASSIVE_SLOTS = 6;
    /** A skill's rank once it has evolved. */
    static final int EVOLVED = 6;

    final Kind kind;
    final String label;
    final Color color;
    final int maxRank;
    /** The evolved skill's name, or null for one that can't evolve. */
    final String evoName;
    final String blurb;
    /** rankText[r - 1] says what rank r gives; null for a passive where every rank is the same (the {@link #blurb}). */
    private final String[] rankText;
    final String evoText;

    Perk(Kind kind, String label, Color color, int maxRank, String evoName, String blurb, String[] rankText, String evoText) {
        this.kind = kind;
        this.label = label;
        this.color = color;
        this.maxRank = maxRank;
        this.evoName = evoName;
        this.blurb = blurb;
        this.rankText = rankText;
        this.evoText = evoText;
    }

    /** The passive a skill needs alongside it (at any rank) before it can evolve. */
    Perk partner() {
        return switch (this) {
            case CRESCENT_WAVE -> HASTE;
            case FIREBALL -> POWER;
            case LIGHTNING -> CASTING;
            case ICE_STORM -> ARMOR;
            case ORBIT_BLADES -> BLADE;
            case HOLY_AURA -> VITALITY;
            default -> null;
        };
    }

    boolean evolvable() { return evoName != null; }

    /** What taking rank {@code r} does (r == {@link #EVOLVED} is the evolution). */
    String text(int r) {
        if (r == EVOLVED) return evoText;
        return rankText == null ? blurb : rankText[r - 1];
    }

    String title(int rank) { return rank >= EVOLVED ? evoName : label; }

    /** A passive's effect for one rank. Skills don't change the player's stats: {@link Arsenal} reads their ranks. */
    void apply(Player p, int newRank) {
        switch (this) {
            case ROLL -> {
                if (newRank == 1) p.rollUnlocked = true;
                else p.dodgeCooldown *= 0.75;
                if (newRank == 3) p.rollBonus = 1.3;
            }
            case COMBO -> p.comboMax = Math.min(5, p.comboMax + 1);
            case BLADE -> p.meleeMult += 0.20;
            case HASTE -> p.attackSpeed += 0.12;
            case REACH -> p.reachMult += 0.15;
            case VAMPIRE -> p.hpPerHit += 1;
            case POWER -> p.spellPower += 0.15;
            case CASTING -> p.cooldownMult *= 0.90;
            case VITALITY -> { p.maxHp += 25; p.hp = Math.min(p.maxHp, p.hp + 25); }
            case SWIFT -> p.moveSpeed *= 1.08;
            case MAGNET -> p.magnet *= 1.4;
            case WISDOM -> p.xpMult += 0.15;
            case ARMOR -> p.armor = Math.min(0.6, p.armor + 0.07);
            case REGEN -> p.regen += 0.6;
            case CRIT -> p.critChance += 0.07;
            default -> { }
        }
    }

    /** Takes one more rank: records it and applies its effect. */
    void take(Player p) {
        int r = p.perk[ordinal()] + 1;
        p.perk[ordinal()] = r;
        apply(p, r);
    }

    // ---------------------------------------------------------------- offering choices

    /** One card on the level-up screen: taking {@code perk} to {@code rank}. */
    record Choice(Perk perk, int rank) {
        boolean evolution() { return rank == EVOLVED; }
    }

    static int owned(Player p, Kind kind) {
        int n = 0;
        for (Perk k : values()) if (k.kind == kind && p.perk[k.ordinal()] > 0) n++;
        return n;
    }

    /** Every choice currently possible, with how likely each is to be offered. */
    private static List<Choice> candidates(Player p, List<Double> weights) {
        List<Choice> out = new ArrayList<>();
        int skills = owned(p, Kind.SKILL), passives = owned(p, Kind.PASSIVE);
        for (Perk k : values()) {
            int r = p.perk[k.ordinal()];
            if (r >= EVOLVED) continue;
            if (r == k.maxRank) {                                      // maxed: maybe it can evolve
                if (k.evolvable() && p.perk[k.partner().ordinal()] > 0) { out.add(new Choice(k, EVOLVED)); weights.add(4.0); }
                continue;
            }
            if (r == 0 && (k.kind == Kind.SKILL ? skills >= SKILL_SLOTS : passives >= PASSIVE_SLOTS)) continue;
            double w = r > 0 ? 1.5 : k.kind == Kind.SKILL ? 1.3 : 1.0;
            if (k == ROLL && r == 0) w = 3;                            // nobody wants to go long without a dodge
            out.add(new Choice(k, r + 1));
            weights.add(w);
        }
        return out;
    }

    /**
     * {@code count} different choices, drawn by weight. While you still can't roll, the roll is always among them for
     * your first few level-ups.
     */
    static List<Choice> offer(Player p, Random rng, int count) {
        return offer(p, rng, count, false);
    }

    /** As {@link #offer(Player, Random, int)}; a chest ({@code chest}) always includes an evolution if one is ready. */
    static List<Choice> offer(Player p, Random rng, int count, boolean chest) {
        List<Double> weights = new ArrayList<>();
        List<Choice> pool = candidates(p, weights);
        List<Choice> out = new ArrayList<>();
        if (chest) {
            for (int i = 0; i < pool.size(); i++) {
                if (pool.get(i).evolution()) { out.add(pool.remove(i)); weights.remove(i); break; }
            }
        }
        if (p.perk[ROLL.ordinal()] == 0 && p.level <= 4) {
            for (int i = 0; i < pool.size(); i++) {
                if (pool.get(i).perk() == ROLL) { out.add(pool.remove(i)); weights.remove(i); break; }
            }
        }
        while (out.size() < count && !pool.isEmpty()) {
            double total = 0;
            for (double w : weights) total += w;
            double x = rng.nextDouble() * total;
            int pick = 0;
            for (; pick < weights.size() - 1; pick++) {
                x -= weights.get(pick);
                if (x < 0) break;
            }
            out.add(pool.remove(pick));
            weights.remove(pick);
        }
        return out;
    }

}
