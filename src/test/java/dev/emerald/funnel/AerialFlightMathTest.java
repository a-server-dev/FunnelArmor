package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class AerialFlightMathTest {
    @Test void exactlyElevenUniqueAndFiniteSlots() {
        Set<AerialFlightMath.Offset> shield = new HashSet<>();
        Set<AerialFlightMath.Offset> rifle = new HashSet<>();
        for (int i = 0; i < 11; i++) {
            assertTrue(shield.add(AerialFlightMath.shield(i)), "overlapping shield slot " + i);
            assertTrue(rifle.add(AerialFlightMath.rifle(i)), "overlapping rifle slot " + i);
            var free = AerialFlightMath.free(100, i);
            assertTrue(Double.isFinite(free.side()) && Double.isFinite(free.height())
                    && Double.isFinite(free.forward()));
            assertTrue(free.height() >= .8 && free.height() <= 2.9);
        }
        assertThrows(IllegalArgumentException.class, () -> AerialFlightMath.free(0, 11));
        assertThrows(IllegalArgumentException.class, () -> AerialFlightMath.shield(-1));
    }

    @Test void flightPositionsChangeIrregularly() {
        var early = AerialFlightMath.free(50, 0);
        var next = AerialFlightMath.free(52, 0);
        var far = AerialFlightMath.free(140, 0);
        assertNotEquals(early, next);
        assertNotEquals(early, far);
        assertTrue(Math.hypot(early.side()-next.side(), early.forward()-next.forward()) < .3,
                "two-tick motion must not teleport several blocks");
    }

    @Test void smoothTransitionKeepsEndpointsAndDoesNotOvershoot() {
        var a = AerialFlightMath.free(33, 5);
        var b = AerialFlightMath.shield(5);
        assertEquals(0, AerialFlightMath.progress(33, 33, 10));
        assertEquals(1, AerialFlightMath.progress(43, 33, 10));
        assertEquals(.5, AerialFlightMath.progress(38, 33, 10), 1e-8);
        assertEquals(a, AerialFlightMath.mix(a,b,0));
        assertEquals(b, AerialFlightMath.mix(a,b,1));
        assertEquals(a, AerialFlightMath.mix(a,b,-50));
        assertEquals(b, AerialFlightMath.mix(a,b,100));
    }

    @Test void aimingUsesMinecraftYawPitchConvention() {
        assertEquals(0, AerialFlightMath.horizontalYaw(0, 1), 1e-9);
        assertEquals(-90, AerialFlightMath.horizontalYaw(1, 0), 1e-9);
        assertEquals(90, AerialFlightMath.horizontalYaw(-1, 0), 1e-9);
        assertEquals(0, AerialFlightMath.pitch(0,0,1),1e-9);
        assertEquals(-45, AerialFlightMath.pitch(0,1,1),1e-9);
    }
}
