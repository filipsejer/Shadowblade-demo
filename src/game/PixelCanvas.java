package game;

import java.awt.image.BufferedImage;

/**
 * A tiny software canvas for painting pixel art in code: no anti-aliasing, every pixel is set on purpose.
 * Colours are packed ARGB ints (see {@link #rgb}). The primitives (ellipse, line, triangle...) all snap to whole pixels,
 * and {@link #outline} / {@link #bevel} give sprites their dark outline and a little top-left light.
 */
final class PixelCanvas {
    final int w, h;
    final int[] px;

    PixelCanvas(int w, int h) {
        this.w = w;
        this.h = h;
        this.px = new int[w * h];
    }

    // ------------------------------------------------------------------ colours

    static int rgb(int r, int g, int b) { return 0xFF000000 | (r & 255) << 16 | (g & 255) << 8 | (b & 255); }

    static int rgba(int r, int g, int b, int a) { return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255); }

    static int alphaOf(int c) { return c >>> 24; }

    /** Blend two colours (alpha included). t = 0 gives a, t = 1 gives b. */
    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (int) Math.round((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return aa << 24 | r << 16 | g << 8 | bl;
    }

    /** Move the colour toward white / black, keeping its own alpha. */
    static int lighten(int c, double t) { return mix(c, (c & 0xFF000000) | 0xFFFFFF, t); }

    static int darken(int c, double t) { return mix(c, c & 0xFF000000, t); }

    static int withAlpha(int c, int a) { return (a & 255) << 24 | (c & 0xFFFFFF); }

    // ------------------------------------------------------------------ pixels

    boolean in(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }

    void set(int x, int y, int c) { if (in(x, y)) px[y * w + x] = c; }

    int get(int x, int y) { return in(x, y) ? px[y * w + x] : 0; }

    boolean solid(int x, int y) { return in(x, y) && (px[y * w + x] >>> 24) > 0; }

    /** Paints over what's there, blending if the new colour is translucent. */
    void blend(int x, int y, int c) {
        if (!in(x, y)) return;
        int a = c >>> 24;
        if (a == 255) { px[y * w + x] = c; return; }
        if (a == 0) return;
        int below = px[y * w + x];
        if ((below >>> 24) == 0) { px[y * w + x] = c; return; }
        int out = mix(below, withAlpha(c, 255), a / 255.0);
        px[y * w + x] = withAlpha(out, Math.max(below >>> 24, a));
    }

    // ------------------------------------------------------------------ shapes

    void rect(int x, int y, int rw, int rh, int c) {
        for (int j = 0; j < rh; j++) for (int i = 0; i < rw; i++) set(x + i, y + j, c);
    }

    /** Filled ellipse; (cx, cy) is the centre in pixel coordinates (0.5 = the middle of pixel 0). */
    void ellipse(double cx, double cy, double rx, double ry, int c) { ellipseY(cx, cy, rx, ry, c, -1e9, 1e9); }

    /** Like {@link #ellipse} but only the rows from y0 up to (not including) y1 are painted. */
    void ellipseY(double cx, double cy, double rx, double ry, int c, double y0, double y1) {
        int minX = (int) Math.floor(cx - rx), maxX = (int) Math.ceil(cx + rx);
        int minY = (int) Math.floor(cy - ry), maxY = (int) Math.ceil(cy + ry);
        for (int y = minY; y <= maxY; y++) {
            if (y < y0 || y >= y1) continue;
            for (int x = minX; x <= maxX; x++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) set(x, y, c);
            }
        }
    }

    void disc(double cx, double cy, double r, int c) { ellipse(cx, cy, r, r, c); }

    /** Bresenham line, one pixel wide. */
    void line(int x0, int y0, int x1, int y1, int c) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            set(x0, y0, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** A line made thicker by stamping a small square at every step. */
    void thickLine(int x0, int y0, int x1, int y1, int thickness, int c) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int lo = -(thickness - 1) / 2, hi = thickness / 2;
        while (true) {
            for (int j = lo; j <= hi; j++) for (int i = lo; i <= hi; i++) set(x0 + i, y0 + j, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** Filled triangle. */
    void tri(int x0, int y0, int x1, int y1, int x2, int y2, int c) {
        int minX = Math.min(x0, Math.min(x1, x2)), maxX = Math.max(x0, Math.max(x1, x2));
        int minY = Math.min(y0, Math.min(y1, y2)), maxY = Math.max(y0, Math.max(y1, y2));
        double area = (double) (x1 - x0) * (y2 - y0) - (double) (x2 - x0) * (y1 - y0);
        if (area == 0) { line(x0, y0, x1, y1, c); line(x1, y1, x2, y2, c); return; }
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px0 = x + 0.5, py0 = y + 0.5;
                double w0 = ((x1 - px0) * (y2 - py0) - (x2 - px0) * (y1 - py0)) / area;
                double w1 = ((x2 - px0) * (y0 - py0) - (x0 - px0) * (y2 - py0)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 >= -1e-9 && w1 >= -1e-9 && w2 >= -1e-9) set(x, y, c);
            }
        }
    }

    /** Fills a polygon by splitting it into triangles from its first point (fine for convex-ish shapes). */
    void poly(int[] xs, int[] ys, int c) {
        for (int i = 1; i + 1 < xs.length; i++) tri(xs[0], ys[0], xs[i], ys[i], xs[i + 1], ys[i + 1], c);
    }

    // ------------------------------------------------------------------ whole-image passes

    /** Puts a 1-pixel outline in the transparent pixels touching a painted one (up / down / left / right). */
    void outline(int c) {
        int[] out = px.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((px[y * w + x] >>> 24) != 0) continue;
                if (solid(x - 1, y) || solid(x + 1, y) || solid(x, y - 1) || solid(x, y + 1)) out[y * w + x] = c;
            }
        }
        System.arraycopy(out, 0, px, 0, px.length);
    }

    /**
     * A little light from the top-left: painted pixels on a top or left edge are lightened, and ones on a bottom or
     * right edge darkened. Call it before {@link #outline}.
     */
    void bevel(double hi, double lo) {
        int[] out = px.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                if ((c >>> 24) == 0) continue;
                boolean top = !solid(x, y - 1) || !solid(x - 1, y);
                boolean bottom = !solid(x, y + 1) || !solid(x + 1, y);
                if (top && !bottom) out[y * w + x] = lighten(c, hi);
                else if (bottom && !top) out[y * w + x] = darken(c, lo);
            }
        }
        System.arraycopy(out, 0, px, 0, px.length);
    }

    /** Draws another canvas on top of this one with its top-left corner at (dx, dy). */
    void paste(PixelCanvas src, int dx, int dy) {
        for (int y = 0; y < src.h; y++) for (int x = 0; x < src.w; x++) blend(dx + x, dy + y, src.px[y * src.w + x]);
    }

    PixelCanvas flippedX() {
        PixelCanvas o = new PixelCanvas(w, h);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) o.px[y * w + (w - 1 - x)] = px[y * w + x];
        return o;
    }

    PixelCanvas copy() {
        PixelCanvas o = new PixelCanvas(w, h);
        System.arraycopy(px, 0, o.px, 0, px.length);
        return o;
    }

    /** Number of painted (non-transparent) pixels. */
    int paintedCount() {
        int n = 0;
        for (int c : px) if ((c >>> 24) != 0) n++;
        return n;
    }

    BufferedImage image() {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, px, 0, w);
        return img;
    }
}
