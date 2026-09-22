package game;

import static game.PixelCanvas.*;

import java.util.Map;

/**
 * The tutorial's squirrel, painted in code like everything else: sitting (idle), running, standing up scared with its tail puffed,
 * and throwing an acorn. Every sprite faces right; the game mirrors it when the squirrel faces left. Also the acorn it throws.
 */
final class SquirrelArt {
    private SquirrelArt() {}

    private static final int FUR = rgb(206, 116, 52), FUR_L = rgb(242, 168, 88), FUR_D = rgb(146, 76, 38);
    private static final int CREAM = rgb(255, 234, 200), PINK = rgb(255, 150, 150), NOSE = rgb(70, 34, 40);
    private static final int EYE = rgb(30, 22, 34), WHITE = rgb(255, 255, 255), OUT = rgb(58, 32, 26);
    private static final int NUT = rgb(214, 158, 84), NUT_L = rgb(244, 204, 130), CAP = rgb(120, 76, 44), CAP_D = rgb(84, 52, 34);

    static void register(Map<String, Sprite[]> m) {
        m.put("squirrel.idle", PeopleArt.frames(2, SquirrelArt::sit, 12, 22));
        m.put("squirrel.run", PeopleArt.frames(4, SquirrelArt::run, 13, 17));
        m.put("squirrel.scared", PeopleArt.frames(2, SquirrelArt::scared, 12, 23));
        m.put("squirrel.throw", PeopleArt.frames(2, SquirrelArt::toss, 12, 22));
        m.put("prop.acorn", PeopleArt.frames(2, SquirrelArt::acorn, 3, 4));
    }

    /** A big bushy tail behind the body: {@code lift} raises the tip, {@code puff} makes it fatter (fright). */
    private static void tail(PixelCanvas c, int sway, double puff) {
        c.ellipse(6.5, 16, 4.6 + puff, 6.2 + puff * 0.6, FUR);
        c.ellipse(6.0 + sway, 8.5, 4.2 + puff, 6.0 + puff * 0.6, FUR);
        c.disc(8.5 + sway, 3.6, 3.4 + puff * 0.6, FUR);
        c.ellipse(7.0, 16, 2.6, 4.0, FUR_L);
        c.ellipse(6.6 + sway, 8.5, 2.2, 4.0, FUR_L);
        c.rect(3, 12, 3, 1, FUR_D); c.rect(2, 16, 3, 1, FUR_D); c.rect(3, 20, 3, 1, FUR_D);
    }

    private static void head(PixelCanvas c, int b, boolean scared, boolean mouthOpen) {
        c.ellipse(15, 9.5 + b, 5.2, 4.6, FUR);
        c.tri(11, 6 + b, 12, 1 + b, 15, 5 + b, FUR);                       // ears
        c.tri(15, 5 + b, 17, 1 + b, 19, 5 + b, FUR_D);
        c.set(12, 3 + b, PINK); c.set(17, 3 + b, PINK);
        c.ellipse(19, 11 + b, 2.7, 2.1, CREAM);                             // muzzle
        c.set(21, 10 + b, NOSE);
        if (mouthOpen) c.rect(19, 12 + b, 2, 2, rgb(150, 40, 60));
        else c.rect(19, 12 + b, 2, 1, FUR_D);
        if (scared) {                                                         // big round eyes, white all around
            c.rect(16, 7 + b, 3, 3, WHITE); c.rect(17, 8 + b, 2, 2, EYE); c.set(17, 8 + b, WHITE);
        } else {
            c.rect(17, 8 + b, 2, 2, EYE); c.set(17, 8 + b, WHITE);
        }
        c.set(15, 11 + b, PINK);                                              // blush
    }

