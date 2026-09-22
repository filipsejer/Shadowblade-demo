package game;

import static game.Dsp.SR;
import static game.Dsp.TWO_PI;

/**
 * The band. Each instrument is a small synthesiser voice that makes one note: {@link Voice#next()} returns the next
 * sample, {@link Voice#release()} is the key coming up, and {@link Voice#done()} says when the tail has died away.
 * Music notes are made on the fly from these, so nothing has to be stored and the music can change with the action.
 *
 * <p>Notes for each drum kit use General MIDI numbers: 36 kick, 38 snare, 39 clap, 41 / 45 / 48 low / mid / high tom,
 * 42 closed hat, 46 open hat, 70 shaker, 76 wood block.
 */
final class Instruments {
    private Instruments() {}

    abstract static class Voice {
        abstract float next();

        abstract void release();

        abstract boolean done();
    }

    enum Inst {
        FLUTE, HARP, PAD_WARM, PAD_DARK, PAD_SYNTH, BASS_ROUND, BASS_SYNTH, BASS_GROWL, FIDDLE, EPIANO, BRASS,
        LEAD_SYNTH, ARP_SYNTH, STAB, DRUMS_FOLK, DRUMS_ELECTRO, DRUMS_WAR, THEREMIN, ORGAN
    }

    /** Makes one note. {@code vel} is 0..1; {@code seed} keeps every note's noise and starting phase different. */
    static Voice make(Inst inst, int midi, float vel, long seed) {
        double f = Dsp.hz(midi);
        return switch (inst) {
            case FLUTE -> new Flute(f, vel, seed);
            case HARP -> new Pluck(f, vel, seed, 0.9975, 0.55);
            case PAD_WARM -> new Pad(f, vel, seed, 3, 9, 1500, 0.9, 1.6, false);
            case PAD_DARK -> new Pad(f, vel, seed, 4, 12, 850, 0.5, 1.2, false);
            case PAD_SYNTH -> new Pad(f, vel, seed, 3, 14, 2200, 0.35, 1.1, true);
            case BASS_ROUND -> new Bass(f, vel, seed, 0);
            case BASS_SYNTH -> new Bass(f, vel, seed, 1);
            case BASS_GROWL -> new Bass(f, vel, seed, 2);
            case FIDDLE -> new Strings(f, vel, seed, 2600, 0.012, 0.16);
            case STAB -> new Strings(f, vel, seed, 3400, 0.006, 0.11);
            case EPIANO -> new EPiano(f, vel, seed);
            case BRASS -> new Brass(f, vel, seed);
            case LEAD_SYNTH -> new Lead(f, vel, seed);
            case ARP_SYNTH -> new SynthPluck(f, vel, seed);
            case DRUMS_FOLK -> new Drum(0, midi, vel, seed);
            case DRUMS_ELECTRO -> new Drum(1, midi, vel, seed);
            case DRUMS_WAR -> new Drum(2, midi, vel, seed);
            case THEREMIN -> new Theremin(f, vel, seed);
            case ORGAN -> new Organ(f, vel, seed);
        };
    }

    private static double startPhase(Dsp.Noise n) { return n.uniform(); }

    // ------------------------------------------------------------------ melodic voices

    /** A breathy flute: a soft tone with a few harmonics, a little vibrato that grows in, and a puff of air. */
    private static final class Flute extends Voice {
        private final double freq;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.07, 0.2, 0.85, 0.22);
        private final Dsp.Noise noise;
        private final Dsp.Biquad breath = new Dsp.Biquad();
        private double ph;
        private long n;

        Flute(double freq, float vel, long seed) {
            this.freq = freq;
            this.vel = vel;
            this.noise = new Dsp.Noise(seed);
            this.ph = startPhase(noise);
            breath.bandpass(Math.min(6000, freq * 3 + 600), 1.1);
        }

