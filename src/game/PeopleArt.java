package game;

import static game.PixelCanvas.*;

import java.util.Map;
import java.util.function.IntFunction;

/** The hero, the guide and the three shopkeepers, painted in code. All of them are small round-headed chibis. */
final class PeopleArt {
    private PeopleArt() {}

    private static final int OUT = rgb(36, 24, 46);
    private static final int SKIN = rgb(255, 216, 176), SKIN_D = rgb(226, 168, 128);
    private static final int HAIR = rgb(128, 78, 44), HAIR_L = rgb(176, 116, 66);
    private static final int TUNIC = rgb(58, 118, 255), TUNIC_L = rgb(136, 182, 255), TUNIC_D = rgb(34, 78, 208);
    private static final int PANTS = rgb(66, 62, 100), BOOT = rgb(100, 62, 44);
    private static final int BELT = rgb(240, 200, 80);
    private static final int EYE = rgb(34, 30, 58), WHITE = rgb(255, 255, 255), BLUSH = rgb(255, 152, 152);
    private static final int STEEL = rgb(226, 236, 248), STEEL_D = rgb(150, 166, 194), GOLD = rgb(248, 204, 76);
    private static final int WOOD = rgb(150, 100, 60), WOOD_L = rgb(190, 136, 84), WOOD_D = rgb(104, 68, 42);

    static void register(Map<String, Sprite[]> m) {
        // the hero faces down / up / right (left is the right one mirrored)
        m.put("hero.down.idle", frames(2, i -> heroFront(-1, i, -1), 10, 22));
        m.put("hero.down.walk", frames(4, i -> heroFront(i, i % 2 == 1 ? 1 : 0, -1), 10, 22));
        m.put("hero.down.attack", frames(2, i -> heroFront(-1, 0, i), 10, 22));
        m.put("hero.up.idle", frames(2, i -> heroBack(-1, i, -1), 10, 22));
        m.put("hero.up.walk", frames(4, i -> heroBack(i, i % 2 == 1 ? 1 : 0, -1), 10, 22));
        m.put("hero.up.attack", frames(2, i -> heroBack(-1, 0, i), 10, 22));
        m.put("hero.side.idle", frames(2, i -> heroSide(-1, i, -1), 10, 22));
        m.put("hero.side.walk", frames(4, i -> heroSide(i, i % 2 == 1 ? 1 : 0, -1), 10, 22));
        m.put("hero.side.attack", frames(2, i -> heroSide(-1, 0, i), 10, 22));
        m.put("hero.roll", frames(4, PeopleArt::heroRoll, 8, 8));

        m.put("guide.idle", frames(2, PeopleArt::guide, 11, 25));
        m.put("shop.combat", frames(2, i -> shop(0, i), 17, 45));
        m.put("shop.spells", frames(2, i -> shop(1, i), 17, 45));
        m.put("shop.survival", frames(2, i -> shop(2, i), 17, 45));
    }

    /** Which of the hero's three views ("side", "down", "up") to show for a facing angle. */
    static String heroDir(double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        if (Math.abs(c) >= Math.abs(s)) return "side";
        return s > 0 ? "down" : "up";
    }

    /** The side view faces right; it's mirrored when the hero faces left. */
    static boolean heroFlip(double angle) { return Math.cos(angle) < 0; }

    static Sprite[] frames(int n, IntFunction<PixelCanvas> paint, int ax, int ay) {
        Sprite[] out = new Sprite[n];
        for (int i = 0; i < n; i++) out[i] = new Sprite(paint.apply(i), ax, ay);
        return out;
    }

    // ------------------------------------------------------------------ the hero

