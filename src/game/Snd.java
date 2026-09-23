package game;

/**
 * Every sound effect the game can make, with how loud it is meant to be and how often it may repeat.
 *
 * <p>{@code loudDb} is how loud the sound is, measured as the A-weighted RMS of its loudest tenth of a second in dBFS
 * (0 dB is the loudest a speaker can play). Measuring loudness the way an ear hears it, rather than by the single highest
 * peak or by raw energy, makes a swishy whoosh, a punchy thud and a deep rumble of the same number seem equally loud. Small
 * everyday sounds sit a few dB under the music (which measures about -18 on this scale), impacts sit above it, and big
 * events (a boss slam, the boss dying) sit well above, as far as the -1.5 dB peak limit allows. {@code minGap} stops a sound
 * being triggered again too soon, so ten enemies can't turn one hit sound into a machine gun, and {@code voices} is how
 * many copies may play at once. {@code variants} is how many slightly different versions are painted, taken in turn so
 * the same sound never repeats exactly.
 */
enum Snd {
    // interface
    MENU_MOVE(-23, 0.03, 2, 1),
    MENU_OPEN(-21, 0.05, 1, 1),
    MENU_BACK(-23, 0.05, 1, 1),
    MENU_SELECT(-16, 0.05, 2, 1),
    MENU_DENY(-21, 0.10, 1, 1),
    PAUSE_IN(-19, 0.10, 1, 1),
    PAUSE_OUT(-19, 0.10, 1, 1),
    LOCK_ON(-19, 0.05, 1, 1),
    LOCK_OFF(-23, 0.05, 1, 1),
    TITLE_START(-14, 0.50, 1, 1),

    // the hero
    SWING_LIGHT(-21, 0.05, 2, 3),
    SWING_HEAVY(-17, 0.05, 2, 2),
    DASH(-21, 0.08, 1, 2),
    HIT_LIGHT(-15, 0.03, 4, 4),
    HIT_HEAVY(-11, 0.05, 3, 3),
    HIT_ARMOR(-12, 0.05, 2, 2),
    ROLL(-22, 0.15, 1, 2),
    STEP_GRASS(-28, 0.12, 2, 4),
    STEP_STONE(-28, 0.12, 2, 4),
    PLAYER_HURT(-13, 0.15, 1, 3),
    LEVEL_UP(-14, 0.50, 1, 1),
    SPELL_FAIL(-23, 0.15, 1, 1),

    // spells
    CAST_FIRE(-17, 0.10, 2, 2),
    FIRE_EXPLODE(-10, 0.05, 3, 3),
    CAST_ICE(-18, 0.20, 1, 2),
    ICE_TICK(-28, 0.25, 2, 3),
    CAST_BOLT(-13, 0.15, 1, 2),
    ZAP_HIT(-19, 0.04, 4, 3),
    CAST_HEAL(-18, 0.30, 1, 1),

    // enemies
    WINDUP_SMALL_FOREST(-26, 0.12, 2, 3),
    WINDUP_SMALL_CITY(-26, 0.12, 2, 3),
    WINDUP_BIG_FOREST(-20, 0.20, 1, 1),
    WINDUP_BIG_CITY(-20, 0.20, 1, 1),
    ENEMY_STRIKE(-22, 0.08, 3, 2),
    SHOOT_FOREST(-22, 0.10, 3, 3),
    SHOOT_CITY(-22, 0.10, 3, 3),
    DIE_FOREST(-18, 0.04, 4, 3),
    DIE_CITY(-18, 0.04, 4, 3),
    DIE_BIG_FOREST(-12, 0.10, 2, 2),
    DIE_BIG_CITY(-12, 0.10, 2, 2),
    SHADE_OUT(-23, 0.20, 2, 1),
    SHADE_IN(-23, 0.20, 2, 1),

    // bosses
    BOSS_INTRO(-9, 1.00, 1, 1),
    BOSS_BURST_FOREST(-13, 0.30, 1, 1),
    BOSS_BURST_CITY(-13, 0.30, 1, 1),
    BOSS_SLAM_FOREST(-8, 0.30, 1, 1),
    BOSS_SLAM_CITY(-8, 0.30, 1, 1),
    BOSS_PHASE2_FOREST(-8, 1.00, 1, 1),
    BOSS_PHASE2_CITY(-8, 1.00, 1, 1),
    BOSS_DIE(-8, 1.00, 1, 1),

    // the world
    LOCK_FOREST(-13, 0.30, 1, 1),
    LOCK_CITY(-13, 0.30, 1, 1),
    ROOM_CLEAR(-15, 0.50, 1, 1),
    BOSS_UNSEAL(-11, 1.00, 1, 1),
    GUIDE_APPEAR(-16, 1.00, 1, 1),
    TRAVEL(-13, 1.00, 1, 1),
    GAME_OVER(-12, 1.00, 1, 1),
    GAME_CLEARED(-11, 1.00, 1, 1),
    SEALED(-21, 1.00, 1, 1),

    // ambience one-shots
    BIRD(-33, 0.50, 2, 4),
    CITY_HORN(-38, 2.00, 1, 2),

    // the laboratory (added last so the sounds above keep their exact variations)
    LOCK_LAB(-13, 0.30, 1, 1),
    WINDUP_SMALL_LAB(-26, 0.12, 2, 3),
    WINDUP_BIG_LAB(-20, 0.20, 1, 1),
    SHOOT_LAB(-22, 0.10, 3, 3),
    DIE_LAB(-18, 0.04, 4, 3),
    DIE_BIG_LAB(-12, 0.10, 2, 2),
    BOSS_BURST_LAB(-13, 0.30, 1, 1),
    BOSS_SLAM_LAB(-8, 0.30, 1, 1),
    BOSS_PHASE2_LAB(-8, 1.00, 1, 1),
    BOSS_SUMMON_LAB(-16, 0.15, 2, 1),
    LAB_BEEP(-36, 0.50, 2, 3),
    LAB_BUBBLE(-38, 0.50, 2, 3),
    BOSS_LOB_LAB(-18, 0.30, 1, 1),
    BOSS_BOMB_LAB(-14, 0.06, 3, 2),

    // the tutorial (added last so every sound above keeps its exact variations)
    SQUIRREL_TALK(-27, 0.045, 2, 4),
    SQUIRREL_ALARM(-14, 0.30, 1, 2),
    SQUIRREL_SCURRY(-24, 0.20, 1, 2),
    ACORN_THROW(-21, 0.30, 1, 2),
    ACORN_BONK(-15, 0.10, 2, 2),
    ROLL_DODGE(-17, 0.20, 1, 1),
    TUT_DONE(-15, 0.50, 1, 1),
    TREE_BONK(-15, 0.30, 1, 1),
    WAKE_UP(-19, 1.00, 1, 1),
    SHELL_CURL(-18, 0.30, 1, 1),
    SHELL_CLINK(-15, 0.05, 2, 2),
    SHELL_BREAK(-11, 0.10, 1, 1),

    // crates and barrels
    CRATE_SMASH(-16, 0.05, 3, 3),
    BARREL_SMASH(-16, 0.05, 3, 3),

    // Transit Town
    TOWN_TALK(-27, 0.045, 2, 4),

    // air combos
    ENEMY_LAND(-24, 0.06, 3, 2);

    final double loudDb, minGap;
    final int voices, variants;

    Snd(double loudDb, double minGap, int voices, int variants) {
        this.loudDb = loudDb;
        this.minGap = minGap;
        this.voices = voices;
        this.variants = variants;
    }
}
