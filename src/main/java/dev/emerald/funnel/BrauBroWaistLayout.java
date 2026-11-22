package dev.emerald.funnel;

import java.util.List;

/**
 * Four MAN-03 waist gun mounts and pure deployment geometry.
 * The held controller remains in the player's hand; only the pods travel.
 * All coordinates are local to the player's horizontal facing direction.
 */
final class BrauBroWaistLayout {
    record Mount(double side, double height, double forward,
                 float yawOffset, boolean twin) {}
    record Offset(double side, double height, double forward) {}

    static final List<Mount> MOUNTS = List.of(
        new Mount(-0.43, 0.96,  0.22, -18.0f, true),
        new Mount( 0.43, 0.96,  0.22,  18.0f, true),
        new Mount(-0.43, 0.91, -0.24, -160.0f, false),
        new Mount( 0.43, 0.91, -0.24,  160.0f, false)
    );

    static Offset stowed(Mount mount) {
        return new Offset(mount.side(), mount.height(), mount.forward());
    }

    static Offset launched(Mount mount, double reach) {
        double distance = Math.max(2.0, Math.min(8.0, reach));
        double outSide = Math.copySign(Math.abs(mount.side()) + 0.63 + 0.20 * distance,
                mount.side());
        double outForward = Math.copySign(0.24 + 0.71 * distance, mount.forward());
        return new Offset(outSide, mount.height() + 0.38, outForward);
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
        // The cable originates inside the waist-mounted purple harness,
        // not at the center of the player's chest or from the held item.
        return new Offset(mount.side() * .66,
                mount.height() - .065, mount.forward() * .52);
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
