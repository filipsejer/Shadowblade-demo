package game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

/** A tiny 3x5 pixel font for damage and healing numbers, drawn with a dark outline. Digits, + and - only. */
final class PixelFont {
    private PixelFont() {}

    private static final String[] GLYPHS = {
        "111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
        "111100111001111", "111100111101111", "111001010010010", "111101111101111", "111101111001111",
    };
    private static final String PLUS = "000010111010000", MINUS = "000000111000000";

    /** True if this font can draw the whole string. */
    static boolean canDraw(String s) {
        if (s.isEmpty()) return false;
        for (char c : s.toCharArray()) if (!(c >= '0' && c <= '9') && c != '+' && c != '-') return false;
        return true;
    }

    static int width(String s, int scale) { return (s.length() * 4 - 1) * scale; }

    /** Draws the text centred on cx, with its top at y. Each font pixel is {@code scale} world units. */
    static void draw(Graphics2D g, String s, double cx, double y, int scale, Color fill, float alpha) {
        java.awt.Composite saved = g.getComposite();
        if (alpha < 1f) g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, alpha)));
        int x0 = (int) Math.round(cx - width(s, scale) / 2.0), y0 = (int) Math.round(y);
        Color dark = new Color(20, 14, 26);
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}}) glyphs(g, s, x0 + o[0] * scale, y0 + o[1] * scale, scale, dark);
        glyphs(g, s, x0, y0, scale, fill);
        g.setComposite(saved);
    }

    private static void glyphs(Graphics2D g, String s, int x0, int y0, int scale, Color c) {
        g.setColor(c);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            String bits = ch == '+' ? PLUS : ch == '-' ? MINUS : GLYPHS[ch - '0'];
            for (int k = 0; k < 15; k++) {
                if (bits.charAt(k) == '1') g.fillRect(x0 + (i * 4 + k % 3) * scale, y0 + (k / 3) * scale, scale, scale);
            }
        }
    }
}
