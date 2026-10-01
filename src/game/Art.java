package game;

import java.util.Map;
import java.util.TreeMap;

/**
 * The sprite atlas. Sprites are painted in code (see {@link PeopleArt}, {@link CreatureArt}, {@link FxArt}) or loaded
 * from drawn images ({@link ImportedArt}, which replaces painted ones of the same name), and registered here by name as
 * a list of animation frames, e.g. {@code "hero.side.walk"}.
 */
final class Art {
    private Art() {}

    /** Size of one art pixel on screen, in world units. Everything is drawn at this scale so pixels match. */
    static final int SCALE = 3;

    private static final Map<String, Sprite[]> SPRITES = new TreeMap<>();

    static {
        PeopleArt.register(SPRITES);
        BreakableArt.register(SPRITES);
        CreatureArt.register(SPRITES);
        FxArt.register(SPRITES);
        TownArt.register(SPRITES);
        RunArt.register(SPRITES);
        ImportedArt.register(SPRITES);      // the drawn art, over the painted sprites it replaces
    }

    static boolean has(String name) { return SPRITES.containsKey(name); }

    static Sprite[] frames(String name) {
        Sprite[] s = SPRITES.get(name);
        if (s == null) throw new IllegalArgumentException("no sprite named " + name);
        return s;
    }

    /** The frame of an animation at a given time, looping. */
    static Sprite frame(String name, double seconds, double fps) {
        Sprite[] f = frames(name);
        int i = (int) Math.floor(seconds * fps);
        return f[((i % f.length) + f.length) % f.length];
    }

    /** Everything, sorted by name (for the art sheet in the tests). */
    static Map<String, Sprite[]> all() { return SPRITES; }

    /** Small helper for the painters: builds a sprite from a canvas. */
    static Sprite sprite(PixelCanvas c, int ax, int ay) { return new Sprite(c, ax, ay); }
}
