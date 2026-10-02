package game;

import static game.PixelCanvas.*;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * The hero, the hermit and the two shopkeepers (Ranger Ash and Bramble), painted in code. All of them are small
 * round-headed chibis, painted on the drawn sprites' finer grid and shaded like them by a {@link Doll}.
 */
final class PeopleArt {
    private PeopleArt() {}

    static void register(Map<String, Sprite[]> m) {
        // the hero faces down / up / right (left is the right one mirrored); painted on the finer grid (see heroFrames)
        int ax = Fine.p(10), ay = Fine.p(22);
        // (the sword isn't part of these: each frame says where its grip is, see grip(), and SwordArt draws it)
        m.put("hero.down.idle", poses(2, i -> heroFront(-1, i, -1), ax, ay));
        m.put("hero.down.walk", poses(4, i -> heroFront(i, i % 2 == 1 ? 1 : 0, -1), ax, ay));
        m.put("hero.down.attack", poses(2, i -> heroFront(-1, 0, i), ax, ay));
        m.put("hero.up.idle", poses(2, i -> heroBack(-1, i, -1), ax, ay));
        m.put("hero.up.walk", poses(4, i -> heroBack(i, i % 2 == 1 ? 1 : 0, -1), ax, ay));
        m.put("hero.up.attack", poses(2, i -> heroBack(-1, 0, i), ax, ay));
        m.put("hero.side.idle", poses(2, i -> heroSide(-1, i, -1), ax, ay));
        m.put("hero.side.walk", poses(4, i -> heroSide(i, i % 2 == 1 ? 1 : 0, -1), ax, ay));
        m.put("hero.side.attack", poses(2, i -> heroSide(-1, 0, i), ax, ay));
        m.put("hero.roll", heroFrames(4, PeopleArt::heroRoll, Fine.p(8), Fine.p(8)));

        m.put("guide.idle", fineFrames(2, PeopleArt::guide, Fine.p(11), Fine.p(25)));
        m.put("shop.combat", fineFrames(2, i -> shop(0, i), Fine.p(17), Fine.p(45)));
        m.put("shop.survival", fineFrames(2, i -> shop(2, i), Fine.p(17), Fine.p(45)));
    }

    /** Frames painted at the ordinary painted-art size (used by the creature and breakable art too). */
    static Sprite[] frames(int n, IntFunction<PixelCanvas> paint, int ax, int ay) {
        Sprite[] out = new Sprite[n];
        for (int i = 0; i < n; i++) out[i] = new Sprite(paint.apply(i), ax, ay);
        return out;
    }

    /** Which of the hero's three views ("side", "down", "up") to show for a facing angle. */
    static String heroDir(double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        if (Math.abs(c) >= Math.abs(s)) return "side";
        return s > 0 ? "down" : "up";
    }

    /** The side view faces right; it's mirrored when the hero faces left. */
    static boolean heroFlip(double angle) { return Math.cos(angle) < 0; }


    // ------------------------------------------------------------------ the hero

    /*
     * The hero is painted 1.5x finer than the old painted art, so their pixels match the drawn monsters'
     * (ImportedArt.PIXEL world units each), and shaded like them by a Doll. The shapes are laid out on the old 21x25
     * grid and scaled by Fine; the small details (eyes, strands of hair, the buckle) are placed pixel by pixel.
     */
    private static final double F = 1.5;
    private static final double HERO_K = 1 / F;                 // the size of one of its pixels, relative to the painted art's
    private static final int IRIS = rgb(64, 96, 200), IRIS_L = rgb(120, 170, 255);
    private static final Doll.Mat SKIN_M = Doll.SKIN, HAIR_M = Doll.HAIR_BROWN, TUNIC_M = Doll.TUNIC_BLUE, PANTS_M = Doll.PANTS,
        BOOT_M = Doll.LEATHER, GOLD_M = Doll.GOLD, STEEL_M = Doll.STEEL;

    /**
     * Where the hero's sword goes in one of his frames: the middle of its grip (in the frame's pixels), the way the
     * blade points (radians, on screen, before any mirroring), and whether it's behind him (at rest at his hip, or
     * held out in front of him while we see his back) or in front of him (in his fist, mid-swing).
     */
    record Grip(double x, double y, double angle, boolean behind) {}

    /** One of the hero's frames and where its sword goes. */
    private record Pose(PixelCanvas canvas, Grip grip) {}

    private static final Map<Sprite, Grip> GRIPS = new IdentityHashMap<>();

    /** Where the sword goes in this frame of the hero, or null if he isn't holding it (rolling, or not the hero). */
    static Grip grip(Sprite frame) { return GRIPS.get(frame); }

    private static Sprite[] poses(int n, IntFunction<Pose> paint, int ax, int ay) {
        Sprite[] out = new Sprite[n];
        for (int i = 0; i < n; i++) {
            Pose p = paint.apply(i);
            out[i] = new Sprite(p.canvas().image(), ax, ay, HERO_K);
            GRIPS.put(out[i], p.grip());
        }
        return out;
    }

