package game;

import static game.Level.Dir.EAST;
import static game.Level.Dir.NORTH;
import static game.Level.Dir.SOUTH;
import static game.Level.Dir.WEST;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The explorable worlds: places to walk around between fights, with people to talk to, chests to find and the gates
 * to challenges. No monsters live here. Roads join them (see {@link Level.Road}): the Whispering Forest (chapter 1),
 * the city of Lumen (chapter 2) and Stormcliff, Doctor Morrow's laboratory on the sea cliffs (chapter 3).
 */
final class Worlds {
    private Worlds() {}

    static final String FOREST = "forest", CITY = "city", LAB = "lab";
    /** Every world, in the order the story reaches them. */
    static final String[] IDS = {FOREST, CITY, LAB};

    static boolean exists(String id) {
        for (String w : IDS) if (w.equals(id)) return true;
        return false;
    }

    /** A world by its id (the forest for anything unknown). */
    static Level of(String id) {
        return switch (id) {
            case CITY -> city();
            case LAB -> lab();
            default -> forest();
        };
    }

    /** How you'd say you were going back there ("the forest"). */
    static String home(String id) {
        return switch (id) { case CITY -> "the city"; case LAB -> "Stormcliff"; default -> "the forest"; };
    }

    /** Who trains you there (spends your skill points). */
    static String trainer(String id) {
        return switch (id) { case CITY -> "Sable"; case LAB -> "Brass"; default -> "Ranger Ash"; };
    }

