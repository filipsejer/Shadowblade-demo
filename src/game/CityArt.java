package game;

import static game.PixelCanvas.*;

import java.util.Map;
import java.util.Random;

/**
 * Lumen's streets and scenery, painted at the drawn sprites' pixel size to sit with them, as the forest is
 * ({@link ForestArt}): cobbled streets, the squares' big paving slabs, brick lanes between them, the slate and
 * terracotta rooftops beyond, and the fronts of the houses lining the streets, with their lit windows. The things
 * standing in the streets (lamps, the fountain, market stalls, the clock tower...) are shaded and outlined by a
 * {@link Doll}, like the people.
 */
final class CityArt {
    private CityArt() {}

    private static final double K = ForestArt.K;
    private static final int OUTLINE = Doll.OUTLINE;

    // ------------------------------------------------------------------ palette

    private static final int[] COBBLE = {rgb(44, 46, 60), rgb(80, 84, 100), rgb(98, 102, 118), rgb(116, 120, 136), rgb(140, 144, 158)};
    private static final int JOINT = rgb(112, 86, 72);
    private static final int[] SAND = {rgb(82, 62, 56), rgb(152, 120, 94), rgb(170, 138, 106), rgb(188, 156, 120), rgb(206, 176, 138)};
    private static final int[] BRICK = {rgb(66, 50, 54), rgb(118, 56, 50), rgb(142, 70, 58), rgb(164, 88, 70), rgb(186, 110, 86)};
    private static final int[] GRANITE = {rgb(28, 30, 40), rgb(56, 58, 74), rgb(70, 74, 90), rgb(86, 90, 106), rgb(104, 108, 124)};
    private static final int[] SLATE = {rgb(20, 22, 34), rgb(34, 38, 54), rgb(44, 50, 68), rgb(56, 62, 82), rgb(70, 78, 98)};
    private static final int[] TILE = {rgb(46, 20, 18), rgb(92, 40, 34), rgb(116, 54, 42), rgb(140, 72, 54), rgb(164, 94, 68)};
    private static final int[] WATER = {rgb(10, 26, 40), rgb(18, 42, 60), rgb(26, 58, 80), rgb(40, 82, 106), rgb(104, 164, 186)};
    private static final int WEED = rgb(52, 76, 46), WEED_L = rgb(78, 108, 58), LIT = rgb(255, 214, 120), LIT_L = rgb(255, 240, 190);

    // ------------------------------------------------------------------ materials for the things standing in the street

    static final Doll.Mat PLASTER = new Doll.Mat(rgb(70, 56, 52), rgb(150, 128, 112), rgb(194, 174, 148), rgb(218, 202, 176), rgb(236, 224, 200), false);
    static final Doll.Mat PLASTER_BLUE = new Doll.Mat(rgb(26, 34, 62), rgb(66, 86, 124), rgb(96, 122, 164), rgb(128, 156, 194), rgb(166, 192, 222), false);
    static final Doll.Mat PLASTER_ROSE = new Doll.Mat(rgb(76, 36, 46), rgb(156, 92, 98), rgb(194, 128, 124), rgb(218, 162, 152), rgb(238, 198, 184), false);
    static final Doll.Mat BRICK_M = new Doll.Mat(rgb(58, 22, 18), rgb(116, 50, 40), rgb(150, 72, 54), rgb(178, 98, 72), rgb(204, 126, 94), false);
    static final Doll.Mat TIMBER = new Doll.Mat(rgb(28, 16, 12), rgb(54, 34, 24), rgb(78, 50, 34), rgb(104, 70, 48), rgb(130, 94, 64), false);
    static final Doll.Mat ROOF_SLATE = new Doll.Mat(rgb(12, 14, 24), rgb(32, 36, 54), rgb(46, 52, 74), rgb(62, 70, 96), rgb(82, 92, 120), false);
    static final Doll.Mat ROOF_TILE = new Doll.Mat(rgb(48, 18, 14), rgb(104, 44, 34), rgb(140, 64, 46), rgb(170, 90, 62), rgb(198, 120, 84), false);
    static final Doll.Mat STONE = new Doll.Mat(rgb(30, 30, 40), rgb(80, 82, 98), rgb(106, 108, 124), rgb(132, 134, 150), rgb(162, 164, 178), false);
    static final Doll.Mat WIN_LIT = new Doll.Mat(rgb(110, 62, 20), rgb(232, 160, 64), rgb(255, 208, 112), rgb(255, 232, 164), rgb(255, 248, 214), false);
    static final Doll.Mat WIN_DARK = new Doll.Mat(rgb(6, 8, 20), rgb(18, 24, 44), rgb(28, 38, 64), rgb(44, 58, 92), rgb(76, 96, 136), false);
    static final Doll.Mat DOOR_GREEN = new Doll.Mat(rgb(12, 32, 26), rgb(28, 66, 50), rgb(42, 94, 68), rgb(62, 122, 88), rgb(88, 152, 110), false);
    static final Doll.Mat DOOR_RED = new Doll.Mat(rgb(50, 12, 18), rgb(104, 30, 36), rgb(142, 46, 48), rgb(176, 70, 64), rgb(206, 104, 90), false);
    static final Doll.Mat IRON = new Doll.Mat(rgb(8, 8, 14), rgb(28, 28, 38), rgb(44, 46, 58), rgb(66, 68, 84), rgb(96, 98, 118), true);
    static final Doll.Mat AWN_RED = new Doll.Mat(rgb(80, 16, 24), rgb(160, 40, 48), rgb(206, 64, 66), rgb(236, 104, 96), rgb(255, 150, 136), false);
    static final Doll.Mat AWN_TEAL = new Doll.Mat(rgb(10, 46, 50), rgb(26, 102, 104), rgb(42, 146, 140), rgb(84, 190, 176), rgb(140, 226, 206), false);
    static final Doll.Mat AWN_GOLD = new Doll.Mat(rgb(96, 60, 10), rgb(186, 128, 30), rgb(228, 174, 54), rgb(250, 210, 100), rgb(255, 236, 160), false);
    static final Doll.Mat CLOTH_WHITE = new Doll.Mat(rgb(90, 86, 96), rgb(186, 182, 190), rgb(226, 222, 228), rgb(244, 242, 246), rgb(255, 255, 255), false);
    static final Doll.Mat LEAVES = new Doll.Mat(rgb(12, 34, 22), rgb(30, 76, 40), rgb(52, 114, 50), rgb(88, 152, 62), rgb(134, 188, 82), true);
    static final Doll.Mat WATER_M = new Doll.Mat(rgb(10, 34, 56), rgb(30, 80, 120), rgb(52, 122, 168), rgb(98, 172, 210), rgb(200, 240, 255), true);
    static final Doll.Mat CREAM = new Doll.Mat(rgb(96, 86, 66), rgb(204, 194, 164), rgb(236, 228, 200), rgb(250, 246, 226), rgb(255, 255, 246), true);
    static final Doll.Mat HYDRANT = new Doll.Mat(rgb(74, 10, 16), rgb(150, 30, 34), rgb(206, 50, 46), rgb(240, 96, 80), rgb(255, 170, 150), true);
    static final Doll.Mat BLIGHT = new Doll.Mat(rgb(36, 8, 40), rgb(84, 26, 92), rgb(126, 46, 136), rgb(170, 76, 178), rgb(214, 130, 220), true);
    static final Doll.Mat CONCRETE = new Doll.Mat(rgb(38, 40, 48), rgb(96, 98, 108), rgb(124, 126, 134), rgb(150, 152, 160), rgb(176, 178, 186), false);
    static final Doll.Mat COPPER = new Doll.Mat(rgb(60, 26, 10), rgb(150, 74, 30), rgb(196, 110, 52), rgb(230, 152, 84), rgb(255, 204, 140), true);