        @Override float next() {
            double t = n++ / (double) SR;
            double vib = Math.sin(TWO_PI * 5.2 * t) * 0.0048 * Math.min(1, t / 0.45);
            double scoop = t < 0.06 ? -0.012 * (1 - t / 0.06) : 0;
            ph += freq * (1 + vib + scoop) / SR;
            if (ph >= 1) ph -= 1;
            double p = ph * TWO_PI;
            double tone = Math.sin(p) + 0.30 * Math.sin(2 * p) + 0.09 * Math.sin(3 * p) + 0.025 * Math.sin(4 * p);
            float e = env.next();
            float air = breath.process(noise.next()) * 0.10f;
            return (float) ((tone * 0.42 + air) * e * vel);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /**
     * A plucked string (Karplus-Strong): a burst of noise circulating in a tuned loop that loses its highs a little
     * on every lap, exactly like a real string. The loop delay is fractional, so it is in tune at every pitch.
     */
    static final class Pluck extends Voice {
        private final float[] buf;
        private final int size;
        private final int whole;
        private final float frac;
        private final float decay;
        private int w;
        private float damp = 1;
        private final float vel;
        private long age;
        private final long maxAge;
        private float follow;
        private boolean released;

        Pluck(double freq, float vel, long seed, double decay, double brightness) {
            double d = SR / freq - 0.5;
            this.whole = (int) d;
            this.frac = (float) (d - whole);
            this.size = whole + 4;
            this.buf = new float[size];
            this.decay = (float) decay;
            this.vel = vel;
            this.maxAge = (long) (SR * 5.5);
            Dsp.Noise noise = new Dsp.Noise(seed);
            Dsp.OnePole soften = new Dsp.OnePole(600 + 9000 * brightness * (0.35 + 0.65 * vel));
            for (int i = 0; i < size; i++) buf[i] = soften.process(noise.next());
            float mean = 0;
            for (float v : buf) mean += v;
            mean /= size;
            for (int i = 0; i < size; i++) buf[i] = (buf[i] - mean) * 1.9f * vel;
        }

        private float tap(int delaySamples) {
            int i = w - delaySamples;
            if (i < 0) i += size;
            return buf[i];
        }

        @Override float next() {
            float a = tap(whole) + (tap(whole + 1) - tap(whole)) * frac;
            float b = tap(whole + 1) + (tap(whole + 2) - tap(whole + 1)) * frac;
            float y = 0.5f * (a + b);
            buf[w] = y * decay * damp;
            if (++w == size) w = 0;
            if (released && damp > 0.9f) damp -= 0.00002f;                   // a lifted finger shortens the ring
            age++;
            float e = Math.abs(a);
            follow += (e - follow) * 0.0005f;
            return age < 48 ? a * (age / 48f) : a;             // a pluck's edge takes about a millisecond to arrive, never a click
        }

        @Override void release() { released = true; }

        @Override boolean done() { return age > maxAge || (age > SR / 4 && follow < 0.00015f); }
    }

    /** A warm pad: a few slightly detuned saws through a low-pass, swelling in and out slowly. */
    private static final class Pad extends Voice {
        private final double[] phase, step;
        private final float vel;
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private final Dsp.Adsr env;
        private final boolean chorus;
        private long n;

        Pad(double freq, float vel, long seed, int voices, double cents, double cutoff, double attack, double release, boolean chorus) {
            this.vel = vel;
            this.chorus = chorus;
            Dsp.Noise noise = new Dsp.Noise(seed);
            phase = new double[voices];
            step = new double[voices];
            for (int i = 0; i < voices; i++) {
                double c = voices == 1 ? 0 : (i / (double) (voices - 1) * 2 - 1) * cents;
                step[i] = freq * Math.pow(2, c / 1200) / SR;
                phase[i] = startPhase(noise);
            }
            filter.lowpass(Math.min(cutoff + freq * 1.2, 9000), 0.7);
            env = new Dsp.Adsr(attack, 0.5, 0.9, release);
        }

        @Override float next() {
            double s = 0;
            for (int i = 0; i < phase.length; i++) {
                phase[i] += step[i];
                if (phase[i] >= 1) phase[i] -= 1;
                s += Dsp.saw(phase[i], step[i]);
            }
            double trem = chorus ? 1 : 1 + 0.05 * Math.sin(TWO_PI * 0.23 * n / SR);
            n++;
            return (float) (filter.process((float) (s / phase.length)) * env.next() * vel * 0.34 * trem);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** Basses: round and woody (0), a saw with a filter that snaps shut (1), or a growling distorted saw (2). */
    private static final class Bass extends Voice {
        private final int kind;
        private final double freq, step;
        private final float vel;
        private final Dsp.Adsr env;
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private double ph, sub;
        private long n;

        Bass(double freq, float vel, long seed, int kind) {
            this.kind = kind;
            this.freq = freq;
            this.step = freq / SR;
            this.vel = vel;
            this.env = switch (kind) {
                case 0 -> new Dsp.Adsr(0.008, 0.55, 0.45, 0.14);
                case 1 -> new Dsp.Adsr(0.004, 0.4, 0.8, 0.07);
                default -> new Dsp.Adsr(0.004, 0.3, 0.7, 0.05);
            };
            this.ph = startPhase(new Dsp.Noise(seed));
        }

        @Override float next() {
            ph += step;
            if (ph >= 1) ph -= 1;
            sub += step * 0.5;
            if (sub >= 1) sub -= 1;
            double t = n++ / (double) SR;
            double s;
            switch (kind) {
                case 0 -> s = Math.sin(TWO_PI * ph) * 0.8 + Math.sin(TWO_PI * 2 * ph) * 0.22 * Math.exp(-t * 5) + Dsp.triangle(ph) * 0.18;
                case 1 -> {
                    if ((n & 7) == 0) filter.lowpass(220 + 2600 * Math.exp(-t / 0.10), 1.6);
                    s = filter.process((float) Dsp.saw(ph, step)) * 0.85 + Math.sin(TWO_PI * sub) * 0.45;
                }
                default -> {
                    if ((n & 7) == 0) filter.lowpass(360 + 1900 * Math.exp(-t / 0.09), 2.2);
                    double raw = Dsp.saw(ph, step) + 0.6 * Dsp.square(ph, step, 0.5);
                    s = filter.process((float) Math.tanh(raw * 2.4)) * 0.55 + Math.sin(TWO_PI * sub) * 0.4;
                }
            }
            return (float) (s * env.next() * vel * 0.62);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A short bowed or brassy stab: two detuned saws whose brightness snaps down as the note begins. */
    private static final class Strings extends Voice {
        private final double[] phase = new double[2], step = new double[2];
        private final float vel;
        private final double cutoff;
        private final Dsp.Adsr env;
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private long n;

        Strings(double freq, float vel, long seed, double cutoff, double attack, double decay) {
            this.vel = vel;
            this.cutoff = cutoff;
            Dsp.Noise noise = new Dsp.Noise(seed);
            for (int i = 0; i < 2; i++) {
                step[i] = freq * Math.pow(2, (i == 0 ? -6 : 6) / 1200.0) / SR;
                phase[i] = startPhase(noise);
            }
            env = new Dsp.Adsr(attack, decay, 0.3, 0.08);
        }

        @Override float next() {
            if ((n++ & 15) == 0) filter.lowpass(700 + cutoff * Math.exp(-n / (SR * 0.22)), 0.9);
            double s = 0;
            for (int i = 0; i < 2; i++) {
                phase[i] += step[i];
                if (phase[i] >= 1) phase[i] -= 1;
                s += Dsp.saw(phase[i], step[i]);
            }
            return (float) (filter.process((float) (s * 0.5)) * env.next() * vel * 0.5);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A soft electric piano (two-operator FM): a bell-like attack that mellows as it rings. */
    private static final class EPiano extends Voice {
        private final double step;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.003, 1.6, 0.06, 0.35);
        private double pc, pm;
        private long n;

        EPiano(double freq, float vel, long seed) {
            this.step = freq / SR;
            this.vel = vel;
            this.pc = startPhase(new Dsp.Noise(seed));
            this.pm = pc;                    // locked in phase with the carrier, or the FM tone would carry a DC offset
        }

        @Override float next() {
            double t = n++ / (double) SR;
            pc += step;
            if (pc >= 1) pc -= 1;
            pm += step * 1.0;
            if (pm >= 1) pm -= 1;
            double index = 0.5 + 2.6 * Math.exp(-t / 0.22) * vel;
            double s = Math.sin(TWO_PI * pc + index * Math.sin(TWO_PI * pm));
            s += 0.18 * Math.sin(TWO_PI * pc * 4 + 0.6 * Math.sin(TWO_PI * pm * 7)) * Math.exp(-t / 0.08);   // the "tine" of the tone bar
            return (float) (s * env.next() * vel * 0.36);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A brass section: bright saws whose filter opens as the note swells, with a late vibrato. */
    private static final class Brass extends Voice {
        private final double[] phase = new double[3], step = new double[3];
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.05, 0.3, 0.85, 0.12);
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private final double freq;
        private long n;

        Brass(double freq, float vel, long seed) {
            this.freq = freq;
            this.vel = vel;
            Dsp.Noise noise = new Dsp.Noise(seed);
            double[] cents = {-8, 0, 8};
            for (int i = 0; i < 3; i++) {
                step[i] = freq * Math.pow(2, cents[i] / 1200) / SR;
                phase[i] = startPhase(noise);
            }
        }

        @Override float next() {
            double t = n++ / (double) SR;
            if ((n & 7) == 0) filter.lowpass(Math.min(7000, freq * 1.5 + 500 + 2600 * (1 - Math.exp(-t / 0.13))), 1.1);
            double vib = 1 + 0.004 * Math.sin(TWO_PI * 5.6 * t) * Math.min(1, Math.max(0, (t - 0.25) / 0.4));
            double s = 0;
            for (int i = 0; i < 3; i++) {
                phase[i] += step[i] * vib;
                if (phase[i] >= 1) phase[i] -= 1;
                s += Dsp.saw(phase[i], step[i]);
            }
            return (float) (filter.process((float) (s / 3)) * env.next() * vel * 0.55);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A synth lead: a saw and a pulse together, gently wobbling. */
    private static final class Lead extends Voice {
        private final double step;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.008, 0.2, 0.75, 0.1);
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private double p1, p2;
        private long n;

        Lead(double freq, float vel, long seed) {
            this.step = freq / SR;
            this.vel = vel;
            Dsp.Noise noise = new Dsp.Noise(seed);
            p1 = startPhase(noise);
            p2 = startPhase(noise);
            filter.lowpass(Math.min(6500, 2400 + freq * 2), 1.0);
        }

        @Override float next() {
            double t = n++ / (double) SR;
            double vib = 1 + 0.003 * Math.sin(TWO_PI * 5.8 * t) * Math.min(1, t / 0.3);
            p1 += step * vib;
            if (p1 >= 1) p1 -= 1;
            p2 += step * 1.004 * vib;
            if (p2 >= 1) p2 -= 1;
            double s = Dsp.saw(p1, step) * 0.6 + (Dsp.square(p2, step, 0.35) - 0.3) * 0.4;   // (a narrow pulse has a DC offset of 0.3)
            return (float) (filter.process((float) s) * env.next() * vel * 0.42);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A plucky synth for arpeggios: a bright saw whose filter closes fast. */
    private static final class SynthPluck extends Voice {
        private final double step;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.002, 0.34, 0.0, 0.05);
        private final Dsp.Biquad filter = new Dsp.Biquad();
        private double p1, p2;
        private long n;

        SynthPluck(double freq, float vel, long seed) {
            this.step = freq / SR;
            this.vel = vel;
            Dsp.Noise noise = new Dsp.Noise(seed);
            p1 = startPhase(noise);
            p2 = startPhase(noise);
        }

        @Override float next() {
            double t = n++ / (double) SR;
            if ((n & 7) == 0) filter.lowpass(420 + 4200 * Math.exp(-t / 0.075) * (0.5 + 0.5 * vel), 2.0);
            p1 += step;
            if (p1 >= 1) p1 -= 1;
            p2 += step * 1.006;
            if (p2 >= 1) p2 -= 1;
            double s = Dsp.saw(p1, step) * 0.55 + Dsp.square(p2, step, 0.5) * 0.45;
            return (float) (filter.process((float) s) * env.next() * vel * 0.5);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** An eerie theremin: a nearly pure tone that swoops up to each note and wobbles with a slow, deep vibrato. */
    private static final class Theremin extends Voice {
        private final double freq;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.07, 0.3, 0.85, 0.28);
        private double ph;
        private long n;

        Theremin(double freq, float vel, long seed) {
            this.freq = freq;
            this.vel = vel;
            this.ph = startPhase(new Dsp.Noise(seed));
        }

        @Override float next() {
            double t = n++ / (double) SR;
            double swoop = t < 0.09 ? -0.045 * (1 - t / 0.09) * (1 - t / 0.09) : 0;
            double vib = Math.sin(TWO_PI * 5.4 * t) * 0.013 * Math.min(1, t / 0.25);
            ph += freq * (1 + swoop + vib) / SR;
            if (ph >= 1) ph -= 1;
            double p = ph * TWO_PI;
            double s = Math.sin(p) + 0.14 * Math.sin(2 * p) + 0.04 * Math.sin(3 * p);
            return (float) (s * env.next() * vel * 0.42);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    /** A pipe-organ style tone: stacked harmonics with a fast tremolo, for the mad scientist's toccata. */
    private static final class Organ extends Voice {
        private final double freq;
        private final float vel;
        private final Dsp.Adsr env = new Dsp.Adsr(0.008, 0.1, 0.9, 0.07);
        private double ph;
        private long n;

        Organ(double freq, float vel, long seed) {
            this.freq = freq;
            this.vel = vel;
            this.ph = startPhase(new Dsp.Noise(seed));
        }

        @Override float next() {
            double t = n++ / (double) SR;
            ph += freq / SR;
            if (ph >= 1) ph -= 1;
            double p = ph * TWO_PI;
            double s = Math.sin(p) + 0.7 * Math.sin(2 * p) + 0.5 * Math.sin(3 * p) + 0.32 * Math.sin(4 * p) + 0.18 * Math.sin(6 * p) + 0.12 * Math.sin(p * 0.5);
            double click = Math.exp(-t / 0.012) * 0.25;                                     // the key's little chiff
            double trem = 1 - 0.1 + 0.1 * Math.sin(TWO_PI * 6.3 * t);
            return (float) ((s * 0.3 + click * Math.sin(p * 5)) * trem * env.next() * vel * 0.5);
        }

        @Override void release() { env.release(); }

        @Override boolean done() { return env.finished(); }
    }

    // ------------------------------------------------------------------ drums

    /** One drum hit. Kit 0 is folk (frame drums, wood, shakers), 1 electronic, 2 war drums. */
    private static final class Drum extends Voice {
        private final int kit, note;
        private final float vel;
        private final Dsp.Noise noise;
        private final Dsp.Biquad band = new Dsp.Biquad(), band2 = new Dsp.Biquad();
        private final double len;
        private final double f0, f1, sweepTau, ampTau, noiseTau, noiseLevel, toneLevel;
        private final int kind;   // 0 kick, 1 snare, 2 hat, 3 tom, 4 clap, 5 shaker, 6 wood
        private double ph;
        private long n;

        Drum(int kit, int note, float vel, long seed) {
            this.kit = kit;
            this.note = note;
            this.vel = vel;
            this.noise = new Dsp.Noise(seed);
            switch (note) {
                case 36 -> kind = 0;
                case 38 -> kind = 1;
                case 42, 46 -> kind = 2;
                case 39 -> kind = 4;
                case 70 -> kind = 5;
                case 76 -> kind = 6;
                default -> kind = 3;
            }
            double a = 0, b = 0, st = 0.03, at = 0.1, nt = 0.05, nl = 0, tl = 1, l = 0.4;
            switch (kind) {
                case 0 -> {
                    a = kit == 0 ? 105 : kit == 1 ? 155 : 88;
                    b = kit == 0 ? 52 : kit == 1 ? 46 : 40;
                    st = 0.035;
                    at = kit == 0 ? 0.13 : kit == 1 ? 0.17 : 0.3;
                    nt = 0.004;
                    nl = 0.35;
                    l = kit == 2 ? 0.9 : 0.55;
                }
                case 1 -> {
                    a = kit == 0 ? 250 : kit == 1 ? 190 : 160;
                    b = a * 0.8;
                    st = 0.02;
                    at = kit == 0 ? 0.05 : 0.07;
                    nt = kit == 0 ? 0.06 : kit == 1 ? 0.13 : 0.22;
                    nl = kit == 0 ? 0.5 : 0.85;
                    tl = 0.55;
                    l = kit == 2 ? 0.7 : 0.4;
                    band.bandpass(kit == 0 ? 2600 : 1900, 0.8);
                    band2.highpass(400, 0.7);
                }
                case 2 -> {
                    nt = note == 46 ? 0.17 : 0.03;
                    nl = kit == 0 ? 0.0 : 0.55;
                    l = note == 46 ? 0.7 : 0.16;
                    band.highpass(kit == 1 ? 7800 : 7000, 0.7);
                    band2.lowpass(12500, 0.7);
                    tl = 0;
                }
                case 3 -> {
                    double base = note == 41 ? 78 : note == 45 ? 104 : 138;
                    if (kit == 2) base *= 0.82;
                    a = base * 1.55;
                    b = base;
                    st = 0.045;
                    at = kit == 2 ? 0.34 : 0.22;
                    nt = 0.008;
                    nl = 0.25;
                    l = kit == 2 ? 0.9 : 0.6;
                }
                case 4 -> {
                    nt = 0.085;
                    nl = 0.7;
                    l = 0.3;
                    band.bandpass(1500, 1.1);
                    tl = 0;
                }
                case 5 -> {
                    nt = 0.055;
                    nl = 0.55;
                    l = 0.2;
                    band.bandpass(5600, 0.9);
                    band2.highpass(2500, 0.7);
                    tl = 0;
                }
                default -> {
                    a = 1150;
                    b = 1150;
                    at = 0.028;
                    nt = 0.004;
                    nl = 0.25;
                    l = 0.16;
                }
            }
            this.f0 = a;
            this.f1 = b;
            this.sweepTau = st;
            this.ampTau = at;
            this.noiseTau = nt;
            this.noiseLevel = nl;
            this.toneLevel = tl;
            this.len = l;
        }

        @Override float next() {
            double t = n++ / (double) SR;
            double s = 0;
            if (kind == 0 || kind == 1 || kind == 3) {
                double f = f1 + (f0 - f1) * Math.exp(-t / sweepTau);
                ph += f / SR;
                if (ph >= 1) ph -= 1;
                double tone = Math.sin(TWO_PI * ph);
                if (kind == 3) tone += 0.25 * Math.sin(TWO_PI * ph * 2.3) * Math.exp(-t / 0.05);
                s += tone * Math.exp(-t / ampTau) * toneLevel * 0.9;
            }
            if (kind == 6) {
                ph += 1150.0 / SR;
                double tone = Math.sin(TWO_PI * ph) + 0.6 * Math.sin(TWO_PI * ph * 1.43);
                s += tone * Math.exp(-t / ampTau) * 0.55;
            }
            float nz = noise.next();
            switch (kind) {
                case 1 -> nz = band2.process(band.process(nz));
                case 2, 5 -> nz = band2.process(band.process(nz));
                case 4 -> {
                    nz = band.process(nz);
                    double burst = Math.exp(-(t % 0.009) / 0.003) * (t < 0.027 ? 1 : 0);
                    s += nz * (burst * 0.6 + Math.exp(-Math.max(0, t - 0.027) / noiseTau) * (t >= 0.027 ? 1 : 0)) * noiseLevel;
                    nz = 0;
                }
                case 0, 3 -> nz = nz * 0.5f;
                default -> { }
            }
            double attack = kind == 5 ? Math.min(1, t / 0.007) : 1;
            s += nz * Math.exp(-t / noiseTau) * noiseLevel * attack;
            if (kit == 1 && kind == 0) s = Math.tanh(s * 1.6) * 0.8;
            return (float) (s * vel * 0.85 * Math.min(1, n / 24.0));       // a quarter of a millisecond of ramp: hard-edged but not a step
        }

        @Override void release() { }

        @Override boolean done() { return n > len * SR; }
    }
}
