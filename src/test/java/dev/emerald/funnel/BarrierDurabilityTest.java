package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BarrierDurabilityTest {
    @Test void breaksOnFifthHitRegardlessOfDamage() {
        BarrierDurability.State state = new BarrierDurability.State(0, 0);
        for (int i=1;i<=5;i++) {
            state=state.absorb(0.5);
            assertEquals(i==5, state.broken(5, 20));
        }
        assertEquals(2.5, state.totalDamage(), 1e-8);
    }
    @Test void breaksOnTwentyDamageBeforeFifthHit() {
        BarrierDurability.State state = new BarrierDurability.State(0, 0);
        state=state.absorb(8).absorb(12);
        assertTrue(state.broken(5,20));
        assertEquals(2, state.hits());
        assertEquals(20, state.totalDamage(), 1e-8);
        assertFalse(new BarrierDurability.State(0,0).broken(5,20));
    }
    @Test void rejectsInvalidDamage() {
        var state = new BarrierDurability.State(0,0);
        assertThrows(IllegalArgumentException.class, () -> state.absorb(-1));
        assertThrows(IllegalArgumentException.class, () -> state.absorb(Double.NaN));
    }
}
