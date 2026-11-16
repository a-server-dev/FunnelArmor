package dev.emerald.funnel;

/**
 * Pure interaction routing for the Aerial controller. A cancelled vanilla
 * right-click-air event is still a valid controller input; the Paper listener
 * therefore must not use ignoreCancelled=true.
 */
final class AerialInputRouting {
    private AerialInputRouting() {}
    enum Intent { NONE, SHIELD_TOGGLE, RIFLE_TOGGLE, RIFLE_FIRE }

    static Intent route(boolean mainHandEvent, boolean mainController,
                        boolean offController, boolean sneaking, boolean rifleMode) {
        if (mainHandEvent) {
            if (!mainController) return Intent.NONE;
            if (sneaking) return Intent.RIFLE_TOGGLE;
            if (rifleMode) return Intent.RIFLE_FIRE;
            return offController ? Intent.SHIELD_TOGGLE : Intent.NONE;
        }
        if (!offController) return Intent.NONE;
        // Aerial in both hands: one physical click must not trigger shield
        // after the main-hand rifle action.
        if (mainController && (sneaking || rifleMode)) return Intent.NONE;
        return Intent.SHIELD_TOGGLE;
    }
}
