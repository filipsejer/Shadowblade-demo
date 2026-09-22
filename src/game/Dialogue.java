package game;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Somebody talking. Lines are written out a letter at a time in a box at the bottom of the screen, each with a chirp of the
 * speaker's voice every few letters. A line either <i>waits</i> (the world stands still until you press E or ENTER, which first
 * finishes writing the line and then moves on) or is <i>called out</i> (the world carries on, and the line goes away by itself a
 * moment after it has been written).
 */
final class Dialogue {
    static final double CHARS_PER_SECOND = 46;
    /** How long a called-out line stays once written, plus a little per letter. */
    static final double HOLD = 1.5, HOLD_PER_CHAR = 0.02;
    private static final int CHARS_PER_BLIP = 3;

    /**
     * @param tag      a small note after the name, e.g. "(from far away)", or ""
     * @param portrait the sprite shown beside the text, e.g. "squirrel.idle"
     * @param waits    true: the world stops until you press a key; false: it is called out while you play
     * @param onShow   run once when the line first appears (moves the camera, changes a pose...), or null
     */
    record Line(String speaker, String tag, String portrait, String text, boolean waits, Snd voice, Runnable onShow) {}

    private final Deque<Line> queue = new ArrayDeque<>();
    private Line line;
    private double shown;          // how many letters of the line are on screen
    private double held;           // seconds the finished line has been on screen
    private int blips;

    void add(Line l) { queue.add(l); }

    /** Says something you have to press a key to get past. */
    void say(String speaker, String portrait, Snd voice, String text) { add(new Line(speaker, "", portrait, text, true, voice, null)); }

    void say(String speaker, String portrait, Snd voice, String text, Runnable onShow) { add(new Line(speaker, "", portrait, text, true, voice, onShow)); }

    /** Calls something out while the game carries on. */
    void shout(String speaker, String tag, String portrait, Snd voice, String text) { add(new Line(speaker, tag, portrait, text, false, voice, null)); }

    /** Drops every line that doesn't wait for a key, the one on screen included (an important one is about to be said). */
    void clearShouts() {
        queue.removeIf(l -> !l.waits());
        if (line != null && !line.waits()) line = null;
    }

    void clear() {
        queue.clear();
        line = null;
    }

    boolean active() { return line != null || !queue.isEmpty(); }

    /** True while a line that waits for a key is on screen: the world stands still. */
    boolean stopsWorld() { return line != null ? line.waits() : !queue.isEmpty() && queue.peek().waits(); }

    Line current() { return line; }

    /** The part of the current line written so far. */
    String visible() { return line == null ? "" : line.text().substring(0, Math.min(line.text().length(), (int) shown)); }

    boolean written() { return line != null && shown >= line.text().length(); }

    /** True when the line on screen is written out and is waiting for you: the box shows its "press E" arrow. */
    boolean waitingForKey() { return stopsWorld() && written(); }

    /** Puts the next queued line on screen (if there is one). */
    private void next() {
        line = queue.poll();
        if (line == null) return;
        shown = 0;
        held = 0;
        blips = 0;
        if (line.onShow() != null) line.onShow().run();
    }

    void update(World w, Input in, double dt) {
        if (line == null) next();
        if (line == null) return;
        if (line.waits() && (in.pressed(KeyEvent.VK_E) || in.pressed(KeyEvent.VK_ENTER))) {
            in.consume(KeyEvent.VK_E, KeyEvent.VK_ENTER);          // the key that turns the page must not also swing the sword
            if (!written()) {
                shown = line.text().length();
                return;
            }
            w.sound(Snd.MENU_MOVE);
            next();                                                   // straight on to the next line, so the world never gets a frame in between
            return;
        }
        String text = line.text();
        shown = Math.min(text.length(), shown + CHARS_PER_SECOND * dt);
        int letters = 0;
        for (int i = 0; i < (int) shown; i++) if (text.charAt(i) != ' ') letters++;
        while (blips * CHARS_PER_BLIP < letters) {
            blips++;
            if (line.voice() != null) w.sound(line.voice());
        }
        if (!line.waits() && written()) {
            held += dt;
            if (held >= HOLD + HOLD_PER_CHAR * text.length()) next();
        }
    }
}
