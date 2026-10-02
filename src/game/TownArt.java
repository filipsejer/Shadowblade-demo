package game;

import static game.PixelCanvas.*;

import java.util.Map;

import game.PeopleArt.Fine;

/**
 * Three of the forest's people: Elder Rowan (an old man with a cane, a white beard and a floppy hat), Hunter Fenn (a
 * hunter in a green hood with a quiver of arrows) and Wren (a small child with pigtails and a bright scarf). Painted in
 * the same chibi style as {@link PeopleArt}: the drawn sprites' fine pixels, shaded by a {@link Doll}.
 */
final class TownArt {
    private TownArt() {}

    private static final double UP = 1 / 1.5;   // one fine pixel, in old-grid units

    private static final Doll.Mat CLOAK_GREY = new Doll.Mat(rgb(34, 30, 50), rgb(76, 70, 98), rgb(110, 104, 134), rgb(148, 142, 174), rgb(186, 180, 210), false);
    private static final Doll.Mat HAT_DARK = new Doll.Mat(rgb(20, 16, 30), rgb(46, 40, 62), rgb(70, 64, 92), rgb(98, 92, 122), rgb(130, 124, 156), false);
    private static final Doll.Mat HAIR_WHITE = new Doll.Mat(rgb(88, 88, 112), rgb(170, 170, 192), rgb(216, 216, 232), rgb(240, 240, 250), rgb(255, 255, 255), true);
    private static final Doll.Mat HOOD_GREEN = new Doll.Mat(rgb(16, 38, 20), rgb(38, 78, 38), rgb(62, 116, 50), rgb(94, 152, 66), rgb(132, 188, 90), false);
    private static final Doll.Mat SHIRT_SKY = new Doll.Mat(rgb(18, 50, 78), rgb(50, 114, 158), rgb(86, 168, 210), rgb(138, 210, 236), rgb(190, 236, 250), false);
    private static final Doll.Mat SCARF_RED = new Doll.Mat(rgb(90, 20, 30), rgb(170, 48, 60), rgb(226, 88, 96), rgb(250, 138, 130), rgb(255, 188, 170), false);
    private static final Doll.Mat HAIR_BLONDE = new Doll.Mat(rgb(96, 58, 12), rgb(168, 118, 38), rgb(210, 164, 70), rgb(240, 204, 112), rgb(255, 236, 170), true);
    private static final Doll.Mat HAIR_DARK = new Doll.Mat(rgb(24, 14, 10), rgb(52, 32, 22), rgb(80, 52, 34), rgb(112, 76, 48), rgb(146, 104, 66), true);
    private static final Doll.Mat FEATHER = new Doll.Mat(rgb(90, 20, 20), rgb(178, 52, 40), rgb(226, 92, 60), rgb(250, 150, 100), rgb(255, 210, 160), false);

    static void register(Map<String, Sprite[]> m) {
        m.put("town.elder.idle", PeopleArt.fineFrames(2, TownArt::elder, Fine.p(10), Fine.p(24)));
        m.put("town.merchant.idle", PeopleArt.fineFrames(2, TownArt::hunter, Fine.p(10), Fine.p(24)));
        m.put("town.child.idle", PeopleArt.fineFrames(2, TownArt::child, Fine.p(9), Fine.p(21)));
    }

    /** A plain round head (old-grid centre column cx, top row top). */
    private static void head(Fine f, double cx, double top) {
        f.piece(Doll.SKIN).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
    }

    /** Legs and boots, standing; the left one lifted by {@code lift} (old-grid units). */
    private static void legs(Fine f, double x0, double y, Doll.Mat trousers, double lift) {
        f.piece(trousers).rect(x0, y - lift, 3, 2);
        f.piece(Doll.LEATHER).rect(x0, y + 2 - lift, 3, 2);
        f.piece(trousers).rect(x0 + 4, y, 3, 2);
        f.piece(Doll.LEATHER).rect(x0 + 4, y + 2, 3, 2);
    }

    /** Elder Rowan: stooped over a cane, a grey cloak, a long white beard under a wide, floppy hat. */
    private static PixelCanvas elder(int fr) {
        Fine f = new Fine(21, 26);
        double b = fr == 0 ? 0 : -UP;
        legs(f, 7, 18, CLOAK_GREY, fr == 1 ? UP : 0);
        f.piece(CLOAK_GREY).rect(6, 12 + b, 9, 7).rect(5, 17 + b, 11, 2);         // the cloak
        f.dot(Fine.p(10.5), Fine.p(14) + (fr == 0 ? 0 : -1), CLOAK_GREY, 0);
        f.dot(Fine.p(10.5), Fine.p(15) + (fr == 0 ? 0 : -1), CLOAK_GREY, 0);
        f.piece(Doll.WOOD).rect(3, 8 + b, 1, 12).rect(2.5, 7 + b, 2, 1.5);        // the cane, a knob at the top
        f.piece(CLOAK_GREY).rect(4, 13 + b, 2, 5);                                 // arms
        f.piece(Doll.SKIN).rect(3.5, 17.5 + b, 2, 1.5);                            // the hand on the cane
        f.piece(CLOAK_GREY).rect(15, 13 + b, 2, 5);
        f.piece(Doll.SKIN).rect(15, 18 + b, 2, 1);
        double top = 3 + b;
        head(f, 10, top);
        f.piece(HAIR_WHITE).ellipseY(10.5, top + 9.5, 4.6, 5.4, top + 8, top + 15.5);   // the long white beard
        f.piece(HAIR_WHITE).rect(6, top + 8, 9, 1).rect(5, top + 5, 1.5, 3).rect(14.5, top + 5, 1.5, 3);   // moustache and white hair at the sides
        f.piece(HAT_DARK).ellipseY(10.5, top + 3, 6.4, 3.6, top - 1, top + 4).rect(3, top + 3, 15, 2);   // the floppy hat
        f.dot(Fine.p(8), Fine.p(top) + 1, HAT_DARK, 2); f.dot(Fine.p(9), Fine.p(top), HAT_DARK, 2);
        PeopleArt.face(f, 10, top, 0, 0, true);
        for (int y = Fine.p(top + 10); y < Fine.p(top + 14); y += 2)          // the beard's strands f.dot(Fine.p(10.5), y, HAIR_WHITE, 0);
        f.dot(Fine.p(9), Fine.p(top + 12), HAIR_WHITE, 0); f.dot(Fine.p(12), Fine.p(top + 11), HAIR_WHITE, 0);
        return f.finish();
    }

