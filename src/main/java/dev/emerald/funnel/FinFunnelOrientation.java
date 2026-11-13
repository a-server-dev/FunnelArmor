package dev.emerald.funnel;

/** Independent correction for the ν fin-funnel model's local orientation. */
public final class FinFunnelOrientation {
    private FinFunnelOrientation() { }
    public static float formationYaw(float playerYaw, double formationYawOffset,
                                     int variant, double finModelYawOffset) {
        if (!Float.isFinite(playerYaw) || !Double.isFinite(formationYawOffset)
                || !Double.isFinite(finModelYawOffset))
            throw new IllegalArgumentException("Non-finite fin yaw");
        return playerYaw + (float) formationYawOffset
                + (variant == 1 ? (float) finModelYawOffset : 0f);
    }
}
