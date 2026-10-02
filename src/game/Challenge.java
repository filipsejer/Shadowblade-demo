package game;

/**
 * A fight you can take on from the explorable world: someone asks for help, you walk to the place (its gate), and
 * you're taken to a battlefield made fresh for the occasion ({@link Battlefield#make}). There the goal is one of two
 * kinds ({@link Goal}): destroy the Blight's nests while the horde keeps coming, or switch the city's relays back on
 * and hold the ground round each one while it powers up. With that done, the challenge's guardian (if it has one)
 * arrives, and beating it opens the way home. Everything else about the fight is {@link Run}'s.
 *
 * <p>A challenge can be fought again after it's cleared. Each clear makes the next attempt more dangerous (and better
 * paid), like looping in Risk of Rain.
 */
enum Challenge {
    // ---- chapter 1: the Whispering Forest
    HOLLOW("hollow", "THE BLIGHTED HOLLOW", "Elder Rowan", Theme.FOREST, Goal.NESTS, "THE BLIGHTED GUARDIAN",
        "The Blight's nests choke the Hollow. Destroy all four, then face what guards them.",
        4, 1.0, Enemy.Type.GRUNT, 160, 5, 70, 1, 24),
    FOX_DENS("dens", "FOX DEN RAID", "Hunter Fenn", Theme.FOREST, Goal.NESTS, null,
        "Three dens of Blight-sick foxes, east of Mossbrook. Tear the dens down.",
        3, 0.0, Enemy.Type.RUNNER, 70, 2, 45, 0, 16),

    // ---- chapter 2: the city of Lumen
    ALLEYS("alleys", "THE BACK ALLEYS", "Old Gus", Theme.CITY, Goal.NESTS, null,
        "The Blight has nested in the alleys behind the canal, and the rats have turned. Three nests. Burn them out.",
        3, 1.5, Enemy.Type.GRUNT, 110, 3, 60, 1, 18),
    SUBSTATION("substation", "THE SUBSTATION", "Tinker Juno", Theme.CITY, Goal.RELAYS, null,
        "Three relays feed the city's power, and the Blight has crept over all of them. Switch each one back on and hold the ground round it while it powers up.",
        3, 2.0, Enemy.Type.RUNNER, 220, 5, 90, 1, 22),
    TOWER("tower", "THE DYNAMO TOWER", "Captain Vell", Theme.CITY, Goal.NESTS, "THE WARDEN",
        "The Blight's roots climb the tower to the Dynamo at its top, and the Warden guards them. Tear out the nests, then bring the Warden down.",
        5, 3.0, Enemy.Type.GRUNT, 300, 6, 110, 1, 28);

    /** What a fight asks of you: destroy the nests, or power the relays up. */
    enum Goal { NESTS, RELAYS }

    final String id, title, giver, brief;
    final Theme theme;
    /** The world whose gate leads here ({@link Worlds#of}). */
    final String world;
    final Goal goal;
    /** The guardian that comes once the goal is met, or null if the way home opens straight away. */
    final String bossName;
    /** True if a guardian comes once the goal is met. */
    final boolean boss;
    /** How many nests (or relays) the battlefield holds. */
    final int nests;
    /** How dangerous it is from the first second (the threat level the director starts from). */
    final double danger;
    /** The monster the horde has most of. */
    final Enemy.Type favoured;
    /** Paid on the first clear, and on every clear after that. */
    final int gold, skillPoints, repeatGold, repeatSkillPoints;
    /** How many map cells the battlefield grows to (its size). */
    final int cells;

    Challenge(String id, String title, String giver, Theme theme, Goal goal, String bossName, String brief, int nests, double danger,
              Enemy.Type favoured, int gold, int skillPoints, int repeatGold, int repeatSkillPoints, int cells) {
        this.id = id;
        this.title = title;
        this.giver = giver;
        this.theme = theme;
        this.world = theme == Theme.FOREST ? Worlds.FOREST : Worlds.CITY;
        this.goal = goal;
        this.bossName = bossName;
        this.boss = bossName != null;
        this.brief = brief;
        this.nests = nests;
        this.danger = danger;
        this.favoured = favoured;
        this.gold = gold;
        this.skillPoints = skillPoints;
        this.repeatGold = repeatGold;
        this.repeatSkillPoints = repeatSkillPoints;
        this.cells = cells;
    }

    static Challenge byId(String id) {
        for (Challenge c : values()) if (c.id.equals(id)) return c;
        return null;
    }

    /** The goal in a few words, for the HUD ("NESTS", "RELAYS"). */
    String goalWord() { return goal == Goal.RELAYS ? "RELAYS" : "NESTS"; }

    /** The extra danger each earlier clear adds to the next attempt. */
    static final double LOOP_DANGER = 1.5;
}
