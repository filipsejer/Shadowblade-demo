package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything drawn in screen space for the roguelike side of the game: the main menu, a run's HUD (XP bar, clock, skill
 * slots, arrows to chests), the level-up cards, a run's pause menu, the results screen and the Armory. The campaign's
 * HUD and menus stay in {@link Renderer}, which hands over to this class whenever a run (or the new main menu) is up.
 */
final class RunHud {
    private final Font f12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font f12b = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private final Font f14 = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Font f14b = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private final Font f16b = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private final Font f18b = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private final Font f22b = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    private final Font f30b = new Font(Font.SANS_SERIF, Font.BOLD, 30);
    private final Font f54b = new Font(Font.SANS_SERIF, Font.BOLD, 54);
    private final Font f72b = new Font(Font.SANS_SERIF, Font.BOLD, 72);
    private final Minimap minimap = new Minimap();

    private static final Color PANEL = new Color(22, 22, 30, 238);
    private static final Color GOLD = new Color(255, 214, 80);
    private static final Color XP = new Color(90, 190, 255);
    private static final Color DIM_TEXT = new Color(170, 172, 188);

    // ------------------------------------------------------------------ the HUD

    void drawHud(Graphics2D g, World w, int width, int height) {
        Run run = w.run;
        Player p = w.player;

        // XP: a bar across the whole top of the screen
        g.setColor(new Color(10, 12, 20, 220));
        g.fillRect(0, 0, width, 16);
        g.setColor(XP);
        g.fill(new Rectangle2D.Double(0, 2, width * Util.clamp((double) p.xp / p.xpNext, 0, 1), 12));
        g.setColor(new Color(255, 255, 255, 60));
        g.fill(new Rectangle2D.Double(0, 2, width * Util.clamp((double) p.xp / p.xpNext, 0, 1), 4));
        g.setFont(f12b);
        centered(g, "LV " + p.level, width / 2.0, 13, Color.WHITE);

        // health, gold, kills (top left)
        bar(g, 20, 26, 260, 20, p.hp / p.maxHp, p.hp < p.maxHp * 0.3 ? new Color(235, 70, 70) : new Color(70, 200, 90));
        g.setFont(f14b);
        g.setColor(Color.WHITE);
        g.drawString("HP  " + (int) Math.ceil(Math.max(0, p.hp)) + " / " + (int) p.maxHp, 28, 41);
        Art.frame("run.coin", w.time, 6).draw(g, 30, 70, 3, false);
        g.setFont(f16b);
        g.setColor(GOLD);
        g.drawString(String.valueOf(run.gold), 44, 70);
        g.setColor(new Color(235, 235, 245));
        g.drawString(w.kills + " kills", 120, 70);
        if (run.reviveAvailable && !run.reviveUsed) {
            g.setFont(f12b);
            g.setColor(new Color(255, 160, 70));
            g.drawString("PHOENIX READY", 210, 70);
        }

        // the clock (top centre)
        Enemy boss = w.boss();
        double left = Run.STAGE_TIME - run.stageTime;
        g.setFont(f30b);
        String clock = run.bossDead ? "CLEARED" : boss != null ? "BOSS" : Run.clock(Math.max(0, run.stageTime));
        centered(g, clock, width / 2.0, 52, run.bossDead ? new Color(150, 240, 150) : boss != null ? new Color(255, 110, 110) : Color.WHITE);
        g.setFont(f12);
        centered(g, "STAGE " + (run.stage + 1) + " / " + Run.STAGES + "   -   " + w.level.theme.title.toUpperCase(), width / 2.0, 70, new Color(215, 215, 228));
        if (!run.bossSpawned && left <= Run.BOSS_WARNING && left > 0) {
            g.setFont(f22b);
            double k = 0.5 + 0.5 * Math.sin(w.time * 10);
            centered(g, w.level.bossName + " APPROACHES   " + (int) Math.ceil(left), width / 2.0, 100, Util.alpha(new Color(255, 90, 90), 0.6 + 0.4 * k));
        }
        if (boss != null) {
            double bw = Math.min(560, width - 520), bx = (width - bw) / 2;
            bar(g, bx, 86, bw, 16, boss.hp / boss.maxHp, boss.phase2 ? new Color(255, 110, 60) : new Color(210, 45, 70));
            g.setFont(f12b);
            boolean lab = w.level.theme == Theme.LAB;
            centered(g, w.level.bossName + (lab ? "   -   STAGE " + (boss.phase2 ? 2 : 1) : ""), width / 2.0, 99, Color.WHITE);
        }

        minimap.draw(g, w, width);
        drawSkillSlots(g, p, height);
        drawArrows(g, w, run, width, height);

        g.setFont(f12);
        g.setColor(new Color(200, 200, 212, 170));
        String hint = "J  attack (hold)" + (p.rollUnlocked ? "    SPACE  roll" : "") + "    TAB  lock on    ESC  pause";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(hint, width - fm.stringWidth(hint) - 16, height - 14);

        if (w.noticeTimer > 0 && w.state == World.State.PLAYING) {
            double fade = Math.min(1, w.noticeTimer);
            g.setFont(f22b);
            centered(g, w.notice, width / 2.0, height - 150, Util.alpha(GOLD, fade));
            g.setFont(f14);
            if (!w.noticeHint.isEmpty()) centered(g, w.noticeHint, width / 2.0, height - 127, Util.alpha(Color.WHITE, fade));
        }
        if (w.bannerTimer > 0) {
            g.setFont(f54b);
            centered(g, w.banner, width / 2.0, height * 0.32, Util.alpha(Color.WHITE, Math.min(1, w.bannerTimer)));
        }
    }

