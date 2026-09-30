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
 * The screens that sit on the title backdrop ({@link World#showTitle}: the hero in a forest clearing with the horde
 * circling): the main menu, and the Classic Campaign's own menu and chapter list. The scene is graded to dusk with a
 * pool of warm light on the hero and fireflies drifting up; the left side darkens to hold the lettering and the rows
 * (see {@link MenuStyle} for the shared look), and a card in the corner says something useful about the selection.
 */
final class TitleScreen {
    private final MenuStyle.Glide mainGlide = new MenuStyle.Glide(), classicGlide = new MenuStyle.Glide(), chapterGlide = new MenuStyle.Glide();
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
        double y0 = title(g, "SPELLBLADE", 92, "SURVIVORS", "Hold the line.  Grow stronger.  Slay the three.", x0, height, s, w.time, true);

        int sel = w.menuCursor;
        String[] details = new String[World.MAIN_MENU.length];
        if (w.savedRun != null) details[1] = w.savedRun.replace("   -   ", DOT);                   // what CONTINUE would pick up
        boolean[] disabled = new boolean[World.MAIN_MENU.length];
        disabled[1] = w.savedRun == null;
        double rowH = 54 * s;
        MenuStyle.rows(g, mainGlide, World.MAIN_MENU, details, disabled, sel, sel == 0 && w.confirmNew, x0, y0, rowH, 29 * s, 470 * s, s, w.time);

        String[] info = {
            w.confirmNew ? "You have a run in progress. Press ENTER again to abandon it and start over." : "Begin a new run: three stages, three bosses, and only your sword to start.",
            w.savedRun != null ? "Pick up your saved run right where you left it." : "No run in progress. Start a new game to make one.",
            "Equip, upgrade and salvage the gear you've found. It comes with you into every run.",
            "The original room-by-room adventure, with its opening story.",
            "Leave the clearing. The horde will wait.",
        };
        MenuStyle.infoLine(g, info[sel], x0, y0 + World.MAIN_MENU.length * rowH + 12 * s, 380 * s, s, sel == 0 && w.confirmNew);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"ENTER"}, {"M"}}, new String[]{"Navigate", "Select", "Mute"});
        drawLegend(g, w, width, height, s);
    }

    /** The corner card: gold, items, what's worn (six slot icons in their rarity colours) and your best run. */
    private void drawLegend(Graphics2D g, World w, int width, int height, double s) {
        Profile p = w.profile;
        String line1, line2;
        if (p.runs == 0) {
            line1 = "No runs yet. The horde awaits.";
            line2 = p.items.isEmpty() ? "Gear you find will appear above." : p.items.size() + " items in your bag";
        } else {
            line1 = p.runs + (p.runs == 1 ? " run" : " runs") + DOT + p.victories + (p.victories == 1 ? " victory" : " victories")
                + DOT + p.items.size() + " items";
            line2 = "Best: stage " + Math.min(Run.STAGES, Math.max(1, p.bestStage)) + DOT + "level " + p.bestLevel + DOT
                + p.bestKills + " kills" + DOT + Run.clock(p.bestTime);
        }
        g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
        FontMetrics lm = g.getFontMetrics();
        double cw = Math.max(318 * s, Math.max(lm.stringWidth(line1), lm.stringWidth(line2)) + 38 * s), ch = 144 * s;   // wide enough for its text
        double x = width - cw - 28 * s, y = height - ch - 26 * s;
        MenuStyle.card(g, x, y, cw, ch, null);

        double px = x + 18 * s, py = y + 26 * s;
        MenuStyle.label(g, "YOUR LEGEND", px, py, s, new Color(255, 214, 120, 220));
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

    // ------------------------------------------------------------------ the Classic Campaign

    /** The campaign's own menu: PLAY (with or without the opening story) and SELECT CHAPTER, and how to play it. */
    void drawClassic(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);
        double y0 = title(g, "CLASSIC", 84, "THE ORIGINAL ADVENTURE", "Clear the rooms.  Chain combos.  Grow stronger every level.", x0, height, s, w.time, false);

        String[] labels = {"PLAY", "SELECT CHAPTER"};
        String[] details = {w.tutorialOn ? "with the opening story" : "straight to level 1", "any level" + DOT + "character level " + World.CHAPTER_SELECT_LEVEL};
        double rowH = 54 * s;
        int sel = Math.min(w.menuCursor, 1);
        MenuStyle.rows(g, classicGlide, labels, details, null, sel, false, x0, y0, rowH, 29 * s, 470 * s, s, w.time);
        String[] info = {
            w.tutorialOn ? "Wake in a forest clearing, with a talkative squirrel to remind you how to fight." : "Skip the opening story and start in the hub of the Whispering Forest.",
            "Jump into any level with a character already at level " + World.CHAPTER_SELECT_LEVEL + " and skill points to spend.",
        };
        MenuStyle.infoLine(g, info[sel], x0, y0 + labels.length * rowH + 12 * s, 380 * s, s, false);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"ENTER"}, {"T"}, {"ESC"}},
            new String[]{"Navigate", "Select", "Opening story " + (w.tutorialOn ? "on" : "off"), "Back"});

        // how the campaign plays: its controls differ from a run's
        String[][] controls = {{"W A S D", "Move"}, {"ENTER", "Attack, and confirm"}, {"SPACE", "Roll"}, {"SHIFT", "Quick-cast magic"},
            {"TAB", "Lock on to an enemy"}, {"E", "Talk to trainers"}};
        double cw = 318 * s, ch = 44 * s + controls.length * 29 * s, x = width - cw - 28 * s, y = height - ch - 26 * s;
        MenuStyle.card(g, x, y, cw, ch, null);
        MenuStyle.label(g, "HOW IT PLAYS", x + 18 * s, y + 26 * s, s, new Color(255, 214, 120, 220));
        double ry = y + 58 * s;
        for (String[] c : controls) {
            MenuStyle.keyCap(g, c[0], x + 18 * s, ry, s);
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.shadowed(g, c[1], x + 118 * s, ry - 1 * s, TEXT);
            ry += 29 * s;
        }
    }

    /** Roman numerals for the chapter rows. */
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};

    /** The chapter list: one row per level and the prototype, with the selected level's boss waiting in the corner card. */
    void drawChapters(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);
        double y0 = title(g, "CHAPTERS", 84, "CHOOSE YOUR BATTLE", "Every level's hub, a hero at level " + World.CHAPTER_SELECT_LEVEL + ", skill points unspent.",
            x0, height, s, w.time, false);

        int n = Level.COUNT + 1, sel = Math.min(w.chapterCursor, n - 1);
        String[] labels = new String[n], details = new String[n];
        for (int i = 0; i < Level.COUNT; i++) {
            String name = Level.THEMES[i].title.replaceFirst("^The ", "").toUpperCase();
            labels[i] = NUMERALS[i] + "    " + name;
        }
        labels[Level.COUNT] = "PROTOTYPE";
        details[Level.COUNT] = "work in progress";
        double rowH = 50 * s;
        MenuStyle.rows(g, chapterGlide, labels, details, null, sel, false, x0, y0, rowH, 25 * s, 560 * s, s, w.time);
        String info = sel < Level.COUNT ? "Start in its hub. Clear the rooms, then face the " + titleCase(Level.BOSS_NAMES[sel]) + "."
            : "A hand-sketched layout being tried out. No enemies, no boss: just walk around.";
        MenuStyle.infoLine(g, info, x0, y0 + n * rowH + 12 * s, 380 * s, s, false);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"ENTER"}, {"ESC"}}, new String[]{"Navigate", "Start", "Back"});

        // the corner card: who waits at the end of the selected level
        double cw = 318 * s, ch = 250 * s, x = width - cw - 28 * s, y = height - ch - 26 * s;
        boolean proto = sel >= Level.COUNT;
        Color tint = proto ? new Color(120, 200, 235) : switch (Level.THEMES[sel]) { case FOREST -> new Color(120, 220, 110); case CITY -> new Color(120, 150, 255); case LAB -> new Color(150, 255, 170); };
        MenuStyle.card(g, x, y, cw, ch, tint);
        MenuStyle.label(g, proto ? "PROTOTYPE" : "CHAPTER " + NUMERALS[sel] + DOT + "THE BOSS", x + 18 * s, y + 26 * s, s, new Color(255, 214, 120, 220));
        String sprite = proto ? "town.child.idle" : Level.THEMES[sel].key + ".boss.idle";
        Sprite boss = Art.frame(sprite, w.time, proto ? 1.8 : 2.5);
        double feet = y + ch - 60 * s, room = feet - (y + 40 * s);                        // between the label and the name
        double spriteScale = Math.min(proto ? 4 * s : 3 * s, room / Math.max(1, boss.ay));
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(x + cw / 2 - 60 * s, feet - 10 * s, 120 * s, 20 * s));
        boss.draw(g, x + cw / 2, feet, spriteScale, false);
        g.setFont(MenuStyle.serif(Font.BOLD, 22 * s, 0.08));
        String name = proto ? "A SKETCH" : Level.BOSS_NAMES[sel];
        FontMetrics fm = g.getFontMetrics();
        MenuStyle.shadowed(g, name, x + (cw - fm.stringWidth(name)) / 2, y + ch - 28 * s, GOLD);
        g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
        fm = g.getFontMetrics();
        String sub = proto ? "no enemies here" : Level.THEMES[sel].title;
        MenuStyle.shadowed(g, sub, x + (cw - fm.stringWidth(sub)) / 2, y + ch - 11 * s, DIM);
    }

    private static String titleCase(String s) {
        StringBuilder out = new StringBuilder();
        for (String word : s.toLowerCase().split(" ")) {
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
