package game;

import static game.PixelCanvas.*;

import java.util.HashMap;
import java.util.Map;

/**
 * The hero's swords, drawn apart from the hero so any sword can go in his hand. A sword is described by its parts
 * ({@link Kind}: the blade's length, width and curve, the guard, the grip, the pommel, their materials, a glow) and
 * painted on demand at whatever angle a pose holds it, with crisp pixel lines (rotating a finished picture would blur
 * its pixels), shaded and outlined like everything else ({@link Doll}). Each picture is anchored at the middle of the
 * grip, where the hand closes round it, and kept once painted.
 *
 * <p>Which sword the hero carries follows the weapon he wears ({@link #forItem}): its kind of blade (sword, blade,
 * saber, longsword) gives the shape, its rarity the metal.
 */
final class SwordArt {
    private SwordArt() {}

    /** A sword's look. Lengths are in the drawn art's pixels; {@code glow} (0 for none) is the colour of runes down the blade. */
    record Kind(String id, double length, int width, double curve, double guard, Doll.Mat blade, Doll.Mat hilt, Doll.Mat grip, Doll.Mat pommel, int glow) {}

    // ------------------------------------------------------------------ metals

    private static final Doll.Mat IRON = new Doll.Mat(rgb(40, 40, 50), rgb(96, 98, 112), rgb(140, 142, 156), rgb(178, 180, 192), rgb(214, 216, 226), true);
    private static final Doll.Mat BRASS = new Doll.Mat(rgb(80, 52, 16), rgb(150, 110, 44), rgb(196, 152, 70), rgb(226, 190, 110), rgb(246, 224, 160), true);
    private static final Doll.Mat BLUE_STEEL = new Doll.Mat(rgb(30, 44, 90), rgb(96, 128, 196), rgb(150, 184, 236), rgb(200, 224, 255), rgb(250, 252, 255), true);
    private static final Doll.Mat SAPPHIRE = new Doll.Mat(rgb(14, 22, 80), rgb(36, 70, 190), rgb(70, 120, 250), rgb(140, 180, 255), rgb(220, 236, 255), true);
    private static final Doll.Mat NIGHT_STEEL = new Doll.Mat(rgb(24, 12, 44), rgb(64, 44, 110), rgb(100, 76, 160), rgb(146, 120, 206), rgb(200, 180, 246), true);
    private static final Doll.Mat SILVER = new Doll.Mat(rgb(50, 54, 72), rgb(132, 140, 164), rgb(190, 196, 214), rgb(228, 232, 244), rgb(255, 255, 255), true);
    private static final Doll.Mat SUNSTEEL = new Doll.Mat(rgb(110, 60, 10), rgb(214, 158, 60), rgb(250, 214, 120), rgb(255, 238, 186), rgb(255, 252, 236), true);
    private static final Doll.Mat RUBY = new Doll.Mat(rgb(80, 8, 20), rgb(170, 24, 44), rgb(226, 52, 66), rgb(255, 120, 120), rgb(255, 210, 200), true);

    /** The traveller's sword the hero starts with (and carries when no weapon is worn). */
    static final Kind TRAVELLER = new Kind("traveller", 13, 2, 0, 2.5, Doll.STEEL, Doll.GOLD, Doll.LEATHER, Doll.GOLD, 0);

    /** The shapes of the four kinds of weapon (see {@link Item.Slot#WEAPON}): long, wide, curved, or plain. */
    private static Kind shaped(String base, String id, Doll.Mat blade, Doll.Mat hilt, Doll.Mat grip, Doll.Mat pommel, int glow) {
        return switch (base) {
            case "Blade" -> new Kind(id, 11, 3, 0, 3, blade, hilt, grip, pommel, glow);              // short and broad
            case "Saber" -> new Kind(id, 14, 2, 1.6, 2, blade, hilt, grip, pommel, glow);            // slim and gently curved
            case "Longsword" -> new Kind(id, 17, 2, 0, 3.5, blade, hilt, grip, pommel, glow);       // long, with a wide cross-guard
            default -> new Kind(id, 13, 2, 0, 2.5, blade, hilt, grip, pommel, glow);
        };
    }