    /** Your skills (big boxes, with their recharge) and passives (small ones), bottom left. */
    private void drawSkillSlots(Graphics2D g, Player p, int height) {
        double x = 18, y = height - 104;
        for (Perk k : Perk.values()) {
            if (k.kind != Perk.Kind.SKILL || p.perk[k.ordinal()] == 0) continue;
            int r = p.perk[k.ordinal()];
            double cd = Arsenal.baseCooldown(p, k) * p.cooldownMult;
            double frac = cd > 0 ? Util.clamp(p.skillCd[k.ordinal()] / cd, 0, 1) : 0;
            slot(g, k, r, x, y, 46, frac);
            x += 52;
        }
        if (x == 18) {
            g.setFont(f12);
            g.setColor(DIM_TEXT);
            g.drawString("Only your sword for now: level up to learn skills", (float) x, (float) (y + 28));
        }
        x = 18;
        y += 56;
        for (Perk k : Perk.values()) {
            if (k.kind != Perk.Kind.PASSIVE || p.perk[k.ordinal()] == 0) continue;
            slot(g, k, p.perk[k.ordinal()], x, y, 34, 0);
            x += 39;
        }
    }

    private void slot(Graphics2D g, Perk k, int rank, double x, double y, double size, double cooling) {
        RoundRectangle2D box = new RoundRectangle2D.Double(x, y, size, size, 9, 9);
        g.setColor(new Color(16, 16, 24, 225));
        g.fill(box);
        drawIcon(g, k, x + size / 2, y + size / 2 - 2, size * 0.78, rank >= Perk.EVOLVED);
        if (cooling > 0) {
            Shape saved = g.getClip();
            g.clip(box);
            g.setColor(new Color(0, 0, 0, 150));
            g.fill(new Arc2D.Double(x - size * 0.3, y - size * 0.3, size * 1.6, size * 1.6, 90, 360 * cooling, Arc2D.PIE));
            g.setClip(saved);
        }
        g.setColor(rank >= Perk.EVOLVED ? GOLD : Util.alpha(k.color, 0.8));
        g.setStroke(new BasicStroke(rank >= Perk.EVOLVED ? 2.6f : 1.6f));
        g.draw(box);
        g.setFont(f12b);
        String label = rank >= Perk.EVOLVED ? "MAX" : String.valueOf(rank);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(label, (float) (x + size - fm.stringWidth(label) - 2), (float) (y + size - 1));
        g.setColor(rank >= Perk.EVOLVED ? GOLD : Color.WHITE);
        g.drawString(label, (float) (x + size - fm.stringWidth(label) - 3), (float) (y + size - 2));
    }

    /** Arrows at the screen's edge pointing to chests and the portal when they're off-screen. */
    private void drawArrows(Graphics2D g, World w, Run run, int width, int height) {
        double left = Util.clamp(w.camX - width / 2.0, 0, Math.max(0, w.level.width - width));
        double top = Util.clamp(w.camY - height / 2.0, 0, Math.max(0, w.level.height - height));
        for (Pickup pk : run.pickups) {
            Color c = switch (pk.kind) {
                case ELITE_CHEST -> GOLD;
                case BOSS_CHEST -> new Color(255, 150, 60);
                case PORTAL -> new Color(200, 150, 255);
                default -> null;
            };
            if (c == null) continue;
            double sx = pk.x - left, sy = pk.y - top;
            if (sx > 30 && sx < width - 30 && sy > 30 && sy < height - 30) continue;
            double cx = width / 2.0, cy = height / 2.0;
            double ang = Math.atan2(sy - cy, sx - cx);
            double ex = Util.clamp(sx, 40, width - 40), ey = Util.clamp(sy, 110, height - 130);
            AffineTransform saved = g.getTransform();
            g.translate(ex, ey);
            g.rotate(ang);
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(16, 0);
            arrow.lineTo(-10, -11);
            arrow.lineTo(-5, 0);
            arrow.lineTo(-10, 11);
            arrow.closePath();
            g.setColor(new Color(0, 0, 0, 160));
            g.setStroke(new BasicStroke(4f));
            g.draw(arrow);
            g.setColor(c);
            g.fill(arrow);
            g.setTransform(saved);
        }
    }

    // ------------------------------------------------------------------ level-up choices

    void drawChoices(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 170);
        Run run = w.run;
        Player p = w.player;
        List<Perk.Choice> choices = run.choices;
        int n = choices.size();
        double cw = n >= 4 ? 236 : 262, ch = 340, gap = 22;
        double total = n * cw + (n - 1) * gap;
        double x0 = (width - total) / 2, y0 = Math.max(120, (height - ch) / 2 - 10);

        g.setFont(f54b);
        centered(g, run.choiceTitle, width / 2.0, y0 - 44, run.choiceTitle.startsWith("LEVEL") ? GOLD : new Color(255, 170, 80));
        g.setFont(f16b);
        String sub = run.choiceTitle.startsWith("LEVEL") ? "Level " + (p.level - run.pendingLevels + 1) + "   -   choose one"
            : "The chest holds a free upgrade   -   choose one";
        centered(g, sub, width / 2.0, y0 - 16, new Color(225, 225, 235));

        for (int i = 0; i < n; i++) {
            boolean sel = i == run.choiceCursor;
            double x = x0 + i * (cw + gap), y = y0 - (sel ? 12 : 0);
            drawCard(g, w, choices.get(i), x, y, cw, ch, sel, p);
        }

