package game;

import static game.PixelCanvas.*;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * The look of a level: floor tiles, walls, the void beyond them, scenery and the barriers over closed doors, all
 * painted in code. One {@link Theme} per level (the forest, the city). Tiles are painted at 16x16 art pixels and
 * scaled up to {@link #TILE} world units.
 */
final class ThemeArt {
    /** Size of one floor tile in world units (16 art pixels x the art scale). */
    static final int TILE = 16 * Art.SCALE;

    private static final Map<Theme, ThemeArt> CACHE = new EnumMap<>(Theme.class);

    static synchronized ThemeArt of(Theme t) { return CACHE.computeIfAbsent(t, ThemeArt::new); }

    final Theme theme;
    final BufferedImage[] ground, plaza, boss, path;    // floor tiles, several variants each
    final TexturePaint voidPaint, wallPaint, combatBarrier, sealedBarrier;
    final Sprite[] floorProps, tall, low, facade;        // scenery: flat bits on the floor, tall things and low things
                                                        // outside the walls, and things stuck onto the wall itself
    final int ambient;                                   // a colour wash over the whole world (ARGB)
    final int shade;                                     // the dark line where floor meets wall (ARGB)

    private ThemeArt(Theme t) {
        this.theme = t;
        switch (t) {
            case FOREST -> {
                ground = variants(8, ThemeArt::grass);
                plaza = variants(4, ThemeArt::flagstone);
                boss = variants(6, ThemeArt::forestBossFloor);
                path = variants(4, ThemeArt::dirt);
                voidPaint = paint(canopy(1));
                wallPaint = paint(hedge(1));
                combatBarrier = paint(bramble(1));
                sealedBarrier = paint(runes(1));
                floorProps = ForestProps.floor();
                tall = ForestProps.tall();
                low = ForestProps.low();
                facade = new Sprite[0];
                ambient = rgba(255, 244, 200, 18);
                shade = rgb(30, 62, 40);
            }
            case CITY -> {
                ground = variants(8, ThemeArt::asphalt);
                plaza = variants(4, ThemeArt::plazaTile);
                boss = variants(6, ThemeArt::steelPlate);
                path = variants(4, ThemeArt::sidewalk);
                voidPaint = paint(rooftops(1));
                wallPaint = paint(brick(1));
                combatBarrier = paint(shutter(1));
                sealedBarrier = paint(runes(1));
                floorProps = CityProps.floor();
                tall = CityProps.tall();
                low = CityProps.low();
                facade = CityProps.facade();
                ambient = rgba(20, 30, 90, 78);
                shade = rgb(34, 30, 44);
            }
            default -> {
                ground = variants(8, ThemeArt::labFloor);
                plaza = variants(4, ThemeArt::labLobby);
                boss = variants(6, ThemeArt::labBossFloor);
                path = variants(4, ThemeArt::labGrating);
                voidPaint = paint(labPipes(1));
                wallPaint = paint(labWall(1));
                combatBarrier = paint(laserGate(1));
                sealedBarrier = paint(blastDoor(1));
                floorProps = LabProps.floor();
                tall = LabProps.tall();
                low = LabProps.low();
                facade = LabProps.facade();
                ambient = rgba(30, 210, 170, 30);
                shade = rgb(22, 44, 46);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private interface TileMaker { PixelCanvas make(int seed); }

    private static BufferedImage[] variants(int n, TileMaker maker) {
        BufferedImage[] out = new BufferedImage[n];
        for (int i = 0; i < n; i++) out[i] = scaled(maker.make(i + 1));
        return out;
    }

    static BufferedImage scaled(PixelCanvas c) {
        BufferedImage big = new BufferedImage(c.w * Art.SCALE, c.h * Art.SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(c.image(), 0, 0, big.getWidth(), big.getHeight(), null);
        g.dispose();
        return big;
    }

    private static TexturePaint paint(PixelCanvas c) {
        return new TexturePaint(scaled(c), new Rectangle(0, 0, c.w * Art.SCALE, c.h * Art.SCALE));
    }

    /** A disc that wraps around the tile's edges, so the tile repeats without seams. */
    private static void wrapDisc(PixelCanvas c, double x, double y, double r, int col) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) c.disc(x + dx * c.w, y + dy * c.h, r, col);
    }

    // ------------------------------------------------------------------ forest tiles

    private static final int GRASS = rgb(86, 158, 68), GRASS_L = rgb(108, 182, 82), GRASS_D = rgb(66, 130, 58), GRASS_H = rgb(134, 204, 96);

    private static PixelCanvas grass(int seed) {
        Random r = new Random(seed * 7919L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, GRASS);
        for (int i = 0; i < 34; i++) c.set(r.nextInt(16), r.nextInt(16), r.nextInt(3) == 0 ? GRASS_L : GRASS_D);
        for (int i = 0; i < 5; i++) {                                            // blades
            int x = 1 + r.nextInt(14), y = 1 + r.nextInt(13);
            c.set(x, y, GRASS_H); c.set(x, y + 1, GRASS_L); c.set(x + 1, y + 1, GRASS_D);
        }
        if (seed % 4 == 0) {                                                       // now and then a clover patch
            int x = 3 + r.nextInt(9), y = 3 + r.nextInt(9);
            int cl = rgb(60, 138, 66);
            c.disc(x + 0.5, y + 0.5, 1.0, cl); c.disc(x + 2.5, y + 0.5, 1.0, cl); c.disc(x + 1.5, y + 2.0, 1.0, cl);
        }
        return c;
    }

    private static PixelCanvas dirt(int seed) {
        Random r = new Random(seed * 104729L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(152, 114, 76));
        for (int i = 0; i < 40; i++) c.set(r.nextInt(16), r.nextInt(16), r.nextInt(2) == 0 ? rgb(172, 134, 92) : rgb(126, 92, 62));
        for (int i = 0; i < 3; i++) {                                            // pebbles
            int x = 1 + r.nextInt(13), y = 1 + r.nextInt(13);
            c.rect(x, y, 2, 1, rgb(190, 182, 168)); c.set(x, y + 1, rgb(140, 132, 122));
        }
        return c;
    }

    private static PixelCanvas flagstone(int seed) {
        Random r = new Random(seed * 15485863L);
        PixelCanvas c = new PixelCanvas(16, 16);
        int grout = rgb(94, 96, 86);
        c.rect(0, 0, 16, 16, grout);
        for (int qy = 0; qy < 2; qy++) {
            for (int qx = 0; qx < 2; qx++) {
                int shade = 150 + r.nextInt(24);
                int col = rgb(shade, shade + 2, shade - 12);
                c.rect(qx * 8 + 1, qy * 8 + 1, 7, 7, col);
                c.rect(qx * 8 + 1, qy * 8 + 1, 7, 1, lighten(col, 0.25));
                c.rect(qx * 8 + 1, qy * 8 + 7, 7, 1, darken(col, 0.15));
                for (int i = 0; i < 3; i++) c.set(qx * 8 + 1 + r.nextInt(7), qy * 8 + 1 + r.nextInt(7), darken(col, 0.12));
            }
        }
        for (int i = 0; i < 8; i++) {                                            // moss creeping over the seams
            int x = r.nextInt(16), y = r.nextInt(2) == 0 ? 0 : 8;
            c.set(x, y, rgb(96, 158, 78)); if (r.nextBoolean()) c.set(x, y + 1, rgb(70, 130, 60));
        }
        return c;
    }

    private static PixelCanvas forestBossFloor(int seed) {
        Random r = new Random(seed * 32452843L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(46, 66, 52));
        for (int i = 0; i < 40; i++) c.set(r.nextInt(16), r.nextInt(16), r.nextInt(2) == 0 ? rgb(38, 56, 46) : rgb(58, 82, 60));
        for (int i = 0; i < 5; i++) {                                            // fallen autumn leaves
            int x = r.nextInt(14), y = r.nextInt(14);
            int lc = r.nextBoolean() ? rgb(150, 84, 44) : rgb(190, 122, 52);
            c.rect(x, y, 2, 1, lc); c.set(x + 1, y + 1, darken(lc, 0.25));
        }
        if (seed % 3 == 0) {                                                       // a faint glowing spore
            int x = 2 + r.nextInt(12), y = 2 + r.nextInt(12);
            c.set(x, y, rgb(220, 90, 150)); c.set(x + 1, y, rgb(122, 78, 100));
        }
        return c;
    }

    private static PixelCanvas canopy(int seed) {
        Random r = new Random(seed * 49979687L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(22, 50, 34));
        for (int i = 0; i < 7; i++) wrapDisc(c, r.nextInt(16), r.nextInt(16), 3.0 + r.nextInt(2), rgb(30, 68, 44));
        for (int i = 0; i < 7; i++) wrapDisc(c, r.nextInt(16), r.nextInt(16), 1.8 + r.nextInt(2), rgb(42, 90, 56));
        for (int i = 0; i < 5; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(62, 118, 72));
        return c;
    }

    private static PixelCanvas hedge(int seed) {
        Random r = new Random(seed * 86028121L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(38, 92, 52));
        for (int i = 0; i < 8; i++) wrapDisc(c, r.nextInt(16), r.nextInt(16), 3.0, rgb(50, 116, 62));
        for (int i = 0; i < 9; i++) wrapDisc(c, r.nextInt(16), r.nextInt(16), 1.8, rgb(72, 150, 76));
        for (int i = 0; i < 6; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(30, 72, 44));
        for (int i = 0; i < 2; i++) { int x = r.nextInt(15), y = r.nextInt(15); c.set(x, y, rgb(226, 74, 84)); c.set(x + 1, y, rgb(170, 40, 60)); }   // berries
        return c;
    }

    private static PixelCanvas bramble(int seed) {
        Random r = new Random(seed * 179424673L);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(30, 34, 28));
        int vine = rgb(70, 78, 44), vineL = rgb(104, 118, 62), thorn = rgb(196, 62, 66);
        for (int i = 0; i < 4; i++) {
            int x = (i * 5 + r.nextInt(3)) % 16;
            for (int y = 0; y < 16; y++) {
                int xx = (x + (int) Math.round(Math.sin(y * 0.7 + i) * 1.6) + 16) % 16;
                c.set(xx, y, vine); c.set((xx + 1) % 16, y, vineL);
                if (y % 8 == i * 2) { c.set((xx + 2) % 16, y, thorn); c.set((xx + 15) % 16, y, thorn); }
            }
        }
        return c;
    }

    // ------------------------------------------------------------------ city tiles

    private static PixelCanvas asphalt(int seed) {
        Random r = new Random(seed * 7919L + 11);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(62, 66, 80));
        for (int i = 0; i < 46; i++) c.set(r.nextInt(16), r.nextInt(16), r.nextInt(2) == 0 ? rgb(74, 78, 94) : rgb(50, 54, 66));
        for (int i = 0; i < 4; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(94, 98, 116));
        if (seed % 3 == 0) {                                                       // a crack
            int x = 2 + r.nextInt(8), y = 2 + r.nextInt(10);
            for (int i = 0; i < 6; i++) { c.set(x + i, y, rgb(34, 36, 46)); if (i % 2 == 0) y += r.nextInt(3) - 1; }
        }
        if (seed % 5 == 0) c.disc(5 + r.nextInt(6) + 0.5, 5 + r.nextInt(6) + 0.5, 2.2, rgb(52, 56, 70));
        return c;
    }

    private static PixelCanvas sidewalk(int seed) {
        Random r = new Random(seed * 104729L + 5);
        PixelCanvas c = new PixelCanvas(16, 16);
        int seam = rgb(120, 124, 142);
        c.rect(0, 0, 16, 16, seam);
        for (int qy = 0; qy < 2; qy++) for (int qx = 0; qx < 2; qx++) {
            int s = 172 + r.nextInt(16);
            int col = rgb(s, s + 2, s + 14);
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 7, col);
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 1, lighten(col, 0.25));
            for (int i = 0; i < 2; i++) c.set(qx * 8 + 1 + r.nextInt(7), qy * 8 + 1 + r.nextInt(7), darken(col, 0.14));
        }
        return c;
    }

    private static PixelCanvas plazaTile(int seed) {
        Random r = new Random(seed * 15485863L + 3);
        PixelCanvas c = new PixelCanvas(16, 16);
        int a = rgb(212, 186, 148), b = rgb(184, 158, 124), grout = rgb(146, 122, 96);
        c.rect(0, 0, 16, 16, grout);
        for (int qy = 0; qy < 2; qy++) for (int qx = 0; qx < 2; qx++) {
            int col = (qx + qy) % 2 == 0 ? a : b;
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 7, col);
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 1, lighten(col, 0.22));
            c.set(qx * 8 + 1 + r.nextInt(7), qy * 8 + 1 + r.nextInt(7), darken(col, 0.12));
        }
        return c;
    }

    private static PixelCanvas steelPlate(int seed) {
        Random r = new Random(seed * 32452843L + 9);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(80, 90, 112));
        c.rect(0, 0, 16, 1, rgb(58, 66, 86)); c.rect(0, 0, 1, 16, rgb(58, 66, 86));
        c.rect(1, 1, 14, 1, rgb(110, 122, 148));
        for (int i = 0; i < 10; i++) c.set(1 + r.nextInt(14), 1 + r.nextInt(14), r.nextBoolean() ? rgb(90, 100, 124) : rgb(70, 80, 100));
        for (int[] p : new int[][]{{2, 2}, {13, 2}, {2, 13}, {13, 13}}) { c.set(p[0], p[1], rgb(154, 166, 190)); c.set(p[0] + 1, p[1] + 1, rgb(50, 58, 76)); }
        if (seed % 6 == 0) {                                                       // hazard stripes
            for (int i = 0; i < 16; i++) for (int j = 5; j < 11; j++) if (((i + j) / 3) % 2 == 0) c.set(i, j, rgb(230, 190, 50)); else c.set(i, j, rgb(40, 42, 52));
        }
        return c;
    }

    private static PixelCanvas rooftops(int seed) {
        Random r = new Random(seed * 49979687L + 7);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(26, 30, 44));
        c.rect(0, 0, 16, 1, rgb(18, 20, 32)); c.rect(0, 0, 1, 16, rgb(18, 20, 32));
        c.rect(2, 2, 6, 5, rgb(36, 42, 60)); c.rect(9, 8, 5, 6, rgb(34, 40, 58));
        c.rect(3, 9, 4, 4, rgb(46, 54, 74)); c.rect(3, 9, 4, 1, rgb(70, 80, 104));
        c.rect(10, 3, 3, 3, rgb(60, 110, 150)); c.set(10, 3, rgb(140, 190, 230));                 // a skylight
        for (int i = 0; i < 6; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(42, 48, 66));
        return c;
    }

    private static PixelCanvas brick(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        int mortar = rgb(112, 66, 60), b1 = rgb(164, 92, 76), b2 = rgb(146, 80, 68), hi = rgb(190, 118, 96);
        c.rect(0, 0, 16, 16, mortar);
        for (int row = 0; row < 4; row++) {
            int off = row % 2 == 0 ? 0 : 4;
            for (int i = -1; i < 4; i++) {
                int x = i * 8 + off;
                int col = (i + row) % 2 == 0 ? b1 : b2;
                for (int dx = 0; dx < 7; dx++) for (int dy = 0; dy < 3; dy++) c.set(((x + dx) % 16 + 16) % 16, row * 4 + dy, col);
                for (int dx = 0; dx < 7; dx++) c.set(((x + dx) % 16 + 16) % 16, row * 4, hi);
            }
        }
        return c;
    }

    private static PixelCanvas shutter(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(96, 100, 118));
        for (int y = 0; y < 16; y += 4) { c.rect(0, y, 16, 1, rgb(150, 156, 176)); c.rect(0, y + 3, 16, 1, rgb(56, 60, 76)); }
        for (int i = 0; i < 16; i++) if ((i / 3) % 2 == 0) { c.set(i, 5, rgb(226, 60, 60)); c.set(i, 6, rgb(226, 60, 60)); }   // warning stripe
        return c;
    }

    private static PixelCanvas runes(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(40, 24, 72));
        int glow = rgb(190, 132, 255), dim = rgb(96, 60, 160);
        c.rect(2, 2, 12, 1, dim); c.rect(2, 13, 12, 1, dim); c.rect(2, 2, 1, 12, dim); c.rect(13, 2, 1, 12, dim);
        c.line(8, 3, 4, 8, glow); c.line(8, 3, 12, 8, glow); c.line(4, 8, 8, 13, glow); c.line(12, 8, 8, 13, glow);
        c.set(8, 8, rgb(255, 230, 255)); c.set(7, 8, glow); c.set(9, 8, glow);
        return c;
    }

    // ------------------------------------------------------------------ laboratory tiles

    /** Clean pale tiles with a faint blue cast, scuffed and stained. */
    private static PixelCanvas labFloor(int seed) {
        Random r = new Random(seed * 7919L + 21);
        PixelCanvas c = new PixelCanvas(16, 16);
        int grout = rgb(126, 150, 158);
        c.rect(0, 0, 16, 16, grout);
        for (int qy = 0; qy < 2; qy++) for (int qx = 0; qx < 2; qx++) {
            int v = 206 + r.nextInt(14);
            int col = rgb(v - 16, v, v + 4);
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 7, col);
            c.rect(qx * 8 + 1, qy * 8 + 1, 7, 1, lighten(col, 0.28));
            c.rect(qx * 8 + 1, qy * 8 + 7, 7, 1, darken(col, 0.1));
            for (int i = 0; i < 2; i++) c.set(qx * 8 + 1 + r.nextInt(7), qy * 8 + 1 + r.nextInt(7), darken(col, 0.09));
        }
        if (seed % 3 == 0) {                                                       // a stain of something green
            int x = 2 + r.nextInt(10), y = 2 + r.nextInt(10);
            c.set(x, y, rgb(150, 200, 130)); c.set(x + 1, y, rgb(134, 188, 120)); c.set(x, y + 1, rgb(134, 188, 120));
        }
        if (seed % 5 == 0) for (int i = 0; i < 5; i++) c.set(3 + i * 2, 3 + r.nextInt(3), rgb(150, 160, 164));   // scuffs
        return c;
    }

    /** The lobby: a black and white checkerboard with a yellow hazard line. */
    private static PixelCanvas labLobby(int seed) {
        Random r = new Random(seed * 15485863L + 13);
        PixelCanvas c = new PixelCanvas(16, 16);
        for (int qy = 0; qy < 2; qy++) for (int qx = 0; qx < 2; qx++) {
            int col = (qx + qy) % 2 == 0 ? rgb(226, 234, 236) : rgb(66, 82, 92);
            c.rect(qx * 8, qy * 8, 8, 8, col);
            c.rect(qx * 8, qy * 8, 8, 1, lighten(col, 0.18));
            c.rect(qx * 8, qy * 8, 1, 8, lighten(col, 0.1));
            c.set(qx * 8 + 2 + r.nextInt(5), qy * 8 + 2 + r.nextInt(5), darken(col, 0.1));
        }
        return c;
    }

    /** The sanctum: dark steel plates with glowing green circuit traces. */
    private static PixelCanvas labBossFloor(int seed) {
        Random r = new Random(seed * 32452843L + 17);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(36, 46, 52));
        c.rect(0, 0, 16, 1, rgb(24, 32, 38)); c.rect(0, 0, 1, 16, rgb(24, 32, 38));
        c.rect(1, 1, 14, 1, rgb(56, 70, 78));
        for (int i = 0; i < 8; i++) c.set(1 + r.nextInt(14), 1 + r.nextInt(14), r.nextBoolean() ? rgb(44, 56, 62) : rgb(30, 40, 46));
        int glow = rgb(74, 230, 140), dim = rgb(34, 120, 84);
        if (seed % 2 == 0) {                                                       // a circuit trace with a node
            int y = 4 + r.nextInt(8);
            c.rect(0, y, 9, 1, dim); c.rect(9, y, 1, 4, dim); c.rect(9, y + 4, 7, 1, dim);
            c.set(9, y + 4, glow); c.set(2, y, glow);
        } else if (seed % 3 == 0) {
            c.rect(3, 0, 1, 10, dim); c.rect(3, 10, 8, 1, dim); c.set(3, 10, glow);
            c.rect(10, 8, 2, 2, glow);
        }
        for (int[] p : new int[][]{{2, 13}, {13, 13}}) { c.set(p[0], p[1], rgb(120, 136, 146)); c.set(p[0] + 1, p[1] + 1, rgb(20, 26, 30)); }
        return c;
    }

    /** Corridors: a metal grating edged with yellow and black. */
    private static PixelCanvas labGrating(int seed) {
        Random r = new Random(seed * 104729L + 19);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(52, 64, 72));
        for (int y = 1; y < 16; y += 3) c.rect(0, y, 16, 1, rgb(96, 112, 122));
        for (int x = 0; x < 16; x += 4) c.rect(x, 0, 1, 16, rgb(38, 48, 54));
        for (int i = 0; i < 4; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(70, 84, 92));
        for (int i = 0; i < 16; i++) if ((i / 2) % 2 == 0) { c.set(i, 0, rgb(232, 194, 54)); c.set(i, 15, rgb(232, 194, 54)); } else { c.set(i, 0, rgb(30, 32, 36)); c.set(i, 15, rgb(30, 32, 36)); }
        return c;
    }

    /** Outside the walls: a dark tangle of pipes, ducts and cables. */
    private static PixelCanvas labPipes(int seed) {
        Random r = new Random(seed * 49979687L + 23);
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(20, 28, 34));
        c.rect(0, 3, 16, 3, rgb(52, 68, 78)); c.rect(0, 3, 16, 1, rgb(96, 118, 130)); c.rect(0, 5, 16, 1, rgb(30, 40, 48));
        c.rect(0, 10, 16, 2, rgb(42, 56, 64)); c.rect(0, 10, 16, 1, rgb(80, 98, 108));
        c.rect(5, 0, 2, 16, rgb(46, 60, 70)); c.rect(5, 0, 1, 16, rgb(88, 108, 120));
        c.rect(4, 2, 4, 1, rgb(120, 136, 146)); c.rect(4, 11, 4, 1, rgb(120, 136, 146));       // couplings
        c.rect(12, 0, 1, 16, rgb(36, 50, 58));
        c.set(9, 4, rgb(74, 230, 140)); c.set(13, 11, rgb(255, 90, 80));                         // status lights
        for (int i = 0; i < 6; i++) c.set(r.nextInt(16), r.nextInt(16), rgb(28, 38, 46));
        return c;
    }

    /** The wall face: teal panels with rivets and a hazard band. */
    private static PixelCanvas labWall(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        int panel = rgb(82, 138, 148), panelL = rgb(126, 184, 190), panelD = rgb(48, 90, 102);
        c.rect(0, 0, 16, 16, panelD);
        c.rect(1, 1, 14, 6, panel); c.rect(1, 1, 14, 1, panelL);
        c.rect(1, 9, 14, 6, panel); c.rect(1, 9, 14, 1, panelL);
        for (int i = 0; i < 16; i++) c.set(i, 7, (i / 2) % 2 == 0 ? rgb(232, 194, 54) : rgb(30, 32, 36));   // hazard band
        for (int[] p : new int[][]{{2, 2}, {13, 2}, {2, 10}, {13, 10}}) c.set(p[0], p[1], rgb(190, 214, 220));
        return c;
    }

    /** A closed door in a fight: a grid of red laser beams. */
    private static PixelCanvas laserGate(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(24, 20, 30));
        for (int y = 1; y < 16; y += 4) {
            c.rect(0, y, 16, 1, rgb(255, 60, 70));
            c.rect(0, y - 1, 16, 1, rgb(140, 26, 44)); c.rect(0, y + 1, 16, 1, rgb(140, 26, 44));
            c.set(3, y, rgb(255, 230, 230)); c.set(11, y, rgb(255, 230, 230));
        }
        c.rect(0, 0, 1, 16, rgb(70, 76, 90)); c.rect(15, 0, 1, 16, rgb(70, 76, 90));
        return c;
    }

    /** The sealed boss door: a steel blast door with chevrons and a red lock light. */
    private static PixelCanvas blastDoor(int seed) {
        PixelCanvas c = new PixelCanvas(16, 16);
        c.rect(0, 0, 16, 16, rgb(70, 82, 92));
        c.rect(0, 0, 16, 1, rgb(126, 142, 152)); c.rect(0, 15, 16, 1, rgb(34, 42, 50));
        for (int i = 0; i < 16; i++) for (int j = 2; j < 5; j++) c.set(i, j, ((i + j) / 2) % 2 == 0 ? rgb(232, 194, 54) : rgb(30, 32, 36));
        for (int i = 0; i < 16; i++) for (int j = 11; j < 14; j++) c.set(i, j, ((i - j + 16) / 2) % 2 == 0 ? rgb(232, 194, 54) : rgb(30, 32, 36));
        c.rect(6, 6, 4, 4, rgb(40, 48, 56)); c.rect(7, 7, 2, 2, rgb(255, 60, 64)); c.set(7, 7, rgb(255, 190, 190));
        return c;
    }
}
