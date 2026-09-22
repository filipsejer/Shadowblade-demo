package game;

import static game.PixelCanvas.*;

import java.util.Map;

/**
 * Crates and barrels, one look per level: plain wood in the forest, painted steel in the city, hazard-striped steel and toxic drums in the
 * laboratory. Anchored at the bottom centre. Also the splinters they burst into.
 */
final class BreakableArt {
    private BreakableArt() {}

    private static final int OUT = rgb(34, 24, 22);
    private static final int WOOD = rgb(172, 118, 66), WOOD_L = rgb(212, 160, 96), WOOD_D = rgb(112, 72, 42);
    private static final int STEEL = rgb(96, 116, 146), STEEL_L = rgb(150, 172, 204), STEEL_D = rgb(58, 72, 98);
    private static final int GREY = rgb(112, 118, 128), GREY_L = rgb(168, 176, 188), GREY_D = rgb(66, 72, 84);
    private static final int HAZARD = rgb(246, 200, 50), HAZARD_D = rgb(40, 40, 46);
    private static final int TOXIC = rgb(70, 190, 96), TOXIC_L = rgb(150, 244, 160), TOXIC_D = rgb(36, 112, 62);

    static void register(Map<String, Sprite[]> m) {
        m.put("crate.forest", new Sprite[]{crate(WOOD, WOOD_L, WOOD_D, WOOD_D, false)});
        m.put("barrel.forest", new Sprite[]{barrel(WOOD, WOOD_L, WOOD_D, rgb(70, 66, 74), false)});
        m.put("crate.city", new Sprite[]{crate(STEEL, STEEL_L, STEEL_D, rgb(232, 122, 60), false)});
        m.put("barrel.city", new Sprite[]{barrel(rgb(70, 130, 200), rgb(140, 190, 240), rgb(38, 82, 140), rgb(226, 226, 232), false)});
        m.put("crate.lab", new Sprite[]{crate(GREY, GREY_L, GREY_D, HAZARD, true)});
        m.put("barrel.lab", new Sprite[]{barrel(TOXIC, TOXIC_L, TOXIC_D, HAZARD_D, true)});
        m.put("fx.plank", PeopleArt.frames(3, BreakableArt::plank, 3, 3));
    }

    static String name(Breakable.Kind kind, Theme theme) {
        return (kind == Breakable.Kind.CRATE ? "crate." : "barrel.") + theme.key;
    }

    /** A box seen from a little above: a lit lid, a front with a frame and cross-braces (or hazard stripes in the lab). */
    private static Sprite crate(int main, int light, int dark, int brace, boolean hazard) {
        PixelCanvas c = new PixelCanvas(18, 18);
        c.rect(1, 6, 16, 11, main);                                   // front
        c.rect(1, 2, 16, 5, light);                                   // lid
        c.rect(1, 6, 16, 1, dark);
        c.rect(1, 6, 2, 11, dark); c.rect(15, 6, 2, 11, dark);        // frame
        c.rect(1, 15, 16, 2, dark);
        if (hazard) {
            for (int i = 0; i < 4; i++) c.line(4 + i * 3, 15, 7 + i * 3, 7, HAZARD_D);
            for (int i = 0; i < 4; i++) c.line(5 + i * 3, 15, 8 + i * 3, 7, HAZARD);
            c.rect(1, 3, 16, 1, HAZARD);                              // yellow edge on the lid
        } else {
            c.line(3, 8, 14, 15, brace); c.line(14, 8, 3, 15, brace);
            c.line(3, 7, 14, 14, main); c.line(14, 7, 3, 14, main);
            c.rect(1, 4, 16, 1, dark);                                // a plank line on the lid
        }
        c.set(2, 3, WHITE_ISH); c.set(3, 3, WHITE_ISH);
        c.bevel(0.1, 0.2);
        c.outline(OUT);
        return new Sprite(c, 9, 17);
    }

    private static final int WHITE_ISH = rgb(255, 248, 226);

    /** A drum with a rounded lid and two bands (a toxic drum in the lab gets a hazard dot). */
    private static Sprite barrel(int main, int light, int dark, int band, boolean toxic) {
        PixelCanvas c = new PixelCanvas(16, 20);
        c.ellipse(8, 17, 6.4, 2.6, dark);                             // base
        c.rect(1, 4, 14, 13, main);
        c.ellipse(8, 10.5, 7.4, 8.0, main);                           // a barrel bulges in the middle
        c.ellipse(8, 4, 6.6, 3.0, light);                             // lid
        c.ellipse(8, 4, 4.4, 1.7, dark);
        c.ellipse(8, 3.6, 3.6, 1.2, light);
        for (int x = 3; x < 14; x += 3) c.rect(x, 6, 1, 11, dark);    // staves
        c.rect(1, 7, 14, 2, band); c.rect(1, 14, 14, 2, band);        // bands
        c.rect(1, 7, 14, 1, dark); c.rect(1, 14, 14, 1, dark);
        if (toxic) {
            c.disc(8, 11, 2.2, HAZARD); c.disc(8, 11, 0.9, HAZARD_D);
            c.set(11, 6, TOXIC_L); c.set(11, 5, TOXIC_L);             // a drip
        }
        c.rect(2, 8, 1, 6, light);
        c.bevel(0.08, 0.2);
        c.outline(OUT);
        return new Sprite(c, 8, 18);
    }

    /** A splinter of wood: tumbling bits of plank. */
    private static PixelCanvas plank(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        if (f == 0) { c.rect(0, 2, 6, 2, WOOD); c.rect(0, 2, 6, 1, WOOD_L); c.set(6, 3, WOOD_D); }
        else if (f == 1) { c.rect(2, 0, 2, 6, WOOD); c.rect(2, 0, 1, 6, WOOD_L); c.set(3, 6, WOOD_D); }
        else { c.rect(1, 1, 4, 4, WOOD_D); c.rect(1, 1, 3, 3, WOOD); c.set(1, 1, WOOD_L); }
        c.outline(OUT);
        return c;
    }
}
