package game;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

/**
 * Keyboard state. {@link #down} is "currently held", {@link #pressed} is "went down since the last
 * {@link #endFrame()}". press()/release() are public-ish so tests can drive the game without a window.
 */
final class Input extends KeyAdapter {
    private static final int N = 1024;
    private final boolean[] down = new boolean[N];
    private final boolean[] pressed = new boolean[N];
    private final boolean[] carry = new boolean[N];

    @Override public void keyPressed(KeyEvent e) { press(e.getKeyCode()); }
    @Override public void keyReleased(KeyEvent e) { release(e.getKeyCode()); }

    void press(int key) {
        if (key < 0 || key >= N) return;
        if (!down[key]) pressed[key] = true; // ignore OS key-repeat
        down[key] = true;
    }

    void release(int key) {
        if (key < 0 || key >= N) return;
        down[key] = false;
    }

    boolean down(int key) { return key >= 0 && key < N && down[key]; }

    boolean pressed(int key) { return key >= 0 && key < N && pressed[key]; }

    /**
     * Keeps these keys' fresh presses alive for one more frame. Used while the game is frozen for a moment (hit stop),
     * when nothing reads the input: without it a press made during the freeze would simply be lost.
     */
    void carryOver(int... keys) {
        for (int k : keys) if (k >= 0 && k < N && pressed[k]) carry[k] = true;
    }

    /** Uses up these keys' fresh presses this frame, so nothing else reacts to them (a key that turned a dialogue page must not also attack). */
    void consume(int... keys) {
        for (int k : keys) if (k >= 0 && k < N) pressed[k] = carry[k] = false;
    }

    void endFrame() {
        for (int i = 0; i < N; i++) {
            pressed[i] = carry[i];
            carry[i] = false;
        }
    }

    void clear() {
        Arrays.fill(down, false);
        Arrays.fill(pressed, false);
    }
}
