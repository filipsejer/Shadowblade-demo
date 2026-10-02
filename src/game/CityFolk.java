package game;

import static game.PixelCanvas.*;

import java.util.Map;

import game.PeopleArt.Fine;

/**
 * The people of Lumen, painted in the same chibi style as everyone else ({@link PeopleArt}, {@link TownArt}): Captain
 * Vell of the Watch, Tinker Juno, Old Gus the rat-catcher, Pip the newsboy, and behind their counters Sable the
 * fencing master (the city's trainer) and Nix the peddler (its merchant).
 */
final class CityFolk {
    private CityFolk() {}

    private static final double UP = 1 / 1.5;   // one fine pixel, in old-grid units

    private static final Doll.Mat SKIN_TAN = new Doll.Mat(rgb(96, 44, 34), rgb(158, 98, 70), rgb(196, 134, 96), rgb(222, 166, 124), rgb(240, 196, 158), true);
    private static final Doll.Mat HAIR_RED = new Doll.Mat(rgb(70, 16, 12), rgb(140, 40, 26), rgb(190, 66, 38), rgb(226, 104, 58), rgb(250, 156, 96), true);
    private static final Doll.Mat HAIR_AUBURN = new Doll.Mat(rgb(36, 14, 12), rgb(70, 30, 22), rgb(102, 46, 30), rgb(138, 68, 42), rgb(176, 98, 62), true);
    private static final Doll.Mat HAIR_GREY = new Doll.Mat(rgb(70, 70, 86), rgb(136, 136, 152), rgb(176, 176, 190), rgb(206, 206, 218), rgb(236, 236, 244), true);
    private static final Doll.Mat HAIR_SILVER = new Doll.Mat(rgb(64, 70, 96), rgb(150, 158, 186), rgb(194, 200, 222), rgb(222, 228, 244), rgb(255, 255, 255), true);
    private static final Doll.Mat HAIR_TEAL = new Doll.Mat(rgb(8, 40, 46), rgb(20, 96, 100), rgb(36, 146, 142), rgb(76, 196, 180), rgb(150, 236, 216), true);
    private static final Doll.Mat HAIR_GINGER = new Doll.Mat(rgb(92, 34, 10), rgb(168, 76, 26), rgb(214, 116, 44), rgb(242, 156, 74), rgb(255, 204, 130), true);
    private static final Doll.Mat TABARD = new Doll.Mat(rgb(12, 18, 54), rgb(28, 44, 112), rgb(44, 70, 160), rgb(80, 112, 206), rgb(132, 164, 240), false);
    private static final Doll.Mat CAPE = new Doll.Mat(rgb(10, 14, 40), rgb(22, 32, 86), rgb(34, 50, 124), rgb(58, 80, 166), rgb(96, 124, 206), false);
    private static final Doll.Mat OVERALLS = new Doll.Mat(rgb(96, 34, 6), rgb(184, 84, 22), rgb(232, 124, 40), rgb(252, 166, 74), rgb(255, 210, 130), false);
    private static final Doll.Mat COAT_BROWN = new Doll.Mat(rgb(36, 22, 14), rgb(78, 52, 32), rgb(108, 76, 46), rgb(140, 104, 66), rgb(172, 136, 92), false);
    private static final Doll.Mat SCARF_GREEN = new Doll.Mat(rgb(12, 44, 24), rgb(32, 96, 52), rgb(52, 140, 74), rgb(94, 186, 104), rgb(150, 226, 150), false);
    private static final Doll.Mat CAP_GREY = new Doll.Mat(rgb(30, 28, 32), rgb(70, 66, 70), rgb(98, 92, 94), rgb(128, 122, 120), rgb(160, 154, 150), false);
    private static final Doll.Mat SHORTS = new Doll.Mat(rgb(40, 24, 14), rgb(88, 56, 34), rgb(120, 82, 50), rgb(150, 110, 72), rgb(180, 140, 98), false);
    private static final Doll.Mat COAT_PURPLE = new Doll.Mat(rgb(28, 10, 40), rgb(62, 26, 86), rgb(92, 44, 124), rgb(128, 74, 164), rgb(170, 116, 204), false);
    private static final Doll.Mat MASK = new Doll.Mat(rgb(6, 4, 12), rgb(16, 12, 24), rgb(26, 20, 36), rgb(42, 34, 54), rgb(70, 60, 86), true);
    private static final Doll.Mat PATCH_ORANGE = new Doll.Mat(rgb(90, 36, 10), rgb(172, 82, 28), rgb(220, 122, 50), rgb(246, 164, 86), rgb(255, 206, 140), false);
    private static final Doll.Mat PATCH_PLUM = new Doll.Mat(rgb(40, 12, 34), rgb(92, 34, 78), rgb(132, 56, 112), rgb(170, 88, 148), rgb(208, 130, 184), false);
    private static final Doll.Mat GLASS = new Doll.Mat(rgb(20, 50, 70), rgb(70, 140, 170), rgb(120, 196, 220), rgb(180, 232, 246), rgb(240, 255, 255), true);
    private static final Doll.Mat BAG = new Doll.Mat(rgb(110, 98, 74), rgb(170, 150, 110), rgb(202, 184, 142), rgb(226, 212, 176), rgb(244, 236, 210), false);
    private static final Doll.Mat RAT = new Doll.Mat(rgb(30, 28, 34), rgb(76, 72, 82), rgb(106, 100, 112), rgb(138, 132, 142), rgb(172, 166, 176), true);

