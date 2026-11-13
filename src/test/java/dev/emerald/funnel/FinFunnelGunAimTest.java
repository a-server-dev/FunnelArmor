package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinFunnelGunAimTest {
    private static void assertAims(double x, double y, double z, double yaw) {
        FinFunnelGunAim.Rotation r = FinFunnelGunAim.toward(x,y,z,yaw);
        // Rodrigues rotation: R(axis,angle) applied to local (-1,0,0)
        double cos = Math.cos(r.radians()), sin = Math.sin(r.radians());
        double ax = r.axisX(), ay = r.axisY(), az = r.axisZ();
        double dot = -ax;
        double lx = -cos + (ax * dot)*(1-cos);
        double ly = -az*sin + (ay * dot)*(1-cos);
        double lz = ay*sin + (az * dot)*(1-cos);
        double angle = Math.toRadians(yaw);
        double wx = Math.cos(angle)*lx-Math.sin(angle)*lz;
        double wz = Math.sin(angle)*lx+Math.cos(angle)*lz;
        double mag = Math.sqrt(x*x+y*y+z*z);
        assertEquals(x/mag,wx,1.e-6);
        assertEquals(y/mag,ly,1.e-6);
        assertEquals(z/mag,wz,1.e-6);
    }
    @Test void muzzlePointsToRayTraceForAllDirectionsAndPlayerYaw() {
        assertAims(0,0,1,-90);
        assertAims(1,0,0,0);
        assertAims(-1,0,0,0);
        assertAims(0,0,-1,90);
        assertAims(2,1,-5,45);
        assertAims(-3,-1,8,178);
        assertAims(0,1,0,-90);
        assertAims(0,-1,0,90);
    }
    @Test void usesSameImmediateSnapAndEightTickReturnAsUnicorn() {
        assertEquals(1, FunnelGunAim.aimWeight(0));
        assertEquals(1, FunnelGunAim.aimWeight(3));
        assertTrue(FunnelGunAim.aimWeight(4) < 1);
        assertEquals(0, FunnelGunAim.aimWeight(8));
    }
}
