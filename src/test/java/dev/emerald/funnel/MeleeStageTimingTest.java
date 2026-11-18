package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MeleeStageTimingTest {
    @Test void saberProgressivelyExtendsBeforeItCanDamage() {
        for (int elapsed = 0; elapsed < 12; elapsed++) {
            int stage = MeleeStageTiming.stageAt(elapsed, 3, true);
            assertEquals(elapsed / 3, stage);
            assertFalse(MeleeStageTiming.canHit(false, stage));
        }
        assertEquals(4, MeleeStageTiming.stageAt(12, 3, true));
        assertTrue(MeleeStageTiming.canHit(true, 4));
        for (int elapsed = 0; elapsed <= 12; elapsed += 3)
            assertEquals(4 - elapsed / 3, MeleeStageTiming.stageAt(elapsed, 3, false));
        assertFalse(MeleeStageTiming.canHit(false, 4),
                "Retracting blades must stop damaging immediately");
        assertFalse(MeleeStageTiming.canHit(true, 3),
                "Partially extended blades must not deal damage");
        assertEquals(0, MeleeStageTiming.stageAt(-5, 3, true));
        assertEquals(4, MeleeStageTiming.stageAt(100, 3, true));
    }
}
