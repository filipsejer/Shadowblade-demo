package game;

import static game.Level.Dir.EAST;
import static game.Level.Dir.NORTH;
import static game.Level.Dir.SOUTH;
import static game.Level.Dir.WEST;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The explorable worlds: places to walk around between fights, with people to talk to, chests to find and the gates
 * to challenges. No monsters live here. So far there is one, the Whispering Forest.
 */
final class Worlds {
    private Worlds() {}

    /** The main menu's backdrop: a forest clearing with trees and rocks around it (always the same). */
    static Level clearing() {
        Level.Builder b = new Level.Builder();
        Level.Room field = b.start("CLEARING", 2400, 2400, Level.Room.State.OPEN);
        List<Level.Door> doors = b.finish();
        Rectangle2D.Double f = field.bounds;
        Level lv = new Level(b.rooms, doors, f.getCenterX(), f.getCenterY());
        lv.theme = Theme.FOREST;
        java.util.Random rng = new java.util.Random(4242);
        String[] kinds = {"oak", "oak", "boulder", "stump", "bush"};
        for (int placed = 0, tries = 0; placed < 22 && tries < 600; tries++) {
            double x = f.x + 150 + rng.nextDouble() * (f.width - 300), y = f.y + 150 + rng.nextDouble() * (f.height - 300);
            if (Util.dist(x, y, f.getCenterX(), f.getCenterY()) < 420) continue;
            boolean ok = true;
            for (Level.Landmark l : lv.landmarks) if (Util.dist(x, y, l.x(), l.y()) < 260) ok = false;
            if (!ok) continue;
            lv.landmarks.add(new Level.Landmark(kinds[rng.nextInt(kinds.length)], x, y, 0));
            placed++;
        }
        return lv;
    }

    /**
     * The Whispering Forest, around the camp of Mossbrook.
     * <pre>
     *                       [ HOLLOW'S EDGE ]          (the gate to the Blighted Hollow)
     *                               |
     *   [ MUSHROOM RING ] -- [  OLD SHRINE  ]
     *                               |
     *   [ WEST THICKET ] -- [ MOSSBROOK CAMP ] -- [ SUNLIT PATH ] -- [ HUNTER'S CLEARING ]   (the gate to the fox dens)
     *                               |
     *                         [ RIVERBANK ]
     * </pre>
     */
    static Level forest() {
        Level.Builder b = new Level.Builder();
        Level.Room camp = b.start("MOSSBROOK CAMP", 1300, 900, Level.Room.State.SAFE);
        Level.Room shrine = b.attach(camp, NORTH, "OLD SHRINE", 1000, 800, Level.Room.State.OPEN);
        Level.Room edge = b.attach(shrine, NORTH, "HOLLOW'S EDGE", 1000, 720, Level.Room.State.OPEN);
        Level.Room ring = b.attach(shrine, WEST, "MUSHROOM RING", 820, 700, Level.Room.State.OPEN);
        Level.Room thicket = b.attach(camp, WEST, "WEST THICKET", 900, 800, Level.Room.State.OPEN);
        Level.Room path = b.attach(camp, EAST, "SUNLIT PATH", 1000, 600, Level.Room.State.OPEN);
        Level.Room clearing = b.attach(path, EAST, "HUNTER'S CLEARING", 900, 820, Level.Room.State.OPEN);
        Level.Room river = b.attach(camp, SOUTH, "RIVERBANK", 1100, 650, Level.Room.State.OPEN);
        List<Level.Door> doors = b.finish();

        Rectangle2D.Double c = camp.bounds;
        Level lv = new Level(b.rooms, doors, c.getCenterX(), c.getCenterY() + 130);
        lv.name = "THE WHISPERING FOREST";
        lv.theme = Theme.FOREST;
        lv.explorable = true;

        // Mossbrook: the elder by the middle, the trainer and the merchant at their stalls, a child running about
        lv.npcs.add(new Level.Npc("rowan", "ELDER ROWAN", "town.elder.idle", Level.Role.TALK, c.getCenterX() - 140, c.getCenterY() - 140));
        lv.npcs.add(new Level.Npc("ash", "RANGER ASH", "shop.combat", Level.Role.TRAINER, c.x + 240, c.y + 200));
        lv.npcs.add(new Level.Npc("bramble", "BRAMBLE", "shop.survival", Level.Role.MERCHANT, c.getMaxX() - 240, c.y + 200));
        lv.npcs.add(new Level.Npc("wren", "WREN", "town.child.idle", Level.Role.TALK, c.getCenterX() + 330, c.getMaxY() - 190));
        lv.landmarks.add(new Level.Landmark("oak", c.x + 90, c.getMaxY() - 70, 28));
        lv.landmarks.add(new Level.Landmark("oak", c.getMaxX() - 80, c.getMaxY() - 90, 28));
        lv.landmarks.add(new Level.Landmark("bush", c.x + 210, c.getMaxY() - 60, 0));
        lv.landmarks.add(new Level.Landmark("stump", c.getCenterX() - 330, c.getMaxY() - 150, 18));

        // the old shrine: ruins, a hermit who knows things, and the grandest chest in the forest
        Rectangle2D.Double s = shrine.bounds;
        lv.npcs.add(new Level.Npc("hermit", "THE HERMIT", "guide.idle", Level.Role.TALK, s.getCenterX(), s.getCenterY() - 110));
        lv.treasures.add(new Level.Treasure("shrine", s.getMaxX() - 150, s.getMaxY() - 150, true));
        for (double[] p : new double[][]{{0.2, 0.25}, {0.8, 0.25}, {0.15, 0.7}, {0.62, 0.78}}) {
            lv.landmarks.add(new Level.Landmark("boulder", s.x + s.width * p[0], s.y + s.height * p[1], 22));
        }

        // the edge of the Hollow: the gate to the main challenge, among dead stumps
        Rectangle2D.Double e = edge.bounds;
        lv.gates.add(new Level.Gate(Challenge.HOLLOW, e.getCenterX(), e.getCenterY() - 60));
        for (double[] p : new double[][]{{0.18, 0.3}, {0.82, 0.28}, {0.25, 0.78}, {0.76, 0.74}, {0.5, 0.86}}) {
            lv.landmarks.add(new Level.Landmark("stump", e.x + e.width * p[0], e.y + e.height * p[1], 18));
        }

        // the mushroom ring: a chest in the middle of it
        Rectangle2D.Double m = ring.bounds;
        lv.treasures.add(new Level.Treasure("mushroom", m.getCenterX(), m.getCenterY() + 20, false));
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2 / 7;
            lv.landmarks.add(new Level.Landmark("bush", m.getCenterX() + Math.cos(a) * 210, m.getCenterY() + 20 + Math.sin(a) * 150, 0));
        }

