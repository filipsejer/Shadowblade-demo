package game;

import static game.PixelCanvas.*;

/** Laboratory scenery: cables, goo and glass on the floor; tanks, tesla coils and server racks around the walls; monitors on them. */
final class LabProps {
    private LabProps() {}

    private static final int OUT = rgb(14, 24, 30);
    private static final int WHITE = rgb(255, 255, 255);
    private static final int STEEL = rgb(122, 140, 152), STEEL_L = rgb(190, 208, 218), STEEL_D = rgb(64, 78, 90);
    private static final int GREEN = rgb(74, 230, 140), GREEN_D = rgb(34, 150, 96), GREEN_L = rgb(170, 255, 200);

    static Sprite[] floor() {
        return new Sprite[]{cable(), goo(), paper(), glass(), hazard(), scorch(), drain(), spill()};
    }

    /** Glow colours are set in {@link LevelView} by the index used here: 0 green tank, 1 tesla coil, 2 server rack, 3 pink tank, 4 lamp. */
    static Sprite[] tall() {
        return new Sprite[]{tank(GREEN, GREEN_D, GREEN_L), tesla(), server(), tank(rgb(255, 110, 190), rgb(170, 50, 120), rgb(255, 200, 230)), lamp()};
    }

    static Sprite[] low() {
        return new Sprite[]{crate(), barrel(), bench(), chair(), cabinet(), cage()};
    }

    /** Things bolted to the walls: screens, a warning sign, a vent, a porthole, a gauge. */
    static Sprite[] facade() {
        return new Sprite[]{monitor(true), sign(), vent(), porthole(), monitor(false), gauge()};
    }

    private static Sprite fin(PixelCanvas c, int ax, int ay, boolean bevel) {
        if (bevel) c.bevel(0.14, 0.2);
        c.outline(OUT);
        return new Sprite(c, ax, ay);
    }

    // ------------------------------------------------------------------ on the floor

    private static Sprite cable() {
        PixelCanvas c = new PixelCanvas(21, 7);
        int k = rgb(26, 30, 38);
        c.thickLine(1, 5, 6, 2, 2, k); c.thickLine(6, 2, 12, 4, 2, k); c.thickLine(12, 4, 19, 2, 2, k);
        c.set(4, 2, rgb(90, 100, 118)); c.set(9, 2, rgb(90, 100, 118)); c.set(15, 3, rgb(90, 100, 118));
        c.rect(0, 4, 2, 3, rgb(200, 60, 60)); c.rect(19, 1, 2, 3, rgb(200, 60, 60));
        return new Sprite(c, 10, 5);
    }

    private static Sprite goo() {
        PixelCanvas c = new PixelCanvas(15, 8);
        c.ellipse(7.5, 4, 7, 3.4, rgba(70, 200, 90, 170)); c.ellipse(7.5, 3.6, 5.6, 2.4, rgba(120, 240, 130, 170));
        c.disc(3, 6.5, 1.2, rgba(70, 200, 90, 170)); c.disc(12.5, 1.5, 1.0, rgba(70, 200, 90, 170));
        c.set(5, 3, rgba(230, 255, 230, 220)); c.set(6, 3, rgba(230, 255, 230, 220)); c.set(10, 4, rgba(230, 255, 230, 180));
        return new Sprite(c, 7, 5);
    }

    private static Sprite paper() {
        PixelCanvas c = new PixelCanvas(9, 6);
        c.rect(1, 1, 6, 4, rgb(244, 244, 236)); c.set(7, 2, rgb(206, 206, 196));
        c.rect(2, 2, 4, 1, rgb(60, 150, 100)); c.rect(2, 4, 3, 1, rgb(170, 60, 60)); c.set(5, 3, rgb(110, 110, 150));
        return new Sprite(c, 4, 5);
    }

