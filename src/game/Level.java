package game;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One continuous map made of rectangular rooms joined by corridors. Everything lives in the same world (no scene
 * switching); the walkable area is just the union of the room and open-corridor rectangles.
 */
final class Level {
    /** How far in from a room's walls you must be to trigger it (so you're fully inside before doors lock). */
    static final double TRIGGER_INSET = 70;
    /** A corridor's walkable rectangle reaches this far into each room so bodies can slide through the doorway. */
    private static final double DOOR_OVERLAP = 60;
    private static final double BARRIER_THICKNESS = 12;
    private static final double CORRIDOR_LENGTH = 180;
    private static final double DOOR_WIDTH = 130;

    /** Builds up the list of enemies a room spawns: {@code new Roster().add(GRUNT, 4).add(RUNNER, 2)}. */
    static final class Roster {
        final List<Enemy.Type> types = new ArrayList<>();

        Roster add(Enemy.Type type, int count) {
            for (int i = 0; i < count; i++) types.add(type);
            return this;
        }
    }

    static final class Room {
        /** SAFE rooms have no enemies. UNVISITED rooms spawn theirs when entered. */
        enum State { SAFE, UNVISITED, COMBAT, CLEARED }

        final String name;
        /**
         * The room's shape: one rectangle, or several glued together into an L, a cross, an alcove off a bigger
         * chamber — anything a hand-drawn map might show, so long as every piece is an axis-aligned box. Built by
         * {@link Builder#start} / {@link Builder#attach} (the first piece) and {@link Builder#extend} (more pieces).
         */
        final List<Rectangle2D.Double> parts = new ArrayList<>();
        /** The bounding box of every part — not the room's actual shape, just "roughly where it is". Kept in sync as parts are added. */
        Rectangle2D.Double bounds;
        final List<Enemy.Type> spawns;
        State state;
        /** A gated room (the boss room) stays sealed until every other room has been cleared. */
        boolean gated;
        /** Set once the player has been inside; the minimap only shows visited rooms. */
        boolean visited;

        Room(String name, double x, double y, double w, double h, Roster roster) {
            this.name = name;
            this.parts.add(new Rectangle2D.Double(x, y, w, h));
            recomputeBounds();
            this.spawns = List.copyOf(roster.types);
            this.state = spawns.isEmpty() ? State.SAFE : State.UNVISITED;
        }

        private void recomputeBounds() {
            Rectangle2D.Double b = new Rectangle2D.Double(parts.get(0).x, parts.get(0).y, parts.get(0).width, parts.get(0).height);
            for (Rectangle2D.Double p : parts) Rectangle2D.union(b, p, b);
            bounds = b;
        }
    }

    /** A corridor between two rooms. It's closed while either room is in combat, or while it leads to a sealed room. */
    static final class Door {
        final Room a, b;
        final Rectangle2D.Double gap;    // the corridor proper, between the two rooms
        final Rectangle2D.Double walk;   // the gap plus some overlap into both rooms
        final boolean vertical;
        final List<Rectangle2D.Double> barriers = new ArrayList<>();   // drawn across the mouths while closed
        /** True while this door leads to a gated room that hasn't been unlocked yet. Set by {@link Level#refresh()}. */
        boolean sealed;

        /** Rooms {@code a} (above) and {@code b} (below), joined by a vertical corridor. */
        static Door vertical(Room a, Room b, double width) {
            double cx = a.bounds.getCenterX();
            Rectangle2D.Double gap = new Rectangle2D.Double(cx - width / 2, a.bounds.getMaxY(), width, b.bounds.y - a.bounds.getMaxY());
            Door d = new Door(a, b, gap, true, new Rectangle2D.Double(gap.x, gap.y - DOOR_OVERLAP, width, gap.height + 2 * DOOR_OVERLAP));
            d.barriers.add(new Rectangle2D.Double(gap.x, gap.y - BARRIER_THICKNESS, width, BARRIER_THICKNESS));
            d.barriers.add(new Rectangle2D.Double(gap.x, gap.getMaxY(), width, BARRIER_THICKNESS));
            return d;
        }

        /** Rooms {@code a} (left) and {@code b} (right), joined by a horizontal corridor. */
        static Door horizontal(Room a, Room b, double width) {
            double cy = a.bounds.getCenterY();
            Rectangle2D.Double gap = new Rectangle2D.Double(a.bounds.getMaxX(), cy - width / 2, b.bounds.x - a.bounds.getMaxX(), width);
            Door d = new Door(a, b, gap, false, new Rectangle2D.Double(gap.x - DOOR_OVERLAP, gap.y, gap.width + 2 * DOOR_OVERLAP, width));
            d.barriers.add(new Rectangle2D.Double(gap.x - BARRIER_THICKNESS, gap.y, BARRIER_THICKNESS, width));
            d.barriers.add(new Rectangle2D.Double(gap.getMaxX(), gap.y, BARRIER_THICKNESS, width));
            return d;
        }