    /**
     * Draws a frame of the hero with a sword in his grip: behind him or in his fist, as the frame says, mirrored with
     * him. (x, y) is where the frame's anchor goes, as for {@link Sprite#draw}. {@code white} > 0 adds a white flash.
     */
    static void drawWithSword(java.awt.Graphics2D g, Sprite body, double x, double y, boolean flip, float alpha, SwordArt.Kind kind, float white) {
        Grip gr = grip(body);
        Sprite sword = null;
        double sx = 0, sy = 0;
        if (gr != null) {
            sword = SwordArt.sprite(kind, gr.angle(), !gr.behind());
            double px = Art.SCALE * body.k;                                        // one of the frame's pixels, in world units
            int gx = (int) Math.floor(gr.x()), gy = (int) Math.floor(gr.y());
            sx = x + (flip ? body.ax - gx : gx - body.ax) * px;                   // the grip's pixel, wherever mirroring put it
            sy = y + (gy - body.ay) * px;
        }
        if (sword != null && gr.behind()) drawPart(g, sword, sx, sy, flip, alpha, white);
        drawPart(g, body, x, y, flip, alpha, white);
        if (sword != null && !gr.behind()) drawPart(g, sword, sx, sy, flip, alpha, white);
    }

    private static void drawPart(java.awt.Graphics2D g, Sprite s, double x, double y, boolean flip, float alpha, float white) {
        s.draw(g, x, y, Art.SCALE, flip, 0, alpha);
        if (white > 0) s.drawSilhouette(g, x, y, Art.SCALE, flip, 0xFFFFFF, white);
    }

    /** A grip given on the old grid (see {@link Fine}). */
    private static Grip grip(double x, double y, double angle, boolean behind) { return new Grip(Fine.q(x), Fine.q(y), angle, behind); }

    /** Frames painted on the fine grid (anchors in fine pixels): the size of the drawn sprites' pixels. */
    static Sprite[] fineFrames(int n, IntFunction<PixelCanvas> paint, int ax, int ay) { return heroFrames(n, paint, ax, ay); }

    private static Sprite[] heroFrames(int n, IntFunction<PixelCanvas> paint, int ax, int ay) {
        Sprite[] out = new Sprite[n];
        for (int i = 0; i < n; i++) out[i] = new Sprite(paint.apply(i).image(), ax, ay, HERO_K);
        return out;
    }

    /** Draws on a fine Doll with coordinates given on the old, coarser grid. Each shape call can start a new piece. */
    static final class Fine {
        final Doll d;

        Fine(int w, int h) { d = new Doll((int) Math.ceil(w * F), (int) Math.ceil(h * F)); }

        static int p(double v) { return (int) Math.round(v * F + 0.25); }

        static double q(double v) { return v * F + 0.25; }

        /** Starts a new piece (see {@link Doll#piece}). */
        Fine piece(Doll.Mat m) { d.piece(m); return this; }

        /** Starts a piece that merges into the last of its material (see {@link Doll#join}). */
        Fine join(Doll.Mat m) { d.join(m); return this; }

        Fine rect(double x, double y, double w, double h) {
            int x0 = p(x), y0 = p(y);
            d.rect(x0, y0, Math.max(1, p(x + w) - x0), Math.max(1, p(y + h) - y0));
            return this;
        }

        Fine ellipse(double cx, double cy, double rx, double ry) { d.ellipse(q(cx), q(cy), rx * F, ry * F); return this; }

        /** An ellipse cut to the rows from y0 up to y1 (old grid). */
        Fine ellipseY(double cx, double cy, double rx, double ry, double y0, double y1) { d.ellipseY(q(cx), q(cy), rx * F, ry * F, q(y0), q(y1)); return this; }

        Fine poly(double[] xs, double[] ys) {
            int[] px = new int[xs.length], py = new int[ys.length];
            for (int i = 0; i < xs.length; i++) { px[i] = p(xs[i]); py[i] = p(ys[i]); }
            d.poly(px, py);
            return this;
        }

        Fine disc(double cx, double cy, double r) { return ellipse(cx, cy, r, r); }

        Fine tri(double x0, double y0, double x1, double y1, double x2, double y2) { d.tri(p(x0), p(y0), p(x1), p(y1), p(x2), p(y2)); return this; }

        Fine thickLine(double x0, double y0, double x1, double y1, int t) { d.thickLine(p(x0), p(y0), p(x1), p(y1), (int) Math.round(t * F)); return this; }

        Fine line(double x0, double y0, double x1, double y1, int t) { return thickLine(x0, y0, x1, y1, t); }

        /** One exact pixel on the fine grid, over the shading. */
        void dot(int x, int y, int col) { d.dot(x, y, col); }

        void dot(int x, int y, Doll.Mat m, int tone) { d.dot(x, y, m, tone); }

        PixelCanvas finish() { return d.render(); }
    }

    /** One fine pixel of bob or lift, in old-grid units. */
    private static final double UP = 1 / F;