    private static Sprite glass() {
        PixelCanvas c = new PixelCanvas(13, 7);
        int g = rgba(190, 240, 250, 190), h = rgba(255, 255, 255, 230);
        c.tri(1, 5, 4, 1, 6, 5, g); c.tri(7, 6, 9, 2, 12, 5, g); c.tri(5, 6, 6, 4, 8, 6, g);
        c.set(4, 2, h); c.set(9, 3, h); c.set(6, 5, h);
        return new Sprite(c, 6, 5);
    }

    private static Sprite hazard() {
        PixelCanvas c = new PixelCanvas(16, 6);
        for (int i = 0; i < 16; i++) for (int j = 0; j < 6; j++) c.set(i, j, ((i + j) / 3) % 2 == 0 ? rgba(232, 194, 54, 230) : rgba(30, 32, 36, 230));
        return new Sprite(c, 8, 5);
    }

    private static Sprite scorch() {
        PixelCanvas c = new PixelCanvas(15, 9);
        int d = rgba(16, 18, 22, 160);
        c.ellipse(7.5, 4.5, 7, 4, d); c.ellipse(7.5, 4.5, 4, 2.4, rgba(6, 8, 10, 190));
        c.line(0, 4, 3, 4, d); c.line(11, 4, 14, 3, d); c.line(7, 0, 7, 2, d);
        return new Sprite(c, 7, 6);
    }

    private static Sprite drain() {
        PixelCanvas c = new PixelCanvas(13, 8);
        c.ellipse(6.5, 4, 6, 3.3, rgb(44, 54, 62)); c.ellipse(6.5, 3.7, 5, 2.6, rgb(90, 108, 118));
        for (int x = 3; x <= 9; x += 2) c.rect(x, 2, 1, 4, rgb(30, 38, 44));
        return new Sprite(c, 6, 5);
    }

    private static Sprite spill() {
        PixelCanvas c = new PixelCanvas(15, 8);
        c.ellipse(7.5, 4, 7, 3.4, rgba(80, 130, 240, 170)); c.ellipse(7.5, 3.6, 5.4, 2.3, rgba(140, 180, 255, 170));
        c.set(4, 3, rgba(255, 255, 255, 220)); c.set(5, 3, rgba(255, 255, 255, 220));
        c.disc(12, 6, 0.9, rgba(80, 130, 240, 170));
        return new Sprite(c, 7, 5);
    }

    // ------------------------------------------------------------------ tall things

    /** A big glass tank of glowing liquid, with bubbles, on a steel base. */
    private static Sprite tank(int liquid, int liquidD, int liquidL) {
        PixelCanvas c = new PixelCanvas(15, 32);
        c.rect(1, 27, 13, 4, STEEL_D); c.rect(1, 27, 13, 1, STEEL);
        c.rect(0, 30, 15, 2, STEEL);
        c.rect(2, 3, 11, 24, rgb(170, 210, 222));                                            // glass
        c.rect(3, 4, 9, 23, rgb(44, 66, 78));
        c.rect(3, 9, 9, 18, liquid); c.rect(3, 9, 9, 1, liquidL); c.rect(10, 10, 2, 17, liquidD);
        int[][] bubbles = {{5, 22}, {8, 17}, {6, 13}, {9, 24}, {5, 11}};
        for (int[] b : bubbles) { c.set(b[0], b[1], liquidL); c.set(b[0] + 1, b[1], lighten(liquidL, 0.3)); }
        c.set(3, 5, WHITE); c.rect(3, 6, 1, 4, rgb(220, 240, 248));                          // reflection on the glass
        c.rect(1, 1, 13, 3, STEEL); c.rect(1, 1, 13, 1, STEEL_L);                             // cap
        c.rect(6, 0, 3, 2, STEEL_D);
        return fin(c, 7, 31, false);
    }

