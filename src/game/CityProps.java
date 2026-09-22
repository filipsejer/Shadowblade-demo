package game;

import static game.PixelCanvas.*;

/** City scenery: road markings and litter for the ground, lamp posts and signs around the walls, windows on them. */
final class CityProps {
    private CityProps() {}

    private static final int OUT = rgb(22, 24, 38);
    private static final int WHITE = rgb(255, 255, 255);
    private static final int METAL = rgb(118, 126, 146), METAL_L = rgb(176, 184, 204), METAL_D = rgb(72, 78, 98);

    static Sprite[] floor() {
        return new Sprite[]{manhole(), puddle(), paper(), cone(), crosswalk(), arrow(), crackDecal(), bottle()};
    }

    static Sprite[] tall() {
        return new Sprite[]{lamp(), neon(rgb(255, 90, 180), rgb(120, 240, 255)), chimney(), lamp(), neon(rgb(120, 240, 255), rgb(255, 90, 180))};
    }

    static Sprite[] low() {
        return new Sprite[]{crate(), trashCan(), barrel(), bench(), hydrant(), planter()};
    }

    /** Things stuck onto the brick walls: lit and dark windows, an awning. */
    static Sprite[] facade() {
        return new Sprite[]{window(true), window(false), window(true), window(false), awning()};
    }

    private static Sprite fin(PixelCanvas c, int ax, int ay, boolean bevel) {
        if (bevel) c.bevel(0.14, 0.2);
        c.outline(OUT);
        return new Sprite(c, ax, ay);
    }

    private static Sprite manhole() {
        PixelCanvas c = new PixelCanvas(13, 7);
        c.ellipse(6.5, 3.5, 6, 3, rgb(46, 50, 62)); c.ellipse(6.5, 3.2, 5, 2.4, rgb(84, 90, 108));
        c.line(3, 3, 10, 3, rgb(52, 56, 70)); c.line(4, 2, 9, 2, rgb(52, 56, 70)); c.line(4, 4, 9, 4, rgb(52, 56, 70));
        return new Sprite(c, 6, 4);
    }

    private static Sprite puddle() {
        PixelCanvas c = new PixelCanvas(15, 7);
        c.ellipse(7.5, 3.5, 7, 3, rgba(90, 140, 200, 150)); c.ellipse(7.5, 3.2, 5.6, 2.2, rgba(130, 180, 230, 150));
        c.set(4, 2, rgba(255, 255, 255, 200)); c.set(5, 2, rgba(255, 255, 255, 200)); c.set(9, 4, rgba(255, 255, 255, 160));
        return new Sprite(c, 7, 4);
    }

    private static Sprite paper() {
        PixelCanvas c = new PixelCanvas(8, 5);
        c.rect(1, 1, 5, 3, rgb(240, 238, 226)); c.set(6, 2, rgb(200, 198, 186)); c.rect(2, 2, 3, 1, rgb(170, 170, 196)); c.set(1, 3, rgb(196, 194, 182));
        return new Sprite(c, 3, 4);
    }

    private static Sprite cone() {
        PixelCanvas c = new PixelCanvas(8, 10);
        c.rect(0, 8, 8, 2, rgb(240, 120, 40));
        c.tri(4, 0, 1, 8, 7, 8, rgb(255, 136, 48));
        c.rect(2, 3, 4, 1, WHITE); c.rect(1, 6, 6, 1, WHITE);
        return fin(c, 4, 9, false);
    }

    private static Sprite crosswalk() {
        PixelCanvas c = new PixelCanvas(16, 10);
        for (int x = 0; x < 16; x += 4) c.rect(x, 0, 3, 10, rgba(232, 232, 240, 220));
        return new Sprite(c, 8, 9);
    }

    private static Sprite arrow() {
        PixelCanvas c = new PixelCanvas(9, 12);
        int w = rgba(232, 232, 240, 210);
        c.rect(3, 5, 3, 7, w); c.tri(4, 0, 0, 6, 8, 6, w);
        return new Sprite(c, 4, 11);
    }