    static void register(Map<String, Sprite[]> m) {
        m.put("city.vell.idle", PeopleArt.fineFrames(2, CityFolk::vell, Fine.p(10), Fine.p(24)));
        m.put("city.juno.idle", PeopleArt.fineFrames(2, CityFolk::juno, Fine.p(10), Fine.p(24)));
        m.put("city.gus.idle", PeopleArt.fineFrames(2, CityFolk::gus, Fine.p(11), Fine.p(24)));
        m.put("city.pip.idle", PeopleArt.fineFrames(2, CityFolk::pip, Fine.p(9), Fine.p(21)));
        m.put("shop.duelist", PeopleArt.fineFrames(2, i -> counter(0, i), Fine.p(17), Fine.p(45)));
        m.put("shop.peddler", PeopleArt.fineFrames(2, i -> counter(1, i), Fine.p(17), Fine.p(45)));
    }

    // ------------------------------------------------------------------ helpers

    /** A round head like everyone's (old-grid centre column cx, top row top). */
    private static void head(Fine f, Doll.Mat skin, double cx, double top) {
        f.piece(skin).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
    }

    /** Legs and boots, standing. */
    private static void legs(Fine f, double x0, double y, Doll.Mat trousers) {
        f.piece(trousers).rect(x0, y, 3, 2);
        f.piece(Doll.LEATHER).rect(x0, y + 2, 3, 2);
        f.piece(trousers).rect(x0 + 4, y, 3, 2);
        f.piece(Doll.LEATHER).rect(x0 + 4, y + 2, 3, 2);
    }

    /** Arms hanging at the sides: sleeves and hands. */
    private static void arms(Fine f, Doll.Mat sleeve, Doll.Mat skin, double b, double lx, double rx) {
        f.piece(sleeve).rect(lx, 13 + b, 2, 5);
        f.piece(skin).rect(lx, 18 + b, 2, 1);
        f.piece(sleeve).rect(rx, 13 + b, 2, 5);
        f.piece(skin).rect(rx, 18 + b, 2, 1);
    }

    /** A pair of brows over the eyes ({@code frown}: slanting in, stern). */
    private static void brows(Fine f, Doll.Mat hair, double cx, double top, boolean frown) {
        int el = Fine.p(cx - 3), er = Fine.p(cx + 3) - 1, ey = Fine.p(top + 6) - 3;
        f.dot(el - 1, ey + (frown ? 0 : 1), hair, 0); f.dot(el, ey + 1, hair, 0); f.dot(el + 1, ey + (frown ? 2 : 1), hair, 0);
        f.dot(er + 2, ey + (frown ? 0 : 1), hair, 0); f.dot(er + 1, ey + 1, hair, 0); f.dot(er, ey + (frown ? 2 : 1), hair, 0);
    }