        double fy = y0 + ch + 36;
        g.setFont(f16b);
        String reroll = run.rerolls > 0 ? "R  reroll (" + run.rerolls + " left)" : "no rerolls left";
        centered(g, "A / D  choose       ENTER  take       " + reroll, width / 2.0, fy,
            run.choiceArm > 0 ? new Color(220, 220, 230, 110) : new Color(220, 220, 230));
        g.setFont(f12);
        centered(g, "Skills " + Perk.owned(p, Perk.Kind.SKILL) + " / " + Perk.SKILL_SLOTS + "     Passives " + Perk.owned(p, Perk.Kind.PASSIVE)
            + " / " + Perk.PASSIVE_SLOTS + "     A maxed skill evolves once you also own its partner passive", width / 2.0, fy + 24, DIM_TEXT);
        drawSkillSlots(g, p, height);
    }

    private void drawCard(Graphics2D g, World w, Perk.Choice c, double x, double y, double cw, double ch, boolean sel, Player p) {
        Perk k = c.perk();
        Color accent = k == null ? GOLD : c.evolution() ? GOLD : k.color;
        RoundRectangle2D card = new RoundRectangle2D.Double(x, y, cw, ch, 18, 18);
        if (sel) {
            double pulse = 0.5 + 0.5 * Math.sin(w.time * 6 + System.nanoTime() / 2e8);
            g.setColor(Util.alpha(accent, 0.25 + 0.2 * pulse));
            g.fill(new RoundRectangle2D.Double(x - 8, y - 8, cw + 16, ch + 16, 24, 24));
        }
        g.setColor(new Color(26, 26, 36, 245));
        g.fill(card);
        Shape saved = g.getClip();
        g.clip(card);
        g.setPaint(new GradientPaint((float) x, (float) y, Util.alpha(accent, 0.55), (float) x, (float) (y + 120), Util.alpha(accent, 0.0)));
        g.fill(new Rectangle2D.Double(x, y, cw, 120));
        g.setClip(saved);
        g.setColor(sel ? Color.WHITE : Util.alpha(accent, 0.7));
        g.setStroke(new BasicStroke(sel ? 3.5f : 2f));
        g.draw(card);

        String tag, name, text;
        if (k == null) {
            tag = "BONUS";
            name = c.rank() == -1 ? "Treasure" : "Rest";
            text = c.rank() == -1 ? "You've mastered everything. Take 40 gold." : "You've mastered everything. Heal 40% of your health.";
        } else {
            int now = p.perk[k.ordinal()];
            name = k.title(c.rank());
            text = c.rank() == 1 && k.kind == Perk.Kind.SKILL ? k.blurb + " " + k.text(1) : k.text(c.rank());
            if (c.evolution()) tag = "EVOLUTION";
            else if (now == 0) tag = k.kind == Perk.Kind.SKILL ? "NEW SKILL" : "NEW PASSIVE";
            else tag = "LEVEL " + now + "  >  " + c.rank();
        }

        g.setFont(f12b);
        centered(g, tag, x + cw / 2, y + 26, c.evolution() ? GOLD : tag.startsWith("NEW") ? new Color(140, 255, 160) : new Color(230, 230, 240));
        double icx = x + cw / 2, icy = y + 88;
        g.setColor(new Color(12, 12, 18, 230));
        g.fill(new Ellipse2D.Double(icx - 40, icy - 40, 80, 80));
        g.setColor(Util.alpha(accent, 0.9));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(icx - 40, icy - 40, 80, 80));
        if (k != null) drawIcon(g, k, icx, icy, 56, c.evolution());
        else drawCoinIcon(g, icx, icy, c.rank() == -1);

        g.setFont(f22b);
        centered(g, name, x + cw / 2, y + 160, Color.WHITE);
        if (k != null) {
            g.setFont(f12);
            centered(g, c.evolution() ? "evolved " + k.label : k.kind == Perk.Kind.SKILL ? "skill - fires by itself" : "passive", x + cw / 2, y + 178, DIM_TEXT);
        }
        g.setFont(f14);
        g.setColor(new Color(235, 235, 242));
        wrapped(g, text, x + 20, y + 206, cw - 40, 19, true);

        if (k != null && !c.evolution()) {                                  // rank pips
            int max = k.maxRank;
            double pw = 14, pg = 6, px0 = x + cw / 2 - (max * pw + (max - 1) * pg) / 2, py = y + ch - 28;
            for (int i = 0; i < max; i++) {
                RoundRectangle2D pip = new RoundRectangle2D.Double(px0 + i * (pw + pg), py, pw, 8, 4, 4);
                g.setColor(i < c.rank() - 1 ? accent : i == c.rank() - 1 ? Color.WHITE : new Color(60, 60, 72));
                g.fill(pip);
            }
            if (k.evolvable() && c.rank() == k.maxRank) {
                g.setFont(f12);
                centered(g, "evolves with " + k.partner().label, x + cw / 2, py - 8, Util.alpha(GOLD, 0.85));
            }
        }
    }

    private void drawCoinIcon(Graphics2D g, double cx, double cy, boolean coin) {
        if (coin) Art.frames("run.coin")[0].draw(g, cx, cy + 18, 6, false);
        else Art.frames("run.heart")[0].draw(g, cx, cy + 22, 5, false);
    }

    // ------------------------------------------------------------------ a run's pause menu

    void drawRunPause(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 190);
        Run run = w.run;
        Player p = w.player;
        double cx = width / 2.0;
        g.setFont(f54b);
        centered(g, "PAUSED", cx, height * 0.14, Color.WHITE);
        g.setFont(f14);
        centered(g, "Stage " + (run.stage + 1) + "   -   " + Run.clock(run.stageTime) + "   -   level " + p.level + "   -   " + w.kills + " kills   -   "
            + run.gold + " gold   -   " + run.loot.size() + " item" + (run.loot.size() == 1 ? "" : "s") + " found", cx, height * 0.14 + 30, new Color(210, 210, 222));

        double rowW = 360, rowH = 48, gap = 10, y0 = height * 0.14 + 64;
        for (int i = 0; i < World.RUN_PAUSE.length; i++) {
            boolean sel = w.pauseCursor == i;
            String label = World.RUN_PAUSE[i];
            String hint = switch (i) {
                case 0 -> "back to the fight";
                case 1 -> volume(w.audio.music) + "   LEFT / RIGHT";
                case 2 -> volume(w.audio.sfx) + "   LEFT / RIGHT";
                case 3 -> "CONTINUE on the main menu picks it up again";
                default -> w.confirmAbandon ? "press ENTER again: the run ends, you keep your loot" : "end the run now (you keep what you found)";
            };
            menuRow(g, cx, y0 + i * (rowH + gap), rowW, rowH, label, hint, sel, i == 4 && w.confirmAbandon ? new Color(255, 120, 110) : null);
        }

        // what you've built so far
        double y = y0 + World.RUN_PAUSE.length * (rowH + gap) + 20;
        g.setFont(f16b);
        centered(g, "YOUR BUILD", cx, y, Color.WHITE);
        List<String> lines = new ArrayList<>();
        for (Perk k : Perk.values()) {
            int r = p.perk[k.ordinal()];
            if (r > 0) lines.add(k.title(r) + (r >= Perk.EVOLVED ? "  (evolved)" : "  " + r + "/" + k.maxRank));
        }
        if (lines.isEmpty()) lines.add("just your sword");
        g.setFont(f12);
        int cols = 3;
        double colW = 250;
        for (int i = 0; i < lines.size(); i++) {
            double lx = cx - colW * cols / 2.0 + (i % cols) * colW + 10;
            g.setColor(new Color(215, 215, 228));
            g.drawString(lines.get(i), (float) lx, (float) (y + 22 + (i / cols) * 17));
        }
        double sy = y + 22 + ((lines.size() + cols - 1) / cols) * 17 + 14;
        g.setFont(f12);
        centered(g, String.format(Locale.ROOT, "Damage x%.2f   Skill power x%.2f   Attack speed x%.2f   Crit %d%%   Armor %d%%   Move %d   Pickup range %d   Combo %d hits",
            p.meleeMult, p.spellPower, p.attackSpeed, (int) Math.round(p.critChance * 100), (int) Math.round(p.armor * 100), Math.round(p.moveSpeed), Math.round(p.magnet), p.comboMax),
            cx, sy, DIM_TEXT);
    }

    private static String volume(int v) {
        StringBuilder bar = new StringBuilder();
        for (int k = 0; k < AudioSettings.STEPS; k++) bar.append(k < v ? '#' : '-');
        return "[" + bar + "]  " + v * 10 + "%";
    }

    // ------------------------------------------------------------------ the end of a run

    void drawRunEnd(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 205);
        Run run = w.run;
        Player p = w.player;
        double cx = width / 2.0;
        g.setFont(f72b);
        centered(g, run.victory ? "VICTORY!" : "DEFEATED", cx, height * 0.15, run.victory ? GOLD : new Color(255, 90, 90));
        g.setFont(f16b);
        centered(g, run.victory ? "All three bosses have fallen. The portal takes you home." : "You fell on stage " + (run.stage + 1) + ". What you found is yours to keep.",
            cx, height * 0.15 + 34, Color.WHITE);

        double y = height * 0.15 + 76;
        String[] stats = {
            "Stage reached   " + (run.stage + 1) + " / " + Run.STAGES,
            "Time survived   " + Run.clock(run.runTime),
            "Character level   " + p.level,
            "Kills   " + w.kills,
            "Elites slain   " + run.elitesKilled,
            "Bosses slain   " + run.bossesKilled,
        };
        g.setFont(f16b);
        for (int i = 0; i < stats.length; i++) {
            double lx = cx - 330 + (i % 3) * 230;
            g.setColor(new Color(225, 225, 235));
            g.drawString(stats[i], (float) lx, (float) (y + (i / 3) * 26));
        }
        y += 70;
        g.setFont(f22b);
        centered(g, "+" + run.gold + " gold", cx, y, GOLD);
        y += 36;

        List<Item> all = new ArrayList<>(run.loot);
        all.addAll(run.bonusLoot);
        g.setFont(f18b);
        centered(g, all.isEmpty() ? "No items found this time" : "LOOT", cx, y, Color.WHITE);
        y += 14;
        int cols = Math.min(3, Math.max(1, all.size()));
        double colW = 300;
        for (int i = 0; i < all.size() && i < 12; i++) {
            Item it = all.get(i);
            double ix = cx - cols * colW / 2 + (i % cols) * colW, iy = y + (i / cols) * 58;
            itemChip(g, it, ix + 6, iy, colW - 12, 50, run.bonusLoot.contains(it) ? (run.victory ? "bonus" : "consolation") : null);
        }
        y += ((Math.min(12, all.size()) + cols - 1) / cols) * 58 + 18;
        if (!run.records.isEmpty()) {
            g.setFont(f16b);
            centered(g, "NEW RECORD:  " + String.join("   -   ", run.records), cx, y, new Color(140, 240, 160));
            y += 28;
        }
        g.setFont(f22b);
        centered(g, "ENTER  back to the main menu", cx, Math.max(y + 20, height - 50), Util.alpha(Color.WHITE, 0.6 + 0.4 * Math.sin(System.nanoTime() / 3.0e8)));
    }

    /** A compact item row: icon, name in its rarity colour, slot and first stat line. */
    private void itemChip(Graphics2D g, Item it, double x, double y, double w, double h, String note) {
        RoundRectangle2D box = new RoundRectangle2D.Double(x, y, w, h, 10, 10);
        g.setColor(new Color(28, 28, 38, 235));
        g.fill(box);
        g.setColor(Util.alpha(it.rarity.color, 0.8));
        g.setStroke(new BasicStroke(2f));
        g.draw(box);
        Art.frames("item." + it.slot.name().toLowerCase())[0].draw(g, x + 26, y + h / 2, 2, false);
        g.setFont(f14b);
        g.setColor(it.rarity.color);
        g.drawString(it.name, (float) (x + 52), (float) (y + 20));
        if (note != null) {                                                   // a bonus item: a small tag in the corner
            g.setFont(f12b);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(GOLD);
            g.drawString(note.toUpperCase(), (float) (x + w - fm.stringWidth(note.toUpperCase()) - 10), (float) (y + 20));
        }
        g.setFont(f12);
        g.setColor(DIM_TEXT);
        g.drawString(it.rarity.label + " " + it.slot.label.toLowerCase() + "  -  " + it.lines().get(0), (float) (x + 52), (float) (y + 38));
    }

    // ------------------------------------------------------------------ the main menu

    void drawMainMenu(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 160);
        double cx = width / 2.0;
        g.setFont(f72b);
        centered(g, "SPELLBLADE", cx, height * 0.17, Color.WHITE);
        g.setFont(f22b);
        centered(g, "S U R V I V O R S", cx, height * 0.17 + 34, GOLD);
        g.setFont(f14);
        centered(g, "Hold off the horde. Level up, pick your powers, slay three bosses. Keep the loot.", cx, height * 0.17 + 60, new Color(205, 205, 218));

        Profile prof = w.profile;
        String[] hints = {
            w.confirmNew ? "press ENTER again: this abandons your saved run" : "a fresh run: stage 1, level 1, only your sword",
            w.savedRun != null ? w.savedRun : "no saved run",
            prof.items.isEmpty() ? "equipment you find ends up here" : prof.items.size() + " items   -   " + prof.equipped.size() + " equipped   -   " + prof.gold + " gold",
            "the original room-by-room adventure, with its opening story",
            "see you soon",
        };
        double rowW = 440, rowH = 56, gap = 12, y0 = height * 0.17 + 84;
        for (int i = 0; i < World.MAIN_MENU.length; i++) {
            boolean disabled = i == 1 && w.savedRun == null;
            Color override = disabled ? new Color(110, 110, 120) : i == 0 && w.confirmNew ? new Color(255, 140, 120) : null;
            menuRow(g, cx, y0 + i * (rowH + gap), rowW, rowH, World.MAIN_MENU[i], hints[i], w.menuCursor == i, override);
        }
        double y = y0 + World.MAIN_MENU.length * (rowH + gap) + 22;
        g.setFont(f14);
        String best = prof.runs == 0 ? "No runs yet - good luck out there."
            : "Runs " + prof.runs + "   -   Victories " + prof.victories + "   -   Best: stage " + Math.min(Run.STAGES, Math.max(1, prof.bestStage))
              + ", level " + prof.bestLevel + ", " + prof.bestKills + " kills, " + Run.clock(prof.bestTime);
        centered(g, best, cx, y, new Color(200, 200, 212));
        g.setFont(f12);
        centered(g, "W / S choose     ENTER confirm     M mute", cx, y + 24, DIM_TEXT);
    }

    // ------------------------------------------------------------------ the Armory

    void drawArmory(Graphics2D g, World w, int width, int height) {
        dim(g, width, height, 205);
        Profile prof = w.profile;
        double pw = Math.min(1220, width - 30), ph = Math.min(660, height - 30);
        double px = (width - pw) / 2, py = (height - ph) / 2;
        RoundRectangle2D panel = new RoundRectangle2D.Double(px, py, pw, ph, 18, 18);
        g.setColor(PANEL);
        g.fill(panel);
        g.setColor(Util.alpha(GOLD, 0.7));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(panel);
        g.setFont(f30b);
        g.setColor(GOLD);
        g.drawString("ARMORY", (float) (px + 26), (float) (py + 44));
        g.setFont(f14);
        g.setColor(DIM_TEXT);
        g.drawString("What you wear here goes with you into every new run.", (float) (px + 170), (float) (py + 40));
        Art.frame("run.coin", w.time, 6).draw(g, px + pw - 140, py + 42, 3, false);
        g.setFont(f22b);
        g.setColor(GOLD);
        g.drawString(String.valueOf(prof.gold), (float) (px + pw - 124), (float) (py + 44));

        // left: the six slots and what they add up to
        double lx = px + 24, ly = py + 70, lw = 320;
        g.setFont(f16b);
        g.setColor(Color.WHITE);
        g.drawString("EQUIPPED", (float) lx, (float) (ly + 4));
        for (int i = 0; i < Item.Slot.values().length; i++) {
            Item.Slot s = Item.Slot.values()[i];
            Item it = prof.equipped.get(s);
            double sy = ly + 16 + i * 52;
            RoundRectangle2D box = new RoundRectangle2D.Double(lx, sy, lw, 46, 10, 10);
            g.setColor(new Color(30, 30, 42));
            g.fill(box);
            g.setColor(it == null ? new Color(70, 70, 84) : Util.alpha(it.rarity.color, 0.85));
            g.setStroke(new BasicStroke(1.8f));
            g.draw(box);
            Art.frames("item." + s.name().toLowerCase())[0].draw(g, lx + 25, sy + 23, 2, false, 0, it == null ? 0.3f : 1f);
            g.setFont(f14b);
            g.setColor(it == null ? new Color(120, 120, 132) : it.rarity.color);
            g.drawString(it == null ? s.label + "  -  empty" : it.name + (it.upgrade > 0 ? "  +" + it.upgrade : ""), (float) (lx + 50), (float) (sy + 20));
            g.setFont(f12);
            g.setColor(DIM_TEXT);
            g.drawString(it == null ? "" : it.lines().get(0), (float) (lx + 50), (float) (sy + 37));
        }
        double ty = ly + 16 + 6 * 52 + 22;
        g.setFont(f14b);
        g.setColor(Color.WHITE);
        g.drawString("TOTAL BONUSES", (float) lx, (float) ty);
        g.setFont(f12);
        java.util.EnumMap<Item.Stat, Double> sum = new java.util.EnumMap<>(Item.Stat.class);
        List<String> uniques = new ArrayList<>();
        for (Item it : prof.equipped.values()) {
            for (Item.Stat s : it.stats.keySet()) sum.merge(s, it.value(s), Double::sum);
            if (it.unique != null) uniques.add(it.unique.text);
        }
        int line = 0;
        for (var e : sum.entrySet()) {
            g.setColor(new Color(200, 225, 200));
            g.drawString(e.getKey().format(e.getValue()), (float) (lx + (line % 2) * 165), (float) (ty + 20 + (line / 2) * 17));
            line++;
        }
        double uy = ty + 20 + ((line + 1) / 2) * 17 + 4;
        for (String u : uniques) {
            g.setColor(new Color(255, 180, 90));
            g.drawString(u, (float) lx, (float) uy);
            uy += 17;
        }
        if (sum.isEmpty()) {
            g.setColor(DIM_TEXT);
            g.drawString("Nothing equipped yet.", (float) lx, (float) (ty + 20));
        }

        // middle: the bag
        List<Item> bag = prof.sorted();
        double mx = lx + lw + 24, my = py + 70, mw = 420, rowH = 36;
        g.setFont(f16b);
        g.setColor(Color.WHITE);
        g.drawString("YOUR ITEMS  (" + bag.size() + " / " + Profile.MAX_ITEMS + ")", (float) mx, (float) (my + 4));
        int visible = (int) ((ph - 150) / rowH);
        int first = Math.max(0, Math.min(w.armoryCursor - visible / 2, bag.size() - visible));
        if (bag.isEmpty()) {
            g.setFont(f14);
            g.setColor(DIM_TEXT);
            wrapped(g, "Nothing here yet. Elites sometimes drop items, every boss chest holds one, and a victory adds a rare one on top. "
                + "You keep everything you found even when a run ends in defeat.", mx, my + 40, mw, 20, false);
        }
        for (int i = first; i < bag.size() && i < first + visible; i++) {
            Item it = bag.get(i);
            boolean sel = i == w.armoryCursor;
            double ry = my + 16 + (i - first) * rowH;
            RoundRectangle2D row = new RoundRectangle2D.Double(mx, ry, mw, rowH - 4, 8, 8);
            g.setColor(sel ? new Color(56, 60, 84) : new Color(32, 32, 44));
            g.fill(row);
            if (sel) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(2f));
                g.draw(row);
            }
            Art.frames("item." + it.slot.name().toLowerCase())[0].draw(g, mx + 19, ry + 16, 2, false);
            g.setFont(f14b);
            g.setColor(it.rarity.color);
            g.drawString(it.name + (it.upgrade > 0 ? "  +" + it.upgrade : ""), (float) (mx + 38), (float) (ry + 21));
            g.setFont(f12);
            g.setColor(DIM_TEXT);
            FontMetrics fm = g.getFontMetrics();
            String tag = (prof.isEquipped(it) ? "EQUIPPED   " : "") + it.slot.label;
            g.setColor(prof.isEquipped(it) ? new Color(140, 240, 160) : DIM_TEXT);
            g.drawString(tag, (float) (mx + mw - fm.stringWidth(tag) - 12), (float) (ry + 21));
        }
        if (bag.size() > visible) {
            g.setFont(f12);
            g.setColor(DIM_TEXT);
            g.drawString((first + 1) + "-" + Math.min(bag.size(), first + visible) + " of " + bag.size(), (float) mx, (float) (py + ph - 44));
        }

        // right: the selected item
        double dx = mx + mw + 24, dw = px + pw - dx - 24, dy = py + 70;
        if (!bag.isEmpty()) {
            Item it = bag.get(Math.min(w.armoryCursor, bag.size() - 1));
            RoundRectangle2D box = new RoundRectangle2D.Double(dx, dy, dw, 360, 14, 14);
            g.setColor(new Color(30, 30, 42));
            g.fill(box);
            g.setColor(Util.alpha(it.rarity.color, 0.9));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(box);
            Art.frames("item." + it.slot.name().toLowerCase())[0].draw(g, dx + dw / 2, dy + 68, 4, false);
            g.setFont(f22b);
            centered(g, it.name + (it.upgrade > 0 ? " +" + it.upgrade : ""), dx + dw / 2, dy + 130, it.rarity.color);
            g.setFont(f12);
            centered(g, it.rarity.label + " " + it.slot.label.toLowerCase() + "   -   found on stage " + Math.min(3, it.tier + 1)
                + (it.tier >= 3 ? " (victory)" : ""), dx + dw / 2, dy + 150, DIM_TEXT);
            double sy = dy + 180;
            g.setFont(f14);
            for (String l : it.lines()) {
                g.setColor(it.unique != null && l.equals(it.unique.text) ? new Color(255, 180, 90) : new Color(225, 240, 225));
                sy = wrapped(g, l, dx + 22, sy, dw - 44, 19, false) + 22;
            }
            Item worn = prof.equipped.get(it.slot);
            if (worn != null && worn != it) {
                g.setFont(f12);
                g.setColor(DIM_TEXT);
                g.drawString("Replaces " + worn.name + (worn.upgrade > 0 ? " +" + worn.upgrade : ""), (float) (dx + 22), (float) (sy + 4));
            }
            g.setFont(f14b);
            double by = dy + 380;
            g.setColor(Color.WHITE);
            g.drawString(prof.isEquipped(it) ? "ENTER  take off" : "ENTER  equip", (float) dx, (float) by);
            g.setColor(it.upgrade >= Item.MAX_UPGRADE ? DIM_TEXT : prof.gold >= it.upgradeCost() ? GOLD : new Color(220, 110, 110));
            g.drawString(it.upgrade >= Item.MAX_UPGRADE ? "U  fully upgraded" : "U  upgrade to +" + (it.upgrade + 1) + "  (" + it.upgradeCost() + " gold, +12% stats)",
                (float) dx, (float) (by + 22));
            g.setColor(w.confirmSalvage ? new Color(255, 130, 120) : new Color(200, 200, 210));
            g.drawString("X  salvage for " + it.salvageValue() + " gold" + (w.confirmSalvage ? "  -  press again" : ""), (float) dx, (float) (by + 44));
        }

        if (!w.armoryMessage.isEmpty()) {
            g.setFont(f16b);
            centered(g, w.armoryMessage, px + pw / 2, py + ph - 42, GOLD);
        }
        g.setFont(f14);
        centered(g, "W / S choose      ENTER equip / take off      U upgrade      X salvage      ESC back", px + pw / 2, py + ph - 16, new Color(190, 190, 202));
    }

    // ------------------------------------------------------------------ perk icons

    /**
     * A small vector picture for a perk, centred on (cx, cy), about {@code size} across. Evolved skills get a gold ring.
     */
    void drawIcon(Graphics2D g, Perk k, double cx, double cy, double size, boolean evolved) {
        AffineTransform saved = g.getTransform();
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(cx, cy);
        g.scale(size / 32.0, size / 32.0);
        Color c = k.color;
        BasicStroke thick = new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke thin = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        if (evolved) {
            g.setColor(new Color(255, 214, 80, 120));
            g.fill(new Ellipse2D.Double(-16, -16, 32, 32));
        }
        switch (k) {
            case CRESCENT_WAVE -> {
                Path2D moon = new Path2D.Double();
                moon.append(new Arc2D.Double(-12, -13, 24, 26, -70, 140, Arc2D.OPEN), false);
                moon.append(new Arc2D.Double(-6, -9, 14, 18, 70, -140, Arc2D.OPEN), true);
                moon.closePath();
                g.setColor(c);
                g.fill(moon);
                g.setColor(Color.WHITE);
                g.setStroke(thin);
                g.draw(new Arc2D.Double(-12, -13, 24, 26, -60, 120, Arc2D.OPEN));
            }
            case FIREBALL -> {
                Path2D tail = new Path2D.Double();
                tail.moveTo(-14, -12);
                tail.quadTo(-4, -2, 2, -6);
                tail.lineTo(8, 6);
                tail.quadTo(-2, 2, -14, -12);
                g.setColor(new Color(220, 70, 30));
                g.fill(tail);
                g.setColor(c);
                g.fill(new Ellipse2D.Double(-5, -5, 18, 18));
                g.setColor(new Color(255, 230, 120));
                g.fill(new Ellipse2D.Double(0, 0, 9, 9));
            }
            case LIGHTNING -> {
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(4, -15);
                bolt.lineTo(-8, 2);
                bolt.lineTo(-1, 2);
                bolt.lineTo(-5, 15);
                bolt.lineTo(9, -3);
                bolt.lineTo(2, -3);
                bolt.closePath();
                g.setColor(c);
                g.fill(bolt);
                g.setColor(new Color(120, 100, 20));
                g.setStroke(new BasicStroke(1.2f));
                g.draw(bolt);
            }
            case ICE_STORM -> {
                g.setColor(c);
                g.setStroke(thick);
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI / 3;
                    g.draw(new Line2D.Double(-Math.cos(a) * 13, -Math.sin(a) * 13, Math.cos(a) * 13, Math.sin(a) * 13));
                }
                g.setColor(Color.WHITE);
                g.setStroke(thin);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3, bx = Math.cos(a) * 8, by = Math.sin(a) * 8;
                    g.draw(new Line2D.Double(bx, by, bx + Math.cos(a + 0.8) * 4, by + Math.sin(a + 0.8) * 4));
                }
            }
            case ORBIT_BLADES -> {
                g.setColor(Util.alpha(c, 0.5));
                g.setStroke(thin);
                g.draw(new Ellipse2D.Double(-11, -11, 22, 22));
                g.setColor(c);
                g.setStroke(thick);
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3 - 0.3, bx = Math.cos(a) * 11, by = Math.sin(a) * 11;
                    g.draw(new Line2D.Double(bx, by, bx + Math.cos(a + Math.PI / 2) * 8, by + Math.sin(a + Math.PI / 2) * 8));
                }
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            case HOLY_AURA -> {
                g.setColor(Util.alpha(c, 0.35));
                g.fill(new Ellipse2D.Double(-14, -14, 28, 28));
                g.setColor(c);
                g.setStroke(thin);
                g.draw(new Ellipse2D.Double(-14, -14, 28, 28));
                g.draw(new Ellipse2D.Double(-8, -8, 16, 16));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            case HEALING -> {
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(-4, -13, 8, 26, 3, 3));
                g.fill(new RoundRectangle2D.Double(-13, -4, 26, 8, 3, 3));
            }
            case ROLL -> {
                g.setColor(c);
                g.setStroke(thick);
                g.draw(new Arc2D.Double(-11, -11, 22, 22, 120, 260, Arc2D.OPEN));
                Path2D head = new Path2D.Double();
                head.moveTo(-11, -9);
                head.lineTo(-3, -13);
                head.lineTo(-5, -4);
                head.closePath();
                g.fill(head);
            }
            case COMBO -> {
                g.setColor(c);
                for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Double(-13 + i * 9, 5, 7, 7));
                g.setStroke(thick);
                g.draw(new Line2D.Double(-12, -2, 12, -12));
            }
            case BLADE -> sword(g, c, thick);
            case HASTE -> {
                g.setColor(c);
                g.setStroke(thick);
                for (int i = 0; i < 2; i++) {
                    Path2D chev = new Path2D.Double();
                    chev.moveTo(-11 + i * 9, -10);
                    chev.lineTo(-3 + i * 9, 0);
                    chev.lineTo(-11 + i * 9, 10);
                    g.draw(chev);
                }
            }
            case REACH -> {
                g.setColor(c);
                g.setStroke(thick);
                g.draw(new Line2D.Double(-13, 0, 13, 0));
                g.draw(new Line2D.Double(-13, 0, -7, -6));
                g.draw(new Line2D.Double(-13, 0, -7, 6));
                g.draw(new Line2D.Double(13, 0, 7, -6));
                g.draw(new Line2D.Double(13, 0, 7, 6));
            }
            case VAMPIRE -> {
                Path2D drop = new Path2D.Double();
                drop.moveTo(0, -14);
                drop.quadTo(12, 4, 0, 13);
                drop.quadTo(-12, 4, 0, -14);
                g.setColor(c);
                g.fill(drop);
                g.setColor(new Color(255, 190, 200));
                g.fill(new Ellipse2D.Double(-5, 0, 4, 6));
            }
            case POWER -> {
                g.setColor(c);
                g.fill(star(0, 0, 14, 5, 4));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            case CASTING -> {
                g.setColor(c);
                g.setStroke(thick);
                g.draw(new Ellipse2D.Double(-12, -12, 24, 24));
                g.draw(new Line2D.Double(0, 0, 0, -8));
                g.draw(new Line2D.Double(0, 0, 6, 3));
            }
            case VITALITY -> {
                Path2D heart = new Path2D.Double();
                heart.moveTo(0, 12);
                heart.curveTo(-18, 0, -8, -16, 0, -6);
                heart.curveTo(8, -16, 18, 0, 0, 12);
                g.setColor(c);
                g.fill(heart);
            }
            case SWIFT -> {
                g.setColor(c);
                g.setStroke(thick);
                for (int i = 0; i < 3; i++) g.draw(new Line2D.Double(-13 + i * 3, -8 + i * 8, 8 + i * 3, -8 + i * 8));
                g.fill(new Ellipse2D.Double(7, -12, 8, 8));
            }
            case MAGNET -> {
                g.setColor(c);
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(-10, -12, 20, 20, 180, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(-10, -2, -10, -12));
                g.draw(new Line2D.Double(10, -2, 10, -12));
                g.setColor(Color.WHITE);
                g.fill(new Rectangle2D.Double(-13, -15, 6, 4));
                g.fill(new Rectangle2D.Double(7, -15, 6, 4));
            }
            case WISDOM -> {
                g.setColor(c);
                Path2D book = new Path2D.Double();
                book.moveTo(0, -8);
                book.lineTo(-14, -12);
                book.lineTo(-14, 10);
                book.lineTo(0, 13);
                book.lineTo(14, 10);
                book.lineTo(14, -12);
                book.closePath();
                g.fill(book);
                g.setColor(new Color(20, 30, 60));
                g.setStroke(thin);
                g.draw(new Line2D.Double(0, -8, 0, 13));
            }
            case ARMOR -> {
                Path2D shield = new Path2D.Double();
                shield.moveTo(0, -14);
                shield.lineTo(12, -9);
                shield.quadTo(12, 7, 0, 14);
                shield.quadTo(-12, 7, -12, -9);
                shield.closePath();
                g.setColor(c);
                g.fill(shield);
                g.setColor(new Color(90, 100, 120));
                g.setStroke(thin);
                g.draw(new Line2D.Double(0, -10, 0, 10));
            }
            case REGEN -> {
                Path2D leaf = new Path2D.Double();
                leaf.moveTo(-10, 12);
                leaf.quadTo(-12, -12, 12, -12);
                leaf.quadTo(12, 10, -10, 12);
                g.setColor(c);
                g.fill(leaf);
                g.setColor(new Color(40, 110, 50));
                g.setStroke(thin);
                g.draw(new Line2D.Double(-10, 12, 6, -6));
            }
            case CRIT -> {
                g.setColor(c);
                g.setStroke(thick);
                g.draw(new Ellipse2D.Double(-10, -10, 20, 20));
                g.draw(new Line2D.Double(0, -15, 0, -5));
                g.draw(new Line2D.Double(0, 5, 0, 15));
                g.draw(new Line2D.Double(-15, 0, -5, 0));
                g.draw(new Line2D.Double(5, 0, 15, 0));
            }
        }
        g.setTransform(saved);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
    }

    private static void sword(Graphics2D g, Color c, BasicStroke thick) {
        g.setColor(c);
        g.setStroke(thick);
        g.draw(new Line2D.Double(-10, 10, 12, -12));
        g.setColor(new Color(255, 214, 80));
        g.draw(new Line2D.Double(-12, 2, -2, 12));
        g.setColor(new Color(140, 90, 50));
        g.draw(new Line2D.Double(-10, 10, -14, 14));
    }

    private static Shape star(double cx, double cy, double outer, double inner, int points) {
        Path2D s = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2 + i * Math.PI / points, r = i % 2 == 0 ? outer : inner;
            if (i == 0) s.moveTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            else s.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        s.closePath();
        return s;
    }

    // ------------------------------------------------------------------ helpers

    private void dim(Graphics2D g, int width, int height, int alpha) {
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(0, 0, width, height);
    }

    private void bar(Graphics2D g, double x, double y, double w, double h, double frac, Color fill) {
        g.setColor(new Color(20, 20, 22, 210));
        g.fill(new Rectangle2D.Double(x - 1.5, y - 1.5, w + 3, h + 3));
        g.setColor(fill);
        g.fill(new Rectangle2D.Double(x, y, w * Util.clamp(frac, 0, 1), h));
    }

    /** One row of a keyboard menu: a pill with a title and a hint; {@code override} recolours the title (disabled, warning). */
    private void menuRow(Graphics2D g, double cx, double y, double w, double h, String title, String hint, boolean selected, Color override) {
        RoundRectangle2D row = new RoundRectangle2D.Double(cx - w / 2, y, w, h, 14, 14);
        g.setColor(selected ? new Color(52, 66, 104, 240) : new Color(24, 24, 30, 220));
        g.fill(row);
        g.setColor(selected ? Color.WHITE : new Color(95, 100, 125));
        g.setStroke(new BasicStroke(selected ? 3f : 1.8f));
        g.draw(row);
        g.setFont(f22b);
        centered(g, title, cx, y + h * 0.5 + 3, override != null ? override : selected ? new Color(255, 225, 120) : new Color(200, 205, 220));
        g.setFont(f12);
        centered(g, hint, cx, y + h - 9, selected ? new Color(220, 220, 230) : new Color(140, 140, 150));
    }

    private void centered(Graphics2D g, String s, double cx, double baseline, Color c) {
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - fm.stringWidth(s) / 2.0);
        if (c.getRed() + c.getGreen() + c.getBlue() > 300) {
            g.setColor(new Color(0, 0, 0, Math.min(200, c.getAlpha())));
            g.drawString(s, x + 1.5f, (float) baseline + 1.5f);
        }
        g.setColor(c);
        g.drawString(s, x, (float) baseline);
    }

    /** Word-wraps {@code text} into lines at most {@code maxWidth} wide; returns the last line's baseline. */
    private double wrapped(Graphics2D g, String text, double x, double y, double maxWidth, double lineHeight, boolean centre) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            double lx = centre ? x + (maxWidth - fm.stringWidth(l)) / 2 : x;
            g.drawString(l, (float) lx, (float) (y + i * lineHeight));
        }
        return y + (lines.size() - 1) * lineHeight;
    }
}
