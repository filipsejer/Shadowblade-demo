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
 * painted in code. One {@link Theme} per level (the forest, the city, Stormcliff). Their floors are painted at the
 * drawn sprites' pixel size ({@link ForestArt}, {@link CityArt}, {@link LabArt}), as large seamless textures
 * ({@link #groundPaint} and friends); only the guardian's floor and the door barriers are still 16x16 tiles, scaled up
 * to {@link #TILE} world units.
 */
final class ThemeArt {
    /** Size of one floor tile in world units (16 art pixels x the art scale). */
    static final int TILE = 16 * Art.SCALE;

    private static final Map<Theme, ThemeArt> CACHE = new EnumMap<>(Theme.class);

    static synchronized ThemeArt of(Theme t) { return CACHE.computeIfAbsent(t, ThemeArt::new); }

    final Theme theme;
    final BufferedImage[] ground, plaza, boss, path;    // floor tiles, several variants each
    final TexturePaint voidPaint, wallPaint, combatBarrier, sealedBarrier;
    final Sprite[] floorProps, tall, low;                // scenery: flat bits on the floor, tall things and low things outside the walls
    /** Seamless floor textures (grass, the safe rooms' paving, the paths between rooms), when the theme has them; null otherwise. */
    final TexturePaint groundPaint, plazaPaint, pathPaint;
    /** Big soft patches laid over the floor before anything else, to break up the texture's repeat (may be empty). */
    final Sprite[] patches;
    /** True if the trees and rocks around the walls are drawn with the characters, in order of depth, so they can hide
     *  someone walking behind them (the forest's are tall enough to need it); false bakes them into the background. */
    final boolean liveScenery;
    final int ambient;                                   // a colour wash over the whole world (ARGB)
    final int shade;                                     // the dark line where floor meets wall (ARGB)

    private ThemeArt(Theme t) {
        this.theme = t;
        switch (t) {
            case FOREST -> {
                ground = plaza = path = new BufferedImage[0];          // (the textures below instead)
                groundPaint = ForestArt.paint(ForestArt.grass());
                plazaPaint = ForestArt.paint(ForestArt.flagstones());
                pathPaint = ForestArt.paint(ForestArt.dirt());
                patches = ForestArt.patches();
                liveScenery = true;
                boss = variants(6, ThemeArt::forestBossFloor);
                voidPaint = ForestArt.paint(ForestArt.canopy());
                wallPaint = ForestArt.paint(ForestArt.hedge());
                combatBarrier = paint(bramble(1));
                sealedBarrier = paint(runes(1));
                floorProps = ForestArt.floor();
                tall = ForestArt.tall();
                low = ForestArt.low();
                ambient = rgba(255, 244, 200, 14);
                shade = rgb(18, 34, 20);
            }
            case CITY -> {
                ground = plaza = path = new BufferedImage[0];          // (the textures below instead)
                groundPaint = ForestArt.paint(CityArt.cobbles());
                plazaPaint = ForestArt.paint(CityArt.plaza());
                pathPaint = ForestArt.paint(CityArt.bricks());
                patches = CityArt.patches();
                liveScenery = true;
                boss = variants(6, ThemeArt::steelPlate);
                voidPaint = ForestArt.paint(CityArt.roofs());
                wallPaint = ForestArt.paint(CityArt.granite());
                combatBarrier = paint(shutter(1));
                sealedBarrier = paint(runes(1));
                floorProps = CityArt.floor();
                tall = CityArt.facades();
                low = CityArt.low();
                ambient = rgba(20, 30, 90, 70);
                shade = rgb(24, 22, 34);
            }
            default -> {
                ground = plaza = path = new BufferedImage[0];          // (the textures below instead)
                groundPaint = ForestArt.paint(LabArt.slate());
                plazaPaint = ForestArt.paint(LabArt.tiles());
                pathPaint = ForestArt.paint(LabArt.grating());
                patches = LabArt.patches();
                liveScenery = true;
                boss = variants(6, ThemeArt::labBossFloor);
                voidPaint = ForestArt.paint(LabArt.sea());
                wallPaint = ForestArt.paint(LabArt.seaWall());
                combatBarrier = paint(laserGate(1));
                sealedBarrier = paint(blastDoor(1));
                floorProps = LabArt.floor();
                tall = LabArt.facades();
                low = LabArt.low();
                ambient = rgba(16, 26, 72, 84);
                shade = rgb(8, 14, 22);
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


    // ------------------------------------------------------------------ forest tiles





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
