package dev.emerald.funnel;

import java.util.List;

/**
 * Four reference-shaped wired turret pods, each with two conical
 * white muzzles. Total: eight particle emitters.
 * These are lateral muzzle offsets in world blocks relative to each gun's
 * forward direction, preserving the source model's barrel spacing.
 */
final class BrauBroFirePattern {
    record Barrel(int podIndex, double lateral) {}

    static final List<Barrel> BARRELS = List.of(
        new Barrel(0, -.102), new Barrel(0, .102),
        new Barrel(1, -.102), new Barrel(1, .102),
        new Barrel(2, -.102), new Barrel(2, .102),
        new Barrel(3, -.102), new Barrel(3, .102)
    );

    static int cooldown(int ticks) {
        return Math.max(4, Math.min(120, ticks));
    }

    static double range(double blocks) {
        return Double.isFinite(blocks) ? Math.max(4, Math.min(48, blocks)) : 30;
    }

    static double damage(double hp) {
        return DamageSettings.nonnegative(hp, .5);
    }

    static double burstDamage(double perBeam, double maximum, int hits) {
        return Math.min(damage(maximum), damage(perBeam) * Math.max(0, hits));
    }

    static double followStep(double separation, double maxDistance) {
        // Responsive, but visually damped follow: rapid player turns get
        // accelerated pursuit without instantly revolving the guns.
        double speed = .30 + Math.max(0, separation) * .43;
        return Math.min(Math.max(.75, maxDistance * .48), Math.max(.30, speed));
    }

    private BrauBroFirePattern() {}
}