    // ------------------------------------------------------------------ the people in the streets

    /** Captain Vell: a red bob, a steel breastplate over the Watch's blue, a cape, a sword at her hip. */
    private static PixelCanvas vell(int fr) {
        Fine f = new Fine(21, 26);
        double b = fr == 0 ? 0 : -UP;
        f.piece(CAPE).poly(new double[]{5, 16, 17.5, 3.5}, new double[]{12 + b, 12 + b, 22.5, 22.5});   // the cape, falling behind
        legs(f, 7, 18, Doll.PANTS);
        f.piece(TABARD).rect(6, 12 + b, 9, 7).rect(5.5, 17 + b, 10, 2);        // the Watch's blue
        f.piece(Doll.STEEL).rect(6.5, 12 + b, 8, 5);                            // the breastplate
        f.dot(Fine.p(10.5), Fine.p(13) + (fr == 0 ? 0 : -1), Doll.GOLD.light()); f.dot(Fine.p(10.5), Fine.p(14) + (fr == 0 ? 0 : -1), Doll.GOLD.mid());   // the Watch's badge
        f.piece(Doll.LEATHER).rect(6, 17 + b, 9, 1);                            // the belt
        f.piece(Doll.GOLD).rect(14.5, 16.5 + b, 1, 3);                          // the sword's hilt, at her hip
        f.piece(Doll.STEEL).rect(14.8, 19.5 + b, 0.8, 3);
        arms(f, TABARD, Doll.SKIN, b, 4, 15);
        f.piece(Doll.STEEL).rect(3.5, 12 + b, 3, 2).rect(14.5, 12 + b, 3, 2);   // pauldrons
        double top = 3 + b;
        f.piece(HAIR_RED).ellipse(10.5, top + 6.4, 6.6, 5.4);                   // the bob, behind the head
        head(f, Doll.SKIN, 10, top);
        f.join(HAIR_RED).ellipseY(10.5, top + 3.4, 6.2, 4.2, top - 1.2, top + 3.6)   // the crown and a fringe swept to one side
            .tri(4.5, top + 3, 9, top + 2, 4.5, top + 7).rect(4.4, top + 4, 1.6, 6).rect(15, top + 4, 1.6, 6);
        f.dot(Fine.p(8), Fine.p(top), HAIR_RED, 3); f.dot(Fine.p(9), Fine.p(top) + 1, HAIR_RED, 2); f.dot(Fine.p(13), Fine.p(top) + 1, HAIR_RED, 2);
        PeopleArt.face(f, 10, top, rgb(60, 130, 80), rgb(110, 190, 120), false);
        brows(f, HAIR_RED, 10, top, true);
        return f.finish();
    }

