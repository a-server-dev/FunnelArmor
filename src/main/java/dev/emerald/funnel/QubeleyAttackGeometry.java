package dev.emerald.funnel;

/**
 * Smooth, independent evasive motion for twelve Qubeley bits around one mob.
 * The pseudo-random phases are stable per activation, so there is no jitter
 * or frame-to-frame teleportation. Position is world-relative to the mob.
 */
public final class QubeleyAttackGeometry {
    private QubeleyAttackGeometry() {}

    public record Position(double x, double y, double z) {}
    public record Aim(float yaw, float pitch) {}

    private static final int LAUNCH_TICKS = 24;

    /** First bit leaves immediately, the other eleven depart in sequence. */
    public static int departureDelay(int index) {
        validateIndex(index);
        return index;
    }

    public static boolean allArrived(long elapsedTicks) {
        return elapsedTicks >= LAUNCH_TICKS + departureDelay(11);
    }

    /**
     * Each bit begins near the wearer's shoulders/back. This offset is in
     * world XYZ axes, rotated from the player's yaw at activation time.
     */
    public static Position launchOffset(int index, double playerYawDegrees) {
        validateIndex(index);
        if (!Double.isFinite(playerYawDegrees))
            throw new IllegalArgumentException("Invalid player yaw");
        int pair = index / 2;
        double side = (index & 1) == 0 ? -1 : 1;
        double right = side * (0.30 + 0.14 * (pair % 3));
        double forward = -0.40 - 0.12 * (pair / 3);
        double up = 1.37 + 0.23 * (pair % 3) + 0.18 * (pair / 3);
        double a = Math.toRadians(playerYawDegrees);
        return new Position(Math.cos(a) * right - Math.sin(a) * forward,
                up, Math.sin(a) * right + Math.cos(a) * forward);
    }

    /**
     * Origin and destination are in world XYZ coordinates. Each bit departs
     * after its own delay and flies on a gentle curved arc for 24 ticks.
     * Position is continuous at departure and arrival, with zero start/end
     * velocity, so nothing instantly teleports to the enemy.
     */
    public static Position flightPosition(Position origin, Position destination,
                                          int index, long elapsedTicks) {
        validateIndex(index);
        if (origin == null || destination == null)
            throw new IllegalArgumentException("Missing flight endpoint");
        if (!finite(origin) || !finite(destination))
            throw new IllegalArgumentException("Nonfinite flight endpoint");
        double t = Math.max(0.0, Math.min(1.0,
                (elapsedTicks - departureDelay(index)) / (double) LAUNCH_TICKS));
        if (t <= 0.0) return origin;
        if (t >= 1.0) return destination;
        double easing = t * t * (3.0 - 2.0 * t);
        double arc = Math.sin(Math.PI * t);
        double dx = destination.x() - origin.x();
        double dz = destination.z() - origin.z();
        double horizontal = Math.hypot(dx, dz);
        double curve = arc * ((index % 4) - 1.5) * 0.22;
        double bendX = horizontal > 1e-8 ? -dz / horizontal * curve : 0;
        double bendZ = horizontal > 1e-8 ?  dx / horizontal * curve : 0;
        return new Position(
                origin.x() + (destination.x() - origin.x()) * easing + bendX,
                origin.y() + (destination.y() - origin.y()) * easing + 0.48 * arc,
                origin.z() + (destination.z() - origin.z()) * easing + bendZ);
    }

    private static boolean finite(Position p) {
        return Double.isFinite(p.x()) && Double.isFinite(p.y())
                && Double.isFinite(p.z());
    }

    public static Position position(int index, long elapsedTicks, long seed) {
        validateIndex(index);
        double t = Math.max(0, elapsedTicks);
        int ring = index / 6, slot = index % 6;
        double p1 = phase(seed, index, 0);
        double p2 = phase(seed, index, 1);
        double p3 = phase(seed, index, 2);
        double angularVelocity = (ring == 0 ? 0.046 : -0.039);
        double angle = (2 * Math.PI * slot / 6.0)
                + ring * Math.PI / 6.0
                + angularVelocity * t
                + 0.18 * Math.sin(t * (0.023 + 0.001 * (index % 3)) + p1)
                + 0.13 * Math.cos(t * 0.031 + p2);
        double radius = (ring == 0 ? 2.60 : 3.03)
                + 0.30 * Math.sin(t * (0.018 + (index % 4) * 0.002) + p2)
                + 0.19 * Math.cos(t * 0.038 + p3);
        double altitude = (ring == 0 ? 0.52 : -0.35)
                + 0.65 * Math.sin(t * (0.022 + (index % 5) * 0.002) + p3)
                + 0.27 * Math.sin(t * 0.043 + p1);
        return new Position(radius * Math.cos(angle), altitude, radius * Math.sin(angle));
    }

    /** World-direction look rotation; 0 yaw points +Z, positive pitch looks down. */
    public static Aim toward(double dx, double dy, double dz) {
        double horizontal = Math.hypot(dx, dz);
        if (!Double.isFinite(dx) || !Double.isFinite(dy)
                || !Double.isFinite(dz) || horizontal + Math.abs(dy) < 1e-8)
            throw new IllegalArgumentException("Invalid Qubeley aim vector");
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Aim(yaw, pitch);
    }

    private static void validateIndex(int index) {
        if (index < 0 || index >= 12)
            throw new IllegalArgumentException("Expected 12 bits");
    }

    private static double phase(long seed, int index, int dimension) {
        long x = seed ^ (0x9e3779b97f4a7c15L * (index + 3L * dimension + 1));
        x ^= x >>> 30;
        x *= 0xbf58476d1ce4e5b9L;
        x ^= x >>> 27;
        x *= 0x94d049bb133111ebL;
        x ^= x >>> 31;
        return 2.0 * Math.PI * ((x >>> 11) * 0x1.0p-53);
    }
}
