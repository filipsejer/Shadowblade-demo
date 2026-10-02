package game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;

/**
 * The save slots: {@value #COUNT} separate games, each a folder ({@code save1} ... {@code save5}) under
 * {@link Profile#home} holding that game's {@code adventure.properties} and {@code profile.properties}. One slot is
 * in use at a time ({@link #use}); the game saves into it, and CONTINUE picks the last one used back up. A save from
 * before there were slots (the two files straight in the home folder) is moved into slot 1.
 */
final class Saves {
    private Saves() {}

    static final int COUNT = 5;

    private static int slot = 1;

    /** The slot in use (1..{@value #COUNT}). */
    static int slot() { return slot; }

    /** Points the save files at a slot without remembering it (the main menu showing the last game's gear). */
    static void select(int s) { slot = Math.max(1, Math.min(COUNT, s)); }

    /** Plays in this slot from now on, and remembers it as the one CONTINUE picks up. */
    static void use(int s) {
        select(s);
        Properties p = new Properties();
        p.setProperty("last", String.valueOf(slot));
        try {
            Files.createDirectories(Profile.home());
            try (Writer out = Files.newBufferedWriter(index())) {
                p.store(out, "Spellblade: the save slot played last");
            }
        } catch (IOException e) {
            System.err.println("Could not remember the save slot: " + e.getMessage());
        }
    }

    /** The folder a slot's files live in. */
    static Path dir(int s) { return Profile.home().resolve("save" + s); }

    /** The slot in use's folder. */
    static Path dir() { return dir(slot); }

    private static Path index() { return Profile.home().resolve("slots.properties"); }

    /** True if a slot holds a game. */
    static boolean used(int s) { return Files.exists(dir(s).resolve("adventure.properties")); }

    /** The slot played last, if it still holds a game; otherwise the newest one that does; 0 if none do. */
    static int last() {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(index())) {
            p.load(r);
        } catch (IOException | RuntimeException ignored) { }
        int s = Profile.intOf(p, "last");
        if (s >= 1 && s <= COUNT && used(s)) return s;
        int best = 0;
        long newest = Long.MIN_VALUE;
        for (int i = 1; i <= COUNT; i++) {
            long t = savedAt(i);
            if (used(i) && t > newest) { newest = t; best = i; }
        }
        return best;
    }

    /** Throws a slot's game away. */
    static void delete(int s) {
        try {
            Files.deleteIfExists(dir(s).resolve("adventure.properties"));
            Files.deleteIfExists(dir(s).resolve("profile.properties"));
        } catch (IOException e) {
            System.err.println("Could not delete save " + s + ": " + e.getMessage());
        }
    }

    /** When a slot was last saved (milliseconds), or {@code Long.MIN_VALUE} if it's empty. */
    static long savedAt(int s) {
        try {
            return Files.getLastModifiedTime(dir(s).resolve("adventure.properties")).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    /** Moves a save from before there were slots into slot 1 (unless slot 1 is already taken). */
    static void migrate() {
        Path home = Profile.home(), oldAdventure = home.resolve("adventure.properties"), oldProfile = home.resolve("profile.properties");
        if (!Files.exists(oldAdventure) && !Files.exists(oldProfile)) return;
        if (used(1)) return;
        try {
            Files.createDirectories(dir(1));
            if (Files.exists(oldAdventure)) Files.move(oldAdventure, dir(1).resolve("adventure.properties"), StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(oldProfile)) Files.move(oldProfile, dir(1).resolve("profile.properties"), StandardCopyOption.REPLACE_EXISTING);
            use(1);
        } catch (IOException e) {
            System.err.println("Could not move the old save into slot 1: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ what's in a slot

    /** What the slot screen shows about a game: where it is, what's next, how far along, gold and gear, when it was saved. */
    record Summary(int slot, String world, String objective, int done, int open, int skillPoints, Profile profile, String when) {}

    /** A slot's game, read for the slot screen, or null if it's empty (or can't be read). */
    static Summary describe(int s) {
        if (!used(s)) return null;
        Adventure a = Adventure.load(dir(s).resolve("adventure.properties"));
        if (a == null) return null;
        int done = 0, open = 0;
        for (Challenge c : Challenge.values()) {
            if (!Story.worldOpen(a, c.world)) continue;
            open++;
            if (a.clears(c) > 0) done++;
        }
        Profile p = Profile.load(dir(s).resolve("profile.properties"));
        return new Summary(s, Worlds.title(a.world), Story.objective(a), done, open, a.skillPoints, p, when(savedAt(s)));
    }

    /** "Today, 14:32", "Yesterday, 09:05", or "3 Oct, 18:20". */
    static String when(long millis) {
        LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        LocalDate today = LocalDate.now();
        String time = t.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        if (t.toLocalDate().equals(today)) return "Today, " + time;
        if (t.toLocalDate().equals(today.minusDays(1))) return "Yesterday, " + time;
        String day = t.format(DateTimeFormatter.ofPattern(t.getYear() == today.getYear() ? "d MMM" : "d MMM yyyy", Locale.ENGLISH));
        return day + ", " + time;
    }
}
