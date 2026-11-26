package dev.emerald.funnel;

/** 3 volleys in 4 ticks: exactly three times the old 1-per-4-tick rate. */
final class HeavyarmsCadence {
    private HeavyarmsCadence(){}
    static int interval(int phase){return Math.floorMod(phase,3)==2?2:1;}
}
