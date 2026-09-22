package game;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Swing panel that runs the fixed-timestep game loop on the UI thread and paints the world. */
final class GamePanel extends JPanel {
    private static final double STEP = 1.0 / 60.0;

    private final World world = new World(!"off".equals(System.getProperty("spellblade.tutorial")));   // ./run.sh notutorial starts with it switched off
    private final Input input = new Input();
    private final Renderer renderer = new Renderer();
    private final AudioEngine engine = new AudioEngine();
    private final GameAudio audio = new GameAudio(engine);
    private final Timer timer;
    private long last;
    private double accumulator;

    GamePanel() {
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);   // so TAB reaches the game instead of moving focus
        world.audio = AudioSettings.load(AudioSettings.file());
        addKeyListener(input);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { input.clear(); }
        });
        timer = new Timer(4, e -> tick());
    }

    void start() {
        last = System.nanoTime();
        if (!"off".equals(System.getProperty("spellblade.audio"))) engine.start();   // ./run.sh silent turns sound off
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        accumulator += Math.min((now - last) / 1e9, 0.1);   // clamp so a stall doesn't cause a huge catch-up
        last = now;
        boolean stepped = false;
        while (accumulator >= STEP) {
            world.update(STEP, input);
            input.endFrame();
            accumulator -= STEP;
            stepped = true;
        }
        audio.update(world);
        if (world.audio.dirty) world.audio.save(AudioSettings.file());
        if (stepped) repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(world, (Graphics2D) g, getWidth(), getHeight());
    }
}
