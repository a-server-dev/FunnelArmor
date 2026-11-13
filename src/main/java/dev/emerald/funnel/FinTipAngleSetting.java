package dev.emerald.funnel;

/** Parsing and validation for signed fin-barrier articulation settings. */
public final class FinTipAngleSetting {
    private FinTipAngleSetting() {}

    public static final double DEFAULT = 6.0;

    private static double parse(Object raw) {
        if (raw instanceof Number number) return number.doubleValue();
        if (raw instanceof String text) {
            String normalized = text.trim().replace('\u2212', '-').replace('\uFF0D', '-');
            try { return Double.parseDouble(normalized); }
            catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid fin-tip angle", ex);
            }
        }
        throw new IllegalArgumentException("Invalid fin-tip angle");
    }

    public static double validate(Object raw) {
        double value = parse(raw);
        if (!Double.isFinite(value) || value < -15.0 || value > 15.0)
            throw new IllegalArgumentException("Fin-tip angle must be -15..15 degrees");
        return value;
    }

    /** Config values are clamped for backwards compatibility; malformed values use default. */
    public static double effective(Object raw) {
        if (raw == null) return DEFAULT;
        try {
            double value = parse(raw);
            if (!Double.isFinite(value)) return DEFAULT;
            return Math.max(-15.0, Math.min(15.0, value));
        } catch (IllegalArgumentException ex) {
            return DEFAULT;
        }
    }
}
