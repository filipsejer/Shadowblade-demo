package game;

import java.util.List;
import java.util.Random;

/**
 * The words: what everyone in the world says (it changes as the story moves on), what each chest holds, and the line
 * at the top of the screen saying what to do next. Talking can move the story on too (a quest accepted, a gift given).
 *
 * <p>Chapter 1, the Whispering Forest: the stars fell, and three nights later the Blight crept out of the Hollow north
 * of Mossbrook. Elder Rowan asks the wandering Spellblade to burn it out.
 */
final class Story {
    private Story() {}

    private static final String HERO = "hero.down.idle";

    /** The opening of a new game. */
    static void intro(World w) {
        Dialogue d = w.dialogue;
        d.say("YOU", HERO, Snd.TOWN_TALK, "Three days on the road, and the trees here still whisper. There's a camp ahead - smoke, voices.");
        d.say("ELDER ROWAN", "town.elder.idle", Snd.TOWN_TALK, "A traveller with a blade? Over here, please! Come and talk to me.");
    }

    /** What to do next, for the top of the screen (and the pause menu). */
    static String objective(Adventure a) {
        if (!a.has("met.rowan")) return "Speak with Elder Rowan in the middle of the camp";
        if (a.clears(Challenge.HOLLOW) == 0) return "Clear the Blighted Hollow: north, past the Old Shrine";
        if (!a.has("gift.hollow")) return "Return to Elder Rowan in Mossbrook";
        return "Chapter 1 complete. The road south will open soon";
    }

    /** True if someone has something new to say or offer (a "!" over their head). */
    static boolean hasNews(Adventure a, String id) {
        return switch (id) {
            case "rowan" -> !a.has("met.rowan") || a.clears(Challenge.HOLLOW) > 0 && !a.has("gift.hollow");
            case "ash" -> {
                if (!a.has("met.ash")) yield true;
                for (Mastery m : Mastery.values()) if (a.rank(m) < m.maxRank && a.skillPoints >= m.cost(a.rank(m))) yield true;
                yield false;
            }
            case "bramble" -> !a.has("met.bramble");
            case "fenn" -> !a.has("quest.dens");
            case "hermit" -> !a.has("met.hermit");
            default -> false;
        };
    }

    /** E next to someone. Trainers and merchants say hello the first time, then just open up shop. */
    static void talk(World w, Level.Npc npc) {
        Adventure a = w.adventure;
        Dialogue d = w.dialogue;
        String n = npc.name(), p = npc.portrait();
        switch (npc.id()) {
            case "rowan" -> {
                if (!a.has("met.rowan")) {
                    a.set("met.rowan");
                    a.set("quest.hollow");
                    d.say(n, p, Snd.TOWN_TALK, "Welcome to Mossbrook. I wish you'd come in kinder times.");
                    d.say(n, p, Snd.TOWN_TALK, "Three nights after the stars fell, a sickness crept out of the Hollow, north of here. Thorns that walk. Flowers that bite.");
                    d.say(n, p, Snd.TOWN_TALK, "We call it the Blight. It grows from nests, and the nests are guarded. Our scouts don't come back.");
                    d.say(n, p, Snd.TOWN_TALK, "You carry yourself like someone who can fight. Will you go into the Hollow and tear the nests out?");
                    d.say("YOU", HERO, Snd.TOWN_TALK, "Point me at it.");
                    d.say(n, p, Snd.TOWN_TALK, "Past the Old Shrine, to the north. But see Ranger Ash and Bramble before you go. And the forest hides old caches - take what you find.", () -> {
                        w.notice = "NEW QUEST: THE BLIGHTED HOLLOW";
                        w.noticeHint = "North of the camp, past the Old Shrine.";
                        w.noticeTimer = 5;
                        w.sound(Snd.GUIDE_APPEAR);
                    });
                } else if (a.clears(Challenge.HOLLOW) > 0 && !a.has("gift.hollow")) {
                    giveGift(w, n, p);
                } else if (a.has("gift.hollow")) {
                    d.say(n, p, Snd.TOWN_TALK, "Rest while you can, Spellblade. When the south road opens, the city will need you as much as we did.");
                } else {
                    d.say(n, p, Snd.TOWN_TALK, "The Hollow lies north, past the Old Shrine. Destroy every nest and whatever guards them.");
                    d.say(n, p, Snd.TOWN_TALK, "If it's too much, there's no shame in coming back stronger. Ash can help with that.");
                }
            }
            case "ash" -> {
                if (!a.has("met.ash")) {
                    a.set("met.ash");
                    d.say(n, p, Snd.TOWN_TALK, "Name's Ash. I train the camp's scouts - what's left of them.");
                    d.say(n, p, Snd.TOWN_TALK, "Every fight teaches you something. Bring me skill points and I'll make the lessons stick, for good. Talk to me again to train.");
                } else {
                    w.openTrainer();
                }
            }
            case "bramble" -> {
                if (!a.has("met.bramble")) {
                    a.set("met.bramble");
                    d.say(n, p, Snd.TOWN_TALK, "Bramble's Wares! Gold for goods, goods for gold, no questions asked.");
                    d.say(n, p, Snd.TOWN_TALK, "I get new stock every time the forest gets a little safer. Funny how that works. Talk to me again to see the table.");
                } else {
                    w.openShop();
                }
            }
            case "wren" -> {
                if (a.has("gift.hollow")) d.say(n, p, Snd.TOWN_TALK, "You did it! When I grow up I'm going to be a Spellblade too. Or a fox. Haven't decided.");
                else if (!a.has("met.rowan")) d.say(n, p, Snd.TOWN_TALK, "Are you a real knight? Bramble says knights are just people with expensive shoes.");
                else if (!a.opened.contains("mushroom")) d.say(n, p, Snd.TOWN_TALK, "I hid in the mushroom ring once. There's a chest right in the middle! West of the shrine. I didn't open it. Probably.");
                else d.say(n, p, Snd.TOWN_TALK, "Grandpa Rowan says the stars that fell were wishes. I think they were angry wishes.");
            }
            case "hermit" -> {
                if (a.clears(Challenge.HOLLOW) > 0) {
                    d.say(n, p, Snd.TOWN_TALK, "So the heart in the Hollow is still. Good. But it was only a seedling.");
                    d.say(n, p, Snd.TOWN_TALK, "Something far from here is still planting them. Follow the roots, when you can.");
                } else {
                    a.set("met.hermit");
                    d.say(n, p, Snd.TOWN_TALK, "The stars fell three nights before the Blight. Most burned out in the sky.");
                    d.say(n, p, Snd.TOWN_TALK, "One did not. It is still down there in the Hollow, beating like a heart. Its nests are its roots.");
                }
            }
            case "fenn" -> {
                if (a.clears(Challenge.FOX_DENS) > 0) {
                    d.say(n, p, Snd.TOWN_TALK, "Nice work out there. They'll dig new dens - they always do. Come by whenever; the pay's still good, and the dens get nastier.");
                } else {
                    a.set("quest.dens");
                    d.say(n, p, Snd.TOWN_TALK, "Careful, stranger. The Blight got into the foxes. Three dens of them, just past this clearing.");
                    d.say(n, p, Snd.TOWN_TALK, "Tear the dens down and the camp eats tonight - I'll make it worth your while. The way in's right there, through the light.");
                }
            }
            default -> d.say(n, p, Snd.TOWN_TALK, "...");
        }
    }

