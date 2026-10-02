package game;

import static game.PixelCanvas.*;

import java.util.Map;

import game.PeopleArt.Fine;

/**
 * The people of Stormcliff, in the same chibi style as everyone else ({@link PeopleArt}, {@link CityFolk}): Doctor
 * Ilse in her lab coat, Fern who keeps the greenhouse, and behind their counters Brass the sparring automaton (the
 * trainer) and Quill the archivist (the merchant). Copper, the robot, is {@link LabArt}'s.
 */
final class LabFolk {
    private LabFolk() {}

    private static final double UP = 1 / 1.5;   // one fine pixel, in old-grid units

    private static final Doll.Mat COAT_WHITE = new Doll.Mat(rgb(86, 92, 104), rgb(178, 186, 196), rgb(218, 224, 230), rgb(240, 244, 248), rgb(255, 255, 255), false);
    private static final Doll.Mat HAIR_BLACK = new Doll.Mat(rgb(10, 8, 16), rgb(30, 26, 40), rgb(46, 40, 60), rgb(70, 62, 90), rgb(104, 96, 130), true);
    private static final Doll.Mat HAIR_WHITE = new Doll.Mat(rgb(90, 90, 104), rgb(176, 176, 190), rgb(214, 214, 224), rgb(236, 236, 244), rgb(255, 255, 255), true);
    private static final Doll.Mat HAIR_BROWN = Doll.HAIR_BROWN;
    private static final Doll.Mat SKIN_DARK = new Doll.Mat(rgb(50, 24, 20), rgb(104, 60, 42), rgb(138, 86, 58), rgb(170, 114, 78), rgb(200, 146, 104), true);
    private static final Doll.Mat BLOUSE = new Doll.Mat(rgb(18, 40, 50), rgb(40, 90, 104), rgb(60, 126, 140), rgb(96, 166, 176), rgb(150, 210, 214), false);
    private static final Doll.Mat APRON = new Doll.Mat(rgb(14, 40, 20), rgb(36, 92, 44), rgb(56, 130, 62), rgb(94, 170, 86), rgb(150, 210, 130), false);
    private static final Doll.Mat STRAW = new Doll.Mat(rgb(110, 76, 24), rgb(190, 146, 64), rgb(226, 190, 100), rgb(244, 220, 146), rgb(255, 244, 200), false);
    private static final Doll.Mat GLOVE = new Doll.Mat(rgb(70, 40, 16), rgb(150, 96, 44), rgb(196, 140, 70), rgb(226, 178, 108), rgb(250, 214, 150), true);
    private static final Doll.Mat ROBE = new Doll.Mat(rgb(30, 14, 18), rgb(70, 34, 40), rgb(100, 52, 58), rgb(134, 78, 80), rgb(170, 110, 106), false);
    private static final Doll.Mat BOOK_RED = new Doll.Mat(rgb(60, 10, 14), rgb(120, 28, 30), rgb(160, 46, 44), rgb(196, 76, 66), rgb(230, 120, 100), false);
    private static final Doll.Mat BOOK_BLUE = new Doll.Mat(rgb(10, 18, 54), rgb(26, 44, 110), rgb(42, 70, 150), rgb(76, 108, 190), rgb(130, 160, 230), false);

    static void register(Map<String, Sprite[]> m) {
        m.put("lab.ilse.idle", PeopleArt.fineFrames(2, LabFolk::ilse, Fine.p(10), Fine.p(24)));
        m.put("lab.fern.idle", PeopleArt.fineFrames(2, LabFolk::fern, Fine.p(11), Fine.p(24)));
        m.put("shop.brass", PeopleArt.fineFrames(2, i -> counter(0, i), Fine.p(17), Fine.p(45)));
        m.put("shop.quill", PeopleArt.fineFrames(2, i -> counter(1, i), Fine.p(17), Fine.p(45)));
    }

    // ------------------------------------------------------------------ helpers

    private static void head(Fine f, Doll.Mat skin, double cx, double top) {
        f.piece(skin).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
    }

    private static void legs(Fine f, double x0, double y, Doll.Mat trousers) {
        f.piece(trousers).rect(x0, y, 3, 2);
        f.piece(Doll.LEATHER).rect(x0, y + 2, 3, 2);
        f.piece(trousers).rect(x0 + 4, y, 3, 2);
        f.piece(Doll.LEATHER).rect(x0 + 4, y + 2, 3, 2);
    }

