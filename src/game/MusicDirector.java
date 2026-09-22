package game;

import game.Song.Mood;
import game.Songs.Tune;

/**
 * Decides which music should be playing from what is happening in the game. It is a pure function of the world, so it is
 * easy to test: the calm theme of the level you are in while exploring, the same theme with the drums and bass added when
 * a room's doors are locked, the boss piece for the boss fight (with its second wave of parts below half health), and
 * silence once you have died.
 */
final class MusicDirector {
    private MusicDirector() {}

    /** {@code bed}: 0 none, 1 forest ambience, 2 city ambience. */
    record Choice(Tune tune, Mood mood, boolean paused, int bed, double bedLevel) {}

    static Choice choose(World w) {
        Theme theme = w.level.theme;
        Tune calm = switch (theme) { case FOREST -> Tune.FOREST; case CITY -> Tune.CITY; case LAB -> Tune.LAB; };
        int bed = switch (theme) { case FOREST -> 1; case CITY -> 2; case LAB -> 3; };
        return switch (w.state) {
            case TITLE -> new Choice(Tune.FOREST, Mood.CALM, false, 1, 0.6);
            case GAME_OVER -> new Choice(null, Mood.CALM, false, 0, 0);
            default -> {
                boolean paused = w.state == World.State.PAUSE;
                Enemy boss = w.boss();
                if (boss != null) {
                    Tune t = switch (theme) { case FOREST -> Tune.FOREST_BOSS; case CITY -> Tune.CITY_BOSS; case LAB -> Tune.LAB_BOSS; };
                    yield new Choice(t, boss.phase2 ? Mood.PEAK : Mood.FIGHT, paused, bed, 0.15);
                }
                if (w.activeRoom != null) yield new Choice(calm, Mood.FIGHT, paused, bed, 0.35);
                yield new Choice(calm, Mood.CALM, paused, bed, 1.0);
            }
        };
    }
}