    /** A world's name, as the main menu and the map write it. */
    static String title(String id) {
        return switch (id) {
            case CITY -> "Lumen, the City of Lamps";
            case LAB -> "Stormcliff, Morrow's Laboratory";
            default -> "The Whispering Forest";
        };
    }

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
        lv.world = FOREST;
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
        lv.roads.add(new Level.Road(CITY, "THE SOUTH ROAD", r.getCenterX() + 300, r.getMaxY() - 70, r.getCenterX() + 300, r.getMaxY() - 170));

        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, c.x + 330, c.getCenterY() + 60, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, c.getMaxX() - 330, c.getCenterY() + 40, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, sp.x + 380, sp.getMaxY() - 120, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, t.getMaxX() - 160, t.getMaxY() - 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, r.x + 420, r.y + 150, 1));
        return lv;
    }

    /**
     * Lumen, the City of Lamps: a night-time city whose lamps have flickered since a star fell into the Dynamo under it.
     * <pre>
     *                         [  NORTH GATE  ]                 [ CLOCKTOWER COURT ]
     *                                |                                  |
     *   [ CANAL WALK ] -- [  LANTERN SQUARE  ] ------------------- [ MARKET ROW ]
     *         |                      |
     *   [ ALLEY MOUTH ]       [ TINKER'S YARD ]      (the gates to the back alleys and the substation)
     *                                |
     *                         [ DYNAMO STEPS ]        (the gate to the Dynamo Tower)
     * </pre>
     */
    /** The city's margin round its outermost streets: room for the house fronts along the top ones to be seen. */
    private static final double EDGE = 300;

    static Level city() {
        Level.Builder b = new Level.Builder();
        Level.Room square = b.start("LANTERN SQUARE", 1400, 1000, Level.Room.State.SAFE);
        Level.Room gate = b.attach(square, NORTH, "NORTH GATE", 900, 640, Level.Room.State.OPEN);
        Level.Room market = b.attach(square, EAST, "MARKET ROW", 1200, 640, Level.Room.State.OPEN);
        Level.Room clock = b.attach(market, NORTH, "CLOCKTOWER COURT", 820, 700, Level.Room.State.SAFE);
        Level.Room canal = b.attach(square, WEST, "CANAL WALK", 1000, 760, Level.Room.State.OPEN);
        Level.Room alley = b.attach(canal, SOUTH, "ALLEY MOUTH", 800, 640, Level.Room.State.OPEN);
        Level.Room yard = b.attach(square, SOUTH, "TINKER'S YARD", 1100, 760, Level.Room.State.OPEN);
        Level.Room steps = b.attach(yard, SOUTH, "DYNAMO STEPS", 1000, 700, Level.Room.State.SAFE);
        List<Level.Door> doors = b.finish(EDGE);

        Rectangle2D.Double q = square.bounds;
        Level lv = new Level(b.rooms, doors, q.getCenterX(), q.getCenterY() + 200, EDGE);
        lv.name = "LUMEN, THE CITY OF LAMPS";
        lv.world = CITY;
        lv.theme = Theme.CITY;
        lv.explorable = true;

        // Lantern Square: the captain by the fountain, the duelist and the peddler at their stalls
        lv.landmarks.add(new Level.Landmark("fountain", q.getCenterX(), q.getCenterY() + 40, 74));
        lv.npcs.add(new Level.Npc("vell", "CAPTAIN VELL", "city.vell.idle", Level.Role.TALK, q.getCenterX() - 230, q.getCenterY() - 70));
        lv.npcs.add(new Level.Npc("sable", "SABLE", "shop.duelist", Level.Role.TRAINER, q.x + 250, q.y + 210));
        lv.npcs.add(new Level.Npc("nix", "NIX", "shop.peddler", Level.Role.MERCHANT, q.getMaxX() - 250, q.y + 210));
        for (double[] p : new double[][]{{0.12, 0.62}, {0.88, 0.62}, {0.32, 0.9}, {0.68, 0.9}}) {
            lv.landmarks.add(new Level.Landmark("lamp", q.x + q.width * p[0], q.y + q.height * p[1], 12));
        }
        lv.landmarks.add(new Level.Landmark("bench", q.x + 150, q.getMaxY() - 110, 0));
        lv.landmarks.add(new Level.Landmark("bench", q.getMaxX() - 150, q.getMaxY() - 110, 0));

        // the north gate: the road back to the forest, and a purse someone dropped on the way in
        Rectangle2D.Double g = gate.bounds;
        lv.roads.add(new Level.Road(FOREST, "THE NORTH ROAD", g.getCenterX(), g.y + 70, g.getCenterX(), g.y + 190));
        lv.treasures.add(new Level.Treasure("city.gate", g.x + 140, g.getMaxY() - 140, false));
        lv.landmarks.add(new Level.Landmark("lamp", g.getCenterX() - 170, g.y + 120, 12));
        lv.landmarks.add(new Level.Landmark("lamp", g.getCenterX() + 170, g.y + 120, 12));
        lv.landmarks.add(new Level.Landmark("planter", g.getMaxX() - 120, g.getCenterY() + 60, 30));

        // Market Row: the newsboy, the stalls, and a chest behind them
        Rectangle2D.Double m = market.bounds;
        lv.npcs.add(new Level.Npc("pip", "PIP", "city.pip.idle", Level.Role.TALK, m.getCenterX() - 120, m.getCenterY() + 40));
        for (int i = 0; i < 3; i++) lv.landmarks.add(new Level.Landmark("stall" + i, m.x + 230 + i * 330, m.y + 150, 44));
        lv.treasures.add(new Level.Treasure("city.market", m.getMaxX() - 120, m.getMaxY() - 120, false));
        lv.landmarks.add(new Level.Landmark("lamp", m.x + 120, m.getMaxY() - 90, 12));
        lv.landmarks.add(new Level.Landmark("cart", m.getCenterX() + 260, m.getMaxY() - 150, 36));
        lv.roads.add(new Level.Road(LAB, "THE COAST ROAD", m.getMaxX() - 90, m.getCenterY() + 40, m.getMaxX() - 220, m.getCenterY() + 40));

        // Clocktower Court: quiet, and the grandest chest in the city, with a spark of starlight beside it
        Rectangle2D.Double k = clock.bounds;
        lv.landmarks.add(new Level.Landmark("clock", k.getCenterX(), k.y + 230, 44));
        lv.treasures.add(new Level.Treasure("city.clock", k.x + 150, k.getMaxY() - 160, true));
        lv.treasures.add(new Level.Treasure("city.spark", k.getMaxX() - 150, k.getMaxY() - 160, false));
        lv.landmarks.add(new Level.Landmark("planter", k.x + 120, k.y + 130, 30));
        lv.landmarks.add(new Level.Landmark("planter", k.getMaxX() - 120, k.y + 130, 30));

        // Canal Walk: the rat-catcher by the water
        Rectangle2D.Double c = canal.bounds;
        lv.water.add(new Rectangle2D.Double(c.x, c.y, c.width, 190));
        lv.npcs.add(new Level.Npc("gus", "OLD GUS", "city.gus.idle", Level.Role.TALK, c.getCenterX() + 120, c.getCenterY() + 70));
        lv.landmarks.add(new Level.Landmark("lamp", c.x + 180, c.y + 250, 12));
        lv.landmarks.add(new Level.Landmark("lamp", c.getMaxX() - 260, c.y + 250, 12));
        lv.landmarks.add(new Level.Landmark("crates", c.x + 140, c.getMaxY() - 120, 34));

        // the alley mouth: the way into the back alleys, and a chest in the rubbish
        Rectangle2D.Double a = alley.bounds;
        lv.gates.add(new Level.Gate(Challenge.ALLEYS, a.getCenterX(), a.getCenterY() + 20));
        lv.treasures.add(new Level.Treasure("city.alley", a.x + 120, a.getMaxY() - 110, false));
        lv.landmarks.add(new Level.Landmark("crates", a.getMaxX() - 130, a.y + 150, 34));
        lv.landmarks.add(new Level.Landmark("hydrant", a.x + 130, a.y + 140, 12));

        // the tinker's yard: Juno's workshop, scrap, and the way into the substation
        Rectangle2D.Double y = yard.bounds;
        lv.npcs.add(new Level.Npc("juno", "TINKER JUNO", "city.juno.idle", Level.Role.TALK, y.x + 300, y.getCenterY() - 60));
        lv.gates.add(new Level.Gate(Challenge.SUBSTATION, y.getMaxX() - 260, y.getCenterY() + 20));
        lv.landmarks.add(new Level.Landmark("scrap", y.x + 140, y.getMaxY() - 130, 34));
        lv.landmarks.add(new Level.Landmark("pylon", y.getMaxX() - 120, y.y + 140, 30));
        lv.landmarks.add(new Level.Landmark("crates", y.getCenterX(), y.getMaxY() - 110, 34));
        lv.treasures.add(new Level.Treasure("city.yard", y.x + 120, y.y + 120, false));
        lv.landmarks.add(new Level.Landmark("lamp", y.getCenterX() - 60, y.y + 110, 12));
        lv.landmarks.add(new Level.Landmark("lamp", y.getCenterX() + 80, y.getMaxY() - 220, 12));

        // the Dynamo Steps: the tower's door at the top of them
        Rectangle2D.Double d = steps.bounds;
        lv.gates.add(new Level.Gate(Challenge.TOWER, d.getCenterX(), d.getCenterY() - 40));
        for (double[] p : new double[][]{{0.15, 0.3}, {0.85, 0.3}, {0.15, 0.78}, {0.85, 0.78}}) {
            lv.landmarks.add(new Level.Landmark("lamp", d.x + d.width * p[0], d.y + d.height * p[1], 12));
        }

        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, m.x + 140, m.getMaxY() - 160, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, m.x + 200, m.getMaxY() - 110, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, c.x + 260, c.getMaxY() - 90, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, a.getMaxX() - 220, a.getMaxY() - 120, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, y.getCenterX() + 170, y.getMaxY() - 140, 1));
        return lv;
    }

    /**
     * Stormcliff: Doctor Morrow's laboratory, bolted onto the sea cliffs up the coast from Lumen, in a storm that never
     * ends. Lightning rods bristle from every roof; the rain comes in sideways.
     * <pre>
     *                          [ OBSERVATORY LIFT ]          (the gate to the Observatory)
     *                                   |
     *   [ THE GREENHOUSE ] ------ [  THE ATRIUM  ] ------ [ EAST WING DOORS ]   (the gates to the greenhouse and the east wing)
     *                                   |
     *   [ LIGHTNING GARDEN ] -- [ THE GATEHOUSE ] -- [ THE SEA WALL ]
     *                                   |
     *                            [ THE CLIFF ROAD ]          (the road back to Lumen)
     * </pre>
     */
    static Level lab() {
        Level.Builder b = new Level.Builder();
        Level.Room atrium = b.start("THE ATRIUM", 1300, 950, Level.Room.State.SAFE);
        Level.Room lift = b.attach(atrium, NORTH, "OBSERVATORY LIFT", 900, 700, Level.Room.State.SAFE);
        Level.Room east = b.attach(atrium, EAST, "EAST WING DOORS", 1000, 760, Level.Room.State.OPEN);
        Level.Room green = b.attach(atrium, WEST, "THE GREENHOUSE", 1100, 900, Level.Room.State.SAFE);
        Level.Room gatehouse = b.attach(atrium, SOUTH, "THE GATEHOUSE", 1000, 700, Level.Room.State.OPEN);
        Level.Room rods = b.attach(gatehouse, WEST, "LIGHTNING GARDEN", 1000, 760, Level.Room.State.OPEN);
        Level.Room sea = b.attach(gatehouse, EAST, "THE SEA WALL", 1100, 640, Level.Room.State.OPEN);
        Level.Room road = b.attach(gatehouse, SOUTH, "THE CLIFF ROAD", 900, 640, Level.Room.State.OPEN);
        List<Level.Door> doors = b.finish(EDGE);

        Rectangle2D.Double q = atrium.bounds;
        Level lv = new Level(b.rooms, doors, q.getCenterX(), q.getCenterY() + 200, EDGE);
        lv.name = "STORMCLIFF";
        lv.world = LAB;
        lv.theme = Theme.LAB;
        lv.explorable = true;

        // the atrium: Ilse by the orrery with Copper, Brass at his sparring post, Quill at the archive's counter
        lv.landmarks.add(new Level.Landmark("orrery", q.getCenterX(), q.getCenterY() + 30, 70));
        lv.npcs.add(new Level.Npc("ilse", "DOCTOR ILSE", "lab.ilse.idle", Level.Role.TALK, q.getCenterX() - 240, q.getCenterY() - 80));
        lv.npcs.add(new Level.Npc("copper", "COPPER", "lab.copper.idle", Level.Role.TALK, q.getCenterX() - 130, q.getCenterY() - 60));
        lv.npcs.add(new Level.Npc("brass", "BRASS", "shop.brass", Level.Role.TRAINER, q.x + 250, q.y + 210));
        lv.npcs.add(new Level.Npc("quill", "QUILL", "shop.quill", Level.Role.MERCHANT, q.getMaxX() - 250, q.y + 210));
        for (double[] p : new double[][]{{0.1, 0.7}, {0.9, 0.7}}) lv.landmarks.add(new Level.Landmark("planter.star", q.x + q.width * p[0], q.y + q.height * p[1], 26));
        lv.landmarks.add(new Level.Landmark("bench.lab", q.x + 170, q.getMaxY() - 110, 30));
        lv.landmarks.add(new Level.Landmark("tank", q.getMaxX() - 150, q.getMaxY() - 120, 28));

        // the lift up to the Observatory, under the dome
        Rectangle2D.Double l = lift.bounds;
        lv.gates.add(new Level.Gate(Challenge.OBSERVATORY, l.getCenterX(), l.getCenterY() - 30));
        lv.landmarks.add(new Level.Landmark("telescope", l.x + 150, l.y + 170, 20));
        lv.landmarks.add(new Level.Landmark("coil", l.getMaxX() - 130, l.y + 150, 22));
        lv.landmarks.add(new Level.Landmark("coil", l.x + 130, l.getMaxY() - 130, 22));
        lv.treasures.add(new Level.Treasure("lab.spark", l.getMaxX() - 140, l.getMaxY() - 130, false));

        // the east wing's doors: the way in for Copper, vines already pushing through
        Rectangle2D.Double e = east.bounds;
        lv.gates.add(new Level.Gate(Challenge.EAST_WING, e.getCenterX() + 120, e.getCenterY() - 20));
        lv.landmarks.add(new Level.Landmark("tank", e.x + 160, e.y + 150, 28));
        lv.landmarks.add(new Level.Landmark("crates", e.getMaxX() - 130, e.getMaxY() - 120, 34));
        lv.landmarks.add(new Level.Landmark("cables", e.getCenterX() - 160, e.getMaxY() - 140, 0));
        lv.landmarks.add(new Level.Landmark("bench.lab", e.x + 180, e.getMaxY() - 110, 30));

        // the greenhouse: Fern among the star-plants, and the way into its overgrown half
        Rectangle2D.Double g = green.bounds;
        lv.npcs.add(new Level.Npc("fern", "FERN", "lab.fern.idle", Level.Role.TALK, g.getCenterX() + 150, g.getCenterY() + 60));
        lv.gates.add(new Level.Gate(Challenge.GREENHOUSE, g.x + 260, g.getCenterY() - 60));
        lv.treasures.add(new Level.Treasure("lab.greenhouse", g.getMaxX() - 140, g.y + 130, false));
        for (double[] p : new double[][]{{0.12, 0.2}, {0.55, 0.18}, {0.85, 0.42}, {0.12, 0.85}, {0.45, 0.82}, {0.8, 0.86}}) {
            lv.landmarks.add(new Level.Landmark("planter.star", g.x + g.width * p[0], g.y + g.height * p[1], 26));
        }

        // the gatehouse: where the road comes in
        Rectangle2D.Double h = gatehouse.bounds;
        lv.landmarks.add(new Level.Landmark("lamp", h.x + 160, h.y + 130, 12));
        lv.landmarks.add(new Level.Landmark("lamp", h.getMaxX() - 160, h.y + 130, 12));
        lv.landmarks.add(new Level.Landmark("crates", h.x + 140, h.getMaxY() - 120, 34));

        // the lightning garden: a field of rods, struck all night long
        Rectangle2D.Double r = rods.bounds;
        for (double[] p : new double[][]{{0.18, 0.25}, {0.5, 0.2}, {0.82, 0.28}, {0.3, 0.55}, {0.7, 0.58}, {0.16, 0.82}, {0.5, 0.86}}) {
            lv.landmarks.add(new Level.Landmark("rod", r.x + r.width * p[0], r.y + r.height * p[1], 10));
        }
        lv.treasures.add(new Level.Treasure("lab.rods", r.getCenterX() - 10, r.getCenterY() + 30, false));

        // the sea wall: the breakers below, and the grandest chest on the cliff
        Rectangle2D.Double w = sea.bounds;
        lv.water.add(new Rectangle2D.Double(w.x, w.getMaxY() - 170, w.width, 170));
        lv.treasures.add(new Level.Treasure("lab.seawall", w.getMaxX() - 160, w.y + 150, true));
        lv.landmarks.add(new Level.Landmark("rod", w.x + 170, w.y + 140, 10));
        lv.landmarks.add(new Level.Landmark("telescope", w.getCenterX(), w.y + 170, 20));

        // the cliff road: the way back to Lumen, and a satchel someone lost in the rain
        Rectangle2D.Double c = road.bounds;
        lv.roads.add(new Level.Road(CITY, "THE COAST ROAD", c.getCenterX(), c.getMaxY() - 70, c.getCenterX(), c.getMaxY() - 190));
        lv.treasures.add(new Level.Treasure("lab.road", c.x + 150, c.y + 140, false));
        lv.landmarks.add(new Level.Landmark("lamp", c.getCenterX() - 170, c.getMaxY() - 130, 12));
        lv.landmarks.add(new Level.Landmark("lamp", c.getCenterX() + 170, c.getMaxY() - 130, 12));

        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, e.x + 330, e.getCenterY() + 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, h.getMaxX() - 300, h.getMaxY() - 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, r.getMaxX() - 160, r.getMaxY() - 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.BARREL, g.getMaxX() - 260, g.getMaxY() - 140, 1));
        lv.breakables.add(new Breakable(Breakable.Kind.CRATE, w.x + 320, w.y + 120, 1));
        return lv;
    }
}
