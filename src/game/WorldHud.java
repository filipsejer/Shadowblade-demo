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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * What's drawn in screen space while you're out in the world (not in a fight): the HUD (gold, skill points, where you
 * are and what to do next), the pause menu, Ranger Ash's training, Bramble's table, and the card before a challenge.
 * All in the menus' look ({@link MenuStyle}). A fight's screens are {@link RunHud}'s.
 */
final class WorldHud {
    private final Minimap minimap = new Minimap();
    private final MenuStyle.Glide pauseGlide = new MenuStyle.Glide(), trainerGlide = new MenuStyle.Glide(), shopGlide = new MenuStyle.Glide();

    // ------------------------------------------------------------------ the HUD

    void draw(Graphics2D g, World w, int width, int height) {
        if (w.adventure == null) return;
        MenuStyle.antialias(g);
        Adventure a = w.adventure;
        double s = Math.max(0.85, MenuStyle.scale(height));

        // gold and skill points, top left
        Art.frame("run.coin", w.time, 6).draw(g, 34 * s, 42 * s, 3 * s, false);
        g.setFont(MenuStyle.serif(Font.BOLD, 22 * s, 0.02));
        MenuStyle.shadowed(g, String.valueOf(w.profile.gold), 50 * s, 42 * s, GOLD);
        if (a.skillPoints > 0) {
            g.setFont(MenuStyle.serif(Font.BOLD, 18 * s, 0.02));
            MenuStyle.shadowed(g, a.skillPoints + " skill point" + (a.skillPoints == 1 ? "" : "s"), 24 * s, 70 * s, new Color(150, 230, 255));
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            MenuStyle.shadowed(g, Worlds.trainer(w.level.world) + " can turn them into upgrades", 24 * s, 88 * s, DIM);
        }

        // where you are, and what to do next (top centre)
        Level.Room here = w.area();
        String place = here != null ? here.name : w.level.name;
        g.setFont(MenuStyle.serif(Font.BOLD, 22 * s, 0.12));
        MenuStyle.centred(g, place, width / 2.0, 38 * s, new Color(240, 232, 214));
        String goal = Story.objective(a);
        g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
        FontMetrics fm = g.getFontMetrics();
        double gw = fm.stringWidth(goal);
        MenuStyle.diamond(g, width / 2.0 - gw / 2 - 12 * s, 59 * s, 4 * s, GOLD);
        MenuStyle.centred(g, goal, width / 2.0, 64 * s, new Color(255, 226, 160));

        if (w.mapZoom <= 0) minimap.draw(g, w, width, height);   // (held open, the Renderer draws it over everything else)

        g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
        String hint = "W A S D  move     E  talk / open / enter     ENTER  swing     SPACE  roll     TAB  map (hold)     ESC  menu";
        g.setColor(new Color(200, 200, 212, 170));
        g.drawString(hint, (float) (width - g.getFontMetrics().stringWidth(hint) - 16), (float) (height - 14));

        if (w.noticeTimer > 0 && w.state == World.State.PLAYING && !w.dialogue.active()) {
            double fade = Math.min(1, w.noticeTimer);
            g.setFont(MenuStyle.serif(Font.BOLD, 24 * s, 0.04));
            MenuStyle.centred(g, w.notice, width / 2.0, height - 150 * s, Util.alpha(GOLD, fade));
            g.setFont(MenuStyle.sans(Font.PLAIN, 14 * s));
            if (!w.noticeHint.isEmpty()) MenuStyle.centred(g, w.noticeHint, width / 2.0, height - 126 * s, Util.alpha(Color.WHITE, fade));
        }
        if (w.bannerTimer > 0 && w.state == World.State.PLAYING && !w.dialogue.stopsWorld()) {
            double k = Math.min(1, w.bannerTimer);
            g.setFont(MenuStyle.serif(Font.BOLD, 46 * s, 0.14));
            MenuStyle.centred(g, w.banner, width / 2.0, height * 0.3, Util.alpha(new Color(255, 236, 200), k));
        }
    }

    // ------------------------------------------------------------------ the pause menu

