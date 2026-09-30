package game;

import static game.MenuStyle.DIM;
import static game.MenuStyle.DOT;
import static game.MenuStyle.GOLD;
import static game.MenuStyle.TEXT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Armory, in the title screens' style ({@link MenuStyle}), over the same backdrop: a gold heading, then three glass
 * cards — what you're wearing (and what it all adds up to), your bag (the selection glides from row to row), and the
 * selected item up close with what ENTER, U and X would do to it. Input is {@link World}'s {@code updateArmory}.
 */
final class ArmoryScreen {
    private static final Color GREEN = new Color(140, 240, 160);
    private static final Color ORANGE = new Color(255, 180, 90);
    private final MenuStyle.Glide glide = new MenuStyle.Glide();

    void draw(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        g.setColor(new Color(4, 4, 12, 120));                                          // quieter than the main menu: this is a screen to read
        g.fillRect(0, 0, width, height);
        Profile prof = w.profile;
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);

        // heading, and the gold you have to spend
        double base = 86 * s;
        Shape heading = MenuStyle.heading(g, "ARMORY", x0, base, 54 * s, s);
        MenuStyle.label(g, "WHAT YOU WEAR GOES WITH YOU INTO EVERY RUN", heading.getBounds2D().getMaxX() + 28 * s, base - 8 * s, s, DIM);
        g.setFont(MenuStyle.serif(Font.BOLD, 28 * s, 0.04));
        FontMetrics gm = g.getFontMetrics();
        String gold = String.valueOf(prof.gold);
        double gx = width - 44 * s - gm.stringWidth(gold);
        MenuStyle.shadowed(g, gold, gx, base - 4 * s, GOLD);
        Art.frame("run.coin", w.time, 6).draw(g, gx - 22 * s, base - 4 * s, 3.2 * s, false);

        // three cards
        double top = base + 34 * s, bottom = height - 62 * s, gap = 18 * s;
        double left = 40 * s, total = width - 80 * s;
        double c1 = total * 0.27, c2 = total * 0.36, c3 = total - c1 - c2 - gap * 2;
        List<Item> bag = prof.sorted();
        int sel = bag.isEmpty() ? -1 : Math.min(w.armoryCursor, bag.size() - 1);
        drawWorn(g, prof, left, top, c1, bottom - top, s);
        drawBag(g, w, prof, bag, sel, left + c1 + gap, top, c2, bottom - top, s);
        drawDetails(g, w, prof, sel < 0 ? null : bag.get(sel), left + c1 + c2 + gap * 2, top, c3, bottom - top, s);

