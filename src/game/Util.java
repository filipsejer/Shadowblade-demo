package game;

import java.awt.Color;

final class Util {
    private Util() {}

    record Vec(double x, double y) {}

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    static double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    static double angleTo(double x1, double y1, double x2, double y2) {
        return Math.atan2(y2 - y1, x2 - x1);
    }

    /** Signed shortest difference from angle a to angle b, in (-PI, PI]. */
    static double angleDiff(double a, double b) {
        double d = (b - a) % (2 * Math.PI);
        if (d > Math.PI) d -= 2 * Math.PI;
        if (d <= -Math.PI) d += 2 * Math.PI;
        return d;
    }

    static double turnToward(double current, double target, double maxStep) {
        double d = angleDiff(current, target);
        if (Math.abs(d) <= maxStep) return target;
        return current + Math.signum(d) * maxStep;
    }

    static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) clamp(a * 255, 0, 255));
    }

    static Color mix(Color a, Color b, double t) {
        t = clamp(t, 0, 1);
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
