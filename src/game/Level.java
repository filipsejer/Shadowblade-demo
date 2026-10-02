package game;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * One continuous map: rooms (each one or more glued-together rectangles) joined by corridors. The walkable area is
 * the union of their rectangles. There are two kinds: an explorable world ({@link Worlds#forest}), with named areas,
 * people to talk to, chests and the gates to challenges; and a battlefield ({@link Battlefield#make}), one big
 * irregular room generated fresh for each fight, with the Blight's nests in it.
 */
final class Level {
    /** A corridor's walkable rectangle reaches this far into each room so bodies can slide through the doorway. */
    private static final double DOOR_OVERLAP = 60;
    static final double CORRIDOR_LENGTH = 180;
    static final double DOOR_WIDTH = 220;

    static final class Room {
        /** SAFE rooms are paved (a camp, a plaza); OPEN ones get the theme's ground. */
        enum State { SAFE, OPEN }

        final String name;
        /** The room's shape: one rectangle, or several glued together, each an axis-aligned box. */
        final List<Rectangle2D.Double> parts = new ArrayList<>();
        /** The bounding box of every part, kept in sync as parts are added. */
        Rectangle2D.Double bounds;
        State state;
        /** Set once the player has been inside; the minimap only shows visited rooms. */
        boolean visited;

        Room(String name, double x, double y, double w, double h, State state) {
            this.name = name;
            this.state = state;
            this.parts.add(new Rectangle2D.Double(x, y, w, h));
            recomputeBounds();
        }

        void add(Rectangle2D.Double part) {
            parts.add(part);
            recomputeBounds();
        }

        void recomputeBounds() {
            Rectangle2D.Double b = new Rectangle2D.Double(parts.get(0).x, parts.get(0).y, parts.get(0).width, parts.get(0).height);
            for (Rectangle2D.Double p : parts) Rectangle2D.union(b, p, b);
            bounds = b;
        }
    }

    /** A corridor between two rooms. Always open: nothing in an explorable world locks you in. */
    static final class Door {
        final Room a, b;
        final Rectangle2D.Double gap;    // the corridor proper, between the two rooms
        final Rectangle2D.Double walk;   // the gap plus some overlap into both rooms
        final boolean vertical;
        final List<Rectangle2D.Double> barriers = new ArrayList<>();
        final boolean sealed = false;

        /** Rooms {@code a} (above) and {@code b} (below), joined by a vertical corridor {@code offset} east of centre. */
        static Door vertical(Room a, Room b, double width, double offset) {
            double cx = a.bounds.getCenterX() + offset;
            Rectangle2D.Double gap = new Rectangle2D.Double(cx - width / 2, a.bounds.getMaxY(), width, b.bounds.y - a.bounds.getMaxY());
            return new Door(a, b, gap, true, new Rectangle2D.Double(gap.x, gap.y - DOOR_OVERLAP, width, gap.height + 2 * DOOR_OVERLAP));
        }

        /** Rooms {@code a} (left) and {@code b} (right), joined by a horizontal corridor {@code offset} south of centre. */
        static Door horizontal(Room a, Room b, double width, double offset) {
            double cy = a.bounds.getCenterY() + offset;
            Rectangle2D.Double gap = new Rectangle2D.Double(a.bounds.getMaxX(), cy - width / 2, b.bounds.x - a.bounds.getMaxX(), width);
            return new Door(a, b, gap, false, new Rectangle2D.Double(gap.x - DOOR_OVERLAP, gap.y, gap.width + 2 * DOOR_OVERLAP, width));
        }

        private Door(Room a, Room b, Rectangle2D.Double gap, boolean vertical, Rectangle2D.Double walk) {
            this.a = a;
            this.b = b;
            this.gap = gap;
            this.vertical = vertical;
            this.walk = walk;
        }

        boolean open() { return true; }
    }

    /**
     * A single big piece of scenery. {@code kind} is "oak", "bush", "boulder", "stump" or "log"; (x, y) is where it
     * stands on the ground; a positive {@code radius} makes it solid.
     */
    record Landmark(String kind, double x, double y, double radius) {}

    /** What talking to someone does: just talk, or open the trainer's or the merchant's screen after a word. */
    enum Role { TALK, TRAINER, MERCHANT }

    /** Someone in an explorable world. What they say is up to {@link Story}, by their {@code id} and the story so far. */
    record Npc(String id, String name, String portrait, Role role, double x, double y) {}

    /** A chest to find. What's in it is {@link Story#treasure}'s, by {@code id}; {@code big} ones look grander. */
    record Treasure(String id, double x, double y, boolean big) {}

    /** The way into a challenge: a swirl of light you walk up to. */
    record Gate(Challenge challenge, double x, double y) {}

    /**
     * A road out of this world to another ({@code to}, a {@link Worlds} id): a signpost you walk up to, which brings up
     * the map of the worlds. Arriving along it, you stand at ({@code ax}, {@code ay}).
     */
    record Road(String to, String name, double x, double y, double ax, double ay) {}

    final List<Room> rooms;
    final List<Door> doors;
    final List<Landmark> landmarks = new ArrayList<>();
    /** Crates and barrels to smash. */
    final List<Breakable> breakables = new ArrayList<>();
    final List<Npc> npcs = new ArrayList<>();
    final List<Treasure> treasures = new ArrayList<>();
    final List<Gate> gates = new ArrayList<>();
    final List<Road> roads = new ArrayList<>();
    /** A battlefield's nests, and the spots its gold-bought caches stand on (see {@link Run}). */
    final List<Util.Vec> nestSpots = new ArrayList<>();
    final List<Util.Vec> cacheSpots = new ArrayList<>();
    /** An escort's way across the battlefield, start to finish, every leg of it walkable in a straight line (empty otherwise). */
    final List<Util.Vec> route = new ArrayList<>();
    /** Solid ground you can't walk on (a flower bed), sitting on top of a room's floor. */
    final List<Rectangle2D.Double> grassPatches = new ArrayList<>();
    /** Water you can't walk into (a canal), cut into a room's floor. */
    final List<Rectangle2D.Double> water = new ArrayList<>();
    final double width, height;          // bounding box of the whole map
    final double spawnX, spawnY;         // where the player starts
    private final List<Rectangle2D.Double> walkable = new ArrayList<>();

    String name = "";
    /** Which explorable world this is ({@link Worlds#of}), or null for a battlefield. */
    String world;
    String bossName = "GUARDIAN";
    Theme theme = Theme.FOREST;
    /** True for an explorable world (no fighting), false for a battlefield. */
    boolean explorable;
    /** Bumped whenever the walkable area changes, so anything cached from the map's shape knows to rebuild. */
    int version;
    /** Health / damage multipliers for big enemies (brutes, the boss) and small ones; a fight keeps them current. */
    double enemyHpMult = 1, enemyDamageMult = 1, smallEnemyHpMult = 1, smallEnemyDamageMult = 1;

    double hpMult(Enemy.Type type) { return type.small() ? smallEnemyHpMult : enemyHpMult; }

    double damageMult(Enemy.Type type) { return type.small() ? smallEnemyDamageMult : enemyDamageMult; }

    Level(List<Room> rooms, List<Door> doors, double spawnX, double spawnY) { this(rooms, doors, spawnX, spawnY, 0); }

    /** {@code pad}: extra map beyond the rooms' right and bottom edges (to match a {@link Builder#finish(double) margin}). */
    Level(List<Room> rooms, List<Door> doors, double spawnX, double spawnY, double pad) {
        this.rooms = rooms;
        this.doors = doors;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        double w = 0, h = 0;
        for (Room r : rooms) {
            w = Math.max(w, r.bounds.getMaxX());
            h = Math.max(h, r.bounds.getMaxY());
        }
        this.width = w + pad;
        this.height = h + pad;
        refresh();
    }

    // ------------------------------------------------------------------ laying out rooms

    enum Dir { NORTH, EAST, SOUTH, WEST }

    /**
     * Lays rooms out by attaching each one to a side of another, centred on the shared axis (so the corridor between
     * them runs straight through both room centres). Coordinates are normalised to be non-negative at the end.
     */
    static final class Builder {
        private record Link(Room a, Room b, boolean vertical, double width, double offset) {}   // a is above / left of b

        final List<Room> rooms = new ArrayList<>();
        private final List<Link> links = new ArrayList<>();

        Room start(String name, double w, double h, Room.State state) {
            Room r = new Room(name, 0, 0, w, h, state);
            rooms.add(r);
            return r;
        }

        Room attach(Room from, Dir dir, String name, double w, double h, Room.State state) {
            return attach(from, dir, name, w, h, state, DOOR_WIDTH, CORRIDOR_LENGTH, 0);
        }

        /** As the plain one, with the doorway shifted {@code doorOffset} off the middle of the shared wall (east, or south). */
        Room attach(Room from, Dir dir, String name, double w, double h, Room.State state, double corridorWidth, double corridorLength, double doorOffset) {
            Rectangle2D.Double f = from.bounds;
            double x, y;
            switch (dir) {
                case NORTH -> { x = f.getCenterX() - w / 2; y = f.y - corridorLength - h; }
                case SOUTH -> { x = f.getCenterX() - w / 2; y = f.getMaxY() + corridorLength; }
                case EAST -> { x = f.getMaxX() + corridorLength; y = f.getCenterY() - h / 2; }
                default -> { x = f.x - corridorLength - w; y = f.getCenterY() - h / 2; }
            }
            Room room = new Room(name, x, y, w, h, state);
            for (Room other : rooms) {
                Rectangle2D.Double o = other.bounds;
                if (new Rectangle2D.Double(o.x - 60, o.y - 60, o.width + 120, o.height + 120).intersects(room.bounds)) {
                    throw new IllegalStateException(name + " overlaps " + other.name);
                }
            }
            rooms.add(room);
            switch (dir) {
                case NORTH -> links.add(new Link(room, from, true, corridorWidth, doorOffset));
                case SOUTH -> links.add(new Link(from, room, true, corridorWidth, doorOffset));
                case EAST -> links.add(new Link(from, room, false, corridorWidth, doorOffset));
                default -> links.add(new Link(room, from, false, corridorWidth, doorOffset));
            }
            return room;
        }

        /** Shifts everything so the top-left of the map is (0, 0), then builds the doors. */
        List<Door> finish() { return finish(0); }

        /**
         * As {@link #finish()}, with {@code margin} of empty map left round the rooms, so the camera can show what
         * stands beyond the outermost walls (the city's house fronts).
         */
        List<Door> finish(double margin) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            for (Room r : rooms) for (Rectangle2D.Double p : r.parts) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
            }
            for (Room r : rooms) {
                for (Rectangle2D.Double p : r.parts) {
                    p.x -= minX - margin;
                    p.y -= minY - margin;
                }
                r.recomputeBounds();
            }
            List<Door> doors = new ArrayList<>();
            for (Link l : links) {
                doors.add(l.vertical() ? Door.vertical(l.a(), l.b(), l.width(), l.offset()) : Door.horizontal(l.a(), l.b(), l.width(), l.offset()));
            }
            return doors;
        }
    }

    // ------------------------------------------------------------------ queries

    /** Recomputes what's walkable. */
    void refresh() {
        version++;
        walkable.clear();
        for (Room r : rooms) walkable.addAll(r.parts);
        for (Door d : doors) walkable.add(d.walk);
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

    /** The room whose interior (shrunk by {@code inset}) contains the point, or null. */
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
}
