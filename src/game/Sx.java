package game;

import static game.Dsp.SR;
import static game.Dsp.TWO_PI;

/**
 * A sound-effect painter: an empty mono buffer that you add tones, filtered noise, bells, plucks and voices to, then
 * shape (filter, reverb, level) and finish. Everything is deterministic, so a sound is the same every run.
 */
final class Sx {
    static final int SINE = 0, TRI = 1, SAW = 2, SQUARE = 3;
    static final int LOW = 0, HIGH = 1, BAND = 2;

    final float[] d;
    final Dsp.Noise rnd;
    private final long seed;

    Sx(double seconds, long seed) {
        this.d = new float[Math.max(1, (int) Math.round(seconds * SR))];
        this.seed = seed;
        this.rnd = new Dsp.Noise(seed);
    }

    /** A fresh painter of another length, for building a part that gets shaped on its own and then mixed in. */
    Sx part(double seconds) { return new Sx(seconds, seed * 31 + 7); }

    private int at(double seconds) { return (int) Math.round(seconds * SR); }

    // ------------------------------------------------------------------ sources

    /** A tone that glides from {@code f0} to {@code f1} over {@code dur}, with a linear attack and an exponential decay. */
    Sx tone(int wave, double t0, double dur, double f0, double f1, double amp, double atk, double tau) {
        int i0 = at(t0), n = at(dur);
        double ph = 0;
        for (int i = 0; i < n && i0 + i < d.length; i++) {
            double t = i / (double) SR;
            double f = f0 * Math.pow(f1 / f0, t / dur);
            double dt = f / SR;
            ph += dt;
            if (ph >= 1) ph -= 1;
            double s = switch (wave) {
                case SINE -> Math.sin(TWO_PI * ph);
                case TRI -> Dsp.triangle(ph);
                case SAW -> Dsp.saw(ph, dt);
                default -> Dsp.square(ph, dt, 0.5);
            };
            d[i0 + i] += (float) (s * env(t, dur, atk, tau) * amp);
        }
        return this;
    }

    /** Attack, exponential decay, and a smooth fade over the last stretch of the layer so it never ends with a cut. */
    private static double env(double t, double dur, double atk, double tau) {
        double tail = Math.min(0.05, dur * 0.35);
        double out = t > dur - tail ? 0.5 - 0.5 * Math.cos(Math.PI * Math.max(0, dur - t) / tail) : 1;
        return Math.min(1, t / Math.max(atk, 1e-4)) * Math.exp(-t / tau) * out;
    }

    /** Filtered noise whose filter cutoff glides from {@code c0} to {@code c1}. */
    Sx noise(int kind, double t0, double dur, double c0, double c1, double q, double amp, double atk, double tau) {
        int i0 = at(t0), n = at(dur);
        Dsp.Biquad f = new Dsp.Biquad();
        for (int i = 0; i < n && i0 + i < d.length; i++) {
            double t = i / (double) SR;
            if ((i & 15) == 0) {
                double fc = c0 * Math.pow(c1 / c0, t / dur);
                switch (kind) {
                    case LOW -> f.lowpass(fc, q);
                    case HIGH -> f.highpass(fc, q);
                    default -> f.bandpass(fc, q);
                }
            }
            d[i0 + i] += f.process(rnd.next()) * (float) (env(t, dur, atk, tau) * amp);
        }
        return this;
    }

    /** A bell: inharmonic partials, the higher ones dying sooner. */
    Sx bell(double t0, double f, double[] ratios, double[] amps, double tau, double amp) {
        int i0 = at(t0);
        double total = tau * 6;
        int n = at(total);
        for (int k = 0; k < ratios.length; k++) {
            double fk = f * ratios[k], tk = tau / (1 + 0.6 * k);
            if (fk > SR * 0.42) continue;
            for (int i = 0; i < n && i0 + i < d.length; i++) {
                double t = i / (double) SR;
                double e = Math.min(1, t / 0.002) * Math.exp(-t / tk) * Math.min(1, (total - t) / 0.01);
                d[i0 + i] += (float) (Math.sin(TWO_PI * fk * t) * e * amps[k] * amp);
            }
        }
        return this;
    }

