package game;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * How the horde finds you on a battlefield: a grid laid over the map, and for every cell of it, how far it is to walk
 * from there to the player. It's worked out once for everyone (a flow field), not once per monster, so a horde of a
 * hundred costs the same as one.
 *
 * <p>A monster that can see you walks straight at you. One that can't (a wall, a tree in the way) follows the field
 * downhill: it looks a few cells ahead along the shortest route and heads for the furthest one it can reach in a
 * straight line, so it rounds corners smoothly instead of zig-zagging cell to cell. Cells right next to a wall cost a
 * little more to cross, which keeps routes off the walls and through the middle of passages. Crates and nests block
 * the way too, until they're smashed.
 */
final class PathField {
    /** The size of one cell, in world units. */
    static final double CELL = 32;
    /** How much room a cell's centre needs around it to count as walkable (about a grunt's body). */
    private static final double CLEARANCE = 16;
    /** Moving to a neighbouring cell: straight, diagonal, and the extra for a cell that touches a wall. */
    private static final int STEP = 10, DIAGONAL = 14, NEAR_WALL = 12;
    /** How many cells ahead along the route a monster looks for one it can walk straight to. */
    private static final int LOOK_AHEAD = 8;
    /** The field is worked out again at most this often (seconds), or at once when you step into another cell. */
    private static final double REBUILD = 0.25;
    /** Beyond this a monster doesn't check whether it could walk straight at you: the route is as good from that far. */
    private static final double SIGHT = 1200;
    private static final int UNREACHED = Integer.MAX_VALUE;

    private final Level level;
    private final int version;
    final int cols, rows;
    /** Walkable as far as the map goes (walls, tree trunks, flower beds); then also clear of crates and nests right now. */
    private final boolean[] base, open;
    /** Open cells that touch a blocked one. */
    private final boolean[] edge;
    /** The crates and nests standing on the map when last looked, each (x, y, radius), and a checksum of them. */
    private List<double[]> obstacles = List.of();
    private double obstacleSum = Double.NaN;
    /** Walking cost from each cell to the player's cell. */
    private final int[] dist;
    /** The cells still to settle, each packed as (cost << 32 | cell). */
    private long[] heap = new long[1024];
    private int heapSize;
    private int targetCell = -1;
    private double sinceBuild = Double.MAX_VALUE;

