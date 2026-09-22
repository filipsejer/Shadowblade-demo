package game;

import game.Song.Mood;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * The mixer. Music, sound effects and ambience are mixed here, 44.1 kHz stereo, in small blocks by a dedicated audio
 * thread, then pushed to the speakers. The game thread only ever drops requests in a queue, so sound can never slow the
 * game down, and if there is no sound device at all the game simply runs silent.
 *
 * <p>Mixing is separate from output: {@link #render} produces the next stretch of audio and can be called by anything,
 * which is how the tests listen to the game without a speaker.
 *
 * <p>The signal path: (music, ducked when big effects play) + effects + ambience, times the volumes, then a DC blocker,
 * a look-ahead peak limiter that keeps everything under about -0.5 dBFS, and 16-bit conversion with a whisper of
 * dither so quiet fades stay smooth.
 */
final class AudioEngine {
    static final int BLOCK = 512;
    private static final int MAX_VOICES = 40;
    /** Size of the speaker's buffer in frames: about 46 ms. Smaller is snappier but risks crackles on a busy machine. */
    static final int LINE_FRAMES = 2048;

    // ------------------------------------------------------------------ settings (read by the audio thread)
    private volatile float master = 0.8f, musicVolume = 0.75f, sfxVolume = 1f, ambienceVolume = 0.8f;
    private volatile boolean muted;
    private volatile boolean snapRequested;

    // ------------------------------------------------------------------ audio-thread state
    private final ConcurrentLinkedQueue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger();
    private static final int MAX_PENDING_CUES = 1000;
    private final Music music = new Music();
    private volatile SfxBank bank;
    private final List<Play> playing = new ArrayList<>();
    private final long[] lastStart = new long[Snd.values().length];
    private final int[] nextVariant = new int[Snd.values().length];
    {
        java.util.Arrays.fill(lastStart, Long.MIN_VALUE / 2);      // "long ago", so the very first cue is never mistaken for a repeat
    }
    private long clock;
    private final Ambience ambience = new Ambience();
    private final Dsp.Limiter limiter = new Dsp.Limiter(0.93);
    private final Dsp.HighPass dcL = new Dsp.HighPass(18), dcR = new Dsp.HighPass(18);
    private double musicVol = 0.75, sfxVol = 1, ambVol = 0.8, masterVol = 0.8, muteGain = 1, duckGain = 1, duckEnv;
    private final Random dither = new Random(11);
    private final float[] mL = new float[BLOCK], mR = new float[BLOCK], sL = new float[BLOCK], sR = new float[BLOCK],
        aL = new float[BLOCK], aR = new float[BLOCK], oL = new float[BLOCK], oR = new float[BLOCK];

    // ------------------------------------------------------------------ output
    private volatile boolean running;
    private volatile String status = "audio not started";
    private Thread thread;
    private long underruns;
    private volatile int ambienceEvents;
    private volatile String lastError;

    private static final class Play {
        final Snd snd;
        final float[] data;
        double pos;
        final double rate;
        final float gl, gr;
        int delay;
        boolean fading;
        double fade = 1;

        Play(Snd snd, float[] data, double rate, float gl, float gr, int delay) {
            this.snd = snd;
            this.data = data;
            this.rate = rate;
            this.gl = gl;
            this.gr = gr;
            this.delay = delay;
        }
    }

    // ================================================================== game-thread API

    /** Installs the painted sound effects (called once the bank is ready). */
    void setBank(SfxBank bank) { this.bank = bank; }

    boolean hasBank() { return bank != null; }

    void setVolumes(double master, double music, double sfx, double ambience, boolean muted) {
        this.master = (float) Dsp.clamp(master, 0, 1);
        this.musicVolume = (float) Dsp.clamp(music, 0, 1);
        this.sfxVolume = (float) Dsp.clamp(sfx, 0, 1);
        this.ambienceVolume = (float) Dsp.clamp(ambience, 0, 1);
        this.muted = muted;
        if (!snapRequested) {                       // the first volumes are applied at once; later changes glide
            snapRequested = true;
            enqueue(this::snapVolumes, false);
        }
    }

    private void snapVolumes() {
        masterVol = master;
        musicVol = musicVolume;
        sfxVol = sfxVolume;
        ambVol = ambienceVolume;
        muteGain = muted ? 0 : 1;
    }

    /**
     * Plays a sound effect. {@code gain} scales it (1 = as designed), {@code pan} places it from -1 (left) to 1 (right),
     * {@code delay} holds it back that many seconds, and {@code rate} changes its pitch (1 = as painted).
     */
    void cue(Snd snd, double gain, double pan, double delay, double rate) {
        enqueue(() -> start(snd, gain, pan, delay, rate), true);      // if nothing is listening (sound off), cues are dropped, not hoarded
    }

    void cue(Snd snd) { cue(snd, 1, 0, 0, 1); }

    /** Sets which music should be playing. {@code song == null} fades it out. Cheap to call every frame. */
    void setMusic(Song song, Mood mood, boolean paused, double fadeOutSeconds) {
        enqueue(() -> {
            music.play(song, mood, fadeOutSeconds);
            music.setPaused(paused);
        }, false);
    }

    /** Sets the background bed: 0 none, 1 forest wind and birds, 2 city hum and traffic; {@code level} 0..1. */
    void setAmbience(int bed, double level) {
        enqueue(() -> ambience.set(bed, level), false);
    }

    private void enqueue(Runnable r, boolean droppable) {
        if (droppable && pending.get() >= MAX_PENDING_CUES) return;
        pending.incrementAndGet();
        commands.add(r);
    }

    /** Requests waiting for the audio thread (for tests). */
    int pendingCommands() { return pending.get(); }

    String status() { return status; }

    /** The last problem the audio thread recovered from, or null. */
    String lastError() { return lastError; }

    boolean running() { return running; }

    long underruns() { return underruns; }

    // ================================================================== output thread

    /**
     * Opens the speakers and starts the audio thread. The sound effects are painted first on that thread (a fraction
     * of a second) so the game window never waits. If anything goes wrong the game just stays silent.
     */
    void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::run, "audio");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY - 1);
        thread.start();
    }

    void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    private void run() {
        if (bank == null) {                    // paint the effects while the speakers are being opened (opening can take seconds)
            Thread painter = new Thread(() -> {
                try {
                    bank = SfxBank.build();
                } catch (Throwable t) {
                    status = "could not paint the sound effects: " + t;
                }
            }, "audio-bank");
            painter.setDaemon(true);
            painter.start();
        }
        SourceDataLine line = null;
        try {
            AudioFormat fmt = new AudioFormat(Dsp.SR, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) {
                status = "no audio output available; running silent";
                running = false;
                return;
            }
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, LINE_FRAMES * 4);
            line.start();
            status = "playing (" + Dsp.SR + " Hz stereo)";
        } catch (Throwable t) {
            status = "audio unavailable: " + t.getMessage();
            running = false;
            if (line != null) line.close();
            return;
        }
        byte[] out = new byte[BLOCK * 4];
        float[] inter = new float[BLOCK * 2];
        try {
            boolean primed = false;
            int errors = 0;
            while (running) {
                if (primed && line.available() >= line.getBufferSize() - 16) underruns++;   // it ran dry: the thread was starved
                try {
                    render(inter, BLOCK);
                    errors = 0;
                } catch (RuntimeException e) {          // a bug in one block must not silence the game for good: play silence and carry on
                    java.util.Arrays.fill(inter, 0f);
                    lastError = e.toString();
                    if (++errors > 200) throw e;
                }
                toBytes(inter, BLOCK, out);
                line.write(out, 0, out.length);
                primed = true;
            }
        } catch (Throwable t) {
            status = "audio stopped: " + t.getMessage();
        } finally {
            running = false;
            if (status.startsWith("playing")) status = "stopped";
            try {
                line.stop();
                line.close();
            } catch (Throwable ignored) { }
        }
    }

    /** 16-bit little-endian with triangular dither of a third of a step, so very quiet fades don't get grainy. */
    private void toBytes(float[] inter, int frames, byte[] out) {
        for (int i = 0; i < frames * 2; i++) {
            double v = inter[i] * 32767.0 + (dither.nextDouble() - dither.nextDouble()) * 0.5;
            int s = (int) Math.round(Dsp.clamp(v, -32768, 32767));
            out[i * 2] = (byte) s;
            out[i * 2 + 1] = (byte) (s >> 8);
        }
    }

    // ================================================================== mixing

    /** Produces the next {@code frames} stereo frames into {@code inter} (L, R, L, R...). */
    void render(float[] inter, int frames) {
        for (int done = 0; done < frames; ) {
            int n = Math.min(BLOCK, frames - done);
            block(n);
            for (int i = 0; i < n; i++) {
                inter[(done + i) * 2] = oL[i];
                inter[(done + i) * 2 + 1] = oR[i];
            }
            done += n;
        }
    }

    private void block(int n) {
        for (Runnable r; (r = commands.poll()) != null; ) {
            pending.decrementAndGet();
            r.run();
        }
        java.util.Arrays.fill(mL, 0, n, 0f);
        java.util.Arrays.fill(mR, 0, n, 0f);
        java.util.Arrays.fill(sL, 0, n, 0f);
        java.util.Arrays.fill(sR, 0, n, 0f);
        java.util.Arrays.fill(aL, 0, n, 0f);
        java.util.Arrays.fill(aR, 0, n, 0f);

        music.render(mL, mR, n);
        double sfxPeak = mixEffects(n);
        ambience.render(aL, aR, n, this);

        // big effects duck the music a little, like a mix engineer riding the fader
        duckEnv = Math.max(sfxPeak, duckEnv * Math.pow(0.5, n / (Dsp.SR * 0.35)));
        double duckTarget = 1 - 0.28 * Dsp.clamp((duckEnv - 0.5) / 0.4, 0, 1);       // only the big moments (peaks above about -6 dB) duck the music
        double duckStep = (duckTarget - duckGain) / n;

        double cVol = Dsp.tau(0.03);
        for (int i = 0; i < n; i++) {
            musicVol += (musicVolume - musicVol) * cVol;
            sfxVol += (sfxVolume - sfxVol) * cVol;
            ambVol += (ambienceVolume - ambVol) * cVol;
            masterVol += (master - masterVol) * cVol;
            muteGain += ((muted ? 0 : 1) - muteGain) * Dsp.tau(0.02);
            duckGain += duckStep;
            double g = masterVol * muteGain;
            double m = musicVol * duckGain;
            oL[i] = (float) ((mL[i] * m + sL[i] * sfxVol + aL[i] * ambVol) * g);
            oR[i] = (float) ((mR[i] * m + sR[i] * sfxVol + aR[i] * ambVol) * g);
            oL[i] = dcL.process(oL[i]);
            oR[i] = dcR.process(oR[i]);
        }
        limiter.process(oL, oR, n);
        clock += n;
    }

    /** Mixes the playing sound effects into the effects bus. @return the loudest sample, for ducking. */
    private double mixEffects(int n) {
        double peak = 0;
        for (int k = playing.size() - 1; k >= 0; k--) {
            Play p = playing.get(k);
            float[] d = p.data;
            boolean finished = false;
            for (int i = 0; i < n; i++) {
                if (p.delay > 0) { p.delay--; continue; }
                int idx = (int) p.pos;
                if (idx + 1 >= d.length) { finished = true; break; }
                float frac = (float) (p.pos - idx);
                float s = d[idx] + (d[idx + 1] - d[idx]) * frac;
                if (p.fading) {
                    p.fade -= 1.0 / (0.010 * Dsp.SR);
                    if (p.fade <= 0) { finished = true; break; }
                    s *= (float) p.fade;
                }
                sL[i] += s * p.gl;
                sR[i] += s * p.gr;
                p.pos += p.rate;
            }
            if (finished) playing.remove(k);
        }
        for (int i = 0; i < n; i++) peak = Math.max(peak, Math.max(Math.abs(sL[i]), Math.abs(sR[i])));
        return peak * sfxVol;
    }

    /** Starts a sound now (audio thread): applies the sound's minimum gap and voice limit. */
    private void start(Snd snd, double gain, double pan, double delay, double rate) {
        SfxBank b = bank;
        if (b == null || gain < 0.01) return;
        int idx = snd.ordinal();
        if (delay <= 0 && clock - lastStart[idx] < snd.minGap * Dsp.SR) return;   // too soon after the last one
        int same = 0;
        Play oldest = null;
        for (Play p : playing) {
            if (p.snd == snd && !p.fading) {
                same++;
                if (oldest == null) oldest = p;
            }
        }
        if (same >= snd.voices && oldest != null) oldest.fading = true;            // make room, with a quick fade so it can't click
        long active = playing.stream().filter(p -> !p.fading).count();
        if (active >= MAX_VOICES) {
            for (Play p : playing) if (!p.fading) { p.fading = true; break; }
        }
        int variant = nextVariant[idx]++ % b.variants(snd);
        double a = (Dsp.clamp(pan, -1, 1) + 1) * Math.PI / 4;
        float gl = (float) (Math.cos(a) * Math.sqrt(2) * gain), gr = (float) (Math.sin(a) * Math.sqrt(2) * gain);
        playing.add(new Play(snd, b.get(snd, variant), Dsp.clamp(rate, 0.5, 2.0), gl, gr, (int) (delay * Dsp.SR)));
        if (delay <= 0) lastStart[idx] = clock;
    }

    /** Starts a sound from inside the audio thread (used by ambience). */
    private void startNow(Snd snd, double gain, double pan) {
        ambienceEvents++;
        start(snd, gain, pan, 0, 1);
    }

    /** The current music ducking gain (1 = none), for tests. */
    double duckGain() { return duckGain; }

    /** How many birds and horns the ambience has started (for tests). */
    int ambienceEvents() { return ambienceEvents; }

    // ================================================================== inspection (tests)

    Music music() { return music; }

    int voicesPlaying() { return playing.size(); }

    // ================================================================== ambience

    /**
     * The background bed under everything. Forest: wind that breathes in and out, and now and then a bird. City: the low
     * hum of a big place at night and traffic swelling in the distance, and a far-off horn once in a while. It is made
     * live from filtered noise, so it never loops.
     */
    private static final class Ambience {
        private int bed;                       // 0 none, 1 forest, 2 city
        private double level = 1;
        private double forest, city, lab;      // current bed volumes (fade)
        private final Dsp.Noise noise = new Dsp.Noise(77);
        private final Dsp.Biquad windBand = new Dsp.Biquad(), windHiss = new Dsp.Biquad(), trafficBand = new Dsp.Biquad(), ventBand = new Dsp.Biquad();
        private final Dsp.OnePole rumble = new Dsp.OnePole(140);
        private double t, lfoA, lfoB, nextEvent = 3 * Dsp.SR;
        private long samples;

        void set(int bed, double level) {
            this.bed = bed;
            this.level = level;
        }

        void render(float[] l, float[] r, int n, AudioEngine engine) {
            double targetF = bed == 1 ? level : 0, targetC = bed == 2 ? level : 0, targetL = bed == 3 ? level : 0;
            double fade = Dsp.tau(1.6);
            for (int i = 0; i < n; i++) {
                forest += (targetF - forest) * fade;
                city += (targetC - city) * fade;
                lab += (targetL - lab) * fade;
                t += 1.0 / Dsp.SR;
                lfoA = 0.5 + 0.5 * Math.sin(Dsp.TWO_PI * 0.071 * t);
                lfoB = 0.5 + 0.5 * Math.sin(Dsp.TWO_PI * 0.043 * t + 1.3);
                float w = noise.next();
                float left = 0, right = 0;
                if (forest > 0.0005) {
                    if ((samples & 63) == 0) {
                        windBand.bandpass(260 + 520 * lfoA, 0.8);
                        windHiss.bandpass(2600 + 900 * lfoB, 0.7);
                    }
                    float wind = windBand.process(w) * (float) (0.55 + 0.45 * lfoA) + windHiss.process(w) * (float) (0.05 * lfoB * lfoB);
                    left += wind * (float) (0.030 * forest);
                    right += wind * (float) (0.030 * forest);
                }
                if (city > 0.0005) {
                    if ((samples & 63) == 0) trafficBand.bandpass(180 + 160 * lfoB, 0.7);
                    double hum = Math.sin(Dsp.TWO_PI * 50 * t) * 0.5 + Math.sin(Dsp.TWO_PI * 100.3 * t) * 0.25 + Math.sin(Dsp.TWO_PI * 150.2 * t) * 0.08;
                    float traffic = trafficBand.process(w) * (float) (0.3 + 0.7 * lfoA * lfoA) + rumble.process(w) * 0.15f;
                    float v = (float) (hum * 0.010 + traffic * 0.045) * (float) city;
                    left += v;
                    right += v;
                }
                if (lab > 0.0005) {                                   // the laboratory: fluorescent hum and the breath of the ventilation
                    if ((samples & 63) == 0) ventBand.bandpass(650 + 500 * lfoA, 0.9);
                    double hum = Math.sin(Dsp.TWO_PI * 100 * t) * 0.5 + Math.sin(Dsp.TWO_PI * 200.4 * t) * 0.2 + Math.sin(Dsp.TWO_PI * 300.2 * t) * 0.07;
                    float vent = ventBand.process(w) * (float) (0.4 + 0.6 * lfoB);
                    float v = (float) (hum * (0.007 + 0.002 * lfoA) + vent * 0.03) * (float) lab;
                    left += v;
                    right += v;
                }
                l[i] += left;
                r[i] += right;
                samples++;
            }
            nextEvent -= n;
            if (nextEvent <= 0) {
                if (bed == 1 && forest > 0.3) engine.startNow(Snd.BIRD, 0.6 + 0.4 * level, noise.range(-0.8, 0.8));
                if (bed == 2 && city > 0.3) engine.startNow(Snd.CITY_HORN, 0.7, noise.range(-0.7, 0.7));
                if (bed == 3 && lab > 0.3) engine.startNow(noise.uniform() < 0.6 ? Snd.LAB_BEEP : Snd.LAB_BUBBLE, 0.7, noise.range(-0.8, 0.8));
                nextEvent = (bed == 2 ? noise.range(22, 55) : bed == 3 ? noise.range(5, 14) : noise.range(4, 12)) * Dsp.SR;
            }
        }
    }
}