    /** Tinker Juno: orange overalls, a tool belt, goggles pushed up into a cloud of curls, a big wrench on her shoulder. */
    private static PixelCanvas juno(int fr) {
        Fine f = new Fine(22, 26);
        double b = fr == 0 ? 0 : -UP;
        legs(f, 7, 18, OVERALLS);
        f.piece(Doll.STEEL).thickLine(16, 19 + b, 19.5, 6 + b, 1);              // the wrench, over her shoulder
        f.piece(Doll.STEEL).rect(18, 3.5 + b, 3, 2).rect(18, 6 + b, 1, 1);
        f.piece(OVERALLS).rect(6, 12 + b, 9, 7).rect(5.5, 17 + b, 10, 2);       // the overalls
        f.piece(OVERALLS).rect(7.5, 11.5 + b, 6, 2);                            // their bib
        f.dot(Fine.p(8), Fine.p(12.5) + (fr == 0 ? 0 : -1), Doll.GOLD.light()); f.dot(Fine.p(13), Fine.p(12.5) + (fr == 0 ? 0 : -1), Doll.GOLD.light());
        f.piece(Doll.LEATHER).rect(5.5, 16.5 + b, 10, 1.5);                     // the tool belt, tools hanging off it
        f.piece(Doll.STEEL).rect(7, 18 + b, 1, 2);
        f.piece(Doll.WOOD).rect(12.5, 18 + b, 1, 2);
        f.piece(Doll.GOLD).rect(10, 17.8 + b, 1.5, 1);
        f.piece(OVERALLS).rect(4, 13 + b, 2, 2);                                // rolled-up sleeves, bare forearms
        f.piece(SKIN_TAN).rect(4, 15 + b, 2, 4);
        f.piece(OVERALLS).rect(15, 13 + b, 2, 2);
        f.piece(SKIN_TAN).rect(15, 15 + b, 2, 3);
        f.piece(SKIN_TAN).rect(15.5, 17.5 + b, 2.5, 1.5);                        // the hand on the wrench
        double top = 3 + b;
        f.piece(HAIR_AUBURN).disc(5.2, top + 6, 2.6).disc(15.8, top + 6, 2.6);    // curls bursting out at the sides
        head(f, SKIN_TAN, 10, top);
        f.join(HAIR_AUBURN).ellipseY(10.5, top + 3.2, 6.4, 4.4, top - 1.5, top + 3.4)
            .disc(6.5, top + 1.2, 2.4).disc(10.5, top - 0.4, 2.6).disc(14.5, top + 1.2, 2.4).disc(5, top + 3.8, 1.8).disc(16, top + 3.8, 1.8);
        f.piece(Doll.LEATHER).rect(4.5, top + 1.6, 12, 1.2);                    // the goggles' strap
        f.piece(Doll.GOLD).disc(8, top + 1.4, 1.9).disc(13, top + 1.4, 1.9);     // and their brass rims
        f.piece(GLASS).disc(8, top + 1.4, 1.1).disc(13, top + 1.4, 1.1);
        PeopleArt.face(f, 10, top, rgb(120, 80, 40), rgb(180, 130, 70), false);
        f.dot(Fine.p(13.5), Fine.p(top + 8.5), SKIN_TAN, 0); f.dot(Fine.p(13.5) + 1, Fine.p(top + 8.5), SKIN_TAN, 0);   // a smudge of soot
        return f.finish();
    }

