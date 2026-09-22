package game;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** The player's volume choices (0 to 10 steps for music and effects, and a mute switch), remembered between runs. */
final class AudioSettings {
    static final int STEPS = 10;

    int music = 8, sfx = 9;
    boolean muted;
    /** Set when something changed and hasn't been saved yet. */
    boolean dirty;

    /** Volume steps are spread on a curve, because loudness is heard logarithmically: half the steps is well under half the volume. */
    static double curve(int step) { return Math.pow(Dsp.clamp(step, 0, STEPS) / (double) STEPS, 1.5); }

    double musicVolume() { return curve(music); }

    double sfxVolume() { return curve(sfx); }

    void toggleMute() {
        muted = !muted;
        dirty = true;
    }

    /** @return true if the value changed. */
    boolean adjustMusic(int delta) {
        int v = (int) Dsp.clamp(music + delta, 0, STEPS);
        if (v == music) return false;
        music = v;
        dirty = true;
        return true;
    }

    boolean adjustSfx(int delta) {
        int v = (int) Dsp.clamp(sfx + delta, 0, STEPS);
        if (v == sfx) return false;
        sfx = v;
        dirty = true;
        return true;
    }

    static Path file() { return Path.of(System.getProperty("user.home"), ".spellblade", "audio.properties"); }

    /** Reads the saved settings; anything missing or unreadable falls back to the defaults. */
    static AudioSettings load(Path path) {
        AudioSettings s = new AudioSettings();
        try (InputStream in = Files.newInputStream(path)) {
            Properties p = new Properties();
            p.load(in);
            s.music = (int) Dsp.clamp(Integer.parseInt(p.getProperty("music", "" + s.music).trim()), 0, STEPS);
            s.sfx = (int) Dsp.clamp(Integer.parseInt(p.getProperty("sfx", "" + s.sfx).trim()), 0, STEPS);
            s.muted = Boolean.parseBoolean(p.getProperty("muted", "false").trim());
        } catch (IOException | RuntimeException e) {
            // no saved settings yet, or a damaged file: use the defaults
        }
        return s;
    }

    void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Properties p = new Properties();
            p.setProperty("music", "" + music);
            p.setProperty("sfx", "" + sfx);
            p.setProperty("muted", "" + muted);
            try (OutputStream out = Files.newOutputStream(path)) {
                p.store(out, "Spellblade sound settings");
            }
            dirty = false;
        } catch (IOException | RuntimeException e) {
            dirty = false;    // not being able to save is not worth interrupting the game for
        }
    }
}
