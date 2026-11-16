package dev.emerald.funnel;

/**
 * Pure, testable local-space flight formations for 11 Aerial bits.
 * X is player's right, Y is up, Z is in front of the player.
 * All positions are world-block units, independent of display model scale.
 */
final class AerialFlightMath {
    static final int BITS = 11;
    private AerialFlightMath() {}

    record Offset(double side, double height, double forward) {}

    static double progress(long now, long start, int duration) {
        if (duration <= 0) return 1;
        double t = Math.max(0, Math.min(1, (now - start) / (double)duration));
        return t * t * (3 - 2 * t);
    }

    static Offset mix(Offset start, Offset end, double amount) {
        double a = Math.max(0, Math.min(1, amount));
        return new Offset(start.side() + (end.side() - start.side()) * a,
                start.height() + (end.height() - start.height()) * a,
                start.forward() + (end.forward() - start.forward()) * a);
    }

    static Offset free(long tick, int bit) {
        check(bit);
        // Non-uniform precession, unique phases, three different frequencies:
        // the bits wander rather than staying in a symmetric fixed formation.
        double t = tick * .045 + bit * 2.399963229728653;
        double phase = t + .34 * Math.sin(t * .47 + bit * 1.17);
        double radius = 1.55 + .33 * Math.sin(t * .61 + bit * 2.29);
        return new Offset(
                Math.cos(phase) * radius,
                1.85 + .52 * Math.sin(t * 1.23 + bit * .77) + .15 * Math.cos(t * .41),
                Math.sin(phase) * radius);
    }

    static Offset shield(int bit) {
        check(bit);
        // Eleven unique mounting locations in a compact 4x3 Escutcheon shell.
        int row = bit / 4;
        int column = bit % 4;
        return new Offset((column - 1.5) * .53 + (row == 1 ? .12 : 0),
                1.53 + (row - 1) * .60, 1.17);
    }

    static Offset rifle(int bit) {
        check(bit);
        // Narrow converging multi-layer weapon dock at the aiming shoulder.
        int row = bit / 4;
        int column = bit % 4;
        return new Offset(.48 + (row - 1) * .10,
                1.55 + (column - 1.5) * .18, .86 + column * .22);
    }

    static double horizontalYaw(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    static double pitch(double dx, double dy, double dz) {
        return -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    }

    private static void check(int bit) {
        if (bit < 0 || bit >= BITS) throw new IllegalArgumentException("Bit index: " + bit);
    }
}
