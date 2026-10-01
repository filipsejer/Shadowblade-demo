package game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Where you are in the story: what's happened (flags such as {@code "met.rowan"}), which chests you've opened, which
 * challenges you've cleared and how often, your skill points and the {@link Mastery} ranks you've bought, the
 * merchant's stock, and where you stood when you last saved. Gold and equipment live in the {@link Profile}; the two
 * files together are the save game ({@code adventure.properties} and {@code profile.properties}).
 */
final class Adventure {
    final Set<String> flags = new LinkedHashSet<>();
    final Set<String> opened = new LinkedHashSet<>();
    final Map<String, Integer> clears = new HashMap<>();
    int skillPoints;
    final int[] mastery = new int[Mastery.values().length];
    /** Where the hero stands in the explorable world (NaN: at the world's own starting point). */
    double x = Double.NaN, y = Double.NaN;
    /** Bramble's wares, and how many restocks there have been (it changes each time a challenge is cleared). */
    final List<Item> stock = new ArrayList<>();
    int restocks;
    int fights, wins;

    boolean has(String flag) { return flags.contains(flag); }

    void set(String flag) { flags.add(flag); }

    int clears(Challenge c) { return clears.getOrDefault(c.id, 0); }

    int rank(Mastery m) { return mastery[m.ordinal()]; }

    /** Puts a fresh set of four items on the merchant's table: mostly common and uncommon, a rare now and then. */
    void restock() {
        Random rng = new Random(9157L * (restocks + 1) + 31);
        restocks++;
        stock.clear();
        for (int i = 0; i < 4; i++) {
            Item.Rarity r = Item.rarity(rng, new double[]{40, 40, 17, 3 + restocks, 0});
            stock.add(Item.roll(rng, r, Math.min(2, restocks / 3)));
        }
    }

    /** What Bramble asks for an item. */
    static int price(Item it) {
        return switch (it.rarity) {
            case COMMON -> 60;
            case UNCOMMON -> 120;
            case RARE -> 240;
            case EPIC -> 480;
            case LEGENDARY -> 900;
        } + 20 * it.tier;
    }

    // ------------------------------------------------------------------ saving

    static Path file() { return Profile.home().resolve("adventure.properties"); }

    static boolean saved() { return Files.exists(file()); }

    static void delete() {
        try {
            Files.deleteIfExists(file());
        } catch (IOException ignored) { }
    }

    void save(Path path) {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("flags", String.join(",", flags));
        p.setProperty("opened", String.join(",", opened));
        for (Map.Entry<String, Integer> e : clears.entrySet()) p.setProperty("clears." + e.getKey(), String.valueOf(e.getValue()));
        p.setProperty("skillPoints", String.valueOf(skillPoints));
        for (Mastery m : Mastery.values()) if (mastery[m.ordinal()] > 0) p.setProperty("mastery." + m.name(), String.valueOf(mastery[m.ordinal()]));
        if (!Double.isNaN(x)) {
            p.setProperty("x", String.valueOf(x));
            p.setProperty("y", String.valueOf(y));
        }
        p.setProperty("restocks", String.valueOf(restocks));
        p.setProperty("stock", String.valueOf(stock.size()));
        for (int i = 0; i < stock.size(); i++) p.setProperty("stock." + i, stock.get(i).encode());
        p.setProperty("fights", String.valueOf(fights));
        p.setProperty("wins", String.valueOf(wins));
        try {
            Files.createDirectories(path.getParent());
            try (Writer out = Files.newBufferedWriter(path)) {
                p.store(out, "Spellblade adventure: story progress");
            }
        } catch (IOException e) {
            System.err.println("Could not save the adventure: " + e.getMessage());
        }
    }

    /** The saved adventure, or null if there isn't a readable one. */
    static Adventure load(Path path) {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path)) {
            p.load(r);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        Adventure a = new Adventure();
        for (String f : p.getProperty("flags", "").split(",")) if (!f.isBlank()) a.flags.add(f.trim());
        for (String f : p.getProperty("opened", "").split(",")) if (!f.isBlank()) a.opened.add(f.trim());
        for (Challenge c : Challenge.values()) {
            int n = Profile.intOf(p, "clears." + c.id);
            if (n > 0) a.clears.put(c.id, n);
        }
        a.skillPoints = Math.max(0, Profile.intOf(p, "skillPoints"));
        for (Mastery m : Mastery.values()) a.mastery[m.ordinal()] = Math.max(0, Math.min(m.maxRank, Profile.intOf(p, "mastery." + m.name())));
        if (p.getProperty("x") != null) {
            a.x = Run.parse(p, "x");
            a.y = Run.parse(p, "y");
        }
        a.restocks = Profile.intOf(p, "restocks");
        for (int i = 0; i < Profile.intOf(p, "stock"); i++) {
            Item it = Item.decode(p.getProperty("stock." + i, ""));
            if (it != null) a.stock.add(it);
        }
        if (a.stock.isEmpty()) a.restock();
        a.fights = Profile.intOf(p, "fights");
        a.wins = Profile.intOf(p, "wins");
        return a;
    }

    /** The main menu's line about the save ("Mossbrook" + progress), or null when there isn't one. */
    static String describeSaved() {
        Adventure a = load(file());
        if (a == null) return null;
        int done = 0;
        for (Challenge c : Challenge.values()) if (a.clears(c) > 0) done++;
        return "The Whispering Forest   -   " + done + " / " + Challenge.values().length + " challenges   -   " + a.skillPoints + " SP";
    }
}