    /** Round spectacles over the eyes: two brass rims and a bridge. */
    private static void spectacles(Fine f, double cx, double top) {
        int ey = Fine.p(top + 6) - 1, el = Fine.p(cx - 3), er = Fine.p(cx + 3) - 1;
        for (int x : new int[]{el, er}) {
            f.dot(x - 2, ey, Doll.GOLD.mid()); f.dot(x + 2, ey, Doll.GOLD.mid());
            f.dot(x - 1, ey - 2, Doll.GOLD.light()); f.dot(x, ey - 2, Doll.GOLD.light()); f.dot(x + 1, ey - 2, Doll.GOLD.light());
            f.dot(x - 1, ey + 2, Doll.GOLD.dark()); f.dot(x, ey + 2, Doll.GOLD.dark()); f.dot(x + 1, ey + 2, Doll.GOLD.dark());
        }
        for (int x = el + 2; x <= er - 2; x++) f.dot(x, ey - 1, Doll.GOLD.mid());
    }

    // ------------------------------------------------------------------ the people about the laboratory

    /** Doctor Ilse: a long white lab coat over teal, black hair up in a bun with a pencil through it, spectacles, a clipboard. */
    private static PixelCanvas ilse(int fr) {
        Fine f = new Fine(21, 26);
        double b = fr == 0 ? 0 : -UP;
        legs(f, 7, 18, Doll.PANTS);
        f.piece(COAT_WHITE).rect(5.5, 12 + b, 10, 9).rect(5, 19 + b, 11, 2);   // the lab coat, long
        for (int y = Fine.p(13); y < Fine.p(21); y++) f.dot(Fine.p(10.5), y + (fr == 0 ? 0 : -1), COAT_WHITE, 0);
        f.piece(BLOUSE).rect(8.5, 12 + b, 4, 3);                                 // the blouse under it
        f.piece(COAT_WHITE).tri(7, 12 + b, 9.5, 12 + b, 9, 15 + b).tri(11.5, 12 + b, 14, 12 + b, 12, 15 + b);   // lapels
        f.piece(Doll.GOLD).rect(13, 14.5 + b, 1.5, 1);                           // a pen in the pocket
        f.piece(COAT_WHITE).rect(4, 13 + b, 2, 5);                                // arms: one hanging,
        f.piece(Doll.SKIN).rect(4, 18 + b, 2, 1);
        f.piece(COAT_WHITE).rect(15, 13 + b, 2, 3);                               // one holding the clipboard
        f.piece(Doll.WOOD).rect(14, 14.5 + b, 5, 6);
        f.piece(CityArt.CLOTH_WHITE).rect(14.6, 15.4 + b, 3.8, 4.6);
        for (int y = Fine.p(16.2); y < Fine.p(19.6); y += 2) for (int x = Fine.p(15); x < Fine.p(18); x++) f.dot(x, y + (fr == 0 ? 0 : -1), rgb(120, 120, 150));
        f.piece(Doll.STEEL).rect(15.8, 14.3 + b, 1.8, 0.8);
        f.piece(Doll.SKIN).rect(15, 16 + b, 1.5, 1.5);
        double top = 3 + b;
        f.piece(HAIR_BLACK).disc(10.5, top - 0.5, 2.8);                          // the bun, a pencil through it
        f.piece(Doll.GOLD).thickLine(7.5, top - 2, 13.5, top + 0.5, 1);
        head(f, Doll.SKIN, 10, top);
        f.join(HAIR_BLACK).ellipseY(10.5, top + 3.4, 6.2, 4.2, top - 1, top + 3.6)
            .rect(4.4, top + 3, 1.6, 4).rect(15, top + 3, 1.6, 4);
        f.dot(Fine.p(8), Fine.p(top) + 1, HAIR_BLACK, 3); f.dot(Fine.p(9), Fine.p(top) + 1, HAIR_BLACK, 3);
        PeopleArt.face(f, 10, top, rgb(70, 110, 150), rgb(120, 170, 210), false);
        spectacles(f, 10.5, top);
        return f.finish();
    }

