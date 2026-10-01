package game;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

/**
 * The drawn art: pixel-art images made with PixelLab (see {@code res/art}), which replace the code-painted sprites of
 * the same names. Each image is a horizontal strip of frames; {@code anchors.properties} says where each one's feet
 * are and how many frames it holds. Anything without an image keeps its code-painted look, so the game still runs
 * (in the old style) if the images are missing from the classpath.
 *
 * The images have finer pixels than the painted art: one of their pixels is {@link #PIXEL} world units, against
 * {@link Art#SCALE} for the painted ones. Poses the generator wasn't asked for (walks, wind-ups, a boss's enraged
 * second phase) are made here from the pose it did draw.
 */
final class ImportedArt {
    private ImportedArt() {}

    /** World units per pixel of imported art. */
    static final int PIXEL = 2;
    private static final double K = PIXEL / (double) Art.SCALE;

    private static final String[] TYPES = {"grunt", "runner", "shooter", "brute"};

    static void register(Map<String, Sprite[]> m) {
        Map<String, Sprite[]> art = load();
        if (art.isEmpty()) return;

        // ---- scenery, crates, chests...: drawn as they are, under the painted sprite's name ("crate_forest" is "crate.forest")
        for (Map.Entry<String, Sprite[]> e : art.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("crate_") || name.startsWith("barrel_") || name.startsWith("landmark_") || name.startsWith("run_")) {
                m.put(name.replace('_', '.'), e.getValue());
            }
        }

