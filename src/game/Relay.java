package game;

/**
 * One of the substation's relays, in a {@link Challenge.Goal#RELAYS} fight (Risk of Rain's teleporter, three times
 * over): switched on with E, it powers up only while you stand inside its circle, and the horde comes for you the
 * whole time, in surges at every quarter of the way (though while you hold it, its light slows and scorches whatever
 * comes into the circle). Fully powered, it lights up for good and its pulse knocks the
 * monsters round it flat. {@link Run} directs all of that; this is just its state.
 */
final class Relay {
    /** The circle you have to stand in (seen from above at a slant, so it's drawn and measured squashed to {@link #FLAT}), and how long it takes to power up while you do. */
    static final double RADIUS = 250, FLAT = 0.62, CHARGE_TIME = 32;
    /** While you hold it, every this many seconds its light slows and scorches the monsters inside the circle. */
    static final double ZAP_EVERY = 0.5;

    final double x, y;
    /** Switched on (it may still be powering up). */
    boolean started;
    /** Fully powered. */
    boolean done;
    /** How far it has powered up, 0..1. */
    double charge;
    /** How many of its surges (at a quarter, a half and three quarters) have come. */
    int surges;
    /** True this frame if you're inside the circle (so it's powering up). */
    boolean held;
    /** Seconds until its light next scorches what's in the circle. */
    double zap;

    Relay(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Switched on and not yet done. */
    boolean charging() { return started && !done; }

    boolean contains(double px, double py) { return Math.hypot(px - x, (py - y) / FLAT) <= RADIUS; }
}