        private Door(Room a, Room b, Rectangle2D.Double gap, boolean vertical, Rectangle2D.Double walk) {
            this.a = a;
            this.b = b;
            this.gap = gap;
            this.vertical = vertical;
            this.walk = walk;
        }

        boolean open() {
            return !sealed && a.state != Room.State.COMBAT && b.state != Room.State.COMBAT;
        }
    }

    /** A trainer in the hub who sells one category of upgrades. */
    record Station(String name, String flavor, Upgrade.Category category, double x, double y) {}

    /**
     * A single big piece of scenery placed by hand (the tutorial's tree, its logs and bushes). {@code kind} is "oak", "bush", "boulder",
     * "stump" or "log"; (x, y) is where it stands on the ground; a positive {@code radius} makes it solid.
     */
    record Landmark(String kind, double x, double y, double radius) {}

    /** A friendly local standing around in Transit Town: say hello and hear one short, unimportant line. */
    record Npc(String name, String portrait, String line, double x, double y) {}

    final List<Room> rooms;
    final List<Door> doors;
    final List<Station> stations;
    final List<Landmark> landmarks = new ArrayList<>();
    /** Crates and barrels to smash for XP (only the real levels have them). */
    final List<Breakable> breakables = new ArrayList<>();
    /** Chatty townsfolk (only Transit Town has them). */
    final List<Npc> npcs = new ArrayList<>();
    final double width, height;          // bounding box of the whole map
    final double spawnX, spawnY;         // where the player starts
    /** Where the guide NPC appears once this level's boss is dead — in Transit Town, this same spot is the train station. */
    final double guideX, guideY;
    private final List<Rectangle2D.Double> walkable = new ArrayList<>();

    String name = "LEVEL";
    String bossName = "GUARDIAN";
    Theme theme = Theme.FOREST;
    /** True only for {@link #town()}: the friendly hub between the opening story and level 1. */
    boolean town;
    /** Transit Town only: where the path out into the forest (level 1) is. */
    double forestX, forestY;
    /** Bumped whenever doors open or close, so anything cached from the map's shape knows to rebuild. */
    int version;
    /** Health / damage multipliers for big enemies (brutes, the boss)... */
    double enemyHpMult = 1, enemyDamageMult = 1;
    /** ...and for the small ones (grunts, runners, shooters, shades), which a level can toughen up separately. */
    double smallEnemyHpMult = 1, smallEnemyDamageMult = 1;

    double hpMult(Enemy.Type type) { return type.small() ? smallEnemyHpMult : enemyHpMult; }

    double damageMult(Enemy.Type type) { return type.small() ? smallEnemyDamageMult : enemyDamageMult; }
    /**
     * True once the boss is dead and the guide has appeared in the hub (only if there is a next level). In Transit
     * Town this instead means the train station has opened (once level 1 is behind you).
     */
    boolean guideAppeared;

    Level(List<Room> rooms, List<Door> doors, List<Station> stations, double spawnX, double spawnY, double guideX, double guideY) {
        this.rooms = rooms;
        this.doors = doors;
        this.stations = stations;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.guideX = guideX;
        this.guideY = guideY;
        double w = 0, h = 0;
        for (Room r : rooms) {
            w = Math.max(w, r.bounds.getMaxX());
            h = Math.max(h, r.bounds.getMaxY());
        }
        this.width = w;
        this.height = h;
        refresh();
    }

    // ------------------------------------------------------------------ layout

    private enum Dir { NORTH, EAST, SOUTH, WEST }

    /**
     * Lays rooms out by attaching each one to a side of another, centred on the shared axis (so the corridor between
     * them is a straight line through both room centres). Coordinates are normalised to be non-negative at the end.
     * A room isn't just one rectangle, either: {@link #extend} glues extra pieces onto one, so a room can be an L, a
     * cross, an alcove off a bigger chamber — anything built out of boxes.
     */
    private static final class Builder {
        private record Link(Room a, Room b, boolean vertical, double width) {}   // a is above / left of b

        final List<Room> rooms = new ArrayList<>();
        final List<Link> links = new ArrayList<>();

        Room start(String name, double w, double h, Roster roster) {
            Room r = new Room(name, 0, 0, w, h, roster);
            rooms.add(r);
            return r;
        }

        Room attach(Room from, Dir dir, String name, double w, double h, Roster roster) {
            return attach(from, dir, name, w, h, roster, DOOR_WIDTH, CORRIDOR_LENGTH);
        }

