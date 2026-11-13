package dev.emerald.funnel;

/** Stateless animation math, used both by the Paper renderer and unit tests. */
public final class ShieldAnimationGeometry {
    private ShieldAnimationGeometry() { }

    /** Relative to player: right, up from feet, forward, yaw offset in degrees. */
    public record Pose(double right, double up, double forward, double yawOffset, double rollDegrees) { }

    public static double approach(double progress, boolean opening, int durationTicks) {
        if (!Double.isFinite(progress) || durationTicks < 1)
            throw new IllegalArgumentException("Invalid shield animation state");
        return Math.max(0, Math.min(1, progress + (opening ? 1 : -1) / (double) durationTicks));
    }

    public static double ease(double progress) {
        double x = Math.max(0, Math.min(1, progress));
        return x * x * (3 - 2 * x);
    }

    /**
     * Three overlapping cherry-blossom petals: one points down and the other
     * two point upper-left/upper-right, 120 degrees apart around the model's Z axis.
     * The player-facing 3D shield wall remains in front of the player.
     */
    public static Pose pose(int index, double progress) {
        if (index < 0 || index > 2) throw new IllegalArgumentException("Shield index: " + index);
        double p = ease(progress);
        double side = index - 1;
        double homeRight = side * 0.68;
        double homeUp = index == 1 ? 1.60 : 1.48;
        double homeForward = index == 1 ? -0.95 : -0.72;
        double homeYaw = side * 55;
        // i=0 down, i=1 upper-left, i=2 upper-right.
        double wallRight = index == 0 ? 0 : index == 1 ? -0.58 : 0.58;
        double wallUp = index == 0 ? 1.08 : 1.97;
        double wallForward = ShieldGuardGeometry.PLANE_DISTANCE + (index - 1) * 0.06;
        double petalRoll = index == 0 ? 0 : index == 1 ? -120 : 120;
        return new Pose(
                mix(homeRight, wallRight, p),
                mix(homeUp, wallUp, p),
                mix(homeForward, wallForward, p),
                mix(homeYaw, 0, p),
                mix(0, petalRoll, p));
    }

    public static double shotPulse(long elapsedTicks) {
        if (elapsedTicks < 0 || elapsedTicks > 8) return 0;
        return Math.sin(Math.PI * elapsedTicks / 8.0);
    }

    private static double mix(double start, double end, double p) {
        return start + (end - start) * p;
    }
}
