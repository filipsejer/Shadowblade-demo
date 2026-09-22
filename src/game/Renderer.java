package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Draws a {@link World}: simple shapes on a grey floor, plain health bars, and the menu overlays. */
final class Renderer {
    private static final Color OUTSIDE = new Color(52, 52, 56);
    private static final Color FLOOR = new Color(112, 112, 116);
    private static final Color GRID = new Color(102, 102, 106);
    private static final Color WALL = new Color(36, 36, 40);
    private static final Color DOOR_BARRIER = new Color(170, 50, 50);   // closed for combat
    private static final Color DOOR_SEALED = new Color(150, 80, 200);    // sealed until the other rooms are cleared
    private static final Color BAR_BG = new Color(20, 20, 22, 210);
    private static final Color HP = new Color(70, 200, 90);
    private static final Color MP = new Color(70, 130, 255);
    private static final Color XP = new Color(255, 205, 70);
    private static final Color PANEL = new Color(30, 30, 34, 235);

    private final Font f12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font f14 = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Font f14b = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private final Font f16 = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private final Font f18b = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private final Font f26b = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    private final Font f54b = new Font(Font.SANS_SERIF, Font.BOLD, 54);
    private final Random shakeRng = new Random();
    private final Minimap minimap = new Minimap();
    private final WorldRenderer worldRenderer = new WorldRenderer();
    private BufferedImage vignetteImage;

