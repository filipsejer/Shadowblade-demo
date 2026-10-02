package game;

import static game.PixelCanvas.*;

import java.util.Map;
import java.util.Random;

/**
 * Stormcliff, painted at the drawn sprites' pixel size like the forest and the city ({@link ForestArt},
 * {@link CityArt}): wet slate flagstones shining in the rain, the atrium's cream and teal tiles, iron gratings over
 * the gaps between the buildings, the storm-tossed sea far below, and the cliff's dark sea wall. Along the tops of
 * the walks stand the laboratory's buildings: brick wings with tall lit windows, stone towers wound with copper pipe,
 * glass conservatories and corrugated machine houses, every roof bristling with lightning rods. The machines and
 * things standing about (tesla coils, specimen tanks, the orrery), Copper, the stasis engine and the Blight's vine
 * walls are shaded and outlined by a {@link Doll}.
 */
final class LabArt {
    private LabArt() {}

    private static final double K = ForestArt.K;
    private static final int OUTLINE = Doll.OUTLINE;

    // ------------------------------------------------------------------ palette

    private static final int[] SLATE = {rgb(16, 20, 30), rgb(40, 50, 64), rgb(54, 66, 82), rgb(70, 84, 102), rgb(98, 116, 136)};
    private static final int[] SHEEN = {rgb(120, 150, 180), rgb(170, 200, 228)};
    private static final int[] CREAM = {rgb(120, 108, 92), rgb(196, 186, 162), rgb(214, 206, 184), rgb(230, 224, 204), rgb(246, 242, 228)};
    private static final int[] TEAL = {rgb(10, 40, 44), rgb(22, 82, 86), rgb(34, 116, 116), rgb(64, 156, 148), rgb(120, 200, 186)};
    private static final int[] IRON = {rgb(10, 12, 18), rgb(30, 34, 44), rgb(46, 52, 64), rgb(66, 72, 86), rgb(96, 104, 120)};
    private static final int[] SEA = {rgb(6, 14, 24), rgb(10, 24, 38), rgb(16, 36, 54), rgb(26, 54, 74), rgb(48, 84, 104)};
    private static final int FOAM = rgb(170, 196, 210), FOAM_L = rgb(220, 236, 244);
    private static final int[] BASALT = {rgb(12, 16, 20), rgb(36, 44, 50), rgb(50, 60, 66), rgb(66, 78, 84), rgb(88, 102, 108)};
    private static final int MOSS = rgb(52, 78, 50), MOSS_L = rgb(78, 106, 62);
    private static final int GLOW_GREEN = rgb(120, 255, 170), GLOW_GOLD = rgb(255, 220, 120), BOLT = rgb(210, 236, 255);

    // ------------------------------------------------------------------ materials

    static final Doll.Mat BRICK_DARK = new Doll.Mat(rgb(30, 12, 12), rgb(70, 32, 30), rgb(96, 46, 40), rgb(122, 64, 52), rgb(150, 88, 70), false);
    static final Doll.Mat STONE_LAB = new Doll.Mat(rgb(20, 26, 30), rgb(62, 72, 78), rgb(84, 96, 102), rgb(108, 120, 126), rgb(140, 152, 158), false);
    static final Doll.Mat VERDIGRIS = new Doll.Mat(rgb(10, 40, 36), rgb(38, 96, 84), rgb(62, 134, 114), rgb(96, 172, 146), rgb(146, 210, 184), false);
    static final Doll.Mat COPPER = CityArt.COPPER;
    static final Doll.Mat BRASS = new Doll.Mat(rgb(70, 46, 12), rgb(150, 108, 34), rgb(200, 156, 60), rgb(232, 196, 100), rgb(255, 236, 170), true);
    static final Doll.Mat IRON_M = CityArt.IRON;
    static final Doll.Mat WIN_GREEN = new Doll.Mat(rgb(20, 70, 50), rgb(60, 170, 120), rgb(110, 220, 160), rgb(170, 248, 200), rgb(230, 255, 236), false);
    static final Doll.Mat WIN_DARK = CityArt.WIN_DARK;
    static final Doll.Mat WIN_LIT = CityArt.WIN_LIT;
    static final Doll.Mat GLASS = new Doll.Mat(rgb(30, 60, 70), rgb(80, 130, 140), rgb(130, 180, 186), rgb(190, 226, 230), rgb(240, 255, 255), true);
    static final Doll.Mat FLUID = new Doll.Mat(rgb(10, 60, 30), rgb(40, 150, 80), rgb(80, 210, 120), rgb(150, 250, 170), rgb(220, 255, 230), true);
    static final Doll.Mat FROST = new Doll.Mat(rgb(20, 50, 90), rgb(70, 140, 210), rgb(130, 196, 250), rgb(196, 234, 255), rgb(250, 255, 255), true);
    static final Doll.Mat CORRUGATED = new Doll.Mat(rgb(22, 26, 30), rgb(70, 78, 82), rgb(96, 104, 106), rgb(126, 134, 134), rgb(160, 168, 166), false);
    static final Doll.Mat FRAME_WHITE = new Doll.Mat(rgb(70, 76, 82), rgb(170, 178, 182), rgb(208, 214, 216), rgb(232, 236, 236), rgb(255, 255, 255), false);
    static final Doll.Mat LEAF = new Doll.Mat(rgb(8, 30, 24), rgb(22, 70, 50), rgb(36, 104, 66), rgb(70, 146, 84), rgb(120, 190, 110), true);
    static final Doll.Mat STARBLOOM = new Doll.Mat(rgb(120, 70, 10), rgb(230, 170, 50), rgb(255, 214, 100), rgb(255, 240, 170), rgb(255, 255, 230), true);
    static final Doll.Mat VINE = CityArt.BLIGHT;
    static final Doll.Mat WOOD = Doll.WOOD;
    static final Doll.Mat PAPER = CityArt.CREAM;

    // ------------------------------------------------------------------ the ground (seamless textures)

    /** Voronoi stones over an n-pixel seamless square: who owns each pixel, and whether it's in the gap between two. */
    private record Stones(int[] owner, boolean[] gap, int count, double[] sx, double[] sy) {}