    /** Old Gus the rat-catcher: a long brown coat and a green scarf, bald with grey mutton chops, a trap on a pole, a pet rat on his shoulder. */
    private static PixelCanvas gus(int fr) {
        Fine f = new Fine(22, 26);
        double b = fr == 0 ? 0 : -UP;
        f.piece(Doll.WOOD).rect(3, 4 + b, 1, 19);                                // the pole, a cage-trap hung from its top
        f.piece(Doll.STEEL).rect(1, 2 + b, 5, 4);
        for (int x = Fine.p(1.5); x < Fine.p(6); x += 2) for (int y = Fine.p(2) + (fr == 0 ? 1 : 0); y < Fine.p(6); y++) f.dot(x, y, Doll.STEEL, 0);
        legs(f, 7.5, 18, Doll.PANTS);
        f.piece(COAT_BROWN).rect(6, 12 + b, 10, 8).rect(5.5, 18 + b, 11, 2.5);  // the long coat
        for (int y = Fine.p(13); y < Fine.p(20); y++) f.dot(Fine.p(11), y + (fr == 0 ? 0 : -1), COAT_BROWN, 0);   // its front
        f.dot(Fine.p(10), Fine.p(14.5) + (fr == 0 ? 0 : -1), Doll.GOLD.mid()); f.dot(Fine.p(10), Fine.p(17) + (fr == 0 ? 0 : -1), Doll.GOLD.mid());
        f.piece(COAT_BROWN).rect(4, 13 + b, 2.5, 5);                            // the arm holding the pole
        f.piece(Doll.SKIN).rect(3, 17 + b, 2.5, 1.5);
        f.piece(COAT_BROWN).rect(15.5, 13 + b, 2.5, 5);
        f.piece(Doll.SKIN).rect(15.5, 18 + b, 2.5, 1);
        f.piece(SCARF_GREEN).rect(7, 11.5 + b, 8, 2).rect(12.5, 13 + b, 2, 3);
        double top = 3.5 + b;
        head(f, Doll.SKIN, 10.5, top);
        f.piece(HAIR_GREY).rect(4.6, top + 4, 2, 5).rect(15.6, top + 4, 2, 5);   // grey mutton chops
        f.join(HAIR_GREY).ellipseY(6, top + 9, 1.8, 2, top + 7, top + 11).ellipseY(16.5, top + 9, 1.8, 2, top + 7, top + 11);
        f.dot(Fine.p(9), Fine.p(top) + 2, Doll.SKIN, 3); f.dot(Fine.p(10), Fine.p(top) + 2, Doll.SKIN, 3);   // the shine on his bald head
        PeopleArt.face(f, 10.5, top, 0, 0, true);
        int ey = Fine.p(top + 6) - 3;
        for (int dx = -2; dx <= 1; dx++) { f.dot(Fine.p(7.5) + dx, ey, HAIR_GREY, 1); f.dot(Fine.p(13.5) + dx + 1, ey, HAIR_GREY, 1); }   // bushy brows
        f.dot(Fine.p(11), Fine.p(top + 7.6), rgb(226, 120, 100)); f.dot(Fine.p(11) + 1, Fine.p(top + 7.6), rgb(200, 96, 84));           // a red nose
        // the rat on his shoulder
        f.piece(RAT).ellipse(18, 12.6 + b, 2.4, 1.5);
        f.piece(RAT).disc(20.2, 11.6 + b, 1.2);
        f.dot(Fine.p(19.8), Fine.p(10.5 + b), rgb(240, 150, 160)); f.dot(Fine.p(20.8), Fine.p(11.5 + b), Doll.EYE);
        f.dot(Fine.p(21.4), Fine.p(12 + b), rgb(240, 150, 160));
        for (int i = 0; i < 4; i++) f.dot(Fine.p(16) - i, Fine.p(13.6 + b) + (i > 1 ? 1 : 0) + (fr == 0 || i < 2 ? 0 : 1), rgb(220, 140, 150));   // its tail, flicking
        return f.finish();
    }

