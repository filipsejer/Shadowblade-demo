package game;

import static game.PixelCanvas.*;

import java.util.Map;

/**
 * Three of the forest's people: Elder Rowan (an old traveller with a cane), Hunter Fenn (a traveller with a pack) and
 * Wren (a small child). Painted in code, in the same small round-headed chibi style as {@link PeopleArt}.
 */
final class TownArt {
    private TownArt() {}

    private static final int OUT = rgb(36, 24, 46);
    private static final int SKIN = rgb(255, 216, 176), SKIN_D = rgb(226, 168, 128);
    private static final int EYE = rgb(34, 30, 58), WHITE = rgb(255, 255, 255), BLUSH = rgb(255, 152, 152);
    private static final int WOOD = rgb(150, 100, 60), WOOD_L = rgb(190, 136, 84), WOOD_D = rgb(104, 68, 42);
    private static final int STONE = rgb(150, 150, 162), STONE_D = rgb(104, 104, 118), STONE_L = rgb(196, 196, 208);

    static void register(Map<String, Sprite[]> m) {
        m.put("town.elder.idle", PeopleArt.frames(2, TownArt::elder, 10, 24));
        m.put("town.merchant.idle", PeopleArt.frames(2, TownArt::merchant, 10, 24));
        m.put("town.child.idle", PeopleArt.frames(2, TownArt::child, 9, 21));
    }

    private static void head(PixelCanvas c, int cx, int top, int hair, boolean bald) {
        c.ellipse(cx + 0.5, top + 5.2, 5.6, 5.2, SKIN);
        if (!bald) {
            c.ellipse(cx + 0.5, top + 3.2, 6.2, 4.4, hair);
            c.ellipse(cx + 0.5, top + 6.4, 4.7, 3.7, SKIN);
        }
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);
        c.set(cx - 4, top + 8, BLUSH); c.set(cx + 4, top + 8, BLUSH);
        c.rect(cx, top + 9, 2, 1, SKIN_D);
    }

    /** A stooped old traveller leaning on a cane, a patched grey cloak, a wide-brimmed hat. */
    private static PixelCanvas elder(int f) {
        PixelCanvas c = new PixelCanvas(21, 26);
        int cloak = rgb(120, 116, 140), cloakD = rgb(80, 76, 100), cloakL = rgb(160, 156, 180);
        int b = f == 0 ? 0 : -1;
        c.rect(7, 18 - (f == 1 ? 1 : 0), 3, 2, cloakD); c.rect(7, 20 - (f == 1 ? 1 : 0), 3, 2, WOOD_D);
        c.rect(11, 18, 3, 2, cloakD); c.rect(11, 20, 3, 2, WOOD_D);
        c.rect(6, 12 + b, 9, 8, cloak);
        c.rect(6, 12 + b, 9, 1, cloakL);
        c.rect(6, 18 + b, 9, 1, cloakD);
        c.rect(4, 13 + b, 2, 5, cloakD); c.rect(15, 13 + b, 2, 5, cloakD);
        c.rect(4, 18 + b, 2, 1, SKIN); c.rect(15, 18 + b, 2, 1, SKIN);
        c.rect(3, 8 + b, 1, 12, WOOD);                                             // the cane
        c.set(3, 7 + b, WOOD_D);
        head(c, 10, 3 + b, rgb(224, 224, 232), false);
        int top = 3 + b;
        c.ellipseY(10.5, top + 3, 6.4, 3.6, rgb(90, 86, 108), top - 1, top + 4);    // wide floppy hat
        c.rect(3, top + 3, 15, 2, rgb(90, 86, 108));
        c.rect(3, top + 4, 15, 1, rgb(64, 60, 82));
        c.bevel(0.14, 0.2);
        c.outline(OUT);
        return c;
    }

    /** A merchant with an empty pack, a flat cap and a friendly, tired smile. */
    private static PixelCanvas merchant(int f) {
        PixelCanvas c = new PixelCanvas(21, 26);
        int coat = rgb(176, 120, 62), coatD = rgb(126, 82, 40), coatL = rgb(216, 164, 100);
        int b = f == 0 ? 0 : -1;
        c.rect(7, 18, 3, 2, rgb(70, 56, 48)); c.rect(7, 20, 3, 2, WOOD_D);
        c.rect(11, 18, 3, 2, rgb(70, 56, 48)); c.rect(11, 20, 3, 2, WOOD_D);
        c.rect(2, 11 + b, 5, 8, WOOD_D);                                           // the empty pack on its back
        c.rect(2, 11 + b, 5, 1, WOOD_L);
        c.rect(6, 12 + b, 9, 7, coat);
        c.rect(6, 12 + b, 9, 1, coatL);
        c.rect(6, 17 + b, 9, 1, rgb(90, 60, 30));
        c.rect(4, 13 + b, 2, 5, coatD); c.rect(15, 13 + b, 2, 5, coatD);
        c.rect(4, 18 + b, 2, 1, SKIN); c.rect(15, 18 + b, 2, 1, SKIN);
        head(c, 10, 3 + b, rgb(90, 60, 34), false);
        int top = 3 + b;
        c.rect(6, top, 9, 2, rgb(70, 48, 30));                                     // flat cap
        c.rect(6, top + 1, 3, 1, rgb(50, 34, 20));
        c.bevel(0.14, 0.2);
        c.outline(OUT);
        return c;
    }

    /** A small child with a bright scarf, curious, standing on tiptoes to look up at you. */
    private static PixelCanvas child(int f) {
        PixelCanvas c = new PixelCanvas(18, 23);
        int shirt = rgb(90, 176, 210), shirtD = rgb(56, 128, 158), shirtL = rgb(140, 214, 236);
        int scarf = rgb(226, 90, 96);
        int b = f == 0 ? 0 : -1;
        c.rect(5, 16, 3, 2, rgb(70, 60, 96)); c.rect(5, 18, 3, 2, WOOD_D);
        c.rect(9, 16, 3, 2, rgb(70, 60, 96)); c.rect(9, 18, 3, 2, WOOD_D);
        c.rect(4, 11 + b, 8, 6, shirt);
        c.rect(4, 11 + b, 8, 1, shirtL);
        c.rect(4, 14 + b, 8, 1, shirtD);
        c.rect(3, 12 + b, 2, 4, shirtD); c.rect(11, 12 + b, 2, 4, shirtD);
        c.rect(3, 15 + b, 2, 1, SKIN); c.rect(11, 15 + b, 2, 1, SKIN);
        c.rect(3, 10 + b, 8, 2, scarf);
        head(c, 8, 1 + b, rgb(196, 150, 60), false);
        c.bevel(0.14, 0.2);
        c.outline(OUT);
        return c;
    }
}
