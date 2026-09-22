package game;

/**
 * The sound engine's toolbox: pitch maths, deterministic noise, filters, envelopes, band-limited oscillators, a
 * reverb, an echo and a limiter. Everything works on {@code float} samples at {@link #SR} and allocates nothing while
 * it runs, so the same code serves both the real-time mixer and the offline sound-effect painter.
 */
final class Dsp {
    private Dsp() {}

    static final int SR = 44100;
    static final double TWO_PI = Math.PI * 2;

    /** Frequency of a MIDI note number (69 = A4 = 440 Hz). */
    static double hz(double midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }

    static double dB(double linear) { return 20 * Math.log10(Math.max(1e-9, linear)); }

    static double lin(double dB) { return Math.pow(10, dB / 20); }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : Math.min(v, hi); }

    /** Smoothing coefficient for a one-pole filter with the given time constant. */
    static double tau(double seconds) { return 1 - Math.exp(-1.0 / (Math.max(1e-4, seconds) * SR)); }

    // ------------------------------------------------------------------ noise

    /** Deterministic white noise (xorshift), so a sound comes out identical every time it is painted. */
    static final class Noise {
        private long s;

        Noise(long seed) {
            s = seed * 0x9E3779B97F4A7C15L + 0x1234567L;
            if (s == 0) s = 1;
        }

        private long step() {
            s ^= s >>> 12;
            s ^= s << 25;
            s ^= s >>> 27;
            return s * 0x2545F4914F6CDD1DL;
        }

        /** White noise in [-1, 1). */
        float next() { return (float) ((step() >>> 40) * (1.0 / (1 << 23)) - 1.0); }

        /** Uniform in [0, 1). */
        double uniform() { return (step() >>> 11) * (1.0 / (1L << 53)); }

        double range(double lo, double hi) { return lo + (hi - lo) * uniform(); }
    }

    // ------------------------------------------------------------------ filters

    /** A biquad filter (RBJ cookbook). Retune it as often as you like; the state carries over. */
    static final class Biquad {
        private double b0 = 1, b1, b2, a1, a2, x1, x2, y1, y2;

        Biquad lowpass(double fc, double q) { return set(0, fc, q); }

        Biquad highpass(double fc, double q) { return set(1, fc, q); }

        Biquad bandpass(double fc, double q) { return set(2, fc, q); }

        Biquad notch(double fc, double q) { return set(3, fc, q); }

        private Biquad set(int kind, double fc, double q) {
            fc = clamp(fc, 20, SR * 0.45);
            double w = TWO_PI * fc / SR, cos = Math.cos(w), sin = Math.sin(w), alpha = sin / (2 * Math.max(0.05, q));
            double a0 = 1 + alpha, nb0, nb1, nb2;
            switch (kind) {
                case 0 -> { nb0 = (1 - cos) / 2; nb1 = 1 - cos; nb2 = nb0; }
                case 1 -> { nb0 = (1 + cos) / 2; nb1 = -(1 + cos); nb2 = nb0; }
                case 2 -> { nb0 = alpha; nb1 = 0; nb2 = -alpha; }
                default -> { nb0 = 1; nb1 = -2 * cos; nb2 = 1; }
            }
            b0 = nb0 / a0;
            b1 = nb1 / a0;
            b2 = nb2 / a0;
            a1 = -2 * cos / a0;
            a2 = (1 - alpha) / a0;
            return this;
        }

        float process(float x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return (float) y;
        }

        void reset() { x1 = x2 = y1 = y2 = 0; }
    }

    /** A one-pole low-pass: cheap smoothing and gentle tone control. */
    static final class OnePole {
        private double a, y;

        OnePole(double cutoffHz) { setCutoff(cutoffHz); }

        void setCutoff(double hz) { a = 1 - Math.exp(-TWO_PI * clamp(hz, 5, SR * 0.45) / SR); }

        float process(float x) {
            y += a * (x - y);
            return (float) y;
        }
    }

    /** A one-pole high-pass: removes DC offset and rumble (at ~20 Hz) or keeps mud out of a reverb (at ~150 Hz). */
    static final class HighPass {
        private final double a;
        private double x1, y1;

        HighPass(double cutoffHz) { a = Math.exp(-TWO_PI * cutoffHz / SR); }

        float process(float x) {
            double y = a * (y1 + x - x1);
            x1 = x;
            y1 = y;
            return (float) y;
        }
    }

    /**
     * The A-weighting curve, the standard model of how sensitive the ear is at each pitch: it barely hears 60 Hz, hears
     * best around 3 kHz. Measuring loudness through it says how loud a sound really seems, not how much raw energy it has.
     * Built from first-order sections (bilinear transform), normalised to 0 dB at 1 kHz.
     */
    static final class AWeight {
        private final double[][] c = new double[6][];        // b0, b1, a1 per stage
        private final double[] x1 = new double[6], y1 = new double[6];
        private final double norm;

        AWeight() {
            double[] f = {20.6, 20.6, 107.7, 737.9, 12194, 12194};
            for (int i = 0; i < 6; i++) {
                double w = Math.tan(Math.PI * f[i] / SR);
                double a1 = (w - 1) / (w + 1);
                c[i] = i < 4 ? new double[]{1 / (1 + w), -1 / (1 + w), a1} : new double[]{w / (1 + w), w / (1 + w), a1};
            }
            // measure the gain at 1 kHz so the weighting reads 0 dB there
            double peak = 0;
            for (int n = 0; n < SR; n++) {
                double y = run(Math.sin(TWO_PI * 1000 * n / SR), false);
                if (n > SR / 2) peak = Math.max(peak, Math.abs(y));
            }
            java.util.Arrays.fill(x1, 0);
            java.util.Arrays.fill(y1, 0);
            norm = 1 / peak;
        }

        private double run(double x, boolean normalise) {
            double v = x;
            for (int i = 0; i < 6; i++) {
                double y = c[i][0] * v + c[i][1] * x1[i] - c[i][2] * y1[i];
                x1[i] = v;
                y1[i] = y;
                v = y;
            }
            return normalise ? v * norm : v;
        }

        float process(float x) { return (float) run(x, true); }
    }

    // ------------------------------------------------------------------ envelope

    /**
     * Attack (linear), then decay toward a sustain level, then release. Decay and release times are "time to fall by
     * 40 dB", which is how the ear reads a fade.
     */
    static final class Adsr {
        private final double attackStep, decayCoef, sustain, releaseCoef;
        private int stage;                 // 0 attack, 1 decay / sustain, 2 release, 3 finished
        private double level;

        Adsr(double attack, double decay, double sustain, double release) {
            this.attackStep = attack <= 0 ? 1 : 1.0 / (attack * SR);
            this.decayCoef = tau(decay / 4.6);
            this.sustain = sustain;
            this.releaseCoef = tau(release / 4.6);
        }

        void release() { if (stage < 2) stage = 2; }

        boolean finished() { return stage == 3; }

        double level() { return level; }

        float next() {
            switch (stage) {
                case 0 -> {
                    level += attackStep;
                    if (level >= 1) { level = 1; stage = 1; }
                }
                case 1 -> level += (sustain - level) * decayCoef;
                case 2 -> {
                    level -= level * releaseCoef;
                    if (level < 1e-4) { level = 0; stage = 3; }
                }
                default -> { }
            }
            return (float) level;
        }
    }

    // ------------------------------------------------------------------ oscillators

    /** Polynomial band-limited step correction: removes the aliasing a naive saw or square would have. */
    private static double blep(double t, double dt) {
        if (t < dt) {
            t /= dt;
            return t + t - t * t - 1;
        }
        if (t > 1 - dt) {
            t = (t - 1) / dt;
            return t * t + t + t + 1;
        }
        return 0;
    }

    /** Band-limited sawtooth for a phase in [0, 1) and a phase step {@code dt} = frequency / sample rate. */
    static double saw(double phase, double dt) { return 2 * phase - 1 - blep(phase, dt); }

    /** Band-limited square (or pulse, with {@code width} = the high part of the cycle). */
    static double square(double phase, double dt, double width) {
        double v = phase < width ? 1 : -1;
        v += blep(phase, dt);
        double p2 = phase - width;
        if (p2 < 0) p2 += 1;
        v -= blep(p2, dt);
        return v;
    }

    static double triangle(double phase) { return 4 * Math.abs(phase - 0.5) - 1; }

    // ------------------------------------------------------------------ effects

    /** A stereo Freeverb-style reverb: 8 combs and 4 all-passes per side. */
    static final class Reverb {
        private static final int[] COMBS = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        private static final int[] ALLPASS = {556, 441, 341, 225};
        private static final int SPREAD = 23;

        private final float[][] combL = new float[8][], combR = new float[8][], apL = new float[4][], apR = new float[4][];
        private final int[] ciL = new int[8], ciR = new int[8], aiL = new int[4], aiR = new int[4];
        private final float[] storeL = new float[8], storeR = new float[8];
        private final float feedback, damp;

        /** {@code room} 0..1 sets the tail length, {@code damping} 0..1 how quickly highs die away. */
        Reverb(double room, double damping) {
            this.feedback = (float) (0.70 + 0.27 * clamp(room, 0, 1));
            this.damp = (float) (0.05 + 0.6 * clamp(damping, 0, 1));
            for (int i = 0; i < 8; i++) {
                combL[i] = new float[COMBS[i]];
                combR[i] = new float[COMBS[i] + SPREAD];
            }
            for (int i = 0; i < 4; i++) {
                apL[i] = new float[ALLPASS[i]];
                apR[i] = new float[ALLPASS[i] + SPREAD];
            }
        }

        /** Adds the wet signal for {@code in} (mono) to the outputs. */
        void process(float[] in, float[] outL, float[] outR, int n, float wet) {
            for (int s = 0; s < n; s++) {
                float x = in[s] * 0.015f;
                float l = 0, r = 0;
                for (int i = 0; i < 8; i++) {
                    float[] bl = combL[i];
                    float yl = bl[ciL[i]];
                    storeL[i] = yl * (1 - damp) + storeL[i] * damp;
                    bl[ciL[i]] = x + storeL[i] * feedback;
                    if (++ciL[i] == bl.length) ciL[i] = 0;
                    l += yl;
                    float[] br = combR[i];
                    float yr = br[ciR[i]];
                    storeR[i] = yr * (1 - damp) + storeR[i] * damp;
                    br[ciR[i]] = x + storeR[i] * feedback;
                    if (++ciR[i] == br.length) ciR[i] = 0;
                    r += yr;
                }
                for (int i = 0; i < 4; i++) {
                    float[] bl = apL[i];
                    float bufL = bl[aiL[i]];
                    bl[aiL[i]] = l + bufL * 0.5f;
                    if (++aiL[i] == bl.length) aiL[i] = 0;
                    l = bufL - l;
                    float[] br = apR[i];
                    float bufR = br[aiR[i]];
                    br[aiR[i]] = r + bufR * 0.5f;
                    if (++aiR[i] == br.length) aiR[i] = 0;
                    r = bufR - r;
                }
                outL[s] += l * wet;
                outR[s] += r * wet;
            }
        }
    }

    /** A stereo ping-pong echo with a darkening feedback path. */
    static final class Echo {
        private final float[] bufL, bufR;
        private final int delay;
        private int pos;
        private final float feedback;
        private final OnePole toneL = new OnePole(3200), toneR = new OnePole(3200);
        private float lastL, lastR;

        Echo(double seconds, double feedback) {
            this.delay = Math.max(1, (int) (seconds * SR));
            this.bufL = new float[delay];
            this.bufR = new float[delay];
            this.feedback = (float) feedback;
        }

        void process(float[] in, float[] outL, float[] outR, int n, float wet) {
            for (int s = 0; s < n; s++) {
                float dl = bufL[pos], dr = bufR[pos];
                bufL[pos] = toneL.process(in[s] + dr * feedback);     // crossing the sides makes it bounce
                bufR[pos] = toneR.process(dl * feedback);
                if (++pos == delay) pos = 0;
                outL[s] += dl * wet;
                outR[s] += dr * wet;
            }
        }
    }

    /**
     * A look-ahead peak limiter: it starts turning the level down a millisecond and a half before a peak arrives, so
     * loud moments are tamed without clicks or crunch. Works on interleaved stereo.
     */
    static final class Limiter {
        private static final int LOOK = 96;
        private final float ceiling;
        private final float[] delayL = new float[LOOK], delayR = new float[LOOK], need = new float[LOOK];
        private int pos;
        private double gain = 1;
        private final double release = tau(0.12), attack = tau(LOOK / (double) SR / 5);

        Limiter(double ceiling) {
            this.ceiling = (float) ceiling;
            java.util.Arrays.fill(need, 1f);
        }

        /** Processes {@code frames} stereo frames in place. */
        void process(float[] l, float[] r, int frames) {
            for (int i = 0; i < frames; i++) {
                float pk = Math.max(Math.abs(l[i]), Math.abs(r[i]));
                need[pos] = pk > ceiling ? ceiling / pk : 1f;
                float outL = delayL[pos], outR = delayR[pos];
                delayL[pos] = l[i];
                delayR[pos] = r[i];
                pos = (pos + 1) % LOOK;
                float target = 1f;
                for (int k = 0; k < LOOK; k++) if (need[k] < target) target = need[k];
                gain += (target - gain) * (target < gain ? attack : release);
                l[i] = clamp((float) (outL * gain), -0.995f, 0.995f);
                r[i] = clamp((float) (outR * gain), -0.995f, 0.995f);
            }
        }
    }
}
