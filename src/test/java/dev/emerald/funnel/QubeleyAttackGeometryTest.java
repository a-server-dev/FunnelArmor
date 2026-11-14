package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QubeleyAttackGeometryTest {
    @Test void producesTwelveDistinctPositionsInTwoCircles() {
        for (long tick : new long[]{0,15,70}) {
            java.util.Set<String> loc = new java.util.HashSet<>();
            for (int i = 0; i < 12; i++) {
                var p=QubeleyAttackGeometry.position(i,tick);
                double r=Math.hypot(p.x(),p.z());
                assertEquals(i < 6 ? 2.65 : 3.1,r,0.00001);
                assertTrue(Double.isFinite(p.y()));
                loc.add(Math.round(p.x()*1000)+":"+Math.round(p.z()*1000));
            }
            assertEquals(12, loc.size());
        }
    }
    @Test void rejectsInvalidSlot() {
        assertThrows(IllegalArgumentException.class, ()->QubeleyAttackGeometry.position(-1,0));
        assertThrows(IllegalArgumentException.class, ()->QubeleyAttackGeometry.position(12,0));
    }
}
