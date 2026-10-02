package game;

import java.util.List;
import java.util.Random;

/**
 * The words: what everyone in the world says (it changes as the story moves on), what each chest holds, and the line
 * at the top of the screen saying what to do next. Talking can move the story on too (a quest accepted, a gift given).
 *
 * <p>Chapter 1, the Whispering Forest: the stars fell, and three nights later the Blight crept out of the Hollow north
 * of Mossbrook. Elder Rowan asks the wandering Spellblade to burn it out.
 *
 * <p>Chapter 2, Lumen, the City of Lamps: one of the stars came down through the roof of the Dynamo, the engine every
 * lamp in the city burns on. The lamps flicker, the city's machines have turned, and the Warden (the iron guard built
 * to watch the Dynamo) has sealed its tower. Captain Vell of the Watch sends the Spellblade to Tinker Juno, who needs
 * the substation's power back before the tower's door will open. In the Warden's core, Juno finds new orders written
 * there by Doctor Morrow, who once ran the Dynamo and left for a laboratory on the cliffs: someone is gathering the
 * fallen stars, and planting them.
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
        boolean inCity = a.world.equals(Worlds.CITY);
        if (!a.has("arrived.city")) return "Follow the south road from the Riverbank to the city";
        String goal;
        if (!a.has("met.vell")) goal = "Find the captain of the Watch in Lantern Square";
        else if (!a.has("quest.substation")) goal = "Find Tinker Juno in the yard south of Lantern Square";
        else if (a.clears(Challenge.SUBSTATION) == 0) goal = "Restore the power: the substation, from the Tinker's Yard";
        else if (!a.has("quest.tower")) goal = "Report to Captain Vell in Lantern Square";
        else if (a.clears(Challenge.TOWER) == 0) goal = "Climb the Dynamo Tower: south of the yard, up the Dynamo Steps";
        else if (!a.has("gift.tower")) goal = "Return to Captain Vell in Lantern Square";
        else if (!a.has("juno.core")) goal = "Ask Tinker Juno what she found in the Warden";
        else return "Chapter 2 complete. The story continues soon";
        return inCity ? goal : "Return to Lumen by the south road. " + goal;
    }

    /** Whether the road to a world is open yet. */
    static boolean worldOpen(Adventure a, String world) {
        return switch (world) {
            case Worlds.FOREST -> true;
            case Worlds.CITY -> a.has("gift.hollow");
            default -> false;
        };
    }

    /** What you think, looking down a road that isn't open yet. */
    static String roadShut(String world) {
        return world.equals(Worlds.CITY)
            ? "The road south, out of the forest. Not yet - not with the Hollow still festering behind me."
            : "The road runs on, but I've no reason to follow it yet.";
    }

    /** What you think at a challenge's gate before anyone has asked you in. */
    static String gateShut(Challenge c) {
        return switch (c) {
            case HOLLOW -> "Thorns as thick as my arm, knotted across the way, and something beating behind them. Someone in the camp must know what this is.";
            case FOX_DENS -> "Fresh tracks, and a sour smell on the wind. Whoever hunts around here would know what's going on.";
            case ALLEYS -> "A rusted grate, and squeaking behind it. A lot of squeaking. Somebody round the canal must know about this.";
            case SUBSTATION -> "A heavy door with a lightning bolt painted on it, humming and locked. Whoever runs this yard would know how to open it.";
            case TOWER -> "The tower's great door is dark and sealed. There's no power in it at all.";
        };
    }

    /** Arriving in a world down its road: the first time in the city, a word as you walk in. */
    static void arrive(World w) {
        Adventure a = w.adventure;
        if (!a.world.equals(Worlds.CITY) || a.has("arrived.city")) return;
        a.set("arrived.city");
        Dialogue d = w.dialogue;
        d.say("YOU", HERO, Snd.TOWN_TALK, "So this is Lumen. Half the lamps are out, and the other half can't make up their minds.");
        d.say("CAPTAIN VELL", "city.vell.idle", Snd.TOWN_TALK, "You there, with the sword! Into the square, quickly - and keep out of the dark bits.");
    }

    /** True if someone has something new to say or offer (a "!" over their head). */
    static boolean hasNews(Adventure a, String id) {
        return switch (id) {
            case "rowan" -> !a.has("met.rowan") || a.clears(Challenge.HOLLOW) > 0 && !a.has("gift.hollow");
            case "ash" -> !a.has("met.ash") || canTrain(a);
            case "bramble" -> !a.has("met.bramble");
            case "fenn" -> !a.has("quest.dens");
            case "hermit" -> !a.has("met.hermit") || a.clears(Challenge.HOLLOW) > 0 && !a.has("hermit.heart");
            case "vell" -> !a.has("met.vell") || a.clears(Challenge.SUBSTATION) > 0 && !a.has("quest.tower") || a.clears(Challenge.TOWER) > 0 && !a.has("gift.tower");
            case "juno" -> !a.has("quest.substation") || a.has("gift.tower") && !a.has("juno.core");
            case "sable" -> !a.has("met.sable") || canTrain(a);
            case "nix" -> !a.has("met.nix");
            case "gus" -> !a.has("quest.alleys");
            default -> false;
        };
    }

    /** True if you have the skill points for another rank of something. */
    private static boolean canTrain(Adventure a) {
        for (Mastery m : Mastery.values()) if (a.rank(m) < m.maxRank && a.skillPoints >= m.cost(a.rank(m))) return true;
        return false;
    }

    /**
     * E next to someone. Trainers and merchants say hello the first time, then just open up shop. True if this was an
     * important conversation (the story moves on, or a quest is given): those get the screen faded to black and the
     * music hushed around them. Small talk, reminders and greetings don't.
     */
    static boolean talk(World w, Level.Npc npc) {
        Adventure a = w.adventure;
        Dialogue d = w.dialogue;
        String n = npc.name(), p = npc.portrait();
        boolean important = false;
        switch (npc.id()) {
            case "rowan" -> {
                if (!a.has("met.rowan")) {
                    important = true;
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
                    important = true;
                    giveGift(w, n, p);
                } else if (a.has("gift.tower")) {
                    d.say(n, p, Snd.TOWN_TALK, "News reaches even Mossbrook. They say every lamp in Lumen is lit again. That was you, wasn't it?");
                } else if (a.has("gift.hollow")) {
                    d.say(n, p, Snd.TOWN_TALK, "The south road starts at the Riverbank. The city will need you as much as we did.");
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
                if (a.has("arrived.city")) d.say(n, p, Snd.TOWN_TALK, "You went to the city! Is it true the bins bite? Bramble says that's just city bins.");
                else if (a.has("gift.hollow")) d.say(n, p, Snd.TOWN_TALK, "You did it! When I grow up I'm going to be a Spellblade too. Or a fox. Haven't decided.");
                else if (!a.has("met.rowan")) d.say(n, p, Snd.TOWN_TALK, "Are you a real knight? Bramble says knights are just people with expensive shoes.");
                else if (!a.opened.contains("mushroom")) d.say(n, p, Snd.TOWN_TALK, "I hid in the mushroom ring once. There's a chest right in the middle! West of the shrine. I didn't open it. Probably.");
                else d.say(n, p, Snd.TOWN_TALK, "Grandpa Rowan says the stars that fell were wishes. I think they were angry wishes.");
            }
            case "hermit" -> {
                important = hasNews(a, "hermit");                     // the first time each part of the story is told
                a.set("met.hermit");
                if (a.clears(Challenge.HOLLOW) > 0) {
                    a.set("hermit.heart");
                    d.say(n, p, Snd.TOWN_TALK, "So the heart in the Hollow is still. Good. But it was only a seedling.");
                    d.say(n, p, Snd.TOWN_TALK, "Something far from here is still planting them. Follow the roots, when you can.");
                } else {
                    d.say(n, p, Snd.TOWN_TALK, "The stars fell three nights before the Blight. Most burned out in the sky.");
                    d.say(n, p, Snd.TOWN_TALK, "One did not. It is still down there in the Hollow, beating like a heart. Its nests are its roots.");
                }
            }
            case "fenn" -> {
                if (a.clears(Challenge.FOX_DENS) > 0) {
                    d.say(n, p, Snd.TOWN_TALK, "Nice work out there. They'll dig new dens - they always do. Come by whenever; the pay's still good, and the dens get nastier.");
                } else {
                    important = !a.has("quest.dens");                 // the first time he asks
                    a.set("quest.dens");
                    d.say(n, p, Snd.TOWN_TALK, "Careful, stranger. The Blight got into the foxes. Three dens of them, just past this clearing.");
                    d.say(n, p, Snd.TOWN_TALK, "Tear the dens down and the camp eats tonight - I'll make it worth your while. The way in's right there, through the light.");
                }
            }
            default -> important = talkCity(w, npc);
        }
        return important;
    }

    /** The people of Lumen (see {@link #talk}). */
    private static boolean talkCity(World w, Level.Npc npc) {
        Adventure a = w.adventure;
        Dialogue d = w.dialogue;
        String n = npc.name(), p = npc.portrait();
        boolean important = false;
        switch (npc.id()) {
            case "vell" -> {
                if (!a.has("met.vell")) {
                    important = true;
                    a.set("met.vell");
                    d.say(n, p, Snd.TOWN_TALK, "Captain Vell, of the Lumen Watch - what's left of it. You came up the forest road? Then it's true: Mossbrook's Hollow is quiet.");
                    d.say(n, p, Snd.TOWN_TALK, "Lumen never sleeps. Every lamp in the city burns on the Dynamo, the great engine under the tower on the south side.");
                    d.say(n, p, Snd.TOWN_TALK, "The night the stars fell, one came down through the Dynamo's roof. Since then the lamps flicker, the drones that sweep the streets shoot at people, and the bins... bite.");
                    d.say(n, p, Snd.TOWN_TALK, "And the Warden - the iron guard we built to watch the Dynamo - has sealed the tower. It won't let anyone near. Not even me.");
                    d.say("YOU", HERO, Snd.TOWN_TALK, "Thorns in the forest, iron in the city. It's the same Blight.");
                    d.say(n, p, Snd.TOWN_TALK, "Then you know more than my whole watch. Find Tinker Juno, in the yard south of here. She built half the Dynamo. If anyone can get you into that tower, it's her.", () -> {
                        w.notice = "NEW LEAD: TINKER JUNO";
                        w.noticeHint = "Her yard is south of Lantern Square.";
                        w.noticeTimer = 5;
                        w.sound(Snd.GUIDE_APPEAR);
                    });
                } else if (a.clears(Challenge.SUBSTATION) > 0 && !a.has("quest.tower")) {
                    important = true;
                    a.set("quest.tower");
                    d.say(n, p, Snd.TOWN_TALK, "You did it! The whole square lit up at once. Lumen hasn't been this bright since the stars fell.");
                    d.say(n, p, Snd.TOWN_TALK, "And Juno sends word: the tower door has power again. It'll open for you.");
                    d.say(n, p, Snd.TOWN_TALK, "The Blight has grown all the way up to the Dynamo, and the Warden will be waiting. Tear the nests out, then put the Warden down. It used to be our friend. I'm sorry it has to be you.", () -> {
                        w.notice = "NEW QUEST: THE DYNAMO TOWER";
                        w.noticeHint = "South of the Tinker's Yard, up the Dynamo Steps.";
                        w.noticeTimer = 5;
                        w.sound(Snd.GUIDE_APPEAR);
                    });
                } else if (a.clears(Challenge.TOWER) > 0 && !a.has("gift.tower")) {
                    important = true;
                    giveCityGift(w, n, p);
                } else if (a.has("gift.tower")) {
                    d.say(n, p, Snd.TOWN_TALK, "Every lamp in Lumen is lit, and my watch can walk the streets again. Whatever you need here, it's yours.");
                } else if (a.has("quest.tower")) {
                    d.say(n, p, Snd.TOWN_TALK, "The Dynamo Steps are south of Juno's yard. Lumen's counting on you, Spellblade.");
                } else {
                    d.say(n, p, Snd.TOWN_TALK, "Juno's yard is south of the square. Ask her about the tower.");
                    d.say(n, p, Snd.TOWN_TALK, "And see Sable and Nix while you're here. A sharp blade and good boots won't hurt.");
                }
            }
            case "juno" -> {
                if (!a.has("quest.substation")) {
                    important = true;
                    a.set("met.juno");
                    a.set("quest.substation");
                    d.say(n, p, Snd.TOWN_TALK, "Mind the cables! ...Oh. You're not a drone. Good. I'm Juno: tinker, engineer, and the last person in Lumen who understands the Dynamo.");
                    d.say(n, p, Snd.TOWN_TALK, "You want into the tower? Its door runs on city power, and the city's power runs through the substation next door. Three relays, and the Blight has wrapped itself round every one.");
                    d.say(n, p, Snd.TOWN_TALK, "Here's the trick. Throw a relay and it takes a while to power up. Stand in its circle and keep the Blight off until it's done. They'll come for you. They hate the light.");
                    d.say("YOU", HERO, Snd.TOWN_TALK, "Three relays. Then the door?");
                    d.say(n, p, Snd.TOWN_TALK, "Then the door. The way into the substation is right there, by the pylon.", () -> {
                        w.notice = "NEW QUEST: THE SUBSTATION";
                        w.noticeHint = "The gate in the Tinker's Yard.";
                        w.noticeTimer = 5;
                        w.sound(Snd.GUIDE_APPEAR);
                    });
                } else if (a.has("gift.tower") && !a.has("juno.core")) {
                    important = true;
                    a.set("juno.core");
                    d.say(n, p, Snd.TOWN_TALK, "Look at this. The Warden's core. Somebody opened it up and wrote new orders into it. See, scratched right here: GUARD THE GARDEN.");
                    d.say(n, p, Snd.TOWN_TALK, "That's not the Watch's hand. Those are the marks of Doctor Morrow, who ran the Dynamo before me. Brilliant. Strange. He left Lumen years ago, for a laboratory up on the cliffs.");
                    d.say("YOU", HERO, Snd.TOWN_TALK, "The hermit in the forest said something was planting the stars. Like seeds.");
                    d.say(n, p, Snd.TOWN_TALK, "Then the stars didn't just fall, Spellblade. Somebody is gathering them and planting them. And I think I know who the gardener is.", () -> {
                        w.notice = "CHAPTER 2 COMPLETE";
                        w.noticeHint = "Lumen is bright again. The story continues soon.";
                        w.noticeTimer = 7;
                        w.sound(Snd.GAME_CLEARED);
                    });
                } else if (a.has("juno.core")) {
                    d.say(n, p, Snd.TOWN_TALK, "If you go after Morrow, take a lamp. And come back. Lumen still needs someone who can fight its bins.");
                } else if (a.clears(Challenge.SUBSTATION) > 0 && a.clears(Challenge.TOWER) == 0) {
                    d.say(n, p, Snd.TOWN_TALK, "Hear that hum? That's my city breathing again. Go and see the Captain. The tower's yours.");
                } else if (a.clears(Challenge.TOWER) > 0) {
                    d.say(n, p, Snd.TOWN_TALK, "The Warden's in pieces on my bench. Go and see the Captain first, then come back. There's something you should see.");
                } else {
                    d.say(n, p, Snd.TOWN_TALK, "Three relays. Throw each one, then stand in its circle while it powers up. Don't let them push you out.");
                }
            }
            case "sable" -> {
                if (!a.has("met.sable")) {
                    a.set("met.sable");
                    d.say(n, p, Snd.TOWN_TALK, "Sable. I taught the Watch to fence, back when the Watch could afford me.");
                    d.say(n, p, Snd.TOWN_TALK, "Ash of Mossbrook sent word you'd come. A ranger's lessons travel, and so do mine. Bring me skill points and I'll make them count. Talk to me again to train.");
                } else {
                    w.openTrainer();
                }
            }
            case "nix" -> {
                if (!a.has("met.nix")) {
                    a.set("met.nix");
                    d.say(n, p, Snd.TOWN_TALK, "Nix's Night Market! Whatever you need, I've got it, or I know a rat who has.");
                    d.say(n, p, Snd.TOWN_TALK, "Bramble and I share a supplier, so you'll know the prices. New stock whenever the streets get a little safer. Talk to me again to browse.");
                } else {
                    w.openShop();
                }
            }
            case "pip" -> {
                if (a.has("gift.tower")) d.say(n, p, Snd.TOWN_TALK, "EXTRA! EXTRA! SPELLBLADE SAVES LUMEN! ...I wrote that one myself. Want a copy? It's free. Everything's free today.");
                else if (!a.has("met.vell")) d.say(n, p, Snd.TOWN_TALK, "Extra, extra! Lamps go out, city in a panic! ...Want a paper? It's free. Nobody's buying.");
                else if (!a.opened.contains("city.clock")) d.say(n, p, Snd.TOWN_TALK, "There's a chest up in Clocktower Court, north of the market. The clock stopped the night the stars fell. Creepy, right?");
                else if (a.clears(Challenge.SUBSTATION) == 0) d.say(n, p, Snd.TOWN_TALK, "Ma says the drones used to sweep the streets. Now they shoot at pigeons. And at me.");
                else d.say(n, p, Snd.TOWN_TALK, "Old Gus says the rats in the alleys have gone funny. Funny like big.");
            }
            case "gus" -> {
                if (a.clears(Challenge.ALLEYS) > 0) {
                    d.say(n, p, Snd.TOWN_TALK, "The alleys are quiet. Quiet as rats get, anyway. They'll nest again, they always do. Pay's still good.");
                } else if (!a.has("quest.alleys")) {
                    important = true;
                    a.set("quest.alleys");
                    d.say(n, p, Snd.TOWN_TALK, "Forty years catching rats in Lumen, and I've never seen 'em like this. Big as dogs, eyes like coals.");
                    d.say(n, p, Snd.TOWN_TALK, "Something's grown up through the cobbles in the back alleys. Three of 'em, and the rats pour out of 'em. Burn 'em out and I'll pay you proper. Way in's just south of here.", () -> {
                        w.notice = "NEW QUEST: THE BACK ALLEYS";
                        w.noticeHint = "The grate at the Alley Mouth, south of the canal.";
                        w.noticeTimer = 5;
                        w.sound(Snd.GUIDE_APPEAR);
                    });
                } else {
                    d.say(n, p, Snd.TOWN_TALK, "Three nests, back of the Alley Mouth. Mind your ankles.");
                }
            }
            default -> d.say(n, p, Snd.TOWN_TALK, "...");
        }
        return important;
    }

    /** Lumen's thanks for the tower: a saber the Watch had made, skill points, gold, and a word that Juno found something. */
    private static void giveCityGift(World w, String n, String p) {
        Adventure a = w.adventure;
        Dialogue d = w.dialogue;
        a.set("gift.tower");
        Item roll = Item.roll(new Random(78), Item.Slot.WEAPON, Item.Rarity.EPIC, 2);
        Item saber = new Item(roll.slot, roll.rarity, "Lumen Saber", roll.tier, roll.stats, null, 0);
        d.say(n, p, Snd.TOWN_TALK, "The Dynamo's humming like it used to, and every lamp in Lumen is lit. The Warden is... still. Juno's looking at what's left of it.");
        d.say(n, p, Snd.TOWN_TALK, "The Watch had this made for whoever would save the city. We'd stopped hoping anyone would. It's yours.", () -> {
            w.profile.add(List.of(saber));
            w.profile.gold += 300;
            a.skillPoints += 4;
            w.notice = "GIFT: " + saber.name.toUpperCase();
            w.noticeHint = "Epic weapon" + MenuStyle.DOT + "+300 gold" + MenuStyle.DOT + "+4 skill points";
            w.noticeTimer = 6;
            w.sound(Snd.GAME_CLEARED);
        });
        d.say(n, p, Snd.TOWN_TALK, "But Spellblade, Juno found something in the Warden's core. Go and see her. I didn't like the look on her face.");
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
        d.say(n, p, Snd.TOWN_TALK, "The Blight didn't start here, Spellblade. The south road is clear now, from the Riverbank. I think you're meant to follow it.", () -> {
            w.notice = "CHAPTER 1 COMPLETE";
            w.noticeHint = "The south road, from the Riverbank, leads to the city.";
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
        } else if (c == Challenge.ALLEYS && w.adventure.clears(c) == 1) {
            w.dialogue.shout("OLD GUS", "(from down by the canal)", "city.gus.idle", Snd.TOWN_TALK, "Ha! I can smell burnt nest from here! Good on you!");
        } else if (c == Challenge.SUBSTATION && !w.adventure.has("quest.tower")) {
            w.dialogue.shout("YOU", "", HERO, Snd.TOWN_TALK, "The lamps are steady. Captain Vell will have seen that.");
        } else if (c == Challenge.TOWER && !w.adventure.has("gift.tower")) {
            w.dialogue.shout("YOU", "", HERO, Snd.TOWN_TALK, "The Warden's down and the Dynamo's quiet. Back to the Captain.");
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
            case "city.gate" -> new Find(60, null, 0, "A traveller's purse, dropped in a hurry.");
            case "city.market" -> new Find(40, Item.Rarity.UNCOMMON, 0, "Under a stall, behind the turnips.");
            case "city.clock" -> new Find(150, Item.Rarity.RARE, 0, "Wedged in the clock's stopped gears.");
            case "city.spark" -> new Find(0, null, 1, "A spark of starlight, still warm. Sable will know what it's worth.");
            case "city.alley" -> new Find(90, null, 0, "Somebody's savings, stuffed up a drainpipe.");
            case "city.yard" -> new Find(0, Item.Rarity.UNCOMMON, 0, "A spare part from Juno's bench. Well, it was spare.");
            default -> new Find(10, null, 0, "");
        };
    }
}