    /** Sitting up on its haunches. */
    private static PixelCanvas sit(int f) {
        PixelCanvas c = new PixelCanvas(24, 24);
        int b = f;
        tail(c, f, 0);
        c.ellipse(12.2, 16.5, 5.6, 6.4, FUR_D);                              // a darker rim so the body reads against the tail
        c.ellipse(13, 16.5, 5.2, 6.0, FUR);
        c.ellipse(14.4, 17.5, 3.2, 4.4, CREAM);
        c.ellipse(12, 22, 3, 1.4, FUR_D); c.ellipse(17.5, 22, 2.6, 1.3, FUR_D);
        head(c, b, false, false);
        c.rect(15, 15 + b, 3, 2, FUR_D);                                     // little paws held to the chest
        c.set(16, 15 + b, CREAM);
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    /** Bounding along: long and low, tail streaming behind. */
    private static PixelCanvas run(int f) {
        PixelCanvas c = new PixelCanvas(30, 20);
        int b = f % 2 == 0 ? 0 : -1;
        c.ellipse(6, 9 + b, 6.0, 4.0, FUR); c.ellipse(4, 6 + b, 4.0, 3.0, FUR); c.disc(2, 4 + b, 2.6, FUR);
        c.ellipse(6.5, 9 + b, 3.8, 2.2, FUR_L);
        c.rect(3, 9 + b, 2, 1, FUR_D); c.rect(8, 8 + b, 2, 1, FUR_D);
        c.ellipse(16, 12 + b, 7, 4.2, FUR);                                   // body
        c.ellipse(17, 13.6 + b, 4.4, 2.0, CREAM);
        int ext = f == 0 ? 3 : f == 2 ? -3 : 0;                               // legs reach out and gather in
        c.rect(9 - ext / 2, 15 + b, 3, 2, FUR_D); c.rect(11 + ext / 2, 15 + b, 3, 2, FUR_D);
        c.rect(19 + ext / 2, 15 + b, 3, 2, FUR_D); c.rect(22 - ext / 2, 15 + b, 3, 2, FUR_D);
        c.ellipse(23, 9.5 + b, 4.6, 4.0, FUR);                                // head
        c.tri(20, 7 + b, 21, 3 + b, 24, 6 + b, FUR); c.tri(24, 6 + b, 26, 3 + b, 27, 7 + b, FUR_D);
        c.ellipse(26.6, 11 + b, 2.2, 1.7, CREAM); c.set(28, 10 + b, NOSE);
        c.rect(24, 8 + b, 2, 2, EYE); c.set(24, 8 + b, WHITE);
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    /** Up on its hind legs, paws by its cheeks, tail bristling, mouth open in a yelp. */
    private static PixelCanvas scared(int f) {
        PixelCanvas c = new PixelCanvas(28, 26);
        int b = f == 0 ? 0 : -1;
        tail(c, f, 1.4);
        c.ellipse(12.2, 17.5, 5.6, 6.6, FUR_D);
        c.ellipse(13, 17.5, 5.2, 6.2, FUR);
        c.ellipse(14.4, 18.5, 3.2, 4.4, CREAM);
        c.ellipse(12, 23, 3, 1.4, FUR_D); c.ellipse(17.5, 23, 2.6, 1.3, FUR_D);
        head(c, b, true, true);
        c.rect(14, 12 + b, 2, 3, FUR_D); c.rect(17, 13 + b, 2, 3, FUR_D);    // both paws up at its face
        c.set(14, 12 + b, CREAM); c.set(17, 13 + b, CREAM);
        for (int i = 0; i < 4; i++) c.set(1 + i * 2 + f, 1 + (i % 2), FUR_L);  // bristling tail-tip fur
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    /** Sitting up throwing an acorn: frame 0 winds the arm back over its head, frame 1 has just let go. */
    private static PixelCanvas toss(int f) {
        PixelCanvas c = new PixelCanvas(28, 24);
        tail(c, 0, 0);
        c.ellipse(12.2, 16.5, 5.6, 6.4, FUR_D);
        c.ellipse(13, 16.5, 5.2, 6.0, FUR);
        c.ellipse(14.4, 17.5, 3.2, 4.4, CREAM);
        c.ellipse(12, 22, 3, 1.4, FUR_D); c.ellipse(17.5, 22, 2.6, 1.3, FUR_D);
        head(c, 0, false, f == 1);
        if (f == 0) {
            c.thickLine(13, 15, 11, 5, 2, FUR_D);                            // arm cocked back above the head...
            c.rect(9, 2, 4, 4, CAP); c.rect(9, 4, 4, 3, NUT); c.set(10, 5, NUT_L);   // ...holding the acorn
        } else {
            c.thickLine(16, 15, 23, 13, 2, FUR_D);                           // arm swung forward
            c.set(24, 13, CREAM);
        }
        c.bevel(0.16, 0.2);
        c.outline(OUT);
        return c;
    }

    /** The acorn it throws (spun in flight by the renderer). */
    private static PixelCanvas acorn(int f) {
        PixelCanvas c = new PixelCanvas(7, 9);
        c.ellipse(3.5, 2.6, 3.2, 2.2, f == 0 ? CAP : CAP_D);
        c.ellipse(3.5, 5.6, 2.6, 2.8, NUT);
        c.set(2, 5, NUT_L); c.set(2, 4, NUT_L);
        c.set(3, 0, CAP_D); c.set(3, 8, CAP_D);
        c.set(2, 2, CAP_D); c.set(4, 1, f == 0 ? CAP_D : CAP);
        c.outline(OUT);
        return c;
    }
}