    /** A tesla coil: stacked rings under a bright sphere with a spark. */
    private static Sprite tesla() {
        PixelCanvas c = new PixelCanvas(17, 34);
        c.rect(3, 29, 11, 4, STEEL_D); c.rect(3, 29, 11, 1, STEEL); c.rect(1, 32, 15, 2, STEEL);
        for (int i = 0; i < 5; i++) {
            int y = 12 + i * 3, w = 3 + i;
            c.rect(8 - w / 2 - 1, y, w + 2, 2, rgb(200, 132, 70)); c.rect(8 - w / 2 - 1, y, w + 2, 1, rgb(244, 190, 120));
            c.rect(8 - w / 2 - 1, y + 2, w + 2, 1, rgb(110, 68, 40));
        }
        c.rect(7, 26, 3, 4, STEEL_D);
        c.rect(7, 9, 3, 4, STEEL);
        c.disc(8.5, 7.5, 4.2, rgb(150, 110, 240)); c.disc(8.5, 7.5, 3.0, rgb(200, 180, 255)); c.disc(8, 7, 1.4, WHITE);
        c.line(12, 6, 15, 3, rgb(230, 220, 255)); c.line(15, 3, 14, 1, rgb(230, 220, 255)); c.line(5, 5, 2, 2, rgb(230, 220, 255)); c.line(2, 2, 3, 0, rgb(230, 220, 255));
        return fin(c, 8, 33, false);
    }

    private static Sprite server() {
        PixelCanvas c = new PixelCanvas(15, 28);
        c.rect(0, 0, 15, 28, STEEL_D); c.rect(1, 1, 13, 26, rgb(40, 50, 60));
        c.rect(0, 0, 15, 1, STEEL); c.rect(0, 27, 15, 1, rgb(30, 38, 46));
        int[] led = {rgb(74, 230, 140), rgb(255, 90, 80), rgb(90, 170, 255), rgb(255, 210, 90)};
        for (int row = 0; row < 6; row++) {
            int y = 2 + row * 4;
            c.rect(2, y, 11, 3, rgb(58, 72, 84)); c.rect(2, y, 11, 1, rgb(84, 102, 116));
            c.set(3, y + 1, led[(row * 3) % 4]); c.set(5, y + 1, led[(row * 5 + 1) % 4]);
            c.rect(8, y + 1, 4, 1, rgb(30, 38, 46));
        }
        return fin(c, 7, 27, false);
    }

    /** A fluorescent standing lamp with a long cyan-white tube. */
    private static Sprite lamp() {
        PixelCanvas c = new PixelCanvas(9, 34);
        c.rect(1, 31, 7, 3, STEEL_D); c.rect(1, 31, 7, 1, STEEL);
        c.rect(4, 8, 1, 23, STEEL);
        c.rect(2, 0, 5, 9, rgb(190, 252, 255)); c.rect(3, 0, 3, 9, WHITE);
        c.rect(1, 0, 7, 1, STEEL); c.rect(1, 8, 7, 1, STEEL);
        return fin(c, 4, 33, false);
    }

    // ------------------------------------------------------------------ low things

    private static Sprite crate() {
        PixelCanvas c = new PixelCanvas(15, 14);
        int w = rgb(184, 152, 100), wl = rgb(220, 190, 132), wd = rgb(120, 92, 60);
        c.rect(1, 1, 13, 12, w); c.rect(1, 1, 13, 1, wl); c.rect(1, 12, 13, 1, wd);
        c.line(1, 1, 13, 12, wd); c.line(13, 1, 1, 12, wd);
        c.rect(1, 1, 1, 12, wd); c.rect(13, 1, 1, 12, wd);
        c.rect(5, 5, 5, 3, rgb(232, 194, 54)); c.set(7, 6, rgb(30, 32, 36));                    // hazard label
        return fin(c, 7, 13, false);
    }