    /** Head seen from the front: spiky brown hair, big eyes, blush. (cx, top) is the head's centre column and top row. */
    private static void headFront(PixelCanvas c, int cx, int top, int hair, int hairL) {
        c.ellipse(cx + 0.5, top + 5.2, 5.6, 5.2, SKIN);
        c.ellipse(cx + 0.5, top + 3.2, 6.2, 4.4, hair);                       // hair cap
        c.ellipse(cx + 0.5, top + 6.4, 4.7, 3.7, SKIN);                       // face, drawn over the cap
        c.tri(cx - 5, top + 3, cx - 4, top - 1, cx - 2, top + 2, hair);       // spikes
        c.tri(cx - 2, top + 1, cx, top - 2, cx + 2, top + 1, hair);
        c.tri(cx + 2, top + 2, cx + 4, top - 1, cx + 5, top + 3, hair);
        c.rect(cx - 4, top + 3, 9, 1, hair);                                  // fringe
        c.set(cx - 3, top + 4, hair); c.set(cx + 3, top + 4, hair);
        c.set(cx - 2, top, hairL); c.set(cx + 1, top - 1, hairL); c.set(cx - 4, top + 2, hairL);
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);         // eye shine
        c.set(cx - 4, top + 8, BLUSH); c.set(cx + 4, top + 8, BLUSH);
        c.rect(cx, top + 9, 2, 1, SKIN_D);                                    // little mouth
    }

    private static PixelCanvas heroFront(int step, int bob, int atk) {
        PixelCanvas c = new PixelCanvas(21, 25);
        int b = -bob;
        int l1 = step == 1 ? 1 : 0, l2 = step == 3 ? 1 : 0;
        c.rect(7, 18 - l1, 3, 2, PANTS); c.rect(7, 20 - l1, 3, 2, BOOT);
        c.rect(11, 18 - l2, 3, 2, PANTS); c.rect(11, 20 - l2, 3, 2, BOOT);
        if (atk < 0) sword(c, 15, 6 + b, 9);                                  // sword held upright at the right hip
        c.rect(7, 12 + b, 7, 7, TUNIC);
        c.rect(7, 12 + b, 7, 1, TUNIC_L);
        c.rect(7, 16 + b, 7, 1, BELT); c.set(10, 16 + b, GOLD); c.set(11, 16 + b, GOLD);
        c.rect(6, 17 + b, 1, 2, TUNIC_D); c.rect(13, 17 + b, 1, 2, TUNIC_D);
        c.rect(5, 13 + b, 2, 4, TUNIC_D); c.rect(5, 17 + b, 2, 1, SKIN);
        if (atk == 0) {                                                        // wind-up: sword up over the shoulder
            c.rect(14, 11 + b, 2, 3, TUNIC_D); c.rect(14, 10 + b, 2, 1, SKIN);
            c.thickLine(15, 10 + b, 18, 1 + b, 2, STEEL); c.rect(13, 9 + b, 4, 1, GOLD);
        } else if (atk == 1) {                                                 // strike: slashed across the front
            c.rect(14, 12 + b, 2, 3, TUNIC_D); c.rect(15, 11 + b, 2, 1, SKIN);
            c.thickLine(17, 10 + b, 3, 21 + b, 2, STEEL); c.rect(15, 9 + b, 4, 1, GOLD);
        } else {
            c.rect(14, 13 + b, 2, 4, TUNIC_D); c.rect(14, 17 + b, 2, 1, SKIN);
        }
        headFront(c, 10, 2 + b, HAIR, HAIR_L);
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    private static PixelCanvas heroBack(int step, int bob, int atk) {
        PixelCanvas c = new PixelCanvas(21, 25);
        int b = -bob;
        int l1 = step == 1 ? 1 : 0, l2 = step == 3 ? 1 : 0;
        c.rect(7, 18 - l1, 3, 2, PANTS); c.rect(7, 20 - l1, 3, 2, BOOT);
        c.rect(11, 18 - l2, 3, 2, PANTS); c.rect(11, 20 - l2, 3, 2, BOOT);
        c.rect(7, 12 + b, 7, 7, TUNIC_D);
        c.rect(7, 12 + b, 7, 1, TUNIC);
        c.rect(7, 16 + b, 7, 1, BELT);
        c.rect(5, 13 + b, 2, 4, TUNIC_D); c.rect(5, 17 + b, 2, 1, SKIN);
        c.rect(14, 13 + b, 2, 4, TUNIC_D); c.rect(14, 17 + b, 2, 1, SKIN);
        if (atk < 0) {
            c.thickLine(5, 19 + b, 15, 8 + b, 2, STEEL_D);                    // sword slung across the back
            c.rect(13, 6 + b, 4, 1, GOLD); c.set(16, 5 + b, STEEL);
        } else {
            c.thickLine(16, atk == 0 ? 3 + b : 12 + b, 16, atk == 0 ? 12 + b : 22 + b, 2, STEEL);
            c.rect(14, atk == 0 ? 12 + b : 11 + b, 5, 1, GOLD);
        }
        c.ellipse(10.5, 7.5 + b, 6.0, 5.6, HAIR);                              // the back of the head is all hair
        c.tri(5, 5 + b, 6, 0 + b, 8, 3 + b, HAIR);
        c.tri(8, 2 + b, 10, -1 + b, 12, 2 + b, HAIR);
        c.tri(12, 3 + b, 14, 0 + b, 16, 5 + b, HAIR);
        c.set(9, 2 + b, HAIR_L); c.set(13, 3 + b, HAIR_L); c.rect(7, 5 + b, 2, 1, HAIR_L);
        c.rect(8, 11 + b, 5, 1, SKIN_D);                                       // a bit of neck
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    private static PixelCanvas heroSide(int step, int bob, int atk) {
        PixelCanvas c = new PixelCanvas(21, 25);
        int b = -bob;
        int stride = step == 1 ? 2 : step == 3 ? -2 : 0;
        int lift1 = step == 1 ? 1 : 0, lift2 = step == 3 ? 1 : 0;
        // back leg (darker) and front leg
        c.rect(8 - stride / 2, 18 - lift2, 3, 2, darken(PANTS, 0.25)); c.rect(8 - stride / 2, 20 - lift2, 3, 2, darken(BOOT, 0.2));
        c.rect(10 + stride / 2, 18 - lift1, 3, 2, PANTS); c.rect(10 + stride / 2, 20 - lift1, 4, 2, BOOT);
        c.rect(8, 12 + b, 6, 7, TUNIC);
        c.rect(8, 12 + b, 6, 1, TUNIC_L);
        c.rect(8, 16 + b, 6, 1, BELT); c.set(12, 16 + b, GOLD);
        // front arm and sword
        if (atk == 0) {
            c.rect(11, 11 + b, 3, 2, TUNIC_D); c.rect(13, 10 + b, 2, 2, SKIN);
            c.thickLine(14, 10 + b, 18, 1 + b, 2, STEEL); c.rect(12, 10 + b, 4, 1, GOLD);
        } else if (atk == 1) {
            c.rect(12, 13 + b, 4, 2, TUNIC_D); c.rect(16, 13 + b, 2, 2, SKIN);
            c.thickLine(17, 14 + b, 20, 14 + b, 2, STEEL); c.rect(16, 12 + b, 1, 5, GOLD);
            c.rect(17, 13 + b, 3, 1, STEEL);
        } else {
            c.rect(10, 13 + b, 3, 4, TUNIC_D); c.rect(10, 17 + b, 3, 1, SKIN);
            c.thickLine(12, 17 + b, 17, 12 + b, 2, STEEL_D); c.rect(11, 16 + b, 3, 1, GOLD);
        }
        // head: hair at the back, face at the front
        c.ellipse(11.0, 8.2 + b, 5.6, 5.4, SKIN);
        c.ellipse(9.4, 6.4 + b, 5.6, 4.7, HAIR);
        c.ellipse(13.2, 9.4 + b, 3.9, 3.5, SKIN);
        c.tri(5, 8 + b, 4, 3 + b, 8, 4 + b, HAIR);
        c.tri(8, 3 + b, 10, -1 + b, 12, 3 + b, HAIR);
        c.tri(11, 3 + b, 14, 0 + b, 15, 5 + b, HAIR);
        c.rect(11, 5 + b, 5, 1, HAIR);
        c.set(9, 1 + b, HAIR_L); c.set(12, 2 + b, HAIR_L);
        c.rect(14, 8 + b, 1, 2, EYE); c.set(14, 8 + b, WHITE);
        c.set(16, 10 + b, SKIN_D);                                             // nose
        c.set(13, 11 + b, BLUSH);
        c.set(15, 12 + b, SKIN_D);
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    /** The dodge roll: the hero tucked into a ball, spinning. */
    private static PixelCanvas heroRoll(int f) {
        PixelCanvas c = new PixelCanvas(17, 17);
        c.disc(8.5, 8.5, 6.6, TUNIC);
        c.disc(8.0, 8.0, 4.6, TUNIC_L);
        c.disc(9.0, 9.0, 3.0, TUNIC);
        double a = f * Math.PI / 2;
        int hx = (int) Math.round(8.5 + Math.cos(a) * 5.0), hy = (int) Math.round(8.5 + Math.sin(a) * 5.0);
        c.disc(hx + 0.5, hy + 0.5, 2.6, HAIR);                                 // head / hair
        c.disc(hx + 0.5 - Math.cos(a) * 0.5, hy + 0.5 + 1 - Math.sin(a) * 0.5, 1.4, SKIN);
        int bx = (int) Math.round(8.5 - Math.cos(a) * 5.4), by = (int) Math.round(8.5 - Math.sin(a) * 5.4);
        c.disc(bx + 0.5, by + 0.5, 2.0, BOOT);                                 // boots on the other side
        c.set((int) Math.round(8.5 + Math.cos(a + 1.4) * 4), (int) Math.round(8.5 + Math.sin(a + 1.4) * 4), STEEL);
        c.bevel(0.14, 0.2);
        c.outline(OUT);
        return c;
    }

    private static void sword(PixelCanvas c, int x, int y, int len) {
        c.rect(x, y, 2, len, STEEL); c.rect(x, y, 1, len, WHITE); c.set(x, y - 1, STEEL);
        c.rect(x - 1, y + len, 4, 1, GOLD); c.rect(x, y + len + 1, 2, 1, BOOT);
    }

    // ------------------------------------------------------------------ the guide

    private static final int CLOAK = rgb(70, 206, 96), CLOAK_D = rgb(38, 138, 66), CLOAK_L = rgb(160, 244, 176);

    /** A green hooded traveller with a glowing lantern on a staff, floating a little above the ground. */
    private static PixelCanvas guide(int f) {
        PixelCanvas c = new PixelCanvas(23, 27);
        int b = f == 0 ? 0 : -1;
        // staff and lantern (behind the cloak's right edge)
        c.rect(19, 8 + b, 1, 17, WOOD);
        c.rect(17, 5 + b, 5, 5, GOLD); c.rect(18, 6 + b, 3, 3, rgb(255, 250, 190));
        c.set(19, 4 + b, WOOD_D); c.rect(18, 10 + b, 3, 1, WOOD_D);
        // cloak
        c.poly(new int[]{8, 15, 18, 4}, new int[]{13 + b, 13 + b, 23 + b, 23 + b}, CLOAK);
        c.poly(new int[]{9, 14, 15, 8}, new int[]{14 + b, 14 + b, 22 + b, 22 + b}, CLOAK_L);
        c.rect(5, 21 + b, 12, 1, CLOAK_D);
        for (int x = 4; x <= 17; x++) if ((x + f) % 3 == 0) c.set(x, 24 + b, CLOAK);   // fluttering hem
        c.rect(4, 23 + b, 14, 1, CLOAK_D);
        // hood and face
        c.ellipse(11.5, 8.5 + b, 6.4, 6.2, CLOAK);
        c.tri(8, 4 + b, 11, -1 + b, 15, 4 + b, CLOAK);
        c.ellipse(11.5, 9.5 + b, 4.4, 4.2, rgb(28, 70, 46));
        c.ellipse(11.5, 10.0 + b, 3.4, 3.2, SKIN);
        c.rect(9, 9 + b, 1, 2, EYE); c.rect(13, 9 + b, 1, 2, EYE);
        c.set(9, 9 + b, WHITE); c.set(13, 9 + b, WHITE);
        c.set(8, 11 + b, BLUSH); c.set(14, 11 + b, BLUSH);
        c.rect(11, 12 + b, 2, 1, SKIN_D);
        c.rect(8, 6 + b, 2, 1, CLOAK_L); c.rect(12, 3 + b, 2, 1, CLOAK_L);
        // hands holding the staff
        c.rect(17, 15 + b, 3, 2, SKIN);
        c.bevel(0.14, 0.2);
        c.outline(rgb(20, 60, 40));
        return c;
    }

    // ------------------------------------------------------------------ shops

    /** A shopkeeper behind a little counter with wares on it. kind: 0 swordsmaster, 1 wizard, 2 survivalist. */
    private static PixelCanvas shop(int kind, int f) {
        PixelCanvas c = new PixelCanvas(35, 47);
        int b = f == 0 ? 0 : -1;
        int ox = 7, oy = 12;
        switch (kind) {
            case 0 -> swordsmaster(c, ox, oy, b);
            case 1 -> wizard(c, ox, oy, b);
            default -> survivalist(c, ox, oy, b);
        }
        // the counter, in front of the shopkeeper's legs
        c.rect(3, 35, 29, 11, WOOD);
        c.rect(3, 35, 29, 2, WOOD_L);
        c.rect(3, 44, 29, 2, WOOD_D);
        for (int x = 8; x < 32; x += 8) c.rect(x, 38, 1, 6, WOOD_D);
        c.rect(4, 39, 27, 1, darken(WOOD, 0.18));
        wares(c, kind);
        c.bevel(0.1, 0.16);
        c.outline(OUT);
        return c;
    }

    private static void wares(PixelCanvas c, int kind) {
        int y = 34;   // the counter's top surface
        switch (kind) {
            case 0 -> {                                                          // swords laid out and a whetstone
                c.rect(6, y - 1, 8, 1, STEEL); c.rect(5, y - 1, 1, 1, GOLD); c.rect(14, y - 1, 1, 1, STEEL_D);
                c.rect(17, y - 2, 6, 2, rgb(130, 132, 146)); c.rect(17, y - 2, 6, 1, rgb(176, 178, 190));
                c.rect(26, y - 2, 4, 2, rgb(216, 66, 66)); c.set(27, y - 3, rgb(246, 190, 70));
            }
            case 1 -> {                                                          // crystal ball, book and potions
                c.disc(9.5, y - 3, 3.1, rgb(170, 130, 250)); c.set(8, y - 4, WHITE); c.set(9, y - 5, rgb(214, 190, 255));
                c.rect(7, y - 1, 5, 1, rgb(90, 60, 130));
                c.rect(16, y - 3, 6, 3, rgb(176, 60, 86)); c.rect(17, y - 2, 4, 1, rgb(240, 230, 210));
                c.rect(25, y - 4, 2, 4, rgb(80, 200, 130)); c.rect(25, y - 5, 2, 1, WOOD_D);
                c.rect(28, y - 3, 2, 3, rgb(90, 150, 250)); c.rect(28, y - 4, 2, 1, WOOD_D);
            }
            default -> {                                                         // coiled rope, a tent, a lantern
                c.ellipse(9.5, y - 1.5, 3.5, 1.7, rgb(214, 180, 110)); c.ellipse(9.5, y - 1.5, 1.5, 0.7, WOOD_D);
                c.tri(15, y, 21, y, 18, y - 6, rgb(70, 160, 130)); c.tri(18, y, 20, y, 18, y - 3, rgb(30, 90, 74));
                c.rect(26, y - 4, 3, 4, GOLD); c.rect(27, y - 3, 1, 2, rgb(255, 250, 190)); c.rect(26, y - 5, 3, 1, WOOD_D);
            }
        }
    }

    private static void npcHead(PixelCanvas c, int ox, int oy, int hair, int b) {
        int cx = ox + 10, top = oy + 2 + b;
        c.ellipse(cx + 0.5, top + 5.2, 5.6, 5.2, SKIN);
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);
        c.set(cx - 4, top + 8, BLUSH); c.set(cx + 4, top + 8, BLUSH);
        c.rect(cx, top + 9, 2, 1, SKIN_D);
    }

    private static void swordsmaster(PixelCanvas c, int ox, int oy, int b) {
        int red = rgb(206, 58, 66), redD = rgb(140, 34, 50), redL = rgb(238, 110, 100);
        int hair = rgb(40, 34, 52);
        c.rect(ox + 6, oy + 12 + b, 9, 12, red);                                    // gi
        c.rect(ox + 6, oy + 12 + b, 9, 1, redL);
        c.rect(ox + 6, oy + 17 + b, 9, 2, GOLD);                                    // sash
        c.rect(ox + 4, oy + 13 + b, 3, 6, redD); c.rect(ox + 14, oy + 13 + b, 3, 6, redD);
        c.rect(ox + 4, oy + 19 + b, 3, 2, SKIN); c.rect(ox + 14, oy + 19 + b, 3, 2, SKIN);
        c.rect(ox + 3, oy + 12 + b, 4, 2, GOLD); c.rect(ox + 14, oy + 12 + b, 4, 2, GOLD);   // shoulder plates
        // a big sword standing behind the right shoulder
        c.rect(ox + 17, oy - 1 + b, 2, 18, STEEL); c.rect(ox + 17, oy - 1 + b, 1, 18, WHITE);
        c.rect(ox + 15, oy + 17 + b, 6, 1, GOLD);
        npcHead(c, ox, oy, hair, b);
        int cx = ox + 10, top = oy + 2 + b;
        c.ellipse(cx + 0.5, top + 3.4, 6.0, 4.0, hair);                             // hair, tied up
        c.ellipse(cx + 0.5, top + 6.4, 4.7, 3.7, SKIN);
        c.rect(cx - 5, top + 4, 11, 2, rgb(226, 60, 66)); c.rect(cx + 5, top + 4, 3, 1, rgb(226, 60, 66));   // headband
        c.disc(cx + 0.5, top - 1, 2.2, hair);                                       // top-knot
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);
        c.set(cx - 4, top + 8, BLUSH); c.set(cx + 4, top + 8, BLUSH);
        c.rect(cx - 1, top + 9, 3, 1, SKIN_D);
    }

    private static void wizard(PixelCanvas c, int ox, int oy, int b) {
        int robe = rgb(126, 78, 200), robeD = rgb(84, 48, 150), robeL = rgb(176, 130, 240);
        c.poly(new int[]{ox + 6, ox + 15, ox + 18, ox + 3}, new int[]{oy + 12 + b, oy + 12 + b, oy + 26 + b, oy + 26 + b}, robe);
        c.rect(ox + 6, oy + 12 + b, 9, 1, robeL);
        c.rect(ox + 5, oy + 17 + b, 11, 1, GOLD);
        c.rect(ox + 3, oy + 13 + b, 3, 6, robeD); c.rect(ox + 15, oy + 13 + b, 3, 6, robeD);
        c.rect(ox + 3, oy + 19 + b, 3, 2, SKIN); c.rect(ox + 15, oy + 19 + b, 3, 2, SKIN);
        // staff with a glowing orb
        c.rect(ox + 19, oy + 3 + b, 1, 21, WOOD);
        c.disc(ox + 19.5, oy + 1.5 + b, 2.6, rgb(210, 176, 255)); c.set(ox + 18, oy + 0 + b, WHITE);
        npcHead(c, ox, oy, robe, b);
        int cx = ox + 10, top = oy + 2 + b;
        // long white beard
        c.ellipseY(cx + 0.5, top + 9, 4.4, 5.0, rgb(240, 240, 250), top + 8, top + 15);
        c.rect(cx - 2, top + 8, 5, 1, rgb(240, 240, 250));
        // the hat: a wide brim and a tall crooked cone with a star
        c.rect(cx - 7, top + 3, 15, 2, robeD);
        c.rect(cx - 5, top + 2, 11, 1, robe);
        c.poly(new int[]{cx - 4, cx + 6, cx + 4, cx + 1}, new int[]{top + 3, top + 3, top - 5, top - 9}, robe);
        c.poly(new int[]{cx + 1, cx + 4, cx + 6}, new int[]{top - 9, top - 5, top - 10}, robe);
        c.rect(cx - 4, top + 1, 10, 1, GOLD);
        c.set(cx + 1, top - 3, GOLD); c.set(cx, top - 4, GOLD); c.set(cx + 2, top - 4, GOLD); c.set(cx + 1, top - 5, GOLD);
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);
    }

    private static void survivalist(PixelCanvas c, int ox, int oy, int b) {
        int shirt = rgb(70, 160, 130), shirtD = rgb(40, 104, 88), shirtL = rgb(128, 212, 176);
        int hat = rgb(176, 130, 76), hatD = rgb(120, 84, 48);
        // a big backpack peeking out behind the shoulders
        c.rect(ox + 1, oy + 9 + b, 6, 10, rgb(196, 110, 52)); c.rect(ox + 1, oy + 9 + b, 6, 2, rgb(232, 154, 86));
        c.rect(ox + 14, oy + 9 + b, 6, 10, rgb(196, 110, 52)); c.rect(ox + 14, oy + 9 + b, 6, 2, rgb(232, 154, 86));
        c.rect(ox + 0, oy + 8 + b, 21, 3, rgb(236, 226, 196));                      // rolled-up bedroll
        c.rect(ox + 0, oy + 8 + b, 21, 1, WHITE);
        c.rect(ox + 6, oy + 12 + b, 9, 12, shirt);
        c.rect(ox + 6, oy + 12 + b, 9, 1, shirtL);
        c.rect(ox + 6, oy + 17 + b, 9, 1, hatD);
        c.rect(ox + 4, oy + 13 + b, 3, 6, shirtD); c.rect(ox + 14, oy + 13 + b, 3, 6, shirtD);
        c.rect(ox + 4, oy + 19 + b, 3, 2, SKIN); c.rect(ox + 14, oy + 19 + b, 3, 2, SKIN);
        c.rect(ox + 8, oy + 12 + b, 5, 2, rgb(226, 90, 70));                        // red scarf
        npcHead(c, ox, oy, hat, b);
        int cx = ox + 10, top = oy + 2 + b;
        c.rect(cx - 6, top + 6, 1, 3, rgb(90, 60, 40)); c.rect(cx + 6, top + 6, 1, 3, rgb(90, 60, 40));  // sideburns
        c.ellipseY(cx + 0.5, top + 3, 6.6, 4.0, hat, top - 1, top + 4);              // wide-brimmed hat
        c.rect(cx - 8, top + 3, 17, 2, hat); c.rect(cx - 8, top + 4, 17, 1, hatD);
        c.rect(cx - 5, top + 1, 11, 1, rgb(226, 90, 70));
        c.rect(cx - 3, top + 6, 1, 2, EYE); c.rect(cx + 3, top + 6, 1, 2, EYE);
        c.set(cx - 3, top + 6, WHITE); c.set(cx + 3, top + 6, WHITE);
    }
}