        // ---- the monsters: one drawn pose each; the walk is a step and a bob, the wind-up a rear back
        for (Theme t : Theme.values()) {
            for (String type : TYPES) {
                Sprite[] s = art.get(t.key + "_" + type);
                if (s == null) continue;
                Sprite base = s[0];
                m.put(t.key + "." + type + ".walk", new Sprite[]{base, bob(squash(base, 1.06, 0.94), 1)});
                m.put(t.key + "." + type + ".windup", new Sprite[]{shear(squash(base, 0.94, 1.08), -3)});
            }
            Sprite[] nest = art.get(t.key + "_nest");
            if (nest != null) m.put(t.key + ".nest.idle", new Sprite[]{nest[0], squash(nest[0], 1.04, 0.97), nest[0], squash(nest[0], 0.97, 1.04)});
            Sprite[] boss = art.get(t.key + "_boss");
            if (boss != null) {
                Sprite[] enraged = art.get(t.key + "_boss2");
                putBoss(m, t.key + ".boss.", boss[0]);
                putBoss(m, t.key + ".boss2.", enraged != null ? enraged[0] : enrage(boss[0], t));
            }
        }
        Sprite[] shade = art.get("shade");
        if (shade != null) {
            m.put("shade.walk", new Sprite[]{shade[0], bob(shade[0], 2)});
            m.put("shade.windup", new Sprite[]{squash(shade[0], 0.92, 1.1)});
            m.put("shade.dizzy", new Sprite[]{shear(shade[0], 2)});
        }
    }

    private static void putBoss(Map<String, Sprite[]> m, String key, Sprite base) {
        m.put(key + "idle", new Sprite[]{base, bob(squash(base, 1.02, 0.98), 0)});
        m.put(key + "walk", new Sprite[]{shear(base, 1), bob(shear(squash(base, 1.03, 0.97), -1), 1)});
        m.put(key + "slam", new Sprite[]{shear(squash(base, 0.95, 1.08), -4)});
        m.put(key + "burst", new Sprite[]{squash(base, 1.1, 0.93)});
    }

    // ------------------------------------------------------------------ loading

    private static Map<String, Sprite[]> load() {
        Map<String, Sprite[]> out = new HashMap<>();
        Properties anchors = new Properties();
        try (InputStream in = ImportedArt.class.getResourceAsStream("/art/anchors.properties")) {
            if (in == null) return out;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) { anchors.load(r); }
        } catch (IOException e) {
            return out;
        }
        for (String name : anchors.stringPropertyNames()) {
            String[] p = anchors.getProperty(name).split(",");
            int ax = Integer.parseInt(p[0].trim()), ay = Integer.parseInt(p[1].trim()), n = Integer.parseInt(p[2].trim());
            BufferedImage strip;
            try (InputStream in = ImportedArt.class.getResourceAsStream("/art/" + name + ".png")) {
                if (in == null) continue;
                strip = argb(ImageIO.read(in));
            } catch (IOException e) {
                continue;
            }
            int fw = strip.getWidth() / n;
            Sprite[] frames = new Sprite[n];
            for (int i = 0; i < n; i++) frames[i] = new Sprite(argb(strip.getSubimage(i * fw, 0, fw, strip.getHeight())), ax, ay, K);
            out.put(name, frames);
        }
        return out;
    }

    private static BufferedImage argb(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    // ------------------------------------------------------------------ made poses

    /** The same picture drawn {@code px} pixels higher. */
    private static Sprite bob(Sprite s, int px) { return new Sprite(s.img, s.ax, s.ay + px, s.k); }

    /** Stretched about the feet: sx across, sy up. */
    private static Sprite squash(Sprite s, double sx, double sy) {
        int w = (int) Math.ceil(s.w * sx) + 1, h = (int) Math.ceil(s.h * sy) + 1;
        int ax = (int) Math.round(s.ax * sx), ay = (int) Math.round((s.ay + 1) * sy) - 1;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sxp = (int) Math.floor((x - ax + 0.5) / sx + s.ax), syp = (int) Math.floor((y - ay - 0.5) / sy + s.ay + 1);
                if (sxp >= 0 && syp >= 0 && sxp < s.w && syp < s.h) out.setRGB(x, y, s.img.getRGB(sxp, syp));
            }
        }
        return new Sprite(out, ax, ay, s.k);
    }

    /** Leaning: the top shifted {@code px} pixels along its facing (negative = rearing back), the feet planted. */
    private static Sprite shear(Sprite s, int px) {
        int pad = Math.abs(px);
        BufferedImage out = new BufferedImage(s.w + pad * 2, s.h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < s.h; y++) {
            double up = Math.max(0, s.ay - y) / Math.max(1.0, s.ay);
            int dx = (int) Math.round(px * up);
            for (int x = 0; x < s.w; x++) {
                int c = s.img.getRGB(x, y);
                if ((c >>> 24) != 0) out.setRGB(x + pad + dx, y, c);
            }
        }
        return new Sprite(out, s.ax + pad, s.ay, s.k);
    }

    /**
     * A boss's second phase when there is no drawn one: the same body, recoloured. The forest's guardian turns autumn
     * (its greens go orange, the bark stays), the city's warden runs hot red, and the scientist goes a sickly glowing green.
     */
    private static Sprite enrage(Sprite s, Theme t) {
        BufferedImage out = new BufferedImage(s.w, s.h, BufferedImage.TYPE_INT_ARGB);
        float[] hsb = new float[3];
        for (int y = 0; y < s.h; y++) {
            for (int x = 0; x < s.w; x++) {
                int c = s.img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, hsb);
                float h = hsb[0], sat = hsb[1], b = hsb[2];
                if (b >= 0.16f) {                                                             // the outline stays dark
                    switch (t) {
                        case FOREST -> { if (h > 0.17f && h < 0.5f && sat > 0.2f) { h = 0.07f + (h - 0.17f) * 0.15f; sat = Math.min(1f, sat * 1.1f); } }
                        case CITY -> { if (sat > 0.2f) h = (h + 0.42f) % 1f; }
                        case LAB -> { if (sat < 0.2f) { h = 0.33f; sat = 0.34f; b *= 0.94f; } }        // the white coat and hair
                    }
                }
                out.setRGB(x, y, (c & 0xFF000000) | (Color.HSBtoRGB(h, sat, b) & 0xFFFFFF));
            }
        }
        return new Sprite(out, s.ax, s.ay, s.k);
    }
}