    /** A drum of toxic waste. */
    private static Sprite barrel() {
        PixelCanvas c = new PixelCanvas(11, 15);
        int b = rgb(84, 168, 84), bl = rgb(140, 224, 120), bd = rgb(44, 106, 60);
        c.rect(1, 1, 9, 13, b); c.rect(1, 1, 2, 13, bl); c.rect(8, 1, 2, 13, bd);
        c.rect(0, 3, 11, 1, bd); c.rect(0, 11, 11, 1, bd); c.rect(1, 0, 9, 2, rgb(150, 160, 150));
        c.disc(5.5, 7.5, 2.2, rgb(240, 220, 60)); c.set(5, 7, rgb(30, 32, 36)); c.set(6, 7, rgb(30, 32, 36)); c.set(5, 8, rgb(30, 32, 36));   // radiation-ish symbol
        c.set(3, 13, GREEN); c.set(2, 14, GREEN);                                               // a drip
        return fin(c, 5, 14, false);
    }

    /** A lab bench with flasks of colourful liquid. */
    private static Sprite bench() {
        PixelCanvas c = new PixelCanvas(21, 15);
        c.rect(0, 7, 21, 3, rgb(190, 204, 210)); c.rect(0, 7, 21, 1, WHITE); c.rect(0, 9, 21, 1, rgb(120, 136, 146));
        c.rect(1, 10, 3, 5, STEEL_D); c.rect(17, 10, 3, 5, STEEL_D); c.rect(1, 11, 19, 1, STEEL);
        c.ellipse(5.5, 4.5, 2.6, 2.6, rgb(255, 120, 190)); c.rect(4, 0, 3, 3, rgb(210, 236, 244)); c.rect(3, 4, 5, 3, rgb(255, 120, 190));
        c.rect(10, 1, 3, 6, rgb(210, 236, 244)); c.rect(10, 3, 3, 4, GREEN);
        c.ellipse(16.5, 4.5, 2.6, 2.6, rgb(120, 180, 255)); c.rect(15, 0, 3, 3, rgb(210, 236, 244)); c.rect(14, 4, 5, 3, rgb(120, 180, 255));
        c.set(5, 5, WHITE); c.set(16, 5, WHITE); c.set(11, 3, GREEN_L);
        return fin(c, 10, 14, false);
    }

    private static Sprite chair() {
        PixelCanvas c = new PixelCanvas(11, 15);
        c.rect(1, 0, 9, 7, rgb(70, 130, 170)); c.rect(1, 0, 9, 1, rgb(120, 186, 224));
        c.rect(0, 7, 11, 3, rgb(50, 100, 140));
        c.rect(5, 10, 1, 3, STEEL); c.rect(2, 13, 7, 1, STEEL_D); c.set(2, 14, STEEL_D); c.set(8, 14, STEEL_D); c.set(5, 14, STEEL_D);
        return fin(c, 5, 14, false);
    }

    private static Sprite cabinet() {
        PixelCanvas c = new PixelCanvas(13, 17);
        c.rect(0, 0, 13, 17, STEEL); c.rect(0, 0, 13, 1, STEEL_L); c.rect(0, 16, 13, 1, STEEL_D);
        c.rect(6, 1, 1, 15, STEEL_D);
        c.rect(2, 3, 3, 4, rgb(190, 232, 244)); c.rect(8, 3, 3, 4, rgb(190, 232, 244));
        c.rect(2, 9, 3, 6, STEEL_D); c.rect(8, 9, 3, 6, STEEL_D); c.set(5, 11, WHITE); c.set(7, 11, WHITE);
        c.set(3, 5, rgb(255, 120, 190)); c.set(9, 5, GREEN);
        return fin(c, 6, 16, false);
    }

    /** An empty animal cage with the door hanging open. */
    private static Sprite cage() {
        PixelCanvas c = new PixelCanvas(15, 13);
        c.rect(0, 10, 15, 3, STEEL_D); c.rect(0, 10, 15, 1, STEEL);
        for (int x = 1; x < 14; x += 3) c.rect(x, 1, 1, 9, STEEL_L);
        c.rect(0, 0, 15, 2, STEEL); c.rect(0, 0, 15, 1, STEEL_L);
        c.rect(11, 2, 3, 8, rgb(24, 34, 40));
        c.set(6, 8, rgb(228, 200, 160)); c.set(7, 8, rgb(228, 200, 160));                          // a bit of straw
        return fin(c, 7, 12, false);
    }

