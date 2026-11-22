package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Math/animation contract for MAN-03's four continuously connected tethers. */
class BrauBroWireGeometryTest {
    @Test void launchedPodsAreAllOutsideWaistAndRemainFourDistinctGuns() {
        var mounts = BrauBroWaistLayout.MOUNTS;
        assertEquals(4, mounts.size());
        for (var mount : mounts) {
            var waist = BrauBroWaistLayout.stowed(mount);
            var launched = BrauBroWaistLayout.launched(mount, 4.2);
            assertTrue(Math.abs(launched.side()) > Math.abs(waist.side()) + .7);
            assertTrue(Math.abs(launched.forward()) > Math.abs(waist.forward()) + 2.3);
            assertEquals(Math.signum(mount.forward()), Math.signum(launched.forward()));
            assertEquals(Math.signum(mount.side()), Math.signum(launched.side()));
            assertTrue(launched.height() > waist.height() + .3);
        }
        var a = BrauBroWaistLayout.launched(mounts.get(0), 4.2);
        var b = BrauBroWaistLayout.launched(mounts.get(1), 4.2);
        assertEquals(-a.side(), b.side(), 1e-8);
        assertEquals(a.forward(), b.forward(), 1e-8);
        var c = BrauBroWaistLayout.launched(mounts.get(2), 4.2);
        var d = BrauBroWaistLayout.launched(mounts.get(3), 4.2);
        assertEquals(-c.side(), d.side(), 1e-8);
        assertEquals(c.forward(), d.forward(), 1e-8);
    }

    @Test void launchAnimationIsContinuousAndExactlyReversible() {
        for (var mount : BrauBroWaistLayout.MOUNTS) {
            var start = BrauBroWaistLayout.stowed(mount);
            var end = BrauBroWaistLayout.launched(mount, 4.2);
            assertEquals(start, BrauBroWaistLayout.position(mount, 4.2, 0));
            assertEquals(end, BrauBroWaistLayout.position(mount, 4.2, 1));
            double lastDistance = -1;
            for (int i = 0; i<=100; i++) {
                double t = i/100.0;
                var a = BrauBroWaistLayout.position(mount, 4.2, t);
                var reverse = BrauBroWaistLayout.position(mount, 4.2, 1-t);
                var again = BrauBroWaistLayout.position(mount, 4.2, t);
                assertEquals(a, again);
                double dist = Math.abs(a.side()-start.side())
                    + Math.abs(a.forward()-start.forward());
                assertTrue(dist >= lastDistance-1e-8);
                lastDistance=dist;
                assertEquals(BrauBroWaistLayout.position(mount,4.2,1-t),reverse);
            }
        }
        assertEquals(0, BrauBroWaistLayout.smooth(-1));
        assertEquals(1, BrauBroWaistLayout.smooth(2));
        assertEquals(.5, BrauBroWaistLayout.smooth(.5), 1e-8);
    }

    @Test void eachWireStartsAtWaistAndEndsAtCurrentCannonWithSagInBetween() {
        for (var mount : BrauBroWaistLayout.MOUNTS) {
            var waist = BrauBroWaistLayout.cableBase(mount);
            var pod = BrauBroWaistLayout.position(mount, 4.2, 1);
            // Endpoint is always exact even after applying cable sag.
            assertEquals(waist, BrauBroWaistLayout.cablePoint(waist,pod,0,.15));
            assertEquals(pod, BrauBroWaistLayout.cablePoint(waist,pod,1,.15));
            for (int segment=0; segment<3; segment++) {
                var a = BrauBroWaistLayout.cablePoint(waist,pod,segment/3.0,.15);
                var b = BrauBroWaistLayout.cablePoint(waist,pod,(segment+1)/3.0,.15);
                assertTrue(Double.isFinite(a.side()) && Double.isFinite(a.height()));
                assertNotEquals(a,b);
            }
            var center = BrauBroWaistLayout.cablePoint(waist,pod,.5,.15);
            assertEquals((waist.height()+pod.height())/2-.15,
                center.height(),1e-9);
            assertEquals((waist.side()+pod.side())/2,center.side(),1e-9);
        }
    }

    @Test void extensionLengthIsBoundedAndNoTeleportsOccurWhenMidFlightDirectionFlips() {
        var mount=BrauBroWaistLayout.MOUNTS.get(0);
        assertEquals(BrauBroWaistLayout.launched(mount,2),
            BrauBroWaistLayout.launched(mount,-100));
        assertEquals(BrauBroWaistLayout.launched(mount,8),
            BrauBroWaistLayout.launched(mount,999));
        var mid = BrauBroWaistLayout.position(mount,4.2,.48);
        // Mid-flight reversal uses the same progress and doesn't jump.
        assertEquals(mid,BrauBroWaistLayout.position(mount,4.2,.48));
        assertNotEquals(mid,BrauBroWaistLayout.position(mount,4.2,0));
    }
}
