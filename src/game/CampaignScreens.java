package game;

import static game.MenuStyle.DIM;
import static game.MenuStyle.DOT;
import static game.MenuStyle.GOLD;
import static game.MenuStyle.TEXT;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

/**
 * The Classic Campaign's pause menu, "you died" screen and trainers' shops, in the same look as a run's screens
 * ({@link MenuStyle}): drawn over the darkened level. The pause menu keeps the campaign's own rows (the two volumes)
 * with a card of your spells and stats beside them; the death screen names where you fell and how far you got; a
 * trainer's shop lists what they teach beside a card with the trainer and the selected upgrade.
 */
final class CampaignScreens {
    private final MenuStyle.Glide pauseGlide = new MenuStyle.Glide(), shopGlide = new MenuStyle.Glide();
    private static final String[] PAUSE_ROWS = {"MUSIC", "EFFECTS"};

    /** The campaign's pause: where you are, the volume sliders, and your hero's spells and stats. ESC resumes. */
    void drawPause(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 165);
        g.setPaint(new GradientPaint(0, 0, new Color(6, 6, 16, 150), (float) (width * 0.55), 0, new Color(6, 6, 16, 0)));
        g.fillRect(0, 0, (int) (width * 0.55) + 1, height);
        Player p = w.player;
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);

        double base = height * 0.2 + 12 * s;
        Rectangle2D hb = MenuStyle.heading(g, "PAUSED", x0, base, 76 * s, s).getBounds2D();
        String where = w.tutorial != null ? "THE OPENING STORY" : w.level.town ? "TRANSIT TOWN" : w.level.name.toUpperCase();
        MenuStyle.ruled(g, where, hb.getCenterX(), base + 36 * s, hb.getX(), hb.getMaxX(), 17 * s, s);
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        String status = w.tutorial != null ? "Your lessons will wait for you." : w.level.theme.title + DOT + "character level " + p.level
            + (p.skillPoints > 0 ? DOT + p.skillPoints + " skill point" + (p.skillPoints == 1 ? "" : "s") + " to spend" : "");
        MenuStyle.shadowed(g, status, x0, base + 66 * s, new Color(214, 206, 228, 220));

        double y0 = base + 128 * s, rowH = 52 * s, rowSize = 27 * s;
        int sel = Math.min(w.pauseCursor, 1);
        MenuStyle.rows(g, pauseGlide, PAUSE_ROWS, null, null, sel, false, x0, y0, rowH, rowSize, 470 * s, s, w.time);
        g.setFont(MenuStyle.serif(Font.BOLD, rowSize, 0.08));
        FontMetrics fm = g.getFontMetrics();
        int[] volumes = {w.audio.music, w.audio.sfx};
        for (int i = 0; i < 2; i++) {
            double lx = x0 + pauseGlide.slide(i) + fm.stringWidth(PAUSE_ROWS[i]) + 22 * s, ly = y0 + i * rowH + rowH * 0.18 - 9 * s;
            MenuStyle.slider(g, lx, ly, volumes[i], sel == i, s);
        }
        String info = w.audio.muted ? "Sound is off: M turns it back on." : sel == 0 ? "The music's volume. LEFT and RIGHT change it." : "The volume of the sound effects. LEFT and RIGHT change it.";
        MenuStyle.infoLine(g, info, x0, y0 + PAUSE_ROWS.length * rowH + 12 * s, 380 * s, s, false);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"A", "D"}, {"M"}, {"ESC"}}, new String[]{"Choose", "Volume", "Mute", "Resume"});
        drawHeroCard(g, p, width, height, s);
    }

    /** The right-hand card: each spell (learned or not, its cost and cooldown), then the hero's stats. */
    private void drawHeroCard(Graphics2D g, Player p, int width, int height, double s) {
        double cw = 412 * s, ch = 304 * s, x = width - cw - 40 * s, y = (height - ch) / 2 + 20 * s;
        MenuStyle.card(g, x, y, cw, ch, null);
        double px = x + 20 * s, right = x + cw - 20 * s, py = y + 28 * s;
        MenuStyle.label(g, "YOUR MAGIC", px, py, s, GOLD);
        double ly = py + 28 * s;
        for (Ability a : Ability.values()) {
            int lv = p.spellLevel[a.ordinal()];
            MenuStyle.diamond(g, px + 4 * s, ly - 5 * s, 4 * s, lv > 0 ? a.color : new Color(70, 68, 86));
            g.setFont(MenuStyle.sans(Font.BOLD, 14 * s));
            MenuStyle.shadowed(g, a.label, px + 16 * s, ly, lv > 0 ? a.color : new Color(120, 118, 136));
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            FontMetrics fm = g.getFontMetrics();
            String detail = lv > 0 ? "Lv " + lv + DOT + (int) a.cost(lv) + " MP" + DOT + String.format(Locale.ROOT, "%.1fs", a.cooldown * p.cooldownMult)
                : "the Wizard teaches it";
            MenuStyle.shadowed(g, detail, right - fm.stringWidth(detail), ly, lv > 0 ? TEXT : DIM);
            ly += 24 * s;
        }
        g.setFont(MenuStyle.serif(Font.ITALIC, 13 * s, 0));
        MenuStyle.shadowed(g, "Magic: RIGHT opens the list, or hold SHIFT to keep it open.", px, ly + 2 * s, DIM);

        double ty = ly + 40 * s;
        g.setColor(new Color(255, 214, 120, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(px, ty - 22 * s, right, ty - 22 * s));
        MenuStyle.label(g, "YOUR HERO", px, ty - 2 * s, s, GOLD);
        String[][] stats = {
            {"Combo", p.comboMax + " hits"}, {"Melee damage", String.valueOf(Math.round(p.meleeDamage * p.meleeMult))},
            {"Attack speed", String.format(Locale.ROOT, "x%.2f", p.attackSpeed)}, {"Spell power", String.format(Locale.ROOT, "x%.2f", p.spellPower)},
            {"Move speed", String.valueOf(Math.round(p.moveSpeed))}, {"Roll cooldown", String.format(Locale.ROOT, "%.2fs", p.dodgeCooldown)},
            {"Roll distance", Math.round(p.rollDistance()) + " px"}, {"Max HP / MP", Math.round(p.maxHp) + " / " + Math.round(p.maxMp)},
        };
        double col = (cw - 40 * s) / 2, sy = ty + 22 * s;
        for (int i = 0; i < stats.length; i++) {
            double lx = px + (i % 2) * col, row = sy + (i / 2) * 22 * s;
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.shadowed(g, stats[i][0], lx, row, DIM);
            g.setFont(MenuStyle.sans(Font.BOLD, 13 * s));
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, stats[i][1], lx + col - 14 * s - fm.stringWidth(stats[i][1]), row, TEXT);
        }
    }

    /** "YOU DIED" in blood red, where it happened, how far you'd got, and R to try again (once the screen has settled). */
    void drawGameOver(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 185);
        double s = MenuStyle.scale(height), cx = width / 2.0;
        boolean story = w.tutorial != null;
        double base = height * 0.32;
        Rectangle2D hb = MenuStyle.headingCentred(g, "YOU  DIED", cx, base, 84 * s, s, true).getBounds2D();   // (one space vanishes at this size)
        String where = story ? "ONLY THE TUTORIAL" : w.level.town ? "TRANSIT TOWN" : w.level.name.toUpperCase() + DOT + w.level.theme.title.toUpperCase();
        MenuStyle.ruled(g, where, cx, base + 40 * s, hb.getX() - 90 * s, hb.getMaxX() + 90 * s, 15 * s, s);
        g.setFont(MenuStyle.serif(Font.ITALIC, 17 * s, 0));
        MenuStyle.centred(g, story ? "Don't worry. The story starts over from the beginning." : "Your journey starts again from the first level, with a fresh hero.",
            cx, base + 70 * s, new Color(214, 206, 228, 225));

        double ty = base + 106 * s;
        if (!story) {
            String[][] tiles = {
                {w.level.clearedRoomCount() + " / " + w.level.combatRoomCount(), "ROOMS"}, {String.valueOf(w.player.level), "LEVEL"},
                {String.valueOf(w.kills), "KILLS"}, {Run.clock(w.time), "TIME"},
            };
            double tw = 124 * s, tx0 = cx - tiles.length * tw / 2;
            for (int i = 0; i < tiles.length; i++) {
                double tcx = tx0 + i * tw + tw / 2;
                g.setFont(MenuStyle.serif(Font.BOLD, 30 * s, 0.02));
                MenuStyle.centred(g, tiles[i][0], tcx, ty + 28 * s, Color.WHITE);
                g.setFont(MenuStyle.caps(10 * s));
                MenuStyle.centred(g, tiles[i][1], tcx, ty + 48 * s, DIM);
                if (i > 0) {
                    g.setColor(new Color(255, 214, 120, 70));
                    g.fill(new Rectangle2D.Double(tx0 + i * tw, ty + 6 * s, 1, 44 * s));
                }
            }
            ty += 70 * s;
        }
        String[][] groups = {{"R"}};
        String[] labels = {story ? "Start the story again" : "Try again"};
        Composite saved = g.getComposite();
        if (w.overTimer > 0) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        MenuStyle.keys(g, cx - MenuStyle.keysWidth(g, s, groups, labels) / 2, ty + 34 * s, s, groups, labels);
        g.setComposite(saved);
    }

    // ------------------------------------------------------------------ a trainer's shop

    /**
     * A trainer's shop: their name as the heading, your skill points in the corner, their upgrades in a card on the
     * left (each with its cost, or MAX), and on the right the trainer themselves above the selected upgrade's details
     * and what ENTER would do. Input is {@link World}'s {@code updateStation}.
     */
    void drawTrainer(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 175);
        Player p = w.player;
        Level.Station station = w.activeStation;
        Color accent = station.category().color;
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);

        double base = Math.max(96 * s, height * 0.5 - 250 * s);                    // the whole shop sits centred on tall windows
        Rectangle2D hb = MenuStyle.heading(g, station.name(), x0, base, 58 * s, s).getBounds2D();
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        MenuStyle.shadowed(g, station.flavor(), hb.getMaxX() + 28 * s, base - 22 * s, new Color(214, 206, 228, 225));
        MenuStyle.label(g, station.category().label + " TRAINER", hb.getMaxX() + 28 * s, base - 2 * s, s, Util.alpha(accent, 0.95));

        g.setFont(MenuStyle.serif(Font.BOLD, 34 * s, 0.02));                     // the skill points you have to spend
        FontMetrics pm = g.getFontMetrics();
        String points = String.valueOf(p.skillPoints);
        double pr = width - 44 * s;
        MenuStyle.shadowed(g, points, pr - pm.stringWidth(points), base - 6 * s, p.skillPoints > 0 ? GOLD : DIM);
        g.setFont(MenuStyle.caps(10 * s));
        String spLabel = "SKILL POINTS";
        MenuStyle.shadowed(g, spLabel, pr - g.getFontMetrics().stringWidth(spLabel), base + 12 * s, DIM);

        java.util.List<Upgrade> items = w.stationItems();
        int sel = items.isEmpty() ? 0 : Math.min(w.stationCursor, items.size() - 1);
        double top = base + 42 * s, bottom = Math.min(height - 62 * s, top + 420 * s), left = 40 * s, total = width - 80 * s, gap = 18 * s;
        double lw = total * 0.56, rw = total - lw - gap;

        // the upgrades
        MenuStyle.card(g, left, top, lw, bottom - top, null);
        MenuStyle.label(g, "WHAT " + firstWord(station.name()) + " TEACHES", left + 20 * s, top + 28 * s, s, GOLD);
        double rowH = Math.min(58 * s, (bottom - top - 60 * s) / Math.max(1, items.size())), listTop = top + 44 * s;
        double barY = shopGlide.step(sel, items.size(), listTop + sel * rowH);
        g.setPaint(new GradientPaint((float) (left + 8 * s), 0, new Color(255, 180, 70, 90), (float) (left + lw - 8 * s), 0, new Color(255, 180, 70, 8)));
        g.fill(new Rectangle2D.Double(left + 8 * s, barY, lw - 16 * s, rowH - 6 * s));
        g.setColor(MenuStyle.GOLD_DEEP);
        g.fill(new Rectangle2D.Double(left + 8 * s, barY, 3 * s, rowH - 6 * s));
        for (int i = 0; i < items.size(); i++) {
            Upgrade u = items.get(i);
            double ry = listTop + i * rowH, tx = left + 40 * s + shopGlide.slide(i) * 0.5;
            boolean affordable = !u.maxed() && p.skillPoints >= u.cost();
            MenuStyle.diamond(g, left + 24 * s, ry + rowH * 0.36, 5 * s, u.maxed() ? new Color(90, 88, 104) : u.accent());
            g.setFont(MenuStyle.sans(Font.BOLD, 16 * s));
            MenuStyle.shadowed(g, u.title(), tx, ry + rowH * 0.42, u.maxed() ? new Color(150, 148, 165) : i == sel ? MenuStyle.ROW_SELECTED : Color.WHITE);
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            MenuStyle.shadowed(g, u.status(), tx, ry + rowH * 0.42 + 17 * s, DIM);
            String cost = u.maxed() ? "MAX" : u.cost() + " SP";
            g.setFont(MenuStyle.serif(Font.BOLD, 18 * s, 0.04));
            FontMetrics cm = g.getFontMetrics();
            MenuStyle.shadowed(g, cost, left + lw - 22 * s - cm.stringWidth(cost), ry + rowH * 0.5,
                u.maxed() ? new Color(130, 128, 146) : affordable ? GOLD : new Color(220, 120, 120));
        }

        // the trainer, and the selected upgrade
        double rx = left + lw + gap;
        MenuStyle.card(g, rx, top, rw, bottom - top, accent);
        String key = switch (station.category()) { case COMBAT -> "combat"; case SPELLS -> "spells"; case SURVIVAL -> "survival"; };
        double feet = top + 156 * s, cx = rx + rw / 2;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new java.awt.geom.Ellipse2D.Double(cx - 58 * s, feet - 9 * s, 116 * s, 18 * s));
        Art.frame("shop." + key, w.time, 1.6).draw(g, cx, feet, 3 * s, false);
        if (items.isEmpty()) return;
        Upgrade u = items.get(sel);
        g.setFont(MenuStyle.serif(Font.BOLD, 24 * s, 0.03));
        MenuStyle.centred(g, u.title(), cx, feet + 42 * s, u.maxed() ? new Color(170, 168, 185) : u.accent());
        g.setFont(MenuStyle.caps(10 * s));
        MenuStyle.centred(g, u.status().toUpperCase(), cx, feet + 62 * s, DIM);
        g.setFont(MenuStyle.sans(Font.PLAIN, 14 * s));
        MenuStyle.wrap(g, u.description(), rx + 26 * s, feet + 94 * s, rw - 52 * s, 20 * s, TEXT);

        double ay = bottom - 30 * s;
        if (u.maxed()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
            MenuStyle.shadowed(g, "Fully upgraded. Nothing more to learn here.", rx + 26 * s, ay, DIM);
        } else {
            boolean affordable = p.skillPoints >= u.cost();
            MenuStyle.keyCap(g, "ENTER", rx + 26 * s, ay, s);
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.shadowed(g, affordable ? "Learn it" + DOT + u.cost() + " skill point" + (u.cost() == 1 ? "" : "s")
                : "Not enough skill points" + DOT + "it costs " + u.cost(), rx + 92 * s, ay - 1 * s, affordable ? GOLD : new Color(220, 120, 120));
        }

        double keysY = bottom + 36 * s;
        MenuStyle.keys(g, x0, keysY, s, new String[][]{{"W", "S"}, {"ENTER"}, {"ESC"}}, new String[]{"Choose", "Learn", "Leave"});
        if (!w.stationMessage.isEmpty()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
            FontMetrics fm = g.getFontMetrics();
            boolean bad = w.stationMessage.startsWith("Not") || w.stationMessage.contains("already");
            MenuStyle.shadowed(g, w.stationMessage, width - 44 * s - fm.stringWidth(w.stationMessage), keysY, bad ? MenuStyle.WARN : GOLD);
        }
    }

    private static String firstWord(String name) {
        int sp = name.indexOf(' ');
        return "THE " + (sp < 0 ? name : name.substring(0, sp));
    }
}