    /** Fern: a wide straw hat with a star-bloom in its band, a green apron, big gardening gloves, a watering can. */
    private static PixelCanvas fern(int fr) {
        Fine f = new Fine(23, 26);
        double b = fr == 0 ? 0 : -UP;
        legs(f, 7.5, 18, Doll.PANTS);
        f.piece(CityArt.CLOTH_WHITE).rect(6, 12 + b, 10, 7);                      // a shirt,
        f.piece(APRON).rect(6.5, 13 + b, 9, 7).rect(6, 18 + b, 10, 2);            // the apron over it
        f.piece(APRON).rect(8, 11.5 + b, 6, 2);
        f.piece(Doll.LEATHER).rect(9.5, 15.5 + b, 3, 2);                          // its pocket, a trowel in it
        f.piece(Doll.STEEL).rect(10.5, 14 + b, 1, 2);
        f.piece(CityArt.CLOTH_WHITE).rect(4, 13 + b, 2, 3);
        f.piece(GLOVE).rect(3.5, 16 + b, 3, 2.5);
        f.piece(CityArt.CLOTH_WHITE).rect(16, 13 + b, 2, 3);
        f.piece(GLOVE).rect(16, 15.5 + b, 3, 2.5);
        f.piece(Doll.STEEL).rect(17, 16.5 + b, 5, 4);                             // the watering can
        f.piece(Doll.STEEL).thickLine(21.5, 17.5 + b, 23, 15.5 + b, 1);
        f.piece(Doll.STEEL).rect(18, 15 + b, 3, 1);
        double top = 3.5 + b;
        f.piece(HAIR_BROWN).ellipse(11, top + 6.8, 6.2, 4.8);                    // a long braid of brown hair
        f.piece(HAIR_BROWN).rect(14.5, top + 8, 2, 7);
        head(f, SKIN_DARK, 10.5, top);
        f.join(HAIR_BROWN).ellipseY(11, top + 3.6, 6, 4, top + 0.5, top + 3.8);
        f.piece(STRAW).ellipse(11, top + 2, 10.5, 2.6);                           // the hat's wide brim
        f.join(STRAW).ellipseY(11, top + 1, 5.5, 4.5, top - 3, top + 1.5);
        f.piece(APRON).rect(5.8, top - 0.2, 10.4, 1.2);                          // its band, and a star-bloom tucked in it
        f.dot(Fine.p(15), Fine.p(top), LabArt.STARBLOOM.high()); f.dot(Fine.p(15) + 1, Fine.p(top), LabArt.STARBLOOM.light()); f.dot(Fine.p(15), Fine.p(top) - 1, LabArt.STARBLOOM.light());
        PeopleArt.face(f, 10.5, top, rgb(90, 140, 60), rgb(140, 190, 90), false);
        f.dot(Fine.p(7.5), Fine.p(top + 8), Doll.BLUSH); f.dot(Fine.p(14), Fine.p(top + 8), Doll.BLUSH);
        return f.finish();
    }

    // ------------------------------------------------------------------ behind their counters

    /** A trainer or merchant behind a counter: 0 Brass the sparring automaton, 1 Quill the archivist. */
    private static PixelCanvas counter(int kind, int fr) {
        Fine f = new Fine(35, 47);
        double b = fr == 0 ? 0 : -UP;
        int fb = fr == 0 ? 0 : -1;
        int ox = 7, oy = 12;
        if (kind == 0) brass(f, ox, oy, b, fb, fr);
        else quill(f, ox, oy, b, fb);
        if (kind == 0) {                                                         // a sparring rail of iron, a practice dummy beside it
            Doll.Mat iron = CityArt.IRON;
            f.piece(iron).rect(3, 35, 29, 3).rect(3, 42, 29, 2);
            for (double x = 4; x < 32; x += 6) f.piece(iron).rect(x, 35, 1.5, 10);
            f.piece(LabArt.BRASS).rect(2, 34, 31, 1.2);
            f.piece(Doll.WOOD).rect(29, 22, 2, 13);                               // the dummy: a post and a straw sack
            f.piece(STRAW).ellipse(30, 21, 3.2, 4.2);
            f.piece(STRAW).disc(30, 15.5, 2.4);
            f.dot(Fine.p(29.5), Fine.p(15.3), Doll.EYE); f.dot(Fine.p(30.6), Fine.p(15.3), Doll.EYE);
            f.piece(CityArt.HYDRANT).rect(28, 19.5, 4, 1);                        // a red sash where it's been hit most
        } else {                                                                  // a desk stacked with books and specimen jars
            f.piece(Doll.WOOD).rect(3, 35, 29, 11);
            for (double x = 8; x < 32; x += 8) for (int y = Fine.p(37.5); y < Fine.p(45); y++) f.dot(Fine.p(x), y, Doll.WOOD, 0);
            f.piece(Doll.WOOD).rect(2, 34, 31, 1.5);
            f.piece(BOOK_RED).rect(4, 30, 7, 2);                                  // a stack of books
            f.piece(BOOK_BLUE).rect(4.5, 28, 6, 2);
            f.piece(APRON).rect(4, 26, 7, 2);
            f.piece(LabArt.GLASS).rect(24, 28.5, 4, 5.5);                         // jars: something green, something with eyes
            f.piece(LabArt.FLUID).rect(24.4, 30.5, 3.2, 3.2);
            f.piece(LabArt.BRASS).rect(23.8, 28, 4.4, 1);
            f.piece(LabArt.GLASS).rect(29, 30, 3, 4);
            f.dot(Fine.p(30), Fine.p(31.8), rgb(255, 230, 120)); f.dot(Fine.p(31), Fine.p(31.8), rgb(255, 230, 120));
            f.piece(CityArt.CLOTH_WHITE).rect(13, 32, 8, 2);                      // an open ledger, a quill in its inkpot
            f.piece(CityArt.IRON).rect(21.5, 31, 2, 2);
            f.piece(CityArt.CLOTH_WHITE).thickLine(22.5, 31, 24.5, 26, 1);
        }
        return f.finish();
    }

