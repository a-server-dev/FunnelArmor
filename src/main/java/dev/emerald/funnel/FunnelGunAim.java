package dev.emerald.funnel;

/**
 * Aiming math for Blockbench shield gun barrels. In the model the muzzles point
 * along negative local Y. Rendering applies the ItemDisplay's yaw after its
 * left rotation, so first transform the world target direction to local space.
 */
public final class FunnelGunAim {
    private FunnelGunAim() { }

    public record Rotation(double axisX, double axisY, double axisZ, double radians) { }

    public static Rotation toward(double dx, double dy, double dz, double displayYawDegrees) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(len) || len < 1.0e-9) {
            return new Rotation(1, 0, 0, 0);
        }
        double yaw = Math.toRadians(displayYawDegrees);
        // Inverse of Minecraft's entity yaw: +Z faces south at yaw 0.
        double x = (Math.cos(yaw) * dx + Math.sin(yaw) * dz) / len;
        double y = dy / len;
        double z = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / len;
        // Cross product (-Y) x target = (-z, 0, x).
        double crossLen = Math.hypot(x, z);
        if (crossLen < 1.0e-8) {
            return y < 0 ? new Rotation(1, 0, 0, 0)
                    : new Rotation(1, 0, 0, Math.PI);
        }
        return new Rotation(-z / crossLen, 0, x / crossLen,
                Math.acos(Math.max(-1, Math.min(1, -y))));
    }

    /**
     * Snap the barrels to the target on the click frame. Hold that aim through
     * tick 3, then smoothly return to idle at tick 8 (unchanged end timing).
     */
    public static double aimWeight(long elapsedTicks) {
        if (elapsedTicks < 0 || elapsedTicks >= 8) return 0;
        if (elapsedTicks <= 3) return 1;
        double x = (elapsedTicks - 3) / 5.0;
        return 1.0 - x * x * (3.0 - 2.0 * x);
    }
}
