package game;

import static game.PixelCanvas.*;

import java.util.Map;

/**
 * Every enemy and boss, painted in code. Forest creatures (level 1), city creatures (level 2), laboratory creatures and the Mad Scientist (level 3), and the Shade, who
 * appears in both. Creatures face right; the renderer mirrors them when they face left.
 */
final class CreatureArt {
    private CreatureArt() {}

    static void register(Map<String, Sprite[]> m) {
        // ---- forest
        m.put("forest.grunt.walk", PeopleArt.frames(2, i -> toadstool(i, false), 8, 16));
        m.put("forest.grunt.windup", PeopleArt.frames(1, i -> toadstool(0, true), 8, 16));
        m.put("forest.runner.walk", PeopleArt.frames(2, i -> quad(i, false, false), 8, 12));
        m.put("forest.runner.windup", PeopleArt.frames(1, i -> quad(0, true, false), 8, 12));
        m.put("forest.shooter.walk", PeopleArt.frames(2, i -> snapBloom(i, false), 7, 18));
        m.put("forest.shooter.windup", PeopleArt.frames(1, i -> snapBloom(0, true), 7, 18));
        m.put("forest.brute.walk", PeopleArt.frames(2, i -> stump(i, false), 13, 24));
        m.put("forest.brute.windup", PeopleArt.frames(1, i -> stump(0, true), 13, 24));
        for (boolean p2 : new boolean[]{false, true}) {
            String k = p2 ? "forest.boss2." : "forest.boss.";
            m.put(k + "idle", PeopleArt.frames(2, i -> treant(i, p2), 23, 40));
            m.put(k + "walk", PeopleArt.frames(2, i -> treant(2 + i, p2), 23, 40));
            m.put(k + "slam", PeopleArt.frames(1, i -> treant(4, p2), 23, 40));
            m.put(k + "burst", PeopleArt.frames(1, i -> treant(5, p2), 23, 40));
        }
        // ---- city
        m.put("city.grunt.walk", PeopleArt.frames(2, i -> rat(i, false), 9, 13));
        m.put("city.grunt.windup", PeopleArt.frames(1, i -> rat(0, true), 9, 13));
        m.put("city.runner.walk", PeopleArt.frames(2, i -> quad(i, false, true), 8, 12));
        m.put("city.runner.windup", PeopleArt.frames(1, i -> quad(0, true, true), 8, 12));
        m.put("city.shooter.walk", PeopleArt.frames(2, i -> drone(i, false), 8, 15));
        m.put("city.shooter.windup", PeopleArt.frames(1, i -> drone(0, true), 8, 15));
        m.put("city.brute.walk", PeopleArt.frames(2, i -> dumpster(i, false), 13, 24));
        m.put("city.brute.windup", PeopleArt.frames(1, i -> dumpster(0, true), 13, 24));
        for (boolean p2 : new boolean[]{false, true}) {
            String k = p2 ? "city.boss2." : "city.boss.";
            m.put(k + "idle", PeopleArt.frames(2, i -> warden(i, p2), 23, 40));
            m.put(k + "walk", PeopleArt.frames(2, i -> warden(2 + i, p2), 23, 40));
            m.put(k + "slam", PeopleArt.frames(1, i -> warden(4, p2), 23, 40));
            m.put(k + "burst", PeopleArt.frames(1, i -> warden(5, p2), 23, 40));
        }
        // ---- laboratory
        m.put("lab.grunt.walk", PeopleArt.frames(2, i -> ooze(i, false), 8, 16));
        m.put("lab.grunt.windup", PeopleArt.frames(1, i -> ooze(0, true), 8, 16));
        m.put("lab.runner.walk", PeopleArt.frames(2, i -> clockworkMouse(i, false), 8, 12));
        m.put("lab.runner.windup", PeopleArt.frames(1, i -> clockworkMouse(0, true), 8, 12));
        m.put("lab.shooter.walk", PeopleArt.frames(2, i -> flask(i, false), 8, 19));
        m.put("lab.shooter.windup", PeopleArt.frames(1, i -> flask(0, true), 8, 19));
        m.put("lab.brute.walk", PeopleArt.frames(2, i -> mutant(i, false), 13, 24));
        m.put("lab.brute.windup", PeopleArt.frames(1, i -> mutant(0, true), 13, 24));
        for (boolean p2 : new boolean[]{false, true}) {
            String k = p2 ? "lab.boss2." : "lab.boss.";
            m.put(k + "idle", PeopleArt.frames(2, i -> scientist(i, p2), 23, 40));
            m.put(k + "walk", PeopleArt.frames(2, i -> scientist(2 + i, p2), 23, 40));
            m.put(k + "slam", PeopleArt.frames(1, i -> scientist(4, p2), 23, 40));
            m.put(k + "burst", PeopleArt.frames(1, i -> scientist(5, p2), 23, 40));
        }
        // ---- both
        m.put("shade.walk", PeopleArt.frames(2, i -> shade(i, false), 8, 17));
        m.put("shade.windup", PeopleArt.frames(1, i -> shade(0, true), 8, 17));
        m.put("shade.dizzy", PeopleArt.frames(1, i -> shade(0, false), 8, 17));
    }

    // ------------------------------------------------------------------ forest

    private static final int EYE = rgb(30, 22, 40), WHITE = rgb(255, 255, 255);

    /** The grunt: an angry little toadstool on stubby legs. */
    private static PixelCanvas toadstool(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int cream = rgb(246, 230, 196), creamD = rgb(208, 184, 148);
        int red = rgb(220, 52, 58), redL = rgb(252, 116, 108), redD = rgb(150, 30, 52);
        int hop = wind || step == 1 ? -1 : 0;
        int fx = step == 0 ? 0 : 1;
        c.rect(5 + fx, 14, 3, 2, creamD); c.rect(9 - fx, 14, 3, 2, creamD);
        c.ellipse(8.5, 11.5 + hop, 4.3, 3.9, cream);
        c.ellipseY(8.5, 8.0 + hop, 8.0, 6.2, red, -100, 9 + hop);
        c.rect(1, 8 + hop, 15, 1, redD);
        c.disc(5.5, 5.5 + hop, 1.4, WHITE); c.disc(11.5, 4.5 + hop, 1.6, WHITE); c.disc(8.5, 7.0 + hop, 1.0, WHITE);
        c.set(4, 3 + hop, redL); c.set(5, 2 + hop, redL); c.set(3, 5 + hop, redL);
        c.rect(6, 10 + hop, 2, 2, WHITE); c.rect(10, 10 + hop, 2, 2, WHITE);
        c.set(7, 11 + hop, EYE); c.set(10, 11 + hop, EYE);
        c.line(5, 9 + hop, 8, 10 + hop, EYE); c.line(12, 9 + hop, 9, 10 + hop, EYE);      // angry brows
        if (wind) {
            c.rect(7, 13 + hop, 4, 2, rgb(70, 16, 30)); c.set(7, 13 + hop, WHITE); c.set(10, 13 + hop, WHITE);
            c.set(14, 2 + hop, redL); c.set(15, 3 + hop, redL); c.set(14, 4 + hop, redL);    // anger mark
        }
        c.bevel(0.12, 0.2);
        c.outline(rgb(62, 16, 30));
        return c;
    }

