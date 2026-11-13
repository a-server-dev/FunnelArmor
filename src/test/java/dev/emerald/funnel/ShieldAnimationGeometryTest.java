package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShieldAnimationGeometryTest {
    @Test void defenseEndsAtExistingWallDistance() {
        for (int i=0; i<3; i++) {
            var pose = ShieldAnimationGeometry.pose(i, 1);
            assertEquals(ShieldGuardGeometry.PLANE_DISTANCE + (i - 1) * 0.06,
                    pose.forward(), 1.e-9);
            assertEquals(0, pose.yawOffset(), 1.e-9);
        }
        assertEquals(0, ShieldAnimationGeometry.pose(0, 1).right(), 1.e-9);
        assertEquals(-0.58, ShieldAnimationGeometry.pose(1, 1).right(), 1.e-9);
        assertEquals(0.58, ShieldAnimationGeometry.pose(2, 1).right(), 1.e-9);
        assertEquals(0, ShieldAnimationGeometry.pose(0, 1).rollDegrees(), 1.e-9);
        assertEquals(-120, ShieldAnimationGeometry.pose(1, 1).rollDegrees(), 1.e-9);
        assertEquals(120, ShieldAnimationGeometry.pose(2, 1).rollDegrees(), 1.e-9);
        assertTrue(ShieldAnimationGeometry.pose(0, 1).up() <
                ShieldAnimationGeometry.pose(1, 1).up());
    }

    @Test void opensFromBehindShouldersAndClosesBackThere() {
        for (int i=0; i<3; i++) {
            assertTrue(ShieldAnimationGeometry.pose(i, 0).forward() < 0);
            double t = 0;
            for (int k=0; k<12; k++) t = ShieldAnimationGeometry.approach(t, true, 12);
            assertEquals(1, t, 1.e-12);
            for (int k=0; k<12; k++) t = ShieldAnimationGeometry.approach(t, false, 12);
            assertEquals(0, t, 1.e-12);
        }
    }

    @Test void easingIsSmoothAndProgressIsReversible() {
        assertEquals(0, ShieldAnimationGeometry.ease(0));
        assertEquals(1, ShieldAnimationGeometry.ease(1));
        assertEquals(.5, ShieldAnimationGeometry.ease(.5));
        assertEquals(ShieldAnimationGeometry.ease(.25), 1-ShieldAnimationGeometry.ease(.75), 1.e-9);
        for (int i=1; i<12; i++) {
            double a=ShieldAnimationGeometry.pose(0,i/12.0).forward();
            double b=ShieldAnimationGeometry.pose(0,(i+1)/12.0).forward();
            assertTrue(b>a);
        }
        double active=.42;
        double higher=ShieldAnimationGeometry.approach(active,true,12);
        assertEquals(active,ShieldAnimationGeometry.approach(higher,false,12),1.e-9);
    }

    @Test void firingPulseReturnsToRest() {
        assertEquals(0, ShieldAnimationGeometry.shotPulse(0),1.e-9);
        assertEquals(1, ShieldAnimationGeometry.shotPulse(4),1.e-9);
        assertEquals(0, ShieldAnimationGeometry.shotPulse(8),1.e-9);
        assertEquals(0, ShieldAnimationGeometry.shotPulse(9),1.e-9);
        assertEquals(0, ShieldAnimationGeometry.shotPulse(-1),1.e-9);
    }
}