    /** A two-operator FM bell or chime: bright at first, mellowing as the modulation index dies away. */
    Sx fm(double t0, double dur, double f, double ratio, double index, double amp, double tau) {
        int i0 = at(t0), n = at(dur);
        for (int i = 0; i < n && i0 + i < d.length; i++) {
            double t = i / (double) SR;
            double idx = index * Math.exp(-t / (tau * 0.6));
            double ph = f * t;
            double s = Math.sin(TWO_PI * ph + idx * Math.sin(TWO_PI * ph * ratio));
            d[i0 + i] += (float) (s * env(t, dur, 0.002, tau) * amp);
        }
        return this;
    }

    /** A plucked string (Karplus-Strong, like the harp in the music). */
    Sx pluck(double t0, double f, double amp, double dur) {
        int i0 = at(t0), n = at(dur);
        Instruments.Pluck p = new Instruments.Pluck(f, 0.85f, seed * 13 + i0, 0.9975, 0.6);
        for (int i = 0; i < n && i0 + i < d.length; i++) {
            double t = i / (double) SR;
            d[i0 + i] += (float) (p.next() * amp * Math.min(1, (dur - t) / 0.08));
        }
        return this;
    }

    /** A throaty voice-like sound: a saw at {@code f0..f1} through two formant filters. */
    Sx vowel(double t0, double dur, double f0, double f1, double formant1, double formant2, double amp, double atk, double tau) {
        int i0 = at(t0), n = at(dur);
        Dsp.Biquad b1 = new Dsp.Biquad(), b2 = new Dsp.Biquad();
        double ph = 0;
        for (int i = 0; i < n && i0 + i < d.length; i++) {
            double t = i / (double) SR;
            double f = f0 * Math.pow(f1 / f0, t / dur);
            double dt = f / SR;
            ph += dt;
            if (ph >= 1) ph -= 1;
            if ((i & 15) == 0) {
                b1.bandpass(formant1, 4);
                b2.bandpass(formant2, 5);
            }
            float s = (float) Dsp.saw(ph, dt);
            d[i0 + i] += (b1.process(s) + 0.6f * b2.process(s)) * (float) (env(t, dur, atk, tau) * amp);
        }
        return this;
    }

    /** A burst of short crackles at random moments in [t0, t1]: embers, sparks, falling debris. */
    Sx crackle(double t0, double t1, int count, double fc, double amp) {
        for (int k = 0; k < count; k++) {
            double t = rnd.range(t0, t1);
            noise(BAND, t, 0.012 + rnd.uniform() * 0.02, fc * rnd.range(0.7, 1.4), fc * 0.7, 1.2, amp * rnd.range(0.4, 1), 0.001, 0.008);
        }
        return this;
    }

    // ------------------------------------------------------------------ shaping

    /** Adds another painter's sound at {@code t0}. */
    Sx mix(Sx other, double t0, double gain) {
        int i0 = at(t0);
        for (int i = 0; i < other.d.length && i0 + i < d.length; i++) d[i0 + i] += other.d[i] * (float) gain;
        return this;
    }

    /** Tremolo: multiplies [t0, t1] by a wobble of the given rate and depth (0..1). */
    Sx am(double t0, double t1, double hz, double depth) {
        int a = at(t0), b = Math.min(d.length, at(t1));
        for (int i = a; i < b; i++) {
            double t = (i - a) / (double) SR;
            d[i] *= (float) (1 - depth * 0.5 + depth * 0.5 * Math.sin(TWO_PI * hz * t));
        }
        return this;
    }

    Sx lowpass(double fc) { return filter(new Dsp.Biquad().lowpass(fc, 0.707)); }

    Sx highpass(double fc) { return filter(new Dsp.Biquad().highpass(fc, 0.707)); }

    private Sx filter(Dsp.Biquad f) {
        for (int i = 0; i < d.length; i++) d[i] = f.process(d[i]);
        return this;
    }

    /** Soft saturation: warms and crunches. */
    Sx drive(double amount) {
        double norm = Math.tanh(amount);
        for (int i = 0; i < d.length; i++) d[i] = (float) (Math.tanh(d[i] * amount) / norm);
        return this;
    }

    /** Reverb, mixed to mono. {@code wet} is how much tail to add. */
    Sx reverb(double room, double damping, double wet) {
        Dsp.Reverb rv = new Dsp.Reverb(room, damping);
        float[] l = new float[d.length], r = new float[d.length];
        rv.process(d, l, r, d.length, 3.0f);
        for (int i = 0; i < d.length; i++) d[i] += (float) (0.5 * (l[i] + r[i]) * wet);
        return this;
    }