    void render(World w, Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(OUTSIDE);
        g.fillRect(0, 0, width, height);

        AffineTransform screen = g.getTransform();
        double left = camOrigin(w.camX, width, w.level.width);
        double top = camOrigin(w.camY, height, w.level.height);
        double sx = (shakeRng.nextDouble() - 0.5) * 2 * w.shake;
        double sy = (shakeRng.nextDouble() - 0.5) * 2 * w.shake;
        double ox = Math.round(-left + sx), oy = Math.round(-top + sy);   // whole pixels: crisp art, no seams between chunks
        g.translate(ox, oy);
        worldRenderer.draw(g, w, new Rectangle2D.Double(-ox, -oy, width, height));

        g.setTransform(screen);
        g.drawImage(vignette(width, height), 0, 0, null);
        if (w.fade > 0) {                                   // arriving in a new level: fade in from black
            g.setColor(new Color(0, 0, 0, (int) (255 * Util.clamp(w.fade, 0, 1))));
            g.fillRect(0, 0, width, height);
        }
        if (w.tutorial != null && w.state != World.State.TITLE) drawEyelids(g, w.tutorial, width, height);
        drawHud(g, w, width, height);
        if (w.tutorial != null && w.state == World.State.PLAYING) drawDialogue(g, w.tutorial.dialogue, SQUIRREL, width, height);
        else if (w.dialogue.active() && w.state == World.State.PLAYING) drawDialogue(g, w.dialogue, TOWNSFOLK, width, height);
        if (w.audio.muted) {
            g.setFont(f14b);
            g.setColor(new Color(255, 200, 120, 220));
            g.drawString("SOUND OFF  (M)", (float) (width - 140), (float) (height - 16));
        }
        if (w.tutorial != null && w.tutorial.blackout() > 0) {                 // the story ends: the screen goes dark before level 1
            g.setColor(new Color(0, 0, 0, (int) (255 * Util.clamp(w.tutorial.blackout(), 0, 1))));
            g.fillRect(0, 0, width, height);
        }
        switch (w.state) {
            case TITLE -> drawTitle(g, w, width, height);
            case CHAPTER_SELECT -> drawChapterSelect(g, w, width, height);
            case UPGRADE -> drawStationMenu(g, w, width, height);
            case PAUSE -> drawPause(g, w, width, height);
            case GAME_OVER -> drawGameOver(g, w, width, height);
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
    private static double camOrigin(double center, int view, double arena) {
        if (view >= arena) return (arena - view) / 2;
        return Util.clamp(center - view / 2.0, 0, arena - view);
    }

    // ------------------------------------------------------------------ HUD

    private void bar(Graphics2D g, double x, double y, double w, double h, double frac, Color fill) {
        g.setColor(BAR_BG);
        g.fill(new Rectangle2D.Double(x - 1.5, y - 1.5, w + 3, h + 3));
        g.setColor(fill);
        g.fill(new Rectangle2D.Double(x, y, w * Util.clamp(frac, 0, 1), h));
    }

    private void drawHud(Graphics2D g, World w, int width, int height) {
        Player p = w.player;
        Tutorial tut = w.tutorial;                       // in the tutorial the HUD grows as the controls are taught

        bar(g, 20, 20, 300, 22, p.hp / p.maxHp, HP);
        g.setFont(f14b);
        g.setColor(Color.WHITE);
        g.drawString("HP  " + (int) Math.ceil(Math.max(0, p.hp)) + " / " + (int) p.maxHp, 28, 36);
        if (tut == null || tut.showMagic()) {
            bar(g, 20, 50, 240, 14, p.mp / p.maxMp, MP);
            g.setFont(f12);
            g.setColor(Color.WHITE);
            g.drawString("MP  " + (int) p.mp + " / " + (int) p.maxMp, 26, 61);
        }
        if (tut == null) {
            bar(g, 20, 72, 240, 7, (double) p.xp / p.xpNext, XP);
            g.setFont(f14b);
            g.setColor(XP);
            g.drawString("LV " + p.level, 270, 80);
        }

        if (tut == null && p.skillPoints > 0) {
            g.setFont(f14b);
            g.setColor(new Color(255, 215, 90));
            g.drawString("Skill points: " + p.skillPoints, 20, 100);
        }
        Enemy boss = w.boss();
        if (boss != null) {
            double bw = 520, bx = (width - bw) / 2;
            bar(g, bx, 68, bw, 16, boss.hp / boss.maxHp, boss.phase2 ? new Color(255, 110, 60) : new Color(210, 45, 70));
            g.setFont(f12);
            boolean lab = w.level.theme == Theme.LAB;               // the last boss has two full bars, one per stage
            centered(g, w.level.bossName + (lab ? "   -   STAGE " + (boss.phase2 ? 2 : 1) : ""), width / 2.0, 81, Color.WHITE);
        }

        if (w.level.town) {
            minimap.draw(g, w, width);
            g.setFont(f18b);
            centered(g, "TRANSIT TOWN", width / 2.0, 34, Color.WHITE);
            g.setFont(f12);
            centered(g, w.stationOpen ? "The train station is open" : "A path somewhere here leads into the forest",
                width / 2.0, 54, new Color(220, 220, 230));
        } else if (tut == null) {
            minimap.draw(g, w, width);

            // room info
            g.setFont(f18b);
            centered(g, w.roomStatus(), width / 2.0, 34, Color.WHITE);
            g.setFont(f12);
            Level map = w.level;
            String roomsLine = map.name + "   -   rooms cleared " + map.clearedRegularCount() + " / " + map.regularRoomCount();
            if (map.clearedRoomCount() == map.combatRoomCount()) roomsLine = map.name + "   -   cleared!";
            else if (map.bossUnlocked()) roomsLine += "   -   boss door unlocked";
            centered(g, roomsLine, width / 2.0, 54, new Color(220, 220, 230));
        } else if (!tut.objective().isEmpty()) {
            g.setFont(f18b);
            centered(g, tut.objective(), width / 2.0, 34, new Color(255, 225, 140));
        }
        if (w.noticeTimer > 0 && w.state == World.State.PLAYING) {
            double fade = Math.min(1, w.noticeTimer);
            g.setFont(f26b);
            centered(g, w.notice, width / 2.0, height - 150, Util.alpha(new Color(255, 220, 90), fade));
            g.setFont(f14);
            if (!w.noticeHint.isEmpty()) centered(g, w.noticeHint, width / 2.0, height - 126, Util.alpha(Color.WHITE, fade));
        }
        if (w.bannerTimer > 0 && w.state != World.State.TITLE && w.state != World.State.CHAPTER_SELECT) {
            g.setFont(f54b);
            centered(g, w.banner, width / 2.0, height * 0.3, Util.alpha(Color.WHITE, Math.min(1, w.bannerTimer)));
        }

        // roll indicator (bottom centre) and the command menu (bottom left)
        double size = 68;
        if (tut == null || tut.showRoll()) {
            actionBox(g, (width - size) / 2, height - size - 26, size, "SPACE", "Roll", new Color(200, 200, 210), p.dodgeCd / p.dodgeCooldown, true);
            if (tut != null && tut.focus() == Tutorial.Focus.ROLL) {
                pulse(g, new RoundRectangle2D.Double((width - size) / 2 - 5, height - size - 31, size + 10, size + 10, 14, 14), w.time);
            }
        }
        if (tut == null || tut.showMenu()) drawCommandMenu(g, p, height, tut, w.time);
    }

    /** A soft gold outline that breathes: "look here". */
    private void pulse(Graphics2D g, Shape s, double time) {
        double k = 0.5 + 0.5 * Math.sin(time * 6);
        g.setColor(new Color(255, 215, 90, (int) (110 + 130 * k)));
        g.setStroke(new BasicStroke((float) (3 + 2.5 * k)));
        g.draw(s);
    }

    /**
     * The Kingdom Hearts style command menu: Attack / Magic / Items stacked in the bottom-left corner. The Magic list
     * opens to the right of it. Items is greyed out ("coming soon") and the cursor never lands on it.
     */
    private void drawCommandMenu(Graphics2D g, Player p, int height, Tutorial tut, double time) {
        CommandMenu menu = p.menu;
        CommandMenu.Item[] items = CommandMenu.Item.values();
        double x = 20, rowW = 190, rowH = 46, rowInner = 40;
        int rows = tut == null || tut.showMagic() ? items.length : 1;        // in the tutorial only Attack is there until magic is taught
        double panelH = items.length * rowH;
        double y0 = height - 24 - rows * rowH;
        panelH = rows * rowH;

        for (int i = 0; i < rows; i++) {
            CommandMenu.Item item = items[i];
            double ry = y0 + i * rowH;
            boolean selected = menu.cursor == item;
            RoundRectangle2D row = new RoundRectangle2D.Double(x, ry, rowW, rowInner, 12, 12);
            g.setColor(!item.enabled ? new Color(20, 20, 24, 190) : selected ? new Color(52, 66, 104, 240) : new Color(24, 24, 30, 220));
            g.fill(row);
            if (tut != null && ((item == CommandMenu.Item.ATTACK && tut.focus() == Tutorial.Focus.ATTACK)
                || (item == CommandMenu.Item.MAGIC && tut.focus() == Tutorial.Focus.MAGIC))) {
                pulse(g, new RoundRectangle2D.Double(x - 4, ry - 4, rowW + 8, rowInner + 8, 15, 15), time);
            }
            g.setColor(!item.enabled ? new Color(70, 70, 78) : selected ? Color.WHITE : new Color(95, 100, 125));
            g.setStroke(new BasicStroke(selected ? 3f : 1.8f));
            g.draw(row);

            g.setFont(f18b);
            Color text = !item.enabled ? new Color(105, 105, 112) : selected ? Color.WHITE : new Color(190, 195, 215);
            g.setColor(text);
            g.drawString(item.label, (float) (x + 40), (float) (ry + (item.enabled ? 26 : 19)));
            if (!item.enabled) {
                g.setFont(f12);
                g.setColor(new Color(105, 105, 112));
                g.drawString("coming soon", (float) (x + 40), (float) (ry + 34));
            }
            if (selected && item.enabled) {                      // the cursor arrow
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(x + 14, ry + 11);
                arrow.lineTo(x + 28, ry + 20);
                arrow.lineTo(x + 14, ry + 29);
                arrow.closePath();
                g.setColor(new Color(255, 225, 120));
                g.fill(arrow);
            }
            if (item == CommandMenu.Item.MAGIC) {                // a small ">" showing there's a list to the right
                Path2D chevron = new Path2D.Double();
                chevron.moveTo(x + rowW - 22, ry + 13);
                chevron.lineTo(x + rowW - 14, ry + 20);
                chevron.lineTo(x + rowW - 22, ry + 27);
                g.setColor(text);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(chevron);
            }
            if (item == CommandMenu.Item.ATTACK) {               // combo progress
                int progress = p.comboProgress();
                for (int k = 0; k < p.comboMax; k++) {
                    double dx = x + rowW - 14 - (p.comboMax - 1 - k) * 11;
                    g.setColor(k < progress ? new Color(255, 215, 90) : new Color(10, 10, 14, 220));
                    g.fill(new Ellipse2D.Double(dx - 4, ry + rowInner / 2 - 4, 8, 8));
                }
            }
        }

        if (menu.magicOpen) drawMagicList(g, p, x + rowW + 10, y0 + panelH);
    }

    // ------------------------------------------------------------------ dialogue

    private static final Color SQUIRREL = new Color(244, 170, 90);
    private static final Color TOWNSFOLK = new Color(120, 200, 235);

    /** Two black lids over the screen that open as you wake up (see {@link Tutorial#eyelids}). */
    private void drawEyelids(Graphics2D g, Tutorial tut, int width, int height) {
        double lid = tut.eyelids();
        if (lid <= 0) return;
        int h = (int) (height / 2.0 * lid), soft = 46;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, h);
        g.fillRect(0, height - h, width, h);
        java.awt.Paint saved = g.getPaint();
        g.setPaint(new java.awt.GradientPaint(0, h, new Color(0, 0, 0, 255), 0, h + soft, new Color(0, 0, 0, 0)));
        g.fillRect(0, h, width, soft);
        g.setPaint(new java.awt.GradientPaint(0, height - h - soft, new Color(0, 0, 0, 0), 0, height - h, new Color(0, 0, 0, 255)));
        g.fillRect(0, height - h - soft, width, soft);
        g.setPaint(saved);
    }

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
        face.draw(g, bx + 60 - (face.w / 2.0 - face.ax) * 3, by + 100, 3, false);
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

    /** The spell list, bottom-aligned with the command menu. Shows MP cost and recharge; red means you can't afford it. */
    private void drawMagicList(Graphics2D g, Player p, double x, double bottom) {
        java.util.List<Ability> spells = p.learnedSpells();
        Ability selected = p.menu.selected(spells);
        double rowW = 240, rowH = 46, rowInner = 40;
        double top = bottom - spells.size() * rowH;
        for (int i = 0; i < spells.size(); i++) {
            Ability a = spells.get(i);
            int lv = p.spellLevel[a.ordinal()];
            double cost = a.cost(lv);
            boolean afford = p.mp >= cost;
            double cd = p.cooldown[a.ordinal()] / (a.cooldown * p.cooldownMult);
            boolean ready = afford && cd <= 0;
            double ry = top + i * rowH;
            boolean isSel = a == selected;
            RoundRectangle2D row = new RoundRectangle2D.Double(x, ry, rowW, rowInner, 12, 12);
            g.setColor(isSel ? new Color(52, 66, 104, 240) : new Color(24, 24, 30, 220));
            g.fill(row);
            if (cd > 0) {                                        // recharging: a pale bar draining across the row
                Shape saved = g.getClip();
                g.clip(row);
                g.setColor(new Color(255, 255, 255, 55));
                g.fill(new Rectangle2D.Double(x, ry, rowW * Util.clamp(cd, 0, 1), rowInner));
                g.setClip(saved);
            }
            g.setColor(isSel ? Color.WHITE : Util.alpha(a.color, 0.55));
            g.setStroke(new BasicStroke(isSel ? 3f : 1.8f));
            g.draw(row);

            g.setFont(f14b);
            g.setColor(ready ? Color.WHITE : new Color(150, 150, 160));
            g.drawString(a.label, (float) (x + 16), (float) (ry + 19));
            g.setFont(f12);
            g.setColor(Util.alpha(a.color, ready ? 1 : 0.55));
            g.drawString("Level " + lv, (float) (x + 16), (float) (ry + 34));
            g.setFont(f18b);
            right(g, (int) cost + " MP", x + rowW - 14, ry + 27, afford ? new Color(140, 185, 255) : new Color(225, 105, 105));
        }
        g.setFont(f12);
        g.setColor(new Color(215, 215, 225));
        g.drawString(p.menu.shiftOpened ? "ENTER cast     release SHIFT to close" : "ENTER cast     LEFT back", (float) x, (float) (top - 8));
    }

    private void actionBox(Graphics2D g, double x, double y, double s, String key, String label, Color accent,
                           double cooldownFrac, boolean usable) {
        RoundRectangle2D box = new RoundRectangle2D.Double(x, y, s, s, 10, 10);
        g.setColor(new Color(24, 24, 28, 220));
        g.fill(box);
        g.setColor(Util.alpha(accent, usable ? 0.9 : 0.35));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(box);
        if (cooldownFrac > 0) {
            g.setColor(new Color(255, 255, 255, 60));
            double h = s * Util.clamp(cooldownFrac, 0, 1);
            g.fill(new Rectangle2D.Double(x + 2, y + s - h, s - 4, Math.max(0, h - 2)));
        }
        g.setFont(key.length() > 2 ? f14b : f26b);
        centered(g, key, x + s / 2, y + s / 2 + (key.length() > 2 ? 2 : 6), usable ? Color.WHITE : new Color(150, 150, 155));
        g.setFont(f12);
        centered(g, label, x + s / 2, y + s - 8, usable ? Util.alpha(accent, 1) : new Color(120, 120, 125));
    }

    // ------------------------------------------------------------------ overlays

    private void dim(Graphics2D g, int width, int height, int alpha) {
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(0, 0, width, height);
    }

    private void drawTitle(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 150);
        double cx = width / 2.0;
        g.setFont(f54b);
        centered(g, "SPELLBLADE", cx, height * 0.2, Color.WHITE);
        g.setFont(f18b);
        centered(g, "Clear the rooms. Chain combos. Grow stronger every level.", cx, height * 0.2 + 36, new Color(200, 200, 210));

        String[] items = {"PLAY", "SELECT CHAPTER"};
        String[] hints = {"the opening story, from the very start" + (w.tutorialOn ? "" : "  (T: currently skipped, straight to LEVEL 1)"),
            "jump into any level, character level " + World.CHAPTER_SELECT_LEVEL + " and skilled up"};
        double y0 = height * 0.2 + 90, rowH = 58, gap = 16;
        for (int i = 0; i < items.length; i++) {
            menuRow(g, cx, y0 + i * (rowH + gap), 380, rowH, items[i], hints[i], w.menuCursor == i);
        }

        g.setFont(f14);
        double y = y0 + items.length * (rowH + gap) + 30;
        centered(g, "WASD move   SPACE roll   ENTER attack / confirm   TAB lock-on   E talk   ESC pause   M mute",
            cx, y, new Color(190, 190, 200));
        g.setFont(f26b);
        centered(g, "W / S choose      ENTER confirm", cx, y + 36, Util.alpha(Color.WHITE, 0.6 + 0.4 * Math.sin(System.nanoTime() / 3.0e8)));
    }

