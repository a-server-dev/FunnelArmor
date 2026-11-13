package dev.emerald.funnel;

/**
 * Pure geometric shield-interception calculations. Local coordinates are:
 * right (+right), up (relative to feet), forward (+in front of the player).
 */
public final class ShieldGuardGeometry {
    private ShieldGuardGeometry() { }

    public static final double PLANE_DISTANCE = 1.85;
    public static final double HALF_WIDTH = 1.72;
    public static final double MIN_HEIGHT = 0.20;
    public static final double MAX_HEIGHT = 2.65;

    /**
     * Projectile must travel from the FRONT side through the shield plane.
     * Both endpoints are measured relative to the player's position/orientation.
     */
    public static boolean intercepts(double fromRight, double fromUp, double fromForward,
                                     double toRight, double toUp, double toForward) {
        if (!Double.isFinite(fromRight) || !Double.isFinite(fromUp) || !Double.isFinite(fromForward)
                || !Double.isFinite(toRight) || !Double.isFinite(toUp) || !Double.isFinite(toForward)) {
            return false;
        }
        if (fromForward < PLANE_DISTANCE || toForward > PLANE_DISTANCE) return false;
        double d = fromForward - toForward;
        if (d <= 1.0e-9) return false;
        double t = (fromForward - PLANE_DISTANCE) / d;
        if (t < 0 || t > 1) return false;
        double right = fromRight + (toRight - fromRight) * t;
        double up = fromUp + (toUp - fromUp) * t;
        return Math.abs(right) <= HALF_WIDTH && up >= MIN_HEIGHT && up <= MAX_HEIGHT;
    }

    /** Fallback for damage events where the projectile already crossed the wall. */
    public static boolean blocksImpact(double projectileForward, double velocityForward) {
        return Double.isFinite(projectileForward) && Double.isFinite(velocityForward)
                && projectileForward >= -0.60 && velocityForward < -0.01;
    }

    public static int shieldCount(int variant) { return variant == 4 ? 3 : 0; }
}
