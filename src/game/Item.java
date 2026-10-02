package game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * A piece of equipment: found in chests (or bought), kept between fights in the {@link Profile}, and worn in
 * the Armory. Each one fills a {@link Slot}, has a {@link Rarity}, a handful of rolled {@link Stat}s, and (legendaries
 * only) a {@link Unique} effect. Gold spent in the Armory raises its {@link #upgrade} level, which scales every stat.
 */
final class Item {
    enum Slot {
        WEAPON("Weapon", Stat.MELEE_DMG, new String[]{"Sword", "Blade", "Saber", "Longsword"}),
        HELM("Helm", Stat.SPELL_DMG, new String[]{"Helm", "Hood", "Circlet", "Cap"}),
        ARMOR("Armor", Stat.MAX_HP, new String[]{"Mail", "Vest", "Robe", "Cuirass"}),
        GLOVES("Gloves", Stat.ATTACK_SPEED, new String[]{"Gloves", "Gauntlets", "Bracers", "Wraps"}),
        BOOTS("Boots", Stat.MOVE_SPEED, new String[]{"Boots", "Greaves", "Sandals", "Striders"}),
        RING("Ring", Stat.CRIT, new String[]{"Ring", "Band", "Signet", "Loop"});

        final String label;
        /** The stat every item for this slot starts with. */
        final Stat primary;
        final String[] bases;

        Slot(String label, Stat primary, String[] bases) {
            this.label = label;
            this.primary = primary;
            this.bases = bases;
        }
    }

    enum Rarity {
        COMMON("Common", new Color(190, 190, 196), 1, 1.0, new String[]{"Worn", "Plain", "Old"}),
        UNCOMMON("Uncommon", new Color(110, 220, 110), 2, 1.25, new String[]{"Sturdy", "Keen", "Solid"}),
        RARE("Rare", new Color(90, 160, 255), 3, 1.6, new String[]{"Runed", "Gleaming", "Tempered"}),
        EPIC("Epic", new Color(190, 110, 255), 4, 2.0, new String[]{"Arcane", "Stormforged", "Radiant"}),
        LEGENDARY("Legendary", new Color(255, 170, 50), 4, 2.5, new String[]{""});

        final String label;
        final Color color;
        /** How many stat lines it rolls (the slot's own stat, then random others). */
        final int stats;
        /**
         * What its stats are multiplied by. The rolls only spread {@value Item#SPREAD} either side of the middle, so at
         * the same tier and upgrade level a rarer item always rolls higher than a less rare one.
         */
        final double mult;
        final String[] prefixes;

        Rarity(String label, Color color, int stats, double mult, String[] prefixes) {
            this.label = label;
            this.color = color;
            this.stats = stats;
            this.mult = mult;
            this.prefixes = prefixes;
        }
    }

    /** What a stat line adds; {@code lo}/{@code hi} is the roll for a common, tier-0 item, before rarity and upgrades. */
    enum Stat {
        MELEE_DMG("Melee damage", 5, 9, true),
        SPELL_DMG("Skill damage", 5, 9, true),
        MAX_HP("Max HP", 10, 18, false),
        ARMOR("Damage taken", 2, 4, true),
        ATTACK_SPEED("Attack speed", 3, 6, true),
        MOVE_SPEED("Move speed", 3, 5, true),
        CRIT("Crit chance", 2, 4, true),
        REGEN("HP regen / s", 0.2, 0.4, false),
        MAGNET("Pickup range", 8, 15, true),
        XP("XP gain", 4, 8, true),
        COOLDOWN("Skill cooldowns", 3, 6, true),
        GOLD("Gold found", 8, 15, true);

        final String label;
        final double lo, hi;
        final boolean percent;

        Stat(String label, double lo, double hi, boolean percent) {
            this.label = label;
            this.lo = lo;
            this.hi = hi;
            this.percent = percent;
        }

        /** "+12% Melee damage", "-4% Damage taken", "+0.6 HP regen / s". */
        String format(double v) {
            boolean minus = this == ARMOR || this == COOLDOWN;
            return (minus ? "-" : "+") + number(v) + (percent ? "% " : " ") + label;
        }

        /** Just the amount, as {@link #format} shows it: "12%", "0.6", "38". */
        String amount(double v) { return number(v) + (percent ? "%" : ""); }

        private String number(double v) {
            return v >= 10 || percent ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
        }
    }

    /** A legendary's special power, on top of its stats. */
    enum Unique {
        WAVE_START(Slot.WEAPON, "Dawnbreaker", "Every run starts with Crescent Wave."),
        REROLL(Slot.HELM, "Crown of Insight", "+2 rerolls on level-up choices."),
        REVIVE(Slot.ARMOR, "Phoenix Mail", "Once per run, rise again at half health."),
        COMBO_START(Slot.GLOVES, "Tempest Gauntlets", "Every run starts with a 3-hit combo."),
        ROLL_START(Slot.BOOTS, "Windwalkers", "Every run starts with Evasive Roll."),
        FOURTH_CARD(Slot.RING, "Ring of Fortune", "Level-ups offer 4 choices instead of 3.");

        final Slot slot;
        final String itemName, text;

        Unique(Slot slot, String itemName, String text) {
            this.slot = slot;
            this.itemName = itemName;
            this.text = text;
        }

        static Unique of(Slot s) {
            for (Unique u : values()) if (u.slot == s) return u;
            throw new IllegalArgumentException(s.name());
        }
    }

    static final int MAX_UPGRADE = 10;

    final Slot slot;
    final Rarity rarity;
    final String name;
    /** How deep into a run it was found (0 = the first stage): later finds are stronger. */
    final int tier;
    /** Base values, before {@link #upgrade}. */
    final Map<Stat, Double> stats;
    final Unique unique;
    int upgrade;

    Item(Slot slot, Rarity rarity, String name, int tier, Map<Stat, Double> stats, Unique unique, int upgrade) {
        this.slot = slot;
        this.rarity = rarity;
        this.name = name;
        this.tier = tier;
        this.stats = stats;
        this.unique = unique;
        this.upgrade = upgrade;
    }

    /** The value of a stat line after upgrades (each upgrade level is +12% of the base). */
    double value(Stat s) {
        Double v = stats.get(s);
        return v == null ? 0 : v * (1 + 0.12 * upgrade);
    }

    /** Stat lines, primary first, formatted for display. */
    List<String> lines() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<Stat, Double> e : stats.entrySet()) out.add(e.getKey().format(value(e.getKey())));
        if (unique != null) out.add(unique.text);
        return out;
    }

    int upgradeCost() { return 30 * (upgrade + 1) * (rarity.ordinal() + 1); }

    int salvageValue() { return 12 * (rarity.ordinal() + 1) + 10 * upgrade * (rarity.ordinal() + 1); }

    /** A rough measure of how strong it is, for sorting the Armory list. */
    double power() {
        double p = rarity.ordinal() * 100 + tier * 20 + upgrade * 8;
        return unique != null ? p + 50 : p;
    }

    // ------------------------------------------------------------------ making new ones

    /** A random item of the given rarity, rolled for a find {@code tier} stages into a run. */
    static Item roll(Random rng, Rarity rarity, int tier) {
        Slot slot = Slot.values()[rng.nextInt(Slot.values().length)];
        return roll(rng, slot, rarity, tier);
    }

    static Item roll(Random rng, Slot slot, Rarity rarity, int tier) {
        Map<Stat, Double> stats = new LinkedHashMap<>();
        List<Stat> pool = new ArrayList<>(List.of(Stat.values()));
        pool.remove(slot.primary);
        stats.put(slot.primary, Math.round(rollValue(rng, slot.primary, rarity, tier) * 13) / 10.0);   // the slot's own stat rolls a bit higher
        for (int i = 1; i < rarity.stats; i++) {
            Stat s = pool.remove(rng.nextInt(pool.size()));
            stats.put(s, rollValue(rng, s, rarity, tier));
        }
        Unique unique = rarity == Rarity.LEGENDARY ? Unique.of(slot) : null;
        String name = unique != null ? unique.itemName
            : rarity.prefixes[rng.nextInt(rarity.prefixes.length)] + " " + slot.bases[rng.nextInt(slot.bases.length)];
        return new Item(slot, rarity, name, tier, stats, unique, 0);
    }

    /** How far a roll strays from the middle of a stat's range (a fraction of it, either way). */
    static final double SPREAD = 0.1;

    private static double rollValue(Random rng, Stat s, Rarity r, int tier) {
        double v = (s.lo + s.hi) / 2 * (1 + SPREAD * (rng.nextDouble() * 2 - 1));
        return Math.round(v * r.mult * (1 + 0.25 * tier) * 10) / 10.0;
    }

    /** Where an item comes from, for {@link #odds}. */
    enum Source { CACHE, ELITE, CHEST, GUARDIAN, SHOP }

    /**
     * How likely each rarity is (weights, common first) for an item from {@code source} in {@code world}. The forest
     * only gives commons and uncommons; the city gives mostly uncommons and rares, with epics now and then and, from
     * a guardian, the odd legendary; Stormcliff gives mostly rares and epics, and legendaries more often. {@code bonus} (a repeated clear, a restocked shop) moves some weight from the
     * least rare to the best rarity that place gives.
     */
    static double[] odds(String world, Source source, int bonus) {
        double[] w = switch (world) {
            case Worlds.FOREST -> switch (source) {
                case CACHE, ELITE -> new double[]{60, 40, 0, 0, 0};
                case CHEST -> new double[]{45, 55, 0, 0, 0};
                case GUARDIAN -> new double[]{15, 85, 0, 0, 0};
                case SHOP -> new double[]{55, 45, 0, 0, 0};
            };
            case Worlds.LAB -> switch (source) {                                 // Stormcliff: mostly rares, epics often
                case CACHE -> new double[]{5, 35, 40, 17, 3};
                case ELITE -> new double[]{5, 30, 42, 20, 3};
                case CHEST -> new double[]{0, 20, 45, 30, 5};
                case GUARDIAN -> new double[]{0, 0, 40, 45, 15};
                case SHOP -> new double[]{10, 35, 38, 15, 2};
            };
            default -> switch (source) {
                case CACHE -> new double[]{15, 45, 32, 8, 0};
                case ELITE -> new double[]{15, 45, 30, 10, 0};
                case CHEST -> new double[]{5, 40, 40, 15, 0};
                case GUARDIAN -> new double[]{0, 10, 50, 35, 5};
                case SHOP -> new double[]{20, 45, 28, 7, 0};
            };
        };
        int bottom = 0, top = w.length - 1;
        while (bottom < top && w[bottom] == 0) bottom++;
        while (top > bottom && w[top] == 0) top--;
        double moved = Math.min(w[bottom], 4 * Math.max(0, bonus));
        w[bottom] -= moved;
        w[top] += moved;
        return w;
    }

    /** Picks a rarity from weights (one per {@link Rarity}, common first). */
    static Rarity rarity(Random rng, double[] weights) {
        double total = 0;
        for (double w : weights) total += w;
        double x = rng.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x < 0) return Rarity.values()[i];
        }
        return Rarity.values()[weights.length - 1];
    }

    // ------------------------------------------------------------------ saving

    /** One line: {@code SLOT;RARITY;tier;upgrade;name;STAT=value,STAT=value;UNIQUE-or-empty}. */
    String encode() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Stat, Double> e : stats.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey().name()).append('=').append(e.getValue());
        }
        return slot.name() + ";" + rarity.name() + ";" + tier + ";" + upgrade + ";" + name.replace(';', ' ') + ";" + sb + ";"
            + (unique == null ? "" : unique.name());
    }

    /** The inverse of {@link #encode}; null for a line it can't read (a damaged file loses that item, not the whole profile). */
    static Item decode(String line) {
        try {
            String[] f = line.split(";", -1);
            Map<Stat, Double> stats = new LinkedHashMap<>();
            if (!f[5].isEmpty()) {
                for (String kv : f[5].split(",")) {
                    String[] p = kv.split("=");
                    stats.put(Stat.valueOf(p[0]), Double.parseDouble(p[1]));
                }
            }
            Unique u = f.length > 6 && !f[6].isEmpty() ? Unique.valueOf(f[6]) : null;
            return new Item(Slot.valueOf(f[0]), Rarity.valueOf(f[1]), f[4], Integer.parseInt(f[2]), stats, u,
                Math.min(MAX_UPGRADE, Integer.parseInt(f[3])));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
