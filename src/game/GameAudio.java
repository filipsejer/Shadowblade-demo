package game;

import java.util.List;

/**
 * The link between the game and the sound engine. Once per frame it sets the volumes, tells the engine which music and
 * ambience the {@link MusicDirector} wants, and turns the sound cues the world queued into positioned, pitch-varied
 * sounds: a hit on the far side of the room is quieter and off to one side.
 */
final class GameAudio {
    private final AudioEngine engine;
    private Songs.Tune tune;
    private Song.Mood mood;
    private boolean paused, first = true;
    private int bed = -1;
    private double bedLevel = -1;
    private final java.util.Random rng = new java.util.Random(5);

    GameAudio(AudioEngine engine) { this.engine = engine; }

    void update(World w) {
        AudioSettings s = w.audio;
        engine.setVolumes(0.8, s.musicVolume(), s.sfxVolume(), 0.8, s.muted);

        MusicDirector.Choice c = MusicDirector.choose(w);
        if (first || c.tune() != tune || c.mood() != mood || c.paused() != paused) {
            boolean toBoss = c.tune() == Songs.Tune.FOREST_BOSS || c.tune() == Songs.Tune.CITY_BOSS || c.tune() == Songs.Tune.LAB_BOSS;
            double fadeOut = c.tune() == null ? 1.6 : toBoss ? 0.4 : 1.6;
            engine.setMusic(c.tune() == null ? null : Songs.get(c.tune()), c.mood(), c.paused(), fadeOut);
            tune = c.tune();
            mood = c.mood();
            paused = c.paused();
        }
        if (first || c.bed() != bed || Math.abs(c.bedLevel() - bedLevel) > 0.001) {
            engine.setAmbience(c.bed(), c.bedLevel());
            bed = c.bed();
            bedLevel = c.bedLevel();
        }
        first = false;

        List<World.Cue> cues = w.drainSounds();
        for (World.Cue cue : cues) play(w, cue);
    }

    private void play(World w, World.Cue cue) {
        double gain = cue.gain(), pan = 0;
        if (cue.positional()) {
            double dx = cue.x() - w.player.x, dy = cue.y() - w.player.y;
            double d = Math.hypot(dx, dy);
            gain *= Math.max(0.12, 1 / (1 + Math.pow(d / 750, 2)));
            pan = Dsp.clamp(dx / 650, -1, 1) * 0.7;
        }
        double rate = cue.rate() * (1 + (rng.nextDouble() - 0.5) * 0.05);        // never quite the same twice
        engine.cue(cue.snd(), gain, pan, cue.delay(), rate);
    }
}
