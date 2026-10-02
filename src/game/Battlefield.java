package game;

import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Makes a challenge's battlefield, fresh for every fight (in the spirit of Risk of Rain's stages): a big, irregular
 * stretch of ground grown cell by cell on a grid, so it has wide open stretches, narrow necks and dead ends, with
 * trees and rocks to weave between. The challenge's nests go in the cells furthest from where you start and from each
 * other, so destroying them all means crossing the whole map; a few gold-bought caches are scattered around too.
 * For an escort, the route instead runs from the start to the cell furthest from it, through the middle of every
 * cell and passage on the way, and nothing is put down on it.
 */
final class Battlefield {
    private Battlefield() {}

    /** Grid side, in cells, and the size of a cell. */
    private static final int GRID = 7;
    private static final double CELL = 660;

    static Level make(Challenge c, long seed) {
        Random rng = new Random(seed);
        boolean[][] in = new boolean[GRID][GRID];
        List<int[]> cells = new ArrayList<>();
        int sx = rng.nextBoolean() ? 0 : GRID - 1, sy = rng.nextInt(GRID);           // start on the west or east edge
        if (rng.nextBoolean()) { int t = sx; sx = sy; sy = t; }                        // ...or the north or south one
        in[sx][sy] = true;
        cells.add(new int[]{sx, sy});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int want = Math.min(GRID * GRID - 6, c.cells);
        for (int tries = 0; cells.size() < want && tries < 20000; tries++) {
            int[] from = cells.get(rng.nextInt(cells.size()));
            int[] d = dirs[rng.nextInt(4)];
            int nx = from[0] + d[0], ny = from[1] + d[1];
            if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID || in[nx][ny]) continue;
            if (neighbours(in, nx, ny) > 1 && rng.nextDouble() < 0.6) continue;       // mostly grow outward: branches, not one blob
            in[nx][ny] = true;
            cells.add(new int[]{nx, ny});
        }

        if (c.goal == Challenge.Goal.DEFEND) {                                         // the engine stands in the middle of it all:
            int best = Integer.MAX_VALUE;                                               // the cell with the shortest walk to the furthest one
            for (int[] cl : cells) {
                int[][] d = bfs(in, cl[0], cl[1]);
                int far = 0;
                for (int[] o : cells) far = Math.max(far, d[o[0]][o[1]]);
                if (far < best) { best = far; sx = cl[0]; sy = cl[1]; }
            }
        }