    /** Pip the newsboy: a flat cap, freckles, braces over a white shirt, a satchel of papers, one held up. */
    private static PixelCanvas pip(int fr) {
        Fine f = new Fine(18, 23);
        double b = fr == 0 ? 0 : -UP;
        f.piece(Doll.PANTS).rect(5, 16, 3, 2);
        f.piece(Doll.LEATHER).rect(5, 18, 3, 2);
        f.piece(Doll.PANTS).rect(9, 16, 3, 2);
        f.piece(Doll.LEATHER).rect(9, 18, 3, 2);
        f.piece(SHORTS).rect(4.5, 14 + b, 8, 2.5);
        f.piece(CityArt.CLOTH_WHITE).rect(4, 10.5 + b, 9, 4);                    // the shirt
        f.piece(Doll.PANTS).rect(5.5, 10.5 + b, 1, 4).rect(10.5, 10.5 + b, 1, 4); // braces
        f.piece(BAG).rect(10, 12 + b, 4, 4);                          // the satchel, papers sticking out
        f.piece(CityArt.CLOTH_WHITE).rect(10.5, 10.8 + b, 3, 1.5);
        f.piece(CityArt.CLOTH_WHITE).rect(3, 11 + b, 2, 3.5);                    // arms
        f.piece(Doll.SKIN).rect(3, 14.5 + b, 2, 1);
        f.piece(CityArt.CLOTH_WHITE).rect(13, 9.5 + b, 2, 3);                    // the other one raised, a paper in its hand
        f.piece(Doll.SKIN).rect(13, 8.5 + b, 2, 1.5);
        f.piece(CityArt.CLOTH_WHITE).rect(13.5, 5 + b, 4, 4);
        for (int y = Fine.p(5.5); y < Fine.p(8.5); y += 2) for (int x = Fine.p(14); x < Fine.p(17); x++) f.dot(x, y + (fr == 0 ? 0 : -1), rgb(120, 120, 140));   // headlines
        double top = 0.5 + b;
        f.piece(HAIR_GINGER).ellipse(8.5, top + 5.2, 5.8, 4.8);                  // ginger hair, poking out under the cap
        head(f, Doll.SKIN, 8, top);
        f.join(HAIR_GINGER).rect(3, top + 4, 2, 3).rect(12, top + 4, 2, 3).tri(4, top + 4, 7, top + 3, 5, top + 6);
        f.piece(CAP_GREY).ellipseY(8.5, top + 3.4, 6.2, 3.8, top - 0.6, top + 3.6);   // the flat cap and its brim
        f.piece(CAP_GREY).rect(5, top + 3, 9, 1.2);
        f.dot(Fine.p(7), Fine.p(top) + 1, CAP_GREY, 3);
        PeopleArt.face(f, 8, top, rgb(70, 120, 180), rgb(120, 170, 230), false);
        for (int[] p : new int[][]{{-3, 4}, {-2, 5}, {3, 4}, {2, 5}}) f.dot(Fine.p(8.5 + p[0]), Fine.p(top + 6) + p[1], rgb(200, 120, 80));   // freckles
        return f.finish();
    }

    // ------------------------------------------------------------------ behind their counters

    /** A trainer or merchant behind a counter: 0 Sable the fencing master, 1 Nix the peddler. */
    private static PixelCanvas counter(int kind, int fr) {
        Fine f = new Fine(35, 47);
        double b = fr == 0 ? 0 : -UP;
        int fb = fr == 0 ? 0 : -1;
        int ox = 7, oy = 12;
        if (kind == 0) sable(f, ox, oy, b, fb);
        else nix(f, ox, oy, b, fb);
        if (kind == 0) {                                                         // dark polished wood, a rack of foils on top
            Doll.Mat dark = CityArt.TIMBER;
            f.piece(dark).rect(3, 35, 29, 11);
            for (double x = 8; x < 32; x += 8) for (int y = Fine.p(37.5); y < Fine.p(45); y++) f.dot(Fine.p(x), y, dark, 0);
            for (int x = Fine.p(3.5); x < Fine.p(31.5); x++) f.dot(x, Fine.p(35) + 1, dark, 2);
            f.piece(dark).rect(2, 34, 31, 1.5);
            for (int i = 0; i < 3; i++) {                                         // foils lying on it, bell guards and all
                double y = 31.5 + i * 1.0;
                f.piece(Doll.STEEL).rect(5 + i * 2, y, 14, 0.7);
                f.piece(Doll.GOLD).disc(19.5 + i * 2, y + 0.3, 0.9);
            }
            f.piece(CityArt.IRON).ellipse(27, 32, 3, 2.6);                       // a fencing mask
            for (int x = Fine.p(25); x < Fine.p(29); x += 2) for (int y = Fine.p(30); y < Fine.p(34); y++) f.dot(x, y, CityArt.IRON.light());
        } else {                                                                  // a painted handcart, a string of little lights over it
            f.piece(PATCH_PLUM).rect(3, 35, 29, 9);
            for (int x = Fine.p(3.5); x < Fine.p(31.5); x++) f.dot(x, Fine.p(35) + 1, PATCH_PLUM, 2);
            for (int x = Fine.p(4); x < Fine.p(31); x += 6) for (int y = Fine.p(37); y < Fine.p(43); y++) f.dot(x, y, PATCH_PLUM, 0);
            f.piece(Doll.GOLD).rect(2, 34, 31, 1.2);
            f.piece(Doll.WOOD).disc(8, 44, 2.6).disc(27, 44, 2.6);               // its wheels
            f.piece(CityArt.IRON).disc(8, 44, 0.8).disc(27, 44, 0.8);
            f.piece(Doll.WOOD).rect(31, 14, 1, 21);                                // a pole with a lantern
            f.piece(CityArt.IRON).rect(30, 13, 3, 1);
            f.piece(CityArt.WIN_LIT).rect(30.3, 14.3, 2.4, 2.6);
            int[] bulbs = {rgb(255, 110, 180), rgb(110, 240, 255), rgb(255, 230, 110), rgb(160, 255, 140)};
            for (int i = 0; i < 9; i++) {                                         // the string of lights, sagging from the pole
                double x = 3 + i * 3.4, y = 14 + Math.sin(i / 8.0 * Math.PI) * 2.5;
                f.dot(Fine.p(x), Fine.p(y), CityArt.IRON.mid());
                f.dot(Fine.p(x), Fine.p(y) + 1, bulbs[(i + fr) % bulbs.length]);
            }
            f.piece(new Doll.Mat(rgb(10, 40, 20), rgb(30, 110, 60), rgb(50, 160, 90), rgb(110, 210, 140), rgb(200, 255, 220), true)).rect(6, 31, 2, 3).rect(6.5, 30, 1, 1);   // bottles
            f.piece(GLASS).rect(9.5, 30.5, 2, 3.5).rect(10, 29.5, 1, 1);
            f.piece(Doll.LEATHER).rect(15, 31.5, 4, 2.5).rect(15, 30, 1.5, 2);   // a boot
            f.piece(CityArt.CLOTH_WHITE).rect(21, 32, 6, 2);                      // rolled scrolls
            f.piece(CityArt.CLOTH_WHITE).rect(22, 30.5, 5, 1.6);
        }
        return f.finish();
    }