    /** Big chibi eyes, 2 x 3 fine pixels: a white glint at the top, the pupil, the coloured iris below. */
    private static void eye(Fine f, int x, int y) { eye(f, x, y, IRIS, IRIS_L); }

    static void eye(Fine f, int x, int y, int iris, int irisL) {
        f.dot(x, y, Doll.EYE); f.dot(x + 1, y, Doll.EYE);
        f.dot(x, y + 1, Doll.EYE_WHITE); f.dot(x + 1, y + 1, Doll.EYE);
        f.dot(x, y + 2, iris); f.dot(x + 1, y + 2, irisL);
    }

    /**
     * Everyone's face, the hero's proportions: eyes, blush and mouth for a head drawn like the hero's (old-grid centre
     * column {@code cx} and top row {@code top}). {@code old} gives narrow, kindly closed eyes instead.
     */
    static void face(Fine f, double cx, double top, int iris, int irisL, boolean old) {
        int el = Fine.p(cx - 3), er = Fine.p(cx + 3) - 1, ey = Fine.p(top + 6) - 1;
        if (old) {                                                                 // smiling, half-closed
            for (int x : new int[]{el, er}) { f.dot(x, ey + 2, Doll.EYE); f.dot(x + 1, ey + 2, Doll.EYE); f.dot(x - 1, ey + 1, Doll.EYE); }
        } else {
            eye(f, el, ey, iris, irisL);
            eye(f, er, ey, iris, irisL);
        }
        f.dot(el - 2, ey + 4, Doll.BLUSH); f.dot(er + 3, ey + 4, Doll.BLUSH);
        f.dot(el + 4, ey + 5, Doll.MOUTH); f.dot(el + 5, ey + 5, Doll.MOUTH);
    }