        // the west thicket: dense trees, and a chest tucked into the far corner
        Rectangle2D.Double t = thicket.bounds;
        lv.treasures.add(new Level.Treasure("thicket", t.x + 120, t.y + 130, false));
        for (double[] p : new double[][]{{0.35, 0.2}, {0.55, 0.32}, {0.3, 0.48}, {0.7, 0.62}, {0.45, 0.75}, {0.2, 0.82}, {0.82, 0.2}}) {
            lv.landmarks.add(new Level.Landmark("oak", t.x + t.width * p[0], t.y + t.height * p[1], 28));
        }
        lv.landmarks.add(new Level.Landmark("bush", t.x + 230, t.y + 170, 0));

        // the sunlit path: a small chest by the wayside
        Rectangle2D.Double sp = path.bounds;
        lv.treasures.add(new Level.Treasure("path", sp.getCenterX() + 120, sp.y + 110, false));
        lv.landmarks.add(new Level.Landmark("oak", sp.x + 200, sp.y + 90, 28));
        lv.landmarks.add(new Level.Landmark("oak", sp.getMaxX() - 180, sp.getMaxY() - 70, 28));
        lv.landmarks.add(new Level.Landmark("boulder", sp.getCenterX() - 160, sp.getMaxY() - 90, 22));

        // the hunter's clearing: Fenn, and the way to the fox dens
        Rectangle2D.Double h = clearing.bounds;
        lv.npcs.add(new Level.Npc("fenn", "HUNTER FENN", "town.merchant.idle", Level.Role.TALK, h.getCenterX() - 190, h.getCenterY() - 40));
        lv.gates.add(new Level.Gate(Challenge.FOX_DENS, h.getCenterX() + 190, h.getCenterY() + 40));
        lv.landmarks.add(new Level.Landmark("log", h.x + 160, h.getMaxY() - 120, 0));
        lv.landmarks.add(new Level.Landmark("stump", h.getMaxX() - 140, h.y + 140, 18));
        lv.landmarks.add(new Level.Landmark("oak", h.getMaxX() - 110, h.getMaxY() - 90, 28));

        // the riverbank: a chest, and a glowing seed
        Rectangle2D.Double r = river.bounds;
        lv.treasures.add(new Level.Treasure("river", r.x + 160, r.getMaxY() - 130, false));
        lv.treasures.add(new Level.Treasure("seed", r.getMaxX() - 170, r.getCenterY(), false));
        lv.landmarks.add(new Level.Landmark("boulder", r.getCenterX(), r.getMaxY() - 110, 22));
        lv.landmarks.add(new Level.Landmark("bush", r.getCenterX() + 220, r.y + 120, 0));

        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, c.x + 330, c.getCenterY() + 60, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, c.getMaxX() - 330, c.getCenterY() + 40, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, sp.x + 380, sp.getMaxY() - 120, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, t.getMaxX() - 160, t.getMaxY() - 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, r.x + 420, r.y + 150, 1));
        return lv;
    }
}
