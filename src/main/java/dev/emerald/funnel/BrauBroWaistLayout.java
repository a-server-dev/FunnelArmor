package dev.emerald.funnel;

import java.util.List;

/**
 * Four identical MAN-03 twin-muzzle wired cannons. The upper pair launches
 * above and ahead, while the lower pair launches slightly below them,
 * still IN FRONT of the player (not behind or to its four cardinal sides).
 *
 * Coordinates are player-relative: side = left/right, forward = facing,
 * height = blocks above feet. Units are Minecraft world blocks.
 */
final class BrauBroWaistLayout {
    record Mount(double side, double height, double forward,
                 float yawOffset, boolean upper) {}
    record Offset(double side, double height, double forward) {}

    static final List<Mount> MOUNTS = List.of(
        // Upper row: same twin-muzzle pod as the lower row.
        new Mount(-0.43, 0.96,  0.22, -11.0f, true),
        new Mount( 0.43, 0.96,  0.22,  11.0f, true),
        // Lower row: these are also twin-muzzle pods, NOT single barrels.
        new Mount(-0.43, 0.91, -0.24, -13.0f, false),
        new Mount( 0.43, 0.91, -0.24,  13.0f, false)
    );

    static Offset stowed(Mount mount) {
        return new Offset(mount.side(), mount.height(), mount.forward());
    }

    static Offset launched(Mount mount, double reach) {
        double clamped = Math.max(2.0, Math.min(8.0, reach));
        // Scale the forward fan without switching quadrants. At the default
        // 4.2 blocks: high row (+/-1.28 side, y=2.84, front=2.78);
        // low row (+/-1.47 side, y=1.26, front=2.62).
        double size = 0.5 + 0.5 * (clamped / 4.2);
        double side = Math.copySign((mount.upper() ? 1.28 : 1.47) * size,
            mount.side());
        double forward = (mount.upper() ? 2.78 : 2.62) * size;
        double lift = (mount.upper() ? 1.88 : 0.35) * size;
        return new Offset(side, mount.height() + lift, forward);
    }

    static double smooth(double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return t * t * (3.0 - 2.0 * t);
    }

    static Offset lerp(Offset from, Offset to, double progress) {
        double t = Math.max(0.0, Math.min(1.0, progress));
        return new Offset(
                from.side() + (to.side() - from.side()) * t,
                from.height() + (to.height() - from.height()) * t,
                from.forward() + (to.forward() - from.forward()) * t);
    }

    static Offset position(Mount mount, double reach, double progress) {
        return lerp(stowed(mount), launched(mount, reach), smooth(progress));
    }

    static Offset cableBase(Mount mount) {
        // All 4 wires originate at the FRONT of the player's waist
        // rather than extending backward from a rear connection point.
        return new Offset(mount.side() * 0.76,
            mount.height() + 0.03, 0.20);
    }

    static Offset cablePoint(Offset start, Offset end, double fraction, double sag) {
        Offset straight = lerp(start, end, fraction);
        double t = Math.max(0.0, Math.min(1.0, fraction));
        return new Offset(straight.side(),
            straight.height() - Math.max(0, sag) * 4.0 * t * (1.0 - t),
            straight.forward());
    }

    private BrauBroWaistLayout() {}
}