    /** Hunter Fenn: a green hood with a red feather, a leather jerkin, a quiver of arrows over the shoulder. */
    private static PixelCanvas hunter(int fr) {
        Fine f = new Fine(21, 26);
        double b = fr == 0 ? 0 : -UP;
        legs(f, 7, 18, Doll.PANTS, 0);
        f.piece(Doll.LEATHER).rect(2, 7 + b, 4, 12);                               // the quiver on the back
        for (double x : new double[]{2.5, 3.8, 5}) {                               // arrows: shafts and red fletching
            f.piece(Doll.WOOD).rect(x, 4 + b, 1, 3);
            f.piece(FEATHER).rect(x - 0.3, 3 + b, 1.4, 1.5);
        }
        f.piece(Doll.LEATHER).rect(6, 12 + b, 9, 7).rect(5.5, 17 + b, 10, 2);      // the jerkin
        f.piece(HOOD_GREEN).rect(6, 12 + b, 9, 2);                                 // the hood's cape over the shoulders
        f.piece(Doll.WOOD).line(5, 12 + b, 14, 18 + b, 1);                         // the quiver's strap across the chest
        f.piece(HOOD_GREEN).rect(4, 13 + b, 2, 5);                                 // sleeves and hands
        f.piece(Doll.SKIN).rect(4, 18 + b, 2, 1);
        f.piece(HOOD_GREEN).rect(15, 13 + b, 2, 5);
        f.piece(Doll.SKIN).rect(15, 18 + b, 2, 1);
        double top = 3 + b;
        head(f, 10, top);
        f.piece(HAIR_DARK).rect(5, top + 4, 2, 4).rect(14, top + 4, 2, 4);   // hair showing at the sides
        f.piece(HOOD_GREEN).ellipseY(10.5, top + 3.6, 6.6, 5.0, top - 1.5, top + 5).tri(13, top, 18, top + 2, 15, top + 4);   // the hood, its point to the side
        f.piece(FEATHER).line(15, top - 1, 18, top - 4, 1);                        // the feather in it
        f.dot(Fine.p(9), Fine.p(top), HOOD_GREEN, 2);
        PeopleArt.face(f, 10, top, rgb(90, 70, 40), rgb(150, 120, 70), false);
        f.dot(Fine.p(14), Fine.p(top + 8.5), Doll.SKIN, 0);                        // a scar on the cheek
        return f.finish();
    }

    /** Wren: a small child in a sky-blue shirt and red scarf, fair hair in two pigtails. */
    private static PixelCanvas child(int fr) {
        Fine f = new Fine(18, 23);
        double b = fr == 0 ? 0 : -UP;
        f.piece(Doll.PANTS).rect(5, 16, 3, 2);
        f.piece(Doll.LEATHER).rect(5, 18, 3, 2);
        f.piece(Doll.PANTS).rect(9, 16, 3, 2);
        f.piece(Doll.LEATHER).rect(9, 18, 3, 2);
        f.piece(SHIRT_SKY).rect(4, 11 + b, 8, 6);                                  // the shirt
        f.piece(SHIRT_SKY).rect(3, 12 + b, 2, 4);                                  // arms
        f.piece(Doll.SKIN).rect(3, 15 + b, 2, 1);
        f.piece(SHIRT_SKY).rect(11, 12 + b, 2, 4);
        f.piece(Doll.SKIN).rect(11, 15 + b, 2, 1);
        f.piece(SCARF_RED).rect(3, 10 + b, 9, 2).rect(9, 12 + b, 2, 2.5);          // the scarf, one end hanging
        double top = 1 + b;
        f.piece(HAIR_BLONDE).disc(2.6, top + 6.5, 1.8).disc(14.4, top + 6.5, 1.8); // pigtails
        head(f, 8, top);
        f.piece(HAIR_BLONDE).ellipse(8.5, top + 3.2, 6.2, 4.4);                    // the hair
        f.join(Doll.SKIN).ellipse(8.5, top + 6.4, 4.7, 3.7);
        f.join(HAIR_BLONDE).rect(4, top + 3, 9, 1);
        f.d.set(Fine.p(5), Fine.p(top + 4)); f.d.set(Fine.p(9), Fine.p(top + 4)); f.d.set(Fine.p(12), Fine.p(top + 4));   // a ragged fringe
        f.dot(Fine.p(6), Fine.p(top + 1), HAIR_BLONDE, 3); f.dot(Fine.p(7), Fine.p(top + 1), HAIR_BLONDE, 2); f.dot(Fine.p(10), Fine.p(top + 1), HAIR_BLONDE, 2);
        PeopleArt.face(f, 8, top, rgb(70, 140, 90), rgb(120, 200, 130), false);
        return f.finish();
    }
}