        /** Like the plain {@link #attach}, but with a corridor of your own width and length instead of the usual ones. */
        Room attach(Room from, Dir dir, String name, double w, double h, Roster roster, double corridorWidth, double corridorLength) {
            Rectangle2D.Double f = from.bounds;
            double x, y;
            switch (dir) {
                case NORTH -> { x = f.getCenterX() - w / 2; y = f.y - corridorLength - h; }
                case SOUTH -> { x = f.getCenterX() - w / 2; y = f.getMaxY() + corridorLength; }
                case EAST -> { x = f.getMaxX() + corridorLength; y = f.getCenterY() - h / 2; }
                default -> { x = f.x - corridorLength - w; y = f.getCenterY() - h / 2; }
            }
            Room room = new Room(name, x, y, w, h, roster);
            checkClear(room.parts.get(0), null, name);
            rooms.add(room);
            switch (dir) {
                case NORTH -> links.add(new Link(room, from, true, corridorWidth));
                case SOUTH -> links.add(new Link(from, room, true, corridorWidth));
                case EAST -> links.add(new Link(from, room, false, corridorWidth));
                default -> links.add(new Link(room, from, false, corridorWidth));
            }
            return room;
        }

        /**
         * Glues another rectangle onto {@code room}'s shape, at ({@code dx}, {@code dy}) relative to its first
         * piece's top-left corner. Not a new connected room and no corridor — just growing this room's own footprint.
         * You place it flush against the piece it's meant to join (no gap, no need to fudge an overlap yourself) —
         * {@link #bridge} grows it into that piece by a little on its own, the same reason a door's own walkable
         * area reaches a little way into the rooms on either side of it (see {@code DOOR_OVERLAP}): two separately
         * clamped rectangles that only ever touch at a single seam leave a body-radius-wide gap neither one claims,
         * where you'd get stuck.
         */
        Room extend(Room room, double dx, double dy, double w, double h) {
            Rectangle2D.Double origin = room.parts.get(0);
            Rectangle2D.Double part = new Rectangle2D.Double(origin.x + dx, origin.y + dy, w, h);
            bridge(part, room.parts);
            checkClear(part, room, "an extra piece of " + room.name);
            room.parts.add(part);
            room.recomputeBounds();
            return room;
        }

        /** How far two of a room's own pieces must overlap where they meet (see {@link #extend}). */
        private static final double PART_OVERLAP = 30;

        /** Grows {@code part}'s edges a little way into any of {@code existing} it sits exactly flush against. */
        private static void bridge(Rectangle2D.Double part, List<Rectangle2D.Double> existing) {
            for (Rectangle2D.Double op : existing) {
                boolean yShared = part.y < op.getMaxY() && part.getMaxY() > op.y;
                boolean xShared = part.x < op.getMaxX() && part.getMaxX() > op.x;
                if (yShared && Math.abs(part.x - op.getMaxX()) < 0.5) { part.x -= PART_OVERLAP; part.width += PART_OVERLAP; }
                if (yShared && Math.abs(part.getMaxX() - op.x) < 0.5) part.width += PART_OVERLAP;
                if (xShared && Math.abs(part.y - op.getMaxY()) < 0.5) { part.y -= PART_OVERLAP; part.height += PART_OVERLAP; }
                if (xShared && Math.abs(part.getMaxY() - op.y) < 0.5) part.height += PART_OVERLAP;
            }
        }

        /** Refuses a piece that (nearly) touches any room other than {@code exclude} — so rooms never bleed into each other. */
        private void checkClear(Rectangle2D.Double part, Room exclude, String label) {
            for (Room other : rooms) {
                if (other == exclude) continue;
                for (Rectangle2D.Double op : other.parts) {
                    Rectangle2D.Double padded = new Rectangle2D.Double(op.x - 60, op.y - 60, op.width + 120, op.height + 120);
                    if (padded.intersects(part)) throw new IllegalStateException(label + " overlaps " + other.name);
                }
            }
        }

