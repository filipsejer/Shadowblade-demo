package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws the game world with sprites: the level, the shops, the guide, the player and every enemy sorted by depth (so
 * whoever is lower on the screen is in front), each with a shadow under its feet, plus attacks and other effects.
 * Menus and the HUD are drawn afterwards by {@link Renderer}.
 */
final class WorldRenderer {
    private final LevelView levelView = new LevelView();
    private final Font f12 = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font f14b = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private record Item(double sortY, Runnable draw) {}

    LevelView levelView() { return levelView; }

    /** {@code g} must already be translated by the camera; {@code view} is the visible part of the world. */
    void draw(Graphics2D g, World w, Rectangle2D view) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Level level = w.level;
        levelView.draw(g, level, view, w.time);
        for (Level.Door door : level.doors) if (door.sealed && view.intersects(door.gap)) drawSealedLabel(g, door);
        if (level.town) drawForestPath(g, w);
        for (Zone z : w.zones) drawZone(g, w, z);
        for (Blast b : w.blasts) drawBlast(g, w, b);
        for (Enemy e : w.enemies) drawTelegraph(g, e, w.player);

        // shadows first, then everyone in order of depth
        List<Item> items = new ArrayList<>();
        for (Level.Station s : level.stations) {
            items.add(new Item(s.y() + 36, () -> drawShop(g, w, s)));
            shadow(g, s.x(), s.y() + 36, 46, 12);
        }
        if (level.town) {                                       // the station is always there, just shuttered until level 1 is done
            items.add(new Item(level.guideY + 44, () -> drawStation(g, w)));
            shadow(g, level.guideX, level.guideY + 46, 40, 12);
        } else if (level.guideAppeared) {
            items.add(new Item(level.guideY + 20, () -> drawGuide(g, w)));
            shadow(g, level.guideX, level.guideY + 22, 22, 7);
        }
        for (Level.Npc npc : level.npcs) {
            items.add(new Item(npc.y() + 20, () -> drawNpc(g, w, npc)));
            shadow(g, npc.x(), npc.y() + 22, 16, 6);
        }
        for (Level.Landmark l : level.landmarks) {
            if (!view.intersects(l.x() - 140, l.y() - 240, 280, 300)) continue;
            Sprite sprite = landmark(l.kind());
            items.add(new Item(l.y(), () -> sprite.draw(g, l.x(), l.y(), Art.SCALE, false)));
            shadow(g, l.x(), l.y() - 2, sprite.w * Art.SCALE * (l.kind().equals("oak") ? 0.3 : 0.4), sprite.w * Art.SCALE * 0.1);
        }
        for (Breakable b : level.breakables) {
            if (b.broken || !view.intersects(b.x - 60, b.y - 90, 120, 120)) continue;
            Sprite sprite = Art.frames(BreakableArt.name(b.kind, level.theme))[0];
            items.add(new Item(b.y, () -> sprite.draw(g, b.x, b.y, Art.SCALE, false)));
            shadow(g, b.x, b.y - 3, b.radius * 1.15, b.radius * 0.38);
        }
        Tutorial tut = w.tutorial;
        if (tut != null && !tut.squirrel.hidden) {
            Tutorial.Squirrel q = tut.squirrel;
            items.add(new Item(q.y, () -> drawSquirrel(g, w, q)));
            shadow(g, q.x, q.y - 1, 26, 7);
        }
        if (tut != null) {
            for (Tutorial.Acorn a : tut.acorns) {
                items.add(new Item(a.y, () -> Art.frame("prop.acorn", w.time, 8).draw(g, a.x, a.y - 22, Art.SCALE, false, a.spin, 1f)));
                shadow(g, a.x, a.y, 9, 3);
            }
        }
        for (Enemy e : w.enemies) {
            if (!view.intersects(e.x - 90, e.y - 140, 180, 220)) continue;
            double sh = e.type == Enemy.Type.BOSS ? 1.5 : e.type.small() ? 1.05 : 1.2;
            if (!e.intangible()) shadow(g, e.x, e.y + e.radius * 0.85, e.radius * sh, e.radius * 0.36);
            items.add(new Item(e.y + e.radius * 0.85, () -> drawEnemy(g, w, e)));
        }
        Player p = w.player;
        shadow(g, p.x, p.y + 14, 17, 6);
        items.add(new Item(p.y + 14, () -> drawPlayer(g, w, p)));
        items.sort(Comparator.comparingDouble(Item::sortY));
        for (Item it : items) it.draw.run();