    PathField(Level level) {
        this.level = level;
        this.version = level.version;
        cols = (int) Math.ceil(level.width / CELL) + 1;
        rows = (int) Math.ceil(level.height / CELL) + 1;
        base = new boolean[cols * rows];
        open = new boolean[cols * rows];
        edge = new boolean[cols * rows];
        dist = new int[cols * rows];
        Arrays.fill(dist, UNREACHED);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = (c + 0.5) * CELL, y = (r + 0.5) * CELL;
                Util.Vec v = level.clamp(x, y, CLEARANCE);
                base[r * cols + c] = Math.abs(v.x() - x) < 0.5 && Math.abs(v.y() - y) < 0.5 && !blocked(x, y);
            }
        }
        System.arraycopy(base, 0, open, 0, base.length);
        markEdges(0, 0, cols - 1, rows - 1);
    }

    /** Works out which open cells in the box (inclusive) touch a blocked one. */
    private void markEdges(int c0, int r0, int c1, int r1) {
        for (int r = Math.max(0, r0); r <= Math.min(rows - 1, r1); r++) {
            for (int c = Math.max(0, c0); c <= Math.min(cols - 1, c1); c++) {
                int i = r * cols + c;
                edge[i] = false;
                if (!open[i]) continue;
                for (int dr = -1; dr <= 1 && !edge[i]; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (!isOpen(c + dc, r + dr)) { edge[i] = true; break; }
                    }
                }
            }
        }
    }

    /**
     * Brings the field up to date with the crates and nests standing right now (one smashed, a nest destroyed or
     * newly grown): only the cells around the ones that changed are looked at again. True if anything changed.
     */
    boolean sync(World w) {
        double sum = 0;
        int n = 0;
        for (Breakable b : level.breakables) if (!b.broken) { sum += b.x * 31 + b.y * 17 + b.radius; n++; }
        for (Enemy e : w.enemies) if (e.rooted()) { sum += e.x * 37 + e.y * 13 + e.radius; n++; }
        sum += n * 1e7;
        if (sum == obstacleSum) return false;
        obstacleSum = sum;
        List<double[]> now = new ArrayList<>();
        for (Breakable b : level.breakables) if (!b.broken) now.add(new double[]{b.x, b.y - 8, b.radius});
        for (Enemy e : w.enemies) if (e.rooted()) now.add(new double[]{e.x, e.y, e.radius});
        List<double[]> changed = new ArrayList<>();
        for (double[] o : now) if (!holds(obstacles, o)) changed.add(o);
        for (double[] o : obstacles) if (!holds(now, o)) changed.add(o);
        obstacles = now;
        for (double[] o : changed) {
            double reach = o[2] + CLEARANCE + CELL;
            int c0 = col(o[0] - reach), c1 = col(o[0] + reach), r0 = row(o[1] - reach), r1 = row(o[1] + reach);
            for (int r = r0; r <= r1; r++) {
                for (int c = c0; c <= c1; c++) {
                    int i = r * cols + c;
                    open[i] = base[i] && !onObstacle((c + 0.5) * CELL, (r + 0.5) * CELL);
                }
            }
            markEdges(c0 - 1, r0 - 1, c1 + 1, r1 + 1);
        }
        return true;
    }

    private static boolean holds(List<double[]> list, double[] o) {
        for (double[] p : list) if (p[0] == o[0] && p[1] == o[1] && p[2] == o[2]) return true;
        return false;
    }

    private boolean onObstacle(double x, double y) {
        for (double[] o : obstacles) if (Util.dist(x, y, o[0], o[1]) < o[2] + CLEARANCE) return true;
        return false;
    }

    /** True if a tree trunk or a flower bed stands on this spot (crates and nests come and go: see {@link #sync}). */
    private boolean blocked(double x, double y) {
        for (Level.Landmark l : level.landmarks) {
            if (l.radius() > 0 && Util.dist(x, y, l.x(), l.y() - 12) < l.radius() + CLEARANCE) return true;
        }
        for (Rectangle2D.Double g : level.grassPatches) {
            if (x > g.x - CLEARANCE && x < g.getMaxX() + CLEARANCE && y > g.y - CLEARANCE && y < g.getMaxY() + CLEARANCE) return true;
        }
        return false;
    }

    /** True if this field was made for the map as it is now (a new map, or one whose shape changed, needs a new field). */
    boolean fits(Level l) { return l == level && l.version == version; }

    private boolean isOpen(int c, int r) { return c >= 0 && r >= 0 && c < cols && r < rows && open[r * cols + c]; }

    private int col(double x) { return Math.max(0, Math.min(cols - 1, (int) (x / CELL))); }

    private int row(double y) { return Math.max(0, Math.min(rows - 1, (int) (y / CELL))); }

    // ------------------------------------------------------------------ the field

    /**
     * Keeps the field pointing at the player: worked out again when they step into another cell, when a crate or nest
     * comes or goes, or every so often.
     */
    void update(World w, double dt) {
        sinceBuild += dt;
        boolean changed = sync(w);
        double x = w.player.x, y = w.player.y;
        if (!changed && row(y) * cols + col(x) == targetCell && sinceBuild < REBUILD) return;
        build(x, y);
    }

    /** Dijkstra from the player outward over the whole map. */
    void build(double x, double y) {
        targetCell = row(y) * cols + col(x);
        sinceBuild = 0;
        Arrays.fill(dist, UNREACHED);
        heapSize = 0;
        int pc = col(x), pr = row(y);
        boolean seeded = false;
        for (int reach = 0; reach <= 3 && !seeded; reach++) {         // standing against a wall your own cell may not count: start from the nearest that do
            for (int r = pr - reach; r <= pr + reach; r++) {
                for (int c = pc - reach; c <= pc + reach; c++) {
                    if (!isOpen(c, r)) continue;
                    int i = r * cols + c;
                    int d = (int) Math.round(Math.hypot((c + 0.5) * CELL - x, (r + 0.5) * CELL - y) / CELL * STEP);
                    if (d < dist[i]) { dist[i] = d; push(d, i); }
                    seeded = true;
                }
            }
        }
        while (heapSize > 0) {
            long top = pop();
            int i = (int) top, d = (int) (top >>> 32);
            if (d > dist[i]) continue;                                // a stale entry: the cell was settled cheaper since
            int c = i % cols, r = i / cols;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nc = c + dc, nr = r + dr;
                    if (!isOpen(nc, nr)) continue;
                    if (dr != 0 && dc != 0 && (!isOpen(c + dc, r) || !isOpen(c, r + dr))) continue;   // no cutting corners
                    int n = nr * cols + nc;
                    int nd = d + (dr != 0 && dc != 0 ? DIAGONAL : STEP) + (edge[n] ? NEAR_WALL : 0);
                    if (nd < dist[n]) {
                        dist[n] = nd;
                        push(nd, n);
                    }
                }
            }
        }
    }

    /** Walking cost from (x, y) to the player, in tenths of a cell, or -1 if there's no way there. */
    int cost(double x, double y) {
        int d = dist[row(y) * cols + col(x)];
        return d == UNREACHED ? -1 : d;
    }

    // a binary min-heap of packed (cost, cell) entries; a cell can be in it more than once, and the stale ones are skipped

    private void push(int cost, int cell) {
        if (heapSize == heap.length) heap = Arrays.copyOf(heap, heap.length * 2);
        int k = heapSize++;
        heap[k] = (long) cost << 32 | cell;
        while (k > 0) {
            int parent = (k - 1) / 2;
            if (heap[parent] <= heap[k]) break;
            long t = heap[parent]; heap[parent] = heap[k]; heap[k] = t;
            k = parent;
        }
    }

    private long pop() {
        long top = heap[0];
        heap[0] = heap[--heapSize];
        int k = 0;
        while (true) {
            int l = 2 * k + 1, r = l + 1, m = k;
            if (l < heapSize && heap[l] < heap[m]) m = l;
            if (r < heapSize && heap[r] < heap[m]) m = r;
            if (m == k) break;
            long t = heap[m]; heap[m] = heap[k]; heap[k] = t;
            k = m;
        }
        return top;
    }

    // ------------------------------------------------------------------ steering

    /**
     * Which way a body of radius {@code radius} at (x, y) should walk to reach the player at (px, py): straight at them
     * if nothing's in the way, otherwise along the shortest route. Null if it's already as close as the field can take
     * it (or cut off).
     */
    Util.Vec direction(double x, double y, double radius, double px, double py) {
        if (Util.dist(x, y, px, py) < SIGHT && clearPath(x, y, px, py, radius)) return unit(px - x, py - y);
        int c = col(x), r = row(y);
        int here = bestNear(c, r);
        if (here < 0) return unit(px - x, py - y);                   // somewhere the field doesn't reach: just try
        // follow the route downhill a few cells, and aim for the furthest of them still in a straight, clear line
        int aim = -1, cell = here;
        for (int k = 0; k < LOOK_AHEAD; k++) {
            int next = downhill(cell);
            if (next < 0) break;
            cell = next;
            if (clearPath(x, y, centreX(cell), centreY(cell), radius)) aim = cell;
        }
        if (aim < 0) aim = downhill(here) >= 0 ? downhill(here) : here;   // nothing in a clear line: take the next step anyway
        Util.Vec v = unit(centreX(aim) - x, centreY(aim) - y);
        return v != null ? v : unit(px - x, py - y);
    }

    /** The cell itself if it's on the field, else the best neighbour within two cells (a body pushed up against a wall). */
    private int bestNear(int c, int r) {
        int i = r * cols + c;
        if (open[i] && dist[i] != UNREACHED) return i;
        int best = -1;
        for (int reach = 1; reach <= 2 && best < 0; reach++) {
            for (int rr = r - reach; rr <= r + reach; rr++) {
                for (int cc = c - reach; cc <= c + reach; cc++) {
                    if (!isOpen(cc, rr)) continue;
                    int n = rr * cols + cc;
                    if (dist[n] != UNREACHED && (best < 0 || dist[n] < dist[best])) best = n;
                }
            }
        }
        return best;
    }

    /** The neighbour one step closer to the player, or -1 at the bottom. */
    private int downhill(int i) {
        int c = i % cols, r = i / cols, best = -1, bestD = dist[i];
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                if (!isOpen(c + dc, r + dr)) continue;
                if (dr != 0 && dc != 0 && (!isOpen(c + dc, r) || !isOpen(c, r + dr))) continue;
                int n = (r + dr) * cols + c + dc;
                if (dist[n] < bestD) { bestD = dist[n]; best = n; }
            }
        }
        return best;
    }

    /**
     * True if a body of radius r can walk straight from one point to the other: its middle and both its sides stay on
     * walkable cells all the way (a body at the edge of a passage can see down it, but may not fit).
     */
    boolean clearPath(double x0, double y0, double x1, double y1, double r) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        if (len < 0.001) return true;
        double sx = -(y1 - y0) / len * r * 0.9, sy = (x1 - x0) / len * r * 0.9;
        return clearLine(x0, y0, x1, y1) && clearLine(x0 + sx, y0 + sy, x1 + sx, y1 + sy) && clearLine(x0 - sx, y0 - sy, x1 - sx, y1 - sy);
    }

    /**
     * True if the straight line between two points only crosses walkable cells. The first and last half cell are let
     * off: a body squeezed against a wall stands in a cell that doesn't count, and so may the player.
     */
    boolean clearLine(double x0, double y0, double x1, double y1) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) Math.ceil(len / (CELL * 0.5));
        for (int k = 1; k < steps; k++) {
            double t = (double) k / steps, along = t * len;
            if (along < CELL * 0.5 || len - along < CELL * 0.5) continue;
            if (!isOpen(col(x0 + (x1 - x0) * t), row(y0 + (y1 - y0) * t))) return false;
        }
        return true;
    }

    private double centreX(int i) { return (i % cols + 0.5) * CELL; }

    private double centreY(int i) { return (i / cols + 0.5) * CELL; }

    private static Util.Vec unit(double dx, double dy) {
        double d = Math.hypot(dx, dy);
        return d < 0.001 ? null : new Util.Vec(dx / d, dy / d);
    }
}
