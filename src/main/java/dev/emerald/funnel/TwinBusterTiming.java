package dev.emerald.funnel;

/** Small, deterministic time checks for the Twin Buster Rifle. */
final class TwinBusterTiming {
    private TwinBusterTiming() {}
    static long until(long tick, int durationTicks) {
        return tick + Math.max(1, durationTicks);
    }
    static boolean ready(long tick, long until) {
        return tick >= until;
    }
    static boolean charged(long tick, long started, int chargeTicks) {
        return tick - started >= Math.max(1, chargeTicks);
    }
}