    private static Sprite crackDecal() {
        PixelCanvas c = new PixelCanvas(14, 6);
        int d = rgb(30, 32, 42);
        c.line(0, 3, 4, 2, d); c.line(4, 2, 8, 4, d); c.line(8, 4, 13, 3, d); c.line(6, 3, 7, 0, d); c.line(8, 4, 10, 5, d);
        return new Sprite(c, 7, 5);
    }

    private static Sprite bottle() {
        PixelCanvas c = new PixelCanvas(6, 6);
        c.rect(1, 2, 4, 3, rgb(80, 180, 110)); c.rect(2, 1, 2, 1, rgb(80, 180, 110)); c.set(2, 3, rgb(180, 240, 200));
        return fin(c, 3, 5, false);
    }

    private static Sprite lamp() {
        PixelCanvas c = new PixelCanvas(13, 46);
        c.rect(5, 6, 3, 38, METAL); c.rect(5, 6, 1, 38, METAL_L); c.rect(7, 6, 1, 38, METAL_D);
        c.rect(3, 42, 7, 3, METAL_D); c.rect(4, 40, 5, 2, METAL);
        c.rect(1, 2, 11, 5, METAL_D);
        c.rect(2, 3, 9, 3, rgb(255, 232, 150)); c.rect(3, 3, 7, 1, rgb(255, 252, 214));
        c.rect(3, 0, 7, 2, METAL);
        return fin(c, 6, 44, false);
    }

    private static Sprite neon(int main, int accent) {
        PixelCanvas c = new PixelCanvas(16, 36);
        c.rect(7, 28, 2, 8, METAL_D);
        c.rect(1, 2, 14, 26, rgb(30, 30, 52));
        c.rect(2, 3, 12, 24, rgb(16, 16, 32));
        for (int y = 5; y < 25; y += 5) { c.rect(4, y, 8, 2, main); c.rect(5, y, 6, 1, lighten(main, 0.5)); }
        c.rect(4, 24, 8, 1, accent);
        c.rect(1, 2, 14, 1, rgb(60, 60, 96));
        return fin(c, 8, 35, false);
    }

    private static Sprite chimney() {
        PixelCanvas c = new PixelCanvas(14, 44);
        c.rect(3, 12, 8, 30, rgb(150, 84, 70)); c.rect(3, 12, 2, 30, rgb(186, 116, 96)); c.rect(9, 12, 2, 30, rgb(112, 62, 56));
        for (int y = 15; y < 42; y += 4) c.rect(3, y, 8, 1, rgb(110, 60, 54));
        c.rect(1, 9, 12, 4, rgb(96, 100, 120)); c.rect(1, 9, 12, 1, METAL_L);
        c.disc(6.5, 5, 2.6, rgba(210, 210, 224, 220)); c.disc(9.5, 2.4, 2.0, rgba(190, 190, 206, 170));
        return fin(c, 7, 41, false);
    }

    private static Sprite crate() {
        PixelCanvas c = new PixelCanvas(16, 14);
        int w = rgb(176, 124, 72), wl = rgb(214, 160, 96), wd = rgb(120, 80, 48);
        c.rect(1, 2, 14, 11, w); c.rect(1, 2, 14, 2, wl); c.rect(1, 11, 14, 2, wd);
        c.rect(1, 2, 2, 11, wd); c.rect(13, 2, 2, 11, wd);
        c.line(3, 4, 12, 11, wd); c.line(12, 4, 3, 11, wd);
        return fin(c, 8, 13, false);
    }

    private static Sprite trashCan() {
        PixelCanvas c = new PixelCanvas(12, 14);
        c.rect(2, 3, 8, 10, METAL); c.rect(2, 3, 2, 10, METAL_L); c.rect(8, 3, 2, 10, METAL_D);
        for (int x = 4; x < 9; x += 2) c.rect(x, 5, 1, 7, METAL_D);
        c.rect(1, 1, 10, 3, METAL_L); c.rect(1, 3, 10, 1, METAL_D); c.rect(5, 0, 2, 1, METAL_D);
        return fin(c, 6, 13, false);
    }

