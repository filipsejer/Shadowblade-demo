package game;

import static game.PixelCanvas.*;

import java.util.Map;

/**
 * Sprites for the fights: XP gems (cyan, green, red by size), gold coins, the crate pickups (a heart, a magnet,
 * a bomb), treasure chests, the portal to the next stage, the orbiting spectral sword, and an icon for each equipment
 * slot (the Armory and the loot lists draw these).
 */
final class RunArt {
    private RunArt() {}

    static void register(Map<String, Sprite[]> m) {
        // small gems are cyan rather than green-blue, so they stand out against grass as well as asphalt and tile
        m.put("run.gem0", PeopleArt.frames(4, f -> gem(f, rgb(60, 220, 255), rgb(225, 250, 255), rgb(20, 90, 190)), 5, 11));
        m.put("run.gem1", PeopleArt.frames(4, f -> gem(f, rgb(170, 255, 90), rgb(240, 255, 210), rgb(40, 140, 30)), 5, 11));
        m.put("run.gem2", PeopleArt.frames(4, f -> gem(f, rgb(255, 70, 120), rgb(255, 215, 230), rgb(150, 20, 60)), 5, 11));
        m.put("run.coin", PeopleArt.frames(4, RunArt::coin, 3, 6));
        m.put("run.heart", PeopleArt.frames(2, RunArt::heart, 5, 9));
        m.put("run.magnet", PeopleArt.frames(2, RunArt::magnet, 5, 10));
        m.put("run.bomb", PeopleArt.frames(2, RunArt::bomb, 5, 11));
        m.put("run.chest.elite", PeopleArt.frames(2, f -> chest(f, rgb(150, 96, 48), rgb(255, 210, 80)), 9, 13));
        m.put("run.chest.boss", PeopleArt.frames(2, f -> chest(f, rgb(110, 50, 140), rgb(255, 150, 60)), 9, 13));
        m.put("run.portal", PeopleArt.frames(6, RunArt::portal, 13, 33));
        m.put("run.blade", PeopleArt.frames(1, f -> blade(), 3, 9));
        for (Item.Slot s : Item.Slot.values()) m.put("item." + s.name().toLowerCase(), PeopleArt.frames(1, f -> icon(s), 8, 8));
    }

    /**
     * A cut gem, turning: the facet highlight moves across frame by frame. A white rim inside a near-black outline keeps
     * it readable on any floor, whatever the colour.
     */
    private static PixelCanvas gem(int f, int body, int light, int dark) {
        PixelCanvas c = new PixelCanvas(11, 13);
        c.poly(new int[]{5, 9, 5, 1}, new int[]{1, 6, 11, 6}, light);                 // the rim
        c.poly(new int[]{5, 8, 5, 2}, new int[]{2, 6, 10, 6}, body);
        c.tri(5, 2, 8, 6, 5, 6, mix(body, light, 0.4));
        c.tri(2, 6, 5, 10, 5, 6, mix(body, dark, 0.45));
        int hx = 3 + f % 4;
        c.set(Math.min(6, hx), 5, rgb(255, 255, 255));
        c.set(Math.min(6, hx), 4, withAlpha(light, 200));
        c.outline(rgb(12, 14, 28));
        return c;
    }

    /** A gold coin spinning: full face, three-quarter, edge-on, three-quarter. */
    private static PixelCanvas coin(int f) {
        PixelCanvas c = new PixelCanvas(7, 7);
        int gold = rgb(255, 205, 60), light = rgb(255, 245, 170), dark = rgb(170, 110, 20);
        double[] rx = {3.0, 2.0, 0.8, 2.0};
        c.ellipse(3, 3, rx[f], 3.0, gold);
        if (f != 2) c.ellipse(3, 3, Math.max(0.6, rx[f] - 1.2), 1.6, mix(gold, dark, 0.3));
        c.set(3 - (f == 0 ? 1 : 0), 1, light);
        c.outline(dark);
        return c;
    }

    private static PixelCanvas heart(int f) {
        PixelCanvas c = new PixelCanvas(11, 10);
        int red = f == 0 ? rgb(240, 60, 80) : rgb(255, 90, 110);
        c.disc(3, 3, 2.6, red);
        c.disc(7, 3, 2.6, red);
        c.tri(0, 4, 10, 4, 5, 9, red);
        c.set(2, 2, rgb(255, 210, 220));
        c.set(3, 2, rgb(255, 180, 190));
        c.outline(rgb(110, 20, 35));
        return c;
    }

    /** A horseshoe magnet, red with silver tips. */
    private static PixelCanvas magnet(int f) {
        PixelCanvas c = new PixelCanvas(11, 11);
        int red = rgb(225, 50, 60), silver = rgb(215, 220, 235);
        c.ellipseY(5, 5, 5, 5, red, -1, 5.5);
        c.ellipseY(5, 5, 2.2, 2.2, 0, -1, 5.5);
        for (int y = 0; y < 5; y++) for (int x = 3; x <= 7; x++) if (Math.hypot(x - 5, y - 5) < 2.3) c.set(x, y, 0);
        c.rect(0, 5, 3, 3, red);
        c.rect(8, 5, 3, 3, red);
        c.rect(0, 8, 3, 3, silver);
        c.rect(8, 8, 3, 3, silver);
        if (f == 1) { c.set(1, 9, rgb(255, 255, 255)); c.set(9, 9, rgb(255, 255, 255)); }
        c.outline(rgb(70, 20, 25));
        return c;
    }

