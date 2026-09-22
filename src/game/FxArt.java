package game;

import static game.PixelCanvas.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Attack, projectile and particle sprites. The fixed ones are registered in the atlas; the ones whose size depends on
 * the attack (slash reach, blast radius, storm radius, the shape of a lightning bolt) are painted on demand at exactly
 * that size and cached, so their pixels always match everything else on screen.
 */
final class FxArt {
    private FxArt() {}

    private static final int WHITE = rgb(255, 255, 255);

    static void register(Map<String, Sprite[]> m) {
        m.put("fx.fireball", PeopleArt.frames(3, FxArt::fireball, 14, 5));
        m.put("fx.spark", PeopleArt.frames(3, FxArt::spark, 3, 3));
        m.put("fx.puff", PeopleArt.frames(4, FxArt::puff, 8, 8));
        m.put("fx.leaf", PeopleArt.frames(3, FxArt::leaf, 2, 2));
        m.put("fx.scrap", PeopleArt.frames(3, FxArt::scrap, 3, 3));
        m.put("fx.plus", PeopleArt.frames(2, FxArt::plus, 3, 3));
        m.put("fx.star", PeopleArt.frames(2, FxArt::star, 3, 3));
        m.put("fx.snow", PeopleArt.frames(2, FxArt::snow, 3, 3));
        m.put("fx.shard", PeopleArt.frames(2, FxArt::shard, 2, 4));
        m.put("fx.impact", PeopleArt.frames(3, FxArt::impact, 8, 8));
        m.put("proj.seed", PeopleArt.frames(2, FxArt::seed, 3, 3));
        m.put("proj.plasma", PeopleArt.frames(2, FxArt::plasma, 4, 4));
        m.put("proj.spore", PeopleArt.frames(2, FxArt::spore, 5, 5));
        m.put("fx.goo", PeopleArt.frames(3, FxArt::goo, 3, 3));
        m.put("proj.acid", PeopleArt.frames(2, FxArt::acid, 4, 4));
        m.put("proj.acidball", PeopleArt.frames(2, FxArt::acidBall, 5, 5));
        m.put("proj.boltball", PeopleArt.frames(2, FxArt::boltBall, 5, 5));
    }

    // ------------------------------------------------------------------ fixed sprites

    /** A comet of fire flying to the right; rotated to the direction of travel when drawn. */
    private static PixelCanvas fireball(int f) {
        PixelCanvas c = new PixelCanvas(19, 11);
        int deep = rgb(214, 52, 30), orange = rgb(255, 142, 40), yellow = rgb(255, 222, 92), core = rgb(255, 250, 214);
        c.ellipse(6.0 - f * 0.5, 5.5, 5.6, 2.7, deep);
        c.ellipse(7.5, 5.5 + (f == 1 ? -0.7 : f == 2 ? 0.7 : 0), 4.4, 2.0, orange);
        c.tri(0, 5, 3, 3 + f % 2, 3, 8 - f % 2, deep);
        c.set(2 + f, 2 + f % 2, orange); c.set(3, 9 - f % 2, orange);                       // stray embers
        c.disc(13.5, 5.5, 4.5, orange);
        c.disc(13.9, 5.5, 3.3, yellow);
        c.disc(14.3, 5.3, 1.8, core);
        c.outline(rgb(120, 22, 12));
        return c;
    }

    private static PixelCanvas spark(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int a = rgb(255, 250, 210), b = rgb(255, 214, 90);
        if (f == 1) {
            c.rect(3, 0, 1, 7, a); c.rect(0, 3, 7, 1, a); c.set(1, 1, b); c.set(5, 1, b); c.set(1, 5, b); c.set(5, 5, b);
        } else if (f == 0) {
            c.rect(3, 1, 1, 5, b); c.rect(1, 3, 5, 1, b); c.set(3, 3, a);
        } else {
            c.set(3, 2, b); c.set(2, 3, b); c.set(4, 3, b); c.set(3, 4, b);
        }
        return c;
    }

