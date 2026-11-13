package dev.emerald.funnel;

/** Pure aiming geometry for the U-shaped ν fin-funnel's local -X emitter. */
public final class FinFunnelGunAim {
    private FinFunnelGunAim() {}

    public record Rotation(double axisX, double axisY, double axisZ, double radians) {}

    /**
     * Rotate the local -X gun axis into the target ray in model-local space.
     * Local vector is calculated by undoing the ItemDisplay's entity yaw.
     */
    public static Rotation toward(double dx, double dy, double dz, double displayYawDegrees) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(len) || len < 1.e-9 || !Double.isFinite(displayYawDegrees))
            return new Rotation(0, 1, 0, 0);
        double yaw = Math.toRadians(displayYawDegrees);
        double localX = (Math.cos(yaw) * dx + Math.sin(yaw) * dz) / len;
        double localY = dy / len;
        double localZ = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / len;
        // (-X) CROSS (x,y,z) = (0,z,-y).
        double cross = Math.hypot(localY, localZ);
        if (cross < 1.e-8)
            return localX < 0
                    ? new Rotation(0, 1, 0, 0)
                    : new Rotation(0, 1, 0, Math.PI);
        return new Rotation(0, localZ / cross, -localY / cross,
                Math.acos(Math.max(-1, Math.min(1, -localX))));
    }
}