        MenuStyle.keys(g, x0, height - 26 * s, s, new String[][]{{"W", "S"}, {"ENTER"}, {"U"}, {"X"}, {"ESC"}},
            new String[]{"Choose", "Equip", "Upgrade", "Salvage", "Back"});
        if (!w.armoryMessage.isEmpty()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, w.armoryMessage, width - 44 * s - fm.stringWidth(w.armoryMessage), height - 26 * s,
                w.confirmSalvage ? MenuStyle.WARN : GOLD);
        }
    }

    // ------------------------------------------------------------------ what's worn

    private void drawWorn(Graphics2D g, Profile prof, double x, double y, double w, double h, double s) {
        MenuStyle.card(g, x, y, w, h, null);
        double px = x + 18 * s, py = y + 26 * s;
        MenuStyle.label(g, "EQUIPPED", px, py, s, GOLD);
        double rowH = 46 * s, ry = py + 14 * s, box = 36 * s;
        for (Item.Slot sl : Item.Slot.values()) {
            Item it = prof.equipped.get(sl);
            TitleScreen.slotBox(g, it, sl, px, ry, box, s);
            double tx = px + box + 12 * s;
            g.setFont(MenuStyle.sans(Font.BOLD, 14 * s));
            if (it == null) {
                MenuStyle.shadowed(g, sl.label, tx, ry + 15 * s, new Color(130, 128, 148));
                g.setFont(MenuStyle.sans(Font.ITALIC, 12 * s));
                MenuStyle.shadowed(g, "empty", tx, ry + 31 * s, new Color(110, 108, 128));
            } else {
                MenuStyle.shadowed(g, fit(g, it.name + (it.upgrade > 0 ? "  +" + it.upgrade : ""), w - (tx - x) - 14 * s), tx, ry + 15 * s, it.rarity.color);
                g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
                MenuStyle.shadowed(g, it.lines().get(0), tx, ry + 31 * s, DIM);
            }
            ry += rowH;
        }

        // what it all adds up to
        double ty = ry + 18 * s;
        g.setColor(new Color(255, 214, 120, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(px, ty - 16 * s, x + w - 18 * s, ty - 16 * s));
        MenuStyle.label(g, "TOTAL BONUSES", px, ty, s, GOLD);
        Map<Item.Stat, Double> sum = new EnumMap<>(Item.Stat.class);
        List<String> uniques = new ArrayList<>();
        for (Item it : prof.equipped.values()) {
            for (Item.Stat st : it.stats.keySet()) sum.merge(st, it.value(st), Double::sum);
            if (it.unique != null) uniques.add(it.unique.text);
        }
        g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
        double col = (w - 36 * s) / 2, ly = ty + 20 * s;
        int i = 0;
        for (Map.Entry<Item.Stat, Double> e : sum.entrySet()) {
            MenuStyle.shadowed(g, e.getKey().format(e.getValue()), px + (i % 2) * col, ly + (i / 2) * 17 * s, new Color(200, 228, 200));
            i++;
        }
        ly += ((i + 1) / 2) * 17 * s + 4 * s;
        g.setFont(MenuStyle.serif(Font.ITALIC, 13 * s, 0));
        for (String u : uniques) ly = MenuStyle.wrap(g, u, px, ly, w - 36 * s, 16 * s, ORANGE);
        if (sum.isEmpty()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 14 * s, 0));
            MenuStyle.shadowed(g, "Nothing equipped yet.", px, ty + 22 * s, DIM);
        }
    }

    // ------------------------------------------------------------------ the bag

    private void drawBag(Graphics2D g, World w, Profile prof, List<Item> bag, int sel, double x, double y, double cw, double ch, double s) {
        MenuStyle.card(g, x, y, cw, ch, null);
        double px = x + 18 * s, py = y + 26 * s;
        MenuStyle.label(g, "YOUR ITEMS", px, py, s, GOLD);
        g.setFont(MenuStyle.caps(11 * s));
        String count = bag.size() + " / " + Profile.MAX_ITEMS;
        MenuStyle.shadowed(g, count, x + cw - 18 * s - g.getFontMetrics().stringWidth(count), py, DIM);
        if (bag.isEmpty()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
            MenuStyle.wrap(g, "Nothing here yet. Elites sometimes drop gear, every boss chest holds a piece, and a victory adds a rare one "
                + "on top. You keep everything you find, even when a run ends in defeat.", px, py + 40 * s, cw - 36 * s, 21 * s, TEXT);
            return;
        }
        double rowH = 34 * s, listTop = py + 16 * s;
        int visible = Math.max(1, (int) ((y + ch - 30 * s - listTop) / rowH));
        int first = Math.max(0, Math.min(sel - visible / 2, bag.size() - visible));
        double barY = glide.step(sel - first, Math.min(visible, bag.size()), listTop + (sel - first) * rowH);

        // the selection: a gold bar fading out to the right, with a solid edge
        Shape clip = g.getClip();
        g.clip(new Rectangle2D.Double(x + 1, listTop - 2, cw - 2, visible * rowH + 4));
        g.setPaint(new GradientPaint((float) (x + 8 * s), 0, new Color(255, 180, 70, 90), (float) (x + cw - 8 * s), 0, new Color(255, 180, 70, 8)));
        g.fill(new Rectangle2D.Double(x + 8 * s, barY, cw - 16 * s, rowH - 4 * s));
        g.setColor(MenuStyle.GOLD_DEEP);
        g.fill(new Rectangle2D.Double(x + 8 * s, barY, 3 * s, rowH - 4 * s));
        g.setClip(clip);

        for (int i = first; i < bag.size() && i < first + visible; i++) {
            Item it = bag.get(i);
            double ry = listTop + (i - first) * rowH, mid = ry + (rowH - 4 * s) / 2;
            Art.frames("item." + it.slot.name().toLowerCase())[0].draw(g, px + 12 * s, mid, 1.7 * s, false);
            g.setFont(MenuStyle.sans(Font.BOLD, 14 * s));
            FontMetrics fm = g.getFontMetrics();
            String tag = it.slot.label;
            g.setFont(MenuStyle.caps(10 * s));
            double tagW = g.getFontMetrics().stringWidth(tag), eqW = prof.isEquipped(it) ? g.getFontMetrics().stringWidth("WORN") + 12 * s : 0;
            g.setFont(MenuStyle.sans(Font.BOLD, 14 * s));
            String name = fit(g, it.name + (it.upgrade > 0 ? "  +" + it.upgrade : ""), cw - 60 * s - tagW - eqW - 20 * s);
            MenuStyle.shadowed(g, name, px + 32 * s, mid + fm.getAscent() * 0.36, i == sel ? it.rarity.color.brighter() : it.rarity.color);
            g.setFont(MenuStyle.caps(10 * s));
            double tx = x + cw - 18 * s - tagW;
            MenuStyle.shadowed(g, tag.toUpperCase(), tx, mid + 4 * s, DIM);
            if (prof.isEquipped(it)) MenuStyle.shadowed(g, "WORN", tx - eqW, mid + 4 * s, GREEN);
        }
        if (bag.size() > visible) {                                                   // there's more above or below
            double ax = x + cw / 2;
            g.setColor(new Color(255, 214, 120, 150));
            if (first > 0) g.fill(chevron(ax, listTop - 8 * s, 6 * s, true));
            if (first + visible < bag.size()) g.fill(chevron(ax, listTop + visible * rowH + 2 * s, 6 * s, false));
        }
    }

    private static Shape chevron(double cx, double cy, double r, boolean up) {
        Path2D p = new Path2D.Double();
        double d = up ? -1 : 1;
        p.moveTo(cx - r, cy - d * r * 0.4);
        p.lineTo(cx, cy + d * r * 0.6);
        p.lineTo(cx + r, cy - d * r * 0.4);
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ the selected item

    private void drawDetails(Graphics2D g, World w, Profile prof, Item it, double x, double y, double cw, double ch, double s) {
        MenuStyle.card(g, x, y, cw, ch, it == null ? null : it.rarity.color);
        double px = x + 22 * s, cx = x + cw / 2;
        if (it == null) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
            MenuStyle.shadowed(g, "Win some gear, then look at it here.", px, y + 44 * s, DIM);
            return;
        }
        Art.frames("item." + it.slot.name().toLowerCase())[0].draw(g, cx, y + 66 * s, 4 * s, false);
        g.setFont(MenuStyle.serif(Font.BOLD, 24 * s, 0.04));
        String name = fit(g, it.name + (it.upgrade > 0 ? "  +" + it.upgrade : ""), cw - 30 * s);
        FontMetrics fm = g.getFontMetrics();
        MenuStyle.shadowed(g, name, cx - fm.stringWidth(name) / 2.0, y + 134 * s, it.rarity.color);
        g.setFont(MenuStyle.caps(10 * s));
        fm = g.getFontMetrics();
        String kind = (it.rarity.label + " " + it.slot.label + DOT + (it.tier >= 3 ? "a victory's reward" : "found on stage " + (it.tier + 1))).toUpperCase();
        MenuStyle.shadowed(g, kind, cx - fm.stringWidth(kind) / 2.0, y + 154 * s, DIM);

        double ly = y + 188 * s;
        for (Map.Entry<Item.Stat, Double> e : it.stats.entrySet()) {
            g.setFont(MenuStyle.sans(Font.PLAIN, 14 * s));
            MenuStyle.diamond(g, px + 4 * s, ly - 5 * s, 3.5 * s, Util.alpha(it.rarity.color, 0.9));
            MenuStyle.shadowed(g, e.getKey().format(it.value(e.getKey())), px + 16 * s, ly, new Color(228, 240, 228));
            ly += 22 * s;
        }
        if (it.unique != null) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
            ly = MenuStyle.wrap(g, it.unique.text, px, ly + 4 * s, cw - 44 * s, 19 * s, ORANGE);
        }
        Item worn = prof.equipped.get(it.slot);
        if (worn != null && worn != it) {
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            MenuStyle.shadowed(g, "Would replace " + worn.name + (worn.upgrade > 0 ? " +" + worn.upgrade : ""), px, ly + 8 * s, DIM);
        }

        // what the keys would do to it
        double ay = y + ch - 84 * s, step = 28 * s;
        boolean maxed = it.upgrade >= Item.MAX_UPGRADE, afford = prof.gold >= it.upgradeCost();
        action(g, "ENTER", prof.isEquipped(it) ? "Take off" : "Equip", TEXT, px, ay, s);
        action(g, "U", maxed ? "Fully upgraded" : "Upgrade to +" + (it.upgrade + 1) + DOT + it.upgradeCost() + " gold",
            maxed ? DIM : afford ? GOLD : new Color(220, 120, 120), px, ay + step, s);
        action(g, "X", w.confirmSalvage ? "Press X again to salvage" : "Salvage" + DOT + it.salvageValue() + " gold",
            w.confirmSalvage ? MenuStyle.WARN : TEXT, px, ay + step * 2, s);
    }

    private static void action(Graphics2D g, String key, String text, Color c, double x, double baseline, double s) {
        MenuStyle.keyCap(g, key, x, baseline, s);
        g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
        MenuStyle.shadowed(g, text, x + 66 * s, baseline - 1 * s, c);
    }

    /** {@code text}, cut short with an ellipsis if it would be wider than {@code max} in the current font. */
    private static String fit(Graphics2D g, String text, double max) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= max) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "...") > max) t = t.substring(0, t.length() - 1);
        return t.trim() + "...";
    }
}
