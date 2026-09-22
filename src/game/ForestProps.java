package game;

import static game.PixelCanvas.*;

/** Forest scenery: flowers and stones for the floor, trees and bushes around the walls. */
final class ForestProps {
    private ForestProps() {}

    private static final int OUT = rgb(24, 46, 32);
    private static final int BARK = rgb(112, 76, 50), BARK_D = rgb(76, 50, 36), BARK_L = rgb(152, 108, 70);

    /** Small flat things scattered over the grass. Anchored at the bottom centre. */
    static Sprite[] floor() {
        return new Sprite[]{
            flower(rgb(255, 255, 255), rgb(255, 214, 80)),
            flower(rgb(255, 224, 90), rgb(214, 130, 40)),
            flower(rgb(255, 120, 160), rgb(255, 232, 130)),
            flower(rgb(130, 170, 255), rgb(255, 240, 150)),
            tuft(), rock(), mushroom(), fern(), leaves(), pebbles(),
        };
    }

    /** Trees that stand outside the walls (drawn on the north, east and west sides). Anchored at the trunk's base. */
    static Sprite[] tall() {
        return new Sprite[]{oak(false), pine(), oak(true)};
    }

    /** Low things for the south wall and for filling gaps. */
    static Sprite[] low() {
        return new Sprite[]{bush(), boulder(), stump(), log(), mushroomCluster()};
    }

    private static Sprite finish(PixelCanvas c, int ax, int ay, boolean bevel) {
        if (bevel) c.bevel(0.14, 0.2);
        c.outline(OUT);
        return new Sprite(c, ax, ay);
    }

    private static Sprite flower(int petal, int center) {
        PixelCanvas c = new PixelCanvas(7, 9);
        int g = rgb(70, 150, 62);
        c.rect(3, 4, 1, 5, g); c.set(2, 6, g); c.set(4, 7, g);
        c.rect(2, 1, 3, 3, petal); c.set(3, 0, petal); c.set(3, 4, petal); c.set(1, 2, petal); c.set(5, 2, petal);
        c.set(3, 2, center);
        return new Sprite(c, 3, 8);
    }

    private static Sprite tuft() {
        PixelCanvas c = new PixelCanvas(9, 6);
        int a = rgb(70, 150, 60), b = rgb(112, 190, 84), d = rgb(48, 112, 52);
        for (int x : new int[]{1, 3, 5, 7}) { c.rect(x, 5 - (x % 3) - 2, 1, 3 + x % 3, x % 2 == 0 ? a : b); c.set(x, 5, d); }
        c.set(4, 1, b); c.set(2, 2, b);
        return new Sprite(c, 4, 5);
    }

    private static Sprite rock() {
        PixelCanvas c = new PixelCanvas(11, 8);
        c.ellipse(5.5, 5, 4.6, 2.8, rgb(150, 154, 150)); c.ellipse(4.8, 4.2, 2.4, 1.4, rgb(184, 188, 182));
        c.set(8, 6, rgb(108, 112, 110)); c.set(9, 5, rgb(108, 112, 110));
        return finish(c, 5, 7, false);
    }

    private static Sprite mushroom() {
        PixelCanvas c = new PixelCanvas(9, 9);
        c.rect(3, 5, 2, 3, rgb(246, 232, 200));
        c.ellipseY(4, 4, 4, 3, rgb(220, 60, 64), 0, 5.5);
        c.set(2, 2, WHITE); c.set(5, 1, WHITE); c.set(5, 3, WHITE);
        return finish(c, 4, 8, false);
    }

    private static final int WHITE = rgb(255, 255, 255);

    private static Sprite fern() {
        PixelCanvas c = new PixelCanvas(13, 9);
        int a = rgb(60, 140, 62), b = rgb(96, 182, 84);
        c.line(6, 8, 6, 3, a);
        for (int i = 0; i < 4; i++) { c.line(6, 7 - i, 1 + i, 4 - i / 2, i % 2 == 0 ? a : b); c.line(6, 7 - i, 11 - i, 4 - i / 2, i % 2 == 0 ? b : a); }
        c.line(6, 3, 6, 0, b);
        return new Sprite(c, 6, 8);
    }

    private static Sprite leaves() {
        PixelCanvas c = new PixelCanvas(9, 4);
        c.rect(1, 1, 3, 2, rgb(196, 122, 54)); c.set(0, 2, rgb(150, 84, 44)); c.rect(5, 0, 3, 2, rgb(226, 160, 64)); c.set(8, 1, rgb(160, 96, 44)); c.set(4, 3, rgb(150, 84, 44));
        return new Sprite(c, 4, 3);
    }

    private static Sprite pebbles() {
        PixelCanvas c = new PixelCanvas(9, 4);
        c.rect(1, 1, 2, 2, rgb(176, 176, 170)); c.rect(4, 2, 2, 1, rgb(150, 150, 146)); c.rect(6, 0, 2, 2, rgb(196, 194, 186));
        return new Sprite(c, 4, 3);
    }