        /** Shifts everything so the top-left of the map is (0, 0), then builds the doors. */
        List<Door> finish() {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            for (Room r : rooms) for (Rectangle2D.Double p : r.parts) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
            }
            for (Room r : rooms) {
                for (Rectangle2D.Double p : r.parts) {
                    p.x -= minX;
                    p.y -= minY;
                }
                r.recomputeBounds();
            }
            List<Door> doors = new ArrayList<>();
            for (Link l : links) {
                doors.add(l.vertical() ? Door.vertical(l.a(), l.b(), l.width()) : Door.horizontal(l.a(), l.b(), l.width()));
            }
            return doors;
        }
    }

    /** How many levels there are. */
    static final int COUNT = 3;

    /** Each level's boss and theme, in {@link #create} order — for previews (the chapter-select screen) without building the whole level. */
    static final String[] BOSS_NAMES = {"GUARDIAN", "WARDEN", "MAD SCIENTIST"};
    static final Theme[] THEMES = {Theme.FOREST, Theme.CITY, Theme.LAB};

    /** Level {@code index} (0 = the first). The hub, with its trainers, is the same in every level. */
    static Level create(int index) {
        return switch (index) {
            case 0 -> levelOne();
            case 1 -> levelTwo();
            default -> levelThree();
        };
    }

    /**
     * Level 1: a hub with a branch of rooms in each direction, and a sealed boss room to the west.
     * <pre>
     *                     [FERN HOLLOW]
     *                           |
     *      [MUSHROOM GROVE]-[MOSSY TRAIL]                [OLD SHRINE]
     *                           |                             |
     *   [BOSS] - - - - - - [   HUB   ] -- [BRAMBLE GATE] -- [SUNLIT GLADE]
     *                           |
     *                       [ BURROW ]
     *                           |
     *      [TREASURE GROVE]-[DEEP WOODS]
     * </pre>
     * The three trainers stand in the hub. The boss door opens once every other room is cleared.
     */
    private static Level levelOne() {
        Builder b = new Builder();
        Room hub = b.start("HUB", 1000, 700, new Roster());

        // north: a hall, a barracks beyond it, and a side room
        Room outpost = b.attach(hub, Dir.NORTH, "MOSSY TRAIL", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 3).add(Enemy.Type.RUNNER, 2));
        b.attach(outpost, Dir.NORTH, "FERN HOLLOW", 1000, 700,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHOOTER, 1));
        b.attach(outpost, Dir.EAST, "MUSHROOM GROVE", 800, 600,
            new Roster().add(Enemy.Type.GRUNT, 2).add(Enemy.Type.SHOOTER, 2).add(Enemy.Type.BRUTE, 1));

        // east: a gate, a courtyard, and a shrine off the courtyard
        Room gate = b.attach(hub, Dir.EAST, "BRAMBLE GATE", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHOOTER, 2));
        Room courtyard = b.attach(gate, Dir.EAST, "SUNLIT GLADE", 1100, 760,
            new Roster().add(Enemy.Type.GRUNT, 3).add(Enemy.Type.RUNNER, 3).add(Enemy.Type.SHOOTER, 2));
        b.attach(courtyard, Dir.NORTH, "OLD SHRINE", 800, 640,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.SHOOTER, 2).add(Enemy.Type.GRUNT, 2));

        // south: a cellar, a crypt, and a vault off the crypt
        Room cellar = b.attach(hub, Dir.SOUTH, "BURROW", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.RUNNER, 2));
        Room crypt = b.attach(cellar, Dir.SOUTH, "DEEP WOODS", 1000, 700,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHOOTER, 2));
        b.attach(crypt, Dir.WEST, "TREASURE GROVE", 900, 640,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.RUNNER, 3));

        // west: the boss, sealed until everything else is cleared
        Room boss = b.attach(hub, Dir.WEST, "BOSS ROOM", 1100, 800, new Roster().add(Enemy.Type.BOSS, 1));
        boss.gated = true;

        return finish(b, hub, "LEVEL 1", "GUARDIAN", Theme.FOREST, 1.0, 1.0, 1.0, 1.0);
    }

    /**
     * Level 2 (the city): a different shape. The boss is to the east, the west and south are longer trails, and the north is a
     * long tower of rooms with a side branch. Enemies are tougher (the small ones especially), and Shades appear.
     * <pre>
     *                                  [GREENHOUSE]
     *                                       |
     *                                  [OBSERVATORY]-[ARCHIVE]
     *                                       |
     *                [WORKSHOP]         [  LIBRARY  ]
     *                     |                 |
     *   [STOREROOM]-[  MARKET  ] ------ [   HUB   ] - - - - - - [BOSS]
     *                                       |
     *                                  [ KITCHEN ]-[PANTRY]
     *                                       |
     *                                  [COLD STORE]
     * </pre>
     */
    private static Level levelTwo() {
        Builder b = new Builder();
        Room hub = b.start("HUB", 1000, 700, new Roster());

        // west: a market with a workshop above it and a storeroom beyond it
        Room market = b.attach(hub, Dir.WEST, "MARKET", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.RUNNER, 3));
        b.attach(market, Dir.NORTH, "WORKSHOP", 800, 600,
            new Roster().add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.SHADE, 1));
        b.attach(market, Dir.WEST, "STOREROOM", 800, 600,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHADE, 2));

        // north: a tower of rooms, with an archive off the middle one
        Room library = b.attach(hub, Dir.NORTH, "LIBRARY", 1000, 700,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.SHOOTER, 2).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHADE, 1));
        Room observatory = b.attach(library, Dir.NORTH, "OBSERVATORY", 900, 640,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.GRUNT, 2).add(Enemy.Type.SHADE, 2));
        b.attach(observatory, Dir.EAST, "ARCHIVE", 800, 600,
            new Roster().add(Enemy.Type.RUNNER, 4).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.SHADE, 2));
        b.attach(observatory, Dir.NORTH, "GREENHOUSE", 1000, 700,
            new Roster().add(Enemy.Type.BRUTE, 3).add(Enemy.Type.SHOOTER, 2).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHADE, 2));

        // south: a kitchen with a pantry beside it and a cold store below it
        Room kitchen = b.attach(hub, Dir.SOUTH, "KITCHEN", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 5).add(Enemy.Type.RUNNER, 3));
        b.attach(kitchen, Dir.EAST, "PANTRY", 800, 600,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHADE, 2));
        b.attach(kitchen, Dir.SOUTH, "COLD STORE", 1000, 700,
            new Roster().add(Enemy.Type.BRUTE, 3).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHADE, 2));

        // east: the boss, sealed until everything else is cleared
        Room boss = b.attach(hub, Dir.EAST, "BOSS ROOM", 1200, 800, new Roster().add(Enemy.Type.BOSS, 1));
        boss.gated = true;

        return finish(b, hub, "LEVEL 2", "WARDEN", Theme.CITY, 1.35, 1.2, 1.9, 1.5);
    }

    /**
     * Level 3 (the laboratory): the boss room is to the north. The west is a chemistry wing with a mutant pen above it and a gene
     * lab beyond it; the east runs through the power room to the reactor and down to a coolant plant; the south is a specimen
     * vault, a cryo chamber and an observation deck. The toughest enemies yet, with Shades among them.
     * <pre>
     *                                   [   BOSS   ]
     *                                        :
     *   [MUTANT PENS]                        :
     *        |                               :
     *   [GENE LAB]-[CHEM LAB] ------- [   HUB   ] -- [POWER ROOM] -- [REACTOR]
     *                                        |                           |
     *                                  [SPECIMEN VAULT]            [COOLANT PLANT]
     *                                        |
     *                        [OBS. DECK]-[CRYO CHAMBER]
     * </pre>
     */
    private static Level levelThree() {
        Builder b = new Builder();
        Room hub = b.start("HUB", 1000, 700, new Roster());

        // west: chemistry, with pens of mutants above it and the gene lab beyond it
        Room chem = b.attach(hub, Dir.WEST, "CHEM LAB", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.RUNNER, 3).add(Enemy.Type.SHOOTER, 1));
        b.attach(chem, Dir.NORTH, "MUTANT PENS", 800, 600,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHADE, 1));
        b.attach(chem, Dir.WEST, "GENE LAB", 800, 600,
            new Roster().add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.RUNNER, 2).add(Enemy.Type.SHADE, 2));

        // east: the power room, the reactor, and the coolant plant below it
        Room power = b.attach(hub, Dir.EAST, "POWER ROOM", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 4).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.RUNNER, 3));
        Room reactor = b.attach(power, Dir.EAST, "REACTOR", 1000, 700,
            new Roster().add(Enemy.Type.BRUTE, 3).add(Enemy.Type.SHOOTER, 2).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHADE, 2));
        b.attach(reactor, Dir.SOUTH, "COOLANT PLANT", 900, 640,
            new Roster().add(Enemy.Type.BRUTE, 2).add(Enemy.Type.SHOOTER, 4).add(Enemy.Type.RUNNER, 3).add(Enemy.Type.SHADE, 2));

        // south: the specimen vault, a cryo chamber, and an observation deck beside it
        Room vault = b.attach(hub, Dir.SOUTH, "SPECIMEN VAULT", 900, 640,
            new Roster().add(Enemy.Type.GRUNT, 5).add(Enemy.Type.RUNNER, 3).add(Enemy.Type.SHADE, 1));
        Room cryo = b.attach(vault, Dir.SOUTH, "CRYO CHAMBER", 1000, 700,
            new Roster().add(Enemy.Type.BRUTE, 3).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.GRUNT, 3).add(Enemy.Type.SHADE, 2));
        b.attach(cryo, Dir.WEST, "OBSERVATION DECK", 800, 600,
            new Roster().add(Enemy.Type.BRUTE, 3).add(Enemy.Type.SHOOTER, 3).add(Enemy.Type.RUNNER, 4).add(Enemy.Type.SHADE, 2));

        // north: the mad scientist's sanctum, sealed until everything else is cleared
        Room boss = b.attach(hub, Dir.NORTH, "BOSS ROOM", 1200, 800, new Roster().add(Enemy.Type.BOSS, 1));
        boss.gated = true;

        return finish(b, hub, "LEVEL 3", "MAD SCIENTIST", Theme.LAB, 1.7, 1.4, 2.6, 1.9);
    }

    /**
     * The tutorial: a forest clearing where you wake up beside a tree, and a trail of three more rooms (the trail, the stream and the
     * hollow) where a squirrel is hiding. There are no enemies here on their own; the tutorial script brings the one you fight.
     * <pre>
     *   [ THE CLEARING ] ---- [ THE TRAIL ]
     *                              |
     *                         [ THE STREAM ] ---- [ THE HOLLOW ]
     * </pre>
     */
    static Level tutorial() {
        Builder b = new Builder();
        Room clearing = b.start("THE CLEARING", 1100, 760, new Roster());
        Room trail = b.attach(clearing, Dir.EAST, "THE TRAIL", 800, 600, new Roster());
        Room stream = b.attach(trail, Dir.SOUTH, "THE STREAM", 800, 600, new Roster());
        b.attach(stream, Dir.EAST, "THE HOLLOW", 900, 640, new Roster());
        List<Door> doors = b.finish();
        Rectangle2D.Double c = clearing.bounds;
        Level level = new Level(b.rooms, doors, List.of(), c.x + 300, c.y + 430, -1000, -1000);
        for (Room r : level.rooms) r.state = Room.State.CLEARED;          // open ground, not a plaza; nothing here spawns anything by itself
        level.name = "TUTORIAL";
        level.bossName = "";
        level.theme = Theme.FOREST;

        level.landmarks.add(new Landmark("oak", c.x + 230, c.y + 400, 30));         // the tree you woke up beside
        level.landmarks.add(new Landmark("bush", c.x + 100, c.y + 200, 0));
        level.landmarks.add(new Landmark("stump", c.x + 900, c.y + 600, 22));
        Rectangle2D.Double t = trail.bounds, s = stream.bounds, h = level.rooms.get(3).bounds;
        level.landmarks.add(new Landmark("bush", t.getMaxX() - 170, t.getMaxY() - 96, 0));      // the squirrel peeks out from behind these
        level.landmarks.add(new Landmark("bush", t.x + 150, t.y + 120, 0));
        level.landmarks.add(new Landmark("boulder", s.getMaxX() - 160, s.getMaxY() - 96, 0));
        level.landmarks.add(new Landmark("bush", s.x + 120, s.y + 160, 0));
        level.landmarks.add(new Landmark("log", h.getMaxX() - 130, h.getCenterY() + 40, 0));
        level.landmarks.add(new Landmark("bush", h.getMaxX() - 90, h.getCenterY() - 90, 0));
        return level;
    }

    /**
     * Transit Town: the friendly hub between the opening story and level 1. No enemies anywhere, just the square you
     * arrive in, an old quarter off to the east where the locals are, the edge of the woods to the west, and a couple
     * more places worth a look: a quiet fountain square to the north, and a back alley off the old quarter. The train
     * station (the same spot as every other level's "guide") stays shut until level 1 is behind you.
     * <pre>
     *                       [ FOUNTAIN SQUARE ]
     *                               |
     *   [ WOODS EDGE ] ---- [ TOWN SQUARE ] ---- [ OLD QUARTER ] ---- [ BACK ALLEY ]
     * </pre>
     */
    static Level town() {
        Builder b = new Builder();
        Room square = b.start("TOWN SQUARE", 1100, 760, new Roster());
        Room quarter = b.attach(square, Dir.EAST, "OLD QUARTER", 900, 640, new Roster());
        Room edge = b.attach(square, Dir.WEST, "WOODS EDGE", 800, 600, new Roster());
        b.attach(square, Dir.NORTH, "FOUNTAIN SQUARE", 900, 640, new Roster());
        b.attach(quarter, Dir.EAST, "BACK ALLEY", 800, 600, new Roster());
        List<Door> doors = b.finish();
        Rectangle2D.Double sq = square.bounds, oq = quarter.bounds, we = edge.bounds;

        double stationX = sq.getCenterX() + 260, stationY = sq.y + 190;
        Level level = new Level(b.rooms, doors, List.of(), sq.getCenterX(), sq.getCenterY(), stationX, stationY);
        level.name = "TRANSIT TOWN";
        level.bossName = "";
        level.theme = Theme.CITY;
        level.town = true;
        level.forestX = we.x + 260;                                                           // far enough from the wall that its "press E" prompt, centred, never runs off the edge of the map
        level.forestY = we.getCenterY();

        level.landmarks.add(new Landmark("oak", we.x + 30, we.getCenterY() - 160, 28));       // the woods crowd in at the edge of town
        level.landmarks.add(new Landmark("oak", we.x + 15, we.getCenterY() + 180, 30));
        level.landmarks.add(new Landmark("bush", we.x + 75, we.getCenterY() - 50, 0));
        level.landmarks.add(new Landmark("bush", we.x + 85, we.getCenterY() + 70, 0));

        level.npcs.add(new Npc("OLD TRAVELLER", "town.elder.idle",
            "Ah, a new face. Rest up a while before you head out there.", sq.x + 230, sq.getCenterY() - 170));
        level.npcs.add(new Npc("MERCHANT", "town.merchant.idle",
            "No wares today, I'm afraid - just passing through, same as you.", oq.getCenterX() - 140, oq.getCenterY()));
        level.npcs.add(new Npc("CHILD", "town.child.idle",
            "Everyone who comes through here ends up somewhere far away. Where will YOU go?", oq.getCenterX() + 210, oq.getMaxY() - 150));
        return level;
    }

    /**
     * Not a real level — a fixture for exercising multi-part rooms (an irregular main hall, a detached-looking alcove
     * that's still one room, and a custom-width corridor) so the shape system has something real to test against.
     * Never reachable in play.
     */
    static Level testShapes() {
        Builder b = new Builder();
        Room hall = b.start("MAIN HALL", 800, 600, new Roster());
        b.extend(hall, 800, 150, 300, 300);                  // a bump to the east, flush against the hall's right wall
        b.extend(hall, 100, 600, 250, 250);                  // and another to the south: together an L / cross-ish shape
        b.attach(hall, Dir.EAST, "SIDE ROOM", 700, 500, new Roster(), 300, 90);   // a wide, short corridor instead of the usual one
        List<Door> doors = b.finish();
        Level level = new Level(b.rooms, doors, List.of(), hall.bounds.getCenterX(), hall.bounds.getCenterY(), -1000, -1000);
        level.name = "SHAPE TEST";
        level.theme = Theme.FOREST;
        for (Room r : level.rooms) r.state = Room.State.CLEARED;
        return level;
    }

    /** Test-only: {@link Builder#extend} refuses a piece that overlaps another room, exactly like {@link Builder#attach} always has. */
    static void testShapesOverlapping() {
        Builder b = new Builder();
        Room hall = b.start("MAIN HALL", 800, 600, new Roster());
        b.attach(hall, Dir.EAST, "SIDE ROOM", 700, 500, new Roster());
        b.extend(hall, 850, 0, 400, 400);   // reaches well past where SIDE ROOM already is
    }

    /** Builds the doors and puts the hub's furniture in: the three trainers, and the spot the guide appears. */
    private static Level finish(Builder b, Room hub, String name, String bossName, Theme theme, double hpMult, double dmgMult,
                                double smallHpMult, double smallDmgMult) {
        List<Door> doors = b.finish();
        Rectangle2D.Double h = hub.bounds;
        List<Station> stations = List.of(
            new Station("SWORDSMASTER", "Sharpen your blade and lengthen your combos.", Upgrade.Category.COMBAT, h.x + 190, h.y + 160),
            new Station("WIZARD", "Learn new spells and master the ones you know.", Upgrade.Category.SPELLS, h.getMaxX() - 190, h.y + 160),
            new Station("SURVIVALIST", "Toughen up: health, mana, speed and evasion.", Upgrade.Category.SURVIVAL, h.x + 190, h.getMaxY() - 160));
        Level level = new Level(b.rooms, doors, stations, h.getCenterX(), h.getCenterY(), h.getCenterX() + 240, h.getCenterY() + 130);
        level.name = name;
        level.bossName = bossName;
        level.theme = theme;
        level.enemyHpMult = hpMult;
        level.enemyDamageMult = dmgMult;
        level.smallEnemyHpMult = smallHpMult;
        level.smallEnemyDamageMult = smallDmgMult;
        level.scatterBreakables();
        return level;
    }

    /**
     * Stands two or three crates and barrels in every room except the boss's: away from the walls, the doorways, the middle of the room, the trainers, the guide's
     * spot and each other. The same level always gets the same ones.
     */
    private void scatterBreakables() {
        Random rng = new Random(rooms.size() * 31L + theme.ordinal() * 7919L + 17);
        double xpScale = 1 + 0.5 * theme.ordinal();
        for (Room r : rooms) {
            if (r.gated) continue;
            List<Rectangle2D.Double> roomy = new ArrayList<>();          // pieces big enough to comfortably hold one; a narrow alcove stays empty
            for (Rectangle2D.Double p : r.parts) if (p.width >= 300 && p.height >= 300) roomy.add(p);
            if (roomy.isEmpty()) continue;
            int want = 2 + rng.nextInt(2);
            for (int tries = 0; tries < 60 && want > 0; tries++) {
                Rectangle2D.Double b = roomy.get(rng.nextInt(roomy.size()));
                double x = b.x + 120 + rng.nextDouble() * (b.width - 240), y = b.y + 120 + rng.nextDouble() * (b.height - 240);
                boolean ok = Util.dist(x, y, spawnX, spawnY) > 140 && Util.dist(x, y, guideX, guideY) > 140
                    && Util.dist(x, y, b.getCenterX(), b.getCenterY()) > 160;                   // the middle of a room stays open for the fight
                for (Door d : doors) {
                    Rectangle2D.Double w = d.walk;
                    if (new Rectangle2D.Double(w.x - 140, w.y - 140, w.width + 280, w.height + 280).contains(x, y)) ok = false;
                }
                for (Station st : stations) if (Util.dist(x, y, st.x(), st.y()) < 150) ok = false;
                for (Breakable o : breakables) if (Util.dist(x, y, o.x, o.y) < 90) ok = false;
                if (!ok) continue;
                breakables.add(new Breakable(rng.nextBoolean() ? Breakable.Kind.CRATE : Breakable.Kind.BARREL, x, y, xpScale));
                want--;
            }
        }
    }

    // ------------------------------------------------------------------ state

    /** Recomputes what's walkable and which doors are sealed. Call whenever a room's state changes. */
    void refresh() {
        version++;
        boolean unlocked = bossUnlocked();
        for (Door d : doors) d.sealed = (d.a.gated || d.b.gated) && !unlocked;
        walkable.clear();
        for (Room r : rooms) walkable.addAll(r.parts);
        for (Door d : doors) if (d.open()) walkable.add(d.walk);
    }

    /** True once every non-gated room with enemies has been cleared. */
    boolean bossUnlocked() {
        for (Room r : rooms) if (!r.gated && r.state != Room.State.SAFE && r.state != Room.State.CLEARED) return false;
        return true;
    }

    /** The point nearest to (x, y) where a body of radius r fits entirely inside the walkable area. */
    Util.Vec clamp(double x, double y, double r) {
        double bestX = x, bestY = y, bestD = Double.MAX_VALUE;
        for (Rectangle2D.Double rect : walkable) {
            double cx = Util.clamp(x, rect.x + r, rect.getMaxX() - r);
            double cy = Util.clamp(y, rect.y + r, rect.getMaxY() - r);
            double d = (cx - x) * (cx - x) + (cy - y) * (cy - y);
            if (d < bestD) {
                bestD = d;
                bestX = cx;
                bestY = cy;
                if (d == 0) break;
            }
        }
        return new Util.Vec(bestX, bestY);
    }

    /** True if the point is on walkable ground (used for projectiles hitting walls). */
    boolean contains(double x, double y) {
        for (Rectangle2D.Double rect : walkable) if (rect.contains(x, y)) return true;
        return false;
    }

    /**
     * The room whose interior (shrunk by {@code inset} from every wall) contains the point, or null. Each of a
     * multi-part room's pieces is shrunk on its own, so a point right at the seam between two pieces (rather than
     * against an actual wall) can occasionally read as just outside — harmless: it only delays a room's trigger by
     * a pixel or two, never lets you skip it.
     */
    Room roomAt(double x, double y, double inset) {
        for (Room r : rooms) {
            for (Rectangle2D.Double b : r.parts) {
                if (x >= b.x + inset && x <= b.getMaxX() - inset && y >= b.y + inset && y <= b.getMaxY() - inset) return r;
            }
        }
        return null;
    }

    /** All walkable ground as a single shape, for drawing. */
    Area floor() {
        Area area = new Area();
        for (Rectangle2D.Double rect : walkable) area.add(new Area(rect));
        return area;
    }

    /** The trainer the point is within {@code range} of, or null. */
    Station stationNear(double x, double y, double range) {
        Station best = null;
        double bestD = range;
        for (Station s : stations) {
            double d = Util.dist(x, y, s.x(), s.y());
            if (d <= bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    /** Rooms with enemies, including the boss room. */
    int combatRoomCount() {
        int n = 0;
        for (Room r : rooms) if (r.state != Room.State.SAFE) n++;
        return n;
    }

    int clearedRoomCount() {
        int n = 0;
        for (Room r : rooms) if (r.state == Room.State.CLEARED) n++;
        return n;
    }

    /** Rooms with enemies that must be cleared to unlock the boss (everything except the gated room). */
    int regularRoomCount() {
        int n = 0;
        for (Room r : rooms) if (r.state != Room.State.SAFE && !r.gated) n++;
        return n;
    }

    int clearedRegularCount() {
        int n = 0;
        for (Room r : rooms) if (r.state == Room.State.CLEARED && !r.gated) n++;
        return n;
    }
}
