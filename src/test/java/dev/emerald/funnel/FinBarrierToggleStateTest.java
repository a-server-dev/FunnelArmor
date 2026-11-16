package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FinBarrierToggleStateTest {
    @Test void deployRetractAndRedeploy() {
        Set<UUID> guards = new HashSet<>();
        UUID player = UUID.randomUUID();
        assertTrue(FinBarrierToggleState.toggle(guards, player));
        assertTrue(guards.contains(player));
        assertFalse(FinBarrierToggleState.toggle(guards, player));
        assertFalse(guards.contains(player));
        assertTrue(FinBarrierToggleState.toggle(guards, player));
        assertTrue(guards.contains(player));
    }

    @Test void doesNotAffectOtherPlayers() {
        Set<UUID> guards = new HashSet<>();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(FinBarrierToggleState.toggle(guards, a));
        assertTrue(FinBarrierToggleState.toggle(guards, b));
        assertFalse(FinBarrierToggleState.toggle(guards, a));
        assertFalse(guards.contains(a));
        assertTrue(guards.contains(b));
    }
}