    // ------------------------------------------------------------------ on the walls

    private static Sprite monitor(boolean lit) {
        PixelCanvas c = new PixelCanvas(13, 11);
        c.rect(0, 0, 13, 11, STEEL_D); c.rect(0, 0, 13, 1, STEEL);
        c.rect(1, 1, 11, 8, lit ? rgb(20, 60, 44) : rgb(18, 26, 32));
        if (lit) {
            c.line(2, 6, 4, 4, GREEN); c.line(4, 4, 6, 7, GREEN); c.line(6, 7, 8, 3, GREEN); c.line(8, 3, 10, 5, GREEN);   // a heartbeat trace
            c.rect(2, 2, 3, 1, GREEN_D); c.set(11, 2, rgb(255, 90, 80));
        } else {
            c.line(2, 2, 10, 8, rgb(34, 48, 56)); c.set(3, 2, rgb(48, 64, 74));
        }
        c.rect(4, 10, 5, 1, STEEL);
        return new Sprite(c, 6, 10);
    }

    private static Sprite sign() {
        PixelCanvas c = new PixelCanvas(11, 12);
        c.rect(0, 0, 11, 12, rgb(232, 194, 54)); c.rect(0, 0, 11, 1, rgb(255, 232, 130));
        c.rect(1, 1, 9, 10, rgb(30, 32, 36));
        c.tri(5, 2, 1, 9, 9, 9, rgb(232, 194, 54)); c.rect(5, 4, 1, 3, rgb(30, 32, 36)); c.set(5, 8, rgb(30, 32, 36));
        return new Sprite(c, 5, 11);
    }

    private static Sprite vent() {
        PixelCanvas c = new PixelCanvas(13, 9);
        c.rect(0, 0, 13, 9, STEEL); c.rect(0, 0, 13, 1, STEEL_L);
        for (int y = 2; y < 8; y += 2) c.rect(2, y, 9, 1, rgb(26, 34, 40));
        for (int[] p : new int[][]{{1, 1}, {11, 1}, {1, 7}, {11, 7}}) c.set(p[0], p[1], STEEL_D);
        return new Sprite(c, 6, 8);
    }

    /** A round window onto a bubbling green tank. */
    private static Sprite porthole() {
        PixelCanvas c = new PixelCanvas(13, 13);
        c.disc(6.5, 6.5, 6.4, STEEL); c.disc(6.5, 6.5, 5.0, rgb(30, 90, 64)); c.disc(6.5, 7.5, 4.0, GREEN_D); c.disc(6.5, 6.5, 2.0, GREEN);
        c.set(4, 4, WHITE); c.set(8, 8, GREEN_L); c.set(5, 9, GREEN_L);
        for (int[] p : new int[][]{{0, 6}, {12, 6}, {6, 0}, {6, 12}}) c.set(p[0], p[1], STEEL_D);
        return new Sprite(c, 6, 12);
    }

    private static Sprite gauge() {
        PixelCanvas c = new PixelCanvas(11, 11);
        c.disc(5.5, 5.5, 5.2, STEEL_D); c.disc(5.5, 5.5, 4.2, rgb(236, 240, 236));
        c.line(5, 5, 8, 2, rgb(220, 50, 50)); c.set(5, 5, rgb(30, 32, 36));
        c.set(2, 5, rgb(60, 60, 70)); c.set(5, 2, rgb(60, 60, 70)); c.set(8, 5, rgb(60, 60, 70));
        c.rect(3, 8, 5, 1, rgb(74, 230, 140));
        return new Sprite(c, 5, 10);
    }
}
