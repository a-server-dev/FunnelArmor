package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FinFormationTest {
 @Test void sixFinsAndSavedFormations(){for(FinFormationMode m:FinFormationMode.values())for(int i=0;i<6;i++){
   var p=FinFormationGeometry.position(m,i,0,0.055,1.65,1.55,0.22);
   assertTrue(Double.isFinite(p.right()));assertTrue(Double.isFinite(p.up()));
 }assertEquals(FinFormationMode.STORED,FinFormationMode.parse("stored"));}
}
