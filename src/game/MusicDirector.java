package game;

import game.Song.Mood;
import game.Songs.Tune;

/**
 * Decides which music should be playing from what is happening in the game. It is a pure function of the world, so it is
 * easy to test: the calm theme while you explore; in a fight, the battle theme (a few bars of piano alone, then the whole
 * band, swelling further while you're taking a nest down); the boss piece for the guardian (with its second
 * wave of parts below half health); the calm theme again once the way home is open; silence after a defeat; and the
 * music holding its breath while you talk to someone.
 */
final class MusicDirector {
    private MusicDirector() {}

    /** Seconds of a fight the battle theme plays its quiet opening (four bars of piano) before the band comes in. */
    static final double BATTLE_INTRO = 6.3;

    /** {@code bed}: 0 none, 1 forest ambience, 2 city ambience, 3 the laboratory's hum. */
    /** {@code hushed}: the song fades out but keeps its place, and carries on from there once it's lifted. */
    record Choice(Tune tune, Mood mood, boolean paused, int bed, double bedLevel, boolean hushed) {
        Choice(Tune tune, Mood mood, boolean paused, int bed, double bedLevel) { this(tune, mood, paused, bed, bedLevel, false); }
    }

    static Choice choose(World w) {
        Theme theme = w.level.theme;
        Tune calm = switch (theme) { case FOREST -> Tune.FOREST; case CITY -> Tune.CITY; case LAB -> Tune.LAB; };
        int bed = switch (theme) { case FOREST -> 1; case CITY -> 2; case LAB -> 3; };
        return switch (w.state) {
            case TITLE, SLOTS -> new Choice(Tune.FOREST, Mood.CALM, false, 1, 0.6);
            case ARMORY -> new Choice(calm, Mood.CALM, true, bed, 0.6);
            case RESULTS -> w.run != null && w.run.outcome == Run.Outcome.VICTORY ? new Choice(calm, Mood.CALM, false, bed, 0.5) : new Choice(null, Mood.CALM, false, 0, 0);
            default -> {
                boolean muffled = w.state != World.State.PLAYING;
                if (w.run != null) {                               // a fight: the theme, drums in once it gets going; the boss piece for the guardian
                    Enemy boss = w.boss();
                    if (boss != null) {
                        Tune t = switch (theme) { case FOREST -> Tune.FOREST_BOSS; case CITY -> Tune.CITY_BOSS; case LAB -> Tune.LAB_BOSS; };
                        yield new Choice(t, boss.phase2 ? Mood.PEAK : Mood.FIGHT, muffled, bed, 0.15);
                    }
                    if (w.run.bossDead) yield new Choice(calm, Mood.CALM, muffled, bed, 1.0);   // the way home is open: the calm theme again
                    if (theme != Theme.LAB) {
                        Mood mood = w.run.time < BATTLE_INTRO ? Mood.CALM : intense(w) ? Mood.PEAK : Mood.FIGHT;
                        yield new Choice(theme == Theme.CITY ? Tune.CITY_BATTLE : Tune.FOREST_BATTLE, mood, muffled, bed, 0.35);
                    }
                    boolean fight = w.run.time > 15;                   // (the lab: its calm theme, with drums once it gets going)
                    yield new Choice(calm, fight ? Mood.FIGHT : Mood.CALM, muffled, bed, fight ? 0.35 : 1.0);
                }
                if (w.musicHushed()) yield new Choice(calm, Mood.CALM, muffled, bed, 0.6, true);   // talking to someone: just the forest around you
                yield new Choice(calm, Mood.CALM, muffled, bed, 1.0);
            }
        };
    }

    /** How close a woken nest has to be for the battle theme's horns and cymbals to come in. */
    static final double NEST_ASSAULT = 650;

    /** Taking a nest down (a woken one close by), or holding a relay: the set pieces of a fight, when the music swells. */
    private static boolean intense(World w) {
        if (w.run != null && w.run.chargingRelay() != null) return true;
        for (Enemy e : w.enemies) {
            if (e.rooted() && e.awake && e.hp > 0 && Util.dist(e.x, e.y, w.player.x, w.player.y) < NEST_ASSAULT) return true;
        }
        return false;
    }
}
