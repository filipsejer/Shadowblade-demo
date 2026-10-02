package game;

import static game.PixelCanvas.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The forest's ground and scenery, painted to sit with the drawn sprites ({@link ImportedArt}): the same pixel size
 * ({@link #PX} world units, finer than the old code-painted art), their palette (yellow-green foliage over deep
 * blue-green shadow, warm bark, olive moss, blue-grey stone), shading in a few hard-edged tones with ordered dithering
 * between them, and near-black outlines on anything that stands up off the ground.
 *
 * <p>The ground is a set of large seamless textures (grass, a dirt path, the camp's flagstones, the hedge that walls
 * the clearings in, the dark canopy beyond), each varied over its whole width so the repeat doesn't show, and the
 * floor is then broken up further with patches and small things lying on it. The trees around the clearings are the
 * drawn landmarks themselves, plus recoloured copies (an autumn oak, a darker one).
 */
final class ForestArt {
    private ForestArt() {}

    /** World units per art pixel: the drawn sprites' size. */
    static final int PX = ImportedArt.PIXEL;
    /** That pixel size relative to the code-painted art's ({@link Sprite#k}). */
    static final double K = PX / (double) Art.SCALE;

    // ------------------------------------------------------------------ palette (sampled from the drawn sprites)

    static final int OUTLINE = rgb(16, 20, 10);
    // the ground: a little darker and quieter than the foliage, so everything standing on it reads
    static final int G_DEEP = rgb(44, 80, 42), G_DARK = rgb(58, 98, 46), G_MID = rgb(74, 118, 50),
        G_LIGHT = rgb(94, 138, 56), G_HIGH = rgb(120, 160, 62), G_TIP = rgb(154, 186, 76);
    static final int[] GRASS = {G_DEEP, G_DARK, G_MID, G_LIGHT, G_HIGH, G_TIP};
    static final int MOSS = rgb(110, 124, 46), MOSS_L = rgb(142, 164, 70), MOSS_D = rgb(84, 96, 38);
    static final int D_DEEP = rgb(70, 48, 34), D_DARK = rgb(92, 64, 44), D_MID = rgb(116, 84, 56), D_LIGHT = rgb(140, 104, 70), D_HIGH = rgb(164, 128, 88);
    private static final int[] DIRT = {D_DEEP, D_DARK, D_MID, D_LIGHT, D_HIGH};
    static final int ST_GROUT = rgb(84, 80, 58), ST_DARK = rgb(118, 112, 100), ST_MID = rgb(136, 130, 118), ST_LIGHT = rgb(152, 146, 132), ST_HIGH = rgb(172, 164, 148);
    static final int BARK_D = rgb(44, 25, 20), BARK = rgb(78, 49, 35), BARK_M = rgb(99, 61, 41), BARK_L = rgb(132, 85, 56), WOOD = rgb(186, 140, 92), WOOD_D = rgb(150, 104, 66);
    private static final int[] HEDGE = {rgb(18, 36, 20), rgb(30, 60, 30), rgb(46, 98, 36), rgb(76, 144, 44), rgb(118, 186, 60)};
    private static final int[] CANOPY = {rgb(10, 20, 16), rgb(16, 34, 24), rgb(24, 50, 30), rgb(36, 70, 38), rgb(52, 92, 46)};

    /** A 4x4 ordered-dither threshold, 0..1. */
    private static final double[][] BAYER = {
        {0 / 16.0, 8 / 16.0, 2 / 16.0, 10 / 16.0}, {12 / 16.0, 4 / 16.0, 14 / 16.0, 6 / 16.0},
        {3 / 16.0, 11 / 16.0, 1 / 16.0, 9 / 16.0}, {15 / 16.0, 7 / 16.0, 13 / 16.0, 5 / 16.0}};

    static double bayer(int x, int y) { return BAYER[Math.floorMod(y, 4)][Math.floorMod(x, 4)]; }

    // ------------------------------------------------------------------ seamless noise

    /** Value noise that repeats every {@code size} pixels: a lattice of {@code cells} random values, smoothly blended. */
    private static final class Wrap {
        private final double[] v;
        private final int cells;
        private final double cell;

        Wrap(int size, int cells, long seed) {
            this.cells = cells;
            this.cell = size / (double) cells;
            Random r = new Random(seed);
            v = new double[cells * cells];
            for (int i = 0; i < v.length; i++) v[i] = r.nextDouble();
        }

        double at(double x, double y) {
            double gx = x / cell, gy = y / cell;
            int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
            double fx = gx - x0, fy = gy - y0;
            fx = fx * fx * (3 - 2 * fx);
            fy = fy * fy * (3 - 2 * fy);
            int xa = Math.floorMod(x0, cells), xb = Math.floorMod(x0 + 1, cells), ya = Math.floorMod(y0, cells), yb = Math.floorMod(y0 + 1, cells);
            double top = v[ya * cells + xa] * (1 - fx) + v[ya * cells + xb] * fx;
            double bot = v[yb * cells + xa] * (1 - fx) + v[yb * cells + xb] * fx;
            return top * (1 - fy) + bot * fy;
        }
    }

    /** Three octaves of seamless noise, 0..1, mostly around the middle. */
    static double[] field(int size, long seed, int coarse) {
        Wrap a = new Wrap(size, coarse, seed), b = new Wrap(size, coarse * 2, seed + 1), c = new Wrap(size, coarse * 4, seed + 2);
        double[] f = new double[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) f[y * size + x] = a.at(x, y) * 0.58 + b.at(x, y) * 0.3 + c.at(x, y) * 0.12;
        return f;
    }

    /** Picks a tone from a ramp for a 0..1 value, dithering across the boundaries between tones. */
    static int tone(int[] ramp, int lo, int hi, double t, int x, int y, double spread) {
        double s = lo + t * (hi - lo + 1) + (bayer(x, y) - 0.5) * spread;
        return ramp[Math.max(lo, Math.min(hi, (int) Math.floor(s)))];
    }

    static int stepOf(int[] ramp, int c) {
        for (int i = 0; i < ramp.length; i++) if (ramp[i] == c) return i;
        return -1;
    }

    // ------------------------------------------------------------------ the ground

    /** Grass: broad light and shadow across the whole tile, then hundreds of little tufts of blades. 160 px, seamless. */
    static PixelCanvas grass() {
        int n = 160;
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = field(n, 11, 2);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) c.set(x, y, tone(GRASS, 1, 3, soft(f[y * n + x]), x, y, 0.8));
        Random r = new Random(12);
        for (int i = 0; i < 520; i++) tuft(c, r.nextInt(n), r.nextInt(n), r, GRASS, true);
        for (int i = 0; i < 70; i++) {                                     // seeds and dew: single bright pixels
            int x = r.nextInt(n), y = r.nextInt(n);
            int s = stepOf(GRASS, c.get(x, y));
            if (s >= 0 && s < 4) c.set(x, y, GRASS[s + 2]);
        }
        for (int i = 0; i < 10; i++) clover(c, r.nextInt(n), r.nextInt(n), true);
        return c;
    }

    /** Mostly the middle tone, with gentle lighter and darker drifts. */
    static double soft(double v) { return Math.max(0, Math.min(1, (v - 0.5) * 1.35 + 0.5)); }

    /** Pushes noise away from the middle, so the tile has real light and dark areas rather than all mid-tone. */
    static double stretch(double v) { return Math.max(0, Math.min(1, (v - 0.5) * 1.9 + 0.5)); }

    /** A tuft of 2-5 blades: dark at the root, lighter up to a bright tip, leaning a little. */
    static void tuft(PixelCanvas c, int x, int y, Random r, int[] ramp, boolean wrap) {
        int blades = 2 + r.nextInt(4);
        for (int b = 0; b < blades; b++) {
            int bx = x + r.nextInt(5) - 2, h = 2 + r.nextInt(3), lean = r.nextInt(3) - 1;
            int base = stepOf(ramp, get(c, bx, y, wrap));
            if (base < 0) base = 2;
            for (int k = 0; k < h; k++) {
                int px = bx + (k == h - 1 ? lean : 0), py = y - k;
                int s = k == 0 ? base - 1 : Math.min(ramp.length - 1, base + k);
                put(c, px, py, ramp[Math.max(0, s)], wrap);
            }
        }
    }

    private static void clover(PixelCanvas c, int x, int y, boolean wrap) {
        int[][] leaf = {{0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {0, 2}, {1, 2}};
        for (int[] p : leaf) put(c, x + p[0], y + p[1], G_DARK, wrap);
        put(c, x, y, G_LIGHT, wrap);
        put(c, x + 1, y + 1, G_MID, wrap);
    }

    static int get(PixelCanvas c, int x, int y, boolean wrap) {
        return wrap ? c.get(Math.floorMod(x, c.w), Math.floorMod(y, c.h)) : c.get(x, y);
    }

    static void put(PixelCanvas c, int x, int y, int col, boolean wrap) {
        if (wrap) c.set(Math.floorMod(x, c.w), Math.floorMod(y, c.h), col);
        else c.set(x, y, col);
    }

    /** A packed-earth path: tones in long streaks along the way, ruts, scattered pebbles and the odd sprig. 96 px, seamless. */
    static PixelCanvas dirt() {
        int n = 96;
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = field(n, 21, 3);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) c.set(x, y, tone(DIRT, 1, 3, stretch(f[y * n + x]), x, y, 1.0));
        Random r = new Random(22);
        for (int i = 0; i < 90; i++) {                                     // grit
            int x = r.nextInt(n), y = r.nextInt(n);
            int s = stepOf(DIRT, c.get(x, y));
            if (s > 0) c.set(x, y, DIRT[r.nextBoolean() ? s - 1 : Math.min(4, s + 1)]);
        }
        for (int i = 0; i < 9; i++) pebble(c, r.nextInt(n), r.nextInt(n), r.nextInt(3) == 0 ? 2 : 1, true);
        for (int i = 0; i < 8; i++) tuft(c, r.nextInt(n), r.nextInt(n), r, GRASS, true);
        return c;
    }

    /** A pebble: light on top, dark underneath, outlined. */
    private static void pebble(PixelCanvas c, int x, int y, int size, boolean wrap) {
        for (int dx = -1; dx <= size; dx++) {                              // the dark rim
            put(c, x + dx, y - 1, OUTLINE_SOFT, wrap);
            put(c, x + dx, y + size, OUTLINE_SOFT, wrap);
        }
        for (int dy = 0; dy < size; dy++) { put(c, x - 1, y + dy, OUTLINE_SOFT, wrap); put(c, x + size, y + dy, OUTLINE_SOFT, wrap); }
        for (int dy = 0; dy < size; dy++) for (int dx = 0; dx < size; dx++) put(c, x + dx, y + dy, dy == 0 ? ST_HIGH : ST_MID, wrap);
        if (size > 1) put(c, x + size - 1, y + size - 1, ST_DARK, wrap);
    }

    private static final int OUTLINE_SOFT = rgb(58, 44, 32);

    /**
     * The camp's paving: rounded cobbles of different sizes (a seamless Voronoi pattern), each lit along its top
     * edge and shaded along its bottom, set in dark earth with moss creeping through. 96 px, seamless.
     */
    static PixelCanvas flagstones() {
        int n = 96, cells = 6;
        double cell = n / (double) cells;
        Random r = new Random(31);
        double[] sx = new double[cells * cells], sy = new double[cells * cells];
        int[] base = new int[cells * cells];
        for (int j = 0; j < cells; j++) for (int i = 0; i < cells; i++) {
            int k = j * cells + i;
            sx[k] = (i + 0.2 + r.nextDouble() * 0.6) * cell;
            sy[k] = (j + 0.2 + r.nextDouble() * 0.6) * cell;
            base[k] = r.nextInt(4) == 0 ? 0 : r.nextInt(3) == 0 ? 2 : 1;
        }
        int[] owner = new int[n * n];
        boolean[] gap = new boolean[n * n];
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            double best = 1e9, second = 1e9;
            int who = 0;
            for (int k = 0; k < sx.length; k++) {
                double dx = Math.abs(x + 0.5 - sx[k]), dy = Math.abs(y + 0.5 - sy[k]);
                dx = Math.min(dx, n - dx);
                dy = Math.min(dy, n - dy);
                double d = Math.hypot(dx, dy);
                if (d < best) { second = best; best = d; who = k; } else if (d < second) second = d;
            }
            owner[y * n + x] = who;
            gap[y * n + x] = second - best < 2.2;
        }
        int[] ramp = {ST_DARK, ST_MID, ST_LIGHT, ST_HIGH};
        PixelCanvas c = new PixelCanvas(n, n);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int i = y * n + x;
            if (gap[i]) { c.set(x, y, (x * 7 + y * 3) % 11 == 0 ? MOSS_D : ST_GROUT); continue; }
            int s = base[owner[i]];
            boolean topEdge = gap[Math.floorMod(y - 1, n) * n + x] || gap[Math.floorMod(y - 2, n) * n + x] && bayer(x, y) > 0.5;
            boolean bottomEdge = gap[Math.floorMod(y + 1, n) * n + x];
            boolean leftEdge = gap[y * n + Math.floorMod(x - 1, n)];
            if (topEdge || leftEdge) s = Math.min(3, s + 1);
            if (bottomEdge) s = Math.max(0, s - 1);
            c.set(x, y, ramp[s]);
        }
        for (int k = 0; k < 220; k++) {                                     // moss and grass in the gaps
            int x = r.nextInt(n), y = r.nextInt(n);
            if (c.get(x, y) != ST_GROUT) continue;
            c.set(x, y, r.nextInt(3) == 0 ? MOSS_L : MOSS);
            if (r.nextInt(4) == 0 && c.get(x, Math.floorMod(y - 1, n)) == ST_GROUT) c.set(x, Math.floorMod(y - 1, n), G_LIGHT);
        }
        for (int k = 0; k < 40; k++) {                                       // wear: a few darker pits in the stone
            int x = r.nextInt(n), y = r.nextInt(n);
            int s = stepOf(ramp, c.get(x, y));
            if (s > 0) c.set(x, y, ramp[s - 1]);
        }
        return c;
    }

    /** The hedge that walls the clearings in: leafy clumps packed together, lit from the top left, a few berries. 96 px, seamless. */
    static PixelCanvas hedge() {
        int n = 96;
        PixelCanvas c = new PixelCanvas(n, n);
        c.rect(0, 0, n, n, HEDGE[0]);
        Random r = new Random(41);
        for (int i = 0; i < 150; i++) clump(c, r.nextInt(n), r.nextInt(n), 4 + r.nextDouble() * 4, HEDGE, r, true);
        for (int i = 0; i < 14; i++) {
            int x = r.nextInt(n), y = r.nextInt(n);
            put(c, x, y, rgb(238, 80, 48), true);
            put(c, x + 1, y + 1, rgb(150, 34, 34), true);
            put(c, x, y + 1, rgb(196, 52, 40), true);
        }
        return c;
    }

    /** Beyond the hedge: the dark roof of the forest, big crowns crowding each other. 128 px, seamless. */
    static PixelCanvas canopy() {
        int n = 128;
        PixelCanvas c = new PixelCanvas(n, n);
        c.rect(0, 0, n, n, CANOPY[0]);
        Random r = new Random(51);
        for (int i = 0; i < 70; i++) clump(c, r.nextInt(n), r.nextInt(n), 7 + r.nextDouble() * 8, CANOPY, r, true);
        return c;
    }

    /**
     * A leafy clump: a scalloped blob (its edge wobbles like bunched leaves) in four tones: a shadow under it, its body,
     * a lit crown towards the top left and a few bright leaves, with dithered edges between the tones.
     */
    static void clump(PixelCanvas c, int cx, int cy, double rad, int[] ramp, Random r, boolean wrap) {
        double phase = r.nextDouble() * Math.PI * 2;
        int lobes = 5 + r.nextInt(3);
        int R = (int) Math.ceil(rad * 1.3) + 2;
        for (int dy = -R; dy <= R; dy++) {
            for (int dx = -R; dx <= R; dx++) {
                double d = Math.hypot(dx, dy), a = Math.atan2(dy, dx);
                double edge = rad * (1 + 0.16 * Math.sin(a * lobes + phase));
                double sd = Math.hypot(dx - 1.2, dy - 1.6);                // the shadow, offset down-right
                if (d > edge) {
                    if (sd <= edge) {                                         // cast onto whatever's underneath: one tone darker
                        int under = stepOf(ramp, get(c, cx + dx, cy + dy, wrap));
                        if (under > 0) put(c, cx + dx, cy + dy, ramp[under - 1], wrap);
                    }
                    continue;
                }
                double lit = 1 - Math.hypot(dx + rad * 0.35, dy + rad * 0.4) / (rad * 1.25);   // brightest up and to the left
                double t = lit + (bayer(cx + dx, cy + dy) - 0.5) * 0.35;
                int s = t > 0.62 ? 4 : t > 0.3 ? 3 : 2;
                if (d > edge - 1.1 && dy > 0) s = 2;                          // the underside rim stays in shade
                if (s == 4 && r.nextInt(3) != 0) s = 3;                       // bright leaves, not a bright patch
                put(c, cx + dx, cy + dy, ramp[s], wrap);
            }
        }
    }

    // ------------------------------------------------------------------ things lying on the floor

    private static Sprite fine(PixelCanvas c, int ax, int ay) { return new Sprite(c.image(), ax, ay, K); }

    /** Indices into {@link #floor()} of the grass clumps (they also fringe the paths, and are all that grows between the camp's stones). */
    static final int CLUMP = 5, CLUMPS = 3;

    /** Little things scattered over the grass (anchored at the bottom middle). */
    static Sprite[] floor() {
        return new Sprite[]{
            flower(rgb(250, 248, 236), rgb(255, 210, 70), rgb(196, 188, 176)),
            flower(rgb(255, 216, 72), rgb(214, 120, 30), rgb(196, 140, 40)),
            flower(rgb(255, 128, 168), rgb(255, 230, 130), rgb(196, 70, 110)),
            flower(rgb(132, 168, 255), rgb(255, 240, 150), rgb(80, 100, 196)),
            flowerBunch(), grassClump(new Random(1)), grassClump(new Random(2)), grassClump(new Random(3)),
            rock(), mushroom(), fern(), leaves(), clover(),
        };
    }

    /** Big soft patches of lighter or darker grass, moss and bare earth: laid down first, they break up the ground's repeat. */
    static Sprite[] patches() {
        return new Sprite[]{
            patch(48, 26, G_LIGHT, G_HIGH, 61), patch(64, 32, G_LIGHT, G_HIGH, 62), patch(44, 24, G_DARK, G_DEEP, 63),
            patch(60, 30, G_DARK, G_DEEP, 64), patch(36, 20, G_LIGHT, G_HIGH, 65), patch(40, 22, G_DARK, G_DEEP, 66), bareEarth(26, 14),
        };
    }

    /** A soft-edged blob of one grass tone (dithered out at the rim), with blades of the next tone on it. */
    private static Sprite patch(int w, int h, int body, int blades, long seed) {
        PixelCanvas c = new PixelCanvas(w, h);
        Random r = new Random(seed);
        double phase = r.nextDouble() * 6;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0);
            double a = Math.atan2(dy, dx), d = Math.hypot(dx, dy) / (1 + 0.14 * Math.sin(a * 4 + phase));
            if (d < 1 && (1 - d) * 1.8 > bayer(x, y)) c.set(x, y, body);
        }
        for (int i = 0; i < w * h / 40; i++) {
            int x = 2 + r.nextInt(w - 4), y = 3 + r.nextInt(h - 5);
            if (c.solid(x, y) && c.solid(x, y - 2)) { c.set(x, y - 1, blades); c.set(x, y - 2, r.nextBoolean() ? blades : G_TIP); }
        }
        return fine(c, w / 2, h / 2);
    }

    private static Sprite bareEarth(int w, int h) {
        PixelCanvas c = new PixelCanvas(w, h);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0), d = Math.hypot(dx, dy);
            if (d >= 1) continue;
            double t = (1 - d) * 2.4 - bayer(x, y);
            if (t > 0.7) c.set(x, y, (x * 3 + y) % 7 == 0 ? D_MID : D_LIGHT);
            else if (t > 0) c.set(x, y, G_DARK);                              // trodden grass around the bare middle
        }
        return fine(c, w / 2, h / 2);
    }

    private static Sprite flower(int petal, int centre, int petalShade) {
        PixelCanvas c = new PixelCanvas(7, 9);
        int stem = G_DARK, leaf = G_LIGHT;
        c.rect(3, 4, 1, 5, stem);
        c.set(2, 6, leaf); c.set(4, 7, leaf); c.set(1, 5, leaf);
        c.set(3, 1, petal); c.set(2, 2, petal); c.set(4, 2, petal); c.set(3, 3, petalShade);
        c.set(2, 3, petalShade); c.set(4, 3, petalShade); c.set(3, 2, centre);
        c.set(1, 2, petalShade); c.set(5, 2, petalShade);
        return fine(c, 3, 8);
    }

    private static Sprite flowerBunch() {
        PixelCanvas c = new PixelCanvas(13, 10);
        int[][] heads = {{3, 2}, {7, 1}, {10, 3}, {5, 5}};
        int[] cols = {rgb(250, 248, 236), rgb(255, 216, 72), rgb(250, 248, 236), rgb(255, 128, 168)};
        for (int i = 0; i < heads.length; i++) {
            int x = heads[i][0], y = heads[i][1];
            c.line(x, y + 1, 6, 9, G_DARK);
        }
        for (int i = 0; i < heads.length; i++) {
            int x = heads[i][0], y = heads[i][1], p = cols[i];
            c.set(x, y - 1, p); c.set(x - 1, y, p); c.set(x + 1, y, p); c.set(x, y + 1, darken(p, 0.25)); c.set(x, y, rgb(255, 200, 60));
        }
        c.set(4, 8, G_LIGHT); c.set(8, 8, G_LIGHT); c.set(5, 9, G_MID); c.set(7, 9, G_MID);
        return fine(c, 6, 9);
    }

    private static Sprite grassClump(Random r) {
        PixelCanvas c = new PixelCanvas(13, 9);
        for (int i = 0; i < 7; i++) {
            int x = 1 + r.nextInt(11), h = 3 + r.nextInt(5), lean = r.nextInt(3) - 1;
            for (int k = 0; k < h; k++) {
                int px = x + (k > h / 2 ? lean : 0);
                c.set(px, 8 - k, k == 0 ? G_DEEP : k == h - 1 ? G_TIP : k > h / 2 ? G_HIGH : G_LIGHT);
            }
        }
        return fine(c, 6, 8);
    }

    private static Sprite rock() {
        PixelCanvas c = new PixelCanvas(12, 9);
        c.ellipse(6, 5.2, 4.8, 3.2, ST_MID);
        c.ellipseY(6, 5.2, 4.8, 3.2, ST_DARK, 6, 9);
        c.ellipse(5, 4, 2.6, 1.6, ST_LIGHT);
        c.set(4, 3, ST_HIGH); c.set(5, 3, ST_HIGH);
        c.set(8, 3, MOSS); c.set(9, 4, MOSS_L); c.set(8, 4, MOSS_D);
        c.outline(OUTLINE);
        return fine(c, 6, 8);
    }


    private static Sprite mushroom() {
        PixelCanvas c = new PixelCanvas(9, 10);
        int cap = rgb(214, 61, 78), capD = rgb(153, 24, 62), capL = rgb(249, 124, 126), stem = rgb(248, 228, 200), stemD = rgb(216, 168, 158);
        c.rect(3, 5, 3, 4, stem); c.rect(5, 5, 1, 4, stemD);
        c.ellipseY(4.5, 4.5, 4, 3.5, cap, 0, 5.5);
        c.ellipseY(4.5, 4.5, 4, 3.5, capD, 4.5, 5.5);
        c.set(2, 2, capL); c.set(3, 1, capL);
        c.set(2, 3, rgb(254, 253, 252)); c.set(6, 2, rgb(254, 253, 252)); c.set(4, 4, rgb(254, 253, 252));
        c.outline(OUTLINE);
        return fine(c, 4, 9);
    }

    private static Sprite fern() {
        PixelCanvas c = new PixelCanvas(15, 10);
        c.line(7, 9, 7, 2, G_DARK);
        for (int i = 0; i < 4; i++) {
            int y = 8 - i * 2, reach = 6 - i;
            c.line(7, y, 7 - reach, y - 2, i % 2 == 0 ? G_LIGHT : G_HIGH);
            c.line(7, y, 7 + reach, y - 2, i % 2 == 0 ? G_HIGH : G_LIGHT);
            c.set(7 - reach, y - 2, G_TIP); c.set(7 + reach, y - 2, G_TIP);
        }
        c.set(7, 1, G_TIP);
        return fine(c, 7, 9);
    }

    private static Sprite leaves() {
        PixelCanvas c = new PixelCanvas(12, 6);
        int[][] at = {{1, 2}, {5, 0}, {8, 3}};
        int[] col = {rgb(214, 120, 40), rgb(236, 176, 60), rgb(176, 82, 36)};
        for (int i = 0; i < at.length; i++) {
            int x = at[i][0], y = at[i][1];
            c.rect(x, y, 3, 2, col[i]); c.set(x + 2, y + 1, darken(col[i], 0.3)); c.set(x, y, lighten(col[i], 0.2));
        }
        return fine(c, 6, 5);
    }

    private static Sprite clover() {
        PixelCanvas c = new PixelCanvas(10, 6);
        clover(c, 2, 1, false);
        clover(c, 6, 2, false);
        return fine(c, 5, 5);
    }

    // ------------------------------------------------------------------ scenery around the clearings

    /** Trees around the clearings: the drawn oak and pine, and the oak recoloured for autumn and for deep shade. */
    static Sprite[] tall() {
        Sprite oak = Art.frames("landmark.oak")[0], pine = Art.frames("landmark.pine")[0];
        return new Sprite[]{oak, pine, recolour(oak, 28, 1.05, 1.0), pine, recolour(oak, -1, 0.82, 0.95)};
    }

    /** Low things for the south side and the gaps: the drawn bush, boulder and stump, a log and a ring of mushrooms. */
    static Sprite[] low() {
        return new Sprite[]{Art.frames("landmark.bush")[0], Art.frames("landmark.boulder")[0], Art.frames("landmark.stump")[0], log(), mushroomCluster()};
    }

    /**
     * A copy of a drawn sprite with its foliage turned to another hue ({@code hue} in degrees, or -1 to keep it), and
     * its brightness and saturation scaled. Only green pixels change, so trunks and outlines stay as drawn.
     */
    static Sprite recolour(Sprite s, double hue, double value, double saturation) {
        BufferedImage src = s.img, out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24, r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                java.awt.Color.RGBtoHSB(r, g, b, hsb);
                double h = hsb[0] * 360;
                if (a > 0 && h > 60 && h < 170 && hsb[1] > 0.25) {
                    double nh = hue < 0 ? h : hue + (h - 100) * 0.35;       // keep the original's spread of hues, shifted
                    int rgb = java.awt.Color.HSBtoRGB((float) (Math.floorMod((int) nh, 360) / 360.0), (float) Math.min(1, hsb[1] * saturation), (float) Math.min(1, hsb[2] * value));
                    p = a << 24 | (rgb & 0xFFFFFF);
                }
                out.setRGB(x, y, p);
            }
        }
        return new Sprite(out, s.ax, s.ay, s.k);
    }

    private static Sprite log() {
        PixelCanvas c = new PixelCanvas(34, 14);
        c.rect(5, 3, 26, 8, BARK_M);
        c.rect(5, 3, 26, 2, BARK_L);
        c.rect(5, 9, 26, 2, BARK);
        for (int x = 9; x < 30; x += 5) { c.set(x, 6, BARK); c.set(x + 1, 7, BARK); c.set(x - 1, 5, BARK_D); }
        c.ellipse(5, 7, 3.6, 4.4, WOOD);
        c.ellipse(5, 7, 2.2, 2.8, WOOD_D);
        c.ellipse(5, 7, 1.0, 1.2, WOOD);
        c.disc(22, 3, 2.2, MOSS); c.disc(21, 2.4, 1.2, MOSS_L); c.set(25, 3, MOSS_D);
        c.outline(OUTLINE);
        return fine(c, 17, 12);
    }

    private static Sprite mushroomCluster() {
        PixelCanvas c = new PixelCanvas(20, 15);
        int stem = rgb(248, 228, 200), stemD = rgb(216, 168, 158);
        int[][] caps = {{5, 8, 4, 0}, {13, 7, 4, 1}, {9, 11, 3, 0}, {16, 11, 2, 1}};
        for (int[] m : caps) {
            int x = m[0], y = m[1], r = m[2];
            int cap = m[3] == 0 ? rgb(214, 61, 78) : rgb(206, 148, 82), capD = m[3] == 0 ? rgb(153, 24, 62) : rgb(150, 98, 52);
            c.rect(x - 1, y, 2, 14 - y, stem); c.set(x, y + 1, stemD);
            c.ellipseY(x, y, r, r * 0.8, cap, 0, y + 0.5);
            c.ellipseY(x, y, r, r * 0.8, capD, y - 0.6, y + 0.5);
            c.set(x - 1, (int) (y - r * 0.5), lighten(cap, 0.35));
        }
        c.outline(OUTLINE);
        return fine(c, 10, 14);
    }

    // ------------------------------------------------------------------ for ThemeArt

    /** A texture tiled at the drawn sprites' pixel size, anchored at the world's origin. */
    static TexturePaint paint(PixelCanvas c) {
        BufferedImage big = new BufferedImage(c.w * PX, c.h * PX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(c.image(), 0, 0, big.getWidth(), big.getHeight(), null);
        g.dispose();
        return new TexturePaint(big, new Rectangle(0, 0, big.getWidth(), big.getHeight()));
    }
}
