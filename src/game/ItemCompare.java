package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * An item's stat lines set against what you wear in the same slot (the Armory's and the shops' close-up). Each line
 * gets an arrow on the right: green and up if it beats the worn item's, red and down if it falls short, a dash if
 * it's the same. Lines only the worn item has come after, struck through, as what you'd lose; so does its legendary
 * power.
 */
final class ItemCompare {
    private ItemCompare() {}

    static final Color BETTER = new Color(120, 235, 140);
    static final Color WORSE = new Color(240, 110, 110);
    private static final Color SAME = new Color(160, 156, 176);
    private static final Color LINE = new Color(228, 240, 228);
    private static final Color LOST = new Color(200, 140, 140);

    /** How many lines {@link #draw} will write for {@code it} against {@code worn} (to fit them in a panel). */
    static int lines(Item it, Item worn) {
        return rows(it, worn).size() + (worn != null && worn != it && worn.unique != null && it.unique == null ? 1 : 0);
    }

    /**
     * Writes {@code it}'s stat lines from {@code x} at baseline {@code y}, each {@code step} apart, with the comparison
     * against {@code worn} (null if the slot is empty) right-aligned at {@code right}. Returns the next baseline.
     */
    static double draw(Graphics2D g, Item it, Item worn, double x, double right, double y, double step, double s) {
        boolean compare = worn != it;
        if (compare) {                                                         // the column's heading, above it
            g.setFont(MenuStyle.caps(8.5 * s));
            String head = worn == null ? "NOTHING WORN" : "VS. WORN";
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, head, right - fm.stringWidth(head), y - step + 4 * s, MenuStyle.DIM);
        }
        for (Item.Stat st : rows(it, worn)) {
            double mine = it.value(st), theirs = worn == null || worn == it ? 0 : worn.value(st);
            boolean lost = !it.stats.containsKey(st);
            g.setFont(MenuStyle.sans(Font.PLAIN, 14 * s));
            if (lost) MenuStyle.diamond(g, x + 4 * s, y - 5 * s, 3.5 * s, Util.alpha(LOST, 0.5));
            else MenuStyle.diamond(g, x + 4 * s, y - 5 * s, 3.5 * s, Util.alpha(it.rarity.color, 0.9));
            String text = st.format(lost ? theirs : mine);
            MenuStyle.shadowed(g, text, x + 16 * s, y, lost ? LOST : LINE);
            if (lost) {                                                        // struck through: you'd lose it
                g.setColor(Util.alpha(LOST, 0.85));
                g.fill(new java.awt.geom.Rectangle2D.Double(x + 14 * s, y - 5.5 * s, g.getFontMetrics().stringWidth(text) + 4 * s, 1.4 * s));
            }
            if (compare) delta(g, st, mine - theirs, right, y, s);
            y += step;
        }
        if (compare && worn != null && worn.unique != null && it.unique == null) {
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.diamond(g, x + 4 * s, y - 5 * s, 3.5 * s, Util.alpha(LOST, 0.5));
            MenuStyle.shadowed(g, "Loses " + worn.unique.itemName + "'s power", x + 16 * s, y, LOST);
            y += step;
        }
        return y;
    }

    /** This item's stats, then the ones only the worn item has. */
    private static Set<Item.Stat> rows(Item it, Item worn) {
        Set<Item.Stat> rows = new LinkedHashSet<>(it.stats.keySet());
        if (worn != null && worn != it) for (Map.Entry<Item.Stat, Double> e : worn.stats.entrySet()) rows.add(e.getKey());
        return rows;
    }

    /** The difference, right-aligned at {@code right}: an arrow and the amount, or a dash if it's too small to show. */
    private static void delta(Graphics2D g, Item.Stat st, double d, double right, double y, double s) {
        g.setFont(MenuStyle.sans(Font.BOLD, 13 * s));
        FontMetrics fm = g.getFontMetrics();
        String amount = st.amount(Math.abs(d));
        if (amount.matches("0(\\.0)?%?")) {
            MenuStyle.shadowed(g, "-", right - fm.stringWidth("-") - 2 * s, y, SAME);
            return;
        }
        boolean up = d > 0;                                     // every stat is stored so that more is better (less damage taken, shorter cooldowns)
        Color c = up ? BETTER : WORSE;
        double tx = right - fm.stringWidth(amount);
        MenuStyle.shadowed(g, amount, tx, y, c);
        double ax = tx - 9 * s, ay = y - 5 * s, r = 4.5 * s;
        Path2D arrow = new Path2D.Double();
        if (up) { arrow.moveTo(ax - r, ay + r * 0.6); arrow.lineTo(ax, ay - r * 0.8); arrow.lineTo(ax + r, ay + r * 0.6); }
        else { arrow.moveTo(ax - r, ay - r * 0.6); arrow.lineTo(ax, ay + r * 0.8); arrow.lineTo(ax + r, ay - r * 0.6); }
        arrow.closePath();
        g.setColor(new Color(0, 0, 0, 120));
        g.translate(s, s);
        g.fill(arrow);
        g.translate(-s, -s);
        g.setColor(c);
        g.fill(arrow);
    }
}