    /** The runner: a fox kit (level 1) or an alley cat (level 2). Both run on the spot, faster than anything else. */
    private static PixelCanvas quad(int step, boolean wind, boolean cat) {
        PixelCanvas c = new PixelCanvas(19, 13);
        int body = cat ? rgb(236, 152, 62) : rgb(238, 118, 40);
        int belly = cat ? rgb(252, 226, 180) : rgb(255, 238, 214);
        int dark = cat ? rgb(170, 92, 34) : rgb(150, 70, 30);
        int crouch = wind ? 1 : 0;
        // far legs (darker), then body
        int reach = step == 0 ? 1 : -1;
        c.rect(6 - reach, 9, 2, 3, darken(body, 0.3)); c.rect(13 + reach, 9, 2, 3, darken(body, 0.3));
        // tail
        if (cat) {
            c.thickLine(4, 7 + crouch, 1, 4, 2, body); c.thickLine(1, 4, 2, 0, 2, body); c.set(2, 0, dark); c.set(1, 1, dark);
        } else {
            c.ellipse(3.0, 6.0 + crouch, 3.6, 2.2, body); c.ellipse(1.2, 5.0 + crouch, 1.6, 1.3, WHITE);
        }
        c.ellipse(9, 7.5 + crouch, 5.6, 3.1, body);
        c.ellipseY(9, 8.6 + crouch, 4.4, 1.9, belly, 8 + crouch, 12);
        if (cat) { c.set(6, 5 + crouch, dark); c.set(6, 6 + crouch, dark); c.set(9, 4 + crouch, dark); c.set(9, 5 + crouch, dark); c.set(12, 5 + crouch, dark); }
        // head
        c.ellipse(14.5, 6.0 + crouch, 3.4, 3.0, body);
        c.ellipse(17.0, 7.0 + crouch, 1.9, 1.4, belly);
        c.tri(12, 4 + crouch, 12, 0 + crouch, 14, 3 + crouch, body);
        c.tri(14, 3 + crouch, 16, 0 + crouch, 17, 4 + crouch, body);
        c.set(12, 2 + crouch, rgb(255, 170, 160)); c.set(16, 2 + crouch, rgb(255, 170, 160));
        c.set(18, 6 + crouch, EYE);                                                        // nose
        c.set(15, 5 + crouch, EYE); c.set(15, 4 + crouch, WHITE);
        c.line(14, 3 + crouch, 16, 4 + crouch, EYE);                                        // brow
        if (wind) { c.set(17, 8 + crouch, WHITE); c.set(15, 8 + crouch, WHITE); }
        // near legs
        c.rect(5 + reach, 9, 2, 3, body); c.rect(12 - reach, 9, 2, 3, body);
        c.rect(5 + reach, 11, 2, 1, dark); c.rect(12 - reach, 11, 2, 1, dark);
        c.bevel(0.12, 0.2);
        c.outline(rgb(70, 30, 20));
        return c;
    }

