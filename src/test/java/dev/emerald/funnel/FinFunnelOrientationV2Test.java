package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinFunnelOrientationV2Test {
    private static void assertFaces(float yaw, double playerYaw, double expectedRight, double expectedForward) {
        double relative = Math.toRadians(yaw - playerYaw);
        // Local -X -> (-cos(yaw), -sin(yaw)) in player-relative horizontal plane.
        double len = Math.hypot(expectedRight,expectedForward);
        assertEquals(expectedRight/len,-Math.cos(relative),1.e-6);
        assertEquals(expectedForward/len,-Math.sin(relative),1.e-6);
    }
    @Test void storedAndStationaryFormationsPointAhead() {
        for (FinFormationMode m : FinFormationMode.values()) {
            if (m == FinFormationMode.ORBIT || m == FinFormationMode.SPIRAL) continue;
            float yaw = FinFunnelOrientation.finFormationYaw(30, m,0.5,0.9,0);
            assertFaces(yaw,30,0,1);
        }
    }
    @Test void orbitAndSpiralPointOutward() {
        for (FinFormationMode m : new FinFormationMode[] {FinFormationMode.ORBIT,FinFormationMode.SPIRAL})
            assertFaces(FinFunnelOrientation.finFormationYaw(45,m,1,1,0),45,1,1);
    }
    @Test void barrierCornersPointInAndFoldedTwoFaceAhead() {
        for(int i=0;i<4;i++){
            double r=i%2==0?-1.48:1.48, f=i<2?-1.48:1.48;
            assertFaces(FinFunnelOrientation.barrierYaw(70,r,f,i,0),70,-r,-f);
        }
        for(int i=4;i<6;i++)
            assertFaces(FinFunnelOrientation.barrierYaw(70,0.5,-0.7,i,0),70,0,1);
    }
    @Test void adjustmentAndLegacyMathAreFinite() {
        assertEquals(-90,FinFunnelOrientation.towardRelative(0,0,1,0),1.e-7);
        assertEquals(-75,FinFunnelOrientation.towardRelative(0,0,1,15),1.e-7);
        assertEquals(90,FinFunnelOrientation.formationYaw(0,0,1,90),1.e-7);
        assertThrows(IllegalArgumentException.class,
                () -> FinFunnelOrientation.towardRelative(0,0,0,0));
    }
}
