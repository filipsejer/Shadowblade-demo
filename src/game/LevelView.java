package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
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
 * (Where the trees are tall, as in the forest, they're handed to the caller instead, to be drawn among the characters.)
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
            double s = Art.SCALE * sprite.k;
            return v.intersects(x - sprite.ax * s, y - sprite.ay * s, sprite.w * s, sprite.h * s);
        }
    }

    /** Everything static about one level layout. */
    private static final class Baked {
        final Level level;
        final ThemeArt art;
        final Area floor;                       // rooms and all corridors, whether open or not
        final Area wallBand;
        final List<Prop> patches = new ArrayList<>();
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
    /** Real grass, regardless of the level's own theme — for a grass patch dropped into a city or lab level. */
    private static TexturePaint grassPaint() { return ThemeArt.of(Theme.FOREST).groundPaint; }

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

    /** A piece of scenery for the caller to draw among the characters (its foot at {@code y}). */
    record Standing(Sprite sprite, double x, double y, boolean flip) {}

    /** The scenery around the walls that's in view, when the theme draws it live (see {@link ThemeArt#liveScenery}). */
    List<Standing> standing(Level lv, Rectangle2D view) {
        update(lv);
        List<Standing> out = new ArrayList<>();
        if (!baked.art.liveScenery) return out;
        for (Prop p : baked.scenery) if (p.touches(view)) out.add(new Standing(p.sprite, p.x, p.y, p.flip));
        return out;
    }

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

        for (Rectangle2D.Double r : lv.grassPatches) {              // solid grass beds, sitting on top of the floor beneath them
            if (!view.intersects(r)) continue;
            g.setPaint(grassPaint());
            g.fill(r);
            g.setColor(new Color(0, 0, 0, 70));
            g.setStroke(new BasicStroke(4f));
            g.draw(r);
        }

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

        tile(g, art.voidPaint, v, v);                               // beyond the walls
        if (b.wallBand.intersects(v)) {
            tile(g, art.wallPaint, b.wallBand, v);                  // the wall band
            g.setColor(new Color(0, 0, 0, 60));                     // a darker line where the wall meets the void
            g.setStroke(new BasicStroke(6f));
            g.draw(b.wallBand);
        }
        if (b.floor.intersects(v)) {
            Shape saved = g.getClip();
            g.clip(b.floor);
            if (art.groundPaint != null) {                          // seamless textures: grass everywhere, then paving and paths over it
                tile(g, art.groundPaint, v, v);
                for (Level.Room r : b.level.rooms) {
                    if (r.state != Level.Room.State.SAFE) continue;
                    for (Rectangle2D.Double part : r.parts) if (part.intersects(v)) tile(g, art.plazaPaint, part, v);
                }
                for (Level.Door d : b.level.doors) if (d.gap.intersects(v)) tile(g, art.pathPaint, d.gap, v);
            } else {
                int t = ThemeArt.TILE;
                int i0 = (int) Math.floor(v.getMinX() / t), i1 = (int) Math.floor(v.getMaxX() / t);
                int j0 = (int) Math.floor(v.getMinY() / t), j1 = (int) Math.floor(v.getMaxY() / t);
                for (int j = j0; j <= j1; j++) {
                    for (int i = i0; i <= i1; i++) {
                        BufferedImage[] set = tilesAt(b.level, art, (i + 0.5) * t, (j + 0.5) * t);
                        g.drawImage(set[FxArt.hash(i, j, 1) % set.length], i * t, j * t, null);
                    }
                }
            }
            for (Prop p : b.patches) if (p.touches(v)) p.sprite.draw(g, p.x, p.y, Art.SCALE, p.flip);
            for (Prop p : b.flat) if (p.touches(v)) p.sprite.draw(g, p.x, p.y, Art.SCALE, p.flip);
            for (Rectangle2D.Double wr : b.level.water) if (wr.intersects(v)) water(g, wr, v, b.level.theme);
            g.setStroke(new BasicStroke(34f));                      // the floor gets a soft shadow along the walls
            g.setColor(new Color(0, 0, 0, 44));
            g.draw(b.floor);
            g.setClip(saved);
            g.setStroke(new BasicStroke(9f));                       // and a dark lip where it ends
            g.setColor(new Color(art.shade));
            g.draw(b.floor);
        }
        if (!art.liveScenery) for (Prop p : b.scenery) if (p.touches(v)) p.sprite.draw(g, p.x, p.y, Art.SCALE, p.flip);
        g.dispose();
        return img;
    }

    private static TexturePaint waterPaint, surfPaint;

    /**
     * A stretch of water (a canal, or the breakers under Stormcliff's sea wall): dark ripples or white water, a shadow
     * under the near wall, and a stone kerb along its open edge.
     */
    private static void water(Graphics2D g, Rectangle2D.Double wr, Rectangle2D v, Theme theme) {
        synchronized (LevelView.class) {
            if (waterPaint == null) waterPaint = ForestArt.paint(CityArt.water());
            if (surfPaint == null) surfPaint = ForestArt.paint(LabArt.surf());
        }
        tile(g, theme == Theme.LAB ? surfPaint : waterPaint, wr, v);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Rectangle2D.Double(wr.x, wr.y, wr.width, 26));
        Rectangle2D.Double kerb = new Rectangle2D.Double(wr.x, wr.getMaxY() - 14, wr.width, 14);
        tile(g, ThemeArt.of(theme == Theme.LAB ? Theme.LAB : Theme.CITY).wallPaint, kerb, v);
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Rectangle2D.Double(kerb.x, kerb.y, kerb.width, 2));
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Rectangle2D.Double(kerb.x, kerb.getMaxY() - 2, kerb.width, 2));
    }

    /**
     * Covers {@code area} (within the chunk {@code v}) with a seamless texture, anchored at the world's origin like the
     * paint would be, by drawing its image side by side: much quicker than filling with the paint.
     */
    private static void tile(Graphics2D g, TexturePaint paint, Shape area, Rectangle2D v) {
        BufferedImage img = paint.getImage();
        int tw = img.getWidth(), th = img.getHeight();
        Rectangle2D box = area.getBounds2D().createIntersection(v);
        if (box.isEmpty()) return;
        Shape saved = g.getClip();
        g.clip(area);
        for (int y = (int) Math.floor(box.getMinY() / th) * th; y < box.getMaxY(); y += th) {
            for (int x = (int) Math.floor(box.getMinX() / tw) * tw; x < box.getMaxX(); x += tw) g.drawImage(img, x, y, null);
        }
        g.setClip(saved);
    }

    private static BufferedImage[] tilesAt(Level lv, ThemeArt art, double x, double y) {
        for (Level.Room r : lv.rooms) {
            for (Rectangle2D.Double p : r.parts) {
                if (p.contains(x, y)) return r.state == Level.Room.State.SAFE ? art.plaza : art.ground;
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

    /** The glow of one light at (x, y) (see {@link #drawGlows}; {@code glow}'s top byte picks its size). */
    void drawGlow(Graphics2D g, double x, double y, int glow) {
        int rgb = glow & 0xFFFFFF;
        int r = (glow >>> 24) == 2 ? 120 : 190;
        BufferedImage halo = glow(rgb, r, 92, false), pool = glow(rgb, (int) (r * 0.9), 56, true);
        g.drawImage(halo, (int) (x - r), (int) (y - 100 - r), null);
        g.drawImage(pool, (int) (x - r * 0.9), (int) (y + 18 - r * 0.45), null);
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
        boolean forest = lv.theme == Theme.FOREST, city = lv.theme == Theme.CITY, lab = lv.theme == Theme.LAB;
        Random rng = new Random(lv.rooms.size() * 7919L + lv.theme.ordinal() * 104729L);

        List<Rectangle2D> keepClear = new ArrayList<>();            // never put anything on the ground near a doorway
        for (Level.Door d : lv.doors) {
            Rectangle2D g = d.walk;
            keepClear.add(new Rectangle2D.Double(g.getX() - 110, g.getY() - 110, g.getWidth() + 220, g.getHeight() + 220));
        }

        if (a.patches.length > 0) {                                  // soft patches first, under everything else on the floor
            for (Level.Room room : lv.rooms) {
                if (room.state == Level.Room.State.SAFE) continue;
                for (Rectangle2D.Double b : room.parts) {
                    int n = (int) (b.getWidth() * b.getHeight() / 26000);
                    for (int i = 0; i < n; i++) {
                        double x = b.getX() + 40 + rng.nextDouble() * (b.getWidth() - 80), y = b.getY() + 40 + rng.nextDouble() * (b.getHeight() - 80);
                        bk.patches.add(new Prop(a.patches[rng.nextInt(a.patches.length)], x, y, rng.nextBoolean(), 0));
                    }
                }
            }
            for (Level.Door d : lv.doors) {                              // grass growing in over the edges of the paths
                if (!forest) break;
                Rectangle2D gp = d.gap;
                for (int side = 0; side < 2; side++) {
                    double len = d.vertical ? gp.getHeight() : gp.getWidth();
                    for (double pos = rng.nextDouble() * 20; pos < len; pos += 14 + rng.nextDouble() * 16) {
                        double in = rng.nextDouble() * 10 - 2;
                        double x = d.vertical ? (side == 0 ? gp.getX() + in : gp.getMaxX() - in) : gp.getX() + pos;
                        double y = d.vertical ? gp.getY() + pos : (side == 0 ? gp.getY() + in + 8 : gp.getMaxY() - in);
                        bk.flat.add(new Prop(a.floorProps[ForestArt.CLUMP + rng.nextInt(ForestArt.CLUMPS)], x, y, rng.nextBoolean(), 0));
                    }
                }
            }
        }

        for (Level.Room room : lv.rooms) {
            for (Rectangle2D.Double b : room.parts) {                    // a multi-part room just runs this once per piece: each piece's own
                                                                           // exterior gets scenery, and the check below already skips any edge
                                                                           // that's actually another piece of the same room, not a real wall
                // things lying on the floor (the city's boss room is bare steel: no road markings there)
                int n = (int) (b.getWidth() * b.getHeight() / (forest ? 15000 : lv.theme == Theme.LAB ? 26000 : 30000));
                for (int i = 0; i < n; i++) {
                    double x = b.getX() + 60 + rng.nextDouble() * (b.getWidth() - 120);
                    double y = b.getY() + 60 + rng.nextDouble() * (b.getHeight() - 120);
                    if (nearInteractive(lv, x, y)) continue;
                    if (city && room.state == Level.Room.State.SAFE && rng.nextInt(3) != 0) continue;   // the squares are swept
                    boolean wet = false;
                    for (Rectangle2D.Double wr : lv.water) if (inflate(wr, 30).contains(x, y)) wet = true;
                    if (wet) continue;
                    boolean blocked = false;
                    for (Rectangle2D k : keepClear) if (k.contains(x, y)) blocked = true;
                    if (blocked) continue;
                    Sprite s = a.floorProps[rng.nextInt(a.floorProps.length)];
                    if (forest && room.state == Level.Room.State.SAFE) {        // paving: only a sprig of grass here and there
                        if (rng.nextInt(3) != 0) continue;
                        s = a.floorProps[ForestArt.CLUMP + rng.nextInt(ForestArt.CLUMPS)];
                    }
                    bk.flat.add(new Prop(s, x, y, rng.nextBoolean(), 0));
                }
                if (city || lab) {                                        // the city (and the laboratory): fronts along the top, odds and ends elsewhere
                    lineStreet(lv, a, rng, b, bk.scenery);
                    continue;
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
                            else if (rng.nextInt(10) < 7) s = a.tall[rng.nextInt(a.tall.length)];
                            else s = a.low[rng.nextInt(a.low.length)];
                            bk.scenery.add(new Prop(s, x, y, rng.nextBoolean(), glow));
                        }
                        pos += 78 + rng.nextDouble() * 70;
                    }
                }
            }
        }
        bk.scenery.sort(Comparator.comparingDouble(p -> p.y));
    }

    /**
     * Lines one piece of a city street: house fronts side by side along its top edge (where we see them from the
     * street), and crates, barrels and planters now and then along the other three.
     */
    private static void lineStreet(Level lv, ThemeArt a, Random rng, Rectangle2D b, List<Prop> out) {
        double x = b.getX() - 30 - rng.nextDouble() * 30;
        while (x < b.getMaxX() + 30) {
            Sprite s = a.tall[rng.nextInt(a.tall.length)];
            double y = b.getY() - 16;
            if (!fitsOutside(lv, s, x + s.w * s.k * Art.SCALE / 2, y)) {     // too tall for the gap behind: the lowest one that fits
                Sprite best = null;
                for (Sprite o : a.tall) if (fitsOutside(lv, o, x + o.w * o.k * Art.SCALE / 2, y) && (best == null || o.h < best.h)) best = o;
                s = best;
            }
            double half = s == null ? 0 : s.w * s.k * Art.SCALE / 2;
            if (s != null && !nearDoor(lv, x + half * 0.4, y) && !nearDoor(lv, x + half * 1.6, y) && !nearDoor(lv, x + half, y)) {
                out.add(new Prop(s, x + half, y, false, 0));
                x += half * 2 - 2;
            } else {
                x += 40;
            }
        }
        for (int side = 1; side < 4; side++) {                              // 1 east, 2 south, 3 west
            boolean horizontal = side == 2;
            double len = horizontal ? b.getWidth() : b.getHeight();
            for (double pos = 60 + rng.nextDouble() * 80; pos < len - 40; pos += 150 + rng.nextDouble() * 160) {
                double px, py;
                switch (side) {
                    case 2 -> { px = b.getX() + pos; py = b.getMaxY() + 46; }
                    case 1 -> { px = b.getMaxX() + 40; py = b.getY() + pos; }
                    default -> { px = b.getX() - 40; py = b.getY() + pos; }
                }
                if (!outsideAll(lv, px, py) || nearDoor(lv, px, py)) continue;
                out.add(new Prop(a.low[rng.nextInt(a.low.length)], px, py, rng.nextBoolean(), 0));
            }
        }
    }

    /** True if a sprite standing at (x, y) is clear of every floor and corridor: it hides nothing you could walk on. */
    private static boolean fitsOutside(Level lv, Sprite s, double x, double y) {
        double w = s.w * s.k * Art.SCALE, h = s.h * s.k * Art.SCALE;
        Rectangle2D box = new Rectangle2D.Double(x - w / 2, y - h, w, h - 4);
        for (Level.Room r : lv.rooms) for (Rectangle2D.Double p : r.parts) if (box.intersects(p)) return false;
        for (Level.Door d : lv.doors) if (box.intersects(d.walk)) return false;
        return true;
    }


    private static boolean nearInteractive(Level lv, double x, double y) {
        for (Level.Npc n : lv.npcs) if (Util.dist(x, y, n.x(), n.y()) < 100) return true;
        for (Level.Treasure t : lv.treasures) if (Util.dist(x, y, t.x(), t.y()) < 70) return true;
        for (Level.Gate g : lv.gates) if (Util.dist(x, y, g.x(), g.y()) < 130) return true;
        for (Util.Vec v : lv.nestSpots) if (Util.dist(x, y, v.x(), v.y()) < 90) return true;
        return Util.dist(x, y, lv.spawnX, lv.spawnY) < 80;
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
