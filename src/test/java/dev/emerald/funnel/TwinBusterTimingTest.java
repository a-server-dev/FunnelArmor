package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TwinBusterTimingTest {
    @Test void cooldownBlocksUntilExactExpiry() {
        long until = TwinBusterTiming.until(100, 200);
        assertFalse(TwinBusterTiming.ready(299, until));
        assertTrue(TwinBusterTiming.ready(300, until));
    }
    @Test void chargeOnlyFinishesAfterFullDuration() {
        assertFalse(TwinBusterTiming.charged(129, 100, 30));
        assertTrue(TwinBusterTiming.charged(130, 100, 30));
    }
    @Test void durationCannotBeZeroOrNegative() {
        assertEquals(21, TwinBusterTiming.until(20, -10));
        assertFalse(TwinBusterTiming.charged(20, 20, -10));
        assertTrue(TwinBusterTiming.charged(21, 20, -10));
    }
}
