package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FinBarrierGlassGeometryTest {
    @Test void fiveThinWhiteGlassFacesMatchBarrierBox() {
        List<FinBarrierGlassGeometry.Panel> p = FinBarrierGlassGeometry.panels(0.035);
        assertEquals(5, p.size());
        double span = FinBarrierGeometry.HALF_WIDTH * 2;
        for (int i=0; i<4; i++) assertEquals(FinBarrierGeometry.TOP - FinBarrierGeometry.FLOOR,
                p.get(i).sizeUp(), 0.000001);
        assertEquals(span, p.get(4).sizeRight(), 0.000001);
        assertEquals(span, p.get(4).sizeForward(), 0.000001);
        assertEquals(FinBarrierGeometry.TOP, p.get(4).up()+p.get(4).sizeUp()/2, 0.000001);
        assertEquals(0.035, p.get(4).sizeUp(), 0.000001);
    }
    @Test void panelsFormAnAlignedBoxForEveryPlayerHeading() {
        var panels = FinBarrierGlassGeometry.panels(0.035);
        double hw = FinBarrierGeometry.HALF_WIDTH;
        double floor = FinBarrierGeometry.FLOOR;
        double top = FinBarrierGeometry.TOP;
        for (double yaw : new double[]{-180, -135, -90, -45, 0, 30, 90, 135, 180}) {
            double angle = Math.toRadians(yaw);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            for (int i = 0; i < panels.size(); i++) {
                var panel = panels.get(i);
                var pose = FinBarrierGlassGeometry.placement(panel, yaw);
                assertEquals(-angle, pose.modelYawRadians(), 1.e-9);
                // Inverse player rotation: world-space lower corner returns
                // to the exact local corner used by the barrier hitbox.
                double localRight = cos * pose.x() + sin * pose.z();
                double localForward = -sin * pose.x() + cos * pose.z();
                assertEquals(panel.right(), localRight, 1.e-8);
                assertEquals(panel.forward(), localForward, 1.e-8);
                assertEquals(panel.up(), pose.y(), 1.e-8);
                // A sizeRight unit edge must rotate in lockstep with the
                // position, rather than accidentally remaining world-aligned.
                double edgeWorldX = Math.cos(-pose.modelYawRadians()) * panel.sizeRight();
                double edgeWorldZ = Math.sin(-pose.modelYawRadians()) * panel.sizeRight();
                assertEquals(cos * panel.sizeRight(), edgeWorldX, 1.e-8);
                assertEquals(sin * panel.sizeRight(), edgeWorldZ, 1.e-8);
                assertTrue(panel.sizeUp() <= top - floor || i == 4);
            }
            // Local-space walls still make one closed rectangular enclosure.
            assertEquals(-hw, panels.get(0).forward() + panels.get(0).sizeForward()/2, 1.e-8);
            assertEquals( hw, panels.get(1).forward() + panels.get(1).sizeForward()/2, 1.e-8);
            assertEquals(-hw, panels.get(2).right() + panels.get(2).sizeRight()/2, 1.e-8);
            assertEquals( hw, panels.get(3).right() + panels.get(3).sizeRight()/2, 1.e-8);
        }
    }
    @Test void rejectsNonFinitePlayerHeading() {
        assertThrows(IllegalArgumentException.class, () ->
                FinBarrierGlassGeometry.placement(FinBarrierGlassGeometry.panels(0.035).get(0), Double.NaN));
    }
    @Test void visualGlassRejectsInvalidThickness() {
        assertThrows(IllegalArgumentException.class, () -> FinBarrierGlassGeometry.panels(0));
        assertThrows(IllegalArgumentException.class, () -> FinBarrierGlassGeometry.panels(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> FinBarrierGlassGeometry.panels(0.5));
    }
    @Test void allFinFunnelFormationsGainOneConsistentCorrection() {
        for(FinFormationMode mode : FinFormationMode.values()){
            for(int i=0;i<6;i++){
                var pos=FinFormationGeometry.position(mode,i,12,0.055,1.65,1.55,0.22);
                assertEquals(90f+(float)pos.yawOffset(),
                    FinFunnelOrientation.formationYaw(0f,pos.yawOffset(),1,90), 0.0002f);
            }
        }
        // Four corner displays and two rear folded displays: no alternate +/-90 yaw.
        for(int i=0;i<6;i++){
            assertEquals(120f, FinFunnelOrientation.formationYaw(30f,0,1,90),0.00001f);
        }
        for(int variant=2;variant<=4;variant++)
            assertEquals(20f, FinFunnelOrientation.formationYaw(30f,-10,variant,90),0.00001f);
    }
}
