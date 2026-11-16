package dev.emerald.funnel;

import java.util.Set;
import java.util.UUID;

/** Toggle an active barrier without losing the retract animation state. */
final class FinBarrierToggleState {
    private FinBarrierToggleState() {}

    /** @return true if the barrier is now deployed, false if retracting. */
    static boolean toggle(Set<UUID> guards, UUID player) {
        if (guards.remove(player)) return false;
        guards.add(player);
        return true;
    }
}
