package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunnelGunAimTest {
    private static void assertAims(double x, double y, double z, double yaw) {
        var r = FunnelGunAim.toward(x, y, z, yaw);
        // Apply rotation to the original negative-Y model-axis using Rodrigues.
        double s=Math.sin(r.radians()), c=Math.cos(r.radians());
        double rx=r.axisZ()*s;
        double ry=-c;
        double rz=-r.axisX()*s;
        // Rotate from model-local into world using entity yaw.
        double a=Math.toRadians(yaw);
        double wx=Math.cos(a)*rx-Math.sin(a)*rz;
        double wz=Math.sin(a)*rx+Math.cos(a)*rz;
        double len=Math.sqrt(x*x+y*y+z*z);
        assertEquals(x/len,wx,1.e-6);
        assertEquals(y/len,ry,1.e-6);
        assertEquals(z/len,wz,1.e-6);
    }
    @Test void gunPointsAtTargetHorizontallyAndVertically() {
        assertAims(0,0,1,0);
        assertAims(0,0,-1,0);
        assertAims(1,0,0,0);
        assertAims(-1,0,0,90);
        assertAims(3,2,5,30);
        assertAims(-1,-0.4,2,155);
        assertAims(0,-1,0,0);
        assertAims(0,1,0,0);
    }

    @Test void turnIsTemporaryAndFinite() {
        assertEquals(0,FunnelGunAim.aimWeight(-1));
        assertEquals(0,FunnelGunAim.aimWeight(9));
        assertEquals(1,FunnelGunAim.aimWeight(0),1.e-9, "The muzzle must point at the target immediately");
        for (int tick=1;tick<=3;tick++) assertEquals(1,FunnelGunAim.aimWeight(tick),1.e-9);
        assertTrue(FunnelGunAim.aimWeight(4) < 1);
        assertTrue(FunnelGunAim.aimWeight(4) > FunnelGunAim.aimWeight(5));
        assertTrue(FunnelGunAim.aimWeight(5) > FunnelGunAim.aimWeight(6));
        assertTrue(FunnelGunAim.aimWeight(6) > FunnelGunAim.aimWeight(7));
        assertEquals(0,FunnelGunAim.aimWeight(8),1.e-9);
        assertEquals(0,FunnelGunAim.toward(0,0,0,0).radians());
    }
}
