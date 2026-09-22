package game;

import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The Kingdom Hearts style command menu. Up / Down move between Attack, Magic and Items (Items isn't available yet,
 * so the cursor skips it). ENTER on Attack attacks: one press, one attack (holding it does nothing). On Magic, ENTER or RIGHT opens the spell list; there UP / DOWN pick a
 * spell, ENTER casts it and LEFT goes back. After a successful cast the cursor drops back to Attack.
 *
 * Shortcut for magic builds: HOLD SHIFT to open the spell list straight away, wherever the cursor is. While Shift is
 * held the list stays open after a cast (so you can keep casting); letting go closes it and puts the cursor back where
 * it was.
 */
final class CommandMenu {
    enum Item {
        ATTACK("Attack", true),
        MAGIC("Magic", true),
        ITEMS("Items", false);       // "coming soon"

        final String label;
        final boolean enabled;

        Item(String label, boolean enabled) {
            this.label = label;
            this.enabled = enabled;
        }
    }

    /** What the player should do this frame. */
    record Result(boolean attackPressed, Ability cast) {
        static final Result NOTHING = new Result(false, null);
    }

    Item cursor = Item.ATTACK;
    boolean magicOpen;
    int spellCursor;
    /** True while the list is open because SHIFT is being held (rather than because you navigated to it). */
    boolean shiftOpened;
    private Item cursorBeforeShift = Item.ATTACK;
    private Ability lastSpell;

    Result update(Input in, List<Ability> spells) {
        boolean confirm = in.pressed(KeyEvent.VK_ENTER);
        Ability cast = null;
        boolean attackPressed = false;

        // Holding SHIFT opens the spell list from anywhere and keeps it open; letting go puts things back.
        boolean shift = in.down(KeyEvent.VK_SHIFT) && !spells.isEmpty();
        if (shift && !magicOpen) {
            cursorBeforeShift = cursor;
            cursor = Item.MAGIC;
            magicOpen = true;
            shiftOpened = true;
            spellCursor = Math.max(0, spells.indexOf(lastSpell));
        } else if (!shift && shiftOpened) {
            magicOpen = false;
            cursor = cursorBeforeShift;
            shiftOpened = false;
        }

        if (magicOpen) {
            if (spells.isEmpty()) {
                magicOpen = false;
            } else {
                spellCursor = Math.min(spellCursor, spells.size() - 1);
                if (in.pressed(KeyEvent.VK_DOWN)) spellCursor = (spellCursor + 1) % spells.size();
                if (in.pressed(KeyEvent.VK_UP)) spellCursor = (spellCursor + spells.size() - 1) % spells.size();
                if (!shift && (in.pressed(KeyEvent.VK_LEFT) || in.pressed(KeyEvent.VK_BACK_SPACE))) magicOpen = false;
                else if (confirm) cast = spells.get(spellCursor);
            }
        } else {
            if (in.pressed(KeyEvent.VK_DOWN)) cursor = step(1);
            if (in.pressed(KeyEvent.VK_UP)) cursor = step(-1);
            if (cursor == Item.MAGIC && !spells.isEmpty() && (confirm || in.pressed(KeyEvent.VK_RIGHT))) {
                magicOpen = true;
                spellCursor = Math.max(0, spells.indexOf(lastSpell));   // starts on the spell you used last
            } else if (cursor == Item.ATTACK && confirm) {
                attackPressed = true;             // one press = one attack; holding ENTER does nothing more
            }
        }

        return (attackPressed || cast != null) ? new Result(attackPressed, cast) : Result.NOTHING;
    }

    /**
     * The player tried the spell from the last {@link Result}. A successful cast closes the menu and drops the cursor
     * back to Attack, unless SHIFT is holding the list open, in which case it stays put for the next cast.
     */
    void castResult(Ability spell, boolean success) {
        if (!success) return;
        lastSpell = spell;
        if (shiftOpened) return;
        magicOpen = false;
        cursor = Item.ATTACK;
    }

    /** The spell under the cursor in the open Magic list, or null. */
    Ability selected(List<Ability> spells) {
        return magicOpen && !spells.isEmpty() ? spells.get(Math.min(spellCursor, spells.size() - 1)) : null;
    }

    /** Next enabled item in the given direction, wrapping around. */
    private Item step(int dir) {
        Item[] items = Item.values();
        int i = cursor.ordinal();
        for (int k = 0; k < items.length; k++) {
            i = (i + dir + items.length) % items.length;
            if (items[i].enabled) return items[i];
        }
        return cursor;
    }
}