    /** Sable: a long plum coat with a high collar, a white cravat, silver hair tied back, a black domino mask. */
    private static void sable(Fine f, int ox, int oy, double b, int fb) {
        f.piece(COAT_PURPLE).rect(ox + 5, oy + 12 + b, 11, 12);                   // the coat
        f.piece(COAT_PURPLE).rect(ox + 4.5, oy + 10 + b, 3, 4).rect(ox + 13.5, oy + 10 + b, 3, 4);   // its high collar
        for (int y = Fine.p(oy + 14); y < Fine.p(oy + 24); y++) f.dot(Fine.p(ox + 10.5), y + fb, COAT_PURPLE, 0);
        for (double y = oy + 15; y < oy + 23; y += 2.5) f.dot(Fine.p(ox + 9.5), Fine.p(y) + fb, Doll.GOLD.light());   // brass buttons
        f.piece(CityArt.CLOTH_WHITE).rect(ox + 8.5, oy + 11.5 + b, 4, 2.5).tri(ox + 9, oy + 14 + b, ox + 12, oy + 14 + b, ox + 10.5, oy + 16.5 + b);   // the cravat
        f.piece(COAT_PURPLE).rect(ox + 3.5, oy + 13 + b, 3, 6);                    // arms, gloved hands
        f.piece(MASK).rect(ox + 3.5, oy + 19 + b, 3, 2);
        f.piece(COAT_PURPLE).rect(ox + 14.5, oy + 13 + b, 3, 6);
        f.piece(MASK).rect(ox + 14.5, oy + 19 + b, 3, 2);
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(HAIR_SILVER).disc(cx + 6.5, top + 7, 2).ellipse(cx + 7.5, top + 10, 1.4, 2.6);   // the tail of hair, tied back
        f.piece(Doll.SKIN).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
        f.piece(HAIR_SILVER).ellipseY(cx + 0.5, top + 3.4, 6.0, 4.2, top - 1, top + 3.8);   // hair swept back
        f.join(HAIR_SILVER).rect(cx - 5.4, top + 3, 1.4, 4).rect(cx + 5, top + 3, 1.4, 4);
        f.dot(Fine.p(cx - 2), Fine.p(top) + 1, HAIR_SILVER, 3); f.dot(Fine.p(cx - 1), Fine.p(top) + 1, HAIR_SILVER, 3); f.dot(Fine.p(cx + 2), Fine.p(top) + 2, HAIR_SILVER, 2);
        f.piece(MASK).rect(cx - 4, top + 5.2, 9, 2);                               // the mask, the eyes showing through it
        f.join(MASK).tri(cx - 5, top + 4.8, cx - 3.4, top + 5.4, cx - 4, top + 6.6).tri(cx + 6, top + 4.8, cx + 4.4, top + 5.4, cx + 5, top + 6.6);
        PeopleArt.face(f, cx, top, rgb(200, 160, 255), rgb(240, 210, 255), false);
        f.dot(Fine.p(cx - 3) + 3, Fine.p(top + 6) + 4, Doll.MOUTH);                // a crooked half-smile
    }

