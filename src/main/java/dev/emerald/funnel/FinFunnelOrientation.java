package dev.emerald.funnel;

/** ν fin-funnel model axis conventions, independent of Bukkit. */
public final class FinFunnelOrientation {
    private FinFunnelOrientation() {}

    /** Original helper retained for other integrations and historical tests. */
    public static float formationYaw(float playerYaw, double formationYawOffset,
                                     int variant, double finModelYawOffset) {
        if (!Float.isFinite(playerYaw) || !Double.isFinite(formationYawOffset)
                || !Double.isFinite(finModelYawOffset))
            throw new IllegalArgumentException("Non-finite fin yaw");
        return playerYaw + (float) formationYawOffset
                + (variant == 1 ? (float) finModelYawOffset : 0f);
    }

    /**
     * The deployed model's open muzzle points toward LOCAL -X (not local -Y).
     * Under ItemDisplay entity yaw, that vector becomes
     * (-cos(yaw), 0, -sin(yaw)) in world coordinates when player yaw=0.
     * Inputs specify desired heading in player-relative (+right,+forward).
     */
    public static float towardRelative(float playerYaw, double right, double forward,
                                       double extraYawDegrees) {
        if (!Float.isFinite(playerYaw) || !Double.isFinite(right)
                || !Double.isFinite(forward) || !Double.isFinite(extraYawDegrees)
                || Math.hypot(right, forward) < 1.0e-10)
            throw new IllegalArgumentException("Invalid fin heading");
        return playerYaw + (float) Math.toDegrees(Math.atan2(-forward, -right))
                + (float) extraYawDegrees;
    }

    public static float finFormationYaw(float playerYaw, FinFormationMode mode,
                                        double right, double forward, double extraYawDegrees) {
        if (mode == null) throw new IllegalArgumentException("Fin formation is null");
        // Orbit and spiral point radially outward. Stationary formations and
        // folded fins face the player's forward direction.
        return (mode == FinFormationMode.ORBIT || mode == FinFormationMode.SPIRAL)
                ? towardRelative(playerYaw, right, forward, extraYawDegrees)
                : towardRelative(playerYaw, 0, 1, extraYawDegrees);
    }

    public static float barrierYaw(float playerYaw, double right, double forward,
                                   int index, double extraYawDegrees) {
        if (index < 0 || index >= 6) throw new IllegalArgumentException("Fin index 0..5");
        // Four overhead corner guns face inward; the two folded units wait
        // behind the player facing forward.
        return index < 4
                ? towardRelative(playerYaw, -right, -forward, extraYawDegrees)
                : towardRelative(playerYaw, 0, 1, extraYawDegrees);
    }
}
