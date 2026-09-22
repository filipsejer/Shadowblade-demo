package game;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * One frame of pixel art plus an anchor: the pixel of the image that lands on the position you draw it at (usually
 * the character's feet). Frames are drawn scaled up with nearest-neighbour so the pixels stay crisp.
 */
final class Sprite {
    final BufferedImage img;
    final int w, h;
    final int ax, ay;     // anchor, in image pixels
    private final Map<Integer, BufferedImage> silhouettes = new HashMap<>();

    Sprite(PixelCanvas canvas, int ax, int ay) {
        this.img = canvas.image();
        this.w = canvas.w;
        this.h = canvas.h;
        this.ax = ax;
        this.ay = ay;
    }

    /** Draws with the anchor at world (x, y). {@code flip} mirrors it left-right around the anchor. */
    void draw(Graphics2D g, double x, double y, double scale, boolean flip) {
        draw(g, x, y, scale, flip, 0, 1f);
    }

    void draw(Graphics2D g, double x, double y, double scale, boolean flip, double angle, float alpha) {
        drawImage(g, img, x, y, scale, flip, angle, alpha);
    }

    /** Draws the sprite as a single flat colour (keeping its shape): hit flashes, ghosts, status tints. */
    void drawSilhouette(Graphics2D g, double x, double y, double scale, boolean flip, int argb, float alpha) {
        drawImage(g, silhouette(argb), x, y, scale, flip, 0, alpha);
    }

    /** The sprite's shape filled with one colour (cached). */
    BufferedImage silhouette(int argb) {
        return silhouettes.computeIfAbsent(argb | 0xFF000000, key -> {
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int a = img.getRGB(i, j) >>> 24;
                    if (a != 0) out.setRGB(i, j, a << 24 | (key & 0xFFFFFF));
                }
            }
            return out;
        });
    }

    private void drawImage(Graphics2D g, BufferedImage image, double x, double y, double scale, boolean flip, double angle, float alpha) {
        AffineTransform saved = g.getTransform();
        java.awt.Composite savedComposite = g.getComposite();
        g.translate(Math.round(x), Math.round(y));
        if (angle != 0) g.rotate(angle);
        g.scale(flip ? -scale : scale, scale);
        if (alpha < 1f) g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
        g.drawImage(image, flip ? -ax - 1 : -ax, -ay, null);     // flipped: the anchor pixel stays in the same cell, so turning doesn't shift the sprite
        g.setComposite(savedComposite);
        g.setTransform(saved);
    }
}