    /** Nix: spiky teal hair, a patchwork coat of orange and plum, a gold earring and a wide grin. */
    private static void nix(Fine f, int ox, int oy, double b, int fb) {
        f.piece(PATCH_ORANGE).rect(ox + 5.5, oy + 12 + b, 10, 12);                 // the patchwork coat
        f.piece(PATCH_PLUM).rect(ox + 5.5, oy + 16 + b, 4, 4).rect(ox + 11.5, oy + 13 + b, 4, 3);
        f.piece(PATCH_PLUM).rect(ox + 10.5, oy + 20 + b, 5, 4);
        for (int y = Fine.p(oy + 13); y < Fine.p(oy + 24); y += 3) f.dot(Fine.p(ox + 9.8), y + fb, Doll.WOOD.light());   // stitching
        f.piece(PATCH_ORANGE).rect(ox + 3.5, oy + 13 + b, 3, 6);                   // arms
        f.piece(SKIN_TAN).rect(ox + 3.5, oy + 19 + b, 3, 2);
        f.piece(PATCH_PLUM).rect(ox + 14.5, oy + 13 + b, 3, 6);
        f.piece(SKIN_TAN).rect(ox + 14.5, oy + 19 + b, 3, 2);
        f.piece(SCARF_GREEN).rect(ox + 7, oy + 11.5 + b, 7, 2);
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(SKIN_TAN).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
        f.piece(HAIR_TEAL).ellipseY(cx + 0.5, top + 3.4, 6.0, 4.0, top - 0.5, top + 3.6)   // spiky teal hair
            .tri(cx - 6, top + 2.5, cx - 6.5, top - 2.5, cx - 3, top + 1)
            .tri(cx - 3, top + 1, cx - 1, top - 4, cx + 1, top + 1)
            .tri(cx + 1, top + 1, cx + 4, top - 3, cx + 5, top + 1.5)
            .tri(cx + 4, top + 2, cx + 7.5, top - 0.5, cx + 6.5, top + 4);
        f.join(HAIR_TEAL).rect(cx - 4, top + 3, 9, 1);
        f.dot(Fine.p(cx - 1), Fine.p(top) - 2, HAIR_TEAL, 3); f.dot(Fine.p(cx + 3), Fine.p(top) - 1, HAIR_TEAL, 3);
        PeopleArt.face(f, cx, top, rgb(200, 140, 40), rgb(250, 200, 90), false);
        int my = Fine.p(top + 6) + 4, mx = Fine.p(cx - 3) + 3;
        f.dot(mx - 1, my, Doll.MOUTH); f.dot(mx + 2, my, Doll.MOUTH); f.dot(mx, my + 1, Doll.MOUTH); f.dot(mx + 1, my + 1, Doll.MOUTH);   // a wide grin
        f.dot(Fine.p(cx - 5.6), Fine.p(top + 8), Doll.GOLD.high());                // the earring
    }
}
