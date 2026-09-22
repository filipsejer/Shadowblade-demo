package game;

import static game.Sx.BAND;
import static game.Sx.HIGH;
import static game.Sx.LOW;
import static game.Sx.SAW;
import static game.Sx.SINE;
import static game.Sx.SQUARE;
import static game.Sx.TRI;

/**
 * The recipes for every sound effect. Each one is a handful of tones, bursts of filtered noise and bells layered in
 * time, then normalised to the level written in {@link Snd}. Every tonal sound uses only the notes C D E G A, which
 * fit all four pieces of music.
 */
final class SfxSynth {
    private SfxSynth() {}

    private static final double[] BELL = {1, 2.76, 5.4, 8.93}, BELL_AMP = {1, 0.55, 0.3, 0.16};
    private static final double[] METAL = {1, 1.59, 2.14, 3.02, 4.1}, METAL_AMP = {1, 0.7, 0.5, 0.32, 0.2};

    /** No effect ever peaks higher than this, however loud its loudness target. */
    static final double PEAK_CAP_DB = -1.5;

    private static double n(int midi) { return Dsp.hz(midi); }

    /** Paints variant {@code v} of a sound, ready to play (peak set to the sound's level, clean start and end). */
    static float[] make(Snd s, int v) {
        long seed = s.ordinal() * 100L + v + 1;
        Sx x = paint(s, v, seed);
        if (x.lowShare(200) > 0.5) x.bassManage(140, 0.5, 0.9);     // deep booms get overtones so a laptop speaker can play them
        x.fade(0.0012, 0.012).normalizeLoudness(s.loudDb, PEAK_CAP_DB);
        return x.d;
    }