    /** Brass: a gleaming brass automaton, round-shouldered, with a visor of a face, a fencing foil for one hand. */
    private static void brass(Fine f, int ox, int oy, double b, int fb, int fr) {
        Doll.Mat brass = LabArt.BRASS;
        f.piece(brass).rect(ox + 5, oy + 12 + b, 11, 10);                          // the barrel chest
        f.piece(LabArt.COPPER).rect(ox + 7, oy + 14 + b, 7, 5);
        for (int x = Fine.p(ox + 7.5); x < Fine.p(ox + 13.5); x += 2) f.dot(x, Fine.p(oy + 16.5) + fb, LabArt.COPPER.high());   // a grille
        f.piece(brass).disc(ox + 4.5, oy + 13 + b, 2.6).disc(ox + 16.5, oy + 13 + b, 2.6);   // shoulders
        f.piece(brass).rect(ox + 3, oy + 14 + b, 3, 6);                             // arms
        f.piece(CityArt.IRON).rect(ox + 3, oy + 20 + b, 3, 2);
        f.piece(brass).rect(ox + 15, oy + 14 + b, 3, 5);
        f.piece(CityArt.IRON).rect(ox + 15, oy + 19 + b, 3, 2);
        f.piece(Doll.STEEL).thickLine(ox + 17, oy + 20 + b, ox + 24, oy + 6 + b + fr, 1);   // the foil, raised in salute
        f.piece(Doll.GOLD).disc(ox + 17, oy + 20 + b, 1.2);
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(brass).ellipse(cx + 0.5, top + 5.2, 5.6, 5.4);                      // the head, a domed helm
        f.piece(CityArt.IRON).rect(cx - 4, top + 5, 9, 2.4);                       // the visor's slit
        f.dot(Fine.p(cx - 2), Fine.p(top + 6), rgb(120, 230, 255)); f.dot(Fine.p(cx + 3), Fine.p(top + 6), rgb(120, 230, 255));   // eyes, lit blue
        f.piece(LabArt.COPPER).rect(cx, top - 2.5, 1, 2.5);                        // a plume-spike on top
        f.dot(Fine.p(cx), Fine.p(top) - 4, rgb(255, 120, 100));
        f.dot(Fine.p(cx - 2.5), Fine.p(top + 1.5), brass.high()); f.dot(Fine.p(cx - 1.5), Fine.p(top + 1), brass.high());
    }

    /** Quill: an old archivist in a plum robe, white hair in a wild halo, spectacles on a chain, ink on the fingers. */
    private static void quill(Fine f, int ox, int oy, double b, int fb) {
        f.piece(ROBE).rect(ox + 5, oy + 12 + b, 11, 12);
        for (int y = Fine.p(oy + 14); y < Fine.p(oy + 24); y++) f.dot(Fine.p(ox + 10.5), y + fb, ROBE, 0);
        f.piece(Doll.GOLD).rect(ox + 6, oy + 16 + b, 9, 1);                       // a sash
        f.piece(ROBE).rect(ox + 3.5, oy + 13 + b, 3, 6);
        f.piece(Doll.SKIN).rect(ox + 3.5, oy + 19 + b, 3, 2);
        f.piece(ROBE).rect(ox + 14.5, oy + 13 + b, 3, 6);
        f.piece(Doll.SKIN).rect(ox + 14.5, oy + 19 + b, 3, 2);
        f.dot(Fine.p(ox + 15), Fine.p(oy + 20.5) + fb, rgb(30, 30, 80)); f.dot(Fine.p(ox + 16), Fine.p(oy + 20.5) + fb, rgb(30, 30, 80));   // ink stains
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(HAIR_WHITE).disc(cx - 4.5, top + 4, 3).disc(cx + 5.5, top + 4, 3).disc(cx + 0.5, top + 0.5, 3.5);   // a wild white halo
        head(f, Doll.SKIN, cx, top);
        f.join(HAIR_WHITE).ellipseY(cx + 0.5, top + 3.2, 6, 4, top + 0.5, top + 2.6);
        PeopleArt.face(f, cx, top, 0, 0, true);
        spectacles(f, cx + 0.5, top + 0.5);
        for (int i = 0; i < 6; i++) f.dot(Fine.p(cx + 5) + (i % 2), Fine.p(top + 7) + i * 2, Doll.GOLD.mid());   // the spectacles' chain
        int ey = Fine.p(top + 6) - 3;
        for (int dx = -2; dx <= 1; dx++) { f.dot(Fine.p(cx - 2.5) + dx, ey, HAIR_WHITE, 1); f.dot(Fine.p(cx + 3.5) + dx + 1, ey, HAIR_WHITE, 1); }   // bushy brows
    }
}
