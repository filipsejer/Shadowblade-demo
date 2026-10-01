package game;

/**
 * A fight you can take on from the explorable world: someone asks for help, you walk to the place (its gate), and
 * you're taken to a battlefield made fresh for the occasion ({@link Level#battlefield}). There you destroy the Blight's
 * nests while the horde keeps coming; with the last one gone, the challenge's guardian (if it has one) arrives, and
 * beating it opens the way home. Everything else about the fight is {@link Run}'s.
 *
 * <p>A challenge can be fought again after it's cleared. Each clear makes the next attempt more dangerous (and better
 * paid), like looping in Risk of Rain.
 */
enum Challenge {
    HOLLOW("hollow", "THE BLIGHTED HOLLOW", "Elder Rowan", Theme.FOREST,
        "The Blight's nests choke the Hollow. Destroy all four, then face what guards them.",
        4, 1.0, true, Enemy.Type.GRUNT, 160, 5, 70, 1, 24),
    FOX_DENS("dens", "FOX DEN RAID", "Hunter Fenn", Theme.FOREST,
        "Three dens of Blight-sick foxes, east of Mossbrook. Tear the dens down.",
        3, 0.0, false, Enemy.Type.RUNNER, 70, 2, 45, 0, 16);

    final String id, title, giver, brief;
    final Theme theme;
    /** How many nests the battlefield holds. */
    final int nests;
    /** How dangerous it is from the first second (the threat level the director starts from). */
    final double danger;
    /** True if a guardian comes once the nests are down (otherwise the way home opens straight away). */
    final boolean boss;
    /** The monster the horde has most of. */
    final Enemy.Type favoured;
    /** Paid on the first clear, and on every clear after that. */
    final int gold, skillPoints, repeatGold, repeatSkillPoints;
    /** How many map cells the battlefield grows to (its size). */
    final int cells;

    Challenge(String id, String title, String giver, Theme theme, String brief, int nests, double danger, boolean boss,
              Enemy.Type favoured, int gold, int skillPoints, int repeatGold, int repeatSkillPoints, int cells) {
        this.id = id;
        this.title = title;
        this.giver = giver;
        this.theme = theme;
        this.brief = brief;
        this.nests = nests;
        this.danger = danger;
        this.boss = boss;
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

    /** The extra danger each earlier clear adds to the next attempt. */
    static final double LOOP_DANGER = 1.5;
}
