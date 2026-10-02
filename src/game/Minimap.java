package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * A Kingdom Hearts 2 style radar: a round map in the top-right corner with a gold bezel. It's centred on the player
 * (the arrow in the middle points where you're facing) and scrolls beneath you; north is always up.
 * Rooms only appear once you've been inside them, unless {@link #REVEAL_ALL} is switched on.
 *
 * <p>Holding TAB ({@link World#mapZoom}) grows it out of the corner into a big map in the middle of the screen,
 * zoomed out to show everything you've explored, with the areas' names on it.
 */
final class Minimap {
    static final double RADIUS = 86;        // pixels
    static final double SCALE = 0.11;       // pixels per world unit: the radar shows about 780 units around you
    static final double MARGIN = 20;
    /** false = fog of war (rooms show up when you enter them). true = the whole map is always visible. */
    static final boolean REVEAL_ALL = false;

    private static final Color BACKDROP = new Color(8, 14, 30, 205);
    private static final Color ROOM_SAFE = new Color(110, 150, 215, 200);
    private static final Color ROOM_CLEARED = new Color(90, 172, 152, 210);
    private static final Color CORRIDOR_OPEN = new Color(160, 182, 222, 210);
    private static final Color WALL = new Color(225, 235, 255, 230);
    private static final Color BEZEL = new Color(224, 190, 96);
    private static final Color ENEMY = new Color(240, 60, 60);
    private static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Font AREA = new Font(Font.SERIF, Font.BOLD, 13);
    /** The big map's radius, as a share of the screen's shorter side. */
    static final double BIG = 0.40;

    static double centerX(int screenWidth) { return screenWidth - MARGIN - RADIUS; }

    static double centerY() { return MARGIN + RADIUS; }

    void draw(Graphics2D g, World w, int screenWidth, int screenHeight) {
        Level level = w.level;
        Player p = w.player;
        double t = w.mapZoom * w.mapZoom * (3 - 2 * w.mapZoom);           // eased
        // small: in the corner, centred on you, at the radar's scale; big: mid-screen, everything you've explored
        Rectangle2D explored = explored(level, p);
        double bigRadius = Math.min(screenWidth, screenHeight) * BIG;
        double fit = Math.min(SCALE * 1.5, bigRadius * 2 * 0.9 / Math.max(1, Math.hypot(explored.getWidth(), explored.getHeight())));
        double radius = lerp(RADIUS, bigRadius, t);
        double cx = lerp(centerX(screenWidth), screenWidth / 2.0, t), cy = lerp(centerY(), screenHeight * 0.54, t);   // (a touch low: the area's name stays readable above it)
        double scale = lerp(SCALE, fit, t);
        double fx = lerp(p.x, explored.getCenterX(), t), fy = lerp(p.y, explored.getCenterY(), t);
        Ellipse2D disc = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2);

        if (t > 0) {                                                       // the world dims behind the big map
            g.setColor(new Color(0, 0, 0, (int) (120 * t)));
            g.fillRect(0, 0, screenWidth, screenHeight);
        }
        g.setColor(t > 0 ? new Color(BACKDROP.getRed(), BACKDROP.getGreen(), BACKDROP.getBlue(), (int) lerp(BACKDROP.getAlpha(), 238, t)) : BACKDROP);
        g.fill(disc);

        AffineTransform screen = g.getTransform();
        Shape savedClip = g.getClip();
        g.clip(disc);
        g.translate(cx, cy);
        g.scale(scale, scale);
        g.translate(-fx, -fy);
        double px = 1 / scale;              // one screen pixel, in world units

        drawMap(g, level, px);
        drawMarkers(g, w, px);

        g.setTransform(screen);
        if (t > 0.5 && level.rooms.size() > 1) drawAreaNames(g, level, cx, cy, scale, fx, fy, (t - 0.5) * 2);
        g.setClip(savedClip);
        drawBezel(g, cx, cy, radius);
        drawArrow(g, cx + (p.x - fx) * scale, cy + (p.y - fy) * scale, p.facing);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /** The part of the map you've seen (the rooms you've been in, and wherever you stand): what the big map shows. */
    private static Rectangle2D explored(Level level, Player p) {
        Rectangle2D.Double box = new Rectangle2D.Double(p.x - 300, p.y - 300, 600, 600);
        for (Level.Room r : level.rooms) if (REVEAL_ALL || r.visited) Rectangle2D.union(box, r.bounds, box);
        return box;
    }

    /** Each explored area's name, on the big map. */
    private void drawAreaNames(Graphics2D g, Level level, double cx, double cy, double scale, double fx, double fy, double alpha) {
        g.setFont(AREA);
        FontMetrics fm = g.getFontMetrics();
        for (Level.Room r : level.rooms) {
            if (!(REVEAL_ALL || r.visited) || r.name.isEmpty()) continue;
            Rectangle2D.Double big = r.parts.get(0);
            for (Rectangle2D.Double part : r.parts) if (part.width * part.height > big.width * big.height) big = part;
            double x = cx + (big.getCenterX() - fx) * scale, y = cy + (big.getCenterY() - fy) * scale;
            float tx = (float) (x - fm.stringWidth(r.name) / 2.0), ty = (float) (y + fm.getAscent() / 2.0 - 2);
            g.setColor(new Color(0, 0, 0, (int) (190 * alpha)));
            g.drawString(r.name, tx + 1, ty + 1);
            g.setColor(new Color(255, 240, 200, (int) (235 * alpha)));
            g.drawString(r.name, tx, ty);
        }
    }

    private void drawMap(Graphics2D g, Level level, double px) {
        Area shown = new Area();

        for (Level.Door d : level.doors) {
            if (!REVEAL_ALL && !d.a.visited && !d.b.visited) continue;
            g.setColor(CORRIDOR_OPEN);
            g.fill(d.gap);
            shown.add(new Area(d.gap));
        }
        for (Level.Room r : level.rooms) {
            if (!REVEAL_ALL && !r.visited) continue;
            g.setColor(r.state == Level.Room.State.SAFE ? ROOM_SAFE : ROOM_CLEARED);
            for (Rectangle2D.Double p : r.parts) {
                g.fill(p);
                shown.add(new Area(p));
            }
        }
        g.setColor(WALL);
        g.setStroke(new BasicStroke((float) (1.6 * px)));
        g.draw(shown);
    }

    private void drawMarkers(Graphics2D g, World w, double px) {
        Level lv = w.level;
        for (Level.Npc n : lv.npcs) {                   // people: blue; the trainer and the merchant: gold
            if (!seen(lv, n.x(), n.y())) continue;
            square(g, n.x(), n.y(), 3.2 * px, px, n.role() == Level.Role.TALK ? new Color(120, 200, 255) : new Color(255, 214, 90));
        }
        for (Level.Treasure t : lv.treasures) {         // chests you haven't opened yet
            if (w.adventure == null || w.adventure.opened.contains(t.id()) || !seen(lv, t.x(), t.y())) continue;
            square(g, t.x(), t.y(), 2.6 * px, px, new Color(255, 170, 60));
        }
        for (Level.Gate gate : lv.gates) {              // the way into a challenge
            if (!seen(lv, gate.x(), gate.y())) continue;
            double r = 5 * px;
            g.setColor(new Color(15, 20, 35));
            g.fill(new Ellipse2D.Double(gate.x() - r - px, gate.y() - r - px, 2 * r + 2 * px, 2 * r + 2 * px));
            g.setColor(new Color(200, 140, 255));
            g.fill(new Ellipse2D.Double(gate.x() - r, gate.y() - r, 2 * r, 2 * r));
        }
        for (Level.Road road : lv.roads) {              // a road out to another world: a gold diamond
            if (!seen(lv, road.x(), road.y())) continue;
            double r = 5.5 * px;
            java.awt.geom.Path2D d = new java.awt.geom.Path2D.Double();
            d.moveTo(road.x(), road.y() - r); d.lineTo(road.x() + r, road.y()); d.lineTo(road.x(), road.y() + r); d.lineTo(road.x() - r, road.y()); d.closePath();
            g.setColor(new Color(15, 20, 35));
            g.setStroke(new BasicStroke((float) (2 * px)));
            g.draw(d);
            g.setColor(new Color(255, 214, 120));
            g.fill(d);
        }
        if (w.run != null) {
            for (Relay r : w.run.relays) {              // relays: dim until powered, then lit
                double rr = 5 * px;
                g.setColor(new Color(15, 20, 35));
                g.fill(new Ellipse2D.Double(r.x - rr - px, r.y - rr - px, 2 * rr + 2 * px, 2 * rr + 2 * px));
                g.setColor(r.done ? Run.RELAY_LIGHT : r.started ? new Color(255, 170, 70) : new Color(170, 150, 120));
                g.fill(new Ellipse2D.Double(r.x - rr, r.y - rr, 2 * rr, 2 * rr));
            }
            Ward ward = w.run.ward;
            if (ward != null) {                         // what you protect: Copper's route still ahead of him, and him (or the engine)
                if (ward.kind == Ward.Kind.ROBOT && w.run.nestsLeft > 0) {
                    java.awt.geom.Path2D route = new java.awt.geom.Path2D.Double();
                    route.moveTo(ward.x, ward.y);
                    for (double d = ward.along; d <= ward.length; d += 120) { Util.Vec v = ward.pointAt(d); route.lineTo(v.x(), v.y()); }
                    Util.Vec end = ward.pointAt(ward.length);
                    route.lineTo(end.x(), end.y());
                    g.setStroke(new BasicStroke((float) (2.2 * px), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{(float) (4 * px), (float) (4 * px)}, 0));
                    g.setColor(new Color(255, 190, 110, 170));
                    g.draw(route);
                    square(g, end.x(), end.y(), 4 * px, px, new Color(255, 214, 120));
                }
                double rr = 5.5 * px;
                g.setColor(new Color(15, 20, 35));
                g.fill(new Ellipse2D.Double(ward.x - rr - px, ward.y - rr - px, 2 * rr + 2 * px, 2 * rr + 2 * px));
                g.setColor(ward.broken ? new Color(255, 110, 90) : ward.kind == Ward.Kind.ROBOT ? Ward.COPPER_LIGHT : Ward.FROST);
                g.fill(new Ellipse2D.Double(ward.x - rr, ward.y - rr, 2 * rr, 2 * rr));
            }
            for (Pickup pk : w.run.pickups) {
                if (pk.kind == Pickup.Kind.CACHE) square(g, pk.x, pk.y, 3 * px, px, new Color(255, 214, 90));
                else if (pk.kind == Pickup.Kind.PORTAL) square(g, pk.x, pk.y, 4 * px, px, new Color(200, 150, 255));
            }
        }

        Enemy locked = w.lockedTarget();
        for (Enemy e : w.enemies) {
            if (e.hp <= 0) continue;
            boolean boss = e.type == Enemy.Type.BOSS || e.rooted() || e.specimen;
            double r = (boss ? 6 : 3.2) * px;
            if (e.intangible()) {                   // a shade in shadow mode: just a faint violet ring
                g.setColor(new Color(170, 130, 235, 200));
                g.setStroke(new BasicStroke((float) (1.3 * px)));
                g.draw(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
                continue;
            }
            g.setColor(e.rooted() || e.specimen ? new Color(230, 100, 255) : boss ? new Color(255, 90, 110) : ENEMY);
            g.fill(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
            if (boss) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke((float) (1.2 * px)));
                g.draw(new Ellipse2D.Double(e.x - r - 2 * px, e.y - r - 2 * px, 2 * r + 4 * px, 2 * r + 4 * px));
            }
            if (e == locked) {                  // the target you've locked onto gets a white ring
                double lr = r + 3 * px;
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke((float) (1.6 * px)));
                g.draw(new Ellipse2D.Double(e.x - lr, e.y - lr, 2 * lr, 2 * lr));
            }
        }
    }

    /** True if the point is in a room you've been to (the radar only shows what you've seen). */
    private static boolean seen(Level lv, double x, double y) {
        Level.Room r = lv.roomAt(x, y, 0);
        return REVEAL_ALL || r != null && r.visited;
    }

    private static void square(Graphics2D g, double x, double y, double h, double px, Color c) {
        g.setColor(new Color(15, 20, 35));
        g.fill(new Rectangle2D.Double(x - h - px, y - h - px, 2 * h + 2 * px, 2 * h + 2 * px));
        g.setColor(c);
        g.fill(new Rectangle2D.Double(x - h, y - h, 2 * h, 2 * h));
    }

    private void drawBezel(Graphics2D g, double cx, double cy, double radius) {
        g.setColor(new Color(10, 12, 22, 220));
        g.setStroke(new BasicStroke(8f));
        g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setColor(BEZEL);
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setColor(new Color(255, 245, 205, 140));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(cx - radius + 3, cy - radius + 3, radius * 2 - 6, radius * 2 - 6));

        g.setFont(LABEL);                       // north marker, since the map never rotates
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString("N", (float) (cx - fm.stringWidth("N") / 2.0 + 1), (float) (cy - radius + 20));
        g.setColor(BEZEL);
        g.drawString("N", (float) (cx - fm.stringWidth("N") / 2.0), (float) (cy - radius + 19));
    }

    /** You: a small arrow (in the middle of the radar, or wherever you are on the big map), pointing the way you're facing. */
    private void drawArrow(Graphics2D g, double cx, double cy, double facing) {
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.rotate(facing);
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(10, 0);
        arrow.lineTo(-6, 6.5);
        arrow.lineTo(-2.5, 0);
        arrow.lineTo(-6, -6.5);
        arrow.closePath();
        g.setColor(new Color(10, 12, 22, 230));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);
        g.setColor(new Color(255, 250, 225));
        g.fill(arrow);
        g.setTransform(saved);
    }
}