    /** The world's pause: where you are, the rows (volumes as sliders), and a card with your journey so far. */
    void drawPause(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 165);
        g.setPaint(new GradientPaint(0, 0, new Color(6, 6, 16, 150), (float) (width * 0.55), 0, new Color(6, 6, 16, 0)));
        g.fillRect(0, 0, (int) (width * 0.55) + 1, height);
        Adventure a = w.adventure;
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);

        double base = height * 0.2 + 12 * s;
        Rectangle2D hb = MenuStyle.heading(g, "PAUSED", x0, base, 76 * s, s).getBounds2D();
        Level.Room here = w.area();
        MenuStyle.ruled(g, here != null ? here.name : w.level.name, hb.getCenterX(), base + 36 * s, hb.getX(), hb.getMaxX(), 17 * s, s);
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        MenuStyle.shadowed(g, w.profile.gold + " gold" + DOT + a.skillPoints + " skill point" + (a.skillPoints == 1 ? "" : "s") + DOT
            + w.profile.items.size() + " item" + (w.profile.items.size() == 1 ? "" : "s"), x0, base + 66 * s, new Color(214, 206, 228, 220));

        String[] rows = w.pauseRows();
        double y0 = base + 128 * s, rowH = 52 * s, rowSize = 27 * s;
        int sel = Math.min(w.pauseCursor, rows.length - 1);
        String[] details = new String[rows.length];
        for (int i = 0; i < rows.length; i++) if (rows[i].equals("SAVE & QUIT")) details[i] = "saved for CONTINUE";
        MenuStyle.rows(g, pauseGlide, rows, details, null, sel, false, x0, y0, rowH, rowSize, 470 * s, s, w.time);
        g.setFont(MenuStyle.serif(Font.BOLD, rowSize, 0.08));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < rows.length; i++) {
            if (!rows[i].equals("MUSIC") && !rows[i].equals("EFFECTS")) continue;
            double lx = x0 + pauseGlide.slide(i) + fm.stringWidth(rows[i]) + 22 * s, ly = y0 + i * rowH + rowH * 0.18 - 9 * s;
            MenuStyle.slider(g, lx, ly, rows[i].equals("MUSIC") ? w.audio.music : w.audio.sfx, sel == i, s);
        }
        String info = switch (rows[sel]) {
            case "RESUME" -> "Back to " + Worlds.home(w.level.world) + ".";
            case "EQUIPMENT" -> "Wear, upgrade and salvage the gear you've found and bought.";
            case "MUSIC" -> "The music's volume. LEFT and RIGHT change it; M mutes everything.";
            case "EFFECTS" -> "The volume of the sound effects. LEFT and RIGHT change it.";
            default -> "Save the game and go back to the main menu. CONTINUE picks it up right here.";
        };
        MenuStyle.infoLine(g, info, x0, y0 + rows.length * rowH + 12 * s, 380 * s, s, false);
        MenuStyle.keys(g, x0, height - 30 * s, s, new String[][]{{"W", "S"}, {"A", "D"}, {"ENTER"}, {"ESC"}}, new String[]{"Navigate", "Volume", "Select", "Resume"});
        drawJourney(g, w, width, height, s);
    }

    /** The pause menu's card: what to do next, the challenges, and what Ash has taught you. */
    private void drawJourney(Graphics2D g, World w, int width, int height, double s) {
        Adventure a = w.adventure;
        int learned = 0;
        for (Mastery m : Mastery.values()) if (a.rank(m) > 0) learned++;
        List<Challenge> known = new java.util.ArrayList<>();
        for (Challenge c : Challenge.values()) if (c.world.equals(w.level.world)) known.add(c);   // only this world's
        double cw = 392 * s, ch = (196 + known.size() * 24 + Math.max(1, learned) * 21) * s;
        double x = width - cw - 40 * s, y = (height - ch) / 2 + 20 * s;
        MenuStyle.card(g, x, y, cw, ch, null);
        double px = x + 20 * s, right = x + cw - 20 * s, py = y + 28 * s;
        MenuStyle.label(g, "YOUR JOURNEY", px, py, s, GOLD);
        g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
        double ly = MenuStyle.wrap(g, Story.objective(a) + ".", px, py + 28 * s, cw - 40 * s, 19 * s, new Color(255, 226, 160));

        ly += 34 * s;
        MenuStyle.label(g, "CHALLENGES IN " + Worlds.home(w.level.world).toUpperCase(), px, ly, s, DIM);
        ly += 24 * s;
        for (Challenge c : known) {
            int n = a.clears(c);
            boolean asked = n > 0 || w.gateOpen(c);                         // nobody has asked you yet: you don't know what it is
            MenuStyle.diamond(g, px + 4 * s, ly - 5 * s, 4 * s, n > 0 ? new Color(140, 240, 160) : asked ? GOLD : new Color(80, 78, 96));
            g.setFont(MenuStyle.sans(Font.BOLD, 13 * s));
            MenuStyle.shadowed(g, asked ? c.title : "???", px + 16 * s, ly, asked ? Color.WHITE : DIM);
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            String st = n > 0 ? "cleared " + n + "x" : w.gateOpen(c) ? "open" : "not yet";
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, st, right - fm.stringWidth(st), ly, n > 0 ? new Color(140, 240, 160) : DIM);
            ly += 24 * s;
        }

        ly += 14 * s;
        MenuStyle.label(g, "MASTERED", px, ly, s, DIM);
        ly += 22 * s;
        if (learned == 0) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 13 * s, 0));
            MenuStyle.shadowed(g, "Nothing yet. Skill points come from challenges and hidden places.", px, ly, DIM);
        }
        for (Mastery m : Mastery.values()) {
            if (a.rank(m) == 0) continue;
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.shadowed(g, m.label, px + 16 * s, ly, TEXT);
            MenuStyle.diamond(g, px + 4 * s, ly - 5 * s, 4 * s, m.color);
            String r = a.rank(m) + " / " + m.maxRank;
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, r, right - fm.stringWidth(r), ly, a.rank(m) == m.maxRank ? GOLD : TEXT);
            ly += 21 * s;
        }
    }

    // ------------------------------------------------------------------ Ranger Ash

    /** The training post: the masteries in a card on the left, Ash and the selected one's details on the right. */
    void drawTrainer(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 175);
        Adventure a = w.adventure;
        Color accent = new Color(255, 170, 90);
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);
        double base = Math.max(96 * s, height * 0.5 - 260 * s);
        String world = w.level.world;
        Rectangle2D hb = MenuStyle.heading(g, Worlds.trainer(world).toUpperCase(), x0, base, 58 * s, s).getBounds2D();
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        String motto = switch (world) {
            case Worlds.CITY -> "\"Elegance is just efficiency with better manners. En garde.\"";
            case Worlds.LAB -> "\"PREPARE TO BE IMPROVED. RESISTANCE IS ADEQUATE.\"";
            default -> "\"Every fight teaches you something. Let's make it stick.\"";
        };
        MenuStyle.shadowed(g, motto, hb.getMaxX() + 28 * s, base - 22 * s, new Color(214, 206, 228, 225));
        MenuStyle.label(g, "PERMANENT UPGRADES" + DOT + "FOR EVERY FIGHT", hb.getMaxX() + 28 * s, base - 2 * s, s, Util.alpha(accent, 0.95));
        points(g, String.valueOf(a.skillPoints), "SKILL POINTS", a.skillPoints > 0, width, base, s);

        Mastery[] all = Mastery.values();
        int sel = Math.min(w.trainerCursor, all.length - 1);
        double top = base + 42 * s, bottom = Math.min(height - 62 * s, top + 470 * s), left = 40 * s, total = width - 80 * s, gap = 18 * s;
        double lw = total * 0.56, rw = total - lw - gap;
        MenuStyle.card(g, left, top, lw, bottom - top, null);
        MenuStyle.label(g, "WHAT " + (Worlds.CITY.equals(world) ? "SABLE" : Worlds.LAB.equals(world) ? "BRASS" : "ASH") + " TEACHES", left + 20 * s, top + 28 * s, s, GOLD);
        double rowH = Math.min(52 * s, (bottom - top - 56 * s) / all.length), listTop = top + 42 * s;
        selectionBar(g, trainerGlide.step(sel, all.length, listTop + sel * rowH), left, lw, rowH, s);
        for (int i = 0; i < all.length; i++) {
            Mastery m = all[i];
            int r = a.rank(m);
            boolean maxed = r >= m.maxRank;
            double ry = listTop + i * rowH, tx = left + 40 * s + trainerGlide.slide(i) * 0.5;
            MenuStyle.diamond(g, left + 24 * s, ry + rowH * 0.42, 5 * s, maxed ? new Color(90, 88, 104) : m.color);
            g.setFont(MenuStyle.sans(Font.BOLD, 15 * s));
            MenuStyle.shadowed(g, m.label, tx, ry + rowH * 0.5, maxed ? new Color(150, 148, 165) : i == sel ? MenuStyle.ROW_SELECTED : Color.WHITE);
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            FontMetrics fm = g.getFontMetrics();
            String rank = m.maxRank == 1 ? (r > 0 ? "learned" : "") : "rank " + r + " / " + m.maxRank;
            MenuStyle.shadowed(g, rank, tx + 190 * s, ry + rowH * 0.5, DIM);
            String cost = maxed ? "MAX" : m.cost(r) + " SP";
            g.setFont(MenuStyle.serif(Font.BOLD, 17 * s, 0.04));
            fm = g.getFontMetrics();
            MenuStyle.shadowed(g, cost, left + lw - 22 * s - fm.stringWidth(cost), ry + rowH * 0.55,
                maxed ? new Color(130, 128, 146) : a.skillPoints >= m.cost(r) ? GOLD : new Color(220, 120, 120));
        }

        double rx = left + lw + gap, cx = rx + rw / 2;
        MenuStyle.card(g, rx, top, rw, bottom - top, accent);
        double feet = top + 156 * s;
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 58 * s, feet - 9 * s, 116 * s, 18 * s));
        Art.frame(switch (world) { case Worlds.CITY -> "shop.duelist"; case Worlds.LAB -> "shop.brass"; default -> "shop.combat"; }, w.time, 1.6).draw(g, cx, feet, 3 * s, false);
        Mastery m = all[sel];
        int r = a.rank(m);
        g.setFont(MenuStyle.serif(Font.BOLD, 24 * s, 0.03));
        MenuStyle.centred(g, m.label, cx, feet + 42 * s, r >= m.maxRank ? new Color(170, 168, 185) : m.color);
        g.setFont(MenuStyle.caps(10 * s));
        MenuStyle.centred(g, (m.maxRank == 1 ? (r > 0 ? "LEARNED" : "NOT LEARNED") : "RANK " + r + " OF " + m.maxRank), cx, feet + 62 * s, DIM);
        g.setFont(MenuStyle.sans(Font.PLAIN, 14 * s));
        double ty = MenuStyle.wrap(g, m.effect, rx + 26 * s, feet + 94 * s, rw - 52 * s, 20 * s, TEXT);
        g.setFont(MenuStyle.serif(Font.ITALIC, 14 * s, 0));
        MenuStyle.wrap(g, m.flavor, rx + 26 * s, ty + 26 * s, rw - 52 * s, 19 * s, DIM);
        double ay = bottom - 30 * s;
        if (r >= m.maxRank) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 15 * s, 0));
            MenuStyle.shadowed(g, "Mastered. Nothing more to learn here.", rx + 26 * s, ay, DIM);
        } else {
            boolean ok = a.skillPoints >= m.cost(r);
            MenuStyle.keyCap(g, "ENTER", rx + 26 * s, ay, s);
            g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
            MenuStyle.shadowed(g, ok ? "Learn it" + DOT + m.cost(r) + " skill point" + (m.cost(r) == 1 ? "" : "s") : "Not enough skill points" + DOT + "it costs " + m.cost(r),
                rx + 92 * s, ay - 1 * s, ok ? GOLD : new Color(220, 120, 120));
        }
        footer(g, w, x0, bottom + 36 * s, width, s, "Learn");
    }

    // ------------------------------------------------------------------ Bramble's table

    /** Bramble's wares: the items for sale on the left, the selected one's stats (and what you wear in its slot) on the right. */
    void drawShop(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 175);
        Color accent = new Color(120, 220, 140);
        double s = MenuStyle.scale(height), x0 = MenuStyle.left(width);
        double base = Math.max(96 * s, height * 0.5 - 240 * s);
        String world = w.level.world;
        String[] shop = switch (world) {
            case Worlds.CITY -> new String[]{"NIX'S NIGHT MARKET", "\"No refunds. No questions. No rats. Well - few rats.\"", "Nix"};
            case Worlds.LAB -> new String[]{"QUILL'S ARCHIVE", "\"Everything catalogued. Some of it even labelled correctly.\"", "Quill"};
            default -> new String[]{"BRAMBLE'S WARES", "\"Gold for goods, goods for gold.\"", "Bramble"};
        };
        Rectangle2D hb = MenuStyle.heading(g, shop[0], x0, base, 58 * s, s).getBounds2D();
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        MenuStyle.shadowed(g, shop[1], hb.getMaxX() + 28 * s, base - 22 * s, new Color(214, 206, 228, 225));
        MenuStyle.label(g, "NEW STOCK AFTER EVERY CLEARED CHALLENGE", hb.getMaxX() + 28 * s, base - 2 * s, s, Util.alpha(accent, 0.95));
        points(g, String.valueOf(w.profile.gold), "GOLD", true, width, base, s);

        List<Item> stock = w.adventure.stock;
        double top = base + 42 * s, bottom = Math.min(height - 62 * s, top + 400 * s), left = 40 * s, total = width - 80 * s, gap = 18 * s;
        double lw = total * 0.52, rw = total - lw - gap;
        MenuStyle.card(g, left, top, lw, bottom - top, null);
        MenuStyle.label(g, "ON THE TABLE", left + 20 * s, top + 28 * s, s, GOLD);
        if (stock.isEmpty()) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
            MenuStyle.shadowed(g, "Sold out! Clear a challenge and " + shop[2] + " will find more.", left + 24 * s, top + 74 * s, DIM);
            footer(g, w, x0, bottom + 36 * s, width, s, "Buy");
            return;
        }
        int sel = Math.min(w.shopCursor, stock.size() - 1);
        double rowH = 64 * s, listTop = top + 42 * s;
        selectionBar(g, shopGlide.step(sel, stock.size(), listTop + sel * rowH), left, lw, rowH, s);
        for (int i = 0; i < stock.size(); i++) {
            Item it = stock.get(i);
            double ry = listTop + i * rowH, tx = left + 76 * s + shopGlide.slide(i) * 0.5;
            TitleScreen.slotBox(g, it, it.slot, left + 22 * s, ry + 8 * s, rowH - 22 * s, s);
            g.setFont(MenuStyle.sans(Font.BOLD, 15 * s));
            MenuStyle.shadowed(g, it.name, tx, ry + rowH * 0.42, it.rarity.color);
            g.setFont(MenuStyle.sans(Font.PLAIN, 12 * s));
            MenuStyle.shadowed(g, it.rarity.label + " " + it.slot.label.toLowerCase() + DOT + it.lines().get(0), tx, ry + rowH * 0.42 + 18 * s, DIM);
            int price = Adventure.price(it);
            String cost = price + " G";
            g.setFont(MenuStyle.serif(Font.BOLD, 17 * s, 0.04));
            FontMetrics fm = g.getFontMetrics();
            MenuStyle.shadowed(g, cost, left + lw - 22 * s - fm.stringWidth(cost), ry + rowH * 0.5, w.profile.gold >= price ? GOLD : new Color(220, 120, 120));
        }

        Item it = stock.get(sel);
        double rx = left + lw + gap, cx = rx + rw / 2;
        MenuStyle.card(g, rx, top, rw, bottom - top, it.rarity.color);
        TitleScreen.slotBox(g, it, it.slot, cx - 36 * s, top + 24 * s, 72 * s, s);
        g.setFont(MenuStyle.serif(Font.BOLD, 22 * s, 0.03));
        MenuStyle.centred(g, it.name, cx, top + 128 * s, it.rarity.color);
        g.setFont(MenuStyle.caps(10 * s));
        MenuStyle.centred(g, (it.rarity.label + DOT + it.slot.label).toUpperCase(), cx, top + 148 * s, DIM);
        Item worn = w.profile.equipped.get(it.slot);                        // its stats, each set against what you wear in this slot
        double ay = bottom - 30 * s, ly = top + 182 * s;
        double room = ay - 46 * s - ly - (it.unique != null ? 40 * s : 0);
        ly = ItemCompare.draw(g, it, worn, rx + 26 * s, rx + rw - 26 * s, ly, Math.min(22 * s, room / Math.max(1, ItemCompare.lines(it, worn))), s);
        if (it.unique != null) {
            g.setFont(MenuStyle.serif(Font.ITALIC, 14 * s, 0));
            ly = MenuStyle.wrap(g, it.unique.text, rx + 26 * s, ly + 2 * s, rw - 52 * s, 18 * s, new Color(255, 180, 90));
        }
        g.setFont(MenuStyle.serif(Font.ITALIC, 13 * s, 0));
        MenuStyle.shadowed(g, worn == null ? "You wear nothing in this slot." : "You wear: " + worn.name + (worn.upgrade > 0 ? " +" + worn.upgrade : ""), rx + 26 * s, ly + 10 * s, DIM);
        int price = Adventure.price(it);
        boolean ok = w.profile.gold >= price;
        MenuStyle.keyCap(g, "ENTER", rx + 26 * s, ay, s);
        g.setFont(MenuStyle.sans(Font.PLAIN, 13 * s));
        MenuStyle.shadowed(g, ok ? "Buy it" + DOT + price + " gold" : "Not enough gold" + DOT + "it costs " + price, rx + 92 * s, ay - 1 * s, ok ? GOLD : new Color(220, 120, 120));
        footer(g, w, x0, bottom + 36 * s, width, s, "Buy");
    }

    // ------------------------------------------------------------------ the card before a challenge

    void drawBriefing(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        MenuStyle.veil(g, width, height, 170);
        Challenge c = w.briefing;
        Adventure a = w.adventure;
        double s = MenuStyle.scale(height), cx = width / 2.0;
        double cw = 640 * s, ch = 372 * s, x = cx - cw / 2, y = (height - ch) / 2 + 10 * s;
        double base = y - 34 * s;
        Rectangle2D hb = MenuStyle.headingCentred(g, c.title, cx, base, 54 * s, s, false).getBounds2D();
        int loops = a.clears(c);
        MenuStyle.ruled(g, loops == 0 ? "A REQUEST FROM " + c.giver.toUpperCase() : "LOOP " + (loops + 1) + DOT + "THE BLIGHT HAS GROWN BACK STRONGER",
            cx, base + 30 * s, hb.getX() - 40 * s, hb.getMaxX() + 40 * s, 14 * s, s);
        MenuStyle.card(g, x, y + 30 * s, cw, ch - 30 * s, new Color(200, 140, 255));
        double px = x + 30 * s, py = y + 76 * s;
        g.setFont(MenuStyle.serif(Font.ITALIC, 17 * s, 0));
        py = MenuStyle.wrap(g, c.brief, px, py, cw - 60 * s, 23 * s, new Color(236, 228, 246)) + 40 * s;

        double startDanger = c.danger + loops * Challenge.LOOP_DANGER;
        String danger = startDanger < 2 ? "Easy" : startDanger < 4 ? "Medium" : startDanger < 6 ? "Hard" : "Very hard";
        String[][] facts = {
            {c.goalWord(), switch (c.goal) {
                case RELAYS -> c.nests + DOT + "hold each one while it powers up";
                case HUNT -> c.nests + DOT + "they bolt when they're hurt";
                case ESCORT -> "Copper, to the far end" + DOT + c.nests + " walls of vines";
                case DEFEND -> "the stasis engine, until it's charged";
                default -> String.valueOf(c.nests);
            }},
            {"GUARDIAN", c.boss ? "Yes: " + MenuStyle.titleCase(c.bossName) : "None"},
            {"DANGER AT THE START", danger + DOT + "rises every minute"},
            {"REWARD", loops == 0 ? c.gold + " gold" + DOT + c.skillPoints + " skill points" : (c.repeatGold + 20 * loops) + " gold" + (c.repeatSkillPoints > 0 ? DOT + c.repeatSkillPoints + " skill point" : "")},
        };
        for (String[] f : facts) {
            g.setFont(MenuStyle.caps(10.5 * s));
            MenuStyle.shadowed(g, f[0], px, py, DIM);
            g.setFont(MenuStyle.sans(Font.BOLD, 15 * s));
            MenuStyle.shadowed(g, f[1], px + 210 * s, py, f[0].equals("REWARD") ? GOLD : Color.WHITE);
            py += 30 * s;
        }
        g.setColor(new Color(255, 214, 120, 60));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Line2D.Double(px, py - 6 * s, x + cw - 30 * s, py - 6 * s));
        g.setFont(MenuStyle.serif(Font.ITALIC, 14 * s, 0));
        MenuStyle.wrap(g, "In there you start again from level 1 with only your sword. Your gear and everything your trainers have taught you come with you; "
            + "the gold and items you find are yours to keep, win or lose.", px, py + 18 * s, cw - 60 * s, 19 * s, DIM);
        String[][] groups = {{"ENTER"}, {"ESC"}};
        String[] labels = {"Go in", "Not yet"};
        MenuStyle.keys(g, cx - MenuStyle.keysWidth(g, s, groups, labels) / 2, y + ch + 40 * s, s, groups, labels);
    }

    // ------------------------------------------------------------------ the map of the worlds

    /** The worlds on the map, left to right: the ones there are, then the one still to come. */
    private static final String[] MAP = {Worlds.FOREST, Worlds.CITY, Worlds.LAB};

    /**
     * The map of the worlds (at a road's signpost): a night sky, each world a small lit globe with a glimpse of it
     * inside, joined by a dotted road; the picked one bigger, with a card saying what's there.
     */
    void drawTravel(Graphics2D g, World w, int width, int height) {
        MenuStyle.antialias(g);
        Adventure a = w.adventure;
        double s = MenuStyle.scale(height), cx = width / 2.0;
        g.setPaint(new GradientPaint(0, 0, new Color(6, 8, 24, 245), 0, height, new Color(24, 14, 44, 245)));
        g.fillRect(0, 0, width, height);
        java.util.Random stars = new java.util.Random(7);
        for (int i = 0; i < 220; i++) {
            double x = stars.nextDouble() * width, y = stars.nextDouble() * height, tw = 0.5 + 0.5 * Math.sin(w.time * (1 + stars.nextDouble() * 2) + i);
            int a2 = (int) (60 + 150 * tw * stars.nextDouble());
            g.setColor(new Color(220, 226, 255, Math.min(255, a2)));
            double r = stars.nextInt(9) == 0 ? 2.2 * s : 1.2 * s;
            g.fill(new Ellipse2D.Double(x - r / 2, y - r / 2, r, r));
        }
        double base = 110 * s;
        Rectangle2D hb = MenuStyle.headingCentred(g, "THE ROADS", cx, base, 58 * s, s, false).getBounds2D();
        MenuStyle.ruled(g, "WHERE WILL YOU GO?", cx, base + 30 * s, hb.getX() - 40 * s, hb.getMaxX() + 40 * s, 14 * s, s);

        String picked = Worlds.IDS[Math.min(w.travelCursor, Worlds.IDS.length - 1)];
        double[] xs = new double[MAP.length], ys = new double[MAP.length];
        for (int i = 0; i < MAP.length; i++) {
            xs[i] = width * (0.22 + 0.28 * i);
            ys[i] = height * 0.46 + (i % 2 == 0 ? 28 : -28) * s;
        }
        for (int i = 0; i + 1 < MAP.length; i++) {                           // the road between neighbours
            boolean open = Story.worldOpen(a, MAP[i + 1]);
            g.setStroke(new BasicStroke((float) (4 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{(float) (2 * s), (float) (12 * s)}, (float) ((w.time * 30 * s) % (14 * s))));
            g.setColor(open ? new Color(255, 214, 120, 200) : new Color(120, 110, 140, 120));
            java.awt.geom.QuadCurve2D road = new java.awt.geom.QuadCurve2D.Double(xs[i], ys[i], (xs[i] + xs[i + 1]) / 2, (ys[i] + ys[i + 1]) / 2 + 70 * s, xs[i + 1], ys[i + 1]);
            g.draw(road);
        }
        for (int i = 0; i < MAP.length; i++) {
            String id = MAP[i];
            boolean exists = Worlds.exists(id), open = exists && Story.worldOpen(a, id), sel = id.equals(picked), here = id.equals(a.world);
            double r = (sel ? 92 : 70) * s + (sel ? 3 * s * Math.sin(w.time * 3) : 0);
            globe(g, w, id, xs[i], ys[i], r, open, s);
            if (sel) {
                g.setColor(new Color(255, 226, 150, 220));
                g.setStroke(new BasicStroke((float) (3 * s)));
                g.draw(new Ellipse2D.Double(xs[i] - r - 8 * s, ys[i] - r - 8 * s, 2 * r + 16 * s, 2 * r + 16 * s));
            }
            g.setFont(MenuStyle.serif(Font.BOLD, (sel ? 20 : 16) * s, 0.06));
            MenuStyle.centred(g, open ? Worlds.title(id) : "???", xs[i], ys[i] + r + 34 * s, open ? (sel ? GOLD : TEXT) : DIM);
            g.setFont(MenuStyle.caps(10 * s));
            MenuStyle.centred(g, here ? "YOU ARE HERE" : open ? "CHAPTER " + (i + 1) : exists ? "THE ROAD IS CLOSED" : "STILL TO COME", xs[i], ys[i] + r + 52 * s, here ? new Color(150, 230, 255) : DIM);
        }

        double cw = 560 * s, ch = 96 * s, x = cx - cw / 2, y = height - 190 * s;      // what's there
        boolean open = Story.worldOpen(a, picked);
        MenuStyle.card(g, x, y, cw, ch, open ? new Color(255, 200, 110) : null);
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        String about = !open ? Story.roadShut(picked) : switch (picked) {
            case Worlds.CITY -> "Lumen, a city of lamps. They've flickered since a star fell into the Dynamo beneath it.";
            case Worlds.LAB -> "Stormcliff, Doctor Morrow's laboratory on the sea cliffs, where the storm never moves on.";
            default -> "Mossbrook's woods, where the Blight first crept out of the Hollow.";
        };
        MenuStyle.wrap(g, about, x + 24 * s, y + 34 * s, cw - 48 * s, 20 * s, TEXT);
        int done = 0, all = 0;
        for (Challenge c : Challenge.values()) if (c.world.equals(picked)) { all++; if (a.clears(c) > 0) done++; }
        g.setFont(MenuStyle.caps(10 * s));
        MenuStyle.shadowed(g, "CHALLENGES CLEARED" + DOT + done + " / " + all, x + 24 * s, y + ch - 18 * s, DIM);

        String[][] groups = {{"A", "D"}, {"ENTER"}, {"ESC"}};
        String[] labels = {"Choose", picked.equals(a.world) ? "Stay" : "Travel", "Back"};
        MenuStyle.keys(g, cx - MenuStyle.keysWidth(g, s, groups, labels) / 2, height - 40 * s, s, groups, labels);
    }

    /** One world on the map: a lit globe with a glimpse of the place inside it (dark, with a "?", if the road is closed). */
    private void globe(Graphics2D g, World w, String id, double x, double y, double r, boolean open, double s) {
        Color sky = switch (id) { case Worlds.CITY -> new Color(40, 50, 110); case Worlds.LAB -> new Color(26, 36, 64); default -> new Color(60, 110, 70); };
        Color ground = switch (id) { case Worlds.CITY -> new Color(150, 130, 110); case Worlds.LAB -> new Color(54, 66, 82); default -> new Color(84, 128, 56); };
        if (!open) { sky = new Color(36, 34, 52); ground = new Color(50, 48, 66); }
        Ellipse2D ball = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g.setColor(Util.alpha(open ? sky.brighter() : sky, 0.35));
        g.fill(new Ellipse2D.Double(x - r * 1.25, y - r * 1.25, r * 2.5, r * 2.5));
        java.awt.Shape saved = g.getClip();
        g.clip(ball);
        g.setPaint(new GradientPaint((float) x, (float) (y - r), sky, (float) x, (float) (y + r), sky.darker()));
        g.fill(ball);
        g.setColor(ground);
        g.fill(new Ellipse2D.Double(x - r * 1.6, y + r * 0.25, r * 3.2, r * 1.8));
        double k = r / (80 * s) * 1.6 * s;                                      // the glimpse of the place, standing on its ground
        java.awt.RenderingHints.Key key = java.awt.RenderingHints.KEY_INTERPOLATION;
        Object hint = g.getRenderingHint(key);
        g.setRenderingHint(key, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (open && id.equals(Worlds.FOREST)) {
            Art.frames("landmark.pine")[0].draw(g, x - r * 0.45, y + r * 0.45, k, false);
            Art.frames("landmark.oak")[0].draw(g, x + r * 0.2, y + r * 0.5, k, false);
            Art.frames("landmark.bush")[0].draw(g, x - r * 0.05, y + r * 0.62, k, false);
        } else if (open && id.equals(Worlds.CITY)) {
            Sprite[] fronts = ThemeArt.of(Theme.CITY).tall;
            fronts[0].draw(g, x - r * 0.42, y + r * 0.42, k, false);
            fronts[2].draw(g, x + r * 0.36, y + r * 0.42, k, false);
            Art.frames("landmark.lamp")[0].draw(g, x - r * 0.02, y + r * 0.6, k, false);
        } else if (open && id.equals(Worlds.LAB)) {
            Sprite[] fronts = ThemeArt.of(Theme.LAB).tall;
            fronts[1].draw(g, x - r * 0.38, y + r * 0.42, k, false);
            Art.frames("landmark.rod")[0].draw(g, x + r * 0.42, y + r * 0.5, k, false);
            Art.frames("landmark.coil")[0].draw(g, x + r * 0.08, y + r * 0.62, k, false);
        }
        g.setRenderingHint(key, hint == null ? java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : hint);
        g.setClip(saved);
        g.setPaint(new java.awt.RadialGradientPaint((float) (x - r * 0.35), (float) (y - r * 0.4), (float) r * 1.2f, new float[]{0f, 1f},
            new Color[]{new Color(255, 255, 255, open ? 50 : 20), new Color(0, 0, 0, 90)}));
        g.fill(ball);
        g.setColor(new Color(10, 10, 20, 200));
        g.setStroke(new BasicStroke((float) (2.5 * s)));
        g.draw(ball);
        if (!open) {
            g.setFont(MenuStyle.serif(Font.BOLD, r * 0.9, 0));
            MenuStyle.centred(g, "?", x, y + r * 0.3, new Color(150, 140, 180));
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A big number in the top-right corner with a small label under it (skill points, gold). */
    private static void points(Graphics2D g, String value, String label, boolean lit, int width, double base, double s) {
        g.setFont(MenuStyle.serif(Font.BOLD, 34 * s, 0.02));
        FontMetrics pm = g.getFontMetrics();
        double pr = width - 44 * s;
        MenuStyle.shadowed(g, value, pr - pm.stringWidth(value), base - 6 * s, lit ? GOLD : DIM);
        g.setFont(MenuStyle.caps(10 * s));
        MenuStyle.shadowed(g, label, pr - g.getFontMetrics().stringWidth(label), base + 12 * s, DIM);
    }

    private static void selectionBar(Graphics2D g, double barY, double left, double lw, double rowH, double s) {
        g.setPaint(new GradientPaint((float) (left + 8 * s), 0, new Color(255, 180, 70, 90), (float) (left + lw - 8 * s), 0, new Color(255, 180, 70, 8)));
        g.fill(new Rectangle2D.Double(left + 8 * s, barY, lw - 16 * s, rowH - 6 * s));
        g.setColor(MenuStyle.GOLD_DEEP);
        g.fill(new Rectangle2D.Double(left + 8 * s, barY, 3 * s, rowH - 6 * s));
    }

    /** Keys along the bottom, and the last message (a purchase, a refusal) on the right. */
    private static void footer(Graphics2D g, World w, double x0, double y, int width, double s, String verb) {
        MenuStyle.keys(g, x0, y, s, new String[][]{{"W", "S"}, {"ENTER"}, {"ESC"}}, new String[]{"Choose", verb, "Leave"});
        if (w.screenMessage.isEmpty()) return;
        g.setFont(MenuStyle.serif(Font.ITALIC, 16 * s, 0));
        FontMetrics fm = g.getFontMetrics();
        boolean bad = w.screenMessage.startsWith("Not") || w.screenMessage.contains("already");
        MenuStyle.shadowed(g, w.screenMessage, width - 44 * s - fm.stringWidth(w.screenMessage), y, bad ? MenuStyle.WARN : GOLD);
    }
}