        // the night / evening wash over the characters and ground; attacks stay bright on top of it
        ThemeArt art = levelView.art();
        if (art != null && (art.ambient >>> 24) > 0) {
            g.setColor(new Color(art.ambient, true));
            g.fill(view);
            levelView.drawGlows(g, view);
        }

        for (Projectile pr : w.projectiles) drawProjectile(g, w, pr);
        for (Effect e : w.effects) e.render(g);
        Enemy lock = w.lockedTarget();
        if (lock != null) drawLockOn(g, lock);
        for (Enemy e : w.enemies) drawEnemyBar(g, w, e);
        for (Enemy e : w.enemies) if (e.stun > 0.05 && e.spawnIn <= 0 && !e.type.armored && e.hp > 0 && !e.shielded) drawDizzy(g, w, e);
        if (tut != null) drawKeyHint(g, w, tut);
    }

    private final java.util.Map<String, Sprite> landmarks = new java.util.HashMap<>();

    /** The sprite of a placed piece of scenery (see {@link Level.Landmark}). */
    private Sprite landmark(String kind) {
        return landmarks.computeIfAbsent(kind, k -> {
            ThemeArt a = ThemeArt.of(Theme.FOREST);
            return switch (k) {
                case "oak" -> a.tall[0];
                case "bush" -> a.low[0];
                case "boulder" -> a.low[1];
                case "stump" -> a.low[2];
                default -> a.low[3];
            };
        });
    }

    private void shadow(Graphics2D g, double x, double y, double rx, double ry) {
        g.setColor(new Color(0, 0, 0, 62));
        g.fill(new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2));
    }

    // ------------------------------------------------------------------ the hero

    private void drawPlayer(Graphics2D g, World w, Player p) {
        String dir = PeopleArt.heroDir(p.facing);
        boolean flip = dir.equals("side") && PeopleArt.heroFlip(p.facing);
        if (p.dodging()) {                                             // rolling: a spinning ball
            Sprite[] roll = Art.frames("hero.roll");
            roll[(int) (p.dodgeProgress() * 6) % roll.length].draw(g, p.x, p.y + 4, Art.SCALE, false, 0, 0.95f);
            return;
        }
        Sprite s;
        if (p.swinging()) s = Art.frames("hero." + dir + ".attack")[p.swingPhase() < 0.4 ? 0 : 1];
        else if (p.moving) s = Art.frame("hero." + dir + ".walk", w.time, 9);
        else s = Art.frame("hero." + dir + ".idle", w.time, 2);
        float alpha = p.hurtTimer > 0 && ((int) (p.hurtTimer * 20) % 2 == 0) ? 0.45f : 1f;
        boolean dazed = w.tutorial != null && w.tutorial.dazed();
        double sway = dazed ? Math.sin(w.time * 2.4) * 4 : 0;                       // waking up: swaying, with stars going round the head
        s.draw(g, p.x + sway, p.y + 14, Art.SCALE, flip, 0, alpha);
        if (dazed) {
            Sprite[] star = Art.frames("fx.star");
            double top = p.y + 14 - s.ay * Art.SCALE;
            for (int i = 0; i < 3; i++) {
                double a = w.time * 4 + i * Math.PI * 2 / 3;
                star[(int) (w.time * 8 + i) % star.length].draw(g, p.x + sway + Math.cos(a) * 22, top + 10 + Math.sin(a) * 6, 2, false);
            }
        }
        if (p.hurtTimer > 0.5) s.drawSilhouette(g, p.x, p.y + 14, Art.SCALE, flip, 0xFFFFFF, 0.65f);   // white flash when hit
    }

    // ------------------------------------------------------------------ the tutorial's squirrel and hints

    private void drawSquirrel(Graphics2D g, World w, Tutorial.Squirrel q) {
        Tutorial.Squirrel.Pose pose = q.pose();
        Sprite s = switch (pose) {
            case RUN -> Art.frame("squirrel.run", w.time, 14);
            case SCARED -> Art.frame("squirrel.scared", w.time, 8);
            case THROW -> Art.frames("squirrel.throw")[Math.max(0, Math.min(1, q.throwFrame))];
            default -> Art.frame("squirrel.idle", w.time, 2);
        };
        double hop = pose == Tutorial.Squirrel.Pose.RUN ? Math.abs(Math.sin(w.time * 14)) * 7 : 0;
        double shiver = pose == Tutorial.Squirrel.Pose.SCARED ? Math.sin(w.time * 50) * 1.5 : 0;
        s.draw(g, q.x + shiver, q.y - hop, Art.SCALE, q.faceLeft);
        if (q.alarm > 0) {                                                       // a "!" in a bubble
            double bx = q.x, by = q.y - s.ay * Art.SCALE - 26 - 5 * Math.sin(w.time * 18);
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(bx - 12, by - 20, 24, 30, 10, 10));
            g.setColor(new Color(200, 50, 50));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            centered(g, "!", bx, by + 3, new Color(200, 50, 50));
        }
    }

    /** Key caps beside the hero when they seem stuck; a cap lights up while its key is held. */
    private void drawKeyHint(Graphics2D g, World w, Tutorial tut) {
        String[] keys = tut.hintKeys();
        double a = tut.hintAlpha();
        if (keys == null || a < 0.02) return;
        Player p = w.player;
        double cx = p.x, cy = p.y - 96 + 4 * Math.sin(w.time * 4);
        g.setFont(f14b);
        if (keys.length == 4) {                                                  // W A S D laid out like the keys
            String[] names = {"W", "A", "S", "D"};
            double[][] pos = {{0, -34}, {-34, 0}, {0, 0}, {34, 0}};
            for (int i = 0; i < 4; i++) cap(g, names[i], cx + pos[i][0], cy + pos[i][1], 30, tut.held(names[i]), a);
            return;
        }
        double total = 0;
        double[] widths = new double[keys.length];
        for (int i = 0; i < keys.length; i++) {
            widths[i] = keys[i].length() > 2 ? 24 + keys[i].length() * 11 : 30;
            total += widths[i] + (i > 0 ? 26 : 0);
        }
        double x = cx - total / 2;
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                centered(g, "+", x + 13, cy + 5, Util.alpha(Color.WHITE, a));
                x += 26;
            }
            cap(g, keys[i], x + widths[i] / 2, cy, widths[i], tut.held(keys[i]), a);
            x += widths[i];
        }
    }

    private void cap(Graphics2D g, String label, double cx, double cy, double width, boolean lit, double alpha) {
        RoundRectangle2D box = new RoundRectangle2D.Double(cx - width / 2, cy - 15, width, 30, 8, 8);
        g.setColor(lit ? Util.alpha(new Color(255, 214, 90), alpha) : Util.alpha(new Color(28, 28, 36), 0.85 * alpha));
        g.fill(box);
        g.setColor(Util.alpha(lit ? Color.WHITE : new Color(225, 225, 235), alpha));
        g.setStroke(new BasicStroke(2f));
        g.draw(box);
        g.setFont(f14b);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Util.alpha(lit ? new Color(40, 30, 10) : Color.WHITE, alpha));
        g.drawString(label, (float) (cx - fm.stringWidth(label) / 2.0), (float) (cy + 5));
    }

    // ------------------------------------------------------------------ shops and the guide

    private void drawShop(Graphics2D g, World w, Level.Station s) {
        String key = switch (s.category()) { case COMBAT -> "combat"; case SPELLS -> "spells"; case SURVIVAL -> "survival"; };
        Color accent = s.category().color;
        double x = s.x(), fy = s.y() + 36;
        if (w.player.skillPoints > 0) {
            double r = 58 + 5 * Math.sin(w.time * 4);
            g.setColor(Util.alpha(accent, 0.6));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(x - r, fy - 16 - r * 0.42, r * 2, r * 0.84));
        }
        Art.frame("shop." + key, w.time + s.x() * 0.01, 1.6).draw(g, x, fy, Art.SCALE, false);
        g.setFont(f12);
        centered(g, s.name(), x, fy - 112, accent);
        if (w.stationNearby() == s && w.state == World.State.PLAYING) {
            g.setFont(f14b);
            centered(g, "Press E to talk", x, fy + 26, Color.WHITE);
        }
    }

    private void drawGuide(Graphics2D g, World w) {
        Level lv = w.level;
        double x = lv.guideX, y = lv.guideY;
        Color green = new Color(70, 205, 95);
        double r = 40 + 4 * Math.sin(w.time * 3);
        g.setColor(Util.alpha(green, 0.45));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(x - r, y + 20 - r * 0.45, r * 2, r * 0.9));
        double hover = 7 + 3 * Math.sin(w.time * 3);
        Art.frame("guide.idle", w.time, 2.2).draw(g, x, y + 20 - hover, Art.SCALE, false);
        g.setFont(f12);
        centered(g, "GUIDE", x, y - 74, green);
        if (w.guideNearby() && w.state == World.State.PLAYING) {
            g.setFont(f14b);
            centered(g, "Press E to travel to the next level", x, y + 52, Color.WHITE);
        }
    }

    /** Transit Town's train station: always standing there, shuttered until level 1 is done, then lit up and ready to go. */
    private void drawStation(Graphics2D g, World w) {
        Level lv = w.level;
        double x = lv.guideX, y = lv.guideY;
        boolean open = lv.guideAppeared;
        if (open) {
            double r = 46 + 4 * Math.sin(w.time * 3);
            g.setColor(Util.alpha(new Color(255, 214, 110), 0.4));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Ellipse2D.Double(x - r, y + 44 - r * 0.3, r * 2, r * 0.6));
        }
        Art.frame(open ? "station.open" : "station.closed", w.time, 2.2).draw(g, x, y + 44, Art.SCALE, false);
        g.setFont(f12);
        centered(g, "TRAIN STATION", x, y - 92, open ? new Color(255, 214, 110) : new Color(170, 170, 182));
        if (!open) {
            g.setFont(f14b);
            centered(g, "Closed for now", x, y + 108, new Color(190, 190, 200));
        } else if (w.guideNearby() && w.state == World.State.PLAYING) {
            g.setFont(f14b);
            centered(g, "Press E to catch the train", x, y + 108, Color.WHITE);
        }
    }

    // ------------------------------------------------------------------ Transit Town: the locals and the way out

    private void drawNpc(Graphics2D g, World w, Level.Npc npc) {
        Sprite s = Art.frame(npc.portrait(), w.time + npc.x() * 0.01, 1.8);
        s.draw(g, npc.x(), npc.y() + 20, Art.SCALE, false);
        if (w.npcNearby() == npc && w.state == World.State.PLAYING && !w.dialogue.active()) {
            g.setFont(f14b);
            centered(g, "Press E to talk", npc.x(), npc.y() + 58, Color.WHITE);
        }
    }

    /** No sprite, just the promise of trees (see the landmarks placed around it in {@link Level#town}) and a prompt up close. */
    private void drawForestPath(Graphics2D g, World w) {
        Level lv = w.level;
        double x = lv.forestX, y = lv.forestY;
        g.setFont(f12);
        centered(g, "TO THE FOREST", x, y - 44, new Color(150, 210, 140));
        if (w.forestPathNearby() && w.state == World.State.PLAYING) {
            g.setFont(f14b);
            centered(g, "Press E to head into the forest", x, y + 34, Color.WHITE);
        }
    }

    /** "SEALED" beside the door, on the side of the room you can actually stand in. */
    private void drawSealedLabel(Graphics2D g, Level.Door door) {
        boolean farSide = door.a.gated;             // then the open room is b, whose barrier is the second one
        Rectangle2D.Double bar = door.barriers.get(farSide ? 1 : 0);
        double lx, ly;
        if (door.vertical) {
            lx = bar.getCenterX();
            ly = farSide ? bar.getMaxY() + 22 : bar.y - 10;
        } else {
            lx = farSide ? bar.getMaxX() + 44 : bar.x - 44;
            ly = bar.getCenterY() + 4;
        }
        g.setFont(f14b);
        centered(g, "SEALED", lx, ly, new Color(206, 150, 255));
    }

    // ------------------------------------------------------------------ enemies

    private static String name(Enemy.Type t) {
        return switch (t) { case GRUNT -> "grunt"; case RUNNER -> "runner"; case SHOOTER -> "shooter"; case BRUTE -> "brute"; case SHADE -> "shade"; case BOSS -> "boss"; };
    }

    /** The sprite an enemy shows right now (its walk cycle, wind-up pose, boss attack pose...). */
    Sprite spriteFor(World w, Enemy e) {
        String theme = w.level.theme.key;
        boolean moving = e.state == Enemy.State.CHASE && e.stun <= 0 && e.spawnIn <= 0;
        double t = w.time + e.animOffset;
        if (e.type == Enemy.Type.BOSS) {
            String base = theme + (e.phase2 ? ".boss2." : ".boss.");
            if (e.state == Enemy.State.WINDUP) return Art.frames(base + (e.attack == Enemy.Attack.SLAM ? "slam" : "burst"))[0];
            return Art.frame(base + (moving ? "walk" : "idle"), t, moving ? 6 : 2.5);
        }
        String base = e.type == Enemy.Type.SHADE ? "shade" : theme + "." + name(e.type);
        if (e.state == Enemy.State.WINDUP) return Art.frames(base + ".windup")[0];
        return Art.frame(base + ".walk", t, moving ? 7 : 2.2);
    }

    /** How high above the ground it floats: drones and shades hover. */
    private double hover(World w, Enemy e) {
        boolean drone = e.type == Enemy.Type.SHOOTER && w.level.theme == Theme.CITY;
        if (e.type != Enemy.Type.SHADE && !drone) return 0;
        return 9 + 3 * Math.sin((w.time + e.animOffset) * 3);
    }

    private void drawEnemy(Graphics2D g, World w, Enemy e) {
        Sprite s = spriteFor(w, e);
        double x = e.x, fy = e.y + e.radius * 0.85 - hover(w, e) - e.z;   // airborne: floats up off its shadow, which stays on the ground
        int sc = Art.SCALE;
        if (e.intangible()) {                                             // shadow mode: a dark see-through silhouette with a violet edge
            float a = 0.6f;
            if (e.spawnIn <= 0 && e.shadowTimer < 1.0) a *= (float) (0.55 + 0.45 * Math.abs(Math.sin(e.shadowTimer * 16)));
            for (int[] o : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {
                s.drawSilhouette(g, x + o[0], fy + o[1], sc, e.faceLeft, 0xA07AE0, a * 0.7f);
            }
            s.drawSilhouette(g, x, fy, sc, e.faceLeft, 0x14101E, a);
            drawClockArc(g, e, true);
            return;
        }
        float alpha = e.spawnIn > 0 ? 0.35f : 1f;
        if (e.type == Enemy.Type.SHADE && e.spawnIn <= 0 && e.shadowTimer < 1.0) alpha *= (float) (0.6 + 0.4 * Math.abs(Math.sin(e.shadowTimer * 16)));
        s.draw(g, x, fy, sc, e.faceLeft, 0, alpha);
        if (e.flash > 0) s.drawSilhouette(g, x, fy, sc, e.faceLeft, 0xFFFFFF, 0.85f);
        if (e.stageTimer > 0) {                                                                          // changing stage: untouchable, glowing
            s.drawSilhouette(g, x, fy, sc, e.faceLeft, 0xA0FFC8, (float) (0.35 + 0.3 * Math.sin(w.time * 24)));
            double ring = e.radius * 2 + 18 * Math.abs(Math.sin(w.time * 6));
            g.setColor(Util.alpha(new Color(150, 255, 190), 0.6));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(x - ring, e.y - ring, ring * 2, ring * 2));
        }
        if (e.shielded) drawShell(g, w, e);
        if (e.burnTimer > 0) s.drawSilhouette(g, x, fy, sc, e.faceLeft, 0xFF8A20, (float) (0.28 + 0.16 * Math.sin(w.time * 20)));
        if (e.slowTimer > 0) s.drawSilhouette(g, x, fy, sc, e.faceLeft, 0x7CC8FF, 0.32f);
        if (e.type.shadowy) drawShadowClock(g, e);
    }

    /** The tutorial monster curled up: a dome of woven thorns around it. Swords bounce off; magic breaks it. */
    private void drawShell(Graphics2D g, World w, Enemy e) {
        double r = e.radius + 12, cy = e.y - 2;
        Ellipse2D dome = new Ellipse2D.Double(e.x - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(70, 140, 60, 120));
        g.fill(dome);
        g.setColor(new Color(40, 90, 40, 230));
        g.setStroke(new BasicStroke(4f));
        g.draw(dome);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(120, 190, 90, 200));
        for (int i = 0; i < 3; i++) g.draw(new Ellipse2D.Double(e.x - r * (0.9 - 0.3 * i), cy - r, r * (1.8 - 0.6 * i), r * 2));   // woven ribs
        int thorns = 12;
        for (int i = 0; i < thorns; i++) {
            double a = i * Math.PI * 2 / thorns + w.time * 0.3, in = r - 2, out = r + 12 + 2 * Math.sin(w.time * 5 + i);
            double half = 0.13;
            Path2D spike = new Path2D.Double();
            spike.moveTo(e.x + Math.cos(a - half) * in, cy + Math.sin(a - half) * in);
            spike.lineTo(e.x + Math.cos(a) * out, cy + Math.sin(a) * out);
            spike.lineTo(e.x + Math.cos(a + half) * in, cy + Math.sin(a + half) * in);
            spike.closePath();
            g.setColor(new Color(96, 66, 40));
            g.fill(spike);
            g.setColor(new Color(40, 28, 20));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(spike);
        }
    }

    /** The danger area of an attack that's winding up (readable through the sprites). */
    private void drawTelegraph(Graphics2D g, Enemy e, Player p) {
        if (e.state != Enemy.State.WINDUP) return;
        double total = e.windupTotal > 0 ? e.windupTotal : e.type.windup;
        double progress = Util.clamp(1 - e.stateTimer / total, 0, 1);
        double r = e.radius;
        if (e.type == Enemy.Type.BOSS && e.attack == Enemy.Attack.BOMBS) {                 // winding up to lob flasks: a green pulse
            double ring = r + 14 + 46 * progress;
            Ellipse2D pulse = new Ellipse2D.Double(e.x - ring, e.y - ring, ring * 2, ring * 2);
            g.setColor(Util.alpha(new Color(90, 255, 140), 0.10 + 0.25 * progress));
            g.fill(pulse);
            g.setColor(Util.alpha(new Color(170, 255, 190), 0.5 + 0.4 * progress));
            g.setStroke(new BasicStroke(3f));
            g.draw(pulse);
        } else if (e.type == Enemy.Type.BOSS && e.attack == Enemy.Attack.BURST) {
            double ring = r + 14 + 46 * progress;
            Ellipse2D pulse = new Ellipse2D.Double(e.x - ring, e.y - ring, ring * 2, ring * 2);
            g.setColor(Util.alpha(new Color(255, 70, 120), 0.10 + 0.25 * progress));
            g.fill(pulse);
            g.setColor(Util.alpha(new Color(255, 130, 160), 0.5 + 0.4 * progress));
            g.setStroke(new BasicStroke(3f));
            g.draw(pulse);
        } else if (e.type == Enemy.Type.BOSS && e.attack == Enemy.Attack.CHARGE) {          // winding up to charge: a line down the lane he'll take
            double ang = Util.angleTo(e.x, e.y, p.x, p.y);
            g.setColor(Util.alpha(new Color(150, 255, 120), 0.15 + 0.55 * progress));
            g.setStroke(new BasicStroke(5f));
            g.draw(new Line2D.Double(e.x, e.y, e.x + Math.cos(ang) * 500, e.y + Math.sin(ang) * 500));
        } else if (e.type == Enemy.Type.SHOOTER) {
            double ang = Util.angleTo(e.x, e.y, p.x, p.y);
            g.setColor(Util.alpha(new Color(255, 80, 80), 0.15 + 0.5 * progress));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(e.x, e.y, e.x + Math.cos(ang) * 420, e.y + Math.sin(ang) * 420));
        } else {
            double reach = r + p.radius + e.strikeReach();
            Ellipse2D danger = new Ellipse2D.Double(e.x - reach, e.y - reach, reach * 2, reach * 2);
            g.setColor(Util.alpha(new Color(255, 60, 60), 0.10 + 0.22 * progress));
            g.fill(danger);
            g.setColor(Util.alpha(new Color(255, 120, 100), 0.5 + 0.4 * progress));
            g.setStroke(new BasicStroke(2f));
            g.draw(danger);
        }
    }

    /**
     * A shade's clock: an arc around it that drains to show how long is left in the current mode (5 seconds each).
     * Pink = solid, lilac = shadow.
     */
    private void drawShadowClock(Graphics2D g, Enemy e) {
        drawClockArc(g, e, false);
    }

    private void drawClockArc(Graphics2D g, Enemy e, boolean ghost) {
        double rr = e.radius + 12;
        double frac = Util.clamp(e.shadowTimer / Enemy.SHADOW_PERIOD, 0, 1);
        g.setColor(ghost ? new Color(200, 165, 255, 230) : new Color(255, 120, 150, 230));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(e.x - rr, e.y - rr, rr * 2, rr * 2, 90, -360 * frac, Arc2D.OPEN));
    }

    private void drawDizzy(Graphics2D g, World w, Enemy e) {
        Sprite[] star = Art.frames("fx.star");
        Sprite s = spriteFor(w, e);
        double top = e.y + e.radius * 0.85 - hover(w, e) - e.z - s.ay * Art.SCALE;
        for (int i = 0; i < 3; i++) {
            double a = w.time * 6 + i * Math.PI * 2 / 3;
            star[(int) (w.time * 8 + i) % star.length].draw(g, e.x + Math.cos(a) * 16, top + 6 + Math.sin(a) * 4, 2, false);
        }
    }

    /** The target-lock marker: a plain white ring (dark edge so it reads on any background) around the enemy. */
    private void drawLockOn(Graphics2D g, Enemy e) {
        double r = e.radius + 12, ey = e.y - e.z;
        Ellipse2D ring = new Ellipse2D.Double(e.x - r, ey - r, r * 2, r * 2);
        g.setColor(new Color(0, 0, 0, 170));
        g.setStroke(new BasicStroke(5.5f));
        g.draw(ring);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(ring);
    }

    private void drawEnemyBar(Graphics2D g, World w, Enemy e) {
        if (e.spawnIn > 0 || e.type == Enemy.Type.BOSS || e.intangible() || e.hp <= 0) return;   // the boss has its own big bar
        Sprite s = spriteFor(w, e);
        double top = e.y + e.radius * 0.85 - hover(w, e) - e.z - s.ay * Art.SCALE;
        double bw = Math.max(30, e.radius * 2);
        double x = e.x - bw / 2, y = top - 10;
        g.setColor(new Color(20, 20, 22, 210));
        g.fill(new Rectangle2D.Double(x - 1.5, y - 1.5, bw + 3, 8));
        g.setColor(new Color(70, 200, 90));
        g.fill(new Rectangle2D.Double(x, y, bw * Util.clamp(e.hp / e.maxHp, 0, 1), 5));
    }

    // ------------------------------------------------------------------ attacks in flight

    private void drawProjectile(Graphics2D g, World w, Projectile p) {
        if (p.friendly) {
            Art.frame("fx.fireball", w.time, 14).draw(g, p.x, p.y, Art.SCALE, false, Math.atan2(p.vy, p.vx), 1f);
            return;
        }
        boolean big = p.radius >= 8;
        String name = switch (w.level.theme) {
            case FOREST -> big ? "proj.spore" : "proj.seed";
            case CITY -> big ? "proj.boltball" : "proj.plasma";
            case LAB -> big ? "proj.acidball" : "proj.acid";
        };
        Art.frame(name, w.time, 8).draw(g, p.x, p.y, Art.SCALE, false, 0, 1f);
    }

    /** A flask bomb: the spot is marked, a flask falls onto it, then it bursts in a green flash. */
    private void drawBlast(Graphics2D g, World w, Blast b) {
        if (b.burst) {
            double a = Util.clamp(b.after / Blast.LINGER, 0, 1);
            Ellipse2D flash = new Ellipse2D.Double(b.x - b.radius, b.y - b.radius, b.radius * 2, b.radius * 2);
            g.setColor(Util.alpha(new Color(150, 255, 170), 0.35 * a));
            g.fill(flash);
            g.setColor(Util.alpha(new Color(220, 255, 230), 0.8 * a));
            g.setStroke(new BasicStroke(3f));
            g.draw(flash);
            return;
        }
        double p = Util.clamp(1 - b.delay / b.total, 0, 1);
        Ellipse2D mark = new Ellipse2D.Double(b.x - b.radius, b.y - b.radius, b.radius * 2, b.radius * 2);
        g.setColor(Util.alpha(new Color(90, 255, 140), 0.08 + 0.2 * p));
        g.fill(mark);
        g.setColor(Util.alpha(new Color(170, 255, 190), 0.45 + 0.45 * p));
        g.setStroke(new BasicStroke(2f));
        g.draw(mark);
        double inner = b.radius * (1 - p);
        g.draw(new Ellipse2D.Double(b.x - inner, b.y - inner, inner * 2, inner * 2));
        g.draw(new Line2D.Double(b.x - 9, b.y, b.x + 9, b.y));
        g.draw(new Line2D.Double(b.x, b.y - 9, b.x, b.y + 9));
        double drop = 420 * (1 - p) * (1 - p);                                                         // the flask falls faster as it nears
        Art.frame("proj.acidball", w.time, 8).draw(g, b.x, b.y - drop, 4, false, w.time * 5, 1f);
    }

    /** An Ice Storm: frost on the ground, snow drifting down and ice shards circling. */
    private void drawZone(Graphics2D g, World w, Zone z) {
        float fade = (float) Math.min(1, z.life / 0.5);
        Sprite[] frost = FxArt.frost(z.radius);
        frost[(int) (w.time * 2.5) % frost.length].draw(g, z.x, z.y, Art.SCALE, false, 0, fade);
        Sprite[] shard = Art.frames("fx.shard");
        for (int i = 0; i < 6; i++) {
            double a = w.time * 1.6 + i * Math.PI / 3;
            double rad = z.radius * 0.6;
            shard[i % shard.length].draw(g, z.x + Math.cos(a) * rad, z.y + Math.sin(a) * rad * 0.65 - 8, Art.SCALE, false, a + Math.PI / 2, fade);
        }
        Sprite[] snow = Art.frames("fx.snow");
        for (int i = 0; i < 14; i++) {
            double a = (FxArt.hash(i, 7, 1) % 628) / 100.0, d = Math.sqrt((FxArt.hash(i, 8, 2) % 100) / 100.0) * z.radius * 0.95;
            double fall = ((w.time * 46 + i * 37) % 70);
            snow[(int) (w.time * 3 + i) % snow.length].draw(g, z.x + Math.cos(a) * d + Math.sin(w.time * 2 + i) * 4,
                z.y + Math.sin(a) * d * 0.7 - 66 + fall, 2, false, 0, fade * (float) (1 - fall / 90));
        }
    }

    private void centered(Graphics2D g, String s, double cx, double baseline, Color c) {
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - fm.stringWidth(s) / 2.0);
        g.setColor(new Color(0, 0, 0, Math.min(200, c.getAlpha())));
        g.drawString(s, x + 1.5f, (float) baseline + 1.5f);
        g.setColor(c);
        g.drawString(s, x, (float) baseline);
    }
}
