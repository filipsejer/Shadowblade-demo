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
 * Where you are in the story: what's happened (flags such as {@code "met.rowan"}), which chests you've opened and
 * which areas you've seen, which challenges you've cleared and how often, your skill points and the {@link Mastery}
 * ranks you've bought, the merchant's stock, and where you stood when you last saved. Gold and equipment live in the
 * {@link Profile}; the two files together are the save game ({@code adventure.properties} and {@code profile.properties}).
 */
final class Adventure {
    final Set<String> flags = new LinkedHashSet<>();
    final Set<String> opened = new LinkedHashSet<>();
    /** The areas of the world you've walked into (by name): the minimap remembers them. */
    final Set<String> visited = new LinkedHashSet<>();
    final Map<String, Integer> clears = new HashMap<>();
    int skillPoints;
    final int[] mastery = new int[Mastery.values().length];
    /** The world you're in ({@link Worlds#of}), and where you stood in each one you've been to. */
    String world = Worlds.FOREST;
    private final Map<String, double[]> spots = new HashMap<>();
    /** Bramble's wares, and how many restocks there have been (it changes each time a challenge is cleared). */
    final List<Item> stock = new ArrayList<>();
    int restocks;
    int fights, wins;

    boolean has(String flag) { return flags.contains(flag); }

    void set(String flag) { flags.add(flag); }

    int clears(Challenge c) { return clears.getOrDefault(c.id, 0); }

    int rank(Mastery m) { return mastery[m.ordinal()]; }

    /** Where you last stood in a world, or null (you'd arrive at its own starting point). */
    double[] spot(String w) { return spots.get(w); }

    void setSpot(String w, double x, double y) { spots.put(w, new double[]{x, y}); }

    /** Forgets where you stood in a world, so you arrive at its entrance next time. */
    void clearSpot(String w) { spots.remove(w); }

    /** Puts a fresh set of four items on the merchant's table, as good as {@code world}'s (see {@link Item#odds}). */
    void restock(String world) {
        Random rng = new Random(9157L * (restocks + 1) + 31);
        restocks++;
        stock.clear();
        for (int i = 0; i < 4; i++) {
            Item.Rarity r = Item.rarity(rng, Item.odds(world, Item.Source.SHOP, restocks / 2));
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

    /** The save slot in use's story file (see {@link Saves}). */
    static Path file() { return Saves.dir().resolve("adventure.properties"); }

    void save(Path path) {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("flags", String.join(",", flags));
        p.setProperty("opened", String.join(",", opened));
        p.setProperty("visited", String.join(",", visited));
        for (Map.Entry<String, Integer> e : clears.entrySet()) p.setProperty("clears." + e.getKey(), String.valueOf(e.getValue()));
        p.setProperty("skillPoints", String.valueOf(skillPoints));
        for (Mastery m : Mastery.values()) if (mastery[m.ordinal()] > 0) p.setProperty("mastery." + m.name(), String.valueOf(mastery[m.ordinal()]));
        p.setProperty("world", world);
        for (Map.Entry<String, double[]> e : spots.entrySet()) {
            p.setProperty("x." + e.getKey(), String.valueOf(e.getValue()[0]));
            p.setProperty("y." + e.getKey(), String.valueOf(e.getValue()[1]));
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
        for (String f : p.getProperty("visited", "").split(",")) if (!f.isBlank()) a.visited.add(f.trim());
        for (Challenge c : Challenge.values()) {
            int n = Profile.intOf(p, "clears." + c.id);
            if (n > 0) a.clears.put(c.id, n);
        }
        a.skillPoints = Math.max(0, Profile.intOf(p, "skillPoints"));
        for (Mastery m : Mastery.values()) a.mastery[m.ordinal()] = Math.max(0, Math.min(m.maxRank, Profile.intOf(p, "mastery." + m.name())));
        a.world = Worlds.exists(p.getProperty("world")) ? p.getProperty("world") : Worlds.FOREST;
        for (String w : Worlds.IDS) if (p.getProperty("x." + w) != null) a.setSpot(w, Run.parse(p, "x." + w), Run.parse(p, "y." + w));
        if (p.getProperty("x") != null) a.setSpot(Worlds.FOREST, Run.parse(p, "x"), Run.parse(p, "y"));   // (a save from before there were other worlds)
        a.restocks = Profile.intOf(p, "restocks");
        for (int i = 0; i < Profile.intOf(p, "stock"); i++) {
            Item it = Item.decode(p.getProperty("stock." + i, ""));
            if (it != null) a.stock.add(it);
        }
        if (a.stock.isEmpty()) a.restock(a.world);
        a.fights = Profile.intOf(p, "fights");
        a.wins = Profile.intOf(p, "wins");
        return a;
    }
}
