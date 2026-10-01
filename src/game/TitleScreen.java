package game;

import static game.MenuStyle.DIM;
import static game.MenuStyle.DOT;
import static game.MenuStyle.GOLD;
import static game.MenuStyle.TEXT;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The main menu, on the title backdrop ({@link World#showTitle}: the hero in a forest clearing with the horde
 * circling). The scene is graded to dusk with a pool of warm light on the hero and fireflies drifting up; the left side
 * darkens to hold the lettering and the rows (see {@link MenuStyle} for the shared look), and a card in the corner
 * shows your saved game. The Armory uses the same backdrop.
 */
final class TitleScreen {
    private final MenuStyle.Glide mainGlide = new MenuStyle.Glide();
    private BufferedImage lighting;
    private int lightX, lightY;

    // ------------------------------------------------------------------ the backdrop's lighting

    /**
     * Grades the scene to dusk: everything darkened and cooled, a pool of warm light where the hero stands, the left
     * side sunk into shadow for the menu, and fireflies. Painted once (it never changes) and stamped each frame.
     */
    void drawBackdrop(Graphics2D g, World w, int width, int height) {
        Util.Vec hero = Renderer.toScreen(w, w.player.x, w.player.y, width, height);
        int hx = (int) Math.round(hero.x()), hy = (int) Math.round(hero.y() - 20 * Renderer.zoom(w));
        if (lighting == null || lighting.getWidth() != width || lighting.getHeight() != height || lightX != hx || lightY != hy) {
            lighting = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = lighting.createGraphics();
            paintLighting(lg, hx, hy, width, height);
            lg.dispose();
            lightX = hx;
            lightY = hy;
        }
        g.drawImage(lighting, 0, 0, null);
        fireflies(g, w.time, width, height);
    }

    private static void paintLighting(Graphics2D g, double hx, double hy, int width, int height) {
        g.setColor(new Color(14, 16, 48, 120));                                        // dusk
        g.fillRect(0, 0, width, height);
        float reach = (float) (Math.max(width, height) * 0.8);
        g.setPaint(new RadialGradientPaint((float) hx, (float) (hy - 20), reach, new float[]{0f, 0.18f, 0.5f, 1f},
            new Color[]{new Color(0, 0, 0, 0), new Color(4, 4, 16, 30), new Color(4, 4, 18, 140), new Color(2, 2, 10, 215)}));
        g.fillRect(0, 0, width, height);
        Composite saved = g.getComposite();                                            // warm light on the hero
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.setPaint(new RadialGradientPaint((float) hx, (float) hy, 260f, new float[]{0f, 1f},
            new Color[]{new Color(255, 190, 110, 90), new Color(255, 170, 80, 0)}));
        g.fill(new Ellipse2D.Double(hx - 260, hy - 260, 520, 520));
        g.setComposite(saved);
        float side = (float) (width * 0.62);                                           // the menu's side, sunk into shadow
        g.setPaint(new LinearGradientPaint(0, 0, side, 0, new float[]{0f, 0.5f, 1f},
            new Color[]{new Color(6, 6, 16, 240), new Color(6, 6, 16, 185), new Color(6, 6, 16, 0)}));
        g.fillRect(0, 0, (int) side + 1, height);
    }

    /** Small lights drifting up and twinkling, the same ones every time (placed by index, moved by the clock). */
    private static void fireflies(Graphics2D g, double t, int width, int height) {
        for (int i = 0; i < 44; i++) {
            double fx = frac(Math.sin(i * 12.9898) * 43758.5453), fy = frac(Math.sin(i * 78.233) * 12345.678), fz = frac(Math.sin(i * 3.17) * 999.13);
            double x = fx * width + Math.sin(t * (0.3 + fy * 0.5) + i) * 28;
            double y = height + 30 - ((t * (10 + fy * 22) + fz * (height + 60)) % (height + 60));
            double a = 0.25 + 0.75 * Math.abs(Math.sin(t * (0.8 + fy * 1.7) + i * 1.3));
            Color c = i % 5 == 0 ? new Color(170, 255, 190) : new Color(255, 214, 130);
            double halo = 5 + fz * 4;
            g.setColor(Util.alpha(c, 0.13 * a));
            g.fill(new Ellipse2D.Double(x - halo, y - halo, halo * 2, halo * 2));
            g.setColor(Util.alpha(c, 0.9 * a));
            g.fill(new Ellipse2D.Double(x - 1.4, y - 1.4, 2.8, 2.8));
        }
    }

    private static double frac(double v) { return v - Math.floor(v); }

    /** A page's heading: gold lettering, spaced capitals between rules under it, and an italic line under that. Returns where the rows start. */
    private static double title(Graphics2D g, String heading, double size, String sub, String tagline, double x0, int height, double s, double t, boolean glint) {
        double base = height * 0.2 + 30 * s;
        Shape shape = MenuStyle.heading(g, heading, x0, base, size * s, s);
        if (glint) MenuStyle.glint(g, shape, t, 5.5);
        Rectangle2D b = shape.getBounds2D();
        double sy = base + (size >= 80 ? 40 : 34) * s;
        // the lines under the heading are centred on it, unless one is wider than it: then they all start at its left edge
        g.setFont(MenuStyle.serif(Font.BOLD, 21 * s, 0.62));
        double subW = g.getFontMetrics().stringWidth(sub);
        g.setFont(MenuStyle.serif(Font.ITALIC, 17 * s, 0));
        FontMetrics tm = g.getFontMetrics();
        double tagW = tm.stringWidth(tagline);
        boolean centred = subW <= b.getWidth() && tagW <= b.getWidth() + 40 * s;
        double cx = b.getCenterX();
        if (centred) MenuStyle.ruled(g, sub, cx, sy, b.getX(), b.getMaxX(), 21 * s, s);
        else MenuStyle.ruled(g, sub, b.getX() + subW / 2, sy, b.getX(), b.getX(), 21 * s, s);        // (no room for the rules)
        g.setFont(MenuStyle.serif(Font.ITALIC, 17 * s, 0));
        MenuStyle.shadowed(g, tagline, centred ? cx - tagW / 2.0 : b.getX(), sy + 32 * s, new Color(214, 206, 228, 220));
        return base + 118 * s;
    }

    // ------------------------------------------------------------------ the main menu

    void draw(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        drawBackdrop(g, w, width, height);
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);
        double y0 = title(g, "SPELLBLADE", 92, "THE BLIGHT", "Explore the forest.  Answer the call.  Burn out the Blight.", x0, height, s, w.time, true);

        int sel = w.menuCursor;
        String[] details = new String[World.MAIN_MENU.length];
        boolean[] disabled = new boolean[World.MAIN_MENU.length];
        disabled[1] = w.savedGame == null;
        double rowH = 54 * s;
        MenuStyle.rows(g, mainGlide, World.MAIN_MENU, details, disabled, sel, sel == 0 && w.confirmNew, x0, y0, rowH, 29 * s, 470 * s, s, w.time);

        String[] info = {
            w.confirmNew ? "This throws away your saved game: its gold, gear and story. Press ENTER again to start over." : "Begin the story: a wandering swordsman, a camp in the woods, and a sickness in the trees.",
            w.savedGame != null ? "Pick up your adventure right where you saved it." : "No saved game yet. Start a new one.",
            "Equip, upgrade and salvage the gear you've found and bought.",
            "Leave the clearing. The forest will wait.",
        };
        MenuStyle.infoLine(g, info[sel], x0, y0 + World.MAIN_MENU.length * rowH + 12 * s, 380 * s, s, sel == 0 && w.confirmNew);
        if (w.savedGame != null) drawLegend(g, w, width, height, s);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"ENTER"}, {"M"}}, new String[]{"Navigate", "Select", "Mute"});
    }

    /** The corner card: the saved game's gold, what's worn (six slot icons in their rarity colours), and how far along it is. */
    private void drawLegend(Graphics2D g, World w, int width, int height, double s) {
        Profile p = w.profile;
        String line1 = w.savedGame.replace("   -   ", DOT);
        String line2 = p.runs + (p.runs == 1 ? " fight" : " fights") + DOT + p.victories + " won" + DOT + p.items.size() + " items";
        g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
        FontMetrics lm = g.getFontMetrics();
        double cw = Math.max(318 * s, Math.max(lm.stringWidth(line1), lm.stringWidth(line2)) + 38 * s), ch = 144 * s;   // wide enough for its text
        double x = width - cw - 28 * s, y = height - ch - 26 * s;
        MenuStyle.card(g, x, y, cw, ch, null);

        double px = x + 18 * s, py = y + 26 * s;
        MenuStyle.label(g, "YOUR ADVENTURE", px, py, s, new Color(255, 214, 120, 220));
        Art.frame("run.coin", w.time, 6).draw(g, x + cw - 92 * s, py + 1 * s, 2.5 * s, false);
        g.setFont(MenuStyle.sans(Font.BOLD, 15 * s));
        MenuStyle.shadowed(g, String.valueOf(p.gold), x + cw - 78 * s, py + 1 * s, GOLD);

        double slot = 38 * s, sx = px, sy = py + 14 * s;                                 // what's worn
        for (Item.Slot sl : Item.Slot.values()) {
            slotBox(g, p.equipped.get(sl), sl, sx, sy, slot, s);
            sx += slot + 9 * s;
        }
        g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
        double ly = sy + slot + 24 * s;
        MenuStyle.shadowed(g, line1, px, ly, new Color(210, 205, 222));
        MenuStyle.shadowed(g, line2, px, ly + 20 * s, DIM);
    }

    /** A square holding an equipment slot's icon, framed in the worn item's rarity colour (dim when empty). */
    static void slotBox(Graphics2D g, Item it, Item.Slot sl, double x, double y, double size, double s) {
        RoundRectangle2D box = new RoundRectangle2D.Double(x, y, size, size, 8, 8);
        g.setColor(new Color(24, 24, 38, 230));
        g.fill(box);
        g.setColor(it == null ? new Color(70, 70, 90) : it.rarity.color);
        g.setStroke(new java.awt.BasicStroke(it == null ? 1f : 1.8f));
        g.draw(box);
        Art.frames("item." + sl.name().toLowerCase())[0].draw(g, x + size / 2, y + size / 2, 2 * size / 38, false, 0, it == null ? 0.22f : 1f);
    }
}