    private static Sprite barrel() {
        PixelCanvas c = new PixelCanvas(12, 15);
        int b = rgb(64, 120, 180), bl = rgb(110, 170, 224), bd = rgb(36, 76, 130);
        c.ellipse(6, 7.5, 5.4, 6.5, b); c.rect(0, 4, 12, 1, bd); c.rect(0, 10, 12, 1, bd);
        c.rect(2, 3, 2, 9, bl);
        c.ellipse(6, 2.5, 4.4, 1.6, bl);
        return fin(c, 6, 14, false);
    }

    private static Sprite bench() {
        PixelCanvas c = new PixelCanvas(22, 12);
        int w = rgb(170, 116, 68), wl = rgb(208, 152, 92), wd = rgb(116, 76, 44);
        c.rect(1, 2, 20, 2, wl); c.rect(1, 4, 20, 2, w); c.rect(1, 0, 20, 2, w);
        c.rect(2, 6, 2, 5, METAL_D); c.rect(18, 6, 2, 5, METAL_D);
        c.rect(1, 6, 20, 1, wd);
        return fin(c, 11, 11, false);
    }

    private static Sprite hydrant() {
        PixelCanvas c = new PixelCanvas(9, 13);
        int r = rgb(226, 62, 60), rl = rgb(255, 130, 116), rd = rgb(150, 32, 44);
        c.rect(2, 3, 5, 9, r); c.rect(2, 3, 1, 9, rl); c.rect(6, 3, 1, 9, rd);
        c.rect(1, 2, 7, 2, rd); c.rect(3, 0, 3, 3, r); c.rect(0, 6, 2, 3, rd); c.rect(7, 6, 2, 3, rd);
        c.rect(1, 11, 7, 2, rd);
        return fin(c, 4, 12, false);
    }

    private static Sprite planter() {
        PixelCanvas c = new PixelCanvas(16, 15);
        c.rect(2, 8, 12, 6, rgb(170, 100, 70)); c.rect(2, 8, 12, 1, rgb(214, 140, 104)); c.rect(2, 13, 12, 1, rgb(110, 64, 50));
        c.disc(5.5, 5.5, 3.2, rgb(70, 160, 76)); c.disc(10.5, 4.5, 3.6, rgb(90, 190, 90)); c.disc(8, 3, 2.6, rgb(120, 214, 108));
        c.set(9, 4, rgb(255, 120, 170)); c.set(6, 5, rgb(255, 232, 110));
        return fin(c, 8, 14, false);
    }

    private static Sprite window(boolean lit) {
        PixelCanvas c = new PixelCanvas(11, 13);
        c.rect(0, 0, 11, 13, rgb(70, 52, 58));
        int glass = lit ? rgb(255, 214, 116) : rgb(34, 44, 74);
        c.rect(1, 1, 9, 11, glass);
        if (lit) { c.rect(1, 1, 9, 2, rgb(255, 240, 170)); c.rect(3, 8, 5, 4, rgb(214, 150, 70)); }
        else { c.rect(2, 2, 2, 3, rgb(60, 76, 118)); c.rect(1, 1, 9, 1, rgb(48, 62, 100)); }
        c.rect(5, 1, 1, 11, rgb(70, 52, 58)); c.rect(1, 6, 9, 1, rgb(70, 52, 58));
        c.rect(0, 12, 11, 1, rgb(200, 190, 180));
        return new Sprite(c, 5, 12);
    }

    private static Sprite awning() {
        PixelCanvas c = new PixelCanvas(18, 9);
        for (int x = 0; x < 18; x++) {
            int col = (x / 3) % 2 == 0 ? rgb(224, 64, 72) : rgb(250, 240, 230);
            c.rect(x, 0, 1, 6, col);
            c.set(x, 6, ((x / 3) % 2 == 0) ? rgb(170, 40, 56) : rgb(210, 200, 190));
        }
        c.rect(0, 0, 18, 1, rgb(255, 140, 130));
        return fin(c, 9, 8, false);
    }
}
