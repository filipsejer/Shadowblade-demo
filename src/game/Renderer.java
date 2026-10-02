package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Draws a {@link World}: the world itself ({@link WorldRenderer}), then the HUD and whichever screen is up. */
final class Renderer {
    private static final Color OUTSIDE = new Color(52, 52, 56);

    private final Font f12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font f14b = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private final Font f16 = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private final Random shakeRng = new Random();
    private final WorldRenderer worldRenderer = new WorldRenderer();
    private final RunHud runHud = new RunHud();
    private final TitleScreen titleScreen = new TitleScreen();
    private final ArmoryScreen armoryScreen = new ArmoryScreen();
    private final WorldHud worldHud = new WorldHud();
    private final Minimap bigMap = new Minimap();
    private BufferedImage vignetteImage;

    void render(World w, Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(OUTSIDE);
        g.fillRect(0, 0, width, height);

        AffineTransform screen = g.getTransform();
        double zoom = zoom(w), viewW = width / zoom, viewH = height / zoom;     // the menus' backdrop is shown close up
        double left = camOrigin(w.camX, viewW, w.level.width);
        double top = camOrigin(w.camY, viewH, w.level.height);
        double sx = (shakeRng.nextDouble() - 0.5) * 2 * w.shake;
        double sy = (shakeRng.nextDouble() - 0.5) * 2 * w.shake;
        double ox = Math.round(-left + sx), oy = Math.round(-top + sy);   // whole pixels: crisp art, no seams between chunks
        g.scale(zoom, zoom);
        g.translate(ox, oy);
        worldRenderer.draw(g, w, new Rectangle2D.Double(-ox, -oy, viewW, viewH));

        g.setTransform(screen);
        g.drawImage(vignette(width, height), 0, 0, null);
        if (w.fade > 0) {                                   // arriving somewhere: fade in from black
            g.setColor(new Color(0, 0, 0, (int) (255 * Util.clamp(w.fade, 0, 1))));
            g.fillRect(0, 0, width, height);
        }
        boolean screenUp = w.titleScene || w.state == World.State.TRAINER || w.state == World.State.SHOP || w.state == World.State.BRIEFING
            || w.state == World.State.ARMORY || w.state == World.State.RESULTS || w.state == World.State.TRAVEL;
        if (w.run != null && w.state != World.State.RESULTS) runHud.drawHud(g, w, width, height);
        else if (!screenUp) worldHud.draw(g, w, width, height);
        if (w.mapZoom > 0 && !screenUp) bigMap.draw(g, w, width, height);   // TAB held: the corner map, grown over the HUD
        if (w.dialogue.active() && w.state == World.State.PLAYING) drawDialogue(g, w.dialogue, TOWNSFOLK, width, height);
        double talkBlack = w.talkBlack();
        if (talkBlack > 0) {                                // into or out of a conversation: everything, the box too, goes black
            double a = talkBlack * talkBlack * (3 - 2 * talkBlack);
            g.setColor(new Color(0, 0, 0, (int) Math.round(255 * a)));
            g.fillRect(0, 0, width, height);
        }
        if (w.audio.muted) {
            g.setFont(f14b);
            g.setColor(new Color(255, 200, 120, 220));
            g.drawString("SOUND OFF  (M)", (float) (width - 140), (float) (height - 16));
        }
        if (w.state == World.State.ARMORY) MenuStyle.veil(g, width, height, 215);
        switch (w.state) {
            case TITLE -> titleScreen.draw(g, w, width, height);
            case SLOTS -> titleScreen.drawSlots(g, w, width, height);
            case TRAINER -> worldHud.drawTrainer(g, w, width, height);
            case SHOP -> worldHud.drawShop(g, w, width, height);
            case BRIEFING -> worldHud.drawBriefing(g, w, width, height);
            case TRAVEL -> worldHud.drawTravel(g, w, width, height);
            case PAUSE -> { if (w.run != null) runHud.drawRunPause(g, w, width, height); else worldHud.drawPause(g, w, width, height); }
            case LEVEL_UP -> runHud.drawChoices(g, w, width, height);
            case RESULTS -> runHud.drawRunEnd(g, w, width, height);
            case ARMORY -> armoryScreen.draw(g, w, width, height);
            case PLAYING -> { }
        }
    }

