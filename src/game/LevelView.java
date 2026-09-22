package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Draws a level's world: the void beyond the walls, the wall band, the tiled floor with scenery on it, the shading
 * where floor meets wall, the trees / lamp posts / windows around the walls, and the barriers over closed doors.
 *
 * Everything except the barriers and glows never changes, so it is painted once into big cached image chunks and each
 * frame just copies the chunks that are on screen. (Corridors are always part of the baked floor; a closed door simply
 * has its barrier drawn on top of the corridor.) The caches are shared, so identical levels are only ever painted once.
 */
final class LevelView {
    /** How far the wall band reaches out from the edge of the floor. */
    static final double WALL = 66;
    private static final int CHUNK = 1024;

    private record Prop(Sprite sprite, double x, double y, boolean flip, int glow) {
        boolean touches(Rectangle2D v) {
            double s = Art.SCALE;
            return v.intersects(x - sprite.ax * s, y - sprite.ay * s, sprite.w * s, sprite.h * s);
        }
    }

    /** Everything static about one level layout. */
    private static final class Baked {
        final Level level;
        final ThemeArt art;
        final Area floor;                       // rooms and all corridors, whether open or not
        final Area wallBand;
        final List<Prop> flat = new ArrayList<>();
        final List<Prop> scenery = new ArrayList<>();
        final String key;

        Baked(Level lv, String key) {
            this.level = lv;
            this.key = key;
            this.art = ThemeArt.of(lv.theme);
            Area a = new Area();
            for (Level.Room r : lv.rooms) for (Rectangle2D.Double p : r.parts) a.add(new Area(p));
            for (Level.Door d : lv.doors) a.add(new Area(d.gap));
            this.floor = a;
            Area stroked = new Area(new BasicStroke((float) (WALL * 2), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f).createStrokedShape(a));
            stroked.subtract(a);
            this.wallBand = stroked;
            buildPropsFor(this);
        }
    }

