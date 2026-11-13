package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FinBarrierGeometryTest {
  @Test void positions(){for(int i=0;i<6;i++){var p=FinBarrierGeometry.pose(i,1);assertEquals(i<4,p.corner());}}
  @Test void tipsOpenSymmetricallyOnlyDuringBarrierDeployment(){
    assertEquals(0.0, FinBarrierGeometry.tipSplayYawDegrees(0,0.0,12), 1e-8);
    assertEquals(0.0, FinBarrierGeometry.tipSplayYawDegrees(5,0.0,12), 1e-8);
    // Back pair, front pair, then the folded pair: opposite tip yaw.
    for(int left=0;left<6;left+=2){
      int right=left+1;
      double l=FinBarrierGeometry.tipSplayYawDegrees(left,1.0,12);
      double r=FinBarrierGeometry.tipSplayYawDegrees(right,1.0,12);
      assertEquals(12.0,Math.abs(l),1e-8);
      assertEquals(-l,r,1e-8);
      assertEquals(l*0.5,FinBarrierGeometry.tipSplayYawDegrees(left,0.5,12),1e-8);
    }
    // Lower-left and lower-right folded tips face away from each other.
    assertEquals(-12.0,FinBarrierGeometry.tipSplayYawDegrees(4,1.0,12),1e-8);
    assertEquals(12.0,FinBarrierGeometry.tipSplayYawDegrees(5,1.0,12),1e-8);
    // Both face pairs open away from the center rather than converge.
    assertEquals(12.0,FinBarrierGeometry.tipSplayYawDegrees(0,1.0,12),1e-8);
    assertEquals(-12.0,FinBarrierGeometry.tipSplayYawDegrees(2,1.0,12),1e-8);
    assertThrows(IllegalArgumentException.class,
        () -> FinBarrierGeometry.tipSplayYawDegrees(6,1.0,12));
    assertThrows(IllegalArgumentException.class,
        () -> FinBarrierGeometry.tipSplayYawDegrees(0,1.0,Double.NaN));
  }
  @Test void intercepts(){assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(-5,1.5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,1.5,4,0,1.5,0)));
     assertEquals(Double.POSITIVE_INFINITY,FinBarrierGeometry.entryT(0,1.5,0,0,1.5,4));}
}