    /** A soft darkening toward the edges of the screen (cached per window size). */
    private BufferedImage vignette(int width, int height) {
        if (vignetteImage == null || vignetteImage.getWidth() != width || vignetteImage.getHeight() != height) {
            vignetteImage = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
            Graphics2D vg = vignetteImage.createGraphics();
            float radius = (float) Math.hypot(width, height) / 2f;
            vg.setPaint(new RadialGradientPaint(width / 2f, height / 2f, radius, new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
            vg.fillRect(0, 0, width, height);
            vg.dispose();
        }
        return vignetteImage;
    }

    /** Top-left world coordinate of the view; centred on the player but never showing past the arena edge. */
    /** The menus' backdrop is drawn this much closer than play (a whole number, so the pixel art stays crisp). */
    static final double TITLE_ZOOM = 2;

    static double zoom(World w) { return w.titleScene ? TITLE_ZOOM : 1; }

    /** Where a point in the world lands on screen right now (ignoring screen shake). */
    static Util.Vec toScreen(World w, double x, double y, int width, int height) {
        double z = zoom(w);
        double left = camOrigin(w.camX, width / z, w.level.width), top = camOrigin(w.camY, height / z, w.level.height);
        return new Util.Vec((x - Math.round(left)) * z, (y - Math.round(top)) * z);
    }

    private static double camOrigin(double center, double view, double arena) {
        if (view >= arena) return (arena - view) / 2;
        return Util.clamp(center - view / 2.0, 0, arena - view);
    }

    // ------------------------------------------------------------------ dialogue

    private static final Color TOWNSFOLK = new Color(120, 200, 235);

    /** The speech box: the speaker's face on the left, their words written out a letter at a time, a "press E" arrow when they wait for you. */
    private void drawDialogue(Graphics2D g, Dialogue dialogue, Color accent, int width, int height) {
        Dialogue.Line line = dialogue.current();
        if (line == null) return;
        double bw = Util.clamp(width - 620, 480, 780);
        double tx0 = 124, tw = bw - tx0 - 24;

        // the words, wrapped to the whole line first so words never jump between rows as they are written (and the box knows how tall to be)
        g.setFont(f16);
        FontMetrics fm = g.getFontMetrics();
        java.util.List<String> rows = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : line.text().split(" ")) {
            String test = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(test) > tw && cur.length() > 0) {
                rows.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) rows.add(cur.toString());

        double bh = Math.max(120, 62 + rows.size() * 25);
        double bx = (width - bw) / 2, by = height - 118 - bh;
        RoundRectangle2D panel = new RoundRectangle2D.Double(bx, by, bw, bh, 18, 18);
        g.setColor(new Color(26, 22, 34, 238));
        g.fill(panel);
        g.setColor(Util.alpha(accent, 0.9));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(panel);

        // portrait
        RoundRectangle2D frame = new RoundRectangle2D.Double(bx + 14, by + 14, 92, 92, 14, 14);
        g.setColor(new Color(52, 42, 58));
        g.fill(frame);
        g.setColor(Util.alpha(accent, 0.7));
        g.setStroke(new BasicStroke(2f));
        g.draw(frame);
        Sprite face = Art.frame(line.portrait(), System.nanoTime() / 1e9, 2);
        java.awt.RenderingHints hints = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        face.draw(g, bx + 60 - (face.w / 2.0 - face.ax) * 3 * face.k, by + 100, 3, false);   // centred, whatever the size of its pixels
        g.setRenderingHints(hints);

        // name tab
        g.setFont(f14b);
        fm = g.getFontMetrics();
        String name = line.speaker();
        double tabW = fm.stringWidth(name) + 28;
        RoundRectangle2D tab = new RoundRectangle2D.Double(bx + 18, by - 17, tabW, 28, 10, 10);
        g.setColor(accent);
        g.fill(tab);
        g.setColor(new Color(50, 30, 18));
        g.drawString(name, (float) (bx + 32), (float) (by + 3));
        if (!line.tag().isEmpty()) {
            g.setFont(f12);
            g.setColor(new Color(200, 190, 205));
            g.drawString(line.tag(), (float) (bx + 18 + tabW + 10), (float) (by + 2));
        }

        g.setFont(f16);
        int left = Math.min(line.text().length(), dialogue.visible().length());
        double ty = by + 42;
        g.setColor(new Color(240, 238, 246));
        for (String row : rows) {
            if (left <= 0) break;
            g.drawString(row.substring(0, Math.min(row.length(), left)), (float) (bx + tx0), (float) ty);
            left -= row.length() + 1;
            ty += 25;
        }

        if (dialogue.waitingForKey()) {                                       // "E" and a bobbing arrow
            double bob = 3 * Math.sin(System.nanoTime() / 2.2e8);
            g.setFont(f12);
            g.setColor(new Color(215, 205, 225));
            g.drawString("E / ENTER", (float) (bx + bw - 92), (float) (by + bh - 14));
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(bx + bw - 26, by + bh - 24 + bob);
            arrow.lineTo(bx + bw - 12, by + bh - 24 + bob);
            arrow.lineTo(bx + bw - 19, by + bh - 14 + bob);
            arrow.closePath();
            g.setColor(accent);
            g.fill(arrow);
        }
    }
}
