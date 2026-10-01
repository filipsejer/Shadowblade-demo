package game;

import game.Song.Mood;
import game.Songs.Tune;

/**
 * Decides which music should be playing from what is happening in the game. It is a pure function of the world, so it is
 * easy to test: the calm theme while you explore, the same theme with the drums and bass added once a fight gets going,
 * the boss piece for the guardian (with its second wave of parts below half health), and silence after a defeat.
 */
final class MusicDirector {
    private MusicDirector() {}

    /** {@code bed}: 0 none, 1 forest ambience, 2 city ambience, 3 the laboratory's hum. */
    record Choice(Tune tune, Mood mood, boolean paused, int bed, double bedLevel) {}

    static Choice choose(World w) {
        Theme theme = w.level.theme;
        Tune calm = switch (theme) { case FOREST -> Tune.FOREST; case CITY -> Tune.CITY; case LAB -> Tune.LAB; };
        int bed = switch (theme) { case FOREST -> 1; case CITY -> 2; case LAB -> 3; };
        return switch (w.state) {
            case TITLE -> new Choice(Tune.FOREST, Mood.CALM, false, 1, 0.6);
            case ARMORY -> new Choice(calm, Mood.CALM, !w.titleScene, bed, 0.6);
            case RESULTS -> w.run != null && w.run.outcome == Run.Outcome.VICTORY ? new Choice(calm, Mood.CALM, false, bed, 0.5) : new Choice(null, Mood.CALM, false, 0, 0);
            default -> {
                boolean muffled = w.state != World.State.PLAYING;
                if (w.run != null) {                               // a fight: the theme, drums in once it gets going; the boss piece for the guardian
                    Enemy boss = w.boss();
                    if (boss != null) {
                        Tune t = switch (theme) { case FOREST -> Tune.FOREST_BOSS; case CITY -> Tune.CITY_BOSS; case LAB -> Tune.LAB_BOSS; };
                        yield new Choice(t, boss.phase2 ? Mood.PEAK : Mood.FIGHT, muffled, bed, 0.15);
                    }
                    boolean fight = w.run.time > 15 && !w.run.bossDead;
                    yield new Choice(calm, fight ? Mood.FIGHT : Mood.CALM, muffled, bed, fight ? 0.35 : 1.0);
                }
                yield new Choice(calm, Mood.CALM, muffled, bed, 1.0);
            }
        };
    }
}
