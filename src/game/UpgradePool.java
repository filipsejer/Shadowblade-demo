package game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Everything that can be bought at the upgrade station, laid out for the player's current progress. */
final class UpgradePool {
    private UpgradePool() {}

    static final int LEARN_COST = 2;      // skill points to learn a new spell
    private static final int MAX_COMBO = 5;

    private record Stat(String id, Upgrade.Category category, String title, String description, int maxRank,
                        Consumer<Player> apply) {}

    private static final List<Stat> STATS = List.of(
        new Stat("damage", Upgrade.Category.COMBAT, "Blade Mastery", "+20% melee damage.", 5,
            p -> p.meleeMult += 0.20),
        new Stat("attackSpeed", Upgrade.Category.COMBAT, "Quick Strikes", "+12% attack speed.", 4,
            p -> p.attackSpeed += 0.12),
        new Stat("reach", Upgrade.Category.COMBAT, "Long Reach", "+15% melee reach.", 3,
            p -> p.reachMult += 0.15),
        new Stat("leech", Upgrade.Category.COMBAT, "Vampiric Strikes", "Landing a melee hit restores 1 HP.", 3,
            p -> p.hpPerHit += 1),
        new Stat("manaHit", Upgrade.Category.COMBAT, "Mana Strikes", "Landing a melee hit restores 2 more MP.", 3,
            p -> p.mpPerHit += 2),

        new Stat("power", Upgrade.Category.SPELLS, "Spell Power", "+15% spell damage.", 4,
            p -> p.spellPower += 0.15),
        new Stat("cast", Upgrade.Category.SPELLS, "Quick Casting", "Spell cooldowns reduced by 12%.", 3,
            p -> p.cooldownMult *= 0.88),

        new Stat("vitality", Upgrade.Category.SURVIVAL, "Vitality", "+25 max HP and heal 25.", 5, p -> {
            p.maxHp += 25;
            p.hp = Math.min(p.maxHp, p.hp + 25);
        }),
        new Stat("moveSpeed", Upgrade.Category.SURVIVAL, "Fleet Foot", "+8% movement speed.", 4,
            p -> p.moveSpeed *= 1.08),
        new Stat("mana", Upgrade.Category.SURVIVAL, "Arcane Reservoir", "+25 max MP and +2 MP/s regeneration.", 4, p -> {
            p.maxMp += 25;
            p.mpRegen += 2;
        }),
        new Stat("roll", Upgrade.Category.SURVIVAL, "Nimble Roll", "Roll cooldown reduced by 0.12s.", 3,
            p -> p.dodgeCooldown -= 0.12)
    );

    /** Every upgrade, grouped by category in a stable order (maxed ones stay in the list, marked as maxed). */
    static List<Upgrade> catalogue(Player p) {
        List<Upgrade> out = new ArrayList<>();
        for (Upgrade.Category category : Upgrade.Category.values()) {
            if (category == Upgrade.Category.COMBAT) out.add(combo(p));
            if (category == Upgrade.Category.SPELLS) for (Ability a : Ability.values()) out.add(spell(p, a));
            for (Stat s : STATS) if (s.category() == category) out.add(stat(p, s));
        }
        return out;
    }

    static List<Upgrade> inCategory(List<Upgrade> all, Upgrade.Category category) {
        List<Upgrade> out = new ArrayList<>();
        for (Upgrade u : all) if (u.category() == category) out.add(u);
        return out;
    }

    private static Upgrade combo(Player p) {
        Upgrade.Category c = Upgrade.Category.COMBAT;
        String status = "Chain: " + p.comboMax + " / " + MAX_COMBO + " hits";
        if (p.comboMax >= MAX_COMBO) return Upgrade.maxed(c, "Combo Extension", status, c.color);
        return new Upgrade(c, "Combo Extension", status,
            "Add a hit to your attack chain (" + p.comboMax + " to " + (p.comboMax + 1)
                + "). Every hit in the chain deals more, and the finisher hits hardest.",
            p.comboMax, c.color, pl -> pl.comboMax++);       // 2 points for the 3rd hit, 3 for the 4th, 4 for the 5th
    }

    private static Upgrade spell(Player p, Ability a) {
        Upgrade.Category c = Upgrade.Category.SPELLS;
        int lv = p.spellLevel[a.ordinal()];
        if (lv == 0) {
            return new Upgrade(c, a.label, "Not learned", a.blurb + " Appears in your Magic menu.", LEARN_COST, a.color,
                pl -> pl.unlockSpell(a));
        }
        String status = "Level " + lv + " / " + Ability.MAX_LEVEL;
        if (lv >= Ability.MAX_LEVEL) return Upgrade.maxed(c, a.label, status, a.color);
        return new Upgrade(c, a.label, status, "Level " + (lv + 1) + ": " + a.upgrades[lv - 1], lv + 1, a.color,
            pl -> pl.spellLevel[a.ordinal()]++);
    }

    private static Upgrade stat(Player p, Stat s) {
        int rank = p.rank(s.id());
        Upgrade.Category c = s.category();
        String status = "Rank " + rank + " / " + s.maxRank();
        if (rank >= s.maxRank()) return Upgrade.maxed(c, s.title(), status, c.color);
        return new Upgrade(c, s.title(), status, s.description(), 1, c.color, pl -> {
            s.apply().accept(pl);
            pl.ranks.merge(s.id(), 1, Integer::sum);
        });
    }
}
