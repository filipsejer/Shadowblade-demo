package game;

import static game.PixelCanvas.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Paints a character the way the drawn sprites ({@link ImportedArt}) are shaded, so painted people stand among the
 * drawn monsters as if they came from the same hand. Shapes aren't drawn in colours but in {@link Mat materials}
 * (skin, hair, cloth, steel...), each part a separate piece laid over the ones before it. {@link #render} then shades
 * every piece from the top left in its material's tones (a lit top and left edge, a shaded bottom and right, a glint
 * on the round ones), darkens what lies just under a piece in front of it (the shadow of the head on the collar), runs
 * a dark line around each piece where it overlaps another, and finally outlines the whole figure in near-black.
 * Faces and other small details are placed on top as exact pixels ({@link #dot}).
 *
 * <p>Coordinates are in the doll's own pixels; the shapes are {@link PixelCanvas}'s, so they come out exactly as they would there.
 */
final class Doll {
    /**
     * A material's tones, darkest to brightest: the line drawn where a piece overlaps another, its shade, its body
     * colour, its lit edge and its glint. {@code round} materials (hair, skin, steel) get the glint; flat cloth doesn't.
     */
    record Mat(int line, int dark, int mid, int light, int high, boolean round) {
        int tone(int t) { return switch (t) { case 0 -> dark; case 1 -> mid; case 2 -> light; default -> high; }; }
    }

    /** The near-black, slightly warm outline of the drawn sprites. */
    static final int OUTLINE = rgb(22, 12, 12);

    // ------------------------------------------------------------------ the shared palette (hue-shifted ramps: cool shade, warm light)

    static final Mat SKIN = new Mat(rgb(128, 58, 52), rgb(212, 136, 112), rgb(242, 186, 150), rgb(255, 216, 182), rgb(255, 236, 214), true);
    static final Mat HAIR_BROWN = new Mat(rgb(50, 24, 18), rgb(92, 48, 30), rgb(134, 80, 44), rgb(180, 120, 62), rgb(222, 168, 96), true);
    static final Mat TUNIC_BLUE = new Mat(rgb(18, 26, 86), rgb(36, 62, 172), rgb(60, 108, 232), rgb(112, 162, 255), rgb(170, 208, 255), false);
    static final Mat PANTS = new Mat(rgb(22, 18, 38), rgb(44, 40, 70), rgb(66, 62, 102), rgb(94, 90, 134), rgb(122, 118, 164), false);
    static final Mat LEATHER = new Mat(rgb(42, 20, 12), rgb(76, 42, 26), rgb(110, 66, 40), rgb(148, 98, 60), rgb(184, 132, 82), true);
    static final Mat GOLD = new Mat(rgb(96, 54, 10), rgb(182, 122, 28), rgb(234, 182, 58), rgb(252, 222, 108), rgb(255, 248, 196), true);
    static final Mat STEEL = new Mat(rgb(48, 56, 84), rgb(122, 136, 170), rgb(186, 198, 220), rgb(226, 234, 248), rgb(255, 255, 255), true);
    static final Mat WOOD = new Mat(rgb(48, 26, 14), rgb(96, 60, 34), rgb(136, 90, 52), rgb(176, 124, 74), rgb(206, 158, 100), false);

    /** Face details, shared by everyone so they all have the same face. */
    static final int EYE = rgb(30, 22, 44), EYE_WHITE = rgb(255, 255, 255), BLUSH = rgb(248, 140, 136), MOUTH = rgb(176, 90, 76);

    final int w, h;
    /** The pieces, each painted in a "colour" that is its number (later pieces are in front). */
    private final PixelCanvas mask;
    private final List<Mat> mats = new ArrayList<>();   // material of piece i is mats.get(i - 1)
    private final List<Integer> groups = new ArrayList<>();   // the shape piece i belongs to (itself, or an earlier piece it joined)
    private final int[] over, overPiece;              // exact detail pixels laid over the shading, and how many pieces existed then
    private int pieces;

    Doll(int w, int h) {
        this.w = w;
        this.h = h;
        mask = new PixelCanvas(w, h);
        over = new int[w * h];
        overPiece = new int[w * h];
    }

    // ------------------------------------------------------------------ pieces (drawn with PixelCanvas's own shapes)

    /** Starts a new piece of the given material: what's drawn until the next {@code piece} call belongs to it. */
    Doll piece(Mat m) {
        mats.add(m);
        pieces++;
        groups.add(pieces);
        return this;
    }

    /**
     * Starts a piece that merges into the last piece of the same material, as one shape with no seam between them
     * (a face drawn over the head, a fringe over the hair). Without one to join, it's a new piece.
     */
    Doll join(Mat m) {
        int into = 0;
        for (int i = mats.size() - 1; i >= 0 && into == 0; i--) if (mats.get(i) == m) into = groups.get(i);
        mats.add(m);
        pieces++;
        groups.add(into != 0 ? into : pieces);
        return this;
    }

    private int group(int piece) { return piece == 0 ? 0 : groups.get(piece - 1); }

    private int code() { return 0xFF000000 | pieces; }

    Doll rect(int x, int y, int rw, int rh) { mask.rect(x, y, rw, rh, code()); return this; }

    Doll ellipse(double cx, double cy, double rx, double ry) { mask.ellipse(cx, cy, rx, ry, code()); return this; }

    Doll ellipseY(double cx, double cy, double rx, double ry, double y0, double y1) { mask.ellipseY(cx, cy, rx, ry, code(), y0, y1); return this; }

    Doll disc(double cx, double cy, double r) { return ellipse(cx, cy, r, r); }

    Doll tri(int x0, int y0, int x1, int y1, int x2, int y2) { mask.tri(x0, y0, x1, y1, x2, y2, code()); return this; }

    Doll poly(int[] xs, int[] ys) { mask.poly(xs, ys, code()); return this; }

    Doll line(int x0, int y0, int x1, int y1) { mask.line(x0, y0, x1, y1, code()); return this; }

    Doll thickLine(int x0, int y0, int x1, int y1, int t) { mask.thickLine(x0, y0, x1, y1, t, code()); return this; }

    Doll set(int x, int y) { mask.set(x, y, code()); return this; }

    // ------------------------------------------------------------------ details

    /** An exact pixel on top of the shading (an eye, a buckle's glint, a strand of hair); a piece drawn later hides it. */
    Doll dot(int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < w && y < h) { over[y * w + x] = argb; overPiece[y * w + x] = pieces; }
        return this;
    }

    /** A pixel in one of a material's tones (0 shade ... 3 glint, -1 its line colour). */
    Doll dot(int x, int y, Mat m, int tone) { return dot(x, y, tone < 0 ? m.line : m.tone(tone)); }

    /** Removes everything at a pixel (to notch a shape). */
    Doll clear(int x, int y) {
        mask.set(x, y, 0);
        if (x >= 0 && y >= 0 && x < w && y < h) over[y * w + x] = 0;
        return this;
    }

    // ------------------------------------------------------------------ shading

    private int partAt(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h ? mask.get(x, y) & 0xFFFFFF : 0; }

    /** How many pixels you can go from (x, y) in a direction before leaving the shape (capped). */
    private int run(int x, int y, int dx, int dy, int g, int cap) {
        int n = 0;
        while (n < cap && group(partAt(x + dx * (n + 1), y + dy * (n + 1))) == g) n++;
        return n;
    }

    /** True if nothing's painted at the pixel: an edge there faces the open air (and catches the light). */
    private boolean open(int x, int y) { return partAt(x, y) == 0; }

    /** True if piece q is in front of piece p and isn't part of the same shape. */
    private boolean inFront(int q, int p) { return q > p && group(q) != group(p); }

    /** Shades, lines and outlines the figure into a canvas. */
    PixelCanvas render() {
        PixelCanvas c = new PixelCanvas(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = partAt(x, y);
                if (p == 0) continue;
                Mat m = mats.get(p - 1);
                int g = group(p);
                int up = run(x, y, 0, -1, g, 4), down = run(x, y, 0, 1, g, 4), left = run(x, y, -1, 0, g, 4), right = run(x, y, 1, 0, g, 4);
                int wide = left + right, tall = up + down;
                int t = 1;
                if (down == 0 || right == 0 && wide >= 3 || down == 1 && tall >= 6 || right == 1 && wide >= 7) t = 0;   // the side away from the light
                else if (up == 0 && open(x, y - 1) || left == 0 && wide >= 3 && open(x - 1, y)
                    || up == 1 && left <= 2 && tall >= 6 && wide >= 6 && open(x, y - 2)) t = 2;                         // the lit top and left: only where they face open air
                if (m.round && t == 2 && up <= 1 && left >= 1 && left <= 3 && right >= 2 && down >= 2) t = 3;             // a glint on round things
                int above = partAt(x, y - 1), above2 = partAt(x, y - 2);
                if (inFront(above, p) || inFront(above2, p) && tall >= 3) t = Math.min(t, 0);                     // in the shadow of a piece in front
                int col = m.tone(t);
                for (int[] d : new int[][]{{0, -1}, {-1, 0}, {1, 0}, {0, 1}}) {           // a piece in front of this one, touching it: a line
                    int q = partAt(x + d[0], y + d[1]);
                    if (inFront(q, p)) { col = mats.get(q - 1) == m ? m.dark : mats.get(q - 1).line; break; }
                }
                c.set(x, y, col);
            }
        }
        for (int i = 0; i < over.length; i++) if (over[i] != 0 && partAt(i % w, i / w) <= overPiece[i]) c.set(i % w, i / w, over[i]);
        c.outline(OUTLINE);
        return c;
    }
}