    /** Head seen from the front: spiky brown hair, big eyes, blush. (cx, top) is the head's centre column and top row (old grid); fb is the bob in fine pixels. */
    private static void headFront(Fine f, double cx, double top, int fb) {
        f.piece(SKIN_M).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);                    // the head (its sides show as ears)
        f.piece(HAIR_M).ellipse(cx + 0.5, top + 3.2, 6.2, 4.4)                     // hair cap and spikes
            .tri(cx - 5, top + 3, cx - 4, top - 1, cx - 2, top + 2)
            .tri(cx - 2, top + 1, cx, top - 2, cx + 2, top + 1)
            .tri(cx + 2, top + 2, cx + 4, top - 1, cx + 5, top + 3);
        f.join(SKIN_M).ellipse(cx + 0.5, top + 6.4, 4.7, 3.7);                     // the face, in front of the cap
        f.join(HAIR_M).rect(cx - 4, top + 3, 9, 1);                                // the fringe, in front of the face
        for (int x = 12; x <= 20; x++) f.d.set(x, 7 + fb);
        int y = 9 + fb;
        for (int x : new int[]{10, 11, 14, 18, 21, 22}) f.d.set(x, y);              // locks hanging over the brow
        f.d.set(11, y + 1); f.d.set(21, y + 1);
        // a band of shine curving over the crown, and dark partings between the locks
        int[][] shine = {{9, 6, 2}, {10, 5, 2}, {11, 4, 3}, {12, 4, 2}, {13, 3, 2}, {14, 1, 3}, {15, 2, 2}, {19, 3, 2}, {20, 4, 3}, {21, 4, 2}, {22, 5, 2}};
        for (int[] d : shine) f.dot(d[0], d[1] + fb, HAIR_M, d[2]);
        int[][] part = {{12, 7}, {13, 6}, {17, 4}, {17, 5}, {18, 6}, {16, 3}, {23, 7}, {9, 8}};
        for (int[] d : part) f.dot(d[0], d[1] + fb, HAIR_M, 0);
        eye(f, 11, 11 + fb);
        eye(f, 19, 11 + fb);
        f.dot(9, 15 + fb, Doll.BLUSH); f.dot(22, 15 + fb, Doll.BLUSH);
        f.dot(15, 16 + fb, Doll.MOUTH); f.dot(16, 16 + fb, Doll.MOUTH);
    }

    /** Legs: trousers and boots, each leg its own pieces (lift is how far it's raised, in old-grid units). */
    private static void leg(Fine f, double x, double lift, boolean back) {
        f.piece(back ? darker(PANTS_M) : PANTS_M).rect(x, 18 - lift, 3, 2);
        f.piece(back ? darker(BOOT_M) : BOOT_M).rect(x, 20 - lift, 3, 2);
    }

    /** The same material a step darker (the far leg, in shade). */
    private static Doll.Mat darker(Doll.Mat m) { return new Doll.Mat(m.line(), m.line(), m.dark(), m.mid(), m.light(), m.round()); }

    private static Pose heroFront(int step, int bob, int atk) {
        Fine f = new Fine(21, 25);
        double b = -bob * UP;
        int fb = -bob;
        double l1 = step == 1 ? 2 * UP : 0, l2 = step == 3 ? 2 * UP : 0;
        leg(f, 7, l1, false);
        leg(f, 11, l2, false);
        f.piece(TUNIC_M).rect(7, 12 + b, 7, 7).rect(6, 17 + b, 9, 2);              // tunic, flaring at the hem
        f.piece(BOOT_M).rect(7, 16 + b, 7, 1);                                     // belt
        f.dot(15, 24 + fb, GOLD_M, 3); f.dot(16, 24 + fb, GOLD_M, 2); f.dot(15, 25 + fb, GOLD_M, 0); f.dot(16, 25 + fb, GOLD_M, 0);   // buckle
        f.dot(15, 19 + fb, TUNIC_M, -1); f.dot(16, 19 + fb, TUNIC_M, -1); f.dot(14, 19 + fb, TUNIC_M, 0); f.dot(17, 19 + fb, TUNIC_M, 0);   // the neckline
        f.dot(15, 20 + fb, TUNIC_M, 0); f.dot(16, 20 + fb, TUNIC_M, 0);
        for (int y = 21; y <= 23; y++) f.dot(18, y + fb, TUNIC_M, 0);             // a fold, and the lit side of the chest
        f.dot(12, 20 + fb, TUNIC_M, 2); f.dot(13, 20 + fb, TUNIC_M, 3); f.dot(12, 21 + fb, TUNIC_M, 2);
        f.piece(TUNIC_M).rect(5, 13 + b, 2, 4);                                    // left arm and hand
        f.piece(SKIN_M).rect(5, 17 + b, 2, 1);
        Grip grip;
        if (atk == 0) {                                                            // wind-up: the sword raised over the shoulder
            f.piece(TUNIC_M).rect(14, 11 + b, 2, 3);
            f.piece(SKIN_M).rect(14, 10 + b, 2, 1);
            grip = grip(15, 10.5 + b, Math.atan2(-9, 3), false);
        } else if (atk == 1) {                                                     // strike: slashed down across the front
            f.piece(TUNIC_M).rect(14, 12 + b, 2, 3);
            f.piece(SKIN_M).rect(15, 11 + b, 2, 1);
            grip = grip(16, 11.5 + b, Math.atan2(11, -14), false);
        } else {                                                                   // at rest: held upright by the right hip
            f.piece(TUNIC_M).rect(14, 13 + b, 2, 4);
            f.piece(SKIN_M).rect(14, 17 + b, 2, 1);
            grip = grip(16, 17.5 + b, -Math.PI / 2, true);
        }
        headFront(f, 10, 2 + b, fb);
        return new Pose(f.finish(), grip);
    }

    private static Pose heroBack(int step, int bob, int atk) {
        Fine f = new Fine(21, 25);
        double b = -bob * UP;
        int fb = -bob;
        double l1 = step == 1 ? 2 * UP : 0, l2 = step == 3 ? 2 * UP : 0;
        leg(f, 7, l1, false);
        leg(f, 11, l2, false);
        f.piece(TUNIC_M).rect(7, 12 + b, 7, 7).rect(6, 17 + b, 9, 2);
        f.piece(BOOT_M).rect(7, 16 + b, 7, 1);
        f.piece(TUNIC_M).rect(5, 13 + b, 2, 4);
        f.piece(SKIN_M).rect(5, 17 + b, 2, 1);
        f.piece(TUNIC_M).rect(14, 13 + b, 2, 4);
        f.piece(SKIN_M).rect(14, 17 + b, 2, 1);
        Grip grip = atk < 0 ? grip(16, 5 + b, Math.atan2(14, -11), false)         // slung across his back, the hilt over the shoulder
            : atk == 0 ? grip(16, 12.5 + b, -Math.PI / 2, true)                     // raised in front of him (we see his back)
            : grip(16, 11.5 + b, Math.PI / 2, true);                                // brought down in front of him
        f.piece(SKIN_M).rect(8, 11 + b, 5, 1);                                     // a bit of neck
        f.piece(HAIR_M).ellipse(10.5, 7.5 + b, 6.0, 5.6)                           // the back of the head is all hair
            .tri(5, 5 + b, 6, 0 + b, 8, 3 + b)
            .tri(8, 2 + b, 10, -1 + b, 12, 2 + b)
            .tri(12, 3 + b, 14, 0 + b, 16, 5 + b);
        f.dot(14, 1 + fb, HAIR_M, 3); f.dot(15, 2 + fb, HAIR_M, 2); f.dot(10, 3 + fb, HAIR_M, 2);   // strands
        f.dot(11, 7 + fb, HAIR_M, 2); f.dot(12, 7 + fb, HAIR_M, 3); f.dot(13, 8 + fb, HAIR_M, 2);
        f.dot(17, 9 + fb, HAIR_M, 0); f.dot(18, 10 + fb, HAIR_M, 0); f.dot(15, 12 + fb, HAIR_M, 0); f.dot(16, 13 + fb, HAIR_M, 0); f.dot(12, 13 + fb, HAIR_M, 0);
        return new Pose(f.finish(), grip);
    }

    private static Pose heroSide(int step, int bob, int atk) {
        Fine f = new Fine(21, 25);
        double b = -bob * UP;
        int fb = -bob;
        int stride = step == 1 ? 2 : step == 3 ? -2 : 0;
        double lift1 = step == 1 ? 2 * UP : 0, lift2 = step == 3 ? 2 * UP : 0;
        leg(f, 8 - stride / 2.0, lift2, true);                                     // the far leg, in shade
        f.piece(PANTS_M).rect(10 + stride / 2.0, 18 - lift1, 3, 2);                // the near leg, its boot a little longer (the toe)
        f.piece(BOOT_M).rect(10 + stride / 2.0, 20 - lift1, 4, 2);
        f.piece(TUNIC_M).rect(8, 12 + b, 6, 7).rect(7, 17 + b, 8, 2);
        f.piece(BOOT_M).rect(8, 16 + b, 6, 1);
        f.dot(18, 24 + fb, GOLD_M, 2); f.dot(19, 24 + fb, GOLD_M, 3); f.dot(18, 25 + fb, GOLD_M, 0); f.dot(19, 25 + fb, GOLD_M, 0);
        Grip grip;
        if (atk == 0) {                                                            // wind-up: raised behind the head
            f.piece(TUNIC_M).rect(11, 11 + b, 3, 2);
            f.piece(SKIN_M).rect(13, 10 + b, 2, 2);
            grip = grip(14, 11 + b, Math.atan2(-9, 4), false);
        } else if (atk == 1) {                                                     // strike: thrust straight out ahead
            f.piece(TUNIC_M).rect(12, 13 + b, 4, 2);
            f.piece(SKIN_M).rect(16, 13 + b, 2, 2);
            grip = grip(17, 14 + b, 0, false);
        } else {                                                                   // at rest: held up and forward
            f.piece(TUNIC_M).rect(10, 13 + b, 3, 4);
            f.piece(SKIN_M).rect(10, 17 + b, 3, 1);
            grip = grip(11.5, 17.5 + b, -Math.PI / 4, false);
        }
        headSide(f, fb);
        return new Pose(f.finish(), grip);
    }

    /**
     * The head in profile, facing right, on the fine grid: hair over the top and the back of the head down to the
     * nape, swept-back spikes, a fringe falling forward over the brow; the face with its nose standing out from the
     * line of the cheek, a chin, the ear halfway back. {@code fb} is the bob, in fine pixels.
     */
    private static void headSide(Fine f, int fb) {
        Doll d = f.d;
        d.piece(SKIN_M).ellipse(17, 11.5 + fb, 8, 7.6);                            // the skull
        d.piece(HAIR_M).ellipse(15.6, 8.4 + fb, 7.8, 6.0)                          // the hair: crown, back of the head, nape
            .ellipse(12.2, 12.4 + fb, 4.6, 5.0)
            .tri(10, 4 + fb, 6, 1 + fb, 13, 3 + fb)                                 // small spikes, swept back
            .tri(14, 3 + fb, 13, -1 + fb, 18, 2 + fb)
            .tri(9, 8 + fb, 5, 7 + fb, 9, 11 + fb);
        d.join(SKIN_M).ellipse(20.6, 12.6 + fb, 5.4, 5.8)                          // the face
            .rect(25, 11 + fb, 1, 3).set(26, 12 + fb)                              // the nose, standing out
            .rect(20, 17 + fb, 4, 2);                                              // the chin
        d.join(HAIR_M);                                                            // the fringe, falling forward over the brow
        for (int x = 16; x <= 24; x++) d.set(x, 6 + fb);
        for (int x = 17; x <= 25; x++) d.set(x, 7 + fb);
        for (int x : new int[]{18, 19, 22, 23, 24, 25}) d.set(x, 8 + fb);
        d.set(24, 9 + fb); d.set(25, 9 + fb);
        f.dot(19, 2 + fb, HAIR_M, 3); f.dot(20, 3 + fb, HAIR_M, 2); f.dot(13, 3 + fb, HAIR_M, 3); f.dot(14, 4 + fb, HAIR_M, 2);   // shine
        f.dot(12, 5 + fb, HAIR_M, 2); f.dot(10, 10 + fb, HAIR_M, 0); f.dot(11, 12 + fb, HAIR_M, 0); f.dot(9, 13 + fb, HAIR_M, 0); f.dot(17, 9 + fb, HAIR_M, 0);
        eye(f, 21, 10 + fb);
        f.dot(23, 9 + fb, HAIR_M, 0);                                              // the brow
        f.dot(16, 11 + fb, SKIN_M, 1); f.dot(16, 12 + fb, SKIN_M, 0); f.dot(17, 12 + fb, SKIN_M, 1); f.dot(16, 13 + fb, SKIN_M, 0);   // the ear
        f.dot(21, 14 + fb, Doll.BLUSH);
        f.dot(24, 16 + fb, Doll.MOUTH);
    }

    /** The dodge roll: the hero tucked into a ball, spinning. */
    private static PixelCanvas heroRoll(int i) {
        Fine f = new Fine(17, 17);
        f.piece(TUNIC_M).disc(8.5, 8.5, 6.6);
        double a = i * Math.PI / 2;
        double bx = 8.5 - Math.cos(a) * 5.4, by = 8.5 - Math.sin(a) * 5.4;
        f.piece(BOOT_M).disc(bx, by, 2.0);                                          // boots on the far side
        double hx = 8.5 + Math.cos(a) * 5.0, hy = 8.5 + Math.sin(a) * 5.0;
        f.piece(HAIR_M).disc(hx, hy, 2.6);                                          // head / hair
        f.piece(SKIN_M).disc(hx - Math.cos(a) * 0.5, hy + 1 - Math.sin(a) * 0.5, 1.4);
        return f.finish();
    }


    // ------------------------------------------------------------------ the hermit

    private static final Doll.Mat MOSS_CLOAK = new Doll.Mat(rgb(14, 42, 28), rgb(34, 92, 56), rgb(58, 144, 78), rgb(106, 196, 106), rgb(168, 234, 156), false);
    private static final Doll.Mat HOOD_IN = new Doll.Mat(rgb(8, 22, 16), rgb(16, 40, 28), rgb(24, 58, 38), rgb(34, 76, 48), rgb(46, 96, 60), false);
    private static final Doll.Mat LANTERN = new Doll.Mat(rgb(110, 70, 20), rgb(255, 220, 120), rgb(255, 240, 170), rgb(255, 250, 214), rgb(255, 255, 240), true);

    /** The hermit of the Old Shrine: a mossy hooded cloak, a glowing lantern on a crooked staff, floating a little above the ground. */
    private static PixelCanvas guide(int fr) {
        Fine f = new Fine(23, 27);
        double b = fr == 0 ? 0 : -UP;
        int fb = fr == 0 ? 0 : -1;
        f.piece(Doll.WOOD).rect(19, 8 + b, 1, 17).rect(18, 7 + b, 1, 1);         // the staff
        f.piece(GOLD_M).rect(17, 4 + b, 5, 1).rect(18, 10 + b, 3, 1);             // the lantern's cap and base
        f.piece(LANTERN).rect(17.5, 5 + b, 4, 5);                                  // its glowing glass
        f.dot(Fine.p(19), Fine.p(6.5) + fb, rgb(255, 255, 255));
        f.piece(MOSS_CLOAK).poly(new double[]{8, 15, 18.5, 3.5}, new double[]{13 + b, 13 + b, 24 + b, 24 + b});   // the cloak
        for (int x = Fine.p(4); x <= Fine.p(18); x++) if ((x + fr) % 4 == 0) f.d.set(x, Fine.p(24) + fb);   // its ragged hem, fluttering
        f.dot(Fine.p(11), Fine.p(16) + fb, MOSS_CLOAK, 0); f.dot(Fine.p(11), Fine.p(17) + fb, MOSS_CLOAK, 0);   // folds
        f.dot(Fine.p(8), Fine.p(20) + fb, MOSS_CLOAK, 0); f.dot(Fine.p(14), Fine.p(21) + fb, MOSS_CLOAK, 0); f.dot(Fine.p(14), Fine.p(22) + fb, MOSS_CLOAK, 0);
        f.piece(MOSS_CLOAK).ellipse(11.5, 8.5 + b, 6.4, 6.2).tri(8, 4 + b, 11, -1 + b, 15, 4 + b);   // the hood
        f.piece(HOOD_IN).ellipse(11.5, 9.5 + b, 4.4, 4.2);                         // the dark inside of the hood
        f.piece(SKIN_M).ellipse(11.5, 10.2 + b, 3.4, 3.0);                         // a small, old face
        f.piece(Doll.SKIN).rect(16.5, 15 + b, 3, 2);                               // the hand on the staff
        int ey = Fine.p(9.5) + fb;
        f.dot(Fine.p(10), ey, Doll.EYE); f.dot(Fine.p(10) + 1, ey, Doll.EYE); f.dot(Fine.p(13), ey, Doll.EYE); f.dot(Fine.p(13) + 1, ey, Doll.EYE);   // eyes narrowed by age
        f.dot(Fine.p(10), ey + 1, rgb(160, 230, 200)); f.dot(Fine.p(13) + 1, ey + 1, rgb(160, 230, 200));          // and a faint green glint in them
        f.dot(Fine.p(11.5), ey + 4, Doll.MOUTH);
        f.dot(Fine.p(9), ey + 3, Doll.BLUSH); f.dot(Fine.p(14.5), ey + 3, Doll.BLUSH);
        return f.finish();
    }

    // ------------------------------------------------------------------ shops

    private static final Doll.Mat GI_RED = new Doll.Mat(rgb(70, 14, 24), rgb(144, 34, 50), rgb(202, 58, 66), rgb(238, 108, 100), rgb(255, 164, 142), false);
    private static final Doll.Mat HAIR_BLACK = new Doll.Mat(rgb(10, 8, 18), rgb(28, 24, 40), rgb(46, 40, 62), rgb(74, 68, 98), rgb(112, 106, 140), true);
    private static final Doll.Mat SHIRT_TEAL = new Doll.Mat(rgb(12, 50, 42), rgb(38, 102, 86), rgb(68, 158, 128), rgb(118, 206, 168), rgb(170, 236, 206), false);
    private static final Doll.Mat HAT_TAN = new Doll.Mat(rgb(68, 42, 18), rgb(120, 84, 48), rgb(174, 128, 74), rgb(210, 168, 108), rgb(236, 204, 150), false);
    private static final Doll.Mat PACK = new Doll.Mat(rgb(84, 36, 14), rgb(150, 76, 34), rgb(196, 110, 52), rgb(232, 154, 86), rgb(250, 196, 132), false);
    private static final Doll.Mat CANVAS = new Doll.Mat(rgb(110, 98, 74), rgb(190, 178, 148), rgb(226, 216, 188), rgb(244, 238, 218), rgb(255, 252, 240), false);
    private static final Doll.Mat SCARF = new Doll.Mat(rgb(96, 22, 26), rgb(172, 52, 50), rgb(226, 90, 70), rgb(250, 140, 112), rgb(255, 190, 160), false);

    /** A shopkeeper behind a little counter with wares on it. kind: 0 the trainer (Ranger Ash), 2 the merchant (Bramble). */
    private static PixelCanvas shop(int kind, int fr) {
        Fine f = new Fine(35, 47);
        double b = fr == 0 ? 0 : -UP;
        int fb = fr == 0 ? 0 : -1;
        int ox = 7, oy = 12;
        if (kind == 0) swordsmaster(f, ox, oy, b, fb);
        else survivalist(f, ox, oy, b, fb);
        // the counter, in front of the shopkeeper's legs: planks, a darker rail, nail heads
        f.piece(Doll.WOOD).rect(3, 35, 29, 11);
        for (double x = 8; x < 32; x += 8) for (int y = Fine.p(37.5); y < Fine.p(45); y++) f.dot(Fine.p(x), y, Doll.WOOD, 0);
        for (int x = Fine.p(3.5); x < Fine.p(31.5); x++) { f.dot(x, Fine.p(39), Doll.WOOD, 0); f.dot(x, Fine.p(35) + 1, Doll.WOOD, 2); }
        for (double x = 5; x < 32; x += 8) f.dot(Fine.p(x), Fine.p(36.5), GOLD_M, 2);
        f.piece(Doll.WOOD).rect(2, 34, 31, 1.5);                                   // the countertop's lip
        wares(f, kind);
        return f.finish();
    }

    private static void wares(Fine f, int kind) {
        double y = 34;   // the counter's top surface
        if (kind == 0) {                                                           // practice swords, a whetstone and a red pouch
            f.piece(STEEL_M).rect(6, y - 1, 8, 1);
            f.piece(GOLD_M).rect(5, y - 1.5, 1, 2);
            f.piece(BOOT_M).rect(3.5, y - 1, 1.5, 1);
            f.piece(new Doll.Mat(rgb(40, 40, 52), rgb(92, 94, 108), rgb(130, 132, 146), rgb(170, 172, 186), rgb(206, 208, 220), false)).rect(17, y - 2, 6, 2);
            f.piece(GI_RED).rect(26, y - 2.5, 4, 2.5);
            f.piece(GOLD_M).rect(27, y - 3.5, 2, 1);
        } else {                                                                   // coiled rope, a little tent, a lantern
            f.piece(HAT_TAN).ellipse(9.5, y - 1.5, 3.5, 1.7);
            f.piece(Doll.WOOD).ellipse(9.5, y - 1.5, 1.5, 0.7);
            f.piece(SHIRT_TEAL).tri(15, y, 21, y, 18, y - 6);
            f.piece(new Doll.Mat(rgb(8, 30, 24), rgb(18, 60, 48), rgb(30, 90, 74), rgb(44, 120, 98), rgb(60, 150, 120), false)).tri(18, y, 20, y, 18, y - 3);
            f.piece(GOLD_M).rect(26, y - 5, 3, 1).rect(26, y - 1, 3, 1);
            f.piece(LANTERN).rect(26, y - 4, 3, 3);
        }
    }

    /** Ranger Ash: red gi with gold shoulder guards, a red headband, black hair in a top-knot, a big sword behind. */
    private static void swordsmaster(Fine f, int ox, int oy, double b, int fb) {
        f.piece(STEEL_M).rect(ox + 17, oy - 1 + b, 2, 18);                         // the big sword, standing behind the right shoulder
        f.piece(GOLD_M).rect(ox + 15, oy + 17 + b, 6, 1);
        f.piece(GI_RED).rect(ox + 6, oy + 12 + b, 9, 12);                          // the gi
        f.piece(BOOT_M).rect(ox + 6, oy + 17 + b, 9, 2);                            // a dark sash
        f.dot(Fine.p(ox + 10), Fine.p(oy + 17.5) + fb, GOLD_M, 2); f.dot(Fine.p(ox + 10) + 1, Fine.p(oy + 17.5) + fb, GOLD_M, 3);
        for (int y = Fine.p(oy + 12.5); y < Fine.p(oy + 17); y++) f.dot(Fine.p(ox + 10.5) + (y % 2), y + fb, GI_RED, 0);   // the gi's crossed front
        f.piece(GI_RED).rect(ox + 4, oy + 13 + b, 3, 6);                           // arms
        f.piece(SKIN_M).rect(ox + 4, oy + 19 + b, 3, 2);
        f.piece(GI_RED).rect(ox + 14, oy + 13 + b, 3, 6);
        f.piece(SKIN_M).rect(ox + 14, oy + 19 + b, 3, 2);
        f.piece(GOLD_M).rect(ox + 3, oy + 12 + b, 4, 2);                            // shoulder guards
        f.piece(GOLD_M).rect(ox + 14, oy + 12 + b, 4, 2);
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(SKIN_M).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
        f.piece(HAIR_BLACK).ellipse(cx + 0.5, top + 3.4, 6.0, 4.0);                // hair, swept back
        f.piece(HAIR_BLACK).disc(cx + 0.5, top - 1.4, 1.9);                         // tied up in a round knot
        f.piece(SCARF).rect(cx - 0.8, top + 0.2, 2.6, 0.8);                         // with a red tie
        f.join(SKIN_M).ellipse(cx + 0.5, top + 6.4, 4.7, 3.7);
        f.piece(SCARF).rect(cx - 5, top + 2.6, 11, 1.4).rect(cx + 5, top + 2.6, 3, 1).rect(cx + 6.5, top + 3.4, 1.5, 2);   // the headband and its trailing ends
        f.dot(Fine.p(cx - 2), Fine.p(top + 1), HAIR_BLACK, 3); f.dot(Fine.p(cx - 1), Fine.p(top + 1), HAIR_BLACK, 2); f.dot(Fine.p(cx), Fine.p(top - 2), HAIR_BLACK, 3);
        face(f, cx, top, rgb(90, 60, 40), rgb(140, 100, 70), false);
        int ey = Fine.p(top + 6) - 1;                                              // a stern brow
        f.dot(Fine.p(cx - 3) - 1, ey - 2, HAIR_BLACK, 1); f.dot(Fine.p(cx - 3), ey - 1, HAIR_BLACK, 1); f.dot(Fine.p(cx + 3), ey - 2, HAIR_BLACK, 1); f.dot(Fine.p(cx + 3) - 1, ey - 1, HAIR_BLACK, 1);
    }

    /** Bramble: a teal shirt, red scarf, a wide-brimmed hat, a big backpack with a bedroll on top. */
    private static void survivalist(Fine f, int ox, int oy, double b, int fb) {
        f.piece(PACK).rect(ox + 1, oy + 9 + b, 6, 10);                             // the backpack, peeking out both sides
        f.piece(PACK).rect(ox + 14, oy + 9 + b, 6, 10);
        f.piece(CANVAS).rect(ox, oy + 8 + b, 21, 3);                               // the bedroll on top
        for (double x : new double[]{ox + 4, ox + 16}) for (int y = Fine.p(oy + 8); y < Fine.p(oy + 11); y++) f.dot(Fine.p(x), y + fb, BOOT_M, 1);   // its straps
        f.piece(SHIRT_TEAL).rect(ox + 6, oy + 12 + b, 9, 12);
        f.piece(BOOT_M).rect(ox + 6, oy + 17 + b, 9, 1);                            // belt
        f.dot(Fine.p(ox + 10), Fine.p(oy + 17) + fb, GOLD_M, 2);
        f.piece(SHIRT_TEAL).rect(ox + 4, oy + 13 + b, 3, 6);
        f.piece(SKIN_M).rect(ox + 4, oy + 19 + b, 3, 2);
        f.piece(SHIRT_TEAL).rect(ox + 14, oy + 13 + b, 3, 6);
        f.piece(SKIN_M).rect(ox + 14, oy + 19 + b, 3, 2);
        f.piece(SCARF).rect(ox + 8, oy + 12 + b, 5, 2).rect(ox + 10, oy + 14 + b, 2, 2);   // the scarf, its end hanging
        double cx = ox + 10, top = oy + 2 + b;
        f.piece(SKIN_M).ellipse(cx + 0.5, top + 5.2, 5.6, 5.2);
        f.piece(HAIR_M).rect(cx - 6, top + 6, 1, 3).rect(cx + 6, top + 6, 1, 3);   // sideburns
        f.piece(HAT_TAN).ellipseY(cx + 0.5, top + 3, 6.6, 4.0, top - 1, top + 4).rect(cx - 8, top + 3, 17, 2);   // the wide-brimmed hat
        f.piece(SCARF).rect(cx - 5, top + 1, 11, 1);                               // its band
        f.dot(Fine.p(cx - 2), Fine.p(top) + 1, HAT_TAN, 3); f.dot(Fine.p(cx - 1), Fine.p(top) + 1, HAT_TAN, 2);
        face(f, cx, top, rgb(70, 120, 60), rgb(120, 180, 90), false);
    }
}