    private static Sprite oak(boolean autumn) {
        PixelCanvas c = new PixelCanvas(36, 44);
        int leaf = autumn ? rgb(222, 128, 48) : rgb(60, 146, 68), leafL = autumn ? rgb(250, 186, 80) : rgb(110, 202, 96), leafD = autumn ? rgb(160, 74, 40) : rgb(34, 98, 54);
        c.ellipse(9, 41, 5, 2, BARK_D); c.ellipse(27, 41, 5, 2, BARK_D);
        c.rect(14, 26, 8, 15, BARK); c.rect(14, 26, 2, 15, BARK_L); c.rect(20, 26, 2, 15, BARK_D);
        c.rect(12, 38, 12, 3, BARK); c.rect(17, 30, 1, 6, BARK_D);
        int[][] blobs = {{18, 16, 13}, {9, 22, 8}, {27, 22, 8}, {18, 8, 9}, {8, 13, 7}, {28, 13, 7}};
        for (int[] b : blobs) c.disc(b[0] + 0.5, b[1] + 0.8, b[2], leafD);
        for (int[] b : blobs) c.disc(b[0] + 0.0, b[1] - 0.4, b[2] - 1.4, leaf);
        for (int[] b : blobs) c.disc(b[0] - 1.8, b[1] - 2.4, Math.max(1.6, b[2] - 5), leafL);
        c.disc(24.5, 18.5, 1.3, autumn ? rgb(255, 230, 120) : rgb(230, 66, 76)); c.disc(12.5, 24.5, 1.2, autumn ? rgb(255, 230, 120) : rgb(255, 232, 110));
        return finish(c, 18, 41, true);
    }

    private static Sprite pine() {
        PixelCanvas c = new PixelCanvas(28, 46);
        int a = rgb(38, 110, 64), b = rgb(58, 142, 78), d = rgb(28, 82, 52), l = rgb(96, 186, 104);
        c.rect(12, 38, 4, 6, BARK); c.rect(12, 38, 1, 6, BARK_L);
        int[][] tiers = {{14, 4, 9}, {14, 12, 12}, {14, 20, 14}, {14, 29, 15}};
        for (int i = 0; i < tiers.length; i++) {
            int cx = tiers[i][0], top = tiers[i][1], half = tiers[i][2];
            c.tri(cx, top, cx - half, top + 12, cx + half, top + 12, i % 2 == 0 ? a : b);
            c.tri(cx, top, cx - half, top + 12, cx, top + 12, d);
            c.tri(cx, top + 1, cx - half / 2, top + 6, cx, top + 6, l);
        }
        return finish(c, 14, 43, false);
    }

    private static Sprite bush() {
        PixelCanvas c = new PixelCanvas(18, 12);
        int a = rgb(52, 128, 62), b = rgb(78, 168, 80), d = rgb(34, 92, 48);
        c.disc(5.5, 7, 4.5, d); c.disc(12.5, 7, 4.5, d); c.disc(9, 5, 5.5, d);
        c.disc(5.5, 6.4, 3.6, a); c.disc(12.5, 6.4, 3.6, a); c.disc(9, 4.4, 4.6, a);
        c.disc(7.5, 3.2, 2.0, b); c.disc(4.5, 5.2, 1.4, b);
        c.set(12, 5, rgb(230, 66, 76)); c.set(6, 7, rgb(230, 66, 76)); c.set(10, 8, rgb(230, 66, 76));
        return finish(c, 9, 11, false);
    }

    private static Sprite boulder() {
        PixelCanvas c = new PixelCanvas(18, 13);
        c.ellipse(9, 8, 8, 5, rgb(138, 142, 140)); c.ellipse(7.5, 6.5, 5, 3, rgb(174, 178, 174)); c.ellipse(6.5, 5.6, 2.4, 1.4, rgb(206, 210, 204));
        c.ellipse(12.5, 9.5, 4, 2.5, rgb(112, 116, 114));
        c.disc(13.5, 4.5, 2.0, rgb(84, 150, 70)); c.disc(14.5, 3.8, 1.2, rgb(126, 200, 100));       // moss
        return finish(c, 9, 12, false);
    }

    private static Sprite stump() {
        PixelCanvas c = new PixelCanvas(14, 11);
        c.rect(2, 4, 10, 6, BARK); c.rect(2, 4, 2, 6, BARK_L); c.rect(10, 4, 2, 6, BARK_D);
        c.ellipse(7, 4, 5.5, 2.6, rgb(212, 166, 106)); c.ellipse(7, 4, 3.4, 1.5, rgb(182, 134, 82)); c.ellipse(7, 4, 1.6, 0.7, rgb(212, 166, 106));
        c.set(1, 9, BARK_D); c.set(12, 9, BARK_D);
        return finish(c, 7, 10, false);
    }

    private static Sprite log() {
        PixelCanvas c = new PixelCanvas(22, 10);
        c.rect(3, 2, 17, 6, BARK); c.rect(3, 2, 17, 1, BARK_L); c.rect(3, 7, 17, 1, BARK_D);
        for (int x = 6; x < 19; x += 4) c.rect(x, 3, 1, 3, BARK_D);
        c.ellipse(3, 5, 2.4, 3.2, rgb(212, 166, 106)); c.ellipse(3, 5, 1.2, 1.8, rgb(182, 134, 82));
        c.disc(15, 2, 1.6, rgb(84, 150, 70));
        return finish(c, 11, 9, false);
    }

    private static Sprite mushroomCluster() {
        PixelCanvas c = new PixelCanvas(15, 11);
        int stem = rgb(246, 232, 200), red = rgb(220, 60, 64), tan = rgb(214, 150, 80);
        c.rect(3, 6, 2, 4, stem); c.ellipseY(4, 5.5, 3.6, 3.0, red, 0, 7); c.set(3, 3, rgb(255, 255, 255)); c.set(5, 4, rgb(255, 255, 255));
        c.rect(10, 5, 2, 5, stem); c.ellipseY(11, 5, 3.6, 3.0, tan, 0, 6.5); c.set(10, 3, rgb(255, 240, 210));
        c.rect(7, 8, 1, 2, stem); c.ellipseY(7.5, 8, 2.4, 2.0, red, 5, 9);
        return finish(c, 7, 10, false);
    }
}
