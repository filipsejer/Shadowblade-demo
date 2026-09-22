package game;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * One row in the upgrade station. {@code cost} is in skill points; a maxed-out upgrade has no effect and no cost.
 * {@code status} is the short "where you are now" text (e.g. "Rank 2 / 5"), {@code description} says what buying does.
 */
record Upgrade(Category category, String title, String status, String description, int cost, Color accent,
               Consumer<Player> effect) {

    enum Category {
        COMBAT("COMBAT", new Color(255, 200, 70)),
        SPELLS("SPELLS", new Color(190, 120, 255)),
        SURVIVAL("SURVIVAL", new Color(90, 200, 190));

        final String label;
        final Color color;

        Category(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    static Upgrade maxed(Category category, String title, String status, Color accent) {
        return new Upgrade(category, title, status, "Fully upgraded.", 0, accent, null);
    }

    boolean maxed() { return effect == null; }

    void apply(Player p) { effect.accept(p); }
}