    // ------------------------------------------------------------------ the ground (seamless textures)

    /** Voronoi stones over an n-pixel seamless square: who owns each pixel, and whether it's in the gap between two. */
    private record Stones(int[] owner, boolean[] gap, int count) {}

    private static Stones stones(int n, int cells, long seed, double gapWidth) {
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
                double d = dx * dx + dy * dy * 1.3;                         // stones a little wider than tall, as seen from above
                if (d < best) { second = best; best = d; who = k; } else if (d < second) second = d;
            }
            owner[y * n + x] = who;
            gap[y * n + x] = Math.sqrt(second) - Math.sqrt(best) < gapWidth;
        }
        return new Stones(owner, gap, count);
    }

    /** Shades a field of stones: each its own tone, lit along the top and left, dark along the bottom. */
    private static void shadeStones(PixelCanvas c, Stones st, int[] ramp, int[] tones, int n) {
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int i = y * n + x;
            if (st.gap()[i]) { c.set(x, y, ramp[0]); continue; }
            int s = tones[st.owner()[i]];
            boolean top = st.gap()[Math.floorMod(y - 1, n) * n + x], left = st.gap()[y * n + Math.floorMod(x - 1, n)];
            boolean bottom = st.gap()[Math.floorMod(y + 1, n) * n + x], right = st.gap()[y * n + Math.floorMod(x + 1, n)];
            if (top || left && ForestArt.bayer(x, y) > 0.3) s = Math.min(ramp.length - 1, s + 1);
            if (bottom || right && ForestArt.bayer(x, y) > 0.6) s = Math.max(1, s - 1);
            c.set(x, y, ramp[s]);
        }
    }

    /** The streets: rounded cobbles of blue-grey stone, worn smooth, with weeds in a few of the cracks. 96 px, seamless. */
    static PixelCanvas cobbles() {
        int n = 96;
        Stones st = stones(n, 12, 71, 1.5);
        Random r = new Random(72);
        double[] f = ForestArt.field(n, 73, 2);
        int[] tones = new int[st.count()];
        PixelCanvas c = new PixelCanvas(n, n);
        for (int k = 0; k < tones.length; k++) tones[k] = r.nextInt(5) == 0 ? 1 : r.nextInt(4) == 0 ? 3 : 2;
        shadeStones(c, st, COBBLE, tones, n);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {          // broad light and shadow, so the repeat doesn't show
            int s = ForestArt.stepOf(COBBLE, c.get(x, y));
            double v = f[y * n + x] + (ForestArt.bayer(x, y) - 0.5) * 0.18;
            if (s > 1 && v < 0.36) c.set(x, y, COBBLE[s - 1]);
            else if (s > 0 && s < 4 && v > 0.66) c.set(x, y, COBBLE[s + 1]);
        }
        for (int k = 0; k < 120; k++) {                                     // weeds in the cracks
            int x = r.nextInt(n), y = r.nextInt(n);
            if (!st.gap()[y * n + x] || f[y * n + x] > 0.5) continue;
            c.set(x, y, r.nextInt(3) == 0 ? WEED_L : WEED);
        }
        for (int k = 0; k < 60; k++) {                                      // grit and wear
            int x = r.nextInt(n), y = r.nextInt(n);
            int s = ForestArt.stepOf(COBBLE, c.get(x, y));
            if (s > 1) c.set(x, y, COBBLE[s - 1]);
        }
        return c;
    }

    /**
     * The squares: big warm sandstone slabs, square and oblong, laid on a grid of 16-pixel cells with some pairs joined
     * into one long slab, each its own shade, a few cracked or worn. 96 px, seamless.
     */
    static PixelCanvas plaza() {
        int n = 96, cell = 24, cells = n / cell;
        Random r = new Random(81);
        int[] id = new int[cells * cells];
        java.util.Arrays.fill(id, -1);
        int next = 0;
        for (int j = 0; j < cells; j++) for (int i = 0; i < cells; i++) {
            if (id[j * cells + i] >= 0) continue;
            id[j * cells + i] = next;
            int roll = r.nextInt(5);
            if (roll == 0 && id[j * cells + (i + 1) % cells] < 0) id[j * cells + (i + 1) % cells] = next;            // a long slab
            else if (roll == 1 && id[((j + 1) % cells) * cells + i] < 0) id[((j + 1) % cells) * cells + i] = next;   // a tall one
            next++;
        }
        int[] tone = new int[next];
        for (int k = 0; k < next; k++) tone[k] = r.nextInt(5) == 0 ? 1 : r.nextInt(3) == 0 ? 3 : 2;
        PixelCanvas c = new PixelCanvas(n, n);
        java.util.function.IntBinaryOperator at = (x, y) -> {             // the slab under a pixel: odd rows are shifted half a slab
            int yy = Math.floorMod(y, n), row = yy / cell;
            int xx = Math.floorMod(x + (row % 2 == 1 ? cell / 2 : 0), n);
            return id[row * cells + xx / cell];
        };
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            int me = at.applyAsInt(x, y);
            int s2 = tone[me], col;
            if (at.applyAsInt(x, y + 1) != me || at.applyAsInt(x + 1, y) != me) col = JOINT;                   // the joint
            else if (at.applyAsInt(x, y - 1) != me || at.applyAsInt(x - 1, y) != me) col = SAND[Math.min(4, s2 + 1)];   // its lit edge
            else if (at.applyAsInt(x, y + 2) != me) col = SAND[Math.max(1, s2 - 1)];                         // and its shaded one
            else col = SAND[ForestArt.bayer(x, y) > 0.92 ? Math.max(1, s2 - 1) : s2];
            c.set(x, y, col);
        }
        for (int k = 0; k < 7; k++) {                                       // cracks
            int x = r.nextInt(n), y = r.nextInt(n);
            for (int i = 0; i < 7; i++) {
                if (ForestArt.stepOf(SAND, c.get(Math.floorMod(x, n), Math.floorMod(y, n))) > 0) c.set(Math.floorMod(x, n), Math.floorMod(y, n), JOINT);
                x++;
                if (r.nextBoolean()) y += r.nextInt(3) - 1;
            }
        }
        for (int k = 0; k < 160; k++) {                                     // speckles in the stone
            int x = r.nextInt(n), y = r.nextInt(n);
            int s2 = ForestArt.stepOf(SAND, c.get(x, y));
            if (s2 >= 2) c.set(x, y, SAND[r.nextBoolean() ? s2 - 1 : Math.min(4, s2 + 1)]);
        }
        return c;
    }

    /** The lanes between: red bricks in running bond. 64 px, seamless. */
    static PixelCanvas bricks() {
        int n = 64, bw = 8, bh = 4;
        Random r = new Random(91);
        PixelCanvas c = new PixelCanvas(n, n);
        for (int row = 0; row < n / bh; row++) {
            int off = row % 2 == 0 ? 0 : bw / 2;
            for (int b = 0; b < n / bw; b++) {
                int tone = r.nextInt(5) == 0 ? 1 : r.nextInt(4) == 0 ? 3 : 2;
                for (int yy = 0; yy < bh; yy++) for (int xx = 0; xx < bw; xx++) {
                    int px = Math.floorMod(b * bw + xx + off, n), py = row * bh + yy;
                    int s = yy == bh - 1 || xx == bw - 1 ? 0 : yy == 0 ? Math.min(4, tone + 1) : yy == bh - 2 && xx > 0 ? Math.max(1, tone - 1) : tone;
                    c.set(px, py, BRICK[s]);
                }
                if (r.nextInt(7) == 0) c.set(Math.floorMod(b * bw + off + 1 + r.nextInt(5), n), row * bh + 1, BRICK[0]);   // a chip
            }
        }
        return c;
    }

    /** The low wall along the streets' edges: dark granite blocks. 64 px, seamless. */
    static PixelCanvas granite() {
        int n = 64, bw = 16, bh = 8;
        Random r = new Random(101);
        PixelCanvas c = new PixelCanvas(n, n);
        for (int row = 0; row < n / bh; row++) {
            int off = row % 2 == 0 ? 0 : bw / 2;
            for (int b = 0; b < n / bw; b++) {
                int tone = 1 + r.nextInt(3);
                for (int yy = 0; yy < bh; yy++) for (int xx = 0; xx < bw; xx++) {
                    int px = Math.floorMod(b * bw + xx + off, n), py = row * bh + yy;
                    int s = yy == bh - 1 || xx == bw - 1 ? 0 : yy == 0 || xx == 0 ? Math.min(4, tone + 1) : yy == bh - 2 ? Math.max(1, tone - 1) : tone;
                    if (s == tone && ForestArt.bayer(px, py) > 0.88) s = Math.max(1, s - 1);
                    c.set(px, py, GRANITE[s]);
                }
            }
        }
        return c;
    }

    /**
     * Beyond the streets: the city's rooftops seen from above, slate and terracotta shingles in four roofs with gutters
     * between, chimneys, and here and there a lit skylight. 128 px, seamless.
     */
    static PixelCanvas roofs() {
        int n = 128, half = n / 2;
        Random r = new Random(111);
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = ForestArt.field(n, 112, 2);
        for (int q = 0; q < 4; q++) {
            int qx = (q % 2) * half, qy = (q / 2) * half;
            int[] ramp = q == 1 || q == 2 ? TILE : SLATE;
            int sw = ramp == TILE ? 5 : 6, sh = 4;
            for (int y = 0; y < half; y++) for (int x = 0; x < half; x++) {
                int row = y / sh, off = row % 2 == 0 ? 0 : sw / 2;
                int xx = Math.floorMod(x + off, sw), yy = y % sh;
                int s = yy == sh - 1 ? 0 : xx == sw - 1 ? 1 : yy == 0 ? 3 : 2;
                double v = f[(qy + y) * n + qx + x] + (ForestArt.bayer(x, y) - 0.5) * 0.2;
                if (s > 1 && v < 0.38) s--;
                if (s > 0 && s < 4 && v > 0.68) s++;
                if (y < 3) s = y == 0 ? 0 : 4;                               // the ridge along the top of each roof
                if (x == 0) s = 0;                                           // and the gutter between
                c.set(qx + x, qy + y, ramp[s]);
            }
        }
        for (int k = 0; k < 4; k++) chimney(c, 10 + r.nextInt(44) + (k % 2) * half, 14 + r.nextInt(30) + (k / 2) * half, r);
        for (int k = 0; k < 3; k++) skylight(c, 8 + r.nextInt(44) + (k % 2) * half, 12 + r.nextInt(36) + (k / 2 == 0 ? half : 0), r.nextInt(3) != 0);
        return c;
    }

    private static void chimney(PixelCanvas c, int x, int y, Random r) {
        int w = 7 + r.nextInt(3), h = 9 + r.nextInt(4);
        for (int dy = 0; dy <= h + 2; dy++) for (int dx = 0; dx <= w + 2; dx++) {                 // its shadow, down and to the right
            int s = ForestArt.stepOf(SLATE, ForestArt.get(c, x + dx + 2, y + dy + 2, true));
            int t = ForestArt.stepOf(TILE, ForestArt.get(c, x + dx + 2, y + dy + 2, true));
            if (s > 0) ForestArt.put(c, x + dx + 2, y + dy + 2, SLATE[s - 1], true);
            else if (t > 0) ForestArt.put(c, x + dx + 2, y + dy + 2, TILE[t - 1], true);
        }
        for (int dy = 0; dy < h; dy++) for (int dx = 0; dx < w; dx++) {
            int col = dy < 3 ? (dy == 0 ? BRICK[4] : BRICK[3]) : (dy % 3 == 2 ? BRICK[0] : dx == w - 1 ? BRICK[1] : BRICK[2]);
            ForestArt.put(c, x + dx, y + dy, col, true);
        }
        for (int dx = 1; dx < w - 1; dx++) ForestArt.put(c, x + dx, y + 1, rgb(26, 20, 22), true);   // the flue
        for (int dx = -1; dx <= w; dx++) { ForestArt.put(c, x + dx, y - 1, OUTLINE, true); ForestArt.put(c, x + dx, y + h, OUTLINE, true); }
        for (int dy = 0; dy < h; dy++) { ForestArt.put(c, x - 1, y + dy, OUTLINE, true); ForestArt.put(c, x + w, y + dy, OUTLINE, true); }
    }

    private static void skylight(PixelCanvas c, int x, int y, boolean lit) {
        int w = 7, h = 5;
        for (int dy = -1; dy <= h; dy++) for (int dx = -1; dx <= w; dx++) ForestArt.put(c, x + dx, y + dy, OUTLINE, true);
        for (int dy = 0; dy < h; dy++) for (int dx = 0; dx < w; dx++) {
            int col = lit ? (dy == 0 || dx == 0 ? LIT_L : LIT) : (dy == 0 ? WATER[2] : WATER[1]);
            ForestArt.put(c, x + dx, y + dy, col, true);
        }
        for (int dy = 0; dy < h; dy++) ForestArt.put(c, x + 3, y + dy, lit ? rgb(196, 130, 60) : OUTLINE, true);   // the frame's middle bar
    }

    /** The canal: dark water with ripples of light. 64 px, seamless. */
    static PixelCanvas water() {
        int n = 64;
        PixelCanvas c = new PixelCanvas(n, n);
        double[] f = ForestArt.field(n, 121, 2);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) c.set(x, y, ForestArt.tone(WATER, 0, 2, f[y * n + x], x, y, 0.9));
        Random r = new Random(122);
        for (int k = 0; k < 40; k++) {                                       // ripples: short bright dashes along the flow
            int x = r.nextInt(n), y = r.nextInt(n), len = 2 + r.nextInt(4);
            for (int i = 0; i < len; i++) c.set(Math.floorMod(x + i, n), y, i == 0 || i == len - 1 ? WATER[3] : WATER[r.nextInt(5) == 0 ? 4 : 3]);
        }
        return c;
    }

    // ------------------------------------------------------------------ on the floor

    private static Sprite fine(PixelCanvas c, int ax, int ay) { return new Sprite(c.image(), ax, ay, K); }

    /** Soft dark patches of damp and grime, laid first to break up the streets' repeat. */
    static Sprite[] patches() {
        return new Sprite[]{grime(46, 24, 0.22, 131), grime(60, 30, 0.18, 132), grime(36, 20, 0.26, 133), grime(52, 26, 0.2, 134), damp(40, 16, 135), damp(56, 20, 136)};
    }

    private static Sprite grime(int w, int h, double strength, long seed) {
        PixelCanvas c = new PixelCanvas(w, h);
        Random r = new Random(seed);
        double phase = r.nextDouble() * 6;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0);
            double a = Math.atan2(dy, dx), d = Math.hypot(dx, dy) / (1 + 0.16 * Math.sin(a * 3 + phase));
            if (d < 1 && (1 - d) * 1.6 > ForestArt.bayer(x, y)) c.set(x, y, rgba(14, 16, 34, (int) (255 * strength)));
        }
        return fine(c, w / 2, h / 2);
    }

    /** A damp patch: darker, with a glint or two where it catches the lamplight. */
    private static Sprite damp(int w, int h, long seed) {
        PixelCanvas c = new PixelCanvas(w, h);
        Random r = new Random(seed);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double dx = (x - w / 2.0 + 0.5) / (w / 2.0), dy = (y - h / 2.0 + 0.5) / (h / 2.0), d = Math.hypot(dx, dy);
            if (d < 1 && (1 - d) * 2.2 > ForestArt.bayer(x, y)) c.set(x, y, rgba(20, 40, 80, 70));
        }
        for (int i = 0; i < 4; i++) c.set(w / 4 + r.nextInt(w / 2), h / 3 + r.nextInt(h / 3), rgba(200, 220, 255, 120));
        return fine(c, w / 2, h / 2);
    }

    /** Little things lying in the street (anchored at the bottom middle). */
    static Sprite[] floor() {
        return new Sprite[]{puddle(18, 7), puddle(26, 9), manhole(), drain(), paper(), leaves(), weeds(), bottle(), pigeon(), crack()};
    }

    private static Sprite puddle(int w, int h) {
        PixelCanvas c = new PixelCanvas(w, h);
        c.ellipse(w / 2.0, h / 2.0, w / 2.0 - 0.5, h / 2.0 - 0.5, rgb(30, 52, 86));
        c.ellipse(w / 2.0 - 1, h / 2.0 - 0.6, w / 2.0 - 2.5, h / 2.0 - 1.8, rgb(46, 76, 118));
        c.rect(w / 3, h / 2 - 1, 3, 1, rgb(140, 180, 230)); c.set(w / 3 + 4, h / 2 - 1, rgb(96, 136, 190));
        c.set(w * 2 / 3, h / 2 + 1, rgb(255, 214, 140));                    // a lamp's reflection
        return fine(c, w / 2, h - 1);
    }

    private static Sprite manhole() {
        PixelCanvas c = new PixelCanvas(16, 9);
        c.ellipse(8, 4.5, 7.6, 4.2, OUTLINE);
        c.ellipse(8, 4.5, 6.6, 3.4, rgb(64, 66, 80));
        c.ellipse(8, 4.2, 5.2, 2.6, rgb(84, 86, 100));
        for (int x = 4; x <= 12; x += 2) c.set(x, 4, rgb(52, 54, 66));
        c.rect(5, 3, 7, 1, rgb(104, 106, 120)); c.rect(5, 6, 7, 1, rgb(52, 54, 66));
        return fine(c, 8, 8);
    }

    private static Sprite drain() {
        PixelCanvas c = new PixelCanvas(12, 7);
        c.rect(0, 0, 12, 7, OUTLINE);
        c.rect(1, 1, 10, 5, rgb(20, 22, 30));
        for (int x = 2; x < 11; x += 2) c.rect(x, 1, 1, 5, rgb(88, 90, 104));
        c.rect(1, 1, 10, 1, rgb(110, 112, 126));
        return fine(c, 6, 6);
    }

    private static Sprite paper() {
        PixelCanvas c = new PixelCanvas(9, 6);
        c.rect(1, 1, 6, 4, rgb(214, 206, 186)); c.rect(1, 1, 6, 1, rgb(236, 230, 214)); c.set(7, 2, rgb(186, 178, 160));
        c.rect(2, 2, 4, 1, rgb(120, 116, 130)); c.rect(2, 3, 3, 1, rgb(150, 146, 156));
        return fine(c, 4, 5);
    }

    private static Sprite leaves() {
        PixelCanvas c = new PixelCanvas(12, 6);
        int[][] at = {{1, 2}, {5, 0}, {8, 3}};
        int[] col = {rgb(180, 100, 40), rgb(206, 150, 54), rgb(150, 70, 34)};
        for (int i = 0; i < at.length; i++) {
            int x = at[i][0], y = at[i][1];
            c.rect(x, y, 3, 2, col[i]); c.set(x + 2, y + 1, darken(col[i], 0.3)); c.set(x, y, lighten(col[i], 0.2));
        }
        return fine(c, 6, 5);
    }

    private static Sprite weeds() {
        PixelCanvas c = new PixelCanvas(9, 7);
        c.line(4, 6, 4, 2, WEED); c.line(4, 6, 1, 3, WEED_L); c.line(4, 6, 7, 2, WEED_L); c.set(2, 5, WEED); c.set(6, 5, WEED);
        c.set(4, 1, rgb(232, 220, 120));                                     // a dandelion
        return fine(c, 4, 6);
    }

    private static Sprite bottle() {
        PixelCanvas c = new PixelCanvas(10, 5);
        c.rect(1, 1, 6, 3, rgb(40, 110, 70)); c.rect(7, 2, 2, 1, rgb(40, 110, 70)); c.rect(2, 1, 4, 1, rgb(120, 200, 150));
        c.outline(OUTLINE);
        return fine(c, 5, 4);
    }

    private static Sprite pigeon() {
        Doll d = new Doll(12, 10);
        d.piece(STONE).ellipse(5.5, 6, 4, 2.6);
        d.piece(STONE).disc(8.5, 3.6, 1.9);
        d.piece(AWN_GOLD).set(10, 4);
        d.dot(9, 3, Doll.EYE);
        d.dot(6, 5, rgb(110, 170, 150)); d.dot(7, 5, rgb(150, 110, 170));    // the sheen on its neck
        d.piece(HYDRANT).set(4, 9).set(6, 9);
        return fine(d.render(), 6, 9);
    }

    private static Sprite crack() {
        PixelCanvas c = new PixelCanvas(16, 6);
        int d = rgb(30, 32, 44);
        c.line(0, 3, 4, 2, d); c.line(4, 2, 9, 4, d); c.line(9, 4, 15, 2, d); c.line(7, 3, 8, 0, d); c.set(12, 4, WEED);
        return fine(c, 8, 5);
    }

    // ------------------------------------------------------------------ the fronts of the houses along the streets

    /** House fronts, to line the top edge of every street (anchored at the bottom middle). */
    static Sprite[] facades() {
        return new Sprite[]{
            facade(0, 76, 124, 1), facade(1, 84, 112, 2), facade(2, 72, 134, 3), facade(3, 64, 128, 4),
            facade(0, 80, 116, 5), facade(1, 76, 130, 6), facade(2, 88, 120, 7), facade(3, 68, 112, 8),
            facade(1, 72, 74, 9), facade(0, 80, 76, 10), facade(2, 76, 72, 11),                     // one-storey shops, for short gaps
        };
    }

    /**
     * A house front {@code w} by {@code h} fine pixels: a pitched roof seen from the street with a chimney, upper floors
     * of windows (some lit), and a ground floor with a door and, for shops, a lit window under an awning.
     * style 0: cream plaster and dark timbers; 1: a brick shop; 2: blue plaster with a balcony; 3: a narrow rose house.
     */
    private static Sprite facade(int style, int w, int h, long seed) {
        Random r = new Random(seed * 7717L);
        Doll d = new Doll(w, h);
        Doll.Mat wall = switch (style) { case 1 -> BRICK_M; case 2 -> PLASTER_BLUE; case 3 -> PLASTER_ROSE; default -> PLASTER; };
        Doll.Mat roof = style == 1 || style == 3 ? ROOF_TILE : ROOF_SLATE;
        int roofH = 18 + r.nextInt(5), inset = 7;
        // the chimney, standing up behind the roof
        int chx = r.nextBoolean() ? 12 + r.nextInt(8) : w - 22 - r.nextInt(8);
        d.piece(BRICK_M).rect(chx, 2, 8, roofH);
        d.piece(STONE).rect(chx - 1, 1, 10, 2);
        // the roof: its slope runs back away from us, so it's narrower at the ridge
        d.piece(roof).poly(new int[]{inset, w - inset, w, 0}, new int[]{6, 6, 6 + roofH, 6 + roofH});
        int wallTop = 6 + roofH;
        for (int y = 9; y < wallTop - 1; y += 3) for (int x = 1; x < w - 1; x++) if ((x + y) % 2 == 0 && x > inset * (wallTop - y) / roofH - 1 && x < w - inset * (wallTop - y) / roofH) d.dot(x, y, roof, 0);   // rows of shingles
        if (h > 100 && (style == 2 || style == 0 && w > 74)) {             // a dormer window in the roof
            int dx = w / 2 - 7;
            d.piece(wall).rect(dx, wallTop - 14, 14, 13);
            d.piece(roof).tri(dx - 2, wallTop - 13, dx + 7, wallTop - 21, dx + 16, wallTop - 13);
            window(d, dx + 3, wallTop - 11, 8, 8, r.nextInt(3) != 0, null);
        }
        // the wall and its cornice
        d.piece(wall).rect(0, wallTop, w, h - wallTop);
        d.piece(STONE).rect(-1, wallTop, w + 2, 3);
        if (style == 1) {                                                   // brick: mortar lines
            for (int y = wallTop + 5; y < h - 6; y += 4) for (int x = 1; x < w - 1; x++) if (x % 2 == 0) d.dot(x, y, BRICK_M, 0);
            for (int y = wallTop + 5; y < h - 6; y += 4) for (int x = (y / 4) % 2 * 4 + 2; x < w - 1; x += 8) { d.dot(x, y + 1, BRICK_M, 0); d.dot(x, y + 2, BRICK_M, 0); }
        }
        int ground = h - 40;                                                // the ground floor's top
        // upper floors of windows
        int floors = Math.max(0, (ground - wallTop - 6) / 30);
        if (style == 0 && floors > 0) {                                    // timber framing
            d.piece(TIMBER).rect(0, ground - 2, w, 2).rect(0, wallTop + 3, 2, ground - wallTop - 3).rect(w - 2, wallTop + 3, 2, ground - wallTop - 3);
            for (int x = w / 3; x < w - 4; x += w / 3) d.piece(TIMBER).rect(x - 1, wallTop + 3, 2, ground - wallTop - 3);
            d.piece(TIMBER).line(2, ground - 2, w / 3 - 1, wallTop + 3).line(w - 3, ground - 2, w - w / 3, wallTop + 3);
        }
        int cols = Math.max(2, (w - 8) / 24);
        double gapX = (w - cols * 12) / (cols + 1.0);
        for (int f = 0; f < floors; f++) {
            int wy = wallTop + 8 + f * 30;
            for (int i = 0; i < cols; i++) {
                int wx = (int) Math.round(gapX + i * (12 + gapX));
                window(d, wx, wy, 12, 17, r.nextInt(5) < 3, style == 2 && f == floors - 1 ? null : style == 3 ? DOOR_GREEN : null);
                if (style != 2 && r.nextInt(3) == 0) flowerBox(d, wx - 1, wy + 18, 14, r);
            }
            if (style == 2 && f == floors - 1) balcony(d, 4, wy + 18, w - 8);
        }
        // the ground floor
        d.piece(STONE).rect(0, h - 5, w, 5);                                // the plinth
        int doorX = style == 3 ? w / 2 - 7 : r.nextBoolean() ? 6 : w - 22;
        Doll.Mat doorMat = r.nextBoolean() ? DOOR_GREEN : DOOR_RED;
        d.piece(STONE).rect(doorX - 2, ground + 6, 18, h - ground - 6);    // the doorway's stone frame
        d.piece(doorMat).rect(doorX, ground + 12, 14, h - ground - 15);
        d.piece(WIN_LIT).rect(doorX + 2, ground + 7, 10, 4);               // a lit fanlight over the door
        d.dot(doorX + 11, ground + 24, AWN_GOLD.mid());                     // the handle
        d.dot(doorX + 7, ground + 14, doorMat, 0); d.dot(doorX + 7, ground + 15, doorMat, 0);
        for (int y = ground + 17; y < h - 6; y++) d.dot(doorX + 7, y, doorMat, 0);   // two leaves
        d.piece(STONE).rect(doorX - 3, h - 3, 20, 3);                       // the step
        int shopX = doorX < w / 2 ? doorX + 22 : 6, shopW = w - 28 - 6;
        if (style == 3) {                                                  // a narrow house: a window either side of the door
            window(d, 5, ground + 10, 10, 15, r.nextBoolean(), null);
            window(d, w - 15, ground + 10, 10, 15, r.nextBoolean(), null);
        } else if (style == 1 || style == 2) {                              // a shop: a wide lit window, a sign and an awning
            d.piece(TIMBER).rect(shopX - 2, ground + 12, shopW + 4, h - ground - 17);
            d.piece(WIN_LIT).rect(shopX, ground + 14, shopW, h - ground - 22);
            for (int x = shopX + shopW / 3; x < shopX + shopW - 2; x += shopW / 3) for (int y = ground + 14; y < h - 8; y++) d.dot(x, y, TIMBER.dark());
            goods(d, shopX + 2, h - 10, shopW - 4, r);
            awning(d, shopX - 4, ground + 3, shopW + 8, style == 1 ? AWN_RED : AWN_TEAL);
        } else {
            window(d, shopX + 2, ground + 10, 12, 16, true, null);
            if (shopW > 30) window(d, shopX + shopW - 14, ground + 10, 12, 16, r.nextBoolean(), null);
        }
        if (r.nextBoolean()) {                                             // a lamp on a bracket by the door
            int lx = doorX < w / 2 ? doorX + 18 : doorX - 5;
            d.piece(IRON).rect(lx, ground + 4, 3, 1);
            d.piece(IRON).rect(lx, ground + 2, 3, 1);
            d.piece(WIN_LIT).rect(lx, ground + 5, 3, 4);
        }
        int pipe = doorX < w / 2 ? w - 3 : 1;                               // a drainpipe down one side
        d.piece(IRON).rect(pipe, wallTop + 3, 2, h - wallTop - 6);
        return new Sprite(d.render().image(), w / 2, h - 1, K);
    }

    /** A window: a stone frame, glass lit warm or dark, a cross of glazing bars, a sill; shutters if a colour is given. */
    private static void window(Doll d, int x, int y, int ww, int wh, boolean lit, Doll.Mat shutters) {
        if (shutters != null) {
            d.piece(shutters).rect(x - 5, y, 4, wh);
            d.piece(shutters).rect(x + ww + 1, y, 4, wh);
            for (int yy = y + 1; yy < y + wh - 1; yy += 2) { d.dot(x - 4, yy, shutters, 0); d.dot(x + ww + 3, yy, shutters, 0); }
        }
        d.piece(STONE).rect(x - 1, y - 1, ww + 2, wh + 2);
        d.piece(lit ? WIN_LIT : WIN_DARK).rect(x + 1, y + 1, ww - 2, wh - 2);
        int bar = lit ? WIN_LIT.line() : OUTLINE;
        for (int yy = y + 1; yy < y + wh - 1; yy++) d.dot(x + ww / 2, yy, bar);
        for (int xx = x + 1; xx < x + ww - 1; xx++) d.dot(xx, y + wh / 2, bar);
        if (lit) { d.dot(x + 2, y + 2, WIN_LIT.high()); d.dot(x + 3, y + 2, WIN_LIT.light()); }
        else d.dot(x + 2, y + 2, WIN_DARK.high());
        d.piece(STONE).rect(x - 2, y + wh, ww + 4, 2);
    }

    private static void flowerBox(Doll d, int x, int y, int w, Random r) {
        d.piece(TIMBER).rect(x, y, w, 3);
        d.piece(LEAVES).rect(x + 1, y - 2, w - 2, 2);
        int[] cols = {rgb(255, 120, 150), rgb(255, 230, 120), rgb(250, 250, 250), rgb(240, 80, 80)};
        for (int i = 0; i < w / 3; i++) d.dot(x + 1 + r.nextInt(w - 2), y - 2 + r.nextInt(2), cols[r.nextInt(cols.length)]);
    }

    private static void balcony(Doll d, int x, int y, int w) {
        d.piece(STONE).rect(x - 1, y, w + 2, 2);
        d.piece(IRON).rect(x, y - 7, w, 1);
        for (int xx = x; xx < x + w; xx += 3) d.piece(IRON).rect(xx, y - 7, 1, 7);
    }

    /** A striped awning over a shop window, its scalloped edge hanging down. */
    private static void awning(Doll d, int x, int y, int w, Doll.Mat m) {
        d.piece(m).poly(new int[]{x + 2, x + w - 2, x + w, x}, new int[]{y, y, y + 8, y + 8});
        for (int xx = x; xx < x + w; xx += 4) d.set(xx + 1, y + 9).set(xx + 2, y + 9);
        for (int xx = x + 4; xx < x + w - 2; xx += 8) for (int yy = y + 1; yy < y + 9; yy++) { d.dot(xx, yy, CLOTH_WHITE.mid()); d.dot(xx + 1, yy, CLOTH_WHITE.light()); d.dot(xx + 2, yy, CLOTH_WHITE.mid()); d.dot(xx + 3, yy, CLOTH_WHITE.dark()); }
    }

    /** Goods in a shop window: jars, loaves, hats, all just a few pixels each. */
    private static void goods(Doll d, int x, int y, int w, Random r) {
        int[] cols = {rgb(196, 70, 60), rgb(90, 160, 90), rgb(220, 170, 70), rgb(120, 110, 200), rgb(200, 120, 70)};
        for (int xx = x; xx < x + w - 3; xx += 5) {
            int c = cols[r.nextInt(cols.length)], hgt = 2 + r.nextInt(3);
            for (int yy = 0; yy < hgt; yy++) { d.dot(xx, y - yy, c); d.dot(xx + 1, y - yy, darken(c, 0.25)); d.dot(xx + 2, y - yy, darken(c, 0.4)); }
            d.dot(xx, y - hgt, lighten(c, 0.4));
        }
    }

    // ------------------------------------------------------------------ along the other edges of the streets

    /** Low things along the bottom and side edges of the streets: crates, barrels, planters, a bin. */
    static Sprite[] low() {
        return new Sprite[]{crates(), barrels(), planter(), bin(), railing(), crates()};
    }

    // ------------------------------------------------------------------ the city's landmarks, by name

    static void register(Map<String, Sprite[]> m) {
        m.put("landmark.lamp", new Sprite[]{lamp()});
        m.put("landmark.planter", new Sprite[]{planter()});
        m.put("landmark.bench", new Sprite[]{bench()});
        m.put("landmark.fountain", new Sprite[]{fountain()});
        m.put("landmark.hydrant", new Sprite[]{hydrant()});
        m.put("landmark.crates", new Sprite[]{crates()});
        m.put("landmark.cart", new Sprite[]{cart()});
        m.put("landmark.stall0", new Sprite[]{stall(AWN_RED, 0)});
        m.put("landmark.stall1", new Sprite[]{stall(AWN_TEAL, 1)});
        m.put("landmark.stall2", new Sprite[]{stall(AWN_GOLD, 2)});
        m.put("landmark.clock", new Sprite[]{clockTower()});
        m.put("landmark.scrap", new Sprite[]{scrap()});
        m.put("landmark.pylon", new Sprite[]{pylon()});
        m.put("landmark.signpost", new Sprite[]{signpost()});
        m.put("relay.off", new Sprite[]{relay(false, 0), relay(false, 1)});
        m.put("relay.on", new Sprite[]{relay(true, 0), relay(true, 1)});
    }

    private static Sprite done(Doll d, int ax, int ay) { return new Sprite(d.render().image(), ax, ay, K); }

    /** An iron street lamp: a fluted post, a crossbar, and a lantern lit warm. */
    static Sprite lamp() {
        Doll d = new Doll(16, 62);
        d.piece(IRON).rect(5, 56, 6, 5).rect(4, 59, 8, 2);                  // the foot
        d.piece(IRON).rect(7, 14, 2, 43);                                    // the post
        d.piece(IRON).rect(6, 26, 4, 2).rect(6, 44, 4, 2);                   // its collars
        d.piece(IRON).rect(3, 13, 10, 2);                                    // the crossbar
        d.piece(IRON).tri(3, 4, 8, 0, 13, 4);                                // the lantern's cap
        d.piece(WIN_LIT).rect(4, 5, 8, 8);                                   // its glass, lit
        d.piece(IRON).rect(4, 4, 8, 1);
        for (int y = 5; y < 13; y++) d.dot(8, y, WIN_LIT.line());
        d.dot(5, 6, WIN_LIT.high()); d.dot(5, 7, WIN_LIT.high());
        return done(d, 8, 61);
    }

    /** A stone planter with a round little tree in it. */
    static Sprite planter() {
        Doll d = new Doll(34, 56);
        d.piece(TIMBER).rect(16, 26, 3, 20);                                 // the trunk
        d.piece(LEAVES).disc(17, 18, 12);
        d.join(LEAVES).disc(9, 23, 7).disc(25, 23, 7).disc(13, 10, 7).disc(22, 11, 7);
        for (int[] p : new int[][]{{10, 12}, {14, 8}, {20, 9}, {8, 20}, {24, 18}, {18, 15}}) d.dot(p[0], p[1], LEAVES, 3);
        for (int[] p : new int[][]{{12, 26}, {20, 27}, {26, 24}, {7, 26}}) d.dot(p[0], p[1], LEAVES, 0);
        d.piece(STONE).rect(5, 42, 24, 12);                                   // the planter
        d.piece(STONE).rect(3, 40, 28, 3);
        d.piece(TIMBER).rect(6, 43, 22, 1);                                   // the soil showing
        return done(d, 17, 54);
    }

    /** A park bench: wooden slats on curly iron legs. */
    static Sprite bench() {
        Doll d = new Doll(36, 22);
        d.piece(IRON).rect(4, 6, 2, 15).rect(30, 6, 2, 15);
        d.piece(TIMBER).rect(3, 3, 30, 3).rect(3, 7, 30, 3);                 // the back
        d.piece(TIMBER).rect(2, 13, 32, 4);                                   // the seat
        d.piece(IRON).rect(3, 17, 3, 4).rect(30, 17, 3, 4);
        for (int x = 4; x < 33; x += 6) d.dot(x, 14, TIMBER, 0);
        return done(d, 18, 21);
    }

    /** The square's fountain: a wide stone basin of water, a pedestal and a bowl spilling over. */
    static Sprite fountain() {
        Doll d = new Doll(92, 72);
        d.piece(STONE).ellipse(46, 52, 44, 17);                               // the basin's outer wall
        d.piece(WATER_M).ellipse(46, 50, 38, 12);                             // the water in it
        for (int[] p : new int[][]{{24, 47}, {30, 52}, {62, 46}, {58, 54}, {40, 56}, {70, 51}}) { d.dot(p[0], p[1], WATER_M, 3); d.dot(p[0] + 1, p[1], WATER_M, 2); }
        d.piece(STONE).rect(41, 26, 10, 24);                                  // the pedestal
        d.piece(STONE).ellipse(46, 26, 16, 6);                                // the bowl
        d.piece(WATER_M).ellipse(46, 25, 12, 3.5);
        d.piece(WATER_M).rect(30, 27, 2, 16).rect(60, 27, 2, 16);             // water spilling over its rim
        d.piece(STONE).rect(43, 10, 6, 14);                                   // the spout's column, a lamp on top
        d.piece(GOLDEN).disc(46, 8, 4);
        for (int y = 30; y < 44; y += 3) { d.dot(31, y, WATER_M, 3); d.dot(61, y + 1, WATER_M, 3); }
        d.dot(45, 6, GOLDEN.high());
        return done(d, 46, 69);
    }

    private static final Doll.Mat GOLDEN = Doll.GOLD;

    /** A red fire hydrant. */
    static Sprite hydrant() {
        Doll d = new Doll(14, 20);
        d.piece(HYDRANT).rect(4, 6, 6, 12);
        d.piece(HYDRANT).ellipse(7, 5.5, 3.5, 3);
        d.piece(HYDRANT).rect(1, 10, 12, 3);
        d.piece(GOLDEN).rect(6, 1, 2, 2);
        d.piece(HYDRANT).rect(3, 17, 8, 2);
        return done(d, 7, 19);
    }

    /** Wooden crates, two stacked on one. */
    static Sprite crates() {
        Doll d = new Doll(36, 34);
        crate(d, 2, 15, 18);
        crate(d, 18, 17, 16);
        crate(d, 9, 1, 16);
        return done(d, 18, 33);
    }

    private static void crate(Doll d, int x, int y, int s) {
        d.piece(TIMBER).rect(x, y, s, s);
        d.piece(new Doll.Mat(TIMBER.line(), rgb(96, 64, 40), rgb(130, 92, 58), rgb(160, 118, 76), rgb(186, 144, 96), false)).rect(x + 2, y + 2, s - 4, s - 4);
        for (int i = 2; i < s - 2; i++) d.dot(x + i, y + i, TIMBER.mid());     // the brace across it
        for (int i = 2; i < s - 2; i++) d.dot(x + i + 1, y + i, TIMBER.dark());
    }

    private static Sprite barrels() {
        Doll d = new Doll(30, 24);
        for (int i = 0; i < 2; i++) {
            int x = 2 + i * 13;
            d.piece(TIMBER).rect(x, 4, 12, 18);
            d.piece(TIMBER).ellipse(x + 6, 4.5, 6, 2.5);
            d.piece(IRON).rect(x, 8, 12, 1).rect(x, 17, 12, 1);
        }
        return done(d, 15, 23);
    }

    private static Sprite bin() {
        Doll d = new Doll(20, 24);
        d.piece(IRON).rect(3, 6, 14, 16);
        d.piece(IRON).rect(1, 4, 18, 3);
        d.piece(IRON).rect(8, 1, 4, 3);
        for (int x = 5; x < 16; x += 3) for (int y = 9; y < 20; y++) d.dot(x, y, IRON, 0);
        return done(d, 10, 23);
    }

    private static Sprite railing() {
        Doll d = new Doll(40, 18);
        d.piece(IRON).rect(0, 3, 40, 2);
        for (int x = 1; x < 40; x += 4) d.piece(IRON).rect(x, 3, 1, 14);
        for (int x = 1; x < 40; x += 4) d.dot(x, 2, IRON.light());
        return done(d, 20, 17);
    }

    /** A greengrocer's handcart: two big wheels, a load of apples and cabbages. */
    static Sprite cart() {
        Doll d = new Doll(54, 38);
        d.piece(TIMBER).rect(50, 18, 4, 2).rect(44, 16, 8, 2);               // the handles
        d.piece(TIMBER).rect(4, 14, 42, 12);                                  // the bed
        d.piece(HYDRANT).disc(10, 12, 4).disc(17, 11, 4).disc(13, 7, 3.5);    // apples
        d.piece(LEAVES).disc(28, 11, 5).disc(37, 12, 4.5);                    // cabbages
        d.piece(AWN_GOLD).disc(23, 13, 3).disc(42, 13, 2.5);                  // lemons
        for (int x = 6; x < 45; x += 5) d.dot(x, 20, TIMBER, 0);
        d.piece(TIMBER).disc(12, 30, 7).disc(38, 30, 7);                      // the wheels
        d.piece(IRON).disc(12, 30, 2).disc(38, 30, 2);
        for (int[] p : new int[][]{{12, 25}, {12, 35}, {7, 30}, {17, 30}, {38, 25}, {38, 35}, {33, 30}, {43, 30}}) d.dot(p[0], p[1], TIMBER, 0);
        return done(d, 27, 37);
    }

    /** A market stall: four posts under a striped canopy, a counter piled with something (0 fruit, 1 fish, 2 pots). */
    static Sprite stall(Doll.Mat canopy, int wares) {
        Doll d = new Doll(66, 60);
        d.piece(TIMBER).rect(6, 14, 3, 44).rect(57, 14, 3, 44);                // the posts
        d.piece(TIMBER).rect(4, 34, 58, 18);                                   // the counter
        d.piece(TIMBER).rect(3, 32, 60, 3);
        for (int x = 8; x < 60; x += 7) for (int y = 37; y < 51; y++) d.dot(x, y, TIMBER, 0);
        Random r = new Random(wares * 31L + 5);
        if (wares == 0) {                                                       // fruit
            for (int i = 0; i < 9; i++) d.piece(i % 3 == 0 ? AWN_GOLD : i % 3 == 1 ? HYDRANT : LEAVES).disc(10 + i * 5.5, 30 - r.nextInt(3), 2.8);
        } else if (wares == 1) {                                                // fish on ice
            d.piece(CLOTH_WHITE).rect(8, 29, 50, 4);
            for (int i = 0; i < 6; i++) { d.piece(STONE).ellipse(13 + i * 8, 28, 4, 1.6); d.piece(STONE).tri(16 + i * 8, 28, 19 + i * 8, 26, 19 + i * 8, 30); }
        } else {                                                                // pots and jugs
            for (int i = 0; i < 6; i++) {
                double x = 11 + i * 8.5;
                d.piece(ROOF_TILE).ellipse(x, 28, 3.6, 4).rect((int) x - 1, 22, 3, 3);
            }
        }
        d.piece(canopy).poly(new int[]{2, 64, 60, 6}, new int[]{14, 14, 2, 2});   // the canopy, sloping back
        for (int x = 3; x < 64; x += 6) d.set(x, 15).set(x + 1, 15).set(x + 2, 16);
        for (int x = 8; x < 60; x += 10) for (int y = 3; y < 15; y++) {
            int xx = x + (14 - y) / 3;
            d.dot(xx, y, CLOTH_WHITE.mid()); d.dot(xx + 1, y, CLOTH_WHITE.light()); d.dot(xx + 2, y, CLOTH_WHITE.mid()); d.dot(xx + 3, y, CLOTH_WHITE.dark());
        }
        return done(d, 33, 58);
    }

    /** The clock tower: a stone shaft, its clock stopped at five to midnight, a slate spire on top. */
    static Sprite clockTower() {
        Doll d = new Doll(52, 156);
        d.piece(ROOF_SLATE).tri(26, 0, 6, 38, 46, 38);                         // the spire
        for (int y = 12; y < 38; y += 4) for (int x = 26 - (y * 20 / 38) + 1; x < 26 + (y * 20 / 38); x += 2) d.dot(x, y, ROOF_SLATE, 0);
        d.piece(GOLDEN).rect(25, 0, 2, 3);
        d.piece(STONE).rect(8, 38, 36, 104);                                   // the shaft
        d.piece(STONE).rect(5, 36, 42, 4).rect(5, 98, 42, 4).rect(4, 140, 44, 14);   // its bands and footing
        for (int y = 44; y < 140; y += 6) for (int x = 9; x < 43; x++) if (y != 98 && y != 99) d.dot(x, y, STONE, 0);   // courses of stone
        for (int y = 44; y < 140; y += 6) for (int x = 12 + (y / 6) % 2 * 6; x < 43; x += 12) for (int k = 1; k < 6; k++) d.dot(x, y + k, STONE, 0);
        d.piece(CREAM).disc(26, 60, 12);                                       // the clock face
        d.piece(IRON).rect(25, 50, 2, 11);                                     // the minute hand, almost at twelve
        d.piece(IRON).line(26, 60, 23, 52);                                    // the hour hand
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            d.dot((int) Math.round(26 + Math.sin(a) * 10), (int) Math.round(60 - Math.cos(a) * 10), IRON.mid());
        }
        d.piece(WIN_DARK).rect(20, 108, 12, 22);                                // the door
        d.piece(WIN_LIT).rect(22, 76, 8, 12);                                   // a lit window above it
        for (int y = 77; y < 88; y++) d.dot(26, y, WIN_LIT.line());
        return done(d, 26, 154);
    }

    /** A heap of scrap from Juno's workshop: sheets of metal, a pipe, gears. */
    static Sprite scrap() {
        Doll d = new Doll(52, 34);
        d.piece(IRON).poly(new int[]{2, 16, 26, 10}, new int[]{32, 14, 32, 32});
        d.piece(CONCRETE).poly(new int[]{16, 34, 46, 24}, new int[]{32, 8, 32, 32});
        d.piece(COPPER).rect(6, 22, 30, 4);                                     // a copper pipe
        d.piece(STONE).disc(38, 22, 8);                                         // a big gear
        d.piece(IRON).disc(38, 22, 3);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            d.piece(STONE).rect((int) Math.round(37 + Math.cos(a) * 9), (int) Math.round(21 + Math.sin(a) * 9), 3, 3);
        }
        d.piece(COPPER).disc(14, 28, 4);
        d.piece(IRON).disc(14, 28, 1.5);
        return done(d, 26, 33);
    }

    /** An electricity pylon: a transformer box on legs, insulators, cables sagging away. */
    static Sprite pylon() {
        Doll d = new Doll(40, 66);
        d.piece(IRON).rect(8, 30, 3, 34).rect(29, 30, 3, 34);                   // the legs
        d.piece(IRON).line(10, 40, 30, 58).line(30, 40, 10, 58);                 // their bracing
        d.piece(CONCRETE).rect(5, 14, 30, 18);                                   // the transformer
        for (int x = 9; x < 33; x += 4) d.piece(CONCRETE).rect(x, 15, 2, 16);   // its cooling fins
        d.piece(AWN_GOLD).tri(16, 26, 20, 18, 24, 26);                          // a warning sign
        d.dot(20, 22, OUTLINE); d.dot(20, 24, OUTLINE);
        for (int x : new int[]{9, 20, 31}) { d.piece(CREAM).rect(x - 1, 6, 3, 8); d.piece(COPPER).rect(x - 2, 4, 5, 2); }   // insulators
        return done(d, 20, 64);
    }

    /** A wooden signpost with its boards pointing down the road. */
    static Sprite signpost() {
        Doll d = new Doll(34, 52);
        d.piece(TIMBER).rect(15, 6, 4, 45);
        d.piece(PLASTER).poly(new int[]{3, 28, 33, 28, 3}, new int[]{8, 8, 12, 16, 16});    // a board pointing right
        d.piece(PLASTER).poly(new int[]{6, 31, 31, 6, 1}, new int[]{20, 20, 28, 28, 24});   // and one pointing left
        for (int x = 7; x < 26; x += 3) { d.dot(x, 12, TIMBER.mid()); d.dot(x + 4, 24, TIMBER.mid()); }   // letters, too small to read
        d.piece(TIMBER).rect(13, 49, 8, 2);
        return done(d, 17, 50);
    }

    /**
     * A relay in the substation: a steel cabinet on a concrete pad, a big coil on top and a lamp. Off, the Blight's vines
     * are wrapped round it and its lamp is dark; on, the lamp burns and the coil glows. {@code fr} flickers the light.
     */
    static Sprite relay(boolean on, int fr) {
        Doll d = new Doll(46, 64);
        d.piece(CONCRETE).rect(2, 52, 42, 10);                                   // the pad
        d.piece(CONCRETE).rect(0, 50, 46, 4);
        d.piece(IRON).rect(8, 24, 30, 28);                                       // the cabinet
        d.piece(STONE).rect(11, 28, 11, 20).rect(24, 28, 11, 20);                // its doors
        d.dot(21, 38, AWN_GOLD.light()); d.dot(25, 38, AWN_GOLD.light());
        d.piece(AWN_GOLD).tri(13, 34, 16, 29, 19, 34);                           // the warning sign
        d.piece(COPPER).rect(17, 10, 12, 14);                                     // the coil
        for (int y = 11; y < 24; y += 2) for (int x = 17; x < 29; x++) d.dot(x, y, on && fr == 0 ? COPPER.high() : COPPER.dark());
        d.piece(CREAM).rect(21, 4, 4, 6);                                         // the insulator and its lamp
        d.piece(on ? WIN_LIT : WIN_DARK).disc(23, 3.5, 3.2);
        if (on) { d.dot(22, 2, WIN_LIT.high()); d.dot(23, 2, WIN_LIT.high()); }
        if (!on) {                                                                // the Blight, wrapped round it
            d.piece(BLIGHT).thickLine(6, 52, 12, 30, 2).thickLine(12, 30, 30, 22, 2).thickLine(30, 22, 40, 44, 2).thickLine(40, 44, 34, 52, 2);
            d.join(BLIGHT).thickLine(16, 24, 22, 12, 2).thickLine(36, 30, 42, 26, 1);
            for (int[] p : new int[][]{{12, 30}, {30, 22}, {40, 44}, {22, 12}}) d.dot(p[0], p[1] - 1, rgb(255, 120, 200));
            d.dot(9 + fr * 2, 44, BLIGHT.high());
        }
        return done(d, 23, 61);
    }
}