    /** The residents' thanks for the Hollow: a charm they made, skill points, gold, and a hint of what's next. */
    private static void giveGift(World w, String n, String p) {
        Adventure a = w.adventure;
        Dialogue d = w.dialogue;
        a.set("gift.hollow");
        Item roll = Item.roll(new Random(77), Item.Slot.RING, Item.Rarity.EPIC, 1);
        Item charm = new Item(roll.slot, roll.rarity, "Mossbrook Charm", roll.tier, roll.stats, null, 0);
        d.say(n, p, Snd.TOWN_TALK, "The Hollow is quiet. For the first time since the stars fell, the birds are singing in it.");
        d.say(n, p, Snd.TOWN_TALK, "The whole camp made something for you. Wear it, and remember there's a place in Mossbrook for you.", () -> {
            w.profile.add(List.of(charm));
            w.profile.gold += 200;
            a.skillPoints += 3;
            w.notice = "GIFT: " + charm.name.toUpperCase();
            w.noticeHint = "Epic ring" + MenuStyle.DOT + "+200 gold" + MenuStyle.DOT + "+3 skill points";
            w.noticeTimer = 6;
            w.sound(Snd.GAME_CLEARED);
        });
        d.say(n, p, Snd.TOWN_TALK, "But listen. The traders who came up the south road last week said the city has sickened too. Lights that flicker. Streets nobody walks at night.");
        d.say(n, p, Snd.TOWN_TALK, "The Blight didn't start here, Spellblade. When the road opens, I think you're meant to follow it.", () -> {
            w.notice = "CHAPTER 1 COMPLETE";
            w.noticeHint = "The Whispering Forest is safe. The story continues soon.";
            w.noticeTimer = 7;
        });
    }

    /** A line called out when you come back from a fight. */
    static void afterFight(World w, Challenge c, boolean won) {
        if (!won) {
            w.dialogue.shout("YOU", "", HERO, Snd.TOWN_TALK, "Too many of them. Next time I'll be ready.");
        } else if (c == Challenge.HOLLOW && !w.adventure.has("gift.hollow")) {
            w.dialogue.shout("YOU", "", HERO, Snd.TOWN_TALK, "The Hollow is clear. Rowan will want to hear.");
        } else if (c == Challenge.FOX_DENS && w.adventure.clears(c) == 1) {
            w.dialogue.shout("HUNTER FENN", "(from across the clearing)", "town.merchant.idle", Snd.TOWN_TALK, "Ha! Smoke on the hill - the dens are down! The camp eats tonight!");
        }
    }

    // ------------------------------------------------------------------ chests

    /** What a chest holds. */
    record Find(int gold, Item.Rarity rarity, int skillPoints, String note) {}

    static Find treasure(String id) {
        return switch (id) {
            case "path" -> new Find(50, null, 0, "A scout's purse, dropped on the path.");
            case "mushroom" -> new Find(20, Item.Rarity.UNCOMMON, 0, "Hidden in the middle of the mushroom ring.");
            case "thicket" -> new Find(0, Item.Rarity.RARE, 0, "Wedged between the roots, far from any path.");
            case "river" -> new Find(80, null, 0, "Washed up and wedged in the reeds.");
            case "seed" -> new Find(0, null, 1, "A glowing seed of the old forest. Ash will know what it's worth.");
            case "shrine" -> new Find(150, Item.Rarity.UNCOMMON, 0, "An offering left at the shrine, long ago.");
            default -> new Find(10, null, 0, "");
        };
    }
}
