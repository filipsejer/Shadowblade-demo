package game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Everything that outlasts a single fight: gold, the items you own, which ones you're wearing, and a few
 * records. Saved as {@code ~/.spellblade/profile.properties} (the folder can be moved with {@code -Dspellblade.home=...},
 * which the tests use so they never touch real saves).
 */
final class Profile {
    static final int MAX_ITEMS = 60;

    int gold;
    final List<Item> items = new ArrayList<>();
    final Map<Item.Slot, Item> equipped = new EnumMap<>(Item.Slot.class);
    int runs, victories, bestStage, bestKills, bestLevel;
    double bestTime;

    /** Where saves live. */
    static Path home() {
        String h = System.getProperty("spellblade.home");
        return h != null ? Path.of(h) : Path.of(System.getProperty("user.home"), ".spellblade");
    }

    static Path file() { return home().resolve("profile.properties"); }

    boolean isEquipped(Item it) { return equipped.get(it.slot) == it; }

    /** Wears an item (whatever was in its slot goes back to the bag); wearing the one already worn takes it off. */
    void toggleEquip(Item it) {
        if (isEquipped(it)) equipped.remove(it.slot);
        else equipped.put(it.slot, it);
    }

    /** Adds finds to the bag. When it's full the weakest unworn items are salvaged for gold to make room. */
    void add(List<Item> finds) {
        items.addAll(finds);
        while (items.size() > MAX_ITEMS) {
            Item weakest = null;
            for (Item it : items) if (!isEquipped(it) && (weakest == null || it.power() < weakest.power())) weakest = it;
            if (weakest == null) break;
            gold += weakest.salvageValue();
            items.remove(weakest);
        }
    }

    void salvage(Item it) {
        if (isEquipped(it)) equipped.remove(it.slot);
        items.remove(it);
        gold += it.salvageValue();
    }

    /** True if it was upgraded (enough gold, not already maxed). */
    boolean upgrade(Item it) {
        if (it.upgrade >= Item.MAX_UPGRADE || gold < it.upgradeCost()) return false;
        gold -= it.upgradeCost();
        it.upgrade++;
        return true;
    }

    /** The bag in display order: worn items first, then by slot, then strongest first. */
    List<Item> sorted() {
        List<Item> out = new ArrayList<>(items);
        out.sort(Comparator.comparing((Item it) -> !isEquipped(it)).thenComparing(it -> it.slot)
            .thenComparing(Comparator.comparingDouble(Item::power).reversed()));
        return out;
    }

    /** What's worn right now, in slot order. */
    List<Item> worn() {
        return new ArrayList<>(equipped.values());
    }

    // ------------------------------------------------------------------ saving

    static Profile load(Path path) {
        Profile p = new Profile();
        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(path)) {
            props.load(r);
        } catch (IOException | RuntimeException e) {
            return p;                                   // no profile yet (or a damaged one): start fresh
        }
        p.gold = intOf(props, "gold");
        p.runs = intOf(props, "runs");
        p.victories = intOf(props, "victories");
        p.bestStage = intOf(props, "bestStage");
        p.bestKills = intOf(props, "bestKills");
        p.bestLevel = intOf(props, "bestLevel");
        try { p.bestTime = Double.parseDouble(props.getProperty("bestTime", "0")); } catch (NumberFormatException ignored) { }
        int n = intOf(props, "items");
        for (int i = 0; i < n; i++) {
            Item it = Item.decode(props.getProperty("item." + i, ""));
            if (it == null) continue;
            p.items.add(it);
            if ("1".equals(props.getProperty("item." + i + ".worn")) && !p.equipped.containsKey(it.slot)) p.equipped.put(it.slot, it);
        }
        return p;
    }

    void save(Path path) {
        Properties props = new Properties();
        props.setProperty("gold", String.valueOf(gold));
        props.setProperty("runs", String.valueOf(runs));
        props.setProperty("victories", String.valueOf(victories));
        props.setProperty("bestStage", String.valueOf(bestStage));
        props.setProperty("bestKills", String.valueOf(bestKills));
        props.setProperty("bestLevel", String.valueOf(bestLevel));
        props.setProperty("bestTime", String.valueOf(bestTime));
        props.setProperty("items", String.valueOf(items.size()));
        for (int i = 0; i < items.size(); i++) {
            props.setProperty("item." + i, items.get(i).encode());
            if (isEquipped(items.get(i))) props.setProperty("item." + i + ".worn", "1");
        }
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                props.store(w, "Spellblade profile: gold, items and records");
            }
        } catch (IOException e) {
            System.err.println("Could not save the profile: " + e.getMessage());
        }
    }

    static int intOf(Properties p, String key) {
        try {
            return Integer.parseInt(p.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
