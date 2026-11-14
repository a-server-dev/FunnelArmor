package dev.emerald.funnel;

/** Pure size calculation for normal drone and Qubeley visual display models. */
public final class DroneModelScale {
    private DroneModelScale() {}
    public static final double DEFAULT_QUBELEY_MULTIPLIER = 0.25;

    public static float forVariant(int variant, double baseSize, double qubeleyMultiplier) {
        if (!Double.isFinite(baseSize)) baseSize = 1.45;
        double base = Math.max(0.1, Math.min(3.0, baseSize));
        if (variant != 3) return (float) base;
        if (!Double.isFinite(qubeleyMultiplier))
            qubeleyMultiplier = DEFAULT_QUBELEY_MULTIPLIER;
        return (float) (base * Math.max(0.05, Math.min(2.0, qubeleyMultiplier)));
    }
}
