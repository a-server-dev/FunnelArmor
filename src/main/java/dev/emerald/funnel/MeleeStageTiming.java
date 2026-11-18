package dev.emerald.funnel;

/** Deterministic state math for right-click beam blade extension / retraction. */
final class MeleeStageTiming {
    private MeleeStageTiming() {}
    static int stageAt(long elapsedTicks, int ticksPerStage, boolean extending) {
        int progress = (int)Math.min(4L,
                Math.max(0L, elapsedTicks) / Math.max(1, ticksPerStage));
        return extending ? progress : 4 - progress;
    }
    static boolean canHit(boolean deployed, int stage) {
        return deployed && stage == 4;
    }
}