    /** The shooter: a snapdragon-like flower on a stem that spits seeds. */
    private static PixelCanvas snapBloom(int sway, boolean wind) {
        PixelCanvas c = new PixelCanvas(17, 20);
        int green = rgb(76, 168, 78), greenL = rgb(140, 220, 110), greenD = rgb(40, 108, 56);
        int pink = rgb(240, 84, 130), pinkL = rgb(255, 150, 176), pinkD = rgb(176, 44, 92);
        int hx = sway == 0 ? 0 : 1;
        c.ellipse(8.5, 18, 5.2, 1.8, rgb(112, 76, 50));
        c.thickLine(8, 17, 8 + hx, 10, 2, green);
        c.tri(8, 15, 2, 11, 5, 16, greenL); c.tri(9, 14, 14, 10, 11, 15, greenL);
        c.line(8, 15, 4, 13, greenD); c.line(9, 14, 12, 12, greenD);
        // petals
        int cx = 8 + hx, cy = 7;
        double pr = wind ? 6.3 : 5.0;
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + 0.3;
            c.disc(cx + 0.5 + Math.cos(a) * pr * 0.72, cy + 0.5 + Math.sin(a) * pr * 0.72, 2.6, i % 2 == 0 ? pink : pinkL);
        }
        c.disc(cx + 0.5, cy + 0.5, 4.2, pink);
        // the mouth, facing right
        c.ellipse(cx + 2.5, cy + 0.8, wind ? 3.6 : 2.7, wind ? 3.2 : 2.3, rgb(70, 14, 44));
        if (wind) c.disc(cx + 3.2, cy + 0.8, 1.8, rgb(255, 236, 110));                     // the seed, glowing
        c.rect(cx - 2, cy - 2, 2, 2, WHITE); c.rect(cx + 1, cy - 3, 2, 2, WHITE);
        c.set(cx - 1, cy - 1, EYE); c.set(cx + 2, cy - 2, EYE);
        c.line(cx - 3, cy - 3, cx - 1, cy - 2, EYE); c.line(cx + 3, cy - 4, cx + 1, cy - 3, EYE);
        c.set(cx - 3, cy - 4, pinkL); c.set(cx - 2, cy - 5, pinkL);
        c.bevel(0.12, 0.2);
        c.outline(rgb(60, 20, 44));
        return c;
    }

    /** The brute: a mossy tree-stump golem with glowing eyes and root arms. */
    private static PixelCanvas stump(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(27, 25);
        int bark = rgb(122, 82, 52), barkD = rgb(84, 54, 38), barkL = rgb(164, 116, 74);
        int woodTop = rgb(214, 168, 108), ring = rgb(178, 130, 80), moss = rgb(94, 158, 74), mossL = rgb(150, 208, 110);
        int bob = step == 1 ? -1 : 0;
        // feet
        c.ellipse(8.5, 22.0 + (step == 1 ? -1 : 0), 4.2, 2.2, barkD);
        c.ellipse(18.5, 22.0 + (step == 0 ? 0 : 0) - (step == 0 ? 1 : 0), 4.2, 2.2, barkD);
        // arms
        if (wind) {
            c.ellipse(3.5, 10 + bob, 2.9, 5.0, bark); c.ellipse(23.5, 10 + bob, 2.9, 5.0, bark);
            for (int i = 0; i < 3; i++) { c.rect(1 + i * 2, 3 + bob, 1, 3, bark); c.rect(21 + i * 2, 3 + bob, 1, 3, bark); }
        } else {
            c.ellipse(3.5, 16 + bob, 3.1, 5.2, bark); c.ellipse(23.5, 16 + bob, 3.1, 5.2, bark);
            c.ellipse(3.5, 20 + bob, 3.0, 2.2, barkL); c.ellipse(23.5, 20 + bob, 3.0, 2.2, barkL);
        }
        c.ellipse(13.5, 15.5 + bob, 9.9, 7.8, bark);
        for (int x : new int[]{8, 11, 16, 19}) c.rect(x, 12 + bob, 1, 9, barkD);
        c.rect(9, 13 + bob, 1, 4, barkL); c.rect(17, 14 + bob, 1, 3, barkL);
        // the cut top with growth rings
        c.ellipse(13.5, 9.2 + bob, 9.6, 4.5, barkD);
        c.ellipse(13.5, 8.8 + bob, 8.7, 3.9, woodTop);
        c.ellipse(13.5, 8.8 + bob, 6.2, 2.7, ring); c.ellipse(13.5, 8.8 + bob, 5.4, 2.3, woodTop);
        c.ellipse(13.5, 8.8 + bob, 3.2, 1.4, ring); c.ellipse(13.5, 8.8 + bob, 2.4, 1.0, woodTop);
        c.disc(6.5, 9 + bob, 1.6, moss); c.disc(8, 7.5 + bob, 1.3, mossL); c.disc(21, 11 + bob, 1.4, moss);
        c.disc(5.5, 16 + bob, 1.3, moss);
        // angry glowing eyes and a jagged mouth
        c.rect(8, 14 + bob, 4, 3, rgb(30, 16, 18)); c.rect(16, 14 + bob, 4, 3, rgb(30, 16, 18));
        int glow = wind ? rgb(255, 224, 90) : rgb(255, 78, 58);
        c.rect(9, 15 + bob, 2, 2, glow); c.rect(17, 15 + bob, 2, 2, glow);
        c.line(7, 13 + bob, 12, 15 + bob, barkD); c.line(21, 13 + bob, 16, 15 + bob, barkD);
        for (int x = 9; x <= 18; x++) c.set(x, 19 + bob + (x % 2), rgb(40, 22, 20));
        c.bevel(0.12, 0.2);
        c.outline(rgb(50, 30, 24));
        return c;
    }

    /** Poses: 0/1 idle, 2/3 walk, 4 slam wind-up (arms high), 5 bullet burst (mouth open, spores). */
    private static PixelCanvas treant(int pose, boolean p2) {
        PixelCanvas c = new PixelCanvas(46, 42);
        int bark = p2 ? rgb(112, 66, 52) : rgb(120, 82, 52), barkD = p2 ? rgb(72, 40, 40) : rgb(82, 54, 38), barkL = p2 ? rgb(160, 96, 72) : rgb(166, 118, 76);
        int leaf = p2 ? rgb(224, 114, 44) : rgb(66, 156, 74), leafL = p2 ? rgb(252, 176, 76) : rgb(116, 208, 104), leafD = p2 ? rgb(152, 62, 40) : rgb(38, 104, 56);
        int eye = p2 ? rgb(255, 240, 210) : rgb(255, 196, 66);
        int bob = pose == 1 || pose == 3 ? -1 : 0;
        int foot = pose == 2 ? 1 : 0, foot2 = pose == 3 ? 1 : 0;
        // roots
        c.ellipse(11, 37.5 - foot, 6.5, 2.6, barkD); c.ellipse(35, 37.5 - foot2, 6.5, 2.6, barkD);
        c.ellipse(23, 38.5, 8, 2.4, barkD);
        // arms (branches), behind the trunk
        boolean up = pose == 4;
        int[][] armL = up ? new int[][]{{12, 22}, {7, 14}, {4, 6}} : new int[][]{{12, 22}, {6, 26}, {3, 33}};
        int[][] armR = up ? new int[][]{{34, 22}, {39, 14}, {42, 6}} : new int[][]{{34, 22}, {40, 26}, {43, 33}};
        for (int[][] arm : new int[][][]{armL, armR}) {
            c.thickLine(arm[0][0], arm[0][1] + bob, arm[1][0], arm[1][1] + bob, 5, bark);
            c.thickLine(arm[1][0], arm[1][1] + bob, arm[2][0], arm[2][1] + bob, 4, bark);
            c.disc(arm[2][0] + 0.5, arm[2][1] + bob + 0.5, 3.2, barkL);
            c.disc(arm[2][0] + 0.5, arm[2][1] + bob - 1.5, 2.2, leaf);
        }
        // trunk
        c.ellipse(23, 25 + bob, 11.8, 13, bark);
        for (int x : new int[]{15, 19, 27, 31}) c.rect(x, 17 + bob, 1, 17, barkD);
        c.rect(17, 18 + bob, 1, 6, barkL); c.rect(29, 20 + bob, 1, 5, barkL); c.rect(21, 27 + bob, 1, 5, barkL);
        // angry face
        c.ellipse(17.5, 22 + bob, 3.6, 3.0, rgb(28, 14, 18)); c.ellipse(28.5, 22 + bob, 3.6, 3.0, rgb(28, 14, 18));
        c.ellipse(17.8, 22.4 + bob, 2.2, 1.7, eye); c.ellipse(28.2, 22.4 + bob, 2.2, 1.7, eye);
        c.thickLine(13, 17 + bob, 21, 20 + bob, 2, barkD); c.thickLine(33, 17 + bob, 25, 20 + bob, 2, barkD);
        boolean open = pose >= 4;
        c.ellipse(23, 31 + bob, 6.0, open ? 4.4 : 2.4, rgb(28, 14, 18));
        c.ellipse(23, 31.6 + bob, 4.4, open ? 2.8 : 1.2, p2 ? rgb(255, 120, 60) : rgb(255, 170, 60));
        for (int x = 18; x <= 28; x += 2) c.set(x, 29 + bob, rgb(230, 220, 200));
        // leafy crown
        int cy = 9 + bob;
        int[][] blobs = {{23, cy, 10}, {13, cy + 3, 7}, {33, cy + 3, 7}, {17, cy - 4, 6}, {29, cy - 4, 6}, {23, cy - 5, 6}};
        for (int[] b : blobs) c.disc(b[0] + 0.5, b[1] + 0.5, b[2], leafD);
        for (int[] b : blobs) c.disc(b[0] + 0.0, b[1] - 0.6, b[2] - 1.3, leaf);
        for (int[] b : blobs) c.disc(b[0] - 1.6, b[1] - 2.2, Math.max(1.5, b[2] - 4.3), leafL);
        c.disc(15.5, cy + 6.5, 1.4, rgb(230, 60, 70)); c.disc(31.5, cy + 6, 1.4, rgb(250, 230, 100)); c.disc(24.5, cy - 8, 1.2, rgb(255, 255, 255));
        if (pose == 5) {                                                                     // spores around the crown
            int[][] sp = {{6, 3}, {10, 0}, {36, 1}, {41, 5}, {3, 10}, {43, 11}, {23, -1}};
            for (int[] s : sp) c.disc(s[0] + 0.5, s[1] + 0.5 + 1, 1.2, rgb(255, 236, 120));
        }
        c.bevel(0.1, 0.18);
        c.outline(rgb(44, 26, 24));
        return c;
    }

    // ------------------------------------------------------------------ city

    /** The grunt: a scruffy alley rat in a red scarf. */
    private static PixelCanvas rat(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(19, 14);
        int fur = rgb(140, 136, 152), furD = rgb(96, 92, 112), belly = rgb(200, 194, 206), pink = rgb(244, 156, 178);
        int scarf = rgb(224, 54, 62), scarfD = rgb(160, 30, 44);
        int reach = step == 0 ? 1 : -1;
        int lift = wind ? 1 : 0;
        // tail: long and pink
        c.line(4, 8, 2, 9, pink); c.line(2, 9, 1, 11, pink); c.line(1, 11, 3, 12, pink); c.set(4, 12, pink);
        c.rect(6 - reach, 10, 2, 3, furD); c.rect(13 + reach, 10, 2, 3, furD);
        c.ellipse(9.5, 8.0 - lift, 5.4, 3.4, fur);
        c.ellipseY(9.5, 9.4 - lift, 4.4, 2.0, belly, 9 - lift, 12);
        c.ellipse(15, 7.0 - lift, 3.2, 2.8, fur);
        c.tri(15, 8 - lift, 19, 8 - lift, 17, 6 - lift, fur);                              // snout
        c.set(18, 7 - lift, pink);
        c.disc(12.5, 3.6 - lift, 2.2, fur); c.disc(12.5, 3.8 - lift, 1.2, pink);             // big round ears
        c.disc(15.5, 3.2 - lift, 2.0, fur); c.disc(15.5, 3.4 - lift, 1.0, pink);
        c.set(15, 6 - lift, EYE); c.set(15, 5 - lift, WHITE);
        c.line(14, 4 - lift, 16, 5 - lift, EYE);
        c.rect(11, 9 - lift, 3, 2, scarf); c.set(10, 10 - lift, scarfD); c.set(10, 11 - lift, scarf);   // scarf and its tail
        if (wind) { c.set(17, 9 - lift, WHITE); c.set(15, 9 - lift, WHITE); c.rect(16, 8 - lift, 1, 1, rgb(60, 20, 30)); }
        c.rect(5 + reach, 10, 2, 3, fur); c.rect(12 - reach, 10, 2, 3, fur);
        c.rect(5 + reach, 12, 3, 1, furD); c.rect(12 - reach, 12, 3, 1, furD);
        c.bevel(0.1, 0.2);
        c.outline(rgb(46, 40, 60));
        return c;
    }

    /** The shooter: a hovering street drone with one big red eye. */
    private static PixelCanvas drone(int f, boolean wind) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int metal = rgb(148, 158, 180), metalL = rgb(198, 208, 226), metalD = rgb(88, 98, 124);
        // rotors
        if (f == 0) { c.rect(0, 2, 5, 1, metalL); c.rect(12, 2, 5, 1, metalL); }
        else { c.rect(1, 3, 3, 1, metalL); c.rect(13, 3, 3, 1, metalL); c.rect(0, 1, 2, 1, metalD); c.rect(15, 1, 2, 1, metalD); }
        c.rect(2, 3, 1, 3, metalD); c.rect(14, 3, 1, 3, metalD);
        c.line(8, 0, 8, 3, metalD); c.set(8, 0, rgb(255, 70, 80));                         // antenna
        c.ellipse(8.5, 8.0, 6.4, 4.6, metal);
        c.ellipseY(8.5, 9.6, 6.0, 3.0, metalD, 10, 14);
        c.line(3, 7, 14, 7, metalD);
        c.disc(8.5, 8.4, 3.3, rgb(20, 22, 36));
        c.disc(8.5, 8.4, wind ? 2.6 : 2.0, wind ? rgb(255, 250, 220) : rgb(255, 62, 72));
        c.set(7, 7, WHITE);
        if (wind) {
            c.set(4, 4, rgb(255, 230, 110)); c.set(13, 5, rgb(255, 230, 110)); c.set(12, 12, rgb(255, 230, 110)); c.set(4, 12, rgb(255, 230, 110));
        }
        c.rect(6, 13, 5, 1, rgb(255, 150, 60)); if (f == 1) c.rect(7, 14, 3, 1, rgb(255, 210, 100));   // thrusters
        c.bevel(0.12, 0.2);
        c.outline(rgb(34, 40, 62));
        return c;
    }

    /** The brute: a green dumpster golem with a snapping lid. */
    private static PixelCanvas dumpster(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(27, 25);
        int body = rgb(54, 130, 92), bodyL = rgb(96, 180, 130), bodyD = rgb(32, 84, 64);
        int lid = rgb(38, 96, 72), metal = rgb(150, 158, 174), metalD = rgb(98, 106, 124);
        int bob = step == 1 ? -1 : 0;
        // arms: pipes ending in fists
        int ay = wind ? 4 : 10;
        c.rect(0, ay + bob, 4, 9, metal); c.rect(23, ay + bob, 4, 9, metal);
        c.disc(2.0, ay + 9.5 + bob, 3.0, metalD); c.disc(25.0, ay + 9.5 + bob, 3.0, metalD);
        // body box
        c.rect(3, 10 + bob, 21, 11, body);
        for (int x : new int[]{8, 13, 18}) c.rect(x, 11 + bob, 1, 9, bodyD);
        c.rect(3, 10 + bob, 21, 1, bodyL); c.rect(3, 20 + bob, 21, 1, bodyD);
        // eyes in the gap under the lid, mouth of teeth
        c.rect(7, 12 + bob, 5, 3, rgb(24, 18, 22)); c.rect(15, 12 + bob, 5, 3, rgb(24, 18, 22));
        int glow = wind ? rgb(255, 236, 110) : rgb(255, 196, 60);
        c.rect(8, 13 + bob, 3, 2, glow); c.rect(16, 13 + bob, 3, 2, glow);
        c.set(9, 13 + bob, EYE); c.set(17, 13 + bob, EYE);
        c.line(6, 11 + bob, 11, 13 + bob, bodyD); c.line(21, 11 + bob, 16, 13 + bob, bodyD);
        c.rect(8, 17 + bob, 11, 3, rgb(24, 18, 22));
        for (int x = 8; x < 19; x += 2) { c.set(x, 17 + bob, WHITE); c.set(x + 1, 19 + bob, WHITE); }
        // the lid
        if (wind) {
            c.rect(3, 8 + bob, 21, 2, rgb(30, 22, 26));
            c.poly(new int[]{2, 25, 23, 4}, new int[]{7 + bob, 7 + bob, 0, 0}, lid);
            c.rect(4, 1, 19, 1, bodyL);
        } else {
            c.rect(2, 6 + bob, 23, 4, lid); c.rect(2, 6 + bob, 23, 1, bodyL); c.rect(2, 9 + bob, 23, 1, bodyD);
            c.rect(11, 7 + bob, 5, 2, metal);
        }
        // wheels
        for (int wx : new int[]{7, 20}) {
            c.disc(wx + 0.5, 22.0, 2.9, rgb(28, 28, 38)); c.disc(wx + 0.5, 22.0, 1.3, metal);
            c.set(wx + (step == 0 ? 0 : 1), 22, metalD);
        }
        c.bevel(0.1, 0.2);
        c.outline(rgb(20, 40, 36));
        return c;
    }

    /** Poses: 0/1 idle (sirens swap), 2/3 walk, 4 slam wind-up (fist raised), 5 bullet burst (cannons flare). */
    private static PixelCanvas warden(int pose, boolean p2) {
        PixelCanvas c = new PixelCanvas(46, 42);
        int armor = p2 ? rgb(128, 84, 100) : rgb(98, 114, 150), armorL = p2 ? rgb(184, 128, 136) : rgb(152, 170, 208), armorD = p2 ? rgb(78, 46, 66) : rgb(58, 68, 102);
        int dark = rgb(34, 38, 58), visor = p2 ? rgb(255, 250, 220) : rgb(255, 62, 72), gold = rgb(248, 204, 76);
        int bob = pose == 1 || pose == 3 ? -1 : 0;
        int step1 = pose == 2 ? 1 : 0, step2 = pose == 3 ? 1 : 0;
        // legs and boots
        c.rect(12, 32, 9, 6 - step1, armorD); c.rect(11, 37 - step1, 11, 3, dark);
        c.rect(25, 32, 9, 6 - step2, armorD); c.rect(24, 37 - step2, 11, 3, dark);
        // torso and chest plate
        c.rect(10, 14 + bob, 26, 19, armor);
        c.rect(14, 16 + bob, 18, 11, armorL);
        c.rect(14, 16 + bob, 18, 1, lighten(armorL, 0.3));
        c.rect(10, 29 + bob, 26, 3, dark); c.rect(21, 29 + bob, 4, 3, gold);
        // badge: a little gold star
        int sx = 23, sy = 21 + bob;
        c.set(sx, sy - 2, gold); c.rect(sx - 1, sy - 1, 3, 1, gold); c.rect(sx - 2, sy, 5, 1, gold); c.rect(sx - 1, sy + 1, 3, 1, gold);
        c.set(sx - 1, sy + 2, gold); c.set(sx + 1, sy + 2, gold);
        // shoulders with sirens
        c.rect(3, 13 + bob, 10, 9, armorD); c.rect(3, 13 + bob, 10, 2, armor);
        c.rect(33, 13 + bob, 10, 9, armorD); c.rect(33, 13 + bob, 10, 2, armor);
        int sirenA = (pose % 2 == 0) ? rgb(255, 70, 80) : rgb(90, 150, 255), sirenB = (pose % 2 == 0) ? rgb(90, 150, 255) : rgb(255, 70, 80);
        if (p2) sirenA = sirenB = rgb(255, 70, 80);
        c.rect(5, 9 + bob, 6, 4, sirenA); c.rect(6, 8 + bob, 4, 1, lighten(sirenA, 0.5));
        c.rect(35, 9 + bob, 6, 4, sirenB); c.rect(36, 8 + bob, 4, 1, lighten(sirenB, 0.5));
        // head: helmet with a red visor and an antenna
        c.ellipse(23, 8 + bob, 8, 7, armor);
        c.rect(15, 8 + bob, 16, 7, armor);
        c.rect(16, 9 + bob, 14, 4, dark);
        c.rect(17, 10 + bob, 12, 2, pose == 5 ? rgb(255, 250, 220) : visor);
        c.rect(17, 12 + bob, 12, 1, armorD);
        c.line(23, 1 + bob, 23, -1 + bob + 1, dark); c.set(23, 0 + bob, visor);
        c.rect(19, 3 + bob, 4, 1, armorL);
        // left arm with a big glass riot shield
        int sh = pose == 4 ? -4 : 0;
        c.rect(0, 16 + bob + sh, 8, 22, armorD);
        c.rect(1, 17 + bob + sh, 6, 20, rgb(150, 200, 232)); c.rect(1, 17 + bob + sh, 2, 20, rgb(210, 236, 250));
        c.rect(0, 26 + bob + sh, 8, 2, dark);
        // right arm: a cannon, or a fist raised for the slam
        if (pose == 4) {
            c.rect(37, 3, 7, 14, armorD); c.rect(36, 0, 9, 6, armor); c.rect(37, 1, 7, 2, armorL);
        } else {
            c.rect(36, 17 + bob, 9, 8, dark); c.rect(37, 18 + bob, 6, 2, armorD);
            int muzzle = pose == 5 ? rgb(255, 200, 80) : dark;
            c.rect(43, 19 + bob, 3, 4, muzzle);
        }
        if (pose == 5) {
            c.disc(17.5, 29.5, 2.0, rgb(255, 210, 90)); c.disc(29.5, 29.5, 2.0, rgb(255, 210, 90));
            c.set(46 - 1, 21 + bob, WHITE);
        }
        if (p2) {                                                                          // battle damage and smoke
            c.line(14, 17 + bob, 18, 24 + bob, dark); c.line(28, 20 + bob, 32, 30 + bob, dark);
            c.set(8, 5, rgb(120, 120, 130)); c.set(9, 3, rgb(160, 160, 170)); c.set(38, 4, rgb(120, 120, 130)); c.set(37, 2, rgb(160, 160, 170));
        }
        c.bevel(0.1, 0.18);
        c.outline(rgb(24, 26, 44));
        return c;
    }

    // ------------------------------------------------------------------ both

    /** The Shade: a hooded wisp with glowing eyes. Painted dark violet; the renderer turns it see-through in shadow mode. */
    private static PixelCanvas shade(int f, boolean wind) {
        PixelCanvas c = new PixelCanvas(19, 20);
        int body = rgb(92, 48, 152), bodyL = rgb(140, 92, 210), bodyD = rgb(52, 26, 96);
        int b = f == 0 ? 0 : -1;
        c.ellipse(9.5, 9.0 + b, 6.6, 6.6, body);
        c.rect(3, 9 + b, 13, 7, body);
        c.tri(6, 4 + b, 9, -1 + b, 13, 4 + b, body);
        for (int x = 3; x <= 15; x++) c.rect(x, 16 + b, 1, ((x + f) % 4 < 2) ? 3 : 2, body);       // wavy hem
        c.rect(3, 12 + b, 1, 4, bodyL); c.rect(4, 6 + b, 1, 3, bodyL); c.set(8, 1 + b, bodyL);
        c.rect(14, 12 + b, 2, 5, bodyD);
        c.ellipse(9.5, 10 + b, 4.2, 3.4, rgb(14, 8, 26));
        int eye = rgb(255, 150, 214);
        if (wind) {
            c.rect(6, 9 + b, 2, 3, eye); c.rect(11, 9 + b, 2, 3, eye); c.set(6, 9 + b, WHITE); c.set(12, 9 + b, WHITE);
            c.rect(0, 10 + b, 3, 3, rgb(210, 190, 250)); c.rect(16, 10 + b, 3, 3, rgb(210, 190, 250));
            c.set(0, 9 + b, rgb(210, 190, 250)); c.set(2, 9 + b, rgb(210, 190, 250)); c.set(16, 9 + b, rgb(210, 190, 250)); c.set(18, 9 + b, rgb(210, 190, 250));
        } else {
            c.rect(6, 9 + b, 2, 2, eye); c.rect(11, 9 + b, 2, 2, eye); c.set(6, 9 + b, WHITE); c.set(11, 9 + b, WHITE);
        }
        c.bevel(0.1, 0.2);
        c.outline(rgb(30, 14, 56));
        return c;
    }

    // ------------------------------------------------------------------ laboratory

    /** The grunt: an angry blob of green lab goo with googly eyes. */
    private static PixelCanvas ooze(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int g = rgb(96, 220, 96), gl = rgb(176, 255, 156), gd = rgb(46, 150, 74);
        double rx = wind ? 5.8 : step == 0 ? 7.2 : 7.9, ry = wind ? 8.6 : step == 0 ? 6.4 : 5.4;
        double cy = 15.6 - ry;
        c.ellipseY(8.5, cy, rx, ry, g, -100, 15);
        c.ellipseY(8.5 - 0.8, cy - 0.6, rx * 0.7, ry * 0.65, lighten(g, 0.12), -100, 15);
        c.rect((int) (8.5 - rx) + 1, 14, (int) (rx * 2) - 1, 2, gd);
        c.disc(5.0, cy + 2.6, 1.1, gl); c.disc(12.0, cy + 3.4, 0.9, gl); c.set(9, (int) cy + 5, gl);         // bubbles inside
        c.set(4, (int) (cy - 1), WHITE); c.set(5, (int) (cy - 2), WHITE);                                    // shine
        int ey = (int) Math.round(cy + 1.5);
        c.disc(6.0, ey, 1.9, WHITE); c.disc(11.0, ey - 0.4, 2.1, WHITE);
        c.set(7, ey, EYE); c.set(12, ey, EYE); c.set(7, ey + 1, EYE); c.set(12, ey + 1, EYE);
        c.line(4, ey - 3, 8, ey - 2, EYE); c.line(13, ey - 3, 9, ey - 2, EYE);                              // angry brows
        if (wind) {
            c.ellipse(9.5, ey + 4.0, 3.0, 2.0, rgb(24, 84, 50)); c.rect(8, ey + 3, 2, 1, WHITE);
            c.set(15, 14, g); c.set(15, 15, g); c.set(1, 15, g);                                            // drips
        } else {
            c.line(7, ey + 3, 11, ey + 3, rgb(24, 84, 50));
        }
        c.bevel(0.12, 0.2);
        c.outline(rgb(20, 72, 44));
        return c;
    }

    /** The runner: a wind-up toy mouse with a key in its back and glowing button eyes. */
    private static PixelCanvas clockworkMouse(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(19, 13);
        int metal = rgb(178, 188, 204), metalL = rgb(226, 232, 244), metalD = rgb(108, 118, 140), pink = rgb(255, 170, 190), gold = rgb(250, 208, 76);
        int crouch = wind ? 1 : 0, reach = step == 0 ? 1 : -1;
        c.rect(6 - reach, 9, 2, 3, metalD); c.rect(13 + reach, 9, 2, 3, metalD);
        c.line(4, 8 + crouch, 1, 6, metalD); c.line(1, 6, 2, 3, metalD); c.line(2, 3, 0, 2, metalD);          // wire tail
        c.ellipse(9, 7.5 + crouch, 5.6, 3.3, metal);
        c.ellipseY(9, 8.4 + crouch, 4.6, 2.0, metalL, 8 + crouch, 12);
        c.rect(7, 4 + crouch, 1, 1, metalD); c.rect(10, 5 + crouch, 1, 1, metalD); c.rect(12, 6 + crouch, 1, 1, metalD);    // rivets
        if (step == 0) { c.rect(8, 0 + crouch, 1, 4, gold); c.rect(6, 0 + crouch, 5, 1, gold); }             // the key turns
        else { c.rect(6, 2 + crouch, 5, 1, gold); c.rect(8, 0 + crouch, 1, 3, gold); c.rect(10, 1 + crouch, 1, 1, gold); }
        c.ellipse(14.5, 6.5 + crouch, 3.4, 2.8, metal);
        c.ellipse(17.0, 7.5 + crouch, 1.9, 1.3, metalL);
        c.disc(13.0, 3.6 + crouch, 2.0, pink); c.disc(16.0, 3.8 + crouch, 1.8, pink);                        // big round ears
        c.disc(13.0, 3.6 + crouch, 1.0, rgb(214, 100, 130));
        c.set(18, 7 + crouch, pink);
        int eye = wind ? rgb(255, 240, 90) : rgb(255, 70, 70);
        c.set(15, 5 + crouch, eye); c.set(16, 5 + crouch, eye); c.set(15, 6 + crouch, eye);
        if (wind) { c.set(17, 9 + crouch, WHITE); c.set(15, 9 + crouch, WHITE); c.set(8, 2, rgb(255, 240, 150)); c.set(11, 1, rgb(255, 240, 150)); }
        c.rect(5 + reach, 9, 2, 3, metal); c.rect(12 - reach, 9, 2, 3, metal);
        c.rect(5 + reach, 11, 2, 1, metalD); c.rect(12 - reach, 11, 2, 1, metalD);
        c.bevel(0.12, 0.2);
        c.outline(rgb(52, 58, 80));
        return c;
    }

    /** The shooter: a round-bottomed flask on stubby legs, full of bubbling acid that it spits. */
    private static PixelCanvas flask(int f, boolean wind) {
        PixelCanvas c = new PixelCanvas(17, 20);
        int glass = rgb(206, 236, 246), glassD = rgb(130, 176, 196), acid = wind ? rgb(150, 255, 110) : rgb(84, 220, 96), acidL = rgb(190, 255, 170);
        int lift = f == 1 ? -1 : 0;
        int fx = f == 0 ? 0 : 1;
        c.rect(5 + fx, 17, 3, 3, rgb(90, 96, 112)); c.rect(9 - fx, 17, 3, 3, rgb(90, 96, 112));               // legs
        c.rect(4 + fx, 19, 4, 1, rgb(50, 54, 68)); c.rect(9 - fx, 19, 4, 1, rgb(50, 54, 68));
        c.disc(8.5, 12.0 + lift, 6.6, glassD); c.disc(8.5, 12.0 + lift, 5.8, glass);
        c.rect(6, 3 + lift, 5, 6, glassD); c.rect(7, 3 + lift, 3, 6, glass);                                 // neck
        c.ellipseY(8.5, 12.6 + lift, 5.3, 5.0, acid, 10 + lift, 18);                                        // the liquid
        c.rect(7, 8 + lift, 3, 2, acid);
        c.rect(6, 10 + lift, 5, 1, acidL);
        c.disc(5.5, 14.5 + lift, 0.9, acidL); c.disc(11.5, 13.5 + lift, 0.8, acidL); c.disc(9, 16 + lift, 0.7, acidL);
        c.set(4, 9 + lift, WHITE); c.set(4, 10 + lift, WHITE); c.set(5, 8 + lift, WHITE);                      // glass shine
        // face in the acid
        c.disc(6.4, 13.0 + lift, 1.6, WHITE); c.disc(11.0, 12.6 + lift, 1.8, WHITE);
        c.set(7, 13 + lift, EYE); c.set(12, 13 + lift, EYE); c.set(7, 14 + lift, EYE); c.set(12, 13 + lift, EYE);
        c.line(4, 10 + lift, 8, 11 + lift, EYE); c.line(13, 10 + lift, 9, 11 + lift, EYE);
        if (wind) {
            c.rect(6, 0, 5, 3, glass);                                                                     // the cork has popped off...
            c.disc(13.5, 1.5, 1.6, rgb(150, 100, 60));
            c.disc(14.5, 5.5, 2.4, acid); c.set(14, 4, acidL); c.set(16, 2, acid);                         // ...and a glob is coming out
            c.ellipse(9.5, 15.5, 2.2, 1.4, rgb(24, 84, 50));
        } else {
            c.rect(6, 1 + lift, 5, 3, rgb(160, 110, 66)); c.rect(6, 1 + lift, 5, 1, rgb(206, 156, 100));    // cork
            c.set(8 + fx * 2, -1 + lift + 1, rgba(220, 255, 220, 200));
            c.line(7, 15 + lift, 10, 15 + lift, rgb(24, 84, 50));
        }
        c.bevel(0.1, 0.16);
        c.outline(rgb(34, 76, 84));
        return c;
    }

    /** The brute: a hulking stitched-together experiment, with bolts in its neck, that swings one huge fist. */
    private static PixelCanvas mutant(int step, boolean wind) {
        PixelCanvas c = new PixelCanvas(27, 26);
        int skin = rgb(150, 132, 196), skinL = rgb(196, 182, 232), skinD = rgb(96, 82, 146), pants = rgb(112, 84, 70), pantsD = rgb(72, 52, 46);
        int stitch = rgb(40, 40, 52), steel = rgb(150, 160, 176);
        int s1 = step == 0 ? 1 : 0, s2 = step == 1 ? 1 : 0;
        // legs
        c.rect(6, 17, 6, 8 - s1, pants); c.rect(15, 17, 6, 8 - s2, pants);
        c.rect(5, 23 - s1, 8, 3, rgb(52, 52, 60)); c.rect(14, 23 - s2, 8, 3, rgb(52, 52, 60));
        // back arm (left), hanging
        c.rect(1, 10, 5, 12, skinD); c.disc(3.5, 22.5, 3.0, skin);
        // torso: ragged shirt over a big green belly
        c.ellipse(13.5, 13.5, 8.5, 6.5, skin); c.rect(6, 10, 15, 8, skin);
        c.rect(6, 14, 15, 4, pants); c.rect(6, 14, 15, 1, pantsD);
        c.line(10, 10, 13, 15, stitch); c.line(13, 10, 10, 15, stitch); c.line(17, 11, 18, 16, stitch);      // stitches across the chest
        c.set(9, 12, skinL); c.set(10, 11, skinL);
        // head
        int hy = wind ? 0 : 0;
        c.ellipse(13.5, 6 + hy, 5.6, 5.0, skin);
        c.rect(8, 1 + hy, 11, 3, rgb(40, 36, 52));                                                       // flat black hair
        c.rect(8, 4 + hy, 11, 1, skinL);
        c.line(10, 4 + hy, 12, 4 + hy, stitch); c.set(11, 3 + hy, stitch); c.set(11, 5 + hy, stitch);        // forehead scar
        c.rect(10, 6 + hy, 3, 2, rgb(255, 224, 90)); c.rect(15, 6 + hy, 3, 2, rgb(255, 224, 90));           // yellow eyes
        c.set(12, 7 + hy, EYE); c.set(17, 7 + hy, EYE);
        c.line(9, 5 + hy, 13, 6 + hy, EYE); c.line(18, 5 + hy, 14, 6 + hy, EYE);
        if (wind) { c.rect(10, 9 + hy, 8, 3, rgb(60, 22, 34)); c.rect(11, 9 + hy, 6, 1, WHITE); }
        else { c.rect(11, 10 + hy, 6, 1, EYE); c.set(11, 9 + hy, WHITE); c.set(16, 9 + hy, WHITE); }
        c.rect(5, 8 + hy, 2, 2, steel); c.rect(20, 8 + hy, 2, 2, steel);                                  // neck bolts
        c.set(5, 8 + hy, WHITE); c.set(20, 8 + hy, WHITE);
        // front arm (right): a huge fist, raised for the slam
        if (wind) {
            c.rect(20, 1, 6, 11, skin); c.rect(24, 1, 2, 11, skinD);
            c.rect(19, 12, 7, 2, rgb(92, 72, 60));
            c.disc(23, 1.5, 3.4, skin); c.rect(20, 0, 7, 1, skinL); c.line(21, 2, 25, 2, skinD);
        } else {
            c.rect(20, 9, 6, 12, skin); c.rect(24, 9, 2, 12, skinD);
            c.rect(19, 9, 7, 2, rgb(92, 72, 60));
            c.disc(23, 22.5, 3.6, skin); c.rect(20, 20, 7, 1, skinL); c.line(20, 23, 26, 23, skinD);
        }
        c.bevel(0.1, 0.18);
        c.outline(rgb(42, 32, 66));
        return c;
    }

    /**
     * The final boss: a mad scientist in a stained lab coat with wild hair, mismatched goggles and a flask of glowing green. Poses:
     * 0/1 idle (bubbles and sparks), 2/3 walk, 4 slam wind-up (a giant wrench held high), 5 bullet burst (arms up, flask
     * raised, laughing). {@code p2} is the second phase: scorched coat, hair standing on end and crackling, eyes aflame.
     */
    private static PixelCanvas scientist(int pose, boolean p2) {
        PixelCanvas c = new PixelCanvas(46, 42);
        int coat = p2 ? rgb(206, 208, 216) : rgb(244, 246, 250), coatL = p2 ? rgb(232, 234, 240) : WHITE, coatD = p2 ? rgb(124, 128, 146) : rgb(180, 190, 208);
        int skin = rgb(244, 206, 170), skinD = rgb(206, 160, 124), pants = rgb(52, 54, 80), shoe = rgb(38, 30, 32);
        int hair = p2 ? rgb(200, 240, 255) : rgb(248, 248, 252), hairD = p2 ? rgb(120, 190, 240) : rgb(190, 196, 212);
        int glowG = rgb(74, 230, 140), glowD = rgb(30, 130, 84), glass = rgb(206, 236, 246), glove = rgb(250, 220, 90), steel = rgb(176, 186, 202), steelD = rgb(100, 112, 130);
        int bob = pose == 1 || pose == 3 ? -1 : 0;
        int s1 = pose == 2 ? 1 : 0, s2 = pose == 3 ? 1 : 0;
        boolean up = pose == 4 || pose == 5;

        // legs and shoes
        c.rect(16, 33, 5, 6 - s1, pants); c.rect(25, 33, 5, 6 - s2, pants);
        c.rect(14, 38 - s1, 8, 3, shoe); c.rect(24, 38 - s2, 8, 3, shoe);
        // the long coat: body, flared tails, shading, lapels, buttons, pocket, stains
        c.rect(11, 14 + bob, 24, 20, coat);
        c.rect(9, 29 + bob, 28, 6 - bob, coat);
        c.rect(12, 15 + bob, 2, 18, coatL); c.rect(31, 15 + bob, 4, 19, coatD); c.rect(9, 33, 28, 2, coatD);
        c.rect(20, 14 + bob, 6, 5, rgb(110, 160, 222));                                                  // shirt and a crooked tie
        c.tri(21, 18 + bob, 25, 18 + bob, 24, 27 + bob, rgb(220, 70, 70));
        c.tri(11, 14 + bob, 21, 14 + bob, 19, 26 + bob, coatD); c.tri(35, 14 + bob, 25, 14 + bob, 27, 26 + bob, coatD);
        c.line(23, 27 + bob, 23, 34, coatD);
        for (int y = 28; y <= 32; y += 2) c.set(21, y + bob, steelD);
        c.rect(12, 24 + bob, 5, 4, coatD); c.set(13, 22 + bob, rgb(220, 60, 60)); c.set(15, 22 + bob, rgb(60, 120, 230));
        c.disc(29.5, 29 + bob, 2.0, rgb(120, 200, 100)); c.disc(16, 31, 1.4, rgb(150, 110, 70)); c.set(31, 24 + bob, rgb(120, 200, 100));
        if (p2) {
            c.rect(26, 20 + bob, 5, 3, rgb(56, 46, 46)); c.rect(14, 31, 4, 3, rgb(56, 46, 46)); c.line(30, 16 + bob, 34, 26 + bob, rgb(70, 60, 60));
            c.line(19, 27 + bob, 16, 33, glowG); c.line(30, 30 + bob, 33, 34, glowG);                    // glowing green cracks
        }
        // head
        int hy = bob;
        c.ellipse(23, 8 + hy, 12.5, 7.6, hair);                                                           // wild hair
        int[][] spikes = {{10, 8, 1, 7, 9, 3}, {11, 4, 3, 0, 14, 2}, {16, 2, 13, 0, 19, 0}, {36, 8, 45, 7, 37, 3}, {35, 4, 43, 0, 32, 2}, {30, 2, 33, 0, 27, 0}};
        for (int[] t : spikes) c.tri(t[0], t[1] + hy, t[2], Math.max(0, t[3] + hy), t[4], t[5] + hy, hair);
        if (p2) { c.tri(21, 2 + hy, 23, 0, 25, 2 + hy, hair); c.tri(6, 5 + hy, 0, 2, 8, 2 + hy, hairD); c.tri(40, 5 + hy, 45, 2, 38, 2 + hy, hairD); }
        c.rect(14, 6 + hy, 4, 2, hairD); c.rect(28, 5 + hy, 4, 2, hairD);
        c.ellipse(23, 13 + hy, 7.2, 6.2, skin);
        c.disc(15.6, 13 + hy, 1.6, skin); c.disc(30.4, 13 + hy, 1.6, skin);                              // ears
        c.rect(15, 9 + hy, 17, 2, rgb(64, 52, 52));                                                       // goggle strap
        int lensA = p2 ? rgb(255, 210, 90) : glowG, lensB = p2 ? rgb(255, 90, 70) : rgb(255, 80, 80);
        c.disc(19, 10.5 + hy, 3.9, rgb(50, 52, 64)); c.disc(19, 10.5 + hy, 3.0, lensA);                  // one big lens...
        c.disc(28, 10.5 + hy, 2.9, rgb(50, 52, 64)); c.disc(28, 10.5 + hy, 2.0, lensB);                  // ...and one small one
        c.set(18, 9 + hy, WHITE); c.set(27, 10 + hy, WHITE); c.set(20, 11 + hy, lighten(lensA, 0.5));
        c.rect(22, 13 + hy, 3, 3, skinD);                                                                 // nose
        boolean wide = up;
        if (wide) { c.rect(16, 17 + hy, 14, 5, rgb(74, 22, 34)); c.rect(17, 17 + hy, 12, 2, WHITE); c.rect(18, 20 + hy, 10, 1, WHITE); c.rect(20, 21 + hy, 6, 1, rgb(230, 100, 110)); }
        else { c.rect(17, 17 + hy, 12, 3, rgb(74, 22, 34)); c.rect(18, 17 + hy, 10, 1, WHITE); c.rect(19, 19 + hy, 8, 1, WHITE); }
        c.set(16, 16 + hy, rgb(255, 160, 150)); c.set(30, 16 + hy, rgb(255, 160, 150));
        // arms
        if (pose == 5) {
            c.rect(3, 4, 5, 13, coat); c.rect(3, 4, 1, 13, coatL); c.rect(2, 17, 7, 2, coatD);        // left arm up, flask high
            c.disc(4.5, 3.5, 2.2, glove);
            c.disc(4.5, -0.5 + 6, 0.1, glove);
            c.disc(5.0, 1.5, 3.6, glass); c.disc(5.0, 2.5, 2.8, glowG); c.set(4, 1, WHITE);
            c.rect(38, 4, 5, 13, coat); c.rect(42, 4, 1, 13, coatD); c.rect(37, 17, 7, 2, coatD);      // right arm up, crackling
            c.disc(40.5, 3.0, 2.3, glove); c.disc(40.5, 1.5, 2.6, WHITE); c.disc(40.5, 1.5, 1.6, rgb(255, 250, 150));
            c.line(37, 1, 33, 4, rgb(255, 250, 150)); c.line(33, 4, 36, 7, rgb(255, 250, 150)); c.line(44, 1, 45, 6, rgb(255, 250, 150));
        } else {
            // left hand: a big flask of glowing liquid
            c.rect(4, 16 + bob, 7, 8, coat); c.rect(4, 16 + bob, 1, 8, coatL); c.rect(3, 23 + bob, 8, 2, coatD);
            c.disc(6.5, 26 + bob, 2.1, glove);
            c.rect(5, 26 + bob, 3, 3, glass);
            c.disc(5.8, 32 + bob, 5.4, glass); c.disc(5.8, 32.6 + bob, 4.4, glowG); c.disc(5.8, 32 + bob, 2.4, lighten(glowG, 0.3));
            c.set(3, 30 + bob, WHITE); c.set(4, 29 + bob, WHITE);
            c.set(pose % 2 == 0 ? 7 : 4, 27 + bob, lighten(glowG, 0.5)); c.set(pose % 2 == 0 ? 3 : 8, 25 + bob, glowG);            // bubbles
            if (pose == 4) { /* wrench arm handled below */ }
            // right arm: a giant wrench, raised for the slam
            if (pose == 4) {
                c.rect(36, 6, 6, 12, coat); c.rect(41, 6, 1, 12, coatD); c.rect(35, 17, 8, 2, coatD);
                c.disc(39, 6.5, 2.3, glove);
                c.rect(38, 0, 3, 9, steel); c.rect(38, 0, 1, 9, lighten(steel, 0.3));                       // the shaft
                c.rect(34, 0, 11, 3, steel); c.rect(34, 0, 11, 1, lighten(steel, 0.3)); c.rect(38, 1, 3, 2, rgb(24, 26, 40));
            } else {
                c.rect(36, 16 + bob, 6, 10, coat); c.rect(41, 16 + bob, 1, 10, coatD); c.rect(35, 25 + bob, 8, 2, coatD);
                c.disc(39, 28 + bob, 2.1, glove);
                c.rect(38, 30 + bob, 3, 9, steel); c.rect(38, 30 + bob, 1, 9, lighten(steel, 0.3));       // a wrench held down by his side
                c.rect(36, 38 + bob, 7, 3, steel); c.rect(38, 38 + bob, 3, 2, rgb(24, 26, 40));
            }
        }
        // crackling hair, phase two
        if (p2) {
            int t = pose % 2;
            int[][] sp = {{6, 1}, {40, 1}, {2, 6}, {44, 6}, {12, 0}, {34, 0}};
            for (int[] q : sp) { c.set(q[0] + t, q[1] + hy, WHITE); c.set(q[0] - 1 + t, q[1] + 1 + hy, rgb(160, 230, 255)); }
        }
        c.bevel(0.08, 0.14);
        c.outline(rgb(26, 26, 44));
        return c;
    }
}
