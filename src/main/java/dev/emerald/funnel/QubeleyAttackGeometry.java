package dev.emerald.funnel;

/** Two staggered rings of six independent Qubeley bits surrounding the chosen mob. */
public final class QubeleyAttackGeometry {
    private QubeleyAttackGeometry() {}

    public record Position(double x, double y, double z) {}

    public static Position position(int index, long ticks) {
        if (index < 0 || index >= 12) throw new IllegalArgumentException("Expected 12 bits");
        int ring = index / 6, slot = index % 6;
        double angle = ticks * (ring == 0 ? 0.077 : -0.063)
                + slot * (Math.PI / 3.0) + ring * (Math.PI / 6.0);
        double radius = ring == 0 ? 2.65 : 3.10;
        double y = (ring == 0 ? 0.64 : -0.47)
                + 0.18 * Math.sin(ticks * 0.11 + index);
        return new Position(radius * Math.cos(angle), y, radius * Math.sin(angle));
    }
}