    /** A small cloud of dust or smoke that swells and fades. */
    private static PixelCanvas puff(int f) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int[] alpha = {235, 200, 150, 90};
        double[] r = {3.2, 4.8, 6.0, 7.0};
        int col = rgba(238, 238, 244, alpha[f]);
        c.disc(8.5, 8.5, r[f], col);
        c.disc(5.5 - f * 0.4, 9.5, r[f] * 0.55, col); c.disc(11.5 + f * 0.4, 9.0, r[f] * 0.55, col); c.disc(8.5, 5.5 - f * 0.3, r[f] * 0.5, col);
        return c;
    }

    private static PixelCanvas leaf(int f) {
        PixelCanvas c = new PixelCanvas(5, 5);
        int[] cols = {rgb(96, 190, 84), rgb(150, 222, 100), rgb(226, 150, 60)};
        int[] dark = {rgb(50, 120, 60), rgb(80, 150, 70), rgb(160, 84, 40)};
        c.tri(0, 4, 4, 0, 3, 3, cols[f]); c.tri(0, 4, 4, 0, 1, 1, cols[f]);
        c.rect(1, 1, 3, 3, cols[f]);
        c.set(0, 4, dark[f]); c.set(1, 3, dark[f]); c.set(2, 2, dark[f]);
        c.set(4, 0, lighten(cols[f], 0.4));
        return c;
    }

    /** Bits of a broken robot or a city critter: a gear, a bolt, a scrap of paper. */
    private static PixelCanvas scrap(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        if (f == 0) {
            int g = rgb(176, 182, 198), gd = rgb(104, 110, 130);
            c.disc(3.5, 3.5, 2.6, g); c.set(3, 0, g); c.set(3, 6, g); c.set(0, 3, g); c.set(6, 3, g);
            c.set(3, 3, 0); c.set(2, 2, gd); c.set(4, 4, gd);
        } else if (f == 1) {
            c.rect(2, 1, 3, 5, rgb(150, 156, 172)); c.rect(1, 0, 5, 2, rgb(196, 202, 216)); c.set(3, 3, rgb(90, 96, 114));
        } else {
            c.rect(1, 1, 5, 4, rgb(240, 238, 226)); c.set(5, 1, rgb(196, 194, 180)); c.rect(2, 2, 3, 1, rgb(170, 168, 190));
        }
        return c;
    }

    private static PixelCanvas plus(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int g = rgb(96, 236, 140), l = rgb(196, 255, 214);
        if (f == 0) { c.rect(2, 0, 3, 7, g); c.rect(0, 2, 7, 3, g); c.rect(3, 1, 1, 5, l); c.rect(1, 3, 5, 1, l); }
        else { c.rect(3, 1, 1, 5, g); c.rect(1, 3, 5, 1, g); c.set(3, 3, l); }
        c.outline(rgb(30, 110, 70));
        return c;
    }

    private static PixelCanvas star(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int y = rgb(255, 224, 90), w = rgb(255, 250, 200);
        if (f == 0) { c.rect(3, 0, 1, 7, y); c.rect(0, 3, 7, 1, y); c.rect(2, 2, 3, 3, y); c.set(3, 3, w); }
        else { c.rect(3, 1, 1, 5, y); c.rect(1, 3, 5, 1, y); c.set(3, 3, w); }
        return c;
    }

    private static PixelCanvas snow(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int w = rgb(236, 248, 255), b = rgb(150, 204, 250);
        c.rect(3, 0, 1, 7, w); c.rect(0, 3, 7, 1, w);
        c.set(1, 1, b); c.set(5, 1, b); c.set(1, 5, b); c.set(5, 5, b);
        if (f == 1) { c.set(2, 2, b); c.set(4, 4, b); c.set(4, 2, b); c.set(2, 4, b); } else c.set(3, 3, b);
        return c;
    }

    private static PixelCanvas shard(int f) {
        PixelCanvas c = new PixelCanvas(5, 9);
        int a = rgb(190, 230, 255), b = rgb(120, 184, 240), d = rgb(230, 248, 255);
        c.tri(2, 0, 0, 6, 4, 6, a);
        c.tri(0, 6, 4, 6, 2, 8 - f, b);
        c.rect(2, 1, 1, 5, d);
        c.outline(rgb(60, 110, 190));
        return c;
    }

    /** The flash where lightning strikes: a jagged star. */
    private static PixelCanvas impact(int f) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int y = rgb(255, 240, 120), w = rgb(255, 255, 240), b = rgb(150, 190, 255);
        int len = new int[]{5, 8, 6}[f];
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + f * 0.2;
            int l = i % 2 == 0 ? len : len - 3;
            c.thickLine(8, 8, (int) Math.round(8 + Math.cos(a) * l), (int) Math.round(8 + Math.sin(a) * l), 2, i % 2 == 0 ? y : b);
        }
        c.disc(8.5, 8.5, 2.6 + (f == 1 ? 1 : 0), w);
        return c;
    }

    /** A thorny seed, spat by the forest shooters. */
    private static PixelCanvas seed(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        c.disc(3.5, 3.5, 2.6, rgb(232, 220, 90)); c.disc(3.0, 3.0, 1.3, rgb(255, 252, 170));
        c.set(f == 0 ? 6 : 5, 3, rgb(226, 60, 70)); c.set(3, f == 0 ? 0 : 1, rgb(226, 60, 70)); c.set(0, 3, rgb(226, 60, 70));
        c.outline(rgb(120, 96, 30));
        return c;
    }

    /** A hot plasma ball, fired by the city drones. */
    private static PixelCanvas plasma(int f) {
        PixelCanvas c = new PixelCanvas(9, 9);
        c.disc(4.5, 4.5, 3.9 + f * 0.4, rgb(255, 84, 60));
        c.disc(4.5, 4.5, 2.7, rgb(255, 176, 90));
        c.disc(4.5, 4.5, 1.4, rgb(255, 252, 226));
        c.outline(rgb(140, 24, 30));
        return c;
    }

    /** The Guardian's bullets: glowing yellow spores with a green rim. */
    /** Bits of lab goo, spat out when a laboratory creature dies. */
    private static PixelCanvas goo(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int g = rgb(96, 220, 96), gl = rgb(190, 255, 170), gd = rgb(40, 130, 66);
        if (f == 0) { c.disc(3.5, 3.5, 2.6, g); c.set(2, 2, gl); c.set(3, 2, gl); c.set(5, 5, gd); }
        else if (f == 1) { c.ellipse(3.5, 3.5, 2.0, 3.0, g); c.set(3, 1, gl); c.set(3, 6, gd); }
        else { c.disc(3.5, 3.5, 1.6, g); c.set(3, 3, gl); c.set(0, 3, g); c.set(6, 2, g); c.set(3, 6, g); }
        return c;
    }

    /** A glob of acid: bright green with a bubble in it. */
    private static PixelCanvas acid(int f) {
        PixelCanvas c = new PixelCanvas(9, 9);
        c.disc(4.5, 4.5, 3.9 + f * 0.4, rgb(70, 200, 90));
        c.disc(4.5, 4.5, 2.7, rgb(150, 255, 110));
        c.disc(3.5 + f, 3.5, 1.0, rgb(240, 255, 220));
        c.outline(rgb(20, 90, 50));
        return c;
    }

    /** The big toxic glob the mad scientist flings: a swirl of green and yellow. */
    private static PixelCanvas acidBall(int f) {
        PixelCanvas c = new PixelCanvas(11, 11);
        c.disc(5.5, 5.5, 4.6 + f * 0.4, rgb(90, 210, 80));
        c.disc(5.5, 5.5, 3.3, rgb(220, 255, 100));
        c.disc(5.0 + f, 5.0, 1.6, rgb(255, 255, 230));
        c.set(2, 8 - f, rgb(150, 255, 110)); c.set(9, 2 + f, rgb(150, 255, 110));
        c.outline(rgb(30, 100, 40));
        return c;
    }

    private static PixelCanvas spore(int f) {
        PixelCanvas c = new PixelCanvas(11, 11);
        c.disc(5.5, 5.5, 4.6 + f * 0.4, rgb(120, 210, 90));
        c.disc(5.5, 5.5, 3.3, rgb(255, 240, 120));
        c.disc(5.0, 5.0, 1.6, rgb(255, 255, 230));
        c.outline(rgb(40, 100, 50));
        return c;
    }

    /** The Warden's bullets: hot pink-white energy balls. */
    private static PixelCanvas boltBall(int f) {
        PixelCanvas c = new PixelCanvas(11, 11);
        c.disc(5.5, 5.5, 4.6 + f * 0.4, rgb(255, 70, 120));
        c.disc(5.5, 5.5, 3.3, rgb(255, 150, 190));
        c.disc(5.0, 5.0, 1.6, rgb(255, 255, 250));
        c.outline(rgb(120, 20, 60));
        return c;
    }

    // ------------------------------------------------------------------ made to size

    private static final Map<String, Sprite[]> CACHE = new HashMap<>();

    /**
     * The sword slash: a bright crescent facing right (rotate it to the attack direction). Three frames: the arc
     * sweeping out, at full brightness, then thinning away. {@code reach} is the arc's radius in world units.
     */
    static Sprite[] slash(double reach, boolean heavy) {
        int r = (int) Math.round(reach / Art.SCALE);
        return CACHE.computeIfAbsent("slash" + r + heavy, k -> {
            int size = r * 2 + 6, c0 = size / 2;
            double half = Math.toRadians(heavy ? 118 : 72);
            Sprite[] out = new Sprite[3];
            for (int f = 0; f < 3; f++) {
                PixelCanvas c = new PixelCanvas(size, size);
                double sweep = f == 0 ? 0.55 : 1.0;
                double thick = new double[]{3.2, 5.0, 2.2}[f] * (heavy ? 1.3 : 1.0);
                int edge = heavy ? rgb(255, 208, 90) : rgb(140, 196, 255);
                int mid = heavy ? rgb(255, 240, 170) : rgb(200, 232, 255);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        double dx = x + 0.5 - c0, dy = y + 0.5 - c0;
                        double rad = Math.hypot(dx, dy), th = Math.atan2(dy, dx);
                        double limit = -half + 2 * half * sweep;
                        if (th > limit || th < -half) continue;
                        double t = thick * Math.pow(Math.max(0, 1 - Math.pow(Math.abs(th) / half, 2)), 0.6) + 0.6;
                        double depth = r - rad;                          // 0 at the outer edge, growing inward
                        if (depth < 0 || depth > t) continue;
                        c.set(x, y, depth < 1.0 ? WHITE : depth < t * 0.55 ? mid : edge);
                    }
                }
                out[f] = new Sprite(c, c0, c0);
            }
            return out;
        });
    }

    /** A fireball's blast: 5 frames of a ragged fire disc growing to {@code radius} world units, then thinning to embers. */
    static Sprite[] explosion(double radius) {
        int r = (int) Math.round(radius / Art.SCALE);
        return CACHE.computeIfAbsent("boom" + r, k -> {
            int size = r * 2 + 6, c0 = size / 2;
            Sprite[] out = new Sprite[5];
            double[] grow = {0.42, 0.72, 0.95, 1.0, 1.0};
            for (int f = 0; f < 5; f++) {
                PixelCanvas c = new PixelCanvas(size, size);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        double dx = x + 0.5 - c0, dy = y + 0.5 - c0;
                        double rad = Math.hypot(dx, dy) / (r * grow[f]);
                        double n = (hash(x, y, f) % 100) / 100.0 - 0.5;
                        rad += n * 0.22;
                        if (rad > 1.0) continue;
                        if (f >= 3 && rad < 0.35 + 0.15 * (f - 3) + n * 0.2) continue;      // hollow out: fading to a ring of embers
                        if (f == 4 && (hash(x, y, 9) % 100) < 45) continue;
                        int col = rad < 0.28 ? rgb(255, 252, 214) : rad < 0.55 ? rgb(255, 222, 96) : rad < 0.8 ? rgb(255, 140, 42) : rgb(214, 56, 32);
                        if (f == 4) col = rad < 0.8 ? rgb(255, 140, 60) : rgb(120, 60, 50);
                        c.set(x, y, col);
                    }
                }
                out[f] = new Sprite(c, c0, c0);
            }
            return out;
        });
    }

    /** The frozen ground of an Ice Storm: a jagged disc of pale ice with cracks and twinkles (2 frames). */
    static Sprite[] frost(double radius) {
        int r = (int) Math.round(radius / Art.SCALE);
        return CACHE.computeIfAbsent("frost" + r, k -> {
            int size = r * 2 + 8, c0 = size / 2;
            Sprite[] out = new Sprite[2];
            for (int f = 0; f < 2; f++) {
                PixelCanvas c = new PixelCanvas(size, size);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        double dx = x + 0.5 - c0, dy = y + 0.5 - c0;
                        double rad = Math.hypot(dx, dy) / r;
                        double th = Math.atan2(dy, dx);
                        double spikes = 0.06 * Math.sin(th * 14) + ((hash(x / 2, y / 2, 3) % 100) / 100.0 - 0.5) * 0.05;
                        if (rad + spikes > 1.0) continue;
                        int col = rad > 0.88 ? rgba(210, 240, 255, 190) : rgba(170, 220, 255, 96);
                        int hh = hash(x, y, 5) % 100;
                        if (hh < 6) col = rgba(235, 250, 255, 170);
                        c.set(x, y, col);
                    }
                }
                for (int i = 0; i < 9; i++) {                                     // cracks running out from the centre
                    double a = i * Math.PI * 2 / 9 + 0.3;
                    c.line(c0, c0, (int) Math.round(c0 + Math.cos(a) * r * 0.85), (int) Math.round(c0 + Math.sin(a) * r * 0.85), rgba(235, 250, 255, 150));
                }
                for (int i = 0; i < 10; i++) {                                    // twinkles, different on each frame
                    int hx = hash(i, f, 11), hy = hash(i, f, 13);
                    double a = (hx % 628) / 100.0, d = (hy % 90) / 100.0 * r;
                    int sx = (int) Math.round(c0 + Math.cos(a) * d), sy = (int) Math.round(c0 + Math.sin(a) * d);
                    c.set(sx, sy, WHITE); c.set(sx + 1, sy, rgba(255, 255, 255, 140)); c.set(sx - 1, sy, rgba(255, 255, 255, 140));
                    c.set(sx, sy + 1, rgba(255, 255, 255, 140)); c.set(sx, sy - 1, rgba(255, 255, 255, 140));
                }
                out[f] = new Sprite(c, c0, c0);
            }
            return out;
        });
    }

    /**
     * A lightning bolt as pixel art. The points are world coordinates (already jagged); the returned sprite's anchor is
     * set so drawing it at world (0, 0) puts every pixel in the right place.
     */
    static Sprite bolt(double[] xs, double[] ys) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]); maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]);
        }
        int pad = 4;
        int ox = (int) Math.floor(minX / Art.SCALE) - pad, oy = (int) Math.floor(minY / Art.SCALE) - pad;
        int w = (int) Math.ceil(maxX / Art.SCALE) + pad - ox + 1, h = (int) Math.ceil(maxY / Art.SCALE) + pad - oy + 1;
        PixelCanvas c = new PixelCanvas(Math.max(1, w), Math.max(1, h));
        int glow = rgba(120, 170, 255, 130), mid = rgb(255, 240, 110), core = rgb(255, 255, 250);
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i + 1 < xs.length; i++) {
                int x0 = (int) Math.round(xs[i] / Art.SCALE) - ox, y0 = (int) Math.round(ys[i] / Art.SCALE) - oy;
                int x1 = (int) Math.round(xs[i + 1] / Art.SCALE) - ox, y1 = (int) Math.round(ys[i + 1] / Art.SCALE) - oy;
                if (pass == 0) c.thickLine(x0, y0, x1, y1, 5, glow);
                else if (pass == 1) c.thickLine(x0, y0, x1, y1, 3, mid);
                else c.line(x0, y0, x1, y1, core);
            }
        }
        return new Sprite(c, -ox, -oy);
    }

    /** A small deterministic scramble, so painted noise is the same every run. */
    static int hash(int x, int y, int salt) {
        int h = x * 374761393 + y * 668265263 + salt * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return h & 0x7fffffff;
    }
}