    private static final Map<String, Baked> BAKED = new LinkedHashMap<>();
    private static final Map<String, BufferedImage> CHUNKS = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) { return size() > 26; }
    };
    private static final Map<Long, BufferedImage> GLOWS = new HashMap<>();

    private Baked baked;

    /** Distinguishes levels whose bake would otherwise look identical (same theme, size and room count) but whose rooms are shaped differently. */
    private static String keyOf(Level lv) {
        long shape = 0;
        for (Level.Room r : lv.rooms) for (Rectangle2D.Double p : r.parts) {
            shape = shape * 1000003 + (long) p.x * 97 + (long) p.y * 89 + (long) p.width * 83 + (long) p.height * 79;
        }
        return lv.theme + ":" + (int) lv.width + "x" + (int) lv.height + ":" + lv.rooms.size() + ":" + shape;
    }

    /** Finds (or paints, the first time) the cached background for this level. */
    private void update(Level lv) {
        if (baked != null && baked.level == lv) return;
        String key = keyOf(lv);
        synchronized (BAKED) {
            Baked b = BAKED.get(key);
            if (b == null) {
                b = new Baked(lv, key);
                BAKED.put(key, b);
                while (BAKED.size() > 3) BAKED.remove(BAKED.keySet().iterator().next());
            }
            baked = b;
        }
    }

    ThemeArt art() { return baked == null ? null : baked.art; }

    // ------------------------------------------------------------------ drawing

    /** Draws everything that isn't a character. {@code g} is already translated by the camera; {@code view} is what's visible, in world units. */
    void draw(Graphics2D g, Level lv, Rectangle2D view, double time) {
        update(lv);
        Baked b = baked;
        int cx0 = (int) Math.floor(view.getMinX() / CHUNK), cx1 = (int) Math.floor(view.getMaxX() / CHUNK);
        int cy0 = (int) Math.floor(view.getMinY() / CHUNK), cy1 = (int) Math.floor(view.getMaxY() / CHUNK);
        for (int cy = cy0; cy <= cy1; cy++) {
            for (int cx = cx0; cx <= cx1; cx++) g.drawImage(chunk(b, cx, cy), cx * CHUNK, cy * CHUNK, null);
        }

        prefetch(b, cx0 - 1, cx1 + 1, cy0 - 1, cy1 + 1);

        for (Level.Door d : lv.doors) {                             // closed doors: bramble / shutters / a purple seal
            if (d.open() || !view.intersects(d.gap)) continue;
            g.setPaint(d.sealed ? b.art.sealedBarrier : b.art.combatBarrier);
            g.fill(d.gap);
            double pulse = 0.5 + 0.5 * Math.sin(time * 4);
            g.setColor(d.sealed ? new Color(190, 120, 255, (int) (50 + 60 * pulse)) : new Color(255, 60, 60, (int) (22 + 40 * pulse)));
            g.fill(d.gap);
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect((int) d.gap.x, (int) d.gap.y, (int) d.gap.width, d.vertical ? 4 : (int) d.gap.height);
            g.setStroke(new BasicStroke(5f));
            g.setColor(new Color(0, 0, 0, 90));
            g.draw(d.gap);
        }
    }

    /** Paints at most one not-yet-painted chunk around the view per frame, so walking into new ground doesn't hitch. */
    private static void prefetch(Baked b, int cx0, int cx1, int cy0, int cy1) {
        for (int cy = cy0; cy <= cy1; cy++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                if (cx < 0 || cy < 0 || cx * CHUNK > b.level.width + CHUNK || cy * CHUNK > b.level.height + CHUNK) continue;
                synchronized (LevelView.class) {
                    if (CHUNKS.containsKey(b.key + ":" + cx + "," + cy)) continue;
                }
                chunk(b, cx, cy);
                return;
            }
        }
    }

    private static synchronized BufferedImage chunk(Baked b, int cx, int cy) {
        return CHUNKS.computeIfAbsent(b.key + ":" + cx + "," + cy, k -> paintChunk(b, cx, cy));
    }

    /** Paints one square of the static world: void, walls, tiled floor, things on the floor, shading, and scenery. */
    private static BufferedImage paintChunk(Baked b, int cx, int cy) {
        BufferedImage img = new BufferedImage(CHUNK, CHUNK, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.translate(-cx * CHUNK, -cy * CHUNK);
        Rectangle2D v = new Rectangle2D.Double(cx * CHUNK, cy * CHUNK, CHUNK, CHUNK);
        ThemeArt art = b.art;

        g.setPaint(art.voidPaint);                                  // beyond the walls
        g.fill(v);
        if (b.wallBand.intersects(v)) {
            g.setPaint(art.wallPaint);                              // the wall band
            g.fill(b.wallBand);
            g.setColor(new Color(0, 0, 0, 60));                     // a darker line where the wall meets the void
            g.setStroke(new BasicStroke(6f));
            g.draw(b.wallBand);
        }
        if (b.floor.intersects(v)) {
            Shape saved = g.getClip();
            g.clip(b.floor);
            int t = ThemeArt.TILE;
            int i0 = (int) Math.floor(v.getMinX() / t), i1 = (int) Math.floor(v.getMaxX() / t);
            int j0 = (int) Math.floor(v.getMinY() / t), j1 = (int) Math.floor(v.getMaxY() / t);
            for (int j = j0; j <= j1; j++) {
                for (int i = i0; i <= i1; i++) {
                    BufferedImage[] set = tilesAt(b.level, art, (i + 0.5) * t, (j + 0.5) * t);
                    g.drawImage(set[FxArt.hash(i, j, 1) % set.length], i * t, j * t, null);
                }
            }
            for (Prop p : b.flat) if (p.touches(v)) p.sprite.draw(g, p.x, p.y, Art.SCALE, p.flip);
            g.setStroke(new BasicStroke(34f));                      // the floor gets a soft shadow along the walls
            g.setColor(new Color(0, 0, 0, 44));
            g.draw(b.floor);
            g.setClip(saved);
            g.setStroke(new BasicStroke(9f));                       // and a dark lip where it ends
            g.setColor(new Color(art.shade));
            g.draw(b.floor);
        }
        for (Prop p : b.scenery) if (p.touches(v)) p.sprite.draw(g, p.x, p.y, Art.SCALE, p.flip);
        g.dispose();
        return img;
    }

    private static BufferedImage[] tilesAt(Level lv, ThemeArt art, double x, double y) {
        for (Level.Room r : lv.rooms) {
            for (Rectangle2D.Double p : r.parts) {
                if (p.contains(x, y)) return r.gated ? art.boss : r.state == Level.Room.State.SAFE ? art.plaza : art.ground;
            }
        }
        for (Level.Door d : lv.doors) if (d.gap.contains(x, y)) return art.path;
        return art.ground;
    }

    // ------------------------------------------------------------------ glows

    /** Warm light pools around lamp posts and coloured glow around neon signs. Drawn after the ambient darkness. */
    void drawGlows(Graphics2D g, Rectangle2D view) {
        if (baked == null) return;
        java.awt.Composite saved = g.getComposite();
        for (Prop p : baked.scenery) {
            if (p.glow == 0 || !view.intersects(p.x - 200, p.y - 300, 400, 400)) continue;
            int rgb = p.glow & 0xFFFFFF;
            int r = (p.glow >>> 24) == 2 ? 120 : 190;
            BufferedImage halo = glow(rgb, r, 92, false), pool = glow(rgb, (int) (r * 0.9), 56, true);
            g.drawImage(halo, (int) (p.x - r), (int) (p.y - 100 - r), null);
            g.drawImage(pool, (int) (p.x - r * 0.9), (int) (p.y + 18 - r * 0.45), null);
        }
        g.setComposite(saved);
    }

    /** A soft round (or flattened) glow, painted once and reused. */
    private static synchronized BufferedImage glow(int rgb, int radius, int alpha, boolean flat) {
        long key = ((long) rgb << 24) ^ ((long) radius << 12) ^ (alpha << 1) ^ (flat ? 1 : 0);
        return GLOWS.computeIfAbsent(key, k -> {
            int w = radius * 2, h = flat ? radius : radius * 2;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            Color c = new Color(rgb);
            g.scale(1, flat ? 0.5 : 1);
            g.setPaint(new RadialGradientPaint(radius, radius, radius, new float[]{0f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
            g.fillRect(0, 0, w, radius * 2);
            g.dispose();
            return img;
        });
    }

    // ------------------------------------------------------------------ scenery placement

    /** Scatters flowers / litter over the floors and lines the outside of the walls with trees, lamps and windows. Deterministic. */
    private static void buildPropsFor(Baked bk) {
        Level lv = bk.level;
        ThemeArt a = bk.art;
        boolean forest = lv.theme == Theme.FOREST;
        Random rng = new Random(lv.rooms.size() * 7919L + lv.theme.ordinal() * 104729L);

        List<Rectangle2D> keepClear = new ArrayList<>();            // never put anything on the ground near a doorway
        for (Level.Door d : lv.doors) {
            Rectangle2D g = d.walk;
            keepClear.add(new Rectangle2D.Double(g.getX() - 110, g.getY() - 110, g.getWidth() + 220, g.getHeight() + 220));
        }

        for (Level.Room room : lv.rooms) {
            for (Rectangle2D.Double b : room.parts) {                    // a multi-part room just runs this once per piece: each piece's own
                                                                           // exterior gets scenery, and the check below already skips any edge
                                                                           // that's actually another piece of the same room, not a real wall
                // things lying on the floor (the city's boss room is bare steel: no road markings there)
                int n = room.gated && lv.theme == Theme.CITY ? 0 : (int) (b.getWidth() * b.getHeight() / (forest ? 15000 : lv.theme == Theme.LAB ? 26000 : 30000));
                for (int i = 0; i < n; i++) {
                    double x = b.getX() + 60 + rng.nextDouble() * (b.getWidth() - 120);
                    double y = b.getY() + 60 + rng.nextDouble() * (b.getHeight() - 120);
                    if (nearInteractive(lv, x, y)) continue;
                    boolean blocked = false;
                    for (Rectangle2D k : keepClear) if (k.contains(x, y)) blocked = true;
                    if (blocked) continue;
                    bk.flat.add(new Prop(a.floorProps[rng.nextInt(a.floorProps.length)], x, y, rng.nextBoolean(), 0));
                }
                // scenery around the outside of the walls: N / E / W get tall things, S gets low ones
                for (int side = 0; side < 4; side++) {
                    boolean horizontal = side == 0 || side == 2;          // 0 north, 1 east, 2 south, 3 west
                    double len = horizontal ? b.getWidth() : b.getHeight();
                    double pos = 30 + rng.nextDouble() * 50;
                    while (pos < len - 30) {
                        double x, y;
                        double off = 26 + rng.nextDouble() * 22;
                        switch (side) {
                            case 0 -> { x = b.getX() + pos; y = b.getY() - off; }
                            case 2 -> { x = b.getX() + pos; y = b.getMaxY() + off + 24; }
                            case 1 -> { x = b.getMaxX() + off; y = b.getY() + pos; }
                            default -> { x = b.getX() - off; y = b.getY() + pos; }
                        }
                        if (outsideAll(lv, x, y) && !nearDoor(lv, x, y)) {
                            Sprite s;
                            int glow = 0;
                            if (side == 2) s = a.low[rng.nextInt(a.low.length)];
                            else if (rng.nextInt(10) < 7) {
                                int idx = rng.nextInt(a.tall.length);
                                s = a.tall[idx];
                                if (lv.theme == Theme.CITY) {
                                    if (idx == 0 || idx == 3) glow = 0x01FFE7A0;                            // warm lamp light
                                    else if (idx == 1) glow = 0x02FF5AB4;                                   // pink neon
                                    else if (idx == 4) glow = 0x0278F0FF;                                   // cyan neon
                                } else if (lv.theme == Theme.LAB) {
                                    glow = switch (idx) {
                                        case 0 -> 0x0250FF90;                                               // green tank
                                        case 1 -> 0x02B98CFF;                                               // tesla coil
                                        case 2 -> 0x0278B4FF;                                               // server lights
                                        case 3 -> 0x02FF6EBE;                                               // pink tank
                                        default -> 0x01C8FFF8;                                              // fluorescent lamp
                                    };
                                }
                            } else s = a.low[rng.nextInt(a.low.length)];
                            bk.scenery.add(new Prop(s, x, y, rng.nextBoolean(), glow));
                        }
                        pos += 78 + rng.nextDouble() * 70;
                    }
                    if (a.facade.length > 0) addWindows(lv, a, rng, b, side, bk.scenery);
                }
            }
        }
        bk.scenery.sort(Comparator.comparingDouble(p -> p.y));
    }

    /** City: windows (and now and then an awning) stuck onto the brick walls. */
    private static void addWindows(Level lv, ThemeArt a, Random rng, Rectangle2D b, int side, List<Prop> out) {
        boolean horizontal = side == 0 || side == 2;
        double len = horizontal ? b.getWidth() : b.getHeight();
        for (double pos = 60; pos < len - 40; pos += 64) {
            double x, y;
            switch (side) {
                case 0 -> { x = b.getX() + pos; y = b.getY() - 20; }
                case 2 -> { x = b.getX() + pos; y = b.getMaxY() + 54; }
                case 1 -> { x = b.getMaxX() + 32; y = b.getY() + pos; }
                default -> { x = b.getX() - 32; y = b.getY() + pos; }
            }
            if (!outsideAll(lv, x, y) || nearDoor(lv, x, y)) continue;
            out.add(new Prop(a.facade[rng.nextInt(a.facade.length)], x, y, false, 0));
        }
    }

    private static boolean nearInteractive(Level lv, double x, double y) {
        for (Level.Station s : lv.stations) if (Util.dist(x, y, s.x(), s.y()) < 100) return true;
        return Util.dist(x, y, lv.guideX, lv.guideY) < 100 || Util.dist(x, y, lv.spawnX, lv.spawnY) < 80;
    }

    /** True if the point isn't inside any room or corridor (open or not), with a little margin. */
    private static boolean outsideAll(Level lv, double x, double y) {
        for (Level.Room r : lv.rooms) for (Rectangle2D.Double p : r.parts) if (inflate(p, 14).contains(x, y)) return false;
        for (Level.Door d : lv.doors) if (inflate(d.gap, 14).contains(x, y)) return false;
        return true;
    }

    private static boolean nearDoor(Level lv, double x, double y) {
        for (Level.Door d : lv.doors) if (inflate(d.walk, 70).contains(x, y)) return true;
        return false;
    }

    private static Rectangle2D inflate(Rectangle2D r, double m) {
        return new Rectangle2D.Double(r.getX() - m, r.getY() - m, r.getWidth() + 2 * m, r.getHeight() + 2 * m);
    }
}
