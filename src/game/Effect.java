package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Short-lived visuals: sword slashes, lightning, fire blasts, particles, damage numbers, pulses and afterimages.
 * Most of them are sprites from {@link FxArt}.
 */
final class Effect {
    enum Kind { RING, SLASH, BOLT, TEXT, SPARK, GHOST, PARTICLE, EXPLOSION }

    private static final Font TEXT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font BIG_TEXT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 21);

    final Kind kind;
    final Color color;
    final double maxLife;
    double x, y, vx, vy, life;
    double r0, r1;              // RING start / end radius, SPARK size (r0), EXPLOSION radius (r1)
    boolean filled;             // RING
    double angle, half, reach;  // SLASH / GHOST (angle)
    Sprite boltSprite;          // BOLT
    double[] px, py;            // BOLT polyline (world coordinates)
    String text;
    boolean big;
    Sprite[] frames;            // PARTICLE / GHOST
    double fps, scale = Art.SCALE, drag = 1, rise, spin;
    int fixedFrame = -1;        // PARTICLE: -1 plays the frames as an animation; otherwise always shows this one
    boolean flip;

    private Effect(Kind kind, double x, double y, double life, Color color) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.life = life;
        this.maxLife = life;
        this.color = color;
    }

    static Effect ring(double x, double y, double r0, double r1, double life, Color c, boolean filled) {
        Effect e = new Effect(Kind.RING, x, y, life, c);
        e.r0 = r0;
        e.r1 = r1;
        e.filled = filled;
        return e;
    }

    static Effect slash(double x, double y, double angle, double half, double reach, Color c) {
        Effect e = new Effect(Kind.SLASH, x, y, 0.2, c);
        e.angle = angle;
        e.half = half;
        e.reach = reach;
        return e;
    }

    /** Builds a jagged lightning line through the given control points. */
    static Effect bolt(double[] xs, double[] ys, Color c, Random rng) {
        int total = 1;
        int[] pieces = new int[xs.length - 1];
        for (int i = 0; i < pieces.length; i++) {
            double len = Util.dist(xs[i], ys[i], xs[i + 1], ys[i + 1]);
            pieces[i] = Math.max(2, (int) (len / 26));
            total += pieces[i];
        }
        double[] px = new double[total];
        double[] py = new double[total];
        int k = 0;
        px[k] = xs[0];
        py[k++] = ys[0];
        for (int i = 0; i < pieces.length; i++) {
            double dx = xs[i + 1] - xs[i], dy = ys[i + 1] - ys[i];
            double len = Math.max(1, Math.hypot(dx, dy));
            double nx = -dy / len, ny = dx / len;
            for (int j = 1; j <= pieces[i]; j++) {
                double t = (double) j / pieces[i];
                double off = j == pieces[i] ? 0 : (rng.nextDouble() - 0.5) * 22;
                px[k] = xs[i] + dx * t + nx * off;
                py[k++] = ys[i] + dy * t + ny * off;
            }
        }
        Effect e = new Effect(Kind.BOLT, xs[0], ys[0], 0.24, c);
        e.px = px;
        e.py = py;
        e.boltSprite = FxArt.bolt(px, py);
        return e;
    }

    static Effect text(double x, double y, String text, Color c, boolean big) {
        Effect e = new Effect(Kind.TEXT, x, y, big ? 0.9 : 0.7, c);
        e.text = text;
        e.big = big;
        e.vy = -55;
        return e;
    }

    /** A pixel-square spark that flies out and fades. */
    static Effect spark(double x, double y, double angle, double speed, double size, double life, Color c) {
        Effect e = new Effect(Kind.SPARK, x, y, life, c);
        e.vx = Math.cos(angle) * speed;
        e.vy = Math.sin(angle) * speed;
        e.r0 = size;
        return e;
    }

    /** The dodge / dash afterimage: the hero's silhouette in a colour, fading out. */
    static Effect ghost(double x, double y, double angle, Color c) {
        Effect e = new Effect(Kind.GHOST, x, y, 0.24, c);
        e.angle = angle;
        e.frames = Art.frames("hero." + PeopleArt.heroDir(angle) + ".walk");
        e.flip = PeopleArt.heroFlip(angle);
        return e;
    }

    /**
     * A sprite that drifts: leaves, scrap, plus signs, dust puffs, stars. It plays its frames as an animation over its
     * life ({@code fixedFrame} = -1) or always shows one frame (a variant, like a green or an orange leaf). It slows by
     * {@code drag} per second (1 = never) and floats up at {@code rise} units per second (negative = falls).
     */
    static Effect particle(double x, double y, double vx, double vy, double life, String sprite, int fixedFrame, double drag, double rise, double scale) {
        Effect e = new Effect(Kind.PARTICLE, x, y, life, Color.WHITE);
        e.vx = vx;
        e.vy = vy;
        e.frames = Art.frames(sprite);
        e.fixedFrame = fixedFrame;
        e.drag = drag;
        e.rise = rise;
        e.scale = scale;
        return e;
    }

    /** A blast: an animated ragged fire disc of the given radius. */
    static Effect explosion(double x, double y, double radius) {
        Effect e = new Effect(Kind.EXPLOSION, x, y, 0.42, Color.WHITE);
        e.r1 = radius;
        e.frames = FxArt.explosion(radius);
        return e;
    }

    /** @return false once the effect has finished. */
    boolean update(double dt) {
        life -= dt;
        switch (kind) {
            case TEXT -> {
                y += vy * dt;
                vy *= Math.exp(-3 * dt);
            }
            case SPARK -> {
                x += vx * dt;
                y += vy * dt;
                double d = Math.exp(-5 * dt);
                vx *= d;
                vy *= d;
            }
            case PARTICLE -> {
                x += vx * dt;
                y += (vy - rise) * dt;
                double d = Math.pow(drag, dt);
                vx *= d;
                vy *= d;
            }
            default -> { }
        }
        return life > 0;
    }

    void render(Graphics2D g) {
        double f = Util.clamp(life / maxLife, 0, 1); // 1 -> 0 over the lifetime
        double p = 1 - f;
        switch (kind) {
            case RING -> {
                double r = r0 + (r1 - r0) * (1 - f * f);
                Ellipse2D circle = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
                if (filled) {
                    g.setColor(Util.alpha(color, 0.28 * f));
                    g.fill(circle);
                }
                g.setColor(Util.alpha(color, f));
                g.setStroke(new BasicStroke(4f * (float) f + 1f));
                g.draw(circle);
            }
            case SLASH -> {
                Sprite[] s = FxArt.slash(reach, half > 1.6);
                s[Math.min(2, (int) (p * 3))].draw(g, x, y, Art.SCALE, false, angle, (float) Math.min(1, f * 2.2 + 0.25));
            }
            case BOLT -> boltSprite.draw(g, 0, 0, Art.SCALE, false, 0, (float) Math.min(1, f * 1.6));
            case EXPLOSION -> frames[Math.min(frames.length - 1, (int) (p * frames.length))].draw(g, x, y, Art.SCALE, false, 0, (float) Math.min(1, f * 2.5 + 0.2));
            case PARTICLE -> {
                Sprite s = fixedFrame >= 0 ? frames[fixedFrame] : frames[Math.min(frames.length - 1, (int) (p * frames.length))];
                s.draw(g, x, y, scale, false, 0, (float) Math.min(1, f * 2.2));
            }
            case TEXT -> {
                float a = (float) Math.min(1, f * 2);
                if (PixelFont.canDraw(text)) {
                    PixelFont.draw(g, text, x, y - 15, big ? 4 : 3, color, a);
                } else {
                    g.setFont(big ? BIG_TEXT_FONT : TEXT_FONT);
                    FontMetrics fm = g.getFontMetrics();
                    float tx = (float) (x - fm.stringWidth(text) / 2.0);
                    g.setColor(new Color(0, 0, 0, (int) (200 * a)));
                    g.drawString(text, tx + 1.5f, (float) y + 1.5f);
                    g.setColor(Util.alpha(color, a));
                    g.drawString(text, tx, (float) y);
                }
            }
            case SPARK -> {
                int side = Math.max(3, (int) Math.round(r0 / 1.5 / 3) * 3);
                g.setColor(Util.alpha(color, f));
                g.fillRect((int) Math.round(x / 3) * 3 - side / 2, (int) Math.round(y / 3) * 3 - side / 2, side, side);
            }
            case GHOST -> {
                Sprite s = frames[0];
                s.drawSilhouette(g, x, y + 12, Art.SCALE, flip, color.getRGB(), (float) (0.55 * f));
            }
        }
    }
}