    private static Stones stones(int n, int cells, long seed, double gapWidth, double squash) {
        Random r = new Random(seed);
        double cell = n / (double) cells;
        int count = cells * cells;
        double[] sx = new double[count], sy = new double[count];
        for (int j = 0; j < cells; j++) for (int i = 0; i < cells; i++) {
            sx[j * cells + i] = (i + 0.2 + r.nextDouble() * 0.6) * cell;
            sy[j * cells + i] = (j + 0.2 + r.nextDouble() * 0.6) * cell;
        }
        int[] owner = new int[n * n];
        boolean[] gap = new boolean[n * n];
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            double best = 1e9, second = 1e9;
            int who = 0;
            for (int k = 0; k < count; k++) {
                double dx = Math.abs(x + 0.5 - sx[k]), dy = Math.abs(y + 0.5 - sy[k]);
                dx = Math.min(dx, n - dx);
                dy = Math.min(dy, n - dy);
                double d = dx * dx + dy * dy * squash;
                if (d < best) { second = best; best = d; who = k; } else if (d < second) second = d;
            }
            owner[y * n + x] = who;
            gap[y * n + x] = Math.sqrt(second) - Math.sqrt(best) < gapWidth;
        }
        return new Stones(owner, gap, count, sx, sy);
    }

    /**
     * Out in the rain: big slate flagstones, dark and wet, each its own shade, with the storm's light caught along
     * their top edges and in the puddles between them. 96 px, seamless.
     */
    static PixelCanvas slate() {
        int n = 96;
        Stones st = stones(n, 7, 201, 1.3, 1.4);
        Random r = new Random(202);
        double[] f = ForestArt.field(n, 203, 2);
        int[] tones = new int[st.count()];
        for (int k = 0; k < tones.length; k++) tones[k] = r.nextInt(4) == 0 ? 1 : r.nextInt(4) == 0 ? 3 : 2;
        PixelCanvas c = new PixelCanvas(n, n);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int i = y * n + x;
            if (st.gap()[i]) { c.set(x, y, SLATE[0]); continue; }
            int s = tones[st.owner()[i]];
            boolean top = st.gap()[Math.floorMod(y - 1, n) * n + x], left = st.gap()[y * n + Math.floorMod(x - 1, n)];
            boolean bottom = st.gap()[Math.floorMod(y + 1, n) * n + x];
            if (top || left && ForestArt.bayer(x, y) > 0.4) s = Math.min(4, s + 1);
            if (bottom) s = Math.max(1, s - 1);
            double v = f[i] + (ForestArt.bayer(x, y) - 0.5) * 0.16;
            if (s > 1 && v < 0.34) s--;
            c.set(x, y, SLATE[s]);
        }
        for (int k = 0; k < 26; k++) {                                       // rain sheen: short bright streaks along the stones
            int x = r.nextInt(n), y = r.nextInt(n), len = 2 + r.nextInt(4);
            if (st.gap()[y * n + x]) continue;
            for (int j = 0; j < len; j++) {
                int xx = Math.floorMod(x + j, n);
                if (!st.gap()[y * n + xx] && ForestArt.stepOf(SLATE, c.get(xx, y)) >= 2) c.set(xx, y, j == 0 ? SHEEN[1] : SHEEN[0]);
            }
        }
        for (int k = 0; k < 6; k++) {                                        // puddles pooled in the joints
            int x = r.nextInt(n), y = r.nextInt(n), w = 4 + r.nextInt(5);
            for (int dx = 0; dx < w; dx++) for (int dy = 0; dy < 2; dy++) {
                int xx = Math.floorMod(x + dx, n), yy = Math.floorMod(y + dy, n);
                c.set(xx, yy, dy == 0 && dx > 0 && dx < w - 1 ? rgb(70, 100, 132) : rgb(34, 50, 72));
            }
            c.set(Math.floorMod(x + 1, n), y, SHEEN[1]);
        }
        return c;
    }

    /**
     * Indoors: the laboratory's floor of cream tiles, a small teal tile at every other crossing, worn here and there.
     * 96 px, seamless.
     */
    static PixelCanvas tiles() {
        int n = 96, t = 12;
        Random r = new Random(211);
        PixelCanvas c = new PixelCanvas(n, n);
        int[] tone = new int[(n / t) * (n / t)];
        for (int k = 0; k < tone.length; k++) tone[k] = r.nextInt(6) == 0 ? 2 : r.nextInt(5) == 0 ? 4 : 3;
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int tx = x / t, ty = y / t, xx = x % t, yy = y % t;
            int s = tone[ty * (n / t) + tx];
            int col;
            if (xx == t - 1 || yy == t - 1) col = CREAM[0];                          // the grout
            else if (yy == 0 || xx == 0) col = CREAM[Math.min(4, s + 1)];
            else if (yy == t - 2) col = CREAM[Math.max(1, s - 1)];
            else col = CREAM[ForestArt.bayer(x, y) > 0.94 ? s - 1 : s];
            c.set(x, y, col);
        }
        for (int ty = 0; ty < n / t; ty += 2) for (int tx = 0; tx < n / t; tx += 2) {   // small teal diamonds where the tiles meet
            int cx = tx * t + t - 1, cy = ty * t + t - 1;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) > 2) continue;
                int col = Math.abs(dx) + Math.abs(dy) == 2 ? TEAL[0] : dx + dy < 0 ? TEAL[3] : TEAL[2];
                c.set(Math.floorMod(cx + dx, n), Math.floorMod(cy + dy, n), col);
            }
        }
        for (int k = 0; k < 90; k++) {                                              // scuffs and wear
            int x = r.nextInt(n), y = r.nextInt(n);
            int s = ForestArt.stepOf(CREAM, c.get(x, y));
            if (s >= 2) c.set(x, y, CREAM[s - 1]);
        }
        return c;
    }

    /** The walks between the buildings: an iron grating with copper bolts, the dark gap showing through it. 32 px, seamless. */
    static PixelCanvas grating() {
        int n = 32;
        PixelCanvas c = new PixelCanvas(n, n);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int xx = x % 8, yy = y % 4;
            int col;
            if (yy == 3) col = rgb(6, 12, 18);                                       // the gaps between the bars
            else if (xx == 7) col = IRON[1];
            else col = yy == 0 ? IRON[4] : yy == 1 ? IRON[3] : IRON[2];
            c.set(x, y, col);
        }
        for (int y = 0; y < n; y += 16) for (int x = 0; x < n; x++) {               // the frame's crossbeams
            c.set(x, y + 7, IRON[1]);
            c.set(x, y + 8, IRON[3]);
        }
        for (int[] p : new int[][]{{3, 8}, {19, 8}, {11, 24}, {27, 24}}) {        // copper bolts
            c.set(p[0], p[1], rgb(230, 152, 84));
            c.set(p[0] + 1, p[1], rgb(150, 74, 30));
        }
        return c;
    }

    /**
     * Beyond the walls, far below: the storm-tossed sea, dark swells driven by the wind, streaked with foam and broken
     * by whitecaps. 128 px, seamless.
     */
    static PixelCanvas sea() {
        int n = 128;
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = ForestArt.field(n, 221, 3);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            double swell = 0.5 + 0.5 * Math.sin((x * 0.6 + y * 1.0) * Math.PI * 2 / 32.0 + f[y * n + x] * 3.5);
            double v = f[y * n + x] * 0.55 + swell * 0.45;
            c.set(x, y, ForestArt.tone(SEA, 0, 3, v, x, y, 0.9));
        }
        Random r = new Random(222);
        for (int k = 0; k < 70; k++) {                                       // foam streaks, blown along the wind
            int x = r.nextInt(n), y = r.nextInt(n), len = 3 + r.nextInt(7);
            for (int i = 0; i < len; i++) {
                int xx = Math.floorMod(x + i, n), yy = Math.floorMod(y + i / 3, n);
                c.set(xx, yy, i == 0 || i == len - 1 ? SEA[4] : FOAM);
            }
        }
        for (int k = 0; k < 14; k++) {                                       // whitecaps
            int x = r.nextInt(n), y = r.nextInt(n);
            c.set(x, y, FOAM_L);
            c.set(Math.floorMod(x + 1, n), y, FOAM_L);
            c.set(Math.floorMod(x - 1, n), Math.floorMod(y + 1, n), FOAM);
            c.set(Math.floorMod(x + 2, n), Math.floorMod(y + 1, n), FOAM);
        }
        return c;
    }

    /** The edge of every walk: the cliff's sea wall, big blocks of dark basalt with moss in the joints. 64 px, seamless. */
    static PixelCanvas seaWall() {
        int n = 64, bw = 16, bh = 10;
        Random r = new Random(231);
        PixelCanvas c = new PixelCanvas(n, n);
        int rows = (n + bh - 1) / bh;
        for (int row = 0; row < rows; row++) {
            int off = row % 2 == 0 ? 0 : bw / 2;
            for (int b = 0; b < n / bw; b++) {
                int tone = 1 + r.nextInt(3);
                for (int yy = 0; yy < bh; yy++) for (int xx = 0; xx < bw; xx++) {
                    int px = Math.floorMod(b * bw + xx + off, n), py = row * bh + yy;
                    if (py >= n) continue;
                    int s = yy == bh - 1 || xx == bw - 1 ? 0 : yy == 0 || xx == 0 ? Math.min(4, tone + 1) : yy == bh - 2 ? Math.max(1, tone - 1) : tone;
                    if (s == tone && ForestArt.bayer(px, py) > 0.86) s = Math.max(1, s - 1);
                    c.set(px, py, BASALT[s]);
                }
            }
        }
        for (int k = 0; k < 50; k++) {                                       // moss creeping along the joints
            int x = r.nextInt(n), y = r.nextInt(n);
            if (ForestArt.stepOf(BASALT, c.get(x, y)) == 0) c.set(x, y, r.nextBoolean() ? MOSS : MOSS_L);
        }
        return c;
    }

    /** The breakers below the sea wall: white water churning against the rocks. 64 px, seamless. */
    static PixelCanvas surf() {
        int n = 64;
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = ForestArt.field(n, 241, 2);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) c.set(x, y, ForestArt.tone(SEA, 1, 4, f[y * n + x], x, y, 0.9));
        Random r = new Random(242);
        for (int k = 0; k < 90; k++) {
            int x = r.nextInt(n), y = r.nextInt(n), len = 2 + r.nextInt(5);
            for (int i = 0; i < len; i++) c.set(Math.floorMod(x + i, n), y, r.nextInt(3) == 0 ? FOAM_L : FOAM);
        }
        return c;
    }

    // ------------------------------------------------------------------ on the floor

    private static Sprite fine(PixelCanvas c, int ax, int ay) { return new Sprite(c.image(), ax, ay, K); }

    private static Sprite done(Doll d, int ax, int ay) { return new Sprite(d.render().image(), ax, ay, K); }

    /** Soft patches of wet shine and old lightning scorch, laid first to break up the ground's repeat. */
    static Sprite[] patches() {
        return new Sprite[]{sheen(50, 22, 251), sheen(64, 26, 252), sheen(40, 18, 253), scorch(34, 18, 254), sheen(56, 24, 255), scorch(28, 14, 256)};
    }

    private static Sprite sheen(int w, int h, long seed) {
        PixelCanvas c = new PixelCanvas(w, h);
        Random r = new Random(seed);
        double phase = r.nextDouble() * 6;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0);
            double a = Math.atan2(dy, dx), d = Math.hypot(dx, dy) / (1 + 0.18 * Math.sin(a * 3 + phase));
            if (d < 1 && (1 - d) * 1.8 > ForestArt.bayer(x, y)) c.set(x, y, rgba(30, 60, 110, 60));
        }
        for (int i = 0; i < 5; i++) c.set(w / 4 + r.nextInt(w / 2), h / 3 + r.nextInt(h / 3), rgba(190, 220, 255, 110));
        return fine(c, w / 2, h / 2);
    }

    private static Sprite scorch(int w, int h, long seed) {
        PixelCanvas c = new PixelCanvas(w, h);
        Random r = new Random(seed);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0), d = Math.hypot(dx, dy);
            if (d < 1 && (1 - d) * 2.4 > ForestArt.bayer(x, y)) c.set(x, y, rgba(8, 8, 14, 120));
        }
        for (int k = 0; k < 7; k++) {                                       // the strike's branching scar
            double a = r.nextDouble() * Math.PI * 2;
            for (int i = 2; i < w / 2; i++) {
                int x = (int) (w / 2 + Math.cos(a) * i), y = (int) (h / 2 + Math.sin(a) * i * h / w);
                if (x >= 0 && y >= 0 && x < w && y < h) c.set(x, y, rgba(4, 4, 8, 170));
                a += (r.nextDouble() - 0.5) * 0.5;
            }
        }
        return fine(c, w / 2, h / 2);
    }

    /** Little things lying about (anchored at the bottom middle). */
    static Sprite[] floor() {
        return new Sprite[]{puddle(20, 7), puddle(28, 9), puddle(16, 6), cable(), glass(), sprout(), plate(), note(), flask(), rivets()};
    }

    private static Sprite puddle(int w, int h) {
        PixelCanvas c = new PixelCanvas(w, h);
        c.ellipse(w / 2.0, h / 2.0, w / 2.0 - 0.5, h / 2.0 - 0.5, rgb(24, 38, 60));
        c.ellipse(w / 2.0 - 1, h / 2.0 - 0.6, w / 2.0 - 2.5, h / 2.0 - 1.8, rgb(40, 66, 100));
        c.rect(w / 3, h / 2 - 1, 4, 1, BOLT); c.set(w / 3 + 4, h / 2 - 1, rgb(130, 170, 220));      // the sky's reflection
        c.set(w * 2 / 3, h / 2 + 1, rgb(120, 160, 210));
        return fine(c, w / 2, h - 1);
    }

    private static Sprite cable() {
        PixelCanvas c = new PixelCanvas(26, 8);
        int prev = 4;
        for (int x = 1; x < 25; x++) {
            int y = (int) Math.round(4 + Math.sin(x * 0.45) * 2.2);
            c.set(x, y, rgb(18, 20, 26)); c.set(x, y - 1, rgb(54, 58, 70));
            if (Math.abs(y - prev) > 1) c.set(x, (y + prev) / 2, rgb(18, 20, 26));
            prev = y;
        }
        c.rect(0, 3, 2, 3, rgb(196, 110, 52)); c.set(0, 3, rgb(255, 204, 140));
        c.rect(24, 3, 2, 3, rgb(196, 110, 52));
        return fine(c, 13, 7);
    }

    private static Sprite glass() {
        PixelCanvas c = new PixelCanvas(12, 6);
        int[][] at = {{1, 3}, {4, 1}, {7, 4}, {9, 2}};
        for (int[] p : at) { c.set(p[0], p[1], rgb(170, 220, 230)); c.set(p[0] + 1, p[1], rgb(90, 140, 156)); }
        c.set(5, 1, rgb(255, 255, 255));
        return fine(c, 6, 5);
    }

    /** A star-seed sprouting through a crack: a curl of stem and a bud that glows. */
    private static Sprite sprout() {
        PixelCanvas c = new PixelCanvas(9, 9);
        c.line(4, 8, 4, 4, rgb(36, 104, 66)); c.set(3, 5, rgb(70, 146, 84)); c.set(5, 6, rgb(70, 146, 84)); c.set(6, 5, rgb(36, 104, 66));
        c.set(4, 3, GLOW_GOLD); c.set(3, 3, rgb(230, 170, 50)); c.set(5, 3, rgb(230, 170, 50)); c.set(4, 2, rgb(255, 250, 220));
        return fine(c, 4, 8);
    }

    private static Sprite plate() {
        PixelCanvas c = new PixelCanvas(14, 8);
        c.rect(0, 0, 14, 8, OUTLINE);
        c.rect(1, 1, 12, 6, IRON[3]); c.rect(1, 1, 12, 1, IRON[4]); c.rect(1, 6, 12, 1, IRON[2]);
        for (int[] p : new int[][]{{2, 2}, {11, 2}, {2, 5}, {11, 5}}) c.set(p[0], p[1], rgb(230, 152, 84));
        return fine(c, 7, 7);
    }

    private static Sprite note() {
        PixelCanvas c = new PixelCanvas(9, 7);
        c.rect(1, 1, 7, 5, rgb(214, 210, 190)); c.rect(1, 1, 7, 1, rgb(236, 232, 216)); c.set(7, 5, rgb(170, 166, 150));
        c.rect(2, 2, 4, 1, rgb(60, 70, 120)); c.rect(2, 4, 5, 1, rgb(90, 100, 140)); c.set(6, 2, rgb(160, 40, 40));   // equations, one circled
        return fine(c, 4, 6);
    }

    private static Sprite flask() {
        PixelCanvas c = new PixelCanvas(14, 6);
        c.ellipse(9, 3.5, 4.5, 1.8, rgb(60, 170, 100));                       // the spill
        c.ellipse(9, 3.2, 3, 1, rgb(110, 220, 150));
        c.rect(1, 2, 5, 3, rgb(150, 200, 210)); c.rect(0, 3, 1, 1, rgb(150, 200, 210)); c.rect(2, 2, 3, 1, rgb(230, 250, 255));   // the flask, on its side
        c.set(6, 3, rgb(110, 220, 150));
        return fine(c, 7, 5);
    }

    private static Sprite rivets() {
        PixelCanvas c = new PixelCanvas(10, 4);
        for (int i = 0; i < 4; i++) { c.set(1 + i * 2 + i % 2, 1 + i % 2, IRON[4]); c.set(2 + i * 2 + i % 2, 2 + i % 2, IRON[1]); }
        return fine(c, 5, 3);
    }

    // ------------------------------------------------------------------ the laboratory's buildings, along the walks

    /** Building fronts, to line the top edge of every walk (anchored at the bottom middle). */
    static Sprite[] facades() {
        return new Sprite[]{
            building(0, 80, 128, 1), building(1, 64, 134, 2), building(2, 88, 112, 3), building(3, 76, 120, 4),
            building(0, 72, 118, 5), building(2, 80, 124, 6), building(1, 68, 124, 7), building(3, 84, 112, 8),
            building(0, 76, 76, 9), building(3, 80, 72, 10), building(2, 72, 74, 11),               // one-storey sheds and porches
        };
    }

    /**
     * A building {@code w} by {@code h} fine pixels. style 0: a dark brick wing with tall arched windows lit green;
     * 1: a stone tower wound with a copper pipe, a round window up top; 2: a glass conservatory in white iron, plants
     * inside; 3: a corrugated machine house with a riveted door, a big gauge and a chimney stack. Every roof has rods.
     */
    private static Sprite building(int style, int w, int h, long seed) {
        Random r = new Random(seed * 9341L);
        Doll d = new Doll(w, h);
        int roofTop = 10, roofH = style == 2 ? 14 : 12 + r.nextInt(5);
        int wallTop = roofTop + roofH;
        // lightning rods on the roof
        for (int i = 0; i < (w > 74 ? 2 : 1); i++) {
            int rx = i == 0 ? 10 + r.nextInt(10) : w - 14 - r.nextInt(8);
            d.piece(IRON_M).rect(rx, 1, 2, roofTop + 4);
            d.piece(COPPER).tri(rx - 1, 3, rx + 1, -1, rx + 3, 3);
            d.piece(GLASS).disc(rx + 1, roofTop - 1, 1.6);
        }
        // the roof
        if (style == 2) {                                                   // a glass roof, ridged
            d.piece(FRAME_WHITE).poly(new int[]{6, w - 6, w, 0}, new int[]{roofTop, roofTop, wallTop, wallTop});
            d.piece(GLASS).poly(new int[]{8, w - 8, w - 3, 3}, new int[]{roofTop + 2, roofTop + 2, wallTop - 1, wallTop - 1});
            for (int x = 8; x < w - 4; x += 8) for (int y = roofTop + 2; y < wallTop; y++) d.dot(x + (y - roofTop) / 4 - 2, y, FRAME_WHITE, 2);
        } else if (style == 3) {                                            // a flat tin roof and a chimney stack
            int cx = r.nextBoolean() ? 10 : w - 20;
            d.piece(BRICK_DARK).rect(cx, -2 + 2, 10, roofTop + 6);
            d.piece(IRON_M).rect(cx - 1, 0, 12, 3);
            d.piece(CORRUGATED).rect(0, roofTop + 4, w, roofH - 4);
            for (int x = 2; x < w; x += 3) for (int y = roofTop + 5; y < wallTop; y++) d.dot(x, y, CORRUGATED, 0);
        } else {                                                            // verdigris copper, sloping back
            d.piece(VERDIGRIS).poly(new int[]{6, w - 6, w, 0}, new int[]{roofTop, roofTop, wallTop, wallTop});
            for (int y = roofTop + 3; y < wallTop; y += 3) for (int x = 2; x < w - 2; x++) if ((x + y) % 4 == 0) d.dot(x, y, VERDIGRIS, 0);
            if (style == 1) {                                               // a little dome on the tower
                d.piece(VERDIGRIS).ellipseY(w / 2.0, roofTop + 2, 10, 9, roofTop - 7, roofTop + 3);
                d.piece(BRASS).rect(w / 2 - 1, roofTop - 11, 2, 5);
            }
        }
        // the wall
        Doll.Mat wall = switch (style) { case 1 -> STONE_LAB; case 2 -> FRAME_WHITE; case 3 -> CORRUGATED; default -> BRICK_DARK; };
        d.piece(wall).rect(0, wallTop, w, h - wallTop);
        if (style != 2) d.piece(STONE_LAB).rect(-1, wallTop, w + 2, 3);     // the cornice
        int ground = h - 40;
        if (style == 0) {                                                  // brick: mortar lines
            for (int y = wallTop + 5; y < h - 6; y += 4) for (int x = 1; x < w - 1; x++) if (x % 2 == 0) d.dot(x, y, BRICK_DARK, 0);
        } else if (style == 1) {                                           // stone courses
            for (int y = wallTop + 8; y < h - 6; y += 8) for (int x = 1; x < w - 1; x++) d.dot(x, y, STONE_LAB, 0);
        } else if (style == 3) {                                           // corrugations
            for (int x = 2; x < w - 1; x += 3) for (int y = wallTop + 3; y < h - 5; y++) d.dot(x, y, CORRUGATED, 0);
        }
        int floors = Math.max(0, (ground - wallTop - 4) / 30);
        if (style == 2) {                                                  // the conservatory: panes all the way down, plants inside
            d.piece(new Doll.Mat(rgb(10, 40, 30), rgb(30, 90, 66), rgb(50, 130, 90), rgb(90, 180, 120), rgb(160, 230, 180), false)).rect(3, wallTop + 2, w - 6, h - wallTop - 8);
            for (int i = 0; i < 6; i++) {                                   // leaves pressed against the glass
                int px = 6 + r.nextInt(w - 14), py = wallTop + 10 + r.nextInt(Math.max(1, h - wallTop - 30));
                d.piece(LEAF).disc(px, py, 4 + r.nextInt(4));
                if (r.nextInt(3) == 0) d.dot(px, py - 2, STARBLOOM.light());
            }
            for (int x = 3; x < w - 3; x += 10) d.piece(FRAME_WHITE).rect(x, wallTop + 2, 2, h - wallTop - 8);
            for (int y = wallTop + 14; y < h - 8; y += 16) d.piece(FRAME_WHITE).rect(3, y, w - 6, 2);
            d.piece(FRAME_WHITE).rect(w / 2 - 8, h - 30, 16, 25);           // the door
            d.piece(GLASS).rect(w / 2 - 6, h - 28, 5, 21).rect(w / 2 + 1, h - 28, 5, 21);
        } else {
            int cols = Math.max(2, (w - 8) / 22);
            double gapX = (w - cols * 11) / (cols + 1.0);
            for (int f = 0; f < floors; f++) {
                int wy = wallTop + 8 + f * 30;
                for (int i = 0; i < cols; i++) {
                    int wx = (int) Math.round(gapX + i * (11 + gapX));
                    if (style == 1 && f == 0 && i == cols / 2) {            // the tower's round window
                        d.piece(BRASS).disc(w / 2.0, wy + 9, 8);
                        d.piece(r.nextBoolean() ? WIN_GREEN : WIN_LIT).disc(w / 2.0, wy + 9, 6);
                        for (int k = -5; k <= 5; k++) { d.dot(w / 2 + k, wy + 9, BRASS.dark()); d.dot(w / 2, wy + 9 + k, BRASS.dark()); }
                        continue;
                    }
                    if (style == 1) continue;
                    archWindow(d, wx, wy, 11, 20, r.nextInt(5) < 3 ? (r.nextInt(3) == 0 ? WIN_LIT : WIN_GREEN) : WIN_DARK);
                }
            }
            if (style == 1) {                                               // a copper pipe winding up the tower
                d.piece(COPPER).rect(w - 12, wallTop + 4, 4, h - wallTop - 10);
                for (int y = wallTop + 10; y < h - 8; y += 14) d.piece(BRASS).rect(w - 13, y, 6, 2);
                d.piece(COPPER).rect(w - 26, h - 30, 18, 4);
                d.piece(IRON_M).disc(w - 30, h - 28, 4);                   // and its valve wheel
                d.piece(CityArt.HYDRANT).disc(w - 30, h - 28, 2);
            }
            // the ground floor: a riveted iron door with a lamp over it
            int doorX = style == 3 ? w / 2 - 10 : r.nextBoolean() ? 7 : w - 25;
            int doorW = style == 3 ? 20 : 16;
            d.piece(STONE_LAB).rect(doorX - 2, ground + 6, doorW + 4, h - ground - 6);
            d.piece(IRON_M).rect(doorX, ground + 10, doorW, h - ground - 13);
            for (int y = ground + 13; y < h - 5; y += 5) { d.dot(doorX + 2, y, COPPER.light()); d.dot(doorX + doorW - 3, y, COPPER.light()); }
            if (style == 3) for (int y = ground + 12; y < h - 4; y++) d.dot(doorX + doorW / 2, y, IRON_M, 0);
            d.piece(WIN_GREEN).rect(doorX + doorW / 2 - 3, ground + 3, 6, 4);   // a green lamp over it
            d.piece(IRON_M).rect(doorX + doorW / 2 - 4, ground + 2, 8, 1);
            if (style == 3) {                                               // a big pressure gauge by the door
                int gx = doorX < w / 2 ? doorX + doorW + 12 : doorX - 12;
                d.piece(BRASS).disc(gx, ground + 18, 7);
                d.piece(PAPER).disc(gx, ground + 18, 5);
                d.dot(gx, ground + 18, OUTLINE); d.dot(gx + 1, ground + 17, CityArt.HYDRANT.mid()); d.dot(gx + 2, ground + 16, CityArt.HYDRANT.mid());
            } else if (floors == 0) {
                int wx = doorX < w / 2 ? w - 22 : 8;
                archWindow(d, wx, ground + 8, 12, 20, WIN_GREEN);
            }
            d.piece(STONE_LAB).rect(0, h - 5, w, 5);                        // the plinth
        }
        int pipe = r.nextBoolean() ? 1 : w - 3;                             // a downpipe, rain running off it
        if (style != 1) {
            d.piece(IRON_M).rect(pipe, wallTop + 2, 2, h - wallTop - 6);
            d.dot(pipe, h - 3, SHEEN[1]);
        }
        return new Sprite(d.render().image(), w / 2, h - 1, K);
    }

    /** A tall window with a round top, its glass lit (or dark), and a glazing bar down the middle. */
    private static void archWindow(Doll d, int x, int y, int ww, int wh, Doll.Mat glass) {
        d.piece(STONE_LAB).rect(x - 1, y + ww / 2, ww + 2, wh - ww / 2 + 2);
        d.join(STONE_LAB).ellipseY(x + ww / 2.0, y + ww / 2.0 + 1, ww / 2.0 + 1, ww / 2.0 + 1, y - 1, y + ww / 2.0 + 1);
        d.piece(glass).rect(x + 1, y + ww / 2, ww - 2, wh - ww / 2);
        d.join(glass).ellipseY(x + ww / 2.0, y + ww / 2.0 + 1, ww / 2.0 - 1, ww / 2.0 - 1, y + 1, y + ww / 2.0 + 1);
        for (int yy = y + 2; yy < y + wh; yy++) d.dot(x + ww / 2, yy, glass.line());
        for (int xx = x + 1; xx < x + ww - 1; xx++) d.dot(xx, y + wh / 2 + 2, glass.line());
        d.dot(x + 2, y + ww / 2, glass.high());
    }

    /** Things standing at the foot of the walls and between the buildings (anchored at the bottom middle). */
    static Sprite[] low() {
        return new Sprite[]{coil(true), valve(), cylinders(), rock(), CityArt.crates(), tank(true)};
    }

    // ------------------------------------------------------------------ scenery that stands about

    static void register(Map<String, Sprite[]> m) {
        m.put("landmark.orrery", new Sprite[]{orrery()});
        m.put("landmark.coil", new Sprite[]{coil(false)});
        m.put("landmark.rod", new Sprite[]{rod()});
        m.put("landmark.tank", new Sprite[]{tank(false)});
        m.put("landmark.bench.lab", new Sprite[]{bench()});
        m.put("landmark.planter.star", new Sprite[]{starPlanter()});
        m.put("landmark.cables", new Sprite[]{cables()});
        m.put("landmark.telescope", new Sprite[]{telescope()});
        m.put("ward.copper", new Sprite[]{copper(0, 0), copper(0, 1)});
        m.put("ward.copper.roll", new Sprite[]{copper(1, 0), copper(1, 1)});
        m.put("ward.copper.work", new Sprite[]{copper(2, 0), copper(2, 1)});
        m.put("ward.copper.broken", new Sprite[]{copper(3, 0)});
        m.put("lab.copper.idle", new Sprite[]{copper(0, 0), copper(0, 1)});
        m.put("ward.engine", new Sprite[]{engine(false, 0), engine(false, 1), engine(false, 2)});
        m.put("ward.engine.broken", new Sprite[]{engine(true, 0)});
        m.put("ward.vines", new Sprite[]{vines(0), vines(1)});
    }

    /**
     * The atrium's centrepiece: a great brass orrery on a stone plinth, its rings tilted round a lamp that stands for
     * the sun, little planets riding them.
     */
    static Sprite orrery() {
        Doll d = new Doll(76, 92);
        d.piece(STONE_LAB).rect(14, 74, 48, 14);                             // the plinth
        d.piece(STONE_LAB).rect(10, 72, 56, 4);
        d.piece(STONE_LAB).rect(12, 86, 52, 4);
        d.piece(BRASS).rect(35, 34, 6, 40);                                  // the column
        d.piece(BRASS).rect(31, 66, 14, 6);
        for (int y = 40; y < 66; y += 6) d.dot(37, y, BRASS.high());
        // the rings: tilted ellipses of brass wire
        double[][] rings = {{38, 30, 34, 10}, {38, 30, 25, 15}, {38, 30, 16, 6}};
        for (double[] rg : rings) {
            for (int i = 0; i < 160; i++) {
                double a = i * Math.PI * 2 / 160;
                int x = (int) Math.round(rg[0] + Math.cos(a) * rg[2]), y = (int) Math.round(rg[1] + Math.sin(a) * rg[3] - Math.cos(a) * rg[3] * 0.25);
                d.piece(BRASS).set(x, y);
            }
        }
        d.piece(STARBLOOM).disc(38, 30, 6);                                  // the sun: a lamp, burning gold
        d.dot(36, 28, STARBLOOM.high()); d.dot(37, 27, STARBLOOM.high());
        d.piece(TEAL_M).disc(70, 32, 3);                                     // the planets
        d.piece(CityArt.HYDRANT).disc(22, 40, 2.5);
        d.piece(STONE_LAB).disc(52, 24, 2.2);
        d.piece(FROST).disc(9, 27, 3.2);
        d.piece(BRASS).disc(48, 43, 2);
        return done(d, 38, 89);
    }

    private static final Doll.Mat TEAL_M = new Doll.Mat(rgb(8, 34, 40), rgb(22, 82, 86), rgb(34, 116, 116), rgb(64, 156, 148), rgb(120, 200, 186), true);

    /**
     * A tesla coil: a stone footing, a tall copper coil, a steel ring and ball on top, crackling with violet arcs.
     * {@code small}: a short one, to stand at the foot of a wall.
     */
    static Sprite coil(boolean small) {
        int h = small ? 44 : 64;
        Doll d = new Doll(30, h);
        int baseY = h - 10;
        d.piece(STONE_LAB).rect(5, baseY, 20, 9);
        d.piece(STONE_LAB).rect(3, baseY + 6, 24, 4);
        int top = small ? 16 : 20;
        d.piece(COPPER).rect(10, top, 10, baseY - top);                       // the coil
        for (int y = top + 1; y < baseY; y += 2) for (int x = 10; x < 20; x++) d.dot(x, y, x < 13 ? COPPER.high() : COPPER.dark());
        d.piece(Doll.STEEL).ellipse(15, top - 2, 10, 3.5);                     // the ring
        d.piece(Doll.STEEL).disc(15, top - 8, 5);                              // the ball
        d.dot(13, top - 10, Doll.STEEL.high());
        int arc = rgb(220, 180, 255);
        for (int[] p : new int[][]{{4, top - 4}, {3, top - 6}, {2, top - 5}, {25, top - 9}, {26, top - 7}, {27, top - 8}, {21, top - 14}, {22, top - 16}}) d.dot(p[0], p[1], arc);
        return done(d, 15, h - 1);
    }

    /** A lightning rod: a tall iron mast with a copper spike, glass insulators, and a cable running down into the ground. */
    static Sprite rod() {
        Doll d = new Doll(18, 92);
        d.piece(STONE_LAB).rect(3, 82, 12, 9);
        d.piece(IRON_M).rect(8, 10, 3, 74);
        d.piece(COPPER).tri(7, 12, 9, 0, 12, 12);
        for (int y : new int[]{30, 52}) d.piece(GLASS).rect(5, y, 9, 3);
        d.piece(COPPER).rect(12, 30, 1, 54);                                   // the down conductor
        d.dot(8, 4, rgb(255, 230, 200));
        return done(d, 9, 90);
    }

    /** A specimen tank: brass base and cap, green fluid behind the glass, bubbles, and something curled up inside. */
    static Sprite tank(boolean small) {
        int h = small ? 40 : 58, w = small ? 22 : 30;
        Doll d = new Doll(w, h);
        d.piece(BRASS).rect(1, h - 9, w - 2, 8);                               // the base
        d.piece(BRASS).rect(3, 2, w - 6, 5);                                   // the cap, a pipe out of it
        d.piece(COPPER).rect(w / 2 - 1, 0, 3, 3);
        d.piece(FLUID).rect(3, 7, w - 6, h - 16);                              // the fluid
        d.piece(new Doll.Mat(rgb(6, 30, 18), rgb(16, 60, 36), rgb(24, 80, 46), rgb(34, 100, 56), rgb(60, 130, 80), true))   // what's in it
            .ellipse(w / 2.0, h / 2.0 + 2, w / 4.0, h / 5.0);
        d.dot(w / 2 - 1, h / 2, rgb(255, 230, 120));                           // an eye, open
        for (int i = 0; i < 4; i++) d.dot(5 + i * (w - 10) / 3, 10 + (i * 7) % (h - 24), FLUID.high());   // bubbles
        for (int y = 8; y < h - 10; y++) d.dot(4, y, GLASS.high());           // the glass's shine
        return done(d, w / 2, h - 1);
    }

    /** A workbench: flasks bubbling in three colours, a microscope, papers. */
    static Sprite bench() {
        Doll d = new Doll(46, 36);
        d.piece(WOOD).rect(2, 18, 42, 5);                                      // the top
        d.piece(WOOD).rect(4, 23, 3, 12).rect(39, 23, 3, 12);                  // legs
        d.piece(IRON_M).rect(6, 30, 34, 2);
        d.piece(PAPER).rect(26, 15, 10, 3);
        d.piece(FLUID).rect(5, 10, 5, 8);                                      // flasks
        d.piece(GLASS).rect(6, 6, 3, 4);
        d.piece(CityArt.HYDRANT).ellipse(15, 14, 3.4, 3.4);
        d.piece(GLASS).rect(14, 7, 2, 4);
        d.piece(new Doll.Mat(rgb(30, 20, 70), rgb(80, 60, 170), rgb(120, 100, 230), rgb(170, 150, 255), rgb(230, 220, 255), true)).rect(20, 11, 4, 7);
        d.piece(BRASS).rect(37, 5, 3, 13);                                     // the microscope
        d.piece(BRASS).rect(33, 15, 9, 3);
        d.piece(IRON_M).rect(36, 2, 4, 4);
        d.dot(16, 12, rgb(255, 230, 230)); d.dot(7, 12, FLUID.high());
        return done(d, 23, 35);
    }

    /** A stone planter with a star-plant in it: dark leaves, and blossoms that glow like little stars. */
    static Sprite starPlanter() {
        Doll d = new Doll(36, 52);
        d.piece(LEAF).disc(18, 22, 11);
        d.join(LEAF).disc(10, 28, 7).disc(26, 28, 7).disc(14, 14, 6).disc(23, 13, 6);
        for (int[] p : new int[][]{{9, 24}, {14, 11}, {22, 10}, {27, 25}, {18, 18}}) d.dot(p[0], p[1], LEAF, 3);
        for (int[] p : new int[][]{{12, 18}, {24, 20}, {18, 9}, {8, 30}, {28, 30}}) {   // the blossoms
            d.piece(STARBLOOM).tri(p[0] - 2, p[1], p[0], p[1] - 3, p[0] + 2, p[1]);
            d.join(STARBLOOM).tri(p[0] - 2, p[1] - 2, p[0], p[1] + 1, p[0] + 2, p[1] - 2);
            d.dot(p[0], p[1] - 1, STARBLOOM.high());
        }
        d.piece(STONE_LAB).rect(6, 38, 24, 12);                                // the planter
        d.piece(STONE_LAB).rect(4, 36, 28, 3);
        d.piece(WOOD).rect(7, 39, 22, 1);
        return done(d, 18, 50);
    }

    /** A bundle of cables snaking across the floor, from one machine to another. */
    static Sprite cables() {
        PixelCanvas c = new PixelCanvas(48, 14);
        int[] cols = {rgb(18, 20, 26), rgb(120, 30, 30), rgb(30, 50, 110)};
        for (int k = 0; k < 3; k++) {
            int prev = 6 + k * 2;
            for (int x = 1; x < 47; x++) {
                int y = (int) Math.round(6 + k * 2 + Math.sin(x * 0.2 + k * 1.4) * 3);
                c.set(x, y, cols[k]); c.set(x, y - 1, lighten(cols[k], 0.3));
                if (Math.abs(y - prev) > 1) c.set(x, (y + prev) / 2, cols[k]);
                prev = y;
            }
        }
        return fine(c, 24, 12);
    }

    /** A brass telescope on a wooden tripod, pointed up at the storm. */
    static Sprite telescope() {
        Doll d = new Doll(34, 56);
        d.piece(WOOD).thickLine(17, 30, 6, 54, 2).thickLine(17, 30, 28, 54, 2).thickLine(17, 30, 17, 54, 2);   // the tripod
        d.piece(BRASS).thickLine(6, 34, 28, 10, 5);                            // the tube
        d.piece(BRASS).thickLine(25, 13, 31, 6, 7);                            // its wide end
        d.piece(GLASS).disc(31, 6, 2.6);
        d.piece(IRON_M).disc(17, 26, 3);
        for (int i = 0; i < 3; i++) d.dot(10 + i * 6, 31 - i * 6, BRASS.high());
        return done(d, 17, 55);
    }

    /** A valve on a riser pipe, its red wheel, and a gauge. */
    private static Sprite valve() {
        Doll d = new Doll(26, 30);
        d.piece(COPPER).rect(10, 4, 6, 25);
        d.piece(COPPER).rect(2, 18, 22, 5);
        d.piece(CityArt.HYDRANT).disc(13, 8, 5);
        d.piece(IRON_M).disc(13, 8, 1.6);
        d.piece(BRASS).disc(21, 12, 3.2);
        d.dot(21, 12, OUTLINE);
        return done(d, 13, 29);
    }

    /** A rack of gas cylinders, chained to a post. */
    private static Sprite cylinders() {
        Doll d = new Doll(30, 40);
        Doll.Mat[] mats = {CityArt.HYDRANT, TEAL_M, CityArt.AWN_GOLD};
        for (int i = 0; i < 3; i++) {
            d.piece(mats[i]).rect(3 + i * 9, 8 + (i % 2) * 2, 7, 30 - (i % 2) * 2);
            d.join(mats[i]).ellipseY(6.5 + i * 9, 8 + (i % 2) * 2, 3.5, 3, 4 + (i % 2) * 2, 9 + (i % 2) * 2);
            d.piece(Doll.STEEL).rect(5 + i * 9, 3 + (i % 2) * 2, 3, 3);
        }
        d.piece(IRON_M).rect(1, 20, 28, 2);
        return done(d, 15, 39);
    }

    /** A wet black rock, with weed in its cracks. */
    private static Sprite rock() {
        Doll d = new Doll(30, 20);
        Doll.Mat basalt = new Doll.Mat(BASALT[0], BASALT[1], BASALT[2], BASALT[3], BASALT[4], true);
        d.piece(basalt).ellipse(15, 12, 13, 7.5);
        d.join(basalt).ellipse(9, 9, 6, 5).ellipse(20, 8, 7, 5);
        d.dot(8, 6, SHEEN[1]); d.dot(19, 5, SHEEN[1]); d.dot(20, 5, SHEEN[0]);
        d.dot(14, 15, MOSS_L); d.dot(15, 15, MOSS); d.dot(23, 14, MOSS);
        return done(d, 15, 19);
    }

    // ------------------------------------------------------------------ Copper, the engine, the vines

    /**
     * Copper, Ilse's surveyor robot: a riveted copper body on two treads, a round lamp of a head on a neck, a cutting
     * torch for one hand and a grabber for the other. {@code mode}: 0 standing, 1 rolling, 2 cutting (sparks at the
     * torch), 3 broken down (slumped, his lamp dark). {@code fr} animates.
     */
    static Sprite copper(int mode, int fr) {
        Doll d = new Doll(38, 40);
        boolean broken = mode == 3;
        int bob = mode == 1 ? fr : mode == 0 ? fr : 0;
        int slump = broken ? 3 : 0;
        // the treads
        d.piece(IRON_M).rect(4, 31, 30, 8);
        d.join(IRON_M).disc(5, 35, 4).disc(33, 35, 4);
        for (int x = 6; x < 33; x += 4) d.dot(x + (mode == 1 ? fr * 2 : 0), 32, IRON_M, 3);
        for (int x : new int[]{8, 15, 22, 29}) { d.piece(Doll.STEEL).disc(x, 35, 2); }
        // the body
        int by = 14 - bob + slump;
        d.piece(COPPER).rect(7, by, 24, 17);
        d.join(COPPER).ellipseY(19, by + 1, 12, 4, by - 2, by + 2);
        for (int[] p : new int[][]{{9, by + 2}, {28, by + 2}, {9, by + 13}, {28, by + 13}}) d.dot(p[0], p[1], COPPER.high());
        d.piece(BRASS).rect(13, by + 5, 12, 7);                               // his chest plate, a dial in it
        d.piece(PAPER).disc(19, by + 8.5, 2.6);
        d.dot(19, by + 8, OUTLINE); d.dot(20, by + 7, CityArt.HYDRANT.mid());
        // the arms: a cutting torch, and a grabber
        d.piece(COPPER).rect(31, by + 6, 4, 3);
        d.piece(IRON_M).rect(34, by + 5, 4, 5);
        d.piece(COPPER).rect(3, by + 6, 4, 3);
        d.piece(IRON_M).rect(0, by + 4, 3, 2).rect(0, by + 9, 3, 2);
        if (mode == 2) {                                                       // the torch, lit
            int[] sp = fr == 0 ? new int[]{37, by + 7, 37, by + 4, 36, by + 10} : new int[]{37, by + 6, 37, by + 9, 36, by + 3};
            d.dot(sp[0], sp[1], rgb(255, 255, 220)); d.dot(sp[2], sp[3], rgb(255, 200, 110)); d.dot(sp[4], sp[5], rgb(255, 170, 80));
        }
        // the neck and the head: a round lamp with a brass rim
        int hy = 6 - bob + slump * 2;
        d.piece(IRON_M).rect(17, hy + 6, 4, by - hy - 4);
        d.piece(BRASS).disc(broken ? 15 : 19, hy + 3, 6.5);
        d.piece(broken ? WIN_DARK : WIN_LIT).disc(broken ? 15 : 19, hy + 3, 4.2);
        if (!broken) { d.dot(17, hy + 1, WIN_LIT.high()); d.dot(18, hy + 1, WIN_LIT.high()); d.dot(20, hy + 3, WIN_LIT.light()); }
        d.piece(IRON_M).rect(broken ? 14 : 18, hy - 5, 1, 3);                  // the antenna, a bead on it
        d.dot(broken ? 14 : 18, hy - 6, broken ? IRON[3] : rgb(255, 120, 100));
        if (broken) for (int[] p : new int[][]{{24, 4}, {26, 2}, {25, 0}}) d.dot(p[0], p[1], rgb(110, 110, 120));   // a wisp of smoke
        return done(d, 19, 39);
    }

    /**
     * Ilse's stasis engine: a heavy iron frame on four feet, a tall glass column of blue cold swirling inside it,
     * copper coils round its waist, gauges, and frost spreading round its base. Broken, the glass goes dark and cracked.
     */
    static Sprite engine(boolean broken, int fr) {
        Doll d = new Doll(64, 92);
        d.piece(FROST).ellipse(32, 86, 30, 5);                                 // frost on the ground round it
        if (!broken) for (int i = 0; i < 9; i++) d.dot(6 + i * 6 + (i % 2) * 2, 84 + i % 3, FROST.high());
        d.piece(IRON_M).rect(8, 72, 48, 12);                                   // the base, on four feet
        d.join(IRON_M).rect(6, 82, 8, 6).rect(50, 82, 8, 6).rect(18, 82, 6, 6).rect(40, 82, 6, 6);
        for (int x = 11; x < 54; x += 6) d.dot(x, 74, COPPER.light());
        d.piece(IRON_M).rect(10, 6, 6, 68).rect(48, 6, 6, 68);                // the frame's uprights
        d.piece(IRON_M).rect(8, 2, 48, 8);                                     // its top
        d.piece(BRASS).rect(26, 0, 12, 4);
        Doll.Mat glow = broken ? new Doll.Mat(rgb(10, 14, 24), rgb(24, 30, 44), rgb(34, 42, 58), rgb(50, 60, 78), rgb(80, 92, 110), true) : FROST;
        d.piece(glow).rect(18, 10, 28, 62);                                    // the glass column and the cold in it
        if (!broken) {
            for (int y = 12; y < 70; y++) {                                    // swirls, turning
                int x = 32 + (int) Math.round(Math.sin((y + fr * 5) * 0.32) * 9);
                d.dot(x, y, FROST.high());
                if (y % 3 == 0) d.dot(32 + (int) Math.round(Math.sin((y + fr * 5) * 0.32 + 2.5) * 7), y, FROST.light());
            }
            d.dot(21, 14, rgb(255, 255, 255)); d.dot(21, 15, rgb(255, 255, 255));
        } else {
            d.dot(26, 30, rgb(180, 200, 220)); d.dot(27, 31, rgb(180, 200, 220)); d.dot(28, 33, rgb(140, 160, 180)); d.dot(27, 35, rgb(140, 160, 180));   // a crack
            d.dot(38, 50, rgb(255, 200, 120)); d.dot(40, 48, rgb(255, 240, 200));    // a spark
        }
        for (int y : new int[]{26, 44, 60}) {                                  // copper coils round its waist
            d.piece(COPPER).rect(14, y, 36, 4);
            for (int x = 15; x < 49; x += 2) d.dot(x, y + 1, COPPER.high());
        }
        d.piece(BRASS).disc(6, 50, 5);                                         // gauges on the side
        d.piece(PAPER).disc(6, 50, 3.4);
        d.dot(6, 50, OUTLINE); d.dot(broken ? 4 : 8, broken ? 52 : 48, CityArt.HYDRANT.mid());
        d.piece(BRASS).disc(58, 40, 4);
        d.piece(broken ? WIN_DARK : WIN_GREEN).disc(58, 40, 2.6);
        return done(d, 32, 89);
    }

    /**
     * A wall of the Blight's vines across the way: a thicket of thick black-violet stems knotted together, thorns all
     * along them, and pink buds glowing in the tangle ({@code fr} pulses them).
     */
    static Sprite vines(int fr) {
        Doll d = new Doll(72, 58);
        Random r = new Random(301);
        d.piece(VINE).ellipse(36, 50, 26, 6);                                 // the knot at the foot of it
        int[][] stems = {{4, 54, 16, 16}, {16, 16, 32, 4}, {32, 4, 48, 14}, {48, 14, 68, 52}, {10, 54, 28, 26}, {28, 26, 46, 30},
                         {46, 30, 60, 54}, {22, 54, 36, 18}, {36, 18, 54, 54}, {14, 36, 4, 22}, {58, 30, 70, 20}, {26, 40, 44, 40}};
        for (int[] st : stems) d.piece(VINE).thickLine(st[0], st[1], st[2], st[3], 3);   // each stem its own, so they cross
        for (int[] st : stems) {                                              // thorns along every stem
            for (int k = 1; k < 4; k++) {
                int x = st[0] + (st[2] - st[0]) * k / 4, y = st[1] + (st[3] - st[1]) * k / 4;
                int side = r.nextBoolean() ? 1 : -1;
                d.dot(x + 3 * side, y - 2, VINE.high()); d.dot(x + 4 * side, y - 3, VINE.light());
            }
        }
        int bud = fr == 0 ? rgb(255, 120, 200) : rgb(255, 180, 236);
        for (int[] p : new int[][]{{16, 15}, {32, 3}, {48, 13}, {28, 25}, {54, 46}, {18, 46}, {40, 38}, {64, 40}}) {
            d.piece(new Doll.Mat(rgb(90, 10, 60), rgb(180, 40, 120), bud, rgb(255, 210, 240), rgb(255, 255, 255), true)).disc(p[0], p[1], 2.2);
        }
        return done(d, 36, 57);
    }
}
