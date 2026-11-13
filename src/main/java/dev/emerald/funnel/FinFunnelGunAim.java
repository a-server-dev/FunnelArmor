package dev.emerald.funnel;

/** Pure aiming geometry for the U-shaped ν fin-funnel's true local +X emitter. */
public final class FinFunnelGunAim {
    private FinFunnelGunAim() {}

    public record Rotation(double axisX, double axisY, double axisZ, double radians) {}

    /**
     * Rotate the fin-funnel's actual +X muzzle axis into the target ray.
     * The deployed model was visually confirmed to face the correct direction
     * with a 180-degree display yaw adjustment: its emitter is +X, not -X.
     * Undo entity yaw before finding the local-space rotation.
     */
    public static Rotation toward(double dx, double dy, double dz, double displayYawDegrees) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(len) || len < 1.e-9 || !Double.isFinite(displayYawDegrees))
            return new Rotation(0, 1, 0, 0);
        double yaw = Math.toRadians(displayYawDegrees);
        double localX = (Math.cos(yaw) * dx + Math.sin(yaw) * dz) / len;
        double localY = dy / len;
        double localZ = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / len;
        // (+X) CROSS (x,y,z) = (0,-z,y). Reversing this axis caused
        // the 180-degree flip on every right-click with a corrected idle yaw.
        double cross = Math.hypot(localY, localZ);
        if (cross < 1.e-8)
            return localX >= 0
                    ? new Rotation(0, 1, 0, 0)
                    : new Rotation(0, 1, 0, Math.PI);
        return new Rotation(0, -localZ / cross, localY / cross,
                Math.acos(Math.max(-1, Math.min(1, localX))));
    }
}