    /** The sword for a worn weapon: its base word picks the shape, its rarity the metal. Null gives the traveller's sword. */
    static Kind forItem(Item weapon) {
        if (weapon == null) return TRAVELLER;
        String base = "Sword";
        for (String b : Item.Slot.WEAPON.bases) if (weapon.name.endsWith(b)) base = b;
        String id = base + "." + weapon.rarity;
        return switch (weapon.rarity) {
            case COMMON -> shaped(base, id, IRON, IRON, Doll.LEATHER, IRON, 0);
            case UNCOMMON -> shaped(base, id, Doll.STEEL, BRASS, Doll.LEATHER, BRASS, 0);
            case RARE -> shaped(base, id, BLUE_STEEL, Doll.GOLD, Doll.LEATHER, SAPPHIRE, 0);
            case EPIC -> shaped(base, id, NIGHT_STEEL, SILVER, Doll.PANTS, SAPPHIRE, rgb(200, 150, 255));
            case LEGENDARY -> shaped(base, id, SUNSTEEL, Doll.GOLD, RUBY, RUBY, rgb(255, 240, 170));
        };
    }

    // ------------------------------------------------------------------ painting

    private static final Map<String, Sprite> CACHE = new HashMap<>();

    /**
     * The sword held at {@code angle} (radians, the way the blade points, on screen), anchored at the grip. With
     * {@code inHand} the middle of the grip is left out, so the fist it's drawn over shows through.
     */
    static synchronized Sprite sprite(Kind k, double angle, boolean inHand) {
        int deg = (int) Math.round(Math.toDegrees(angle));
        return CACHE.computeIfAbsent(k.id() + "/" + Math.floorMod(deg, 360) + "/" + inHand, key -> paint(k, Math.toRadians(deg), inHand));
    }

    private static Sprite paint(Kind k, double a, boolean inHand) {
        int r = (int) Math.ceil(k.length() + k.curve() + 8);
        int n = 2 * r + 3;
        double cx = r + 1.5, cy = r + 1.5;                                    // the grip's middle: the centre of the anchor pixel
        double dx = Math.cos(a), dy = Math.sin(a), nx = -dy, ny = dx;       // along the blade, and across it
        Doll d = new Doll(n, n);
        // pommel, grip, guard, blade, from the back
        d.piece(k.pommel()).disc(cx - dx * 3.4, cy - dy * 3.4, k.width() >= 3 ? 1.6 : 1.3);
        d.piece(k.grip());
        for (double u = -2.6; u <= 1.0; u += 0.25) {
            if (inHand && u > -1.4 && u < 0.8) continue;                      // the fist goes here
            d.set((int) Math.floor(cx + dx * u), (int) Math.floor(cy + dy * u));
        }
        d.piece(k.hilt());
        double gu = 1.7;
        d.thickLine(px(cx + dx * gu - nx * k.guard()), px(cy + dy * gu - ny * k.guard()), px(cx + dx * gu + nx * k.guard()), px(cy + dy * gu + ny * k.guard()), 2);
        d.piece(k.blade());
        double u0 = 3.0, u1 = 3.0 + k.length();
        double prevX = Double.NaN, prevY = Double.NaN;
        for (double u = u0; u <= u1; u += 0.5) {
            double t = (u - u0) / (u1 - u0), bend = k.curve() * t * t;   // a saber curves away towards the tip
            double x = cx + dx * u + nx * bend, y = cy + dy * u + ny * bend;
            int w = u > u1 - 2 ? 1 : k.width();                           // tapering to a point
            if (!Double.isNaN(prevX)) d.thickLine(px(prevX), px(prevY), px(x), px(y), w);
            prevX = x;
            prevY = y;
        }
        if (k.glow() != 0) {                                               // runes glowing down the middle of the blade
            for (double u = u0 + 2; u < u1 - 3; u += 3) {
                double t = (u - u0) / (u1 - u0), bend = k.curve() * t * t;
                d.dot(px(cx + dx * u + nx * bend), px(cy + dy * u + ny * bend), k.glow());
            }
        }
        return new Sprite(d.render().image(), (int) Math.floor(cx), (int) Math.floor(cy), ForestArt.K);
    }

    private static int px(double v) { return (int) Math.floor(v); }
}