    /** One row of a keyboard menu: a highlighted pill with a title and a small hint line beneath it. */
    private void menuRow(Graphics2D g, double cx, double y, double w, double h, String title, String hint, boolean selected) {
        RoundRectangle2D row = new RoundRectangle2D.Double(cx - w / 2, y, w, h, 14, 14);
        g.setColor(selected ? new Color(52, 66, 104, 240) : new Color(24, 24, 30, 220));
        g.fill(row);
        g.setColor(selected ? Color.WHITE : new Color(95, 100, 125));
        g.setStroke(new BasicStroke(selected ? 3f : 1.8f));
        g.draw(row);
        g.setFont(f26b);
        centered(g, title, cx, y + 30, selected ? new Color(255, 225, 120) : new Color(200, 205, 220));
        g.setFont(f12);
        centered(g, hint, cx, y + 48, selected ? new Color(220, 220, 230) : new Color(140, 140, 150));
    }

    /** The Select Chapter screen: one row per level, each starting the character at {@link World#CHAPTER_SELECT_LEVEL}. */
    private void drawChapterSelect(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 170);
        double cx = width / 2.0;
        g.setFont(f54b);
        centered(g, "SELECT CHAPTER", cx, height * 0.16, Color.WHITE);
        g.setFont(f14);
        centered(g, "Starts in that level's hub, character level " + World.CHAPTER_SELECT_LEVEL + ", skill points ready to spend.",
            cx, height * 0.16 + 30, new Color(200, 200, 210));