        // each cell is a rough rectangle (insets differ per side, so the edges are ragged); neighbours are joined by a
        // passage that's sometimes wide open and sometimes a narrow neck
        Rectangle2D.Double[][] rect = new Rectangle2D.Double[GRID][GRID];
        for (int[] cl : cells) {
            double l = 30 + rng.nextDouble() * 130, r = 30 + rng.nextDouble() * 130, t = 30 + rng.nextDouble() * 130, b = 30 + rng.nextDouble() * 130;
            rect[cl[0]][cl[1]] = new Rectangle2D.Double(cl[0] * CELL + l, cl[1] * CELL + t, CELL - l - r, CELL - t - b);
        }
        Level.Room field = null;
        for (int[] cl : cells) {
            Rectangle2D.Double p = rect[cl[0]][cl[1]];
            if (field == null) field = new Level.Room(c.title, p.x, p.y, p.width, p.height, Level.Room.State.OPEN);
            else field.add(new Rectangle2D.Double(p.x, p.y, p.width, p.height));
        }
        for (int[] cl : cells) {
            for (int k = 0; k < 2; k++) {
                int nx = cl[0] + (k == 0 ? 1 : 0), ny = cl[1] + (k == 1 ? 1 : 0);
                if (nx >= GRID || ny >= GRID || !in[nx][ny]) continue;
                Rectangle2D.Double a = rect[cl[0]][cl[1]], bb = rect[nx][ny];
                double width = rng.nextDouble() < 0.3 ? 200 + rng.nextDouble() * 60 : 300 + rng.nextDouble() * 220;
                if (k == 0) {                                                            // east-west passage
                    double lo = Math.max(a.y, bb.y), hi = Math.min(a.getMaxY(), bb.getMaxY());
                    double mid = lo < hi ? (lo + hi) / 2 : (a.getCenterY() + bb.getCenterY()) / 2;
                    width = Math.min(width, Math.max(200, hi - lo));
                    field.add(new Rectangle2D.Double(a.getCenterX(), mid - width / 2, bb.getCenterX() - a.getCenterX(), width));
                } else {                                                                 // north-south passage
                    double lo = Math.max(a.x, bb.x), hi = Math.min(a.getMaxX(), bb.getMaxX());
                    double mid = lo < hi ? (lo + hi) / 2 : (a.getCenterX() + bb.getCenterX()) / 2;
                    width = Math.min(width, Math.max(200, hi - lo));
                    field.add(new Rectangle2D.Double(mid - width / 2, a.getCenterY(), width, bb.getCenterY() - a.getCenterY()));
                }
            }
        }
        // normalise so the map starts at (0, 0)
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        for (Rectangle2D.Double p : field.parts) { minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); }
        for (Rectangle2D.Double p : field.parts) { p.x -= minX; p.y -= minY; }
        for (int[] cl : cells) { Rectangle2D.Double p = rect[cl[0]][cl[1]]; p.x -= minX; p.y -= minY; }
        field.recomputeBounds();
        field.visited = true;

        Rectangle2D.Double start = rect[sx][sy];
        boolean defend = c.goal == Challenge.Goal.DEFEND;
        Level lv = new Level(List.of(field), List.of(), start.getCenterX(), start.getCenterY() + (defend ? 170 : c.goal == Challenge.Goal.ESCORT ? 110 : 0));
        if (defend) lv.route.add(new Util.Vec(start.getCenterX(), start.getCenterY() - 20));   // where the engine stands
        lv.theme = c.theme;
        lv.name = c.title;
        lv.bossName = c.boss ? c.bossName : "GUARDIAN";

        // the nests: furthest from the start, then furthest from each other (walking distance, in cells)
        List<int[]> chosen = new ArrayList<>();
        int[][] fromStart = bfs(in, sx, sy);
        if (c.goal == Challenge.Goal.ESCORT) {
            route(lv, in, rect, fromStart, sx, sy);
            chosen.addAll(routeCells);
        }
        List<int[][]> dists = new ArrayList<>();
        dists.add(fromStart);
        for (int n = 0; n < (c.warded() ? 0 : c.nests); n++) {
            int[] best = null;
            int bestScore = -1;
            for (int[] cl : cells) {
                if (cl[0] == sx && cl[1] == sy || contains(chosen, cl)) continue;
                int score = Integer.MAX_VALUE;
                for (int[][] d : dists) score = Math.min(score, d[cl[0]][cl[1]]);
                if (fromStart[cl[0]][cl[1]] < 2) score = Math.min(score, 0);           // never right next to where you start
                if (score > bestScore) { bestScore = score; best = cl; }
            }
            if (best == null) break;
            chosen.add(best);
            dists.add(bfs(in, best[0], best[1]));
            Rectangle2D.Double p = rect[best[0]][best[1]];
            lv.nestSpots.add(new Util.Vec(p.getCenterX() + (rng.nextDouble() - 0.5) * 80, p.getCenterY() + (rng.nextDouble() - 0.5) * 80));
        }

        // scenery to weave between (trees and rocks; in the city, lamps, planters, carts and crates; in the lab, its
        // machines), crates, and a few caches
        String[] kinds;
        double[] radii;
        switch (c.theme) {
            case CITY -> {
                kinds = new String[]{"lamp", "planter", "crates", "cart", "hydrant", "bench", "scrap"};
                radii = new double[]{12, 30, 34, 36, 12, 0, 34};
            }
            case LAB -> {
                kinds = new String[]{"coil", "tank", "bench.lab", "crates", "rod", "planter.star", "cables"};
                radii = new double[]{22, 28, 30, 34, 10, 26, 0};
            }
            default -> {
                kinds = new String[]{"oak", "oak", "boulder", "stump", "bush", "bush"};
                radii = new double[]{28, 28, 22, 18, 0, 0};
            }
        }
        List<int[]> open = new ArrayList<>();
        for (int[] cl : cells) {
            Rectangle2D.Double p = rect[cl[0]][cl[1]];
            boolean startCell = cl[0] == sx && cl[1] == sy;
            if (!startCell && !contains(chosen, cl)) open.add(cl);
            int props = startCell ? 1 : 2 + rng.nextInt(3);
            for (int i = 0, tries = 0; i < props && tries < 40; tries++) {
                double x = p.x + 90 + rng.nextDouble() * (p.width - 180), y = p.y + 90 + rng.nextDouble() * (p.height - 180);
                if (p.width < 200 || p.height < 200) break;
                if (Util.dist(x, y, p.getCenterX(), p.getCenterY()) < 150) continue;      // the middle of a cell stays open (nests, caches, you)
                if (nearRoute(lv, x, y, 150)) continue;                                   // and so does the escort's way
                boolean ok = true;
                for (Level.Landmark l : lv.landmarks) if (Util.dist(x, y, l.x(), l.y()) < 150) ok = false;
                if (!ok) continue;
                int k = rng.nextInt(kinds.length);
                lv.landmarks.add(new Level.Landmark(kinds[k], x, y, radii[k]));
                i++;
            }
            for (int i = 0; i < 2 && !startCell; i++) {
                double x = p.x + 80 + rng.nextDouble() * Math.max(1, p.width - 160), y = p.y + 80 + rng.nextDouble() * Math.max(1, p.height - 160);
                boolean ok = Util.dist(x, y, p.getCenterX(), p.getCenterY()) > 110 && !nearRoute(lv, x, y, 110);
                for (Level.Landmark l : lv.landmarks) if (Util.dist(x, y, l.x(), l.y()) < 90) ok = false;
                if (ok) lv.breakables.add(new Breakable(rng.nextBoolean() ? Breakable.Kind.CRATE : Breakable.Kind.BARREL, x, y, 1));
            }
        }
        for (int i = 0; i < 3 && !open.isEmpty(); i++) {
            int[] cl = open.remove(rng.nextInt(open.size()));
            Rectangle2D.Double p = rect[cl[0]][cl[1]];
            lv.cacheSpots.add(new Util.Vec(p.getCenterX(), p.getCenterY()));
        }
        return lv;
    }

    /** The cells an escort's route passes through (set by {@link #route}; the nests' and caches' picks avoid them). */
    private static final List<int[]> routeCells = new ArrayList<>();

    /**
     * Lays the escort's route: the walk (in cells) from the start to the cell furthest from it, then through each cell's
     * middle and along the middle of each passage. Every leg is a straight line on open ground.
     */
    private static void route(Level lv, boolean[][] in, Rectangle2D.Double[][] rect, int[][] fromStart, int sx, int sy) {
        routeCells.clear();
        int ex = sx, ey = sy;
        for (int x = 0; x < GRID; x++) for (int y = 0; y < GRID; y++) {
            if (in[x][y] && fromStart[x][y] < (1 << 20) && fromStart[x][y] > fromStart[ex][ey]) { ex = x; ey = y; }
        }
        List<int[]> walk = new ArrayList<>();                       // back from the end, always to a neighbour one step nearer the start
        int cx = ex, cy = ey;
        walk.add(new int[]{cx, cy});
        while (fromStart[cx][cy] > 0) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID || !in[nx][ny] || fromStart[nx][ny] != fromStart[cx][cy] - 1) continue;
                cx = nx;
                cy = ny;
                break;
            }
            walk.add(0, new int[]{cx, cy});
        }
        routeCells.addAll(walk);
        for (int i = 0; i < walk.size(); i++) {
            Rectangle2D.Double a = rect[walk.get(i)[0]][walk.get(i)[1]];
            lv.route.add(new Util.Vec(a.getCenterX(), a.getCenterY()));
            if (i + 1 == walk.size()) break;
            Rectangle2D.Double b = rect[walk.get(i + 1)[0]][walk.get(i + 1)[1]];
            if (walk.get(i + 1)[0] != walk.get(i)[0]) {              // east or west: along the passage's middle line
                double lo = Math.max(a.y, b.y), hi = Math.min(a.getMaxY(), b.getMaxY());
                double mid = lo < hi ? (lo + hi) / 2 : (a.getCenterY() + b.getCenterY()) / 2;
                lv.route.add(new Util.Vec(a.getCenterX(), mid));
                lv.route.add(new Util.Vec(b.getCenterX(), mid));
            } else {                                                  // north or south
                double lo = Math.max(a.x, b.x), hi = Math.min(a.getMaxX(), b.getMaxX());
                double mid = lo < hi ? (lo + hi) / 2 : (a.getCenterX() + b.getCenterX()) / 2;
                lv.route.add(new Util.Vec(mid, a.getCenterY()));
                lv.route.add(new Util.Vec(mid, b.getCenterY()));
            }
        }
    }

    /** True if (x, y) is within {@code r} of the escort's route. */
    private static boolean nearRoute(Level lv, double x, double y, double r) {
        for (int i = 1; i < lv.route.size(); i++) {
            Util.Vec a = lv.route.get(i - 1), b = lv.route.get(i);
            if (java.awt.geom.Line2D.ptSegDist(a.x(), a.y(), b.x(), b.y(), x, y) < r) return true;
        }
        return false;
    }

    private static int neighbours(boolean[][] in, int x, int y) {
        int n = 0;
        if (x > 0 && in[x - 1][y]) n++;
        if (y > 0 && in[x][y - 1]) n++;
        if (x < GRID - 1 && in[x + 1][y]) n++;
        if (y < GRID - 1 && in[x][y + 1]) n++;
        return n;
    }

    private static boolean contains(List<int[]> list, int[] cl) {
        for (int[] o : list) if (o[0] == cl[0] && o[1] == cl[1]) return true;
        return false;
    }

    /** Steps from (x, y) to every cell of the field (a large number for cells outside it). */
    private static int[][] bfs(boolean[][] in, int x, int y) {
        int[][] d = new int[GRID][GRID];
        for (int[] row : d) java.util.Arrays.fill(row, 1 << 20);
        ArrayDeque<int[]> q = new ArrayDeque<>();
        d[x][y] = 0;
        q.add(new int[]{x, y});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] dd : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = c[0] + dd[0], ny = c[1] + dd[1];
                if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID || !in[nx][ny] || d[nx][ny] <= d[c[0]][c[1]] + 1) continue;
                d[nx][ny] = d[c[0]][c[1]] + 1;
                q.add(new int[]{nx, ny});
            }
        }
        return d;
    }
}
