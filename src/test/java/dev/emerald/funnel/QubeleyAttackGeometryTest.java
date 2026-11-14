package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QubeleyAttackGeometryTest {
    @Test void twelveBitsStaySeparatedAndMoveSmoothlyButDifferently() {
        final long seed = 0x123ABC919L;
        for (long tick : new long[]{0, 12, 70, 160, 400}) {
            java.util.List<QubeleyAttackGeometry.Position> positions = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) {
                var p = QubeleyAttackGeometry.position(i,tick,seed);
                double radius = Math.hypot(p.x(),p.z());
                assertTrue(radius >= 2.0 && radius <= 3.60, "radius: " + radius);
                assertTrue(Math.abs(p.y()) <= 1.8);
                assertEquals(p, QubeleyAttackGeometry.position(i,tick,seed));
                var next = QubeleyAttackGeometry.position(i,tick+1,seed);
                double diff = Math.sqrt(Math.pow(p.x()-next.x(),2) +
                        Math.pow(p.y()-next.y(),2) + Math.pow(p.z()-next.z(),2));
                assertTrue(diff < 0.45, "bit should not teleport between ticks");
                for (var prior:positions) {
                    double distance = Math.hypot(p.x()-prior.x(),p.z()-prior.z());
                    assertTrue(distance > 0.22, "bits overlap at " + tick);
                }
                positions.add(p);
            }
        }
        var a = QubeleyAttackGeometry.position(3,80,seed);
        var b = QubeleyAttackGeometry.position(3,80,seed + 1);
        assertNotEquals(a, b, "Each activation should vary the motion");
        var c = QubeleyAttackGeometry.position(3,120,seed);
        assertTrue(Math.abs(Math.hypot(a.x(),a.z())-Math.hypot(c.x(),c.z())) > 0.001);
    }

    @Test void twelveBitsLaunchNearPlayerBeforeFlyingToTarget() {
        final var destination = new QubeleyAttackGeometry.Position(14, 66.0, 4);
        java.util.Set<String> starts = new java.util.HashSet<>();
        for (int i = 0; i < 12; i++) {
            var offset = QubeleyAttackGeometry.launchOffset(i, 45);
            var source = new QubeleyAttackGeometry.Position(
                    5 + offset.x(), 64 + offset.y(), -3 + offset.z());
            starts.add(source.x() + "/" + source.y() + "/" + source.z());
            assertTrue(Math.hypot(offset.x(), offset.z()) < 1.0);
            assertTrue(offset.y() >= 1.3 && offset.y() <= 2.2);
            assertEquals(source, QubeleyAttackGeometry.flightPosition(
                    source, destination, i, 0), "Do not pop at the target on spawn");
            assertEquals(source, QubeleyAttackGeometry.flightPosition(
                    source, destination, i, QubeleyAttackGeometry.departureDelay(i)));
            assertEquals(destination, QubeleyAttackGeometry.flightPosition(
                    source, destination, i, 100));
            var last = source;
            for (int tick = 1; tick <= 60; tick++) {
                var current = QubeleyAttackGeometry.flightPosition(
                        source, destination, i, tick);
                double distance = Math.sqrt(
                        Math.pow(current.x()-last.x(), 2) +
                        Math.pow(current.y()-last.y(), 2) +
                        Math.pow(current.z()-last.z(), 2));
                assertTrue(distance < 2.0, "No instantaneous target teleport");
                last = current;
            }
        }
        assertEquals(12, starts.size());
        assertFalse(QubeleyAttackGeometry.allArrived(34));
        assertTrue(QubeleyAttackGeometry.allArrived(35));
        assertThrows(IllegalArgumentException.class,
                () -> QubeleyAttackGeometry.launchOffset(12, 0));
    }

    @Test void aimCorrectsReversedYawAndTracksUpDown() {
        var forward = QubeleyAttackGeometry.toward(0, 0, 1);
        assertEquals(0,forward.yaw(), 1e-6);
        assertEquals(0,forward.pitch(), 1e-6);
        assertEquals(-90,QubeleyAttackGeometry.toward(1,0,0).yaw(),1e-5);
        assertEquals(90,QubeleyAttackGeometry.toward(-1,0,0).yaw(),1e-5);
        assertEquals(-45,QubeleyAttackGeometry.toward(0,1,1).pitch(),1e-5);
        assertEquals(45,QubeleyAttackGeometry.toward(0,-1,1).pitch(),1e-5);
        assertEquals(-90,QubeleyAttackGeometry.toward(0,1,0).pitch(),1e-5);
    }

    @Test void invalidDataRejected() {
        assertThrows(IllegalArgumentException.class, () -> QubeleyAttackGeometry.position(-1,0,0));
        assertThrows(IllegalArgumentException.class, () -> QubeleyAttackGeometry.position(12,0,0));
        assertThrows(IllegalArgumentException.class, () -> QubeleyAttackGeometry.toward(0,0,0));
        assertThrows(IllegalArgumentException.class, () -> QubeleyAttackGeometry.toward(Double.NaN,0,0));
    }
}