        double rowW = 520, rowH = 66, gap = 16, y0 = height * 0.16 + 70;
        for (int i = 0; i < Level.COUNT; i++) {
            String title = "LEVEL " + (i + 1) + "   -   " + Level.BOSS_NAMES[i];
            menuRow(g, cx, y0 + i * (rowH + gap), rowW, rowH, title, Level.THEMES[i].title, w.chapterCursor == i);
        }
        g.setFont(f14);
        centered(g, "W / S choose      ENTER start      ESC back", cx, y0 + Level.COUNT * (rowH + gap) + 24, new Color(200, 200, 200));
    }

    /** A trainer's menu: their upgrades on the left, the selected one's details on the right. */
    private void drawStationMenu(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 200);
        Player p = w.player;
        Level.Station station = w.activeStation;
        Color accent = station.category().color;
        double pw = 940, ph = 500;
        double px = (width - pw) / 2, py = Math.max(16, (height - ph) / 2);
        Color gold = new Color(255, 215, 90);

        RoundRectangle2D panel = new RoundRectangle2D.Double(px, py, pw, ph, 18, 18);
        g.setColor(PANEL);
        g.fill(panel);
        g.setColor(Util.alpha(accent, 0.8));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(panel);

        g.setFont(f26b);
        g.setColor(accent);
        g.drawString(station.name(), (float) (px + 30), (float) (py + 46));
        g.setFont(f14);
        g.setColor(new Color(200, 200, 210));
        g.drawString(station.flavor(), (float) (px + 30), (float) (py + 72));
        g.setFont(f18b);
        right(g, "Skill points: " + p.skillPoints, px + pw - 30, py + 44, gold);

        // rows
        List<Upgrade> items = w.stationItems();
        double rowH = 50, ry0 = py + 100;
        for (int i = 0; i < items.size(); i++) {
            Upgrade u = items.get(i);
            boolean selected = i == w.stationCursor;
            boolean affordable = !u.maxed() && p.skillPoints >= u.cost();
            double ry = ry0 + i * rowH;
            RoundRectangle2D row = new RoundRectangle2D.Double(px + 30, ry, 480, rowH - 6, 10, 10);
            g.setColor(selected ? new Color(64, 64, 74) : new Color(40, 40, 46));
            g.fill(row);
            g.setColor(selected ? Color.WHITE : Util.alpha(u.accent(), 0.45));
            g.setStroke(new BasicStroke(selected ? 2.5f : 1.5f));
            g.draw(row);

            g.setFont(f18b);
            g.setColor(u.maxed() ? new Color(150, 150, 158) : Color.WHITE);
            g.drawString(u.title(), (float) (px + 46), (float) (ry + 21));
            g.setFont(f12);
            g.setColor(new Color(185, 185, 195));
            g.drawString(u.status(), (float) (px + 46), (float) (ry + 37));
            g.setFont(f18b);
            if (u.maxed()) right(g, "MAX", px + 496, ry + 28, new Color(150, 150, 158));
            else right(g, u.cost() + " SP", px + 496, ry + 28, affordable ? gold : new Color(200, 100, 100));
        }

        // details of the selected row
        if (!items.isEmpty()) {
            Upgrade u = items.get(Math.min(w.stationCursor, items.size() - 1));
            double dx = px + 540, dw = pw - 570;
            g.setFont(f26b);
            g.setColor(u.accent());
            wrapped(g, u.title(), dx, py + 130, dw, 30);
            g.setFont(f14);
            g.setColor(new Color(185, 185, 195));
            g.drawString(u.status(), (float) dx, (float) (py + 156));
            g.setColor(Color.WHITE);
            wrapped(g, u.description(), dx, py + 192, dw, 21);

            g.setFont(f18b);
            if (u.maxed()) {
                g.setColor(new Color(150, 150, 158));
                g.drawString("Fully upgraded", (float) dx, (float) (py + 350));
            } else {
                g.setColor(gold);
                g.drawString("Cost: " + u.cost() + " skill point" + (u.cost() == 1 ? "" : "s"), (float) dx, (float) (py + 350));
                boolean affordable = p.skillPoints >= u.cost();
                g.setColor(affordable ? new Color(120, 230, 130) : new Color(220, 110, 110));
                g.drawString(affordable ? "Press ENTER to buy" : "Not enough skill points", (float) dx, (float) (py + 380));
            }
        }

        if (!w.stationMessage.isEmpty()) {
            g.setFont(f18b);
            centered(g, w.stationMessage, px + pw / 2, py + ph - 56, gold);
        }
        g.setFont(f14);
        centered(g, "W / S  choose      ENTER  buy      ESC  leave", px + pw / 2, py + ph - 24, new Color(190, 190, 200));
    }

    private void drawPause(Graphics2D g, World w, int width, int height) {
        Player p = w.player;
        dim(g, width, height, 190);
        double cx = width / 2.0;
        double left = cx - 400;
        g.setFont(f54b);
        centered(g, "PAUSED", cx, height * 0.16, Color.WHITE);
        g.setFont(f14);
        centered(g, "Press ESC to resume", cx, height * 0.16 + 30, new Color(200, 200, 210));

        double y = height * 0.16 + 84;
        g.setFont(f18b);
        g.setColor(Color.WHITE);
        g.drawString("Magic", (float) left, (float) y);
        for (Ability a : Ability.values()) {
            y += 27;
            int lv = p.spellLevel[a.ordinal()];
            g.setFont(f14b);
            if (lv == 0) {
                g.setColor(new Color(120, 120, 126));
                g.drawString(a.label + "  -  not learned yet (the Wizard teaches it)", (float) left, (float) y);
            } else {
                g.setColor(a.color);
                g.drawString(a.label + "  Lv " + lv, (float) left, (float) y);
                g.setFont(f14);
                g.setColor(new Color(205, 205, 215));
                g.drawString((int) a.cost(lv) + " MP   " + String.format(Locale.ROOT, "%.1fs", a.cooldown * p.cooldownMult) + " cooldown   -   " + a.blurb,
                    (float) (left + 190), (float) y);
            }
        }

        y += 56;
        g.setFont(f18b);
        g.setColor(Color.WHITE);
        g.drawString("Stats", (float) left, (float) y);
        g.setFont(f14);
        g.setColor(new Color(205, 205, 215));
        String[] lines = {
            "Combo length:  " + p.comboMax + " hits",
            "Melee damage:  " + Math.round(p.meleeDamage * p.meleeMult),
            "Attack speed:  x" + String.format(Locale.ROOT, "%.2f", p.attackSpeed),
            "Spell power:  x" + String.format(Locale.ROOT, "%.2f", p.spellPower),
            "Move speed:  " + Math.round(p.moveSpeed),
            "Roll cooldown:  " + String.format(Locale.ROOT, "%.2fs", p.dodgeCooldown),
            "Roll distance:  " + Math.round(p.rollDistance()) + " px"
                + (p.level >= Player.ROLL_BONUS_LEVEL_2 ? "  (level " + Player.ROLL_BONUS_LEVEL_2 + " bonus)"
                    : p.level >= Player.ROLL_BONUS_LEVEL ? "  (level " + Player.ROLL_BONUS_LEVEL + " bonus, more at " + Player.ROLL_BONUS_LEVEL_2 + ")"
                    : "  (further at level " + Player.ROLL_BONUS_LEVEL + ")"),
        };
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], (float) (left + (i / 4) * 260), (float) (y + 28 + (i % 4) * 22));
        }

        y += 28 + 4 * 22 + 40;
        g.setFont(f14);
        g.setColor(new Color(170, 175, 195));
        g.drawString("UP / DOWN choose a command    ENTER attack or confirm    RIGHT opens the spell list    LEFT goes back",
            (float) left, (float) y);
        g.drawString("HOLD SHIFT opens the spell list directly (and keeps it open for repeat casts)", (float) left, (float) (y + 22));
        g.drawString("WASD move    SPACE roll    TAB lock on    Q release    E talk to a trainer", (float) left, (float) (y + 44));

        y += 92;
        g.setFont(f18b);
        g.setColor(Color.WHITE);
        g.drawString("Sound", (float) left, (float) y);
        String[] names = {"Music", "Effects"};
        int[] values = {w.audio.music, w.audio.sfx};
        for (int i = 0; i < 2; i++) {
            double ry = y + 30 + i * 26;
            boolean sel = w.pauseCursor == i;
            g.setFont(sel ? f14b : f14);
            g.setColor(sel ? new Color(255, 215, 90) : new Color(205, 205, 215));
            g.drawString((sel ? "> " : "  ") + names[i], (float) left, (float) ry);
            StringBuilder bar = new StringBuilder();
            for (int k = 0; k < AudioSettings.STEPS; k++) bar.append(k < values[i] ? '#' : '-');
            g.drawString("[" + bar + "]  " + values[i] * 10 + "%", (float) (left + 120), (float) ry);
        }
        g.setFont(f14);
        g.setColor(new Color(170, 175, 195));
        g.drawString("UP / DOWN choose    LEFT / RIGHT change    M mute" + (w.audio.muted ? "  (sound is off)" : ""), (float) left, (float) (y + 96));
    }

    private void drawGameOver(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 170);
        double cx = width / 2.0;
        g.setFont(f54b);
        centered(g, "YOU DIED", cx, height * 0.36, new Color(255, 90, 90));
        g.setFont(f18b);
        int secs = (int) w.time;
        if (w.tutorial != null) centered(g, "Don't worry, it was only the tutorial. The story starts over.", cx, height * 0.36 + 44, Color.WHITE);
        else centered(g, w.level.name + "   -   rooms cleared " + w.level.clearedRoomCount() + " / " + w.level.combatRoomCount() + "   -   character level " + w.player.level + "   -   " + w.kills + " kills   -   "
            + secs / 60 + ":" + String.format(Locale.ROOT, "%02d", secs % 60), cx, height * 0.36 + 44, Color.WHITE);
        g.setFont(f26b);
        centered(g, "Press R to try again", cx, height * 0.36 + 100, Util.alpha(Color.WHITE, 0.6 + 0.4 * Math.sin(System.nanoTime() / 3.0e8)));
    }

    // ------------------------------------------------------------------ text helpers

    private void centered(Graphics2D g, String s, double cx, double baseline, Color c) {
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - fm.stringWidth(s) / 2.0);
        boolean light = c.getRed() + c.getGreen() + c.getBlue() > 300;   // a drop shadow only helps light text
        if (light) {
            g.setColor(new Color(0, 0, 0, Math.min(200, c.getAlpha())));
            g.drawString(s, x + 1.5f, (float) baseline + 1.5f);
        }
        g.setColor(c);
        g.drawString(s, x, (float) baseline);
    }

    private void right(Graphics2D g, String s, double rightX, double baseline, Color c) {
        FontMetrics fm = g.getFontMetrics();
        g.setColor(c);
        g.drawString(s, (float) (rightX - fm.stringWidth(s)), (float) baseline);
    }

    private void wrapped(Graphics2D g, String text, double x, double y, double maxWidth, double lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                g.drawString(line.toString(), (float) x, (float) y);
                y += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) g.drawString(line.toString(), (float) x, (float) y);
    }
}
