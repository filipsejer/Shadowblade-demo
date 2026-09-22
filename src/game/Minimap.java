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
 */
final class Minimap {
    static final double RADIUS = 86;        // pixels
    static final double SCALE = 0.11;       // pixels per world unit: the radar shows about 780 units around you
    static final double MARGIN = 20;
    /** false = fog of war (rooms show up when you enter them). true = the whole map is always visible. */
    static final boolean REVEAL_ALL = false;

    private static final Color BACKDROP = new Color(8, 14, 30, 205);
    private static final Color ROOM_SAFE = new Color(110, 150, 215, 200);
    private static final Color ROOM_UNVISITED = new Color(80, 92, 125, 140);
    private static final Color ROOM_COMBAT = new Color(205, 80, 80, 210);
    private static final Color ROOM_CLEARED = new Color(90, 172, 152, 210);
    private static final Color CORRIDOR_OPEN = new Color(160, 182, 222, 210);
    private static final Color CORRIDOR_LOCKED = new Color(205, 80, 80, 230);
    private static final Color CORRIDOR_SEALED = new Color(165, 105, 225, 235);
    private static final Color WALL = new Color(225, 235, 255, 230);
    private static final Color BEZEL = new Color(224, 190, 96);
    private static final Color ENEMY = new Color(240, 60, 60);
    private static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    static double centerX(int screenWidth) { return screenWidth - MARGIN - RADIUS; }

    static double centerY() { return MARGIN + RADIUS; }

    void draw(Graphics2D g, World w, int screenWidth) {
        Level level = w.level;
        Player p = w.player;
        double cx = centerX(screenWidth), cy = centerY();
        Ellipse2D disc = new Ellipse2D.Double(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2);

        g.setColor(BACKDROP);
        g.fill(disc);

        AffineTransform screen = g.getTransform();
        Shape savedClip = g.getClip();
        g.clip(disc);
        g.translate(cx, cy);
        g.scale(SCALE, SCALE);
        g.translate(-p.x, -p.y);
        double px = 1 / SCALE;              // one screen pixel, in world units

        drawMap(g, level, px);
        drawMarkers(g, w, px);

        g.setTransform(screen);
        g.setClip(savedClip);
        drawBezel(g, cx, cy);
        drawArrow(g, cx, cy, p.facing);
    }

    private void drawMap(Graphics2D g, Level level, double px) {
        Area shown = new Area();

        for (Level.Door d : level.doors) {
            if (!REVEAL_ALL && !d.a.visited && !d.b.visited) continue;
            g.setColor(d.sealed ? CORRIDOR_SEALED : d.open() ? CORRIDOR_OPEN : CORRIDOR_LOCKED);
            g.fill(d.gap);
            shown.add(new Area(d.gap));
        }
        for (Level.Room r : level.rooms) {
            if (!REVEAL_ALL && !r.visited) continue;
            g.setColor(switch (r.state) {
                case SAFE -> ROOM_SAFE;
                case UNVISITED -> ROOM_UNVISITED;
                case COMBAT -> ROOM_COMBAT;
                case CLEARED -> ROOM_CLEARED;
            });
            g.fill(r.bounds);
            shown.add(new Area(r.bounds));
        }
        g.setColor(WALL);
        g.setStroke(new BasicStroke((float) (1.6 * px)));
        g.draw(shown);
    }

    private void drawMarkers(Graphics2D g, World w, double px) {
        for (Level.Station s : w.level.stations) {
            double h = 3.2 * px;
            g.setColor(new Color(15, 20, 35));
            g.fill(new Rectangle2D.Double(s.x() - h - px, s.y() - h - px, 2 * h + 2 * px, 2 * h + 2 * px));
            g.setColor(s.category().color);
            g.fill(new Rectangle2D.Double(s.x() - h, s.y() - h, 2 * h, 2 * h));
        }

        if (w.level.guideAppeared) {                    // the guide: a green marker, so you can find your way back to them
            double h = 3.6 * px;
            g.setColor(new Color(15, 20, 35));
            g.fill(new Rectangle2D.Double(w.level.guideX - h - px, w.level.guideY - h - px, 2 * h + 2 * px, 2 * h + 2 * px));
            g.setColor(new Color(70, 205, 95));
            g.fill(new Rectangle2D.Double(w.level.guideX - h, w.level.guideY - h, 2 * h, 2 * h));
        }

        Enemy locked = w.lockedTarget();
        for (Enemy e : w.enemies) {
            if (e.hp <= 0) continue;
            boolean boss = e.type == Enemy.Type.BOSS;
            double r = (boss ? 6 : 3.2) * px;
            if (e.intangible()) {                   // a shade in shadow mode: just a faint violet ring
                g.setColor(new Color(170, 130, 235, 200));
                g.setStroke(new BasicStroke((float) (1.3 * px)));
                g.draw(new Ellipse2D.Double(e.x - r, e.y - r, 2 * r, 2 * r));
                continue;
            }
            g.setColor(boss ? new Color(255, 90, 110) : ENEMY);
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

    private void drawBezel(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(10, 12, 22, 220));
        g.setStroke(new BasicStroke(8f));
        g.draw(new Ellipse2D.Double(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2));
        g.setColor(BEZEL);
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Ellipse2D.Double(cx - RADIUS, cy - RADIUS, RADIUS * 2, RADIUS * 2));
        g.setColor(new Color(255, 245, 205, 140));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(cx - RADIUS + 3, cy - RADIUS + 3, RADIUS * 2 - 6, RADIUS * 2 - 6));

        g.setFont(LABEL);                       // north marker, since the map never rotates
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString("N", (float) (cx - fm.stringWidth("N") / 2.0 + 1), (float) (cy - RADIUS + 20));
        g.setColor(BEZEL);
        g.drawString("N", (float) (cx - fm.stringWidth("N") / 2.0), (float) (cy - RADIUS + 19));
    }

    /** You: a small arrow in the middle of the radar, pointing the way you're facing. */
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