    private static Sx paint(Snd snd, int v, long seed) {
        double k = 1 + 0.07 * (v - 1);        // a little different each time
        switch (snd) {
            // ------------------------------------------------------------ interface
            case MENU_MOVE -> {
                Sx x = new Sx(0.1, seed);
                x.tone(SINE, 0, 0.09, n(76), n(76), 1, 0.002, 0.025).tone(SINE, 0, 0.09, n(88), n(88), 0.35, 0.002, 0.018);
                return x;
            }
            case MENU_OPEN -> {
                Sx x = new Sx(0.25, seed);
                x.tone(SINE, 0, 0.12, n(79), n(79), 1, 0.003, 0.05).tone(SINE, 0.06, 0.18, n(84), n(84), 1, 0.003, 0.07)
                    .tone(SINE, 0.06, 0.18, n(96), n(96), 0.25, 0.003, 0.05);
                return x;
            }
            case MENU_BACK -> {
                Sx x = new Sx(0.2, seed);
                x.tone(SINE, 0, 0.1, n(84), n(84), 1, 0.003, 0.045).tone(SINE, 0.05, 0.14, n(79), n(79), 0.9, 0.003, 0.06);
                return x;
            }
            case MENU_SELECT -> {
                Sx x = new Sx(0.9, seed);
                x.fm(0, 0.7, n(88), 3.5, 1.4, 1, 0.22).fm(0.07, 0.8, n(93), 3.5, 1.4, 0.9, 0.3);   // E6, then A6
                x.reverb(0.4, 0.5, 0.15);
                return x;
            }
            case MENU_DENY -> {
                Sx x = new Sx(0.3, seed);
                x.tone(SAW, 0, 0.1, 155, 140, 1, 0.004, 0.06).tone(SAW, 0.11, 0.15, 155, 116, 1, 0.004, 0.08).lowpass(1800);
                return x;
            }
            case PAUSE_IN -> {
                Sx x = new Sx(0.4, seed);
                x.tone(SINE, 0, 0.35, 880, 300, 1, 0.01, 0.13).noise(BAND, 0, 0.3, 2500, 600, 0.8, 0.12, 0.01, 0.08);
                return x;
            }
            case PAUSE_OUT -> {
                Sx x = new Sx(0.4, seed);
                x.tone(SINE, 0, 0.3, 300, 880, 1, 0.03, 0.12).noise(BAND, 0, 0.25, 600, 2500, 0.8, 0.12, 0.03, 0.08);
                return x;
            }
            case LOCK_ON -> {
                Sx x = new Sx(0.22, seed);
                x.noise(BAND, 0, 0.02, 3000, 3000, 1, 1, 0.001, 0.005).tone(SINE, 0.005, 0.15, n(91), n(91), 0.55, 0.002, 0.05)
                    .tone(SINE, 0.05, 0.15, n(96), n(96), 0.4, 0.002, 0.05);
                return x;
            }
            case LOCK_OFF -> {
                Sx x = new Sx(0.2, seed);
                x.noise(BAND, 0, 0.02, 2200, 2200, 1, 0.8, 0.001, 0.005).tone(SINE, 0.005, 0.14, n(84), n(79), 0.5, 0.002, 0.045);
                return x;
            }
            case TITLE_START -> {
                Sx x = new Sx(2.2, seed);
                int[] up = {72, 76, 79, 84, 88};
                for (int i = 0; i < up.length; i++) x.fm(0.07 * i, 1.2, n(up[i]), 3.5, 1.3, 0.7, 0.35);
                x.noise(BAND, 0, 0.9, 300, 4200, 0.8, 0.35, 0.5, 0.5);
                x.reverb(0.62, 0.45, 0.32);
                return x;
            }

            // ------------------------------------------------------------ the hero
            case SWING_LIGHT -> {
                double[][] bands = {{700, 2600}, {2600, 800}, {900, 2300}};
                double[] b = bands[v % 3];
                Sx x = new Sx(0.25, seed);
                x.noise(BAND, 0, 0.22, b[0], b[1], 1.1, 1, 0.045, 0.07).tone(SINE, 0, 0.12, 180 * k, 90, 0.14, 0.01, 0.05);
                return x;
            }
            case SWING_HEAVY -> {
                Sx x = new Sx(0.45, seed);
                x.noise(BAND, 0, 0.36, 380 * k, 1900, 0.9, 1, 0.09, 0.13).tone(SINE, 0, 0.25, 130, 60, 0.3, 0.02, 0.1)
                    .tone(SINE, 0.05, 0.3, 2400, 2350, 0.07, 0.02, 0.1).tone(SINE, 0.05, 0.3, 3100, 3050, 0.05, 0.02, 0.09);
                return x;
            }
            case DASH -> {
                Sx x = new Sx(0.3, seed);
                x.noise(BAND, 0, 0.26, 1600 * k, 480, 0.8, 1, 0.012, 0.09).lowpass(5500);
                return x;
            }
            case HIT_LIGHT -> {
                Sx x = new Sx(0.2, seed);
                x.tone(SINE, 0, 0.18, 260 * k, 95, 0.7, 0.001, 0.06).noise(BAND, 0, 0.04, 2600 * k, 3800, 1.0, 1.2, 0.0005, 0.011)
                    .noise(LOW, 0, 0.14, 1400, 550, 0.8, 1.0, 0.001, 0.05).noise(BAND, 0, 0.12, 1000, 600, 1.0, 0.9, 0.001, 0.05)
                    .tone(SINE, 0, 0.12, 520 * k, 220, 0.4, 0.001, 0.05);
                return x;
            }
            case HIT_HEAVY -> {
                Sx x = new Sx(0.6, seed);
                x.tone(SINE, 0, 0.4, 170 * k, 48, 0.8, 0.001, 0.12).noise(BAND, 0, 0.08, 1800, 2500, 0.9, 1.4, 0.0005, 0.022)
                    .noise(LOW, 0, 0.3, 1600, 300, 0.8, 1.4, 0.001, 0.11).noise(BAND, 0, 0.25, 900, 450, 1.0, 1.1, 0.001, 0.09)
                    .tone(SINE, 0, 0.25, 380 * k, 140, 0.5, 0.001, 0.09)
                    .tone(SINE, 0, 0.3, 1200, 1180, 0.1, 0.001, 0.12).tone(SINE, 0, 0.3, 1720, 1700, 0.08, 0.001, 0.1);
                x.reverb(0.3, 0.6, 0.12);
                return x;
            }
            case HIT_ARMOR -> {
                Sx x = new Sx(0.8, seed);
                x.bell(0, (v == 0 ? 380 : 445), BELL, BELL_AMP, 0.28, 0.55).noise(BAND, 0, 0.03, 3000, 3000, 1, 0.7, 0.0005, 0.008)
                    .tone(SINE, 0, 0.2, 130, 62, 0.9, 0.001, 0.06);
                return x;
            }
            case ROLL -> {
                Sx x = new Sx(0.38, seed);
                x.noise(BAND, 0, 0.32, 950 * k, 480, 0.7, 1, 0.07, 0.1).tone(SINE, 0, 0.1, 95, 60, 0.22, 0.005, 0.04);
                return x;
            }
            case STEP_GRASS -> {
                Sx x = new Sx(0.1, seed);
                x.noise(BAND, 0, 0.07, 2400 * k, 1800, 0.6, 1, 0.005, 0.02).noise(LOW, 0, 0.04, 800, 500, 0.7, 0.55, 0.001, 0.015);
                return x;
            }
            case STEP_STONE -> {
                Sx x = new Sx(0.08, seed);
                x.noise(BAND, 0, 0.03, 3400 * k, 2800, 0.9, 1.5, 0.0005, 0.008).tone(SINE, 0, 0.05, 240 * k, 150, 0.3, 0.001, 0.02);
                return x;
            }
            case PLAYER_HURT -> {
                double f0 = 205 * k;
                Sx x = new Sx(0.45, seed);
                x.vowel(0, 0.24, f0, f0 * 0.62, 690 * k, 1180, 1, 0.012, 0.1).noise(BAND, 0, 0.12, 900, 500, 0.9, 0.5, 0.001, 0.04)
                    .tone(SINE, 0, 0.14, 130, 70, 0.3, 0.002, 0.06);
                return x;
            }
            case LEVEL_UP -> {
                Sx x = new Sx(2.4, seed);
                int[] up = {72, 76, 79, 84, 88};
                for (int i = 0; i < up.length; i++) x.fm(0.09 * i, 1.4, n(up[i]), 3.5, 1.4, 0.6, 0.4);
                x.fm(0.5, 1.8, n(84), 2, 1.0, 0.4, 0.7).fm(0.5, 1.8, n(88), 2, 1.0, 0.35, 0.7).fm(0.5, 1.8, n(91), 2, 1.0, 0.3, 0.7);
                x.noise(HIGH, 0.4, 0.8, 6500, 6500, 0.7, 0.05, 0.1, 0.3);
                x.reverb(0.62, 0.45, 0.35);
                return x;
            }
            case SPELL_FAIL -> {
                Sx x = new Sx(0.25, seed);
                x.tone(TRI, 0, 0.14, 300, 180, 1, 0.004, 0.06).lowpass(1500);
                return x;
            }

            // ------------------------------------------------------------ spells
            case CAST_FIRE -> {
                Sx x = new Sx(0.7, seed);
                x.noise(LOW, 0, 0.5, 400 * k, 3000, 0.9, 1, 0.16, 0.24).tone(SINE, 0, 0.35, 110, 220, 0.25, 0.1, 0.15).crackle(0.05, 0.4, 14, 4200, 0.4);
                return x;
            }
            case FIRE_EXPLODE -> {
                Sx x = new Sx(1.1, seed);
                x.tone(SINE, 0, 0.7, 110 * k, 30, 0.6, 0.002, 0.2).noise(LOW, 0, 0.55, 3200, 260, 0.8, 1.8, 0.002, 0.17)
                    .noise(BAND, 0, 0.06, 1400, 1700, 0.8, 1.4, 0.0005, 0.02).noise(BAND, 0, 0.3, 900, 500, 0.8, 0.9, 0.002, 0.1).crackle(0.08, 0.6, 16, 3500, 0.4);
                x.reverb(0.55, 0.55, 0.22);
                return x;
            }
            case CAST_ICE -> {
                Sx x = new Sx(1.1, seed);
                int[] ping = {96, 100, 103, 108};
                for (int i = 0; i < ping.length; i++) x.tone(SINE, x.rnd.range(0, 0.25), 0.5, n(ping[i]) * (v == 0 ? 1 : 1.01), n(ping[i]), 0.5, 0.002, 0.2);
                x.noise(HIGH, 0, 0.55, 3000, 1500, 0.7, 0.5, 0.12, 0.2).tone(SINE, 0, 0.3, 72, 60, 0.55, 0.02, 0.18);
                x.reverb(0.5, 0.4, 0.3);
                return x;
            }
            case ICE_TICK -> {
                double f = new double[]{2637, 3136, 3520}[v % 3];
                Sx x = new Sx(0.14, seed);
                x.tone(SINE, 0, 0.12, f, f, 1, 0.001, 0.03).tone(SINE, 0, 0.1, f * 1.5, f * 1.5, 0.5, 0.001, 0.02).noise(HIGH, 0, 0.02, 6000, 6000, 0.7, 0.3, 0.0005, 0.006);
                return x;
            }
            case CAST_BOLT -> {
                Sx x = new Sx(1.3, seed);
                Sx buzz = x.part(0.4).tone(SAW, 0, 0.35, 120, 118, 1, 0.01, 0.12).am(0, 0.35, 40, 0.8);
                x.noise(HIGH, 0, 0.06, 800, 1200, 0.7, 1, 0.0005, 0.014).tone(SAW, 0, 0.2, 1800 * k, 160, 0.65, 0.001, 0.06).mix(buzz, 0.02, 0.32)
                    .noise(LOW, 0, 1.0, 420, 110, 0.7, 0.85, 0.04, 0.34);
                x.reverb(0.62, 0.5, 0.28);
                return x;
            }
            case ZAP_HIT -> {
                double f = 1400 * k;
                Sx x = new Sx(0.22, seed);
                x.tone(SAW, 0, 0.16, f, 300, 0.6, 0.001, 0.04).noise(HIGH, 0, 0.03, 2000, 2000, 0.7, 0.8, 0.0005, 0.012);
                return x;
            }
            case CAST_HEAL -> {
                Sx x = new Sx(1.5, seed);
                int[] up = {72, 76, 79, 84, 88};
                for (int i = 0; i < up.length; i++) x.pluck(0.08 * i, n(up[i]), 0.8, 1.0);
                x.fm(0.3, 1.0, n(91), 3.5, 1.0, 0.25, 0.4);
                x.reverb(0.55, 0.45, 0.3);
                return x;
            }

            // ------------------------------------------------------------ enemies
            case WINDUP_SMALL_FOREST -> {
                Sx x = new Sx(0.35, seed);
                Sx creak = x.part(0.3).tone(SAW, 0, 0.26, 140 * k, 270 * k, 1, 0.05, 0.12).lowpass(900).am(0, 0.3, 30, 0.5);
                x.mix(creak, 0, 1).noise(BAND, 0, 0.25, 3000, 3600, 0.8, 0.25, 0.04, 0.1);
                return x;
            }
            case WINDUP_SMALL_CITY -> {
                Sx x = new Sx(0.35, seed);
                x.tone(SINE, 0, 0.27, 300 * k, 900 * k, 1, 0.02, 0.16).tone(SQUARE, 0, 0.27, 150 * k, 450 * k, 0.22, 0.02, 0.14).lowpass(4500);
                return x;
            }
            case WINDUP_BIG_FOREST -> {
                Sx x = new Sx(0.8, seed);
                x.vowel(0, 0.65, 62, 92, 340, 720, 1, 0.2, 0.32).noise(LOW, 0, 0.6, 600, 400, 0.7, 0.3, 0.15, 0.3);
                return x;
            }
            case WINDUP_BIG_CITY -> {
                Sx x = new Sx(0.8, seed);
                x.tone(SAW, 0, 0.65, 80, 240, 0.8, 0.2, 0.32).lowpass(1500);
                x.tone(SINE, 0, 0.65, 400, 1200, 0.4, 0.2, 0.32).noise(BAND, 0, 0.6, 1200, 1500, 1.2, 0.2, 0.15, 0.3);
                return x;
            }
            case ENEMY_STRIKE -> {
                Sx x = new Sx(0.2, seed);
                x.noise(BAND, 0, 0.16, 1250 * k, 700, 0.8, 1, 0.02, 0.05);
                return x;
            }
            case SHOOT_FOREST -> {
                Sx x = new Sx(0.25, seed);
                x.noise(LOW, 0, 0.13, 3000, 600, 0.8, 1, 0.005, 0.04).tone(SINE, 0, 0.1, 500 * k, 220, 0.6, 0.002, 0.04);
                return x;
            }
            case SHOOT_CITY -> {
                Sx x = new Sx(0.28, seed);
                x.tone(SINE, 0, 0.16, 1900 * k, 420, 1, 0.001, 0.07).tone(SAW, 0, 0.14, 950 * k, 210, 0.25, 0.001, 0.05).noise(HIGH, 0, 0.015, 4000, 4000, 0.7, 0.25, 0.0005, 0.006);
                x.lowpass(9000);
                return x;
            }
            case DIE_FOREST -> {
                Sx x = new Sx(0.55, seed);
                x.noise(LOW, 0, 0.22, 900, 300, 0.8, 1, 0.003, 0.07).noise(BAND, 0.02, 0.3, 4500 * k, 3500, 0.7, 0.4, 0.02, 0.1).tone(SINE, 0, 0.13, 520 * k, 160, 0.7, 0.002, 0.04);
                return x;
            }
            case DIE_CITY -> {
                Sx x = new Sx(0.55, seed);
                x.noise(BAND, 0, 0.14, 1800 * k, 1500, 1.5, 1, 0.001, 0.03).bell(0, 620 * k, new double[]{1, 2.4, 3.9}, new double[]{1, 0.5, 0.3}, 0.1, 0.4)
                    .tone(SINE, 0, 0.14, 1200, 200, 0.5, 0.001, 0.05);
                x.mix(x.part(0.35).noise(HIGH, 0, 0.3, 5000, 5000, 0.7, 1, 0.005, 0.1).am(0, 0.3, 90, 0.9), 0.04, 0.4);
                return x;
            }
            case DIE_BIG_FOREST -> {
                Sx x = new Sx(0.9, seed);
                x.tone(SINE, 0, 0.4, 120, 44, 0.6, 0.002, 0.13).noise(LOW, 0, 0.35, 1800, 250, 0.8, 1.5, 0.003, 0.11).noise(BAND, 0.03, 0.5, 4200, 3000, 0.7, 0.7, 0.03, 0.18)
                    .crackle(0.05, 0.5, 8, 2500, 0.4);
                x.reverb(0.4, 0.55, 0.15);
                return x;
            }
            case DIE_BIG_CITY -> {
                Sx x = new Sx(0.9, seed);
                x.tone(SINE, 0, 0.4, 120, 42, 0.6, 0.002, 0.13).noise(BAND, 0, 0.2, 1500, 1200, 1.2, 1.5, 0.001, 0.05).bell(0, 180, METAL, METAL_AMP, 0.3, 0.7)
                    .noise(HIGH, 0.05, 0.5, 5000, 4000, 0.7, 0.5, 0.005, 0.2).crackle(0.05, 0.6, 12, 4500, 0.4);
                x.reverb(0.4, 0.55, 0.15);
                return x;
            }
            case SHADE_OUT -> {
                Sx x = new Sx(0.6, seed);
                Sx t = x.part(0.5).tone(SINE, 0, 0.45, 800, 260, 1, 0.02, 0.2).am(0, 0.5, 14, 0.6);
                x.mix(t, 0, 1).noise(HIGH, 0, 0.35, 3000, 2000, 0.7, 0.15, 0.05, 0.1);
                x.reverb(0.5, 0.5, 0.15);
                return x;
            }
            case SHADE_IN -> {
                Sx x = new Sx(0.6, seed);
                Sx t = x.part(0.5).tone(SINE, 0, 0.45, 260, 800, 1, 0.22, 0.25).am(0, 0.5, 14, 0.6);
                x.mix(t, 0, 1).noise(HIGH, 0, 0.35, 2000, 3000, 0.7, 0.15, 0.2, 0.15);
                x.reverb(0.5, 0.5, 0.15);
                return x;
            }

            // ------------------------------------------------------------ bosses
            case BOSS_INTRO -> {
                Sx x = new Sx(2.6, seed);
                x.tone(SINE, 0, 1.2, 60, 26, 0.5, 0.003, 0.35).tone(SAW, 0, 2.2, 55, 110, 0.7, 0.9, 1.4).lowpass(1300)
                    .noise(HIGH, 0, 1.8, 1500, 6000, 0.7, 0.6, 1.4, 1.5).noise(LOW, 0, 0.8, 1500, 150, 0.7, 1.4, 0.005, 0.3);
                x.reverb(0.75, 0.55, 0.3);
                return x;
            }
            case BOSS_BURST_FOREST -> {
                Sx x = new Sx(1.1, seed);
                x.tone(SINE, 0, 0.5, 100, 44, 0.5, 0.005, 0.18).noise(LOW, 0, 0.5, 2200, 350, 0.8, 1.4, 0.005, 0.2);
                for (int i = 0; i < 8; i++) x.noise(LOW, 0.02 + i * 0.025, 0.1, 3000, 700, 0.8, 0.8, 0.004, 0.035);
                x.reverb(0.5, 0.55, 0.2);
                return x;
            }
            case BOSS_BURST_CITY -> {
                Sx x = new Sx(1.1, seed);
                x.tone(SINE, 0, 0.3, 300, 1800, 0.5, 0.15, 0.2);
                for (int i = 0; i < 6; i++) x.tone(SINE, 0.3 + i * 0.03, 0.14, 1900 - i * 120, 420, 0.5, 0.001, 0.06);
                x.tone(SINE, 0.3, 0.5, 90, 40, 0.9, 0.003, 0.15).noise(HIGH, 0.3, 0.3, 3000, 1500, 0.7, 0.3, 0.003, 0.1);
                x.reverb(0.5, 0.5, 0.2);
                return x;
            }
            case BOSS_SLAM_FOREST -> {
                Sx x = new Sx(1.9, seed);
                x.tone(SINE, 0, 1.0, 70, 26, 0.6, 0.003, 0.3).noise(LOW, 0, 0.9, 2600, 160, 0.8, 1.9, 0.003, 0.3).noise(BAND, 0, 0.06, 900, 1100, 0.9, 1.7, 0.0005, 0.02)
                    .tone(SINE, 0, 0.5, 180, 175, 0.35, 0.002, 0.15).tone(SINE, 0, 0.5, 260, 255, 0.3, 0.002, 0.15).tone(SINE, 0, 0.5, 410, 400, 0.25, 0.002, 0.15)
                    .noise(BAND, 0, 0.7, 700, 300, 0.8, 1.0, 0.01, 0.25).crackle(0.1, 0.9, 20, 2500, 0.5);
                x.reverb(0.7, 0.55, 0.28);
                return x;
            }
            case BOSS_SLAM_CITY -> {
                Sx x = new Sx(1.9, seed);
                x.tone(SINE, 0, 1.0, 66, 28, 0.6, 0.003, 0.3).noise(LOW, 0, 0.9, 2600, 160, 0.8, 1.7, 0.003, 0.3).noise(BAND, 0, 0.05, 1500, 1800, 1.0, 1.7, 0.0005, 0.015)
                    .bell(0, 230, METAL, METAL_AMP, 0.6, 1.5).noise(BAND, 0, 0.7, 700, 300, 0.8, 1.2, 0.01, 0.25).crackle(0.1, 0.9, 20, 4500, 0.5);
                x.reverb(0.7, 0.55, 0.28);
                return x;
            }
            case BOSS_PHASE2_FOREST -> {
                Sx x = new Sx(2.2, seed);
                Sx roar = x.part(1.6).vowel(0, 1.5, 80, 120, 420, 900, 1, 0.1, 0.9).am(0, 1.5, 22, 0.4);
                x.mix(roar, 0, 1).noise(BAND, 0, 1.4, 700, 450, 0.8, 0.5, 0.1, 0.6).tone(SINE, 0, 0.6, 70, 34, 0.4, 0.003, 0.3);
                x.reverb(0.65, 0.55, 0.25);
                return x;
            }
            case BOSS_PHASE2_CITY -> {
                Sx x = new Sx(2.2, seed);
                x.tone(SINE, 0, 0.55, 400, 900, 0.5, 0.05, 0.6).tone(SINE, 0.55, 0.55, 400, 900, 0.5, 0.05, 0.6).tone(SAW, 0, 1.2, 60, 600, 0.5, 0.3, 0.8).lowpass(3000)
                    .tone(SINE, 1.0, 0.7, 70, 30, 1, 0.003, 0.25).noise(LOW, 1.0, 0.7, 2500, 150, 0.8, 0.9, 0.003, 0.25);
                x.reverb(0.65, 0.55, 0.25);
                return x;
            }
            case BOSS_DIE -> {
                Sx x = new Sx(3.6, seed);
                double[][] booms = {{0, 1}, {0.35, 0.8}, {0.7, 0.9}, {1.05, 0.7}, {1.4, 1.4}};
                for (double[] b : booms) {
                    x.tone(SINE, b[0], 0.8, 105, 30, 0.5 * b[1], 0.002, 0.2).noise(LOW, b[0], 0.6, 3000, 220, 0.8, 1.6 * b[1], 0.002, 0.18);
                    x.noise(BAND, b[0], 0.05, 1200, 1600, 0.8, 1.2 * b[1], 0.0005, 0.015);
                }
                x.tone(SAW, 0, 2.2, 900, 45, 0.25, 0.05, 1.2).lowpass(2500).crackle(0.2, 2.8, 40, 3000, 0.3).noise(LOW, 1.4, 2.0, 200, 70, 0.7, 0.7, 0.02, 0.7);
                x.reverb(0.8, 0.55, 0.3);
                return x;
            }

            // ------------------------------------------------------------ the world
            case LOCK_FOREST -> {
                Sx x = new Sx(1.1, seed);
                x.noise(BAND, 0, 0.55, 800, 2500, 0.8, 0.9, 0.22, 0.3).vowel(0, 0.5, 120, 180, 450, 900, 0.5, 0.15, 0.3).lowpass(3500)
                    .tone(SINE, 0.35, 0.4, 80, 44, 0.5, 0.003, 0.12).noise(LOW, 0.35, 0.25, 1800, 250, 0.8, 1.6, 0.003, 0.08);
                x.reverb(0.45, 0.55, 0.15);
                return x;
            }
            case LOCK_CITY -> {
                Sx x = new Sx(1.1, seed);
                Sx rattle = x.part(0.45).noise(BAND, 0, 0.4, 2000, 2200, 1.2, 1, 0.02, 0.2).am(0, 0.4, 35, 0.8);
                x.mix(rattle, 0, 1.2).tone(SINE, 0.35, 0.4, 100, 46, 0.4, 0.003, 0.12).bell(0.35, 320, new double[]{1, 2.3, 3.1}, new double[]{1, 0.6, 0.4}, 0.25, 1.4)
                    .noise(BAND, 0.35, 0.05, 1600, 1800, 1, 1.5, 0.0005, 0.015);
                x.reverb(0.45, 0.55, 0.15);
                return x;
            }
            case ROOM_CLEAR -> {
                Sx x = new Sx(1.8, seed);
                int[] notes = {84, 88, 91};
                for (int i = 0; i < notes.length; i++) x.fm(0.09 * i, 1.3, n(notes[i]), 3.5, 1.2, 0.7, 0.4);
                x.noise(HIGH, 0, 0.6, 7000, 7000, 0.7, 0.1, 0.05, 0.25);
                x.reverb(0.6, 0.45, 0.3);
                return x;
            }
            case BOSS_UNSEAL -> {
                Sx x = new Sx(2.6, seed);
                x.tone(SAW, 0, 1.8, 110, 112, 0.5, 0.6, 1.0).tone(SAW, 0, 1.8, 165, 167, 0.4, 0.6, 1.0).tone(SAW, 0, 1.8, 220, 223, 0.35, 0.6, 1.0).lowpass(1800)
                    .tone(SINE, 0, 1.6, 55, 50, 0.7, 0.05, 1.0).noise(BAND, 0, 0.06, 1500, 1800, 0.8, 0.8, 0.0005, 0.02)
                    .fm(1.0, 1.5, n(81), 3.5, 1.2, 0.5, 0.5).fm(1.1, 1.5, n(86), 3.5, 1.2, 0.4, 0.5).fm(1.2, 1.5, n(93), 3.5, 1.2, 0.35, 0.5);
                x.reverb(0.7, 0.5, 0.3);
                return x;
            }
            case GUIDE_APPEAR -> {
                Sx x = new Sx(2.4, seed);
                int[] up = {72, 76, 79, 81, 84, 88};
                for (int i = 0; i < up.length; i++) x.fm(0.1 * i, 1.3, n(up[i]), 3.5, 1.2, 0.6, 0.4);
                x.tone(SINE, 0.1, 1.9, n(72), n(72), 0.25, 0.4, 0.8).tone(SINE, 0.1, 1.9, n(79), n(79), 0.22, 0.4, 0.8).tone(SINE, 0.1, 1.9, n(88), n(88), 0.18, 0.4, 0.8);
                x.reverb(0.68, 0.45, 0.38);
                return x;
            }
            case TRAVEL -> {
                Sx x = new Sx(2.6, seed);
                x.noise(BAND, 0, 1.6, 200, 5000, 0.8, 0.8, 1.0, 1.0).tone(SINE, 0, 1.4, 200, 1200, 0.3, 1.0, 1.0)
                    .fm(1.2, 1.4, n(84), 3.5, 1.2, 0.6, 0.45).fm(1.3, 1.4, n(91), 3.5, 1.2, 0.5, 0.45);
                x.reverb(0.65, 0.5, 0.3);
                return x;
            }
            case GAME_OVER -> {
                Sx x = new Sx(4.6, seed);
                x.tone(SAW, 0, 3.6, 110, 108, 0.35, 1.2, 1.8).tone(SAW, 0, 3.6, 165, 163, 0.28, 1.2, 1.8).lowpass(600);
                x.fm(0, 2.0, n(76), 2, 1.1, 0.7, 0.9).fm(0.7, 2.0, n(72), 2, 1.1, 0.7, 0.9).fm(1.4, 2.6, n(69), 2, 1.1, 0.8, 1.1).fm(1.4, 2.6, n(57), 2, 0.8, 0.5, 1.2);
                x.reverb(0.8, 0.5, 0.4);
                return x;
            }
            case GAME_CLEARED -> {
                Sx x = new Sx(6.0, seed);
                int[] run = {72, 76, 79, 84};
                double[] at = {0, 0.25, 0.5, 0.8};
                for (int i = 0; i < run.length; i++) x.fm(at[i], 1.6, n(run[i]), 3.5, 1.3, 0.7, 0.5);
                int[] chord = {48, 55, 60, 64, 67};
                for (int c : chord) x.tone(SAW, 1.2, 3.0, n(c), n(c) * 1.002, 0.16, 0.05, 1.5);
                x.lowpass(2800);
                x.fm(1.2, 3.5, n(84), 2, 1.0, 0.5, 1.2).fm(1.3, 3.5, n(88), 2, 1.0, 0.4, 1.2).fm(1.4, 3.5, n(91), 2, 1.0, 0.35, 1.2);
                x.reverb(0.78, 0.45, 0.4);
                return x;
            }
            case SEALED -> {
                Sx x = new Sx(0.9, seed);
                x.tone(SAW, 0, 0.7, 82, 80, 1, 0.1, 0.3).lowpass(300).noise(LOW, 0, 0.6, 250, 200, 0.7, 0.4, 0.1, 0.3);
                x.reverb(0.4, 0.6, 0.1);
                return x;
            }

            // ------------------------------------------------------------ ambience
            case BIRD -> {
                Sx x = new Sx(0.8, seed);
                double base = 2600 + 700 * v;
                int chirps = 2 + v % 3;
                for (int i = 0; i < chirps; i++) {
                    double t = i * (0.11 + 0.02 * v);
                    x.tone(SINE, t, 0.08, base * (1 + 0.05 * i), base * (1.5 + 0.1 * i), 1, 0.008, 0.04);
                    x.tone(SINE, t, 0.08, base * 2 * (1 + 0.05 * i), base * 3 * (1 + 0.1 * i), 0.15, 0.008, 0.03);
                }
                x.reverb(0.5, 0.4, 0.2);
                return x;
            }
            // ------------------------------------------------------------ laboratory
            case LOCK_LAB -> {
                Sx x = new Sx(1.1, seed);
                Sx buzz = x.part(0.5).tone(SQUARE, 0, 0.45, 118, 122, 1, 0.2, 0.3).am(0, 0.45, 60, 0.9).lowpass(2400);
                x.tone(SAW, 0, 0.5, 200, 1300, 0.5, 0.3, 0.3).lowpass(3500).mix(buzz, 0, 0.7)
                    .noise(HIGH, 0.3, 0.2, 3000, 3000, 0.7, 0.4, 0.01, 0.08)
                    .tone(SINE, 0.36, 0.4, 100, 46, 0.4, 0.003, 0.12).bell(0.36, 300, METAL, METAL_AMP, 0.3, 1.3)
                    .noise(BAND, 0.36, 0.05, 1600, 1800, 1, 1.4, 0.0005, 0.015);
                x.reverb(0.45, 0.55, 0.15);
                return x;
            }
            case WINDUP_SMALL_LAB -> {
                Sx x = new Sx(0.35, seed);
                Sx warble = x.part(0.3).tone(SINE, 0, 0.27, 220 * k, 560 * k, 1, 0.03, 0.15).am(0, 0.3, 24, 0.7);
                x.mix(warble, 0, 1).noise(BAND, 0, 0.25, 900, 1600, 0.9, 0.35, 0.03, 0.1);
                return x;
            }
            case WINDUP_BIG_LAB -> {
                Sx x = new Sx(0.8, seed);
                x.vowel(0, 0.65, 70, 105, 330, 760, 1, 0.2, 0.32).tone(SAW, 0, 0.6, 90, 210, 0.3, 0.2, 0.3).lowpass(1400)
                    .noise(BAND, 0, 0.6, 1400, 2200, 1.2, 0.2, 0.15, 0.3);
                return x;
            }
            case SHOOT_LAB -> {
                Sx x = new Sx(0.28, seed);
                x.tone(SINE, 0, 0.14, 280 * k, 900 * k, 1, 0.002, 0.06).noise(LOW, 0, 0.12, 2400, 500, 0.8, 0.8, 0.004, 0.04)
                    .tone(SINE, 0.07, 0.1, 500, 200, 0.4, 0.002, 0.04);
                return x;
            }
            case DIE_LAB -> {
                Sx x = new Sx(0.6, seed);
                x.noise(LOW, 0, 0.22, 1600, 300, 0.8, 1, 0.003, 0.07).tone(SINE, 0, 0.14, 430 * k, 120, 0.7, 0.002, 0.045)
                    .noise(BAND, 0.02, 0.3, 3200, 2400, 0.8, 0.3, 0.02, 0.1).bell(0.04, 1000 * k, new double[]{1, 2.4, 3.9}, new double[]{1, 0.5, 0.3}, 0.08, 0.3);
                return x;
            }
            case DIE_BIG_LAB -> {
                Sx x = new Sx(0.95, seed);
                x.tone(SINE, 0, 0.4, 120, 44, 0.6, 0.002, 0.13).noise(LOW, 0, 0.35, 2000, 250, 0.8, 1.5, 0.003, 0.11)
                    .noise(BAND, 0.02, 0.3, 1000, 600, 1.0, 0.9, 0.003, 0.12).crackle(0.05, 0.7, 26, 5200, 0.5)
                    .bell(0.03, 1300, new double[]{1, 2.4, 3.9, 5.6}, new double[]{1, 0.6, 0.4, 0.3}, 0.12, 0.5);
                x.reverb(0.45, 0.55, 0.15);
                return x;
            }
            case BOSS_BURST_LAB -> {
                Sx x = new Sx(1.1, seed);
                x.tone(SINE, 0, 0.5, 110, 46, 0.5, 0.005, 0.18).noise(LOW, 0, 0.5, 2200, 350, 0.8, 1.3, 0.005, 0.2);
                for (int i = 0; i < 7; i++) x.tone(SINE, 0.05 + i * 0.045, 0.1, 300 + i * 90, 900 + i * 120, 0.45, 0.002, 0.04);
                x.noise(HIGH, 0, 0.2, 3500, 1800, 0.7, 0.3, 0.003, 0.08);
                x.reverb(0.5, 0.55, 0.2);
                return x;
            }
            case BOSS_SLAM_LAB -> {
                Sx x = new Sx(1.9, seed);
                x.tone(SINE, 0, 1.0, 68, 26, 0.6, 0.003, 0.3).noise(LOW, 0, 0.9, 2600, 160, 0.8, 1.7, 0.003, 0.3)
                    .noise(BAND, 0, 0.05, 1500, 1800, 1.0, 1.6, 0.0005, 0.015).bell(0, 260, METAL, METAL_AMP, 0.5, 1.5)
                    .noise(BAND, 0, 0.7, 700, 300, 0.8, 1.0, 0.01, 0.25).crackle(0.05, 1.0, 30, 5000, 0.6);
                x.reverb(0.7, 0.55, 0.28);
                return x;
            }
            case BOSS_PHASE2_LAB -> {                                            // the mad laugh: "ha ha ha HAAA", over a power-up
                Sx x = new Sx(2.6, seed);
                x.tone(SINE, 0, 0.5, 70, 32, 0.5, 0.003, 0.25).noise(LOW, 0, 0.4, 2400, 200, 0.8, 1.0, 0.003, 0.15);
                x.tone(SAW, 0, 1.4, 60, 700, 0.35, 0.3, 0.7).lowpass(2500).crackle(0.1, 1.4, 30, 4500, 0.4);
                for (int i = 0; i < 5; i++) {
                    double t = 0.35 + 0.2 * i, f = 225 - 9 * i;
                    x.vowel(t, 0.14, f * 1.06, f * 0.9, i % 2 == 0 ? 780 : 700, i % 2 == 0 ? 1200 : 1100, 1.1, 0.012, 0.07);
                }
                Sx last = x.part(0.9).vowel(0, 0.85, 215, 150, 800, 1250, 1.1, 0.03, 0.5).am(0, 0.85, 9, 0.5);
                x.mix(last, 1.4, 1);
                x.reverb(0.65, 0.5, 0.25);
                return x;
            }
            case BOSS_SUMMON_LAB -> {
                Sx x = new Sx(0.85, seed);
                Sx warble = x.part(0.6).tone(SINE, 0, 0.55, 300, 1200, 1, 0.1, 0.3).am(0, 0.55, 16, 0.6);
                x.noise(BAND, 0, 0.55, 400, 2500, 0.8, 0.6, 0.15, 0.25).mix(warble, 0, 0.7).crackle(0.1, 0.55, 14, 4200, 0.4)
                    .tone(SINE, 0.55, 0.15, 500, 1400, 0.8, 0.003, 0.05).noise(HIGH, 0.55, 0.05, 3000, 3000, 0.7, 0.5, 0.001, 0.02);
                x.reverb(0.5, 0.5, 0.15);
                return x;
            }
            case BOSS_LOB_LAB -> {                                               // a flask thrown high: whoosh, then a glass clink
                Sx x = new Sx(0.7, seed);
                x.noise(BAND, 0, 0.35, 500, 2400, 0.9, 1, 0.1, 0.15).tone(SINE, 0, 0.3, 280, 800, 0.4, 0.05, 0.12)
                    .bell(0.05, 1800, new double[]{1, 2.4, 3.9}, new double[]{1, 0.5, 0.3}, 0.07, 0.3);
                x.reverb(0.4, 0.5, 0.12);
                return x;
            }
            case BOSS_BOMB_LAB -> {                                              // a flask bursting: splash, glass and a fizz of acid
                Sx x = new Sx(0.9, seed);
                x.tone(SINE, 0, 0.35, 130 * k, 46, 0.6, 0.002, 0.1).noise(LOW, 0, 0.35, 2800, 300, 0.8, 1.4, 0.002, 0.1)
                    .noise(BAND, 0, 0.5, 3000, 1500, 0.8, 0.5, 0.005, 0.18).crackle(0.03, 0.7, 16, 3800, 0.4)
                    .bell(0.01, 1200 * k, new double[]{1, 2.4, 3.9, 5.6}, new double[]{1, 0.6, 0.4, 0.3}, 0.1, 0.5);
                x.reverb(0.5, 0.55, 0.16);
                return x;
            }
            case LAB_BEEP -> {
                Sx x = new Sx(0.55, seed);
                int[][] pats = {{88, 84}, {91, 91, 91}, {81, 81, 86}};
                int[] p = pats[v % 3];
                for (int i = 0; i < p.length; i++) x.tone(TRI, 0.09 * i, 0.07, n(p[i]), n(p[i]), 1, 0.003, 0.05);
                x.reverb(0.4, 0.5, 0.15);
                return x;
            }
            case LAB_BUBBLE -> {
                Sx x = new Sx(0.9, seed);
                int count = 4 + v;
                for (int i = 0; i < count; i++) {
                    double f = x.rnd.range(420, 900);
                    x.tone(SINE, x.rnd.range(0, 0.6), 0.09, f, f * 1.7, 1, 0.005, 0.03);
                }
                x.reverb(0.4, 0.5, 0.12);
                return x;
            }
            // ------------------------------------------------------------ the tutorial
            case SQUIRREL_TALK -> {                                              // a tiny chirp; a different note each time, so speech chatters
                int[] notes = {84, 86, 88, 91};                                  // C D E G, two octaves and a bit above middle C
                double f = n(notes[v % 4]);
                Sx x = new Sx(0.1, seed);
                x.tone(SINE, 0, 0.07, f * 0.95, f * 1.1, 1, 0.004, 0.025).tone(SINE, 0, 0.07, f * 1.9, f * 2.2, 0.22, 0.004, 0.02)
                    .noise(BAND, 0, 0.012, 4500, 4500, 1, 0.25, 0.0005, 0.004);
                return x;
            }
            case SQUIRREL_ALARM -> {                                             // "eek!": two frightened rising squeals
                int s0 = v == 0 ? 0 : 2;
                Sx x = new Sx(0.55, seed);
                x.tone(SINE, 0, 0.14, n(84 + s0), n(96 + s0), 1, 0.004, 0.08).tone(SINE, 0, 0.14, n(96 + s0), n(108 + s0), 0.2, 0.004, 0.06)
                    .tone(SINE, 0.15, 0.2, n(91 + s0), n(103 + s0), 1, 0.004, 0.1).tone(TRI, 0.15, 0.2, n(79 + s0), n(91 + s0), 0.3, 0.004, 0.1)
                    .noise(BAND, 0, 0.02, 3500, 3500, 1, 0.3, 0.0005, 0.006);
                x.reverb(0.3, 0.5, 0.1);
                return x;
            }
            case SQUIRREL_SCURRY -> {                                            // little feet in dry leaves, quickening
                Sx x = new Sx(0.55, seed);
                double t = 0;
                for (int i = 0; i < 11; i++) {
                    x.noise(BAND, t, 0.02, 1900 * (1 + 0.1 * x.rnd.range(-1, 1)), 1500, 1.1, 0.9, 0.0005, 0.007)
                        .tone(SINE, t, 0.02, 330, 190, 0.35, 0.001, 0.008);     // a little weight under each tap
                    t += 0.055 - i * 0.002 + 0.01 * v;
                }
                x.noise(BAND, 0, 0.5, 2400, 1600, 0.6, 0.15, 0.05, 0.2).lowpass(6000);
                return x;
            }
            case ACORN_THROW -> {                                                // a little "hup" and a whoosh
                Sx x = new Sx(0.4, seed);
                x.noise(BAND, 0.02, 0.22, 1300 * k, 2600, 0.9, 1, 0.03, 0.09).tone(SINE, 0, 0.09, n(88), n(84), 0.35, 0.004, 0.04)
                    .tone(SINE, 0, 0.09, n(88) * 2, n(84) * 2, 0.07, 0.004, 0.03);
                return x;
            }
            case ACORN_BONK -> {                                                 // a wooden knock and a cartoon wobble
                Sx x = new Sx(0.55, seed);
                Sx wobble = x.part(0.32).tone(SINE, 0, 0.3, 720 * k, 250, 1, 0.005, 0.13).am(0, 0.3, 22, 0.6);
                x.tone(SINE, 0, 0.16, 300 * k, 105, 0.8, 0.001, 0.05).noise(BAND, 0, 0.03, 2300, 3000, 1, 0.45, 0.0005, 0.009)
                    .bell(0, 640 * k, new double[]{1, 2.3, 3.9}, new double[]{1, 0.5, 0.25}, 0.06, 0.3).mix(wobble, 0.05, 0.7);
                return x;
            }
            case ROLL_DODGE -> {                                                 // slipped through: a bright rising sparkle
                Sx x = new Sx(0.9, seed);
                x.fm(0, 0.6, n(91), 3.5, 1.2, 0.7, 0.22).fm(0.06, 0.7, n(96), 3.5, 1.2, 0.7, 0.28).fm(0.12, 0.8, n(100), 3.5, 1.2, 0.5, 0.3)
                    .noise(HIGH, 0, 0.18, 3000, 7000, 0.7, 0.25, 0.03, 0.07);
                x.reverb(0.5, 0.5, 0.25);
                return x;
            }
            case TUT_DONE -> {                                                   // a lesson learned: a soft, warm arpeggio
                int[] up = {79, 84, 88, 91};
                Sx x = new Sx(1.6, seed);
                for (int i = 0; i < up.length; i++) x.fm(0.08 * i, 1.0, n(up[i]), 3.5, 1.1, 0.7, 0.32);
                x.fm(0.3, 1.2, n(72), 2, 0.8, 0.35, 0.5);
                x.reverb(0.6, 0.5, 0.32);
                return x;
            }
            case TREE_BONK -> {                                                  // head against a tree trunk: a hollow knock, then stars
                Sx x = new Sx(0.9, seed);
                Sx wobble = x.part(0.4).tone(SINE, 0, 0.38, 520, 190, 1, 0.005, 0.16).am(0, 0.38, 14, 0.5);
                x.tone(SINE, 0, 0.3, 220, 95, 0.8, 0.001, 0.08).noise(BAND, 0, 0.03, 1900, 2600, 1, 0.7, 0.0005, 0.01)
                    .bell(0, 300, new double[]{1, 2.3, 3.9}, new double[]{1, 0.6, 0.35}, 0.14, 0.7).mix(wobble, 0.04, 0.6)
                    .bell(0.12, 1700, new double[]{1, 2.4, 3.9}, new double[]{1, 0.5, 0.3}, 0.1, 0.15).bell(0.2, 2200, new double[]{1, 2.4, 3.9}, new double[]{1, 0.5, 0.3}, 0.1, 0.12);
                x.reverb(0.3, 0.6, 0.1);
                return x;
            }
            case WAKE_UP -> {                                                    // a heartbeat, a sleepy wobbling hum and a thin ringing
                Sx x = new Sx(2.6, seed);
                for (double t : new double[]{0.1, 0.4, 1.15, 1.45}) x.tone(SINE, t, 0.2, 130, 62, t == 0.1 || t == 1.15 ? 0.7 : 0.5, 0.004, 0.07);
                Sx hum = x.part(2.3).tone(SINE, 0, 2.2, n(60), n(60) * 1.008, 1, 0.5, 1.0).tone(SINE, 0, 2.2, n(67), n(67) * 0.994, 0.6, 0.5, 1.0)
                    .tone(SINE, 0, 2.2, n(72), n(72), 0.25, 0.6, 1.0).am(0, 2.2, 3.2, 0.6);
                x.mix(hum, 0.1, 0.8).tone(SINE, 0, 2.3, n(96), n(96), 0.05, 0.6, 1.0);
                x.reverb(0.6, 0.55, 0.25);
                return x;
            }
            case SHELL_CURL -> {                                                 // creaking thorns pulling in tight
                Sx x = new Sx(0.7, seed);
                x.noise(BAND, 0, 0.35, 700, 2200, 0.9, 1.0, 0.05, 0.14).crackle(0.03, 0.4, 14, 3400, 0.6)
                    .tone(SINE, 0.28, 0.2, 190, 110, 0.4, 0.003, 0.07).noise(BAND, 0.28, 0.12, 900, 500, 1.0, 0.7, 0.002, 0.04)
                    .bell(0.3, 420, new double[]{1, 2.3, 3.9}, new double[]{1, 0.5, 0.3}, 0.07, 0.3);
                x.reverb(0.3, 0.6, 0.1);
                return x;
            }
            case SHELL_CLINK -> {                                                // a sword glancing off wood and thorn
                Sx x = new Sx(0.4, seed);
                x.bell(0, v == 0 ? 520 : 610, BELL, BELL_AMP, 0.1, 0.6).noise(BAND, 0, 0.025, 3200, 4200, 1, 0.9, 0.0005, 0.007)
                    .tone(SINE, 0, 0.09, 220, 120, 0.5, 0.001, 0.03);
                return x;
            }
            case SHELL_BREAK -> {                                                // the shell cracks open in a burst of leaves
                Sx x = new Sx(1.4, seed);
                x.noise(BAND, 0, 0.06, 2000, 3200, 1, 1.5, 0.0005, 0.015).crackle(0, 0.5, 22, 4200, 0.7)
                    .tone(SINE, 0, 0.35, 170, 58, 0.8, 0.001, 0.1).noise(LOW, 0, 0.25, 2200, 300, 0.8, 1.0, 0.002, 0.08)
                    .noise(BAND, 0.05, 0.4, 900, 2600, 0.7, 0.4, 0.05, 0.14);
                int[] up = {84, 88, 91};
                for (int i = 0; i < up.length; i++) x.fm(0.12 + 0.07 * i, 1.0, n(up[i]), 3.5, 1.2, 0.5, 0.3);
                x.reverb(0.5, 0.55, 0.2);
                return x;
            }
            // ------------------------------------------------------------ crates and barrels
            case CRATE_SMASH -> {                                                // planks cracking apart, splinters, and a small bright reward chime
                Sx x = new Sx(0.8, seed);
                x.noise(BAND, 0, 0.05, 1500 * k, 2800, 1, 0.4, 0.0008, 0.014).tone(SINE, 0, 0.2, 340 * k, 150, 0.9, 0.002, 0.07)
                    .noise(BAND, 0.02, 0.22, 1100, 500, 0.9, 0.9, 0.006, 0.09).crackle(0.03, 0.28, 12, 3200, 0.35)
                    .bell(0, 420 * k, new double[]{1, 2.3, 3.9}, new double[]{1, 0.5, 0.3}, 0.06, 0.4)
                    .fm(0.07, 0.6, n(91), 3.5, 1.0, 0.22, 0.2).fm(0.13, 0.6, n(96), 3.5, 1.0, 0.2, 0.22);
                x.reverb(0.3, 0.6, 0.08);
                return x;
            }
            case BARREL_SMASH -> {                                               // a hollow boom, staves flying, and the same reward chime
                Sx x = new Sx(0.9, seed);
                x.tone(SINE, 0, 0.3, 260 * k, 110, 0.9, 0.002, 0.1).noise(BAND, 0, 0.04, 1300 * k, 2400, 1, 0.4, 0.0008, 0.012)
                    .noise(BAND, 0.02, 0.3, 900, 400, 0.8, 0.9, 0.006, 0.12).crackle(0.03, 0.3, 10, 2800, 0.35)
                    .bell(0, 260 * k, new double[]{1, 2.1, 3.4}, new double[]{1, 0.6, 0.3}, 0.12, 0.6)
                    .fm(0.08, 0.6, n(88), 3.5, 1.0, 0.22, 0.2).fm(0.14, 0.6, n(91), 3.5, 1.0, 0.2, 0.22);
                x.reverb(0.4, 0.55, 0.12);
                return x;
            }
            case CITY_HORN -> {
                Sx x = new Sx(2.2, seed);
                double f = v == 0 ? 220 : 196;
                x.tone(SAW, 0, 1.2, f, f, 1, 0.06, 0.6).tone(SAW, 0, 1.2, f * 1.26, f * 1.26, 0.8, 0.06, 0.6).lowpass(700);
                x.reverb(0.8, 0.6, 0.6);
                return x;
            }
        }
        throw new IllegalStateException("no recipe for " + snd);
    }
}
