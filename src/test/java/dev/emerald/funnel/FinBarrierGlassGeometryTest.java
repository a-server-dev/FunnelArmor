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
