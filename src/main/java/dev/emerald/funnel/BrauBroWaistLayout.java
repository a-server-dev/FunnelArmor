package dev.emerald.funnel;

import java.util.List;

/**
 * Four waist-mounted MAN-03-inspired gun pods, controlled by one hand item.
 * Two twin-barrel pods are forward; two single-barrel pods face rearward.
 * Side and forward are in the player's horizontal local coordinate system.
 */
final class BrauBroWaistLayout {
    record Mount(double side, double height, double forward,
                 float yawOffset, boolean twin) {}

    static final List<Mount> MOUNTS = List.of(
        new Mount(-0.43, 0.96,  0.22, -18.0f, true),
        new Mount( 0.43, 0.96,  0.22,  18.0f, true),
        new Mount(-0.43, 0.91, -0.24, -160.0f, false),
        new Mount( 0.43, 0.91, -0.24,  160.0f, false)
    );

    private BrauBroWaistLayout() {}
}