    Sx gain(double g) {
        for (int i = 0; i < d.length; i++) d[i] *= (float) g;
        return this;
    }

    /** A short fade-in and fade-out so a sound can never begin or end with a click. */
    Sx fade(double in, double out) {
        int a = at(in), b = at(out);
        for (int i = 0; i < a && i < d.length; i++) d[i] *= (float) (0.5 - 0.5 * Math.cos(Math.PI * i / a));
        for (int i = 0; i < b && i < d.length; i++) d[d.length - 1 - i] *= (float) (0.5 - 0.5 * Math.cos(Math.PI * i / b));
        return this;
    }

    /**
     * Makes deep sounds work on small speakers. A laptop can't play 60 Hz and the ear is deaf to it anyway, but a low boom is
     * also heard by its overtones. So the part below {@code fc} is turned down to {@code keep} (0..1), and the overtones of
     * that low part (made by saturating it) are added back in.
     */
    Sx bassManage(double fc, double keep, double harmonics) {
        Dsp.Biquad low = new Dsp.Biquad().lowpass(fc, 0.7), hp = new Dsp.Biquad().highpass(fc * 1.5, 0.7), lp = new Dsp.Biquad().lowpass(fc * 9, 0.7);
        double peak = 1e-9;
        for (float v : d) peak = Math.max(peak, Math.abs(v));
        for (int i = 0; i < d.length; i++) {
            float lo = low.process(d[i]);
            double h = Math.tanh(3.2 * lo / peak + 0.7) - Math.tanh(0.7);
            float extra = lp.process(hp.process((float) h)) * (float) (peak * harmonics);
            d[i] = d[i] - lo * (float) (1 - keep) + extra;
        }
        return this;
    }

    /** Share of the sound's energy below {@code hz}. */
    double lowShare(double hz) {
        Dsp.Biquad f = new Dsp.Biquad().lowpass(hz, 0.707);
        double low = 0, all = 1e-12;
        for (float v : d) {
            float y = f.process(v);
            low += y * y;
            all += v * v;
        }
        return low / all;
    }

    /**
     * The A-weighted RMS of the loudest tenth of a second, in dBFS: how loud the sound seems to an ear, which hears deep
     * rumbles far more quietly than a bright sound of the same energy.
     */
    static double loudness(float[] d) {
        Dsp.AWeight a = new Dsp.AWeight();
        float[] w = new float[d.length];
        for (int i = 0; i < d.length; i++) w[i] = a.process(d[i]);
        int win = Math.min(d.length, SR / 10);
        double sum = 0, best = 0;
        for (int i = 0; i < w.length; i++) {
            sum += (double) w[i] * w[i];
            if (i >= win) sum -= (double) w[i - win] * w[i - win];
            if (i >= win - 1) best = Math.max(best, sum / win);
        }
        if (d.length < SR / 10) best = sum / d.length;
        return Dsp.dB(Math.sqrt(Math.max(best, 1e-14)));
    }

    /**
     * Removes DC offset, then sets the loudness: the A-weighted RMS of the loudest tenth of a second becomes
     * {@code loudDb} dBFS, unless that would push the highest peak above {@code peakCapDb}, in which case the peak wins.
     */
    Sx normalizeLoudness(double loudDb, double peakCapDb) {
        double mean = 0;
        for (float v : d) mean += v;
        mean /= d.length;
        double peak = 1e-9;
        for (int i = 0; i < d.length; i++) {
            d[i] -= (float) mean;
            peak = Math.max(peak, Math.abs(d[i]));
        }
        double g = Math.min(Dsp.lin(loudDb - loudness(d)), Dsp.lin(peakCapDb) / peak);
        for (int i = 0; i < d.length; i++) d[i] *= (float) g;
        return this;
    }

    /** Removes DC offset, then scales so the loudest sample sits at {@code peakDb} dBFS. */
    Sx normalize(double peakDb) {
        double mean = 0;
        for (float v : d) mean += v;
        mean /= d.length;
        double peak = 1e-9;
        for (int i = 0; i < d.length; i++) {
            d[i] -= (float) mean;
            peak = Math.max(peak, Math.abs(d[i]));
        }
        double g = Dsp.lin(peakDb) / peak;
        for (int i = 0; i < d.length; i++) d[i] *= (float) g;
        return this;
    }
}
