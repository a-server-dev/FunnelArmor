package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FinBarrierGeometryTest {
  @Test void positions(){for(int i=0;i<6;i++){var p=FinBarrierGeometry.pose(i,1);assertEquals(i<4,p.corner());}}
  @Test void intercepts(){assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(-5,1.5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,1.5,4,0,1.5,0)));
     assertEquals(Double.POSITIVE_INFINITY,FinBarrierGeometry.entryT(0,1.5,0,0,1.5,4));}
}