    /** A round black bomb with a fizzing fuse. */
    private static PixelCanvas bomb(int f) {
        PixelCanvas c = new PixelCanvas(11, 12);
        c.disc(5, 7, 4.2, rgb(50, 50, 62));
        c.set(3, 5, rgb(140, 140, 160));
        c.set(4, 5, rgb(110, 110, 130));
        c.rect(4, 1, 2, 2, rgb(120, 110, 90));
        c.set(f == 0 ? 6 : 7, 0, rgb(255, 220, 90));
        c.set(f == 0 ? 7 : 6, 0, rgb(255, 140, 40));
        c.outline(rgb(15, 15, 20));
        return c;
    }

    /** A treasure chest: wood (or dark lacquer) banded with metal, a lock glinting in the second frame. */
    private static PixelCanvas chest(int f, int wood, int trim) {
        PixelCanvas c = new PixelCanvas(19, 14);
        c.rect(1, 5, 17, 8, wood);
        c.ellipseY(9.5, 5.5, 8.5, 4.5, lighten(wood, 0.12), -1, 5.5);
        c.rect(1, 5, 17, 1, trim);
        c.rect(3, 1, 2, 12, trim);
        c.rect(14, 1, 2, 12, trim);
        c.rect(8, 5, 3, 4, trim);
        c.set(9, 7, f == 1 ? rgb(255, 255, 255) : darken(trim, 0.4));
        c.bevel(0.25, 0.25);
        c.outline(rgb(30, 18, 10));
        return c;
    }

    /** A swirling violet gateway on a stone base. */
    private static PixelCanvas portal(int f) {
        PixelCanvas c = new PixelCanvas(27, 35);
        c.ellipse(13, 31, 12, 3.4, rgb(90, 88, 100));
        c.ellipse(13, 30, 10, 2.6, rgb(130, 128, 145));
        c.ellipse(13, 15, 10, 14, rgb(70, 30, 130));
        c.ellipse(13, 15, 8, 12, rgb(120, 60, 210));
        c.ellipse(13, 15, 5.5, 9, rgb(170, 110, 255));
        c.ellipse(13, 15, 3, 5.5, rgb(225, 200, 255));
        for (int i = 0; i < 10; i++) {                                          // specks spiralling in
            double a = i * 0.9 + f * Math.PI / 3, r = 2 + (i * 7 % 10) * 0.8;
            c.set((int) Math.round(13 + Math.cos(a) * r * 0.8), (int) Math.round(15 + Math.sin(a) * r * 1.3), rgb(255, 240, 255));
        }
        c.outline(rgb(40, 16, 70));
        return c;
    }

    /** A spectral sword pointing up; spun around its middle as it orbits. */
    private static PixelCanvas blade() {
        PixelCanvas c = new PixelCanvas(7, 17);
        int steel = rgb(215, 230, 255), edge = rgb(150, 190, 255), gold = rgb(255, 205, 90);
        c.rect(2, 1, 3, 10, steel);
        c.set(3, 0, steel);
        c.rect(3, 1, 1, 10, rgb(255, 255, 255));
        c.rect(0, 11, 7, 2, gold);
        c.rect(3, 13, 1, 3, rgb(120, 80, 50));
        c.set(3, 16, gold);
        c.outline(edge);
        return c;
    }

    /** A 17x17 icon for an equipment slot. */
    private static PixelCanvas icon(Item.Slot s) {
        PixelCanvas c = new PixelCanvas(17, 17);
        int steel = rgb(200, 205, 220), dark = rgb(110, 115, 135), gold = rgb(255, 200, 80), leather = rgb(140, 90, 50);
        switch (s) {
            case WEAPON -> {
                c.thickLine(3, 13, 13, 3, 2, steel);
                c.line(4, 13, 13, 4, rgb(255, 255, 255));
                c.thickLine(2, 10, 6, 14, 2, gold);
                c.thickLine(1, 15, 3, 13, 2, leather);
            }
            case HELM -> {
                c.ellipseY(8, 9, 6.5, 7, steel, -1, 9.5);
                c.rect(2, 9, 13, 3, steel);
                c.rect(7, 6, 3, 8, dark);
                c.rect(3, 9, 3, 1, dark);
                c.rect(11, 9, 3, 1, dark);
                c.rect(7, 1, 3, 2, rgb(220, 60, 60));
            }
            case ARMOR -> {
                c.rect(3, 3, 11, 12, steel);
                c.rect(0, 3, 4, 5, steel);
                c.rect(13, 3, 4, 5, steel);
                c.rect(6, 2, 5, 3, 0);
                c.rect(8, 5, 1, 9, dark);
                c.rect(3, 10, 11, 1, gold);
            }
            case GLOVES -> {
                c.rect(4, 5, 8, 8, leather);
                c.rect(4, 2, 2, 4, leather);
                c.rect(7, 1, 2, 5, leather);
                c.rect(10, 2, 2, 4, leather);
                c.rect(12, 7, 3, 3, leather);
                c.rect(4, 12, 8, 3, gold);
            }
            case BOOTS -> {
                c.rect(4, 2, 5, 10, leather);
                c.rect(4, 11, 10, 4, leather);
                c.rect(4, 14, 11, 1, dark);
                c.rect(4, 5, 5, 1, gold);
                c.set(13, 12, lighten(leather, 0.3));
            }
            case RING -> {
                c.ellipse(8, 10, 6, 5, gold);
                c.ellipse(8, 10, 3.6, 2.8, 0);
                c.disc(8, 4, 2.8, rgb(90, 200, 255));
                c.set(7, 3, rgb(230, 250, 255));
            }
        }
        c.bevel(0.2, 0.25);
        c.outline(rgb(20, 20, 28));
        return c;
    }
}
