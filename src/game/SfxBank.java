package game;

import java.util.stream.IntStream;

/** Every sound effect, painted once at start-up and kept as samples, several variants each. */
final class SfxBank {
    private final float[][][] data = new float[Snd.values().length][][];

    private SfxBank() {}

    /** Paints everything (in parallel; a fraction of a second of work). */
    static SfxBank build() {
        SfxBank bank = new SfxBank();
        IntStream.range(0, Snd.values().length).parallel().forEach(i -> {
            Snd s = Snd.values()[i];
            float[][] v = new float[s.variants][];
            for (int k = 0; k < s.variants; k++) v[k] = SfxSynth.make(s, k);
            bank.data[i] = v;
        });
        return bank;
    }

    float[] get(Snd s, int variant) {
        float[][] v = data[s.ordinal()];
        return v[Math.floorMod(variant, v.length)];
    }

    int variants(Snd s) { return data[s.ordinal()].length; }

    /** Total memory in megabytes (for the report). */
    double megabytes() {
        long n = 0;
        for (float[][] v : data) for (float[] a : v) n += a.length;
        return n * 4 / 1e6;
    }
}
