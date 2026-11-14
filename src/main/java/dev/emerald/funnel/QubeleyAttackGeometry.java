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

    public static Position position(int index, long elapsedTicks, long seed) {
        if (index < 0 || index >= 12)
            throw new IllegalArgumentException("Expected 12 bits");
        double t = Math.max(0, elapsedTicks);
        int ring = index / 6, slot = index % 6;
        double p1 = phase(seed, index, 0);
        double p2 = phase(seed, index, 1);
        double p3 = phase(seed, index, 2);
        // These individual harmonic movements create continuously shifting
        // pockets rather than twelve identical clockwork orbits.
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
        // FIXED ItemDisplay's rendered muzzle was facing backwards with the
        // previous extra +180 degrees. Do not apply that half-turn.
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new Aim(yaw, pitch);
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
