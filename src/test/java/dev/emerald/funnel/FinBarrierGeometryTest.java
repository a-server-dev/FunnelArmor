package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FinBarrierGeometryTest {
  @Test void positions(){for(int i=0;i<6;i++){var p=FinBarrierGeometry.pose(i,1);assertEquals(i<4,p.corner());}}
  @Test void openingMovesOnlyIndependentArmTipsNotWholeFunnelYaw(){
    double scale=1.45;
    for(double progress : new double[]{0.0,0.25,0.5,0.75,1.0}) {
      var u=FinBarrierGeometry.armPose(true,progress,6.0,scale);
      var l=FinBarrierGeometry.armPose(false,progress,6.0,scale);
      assertEquals(-u.angleDegrees(),l.angleDegrees(),1e-9);
      if(progress==0) {
        assertEquals(0,u.angleDegrees(),1e-9);
        assertEquals(0,u.shiftX(),1e-9);
        assertEquals(0,u.shiftY(),1e-9);
      }
      for(boolean upper : new boolean[]{true,false}){
        var pose=FinBarrierGeometry.armPose(upper,progress,6.0,scale);
        double a=Math.toRadians(pose.angleDegrees());
        double px=(13.83-8.0)/16.0;
        double py=((upper?9.65:6.40)-8.0)/16.0;
        // Articulation must keep the rear hinge at the original position.
        double hingeX=pose.shiftX()+scale*(Math.cos(a)*px-Math.sin(a)*py);
        double hingeY=pose.shiftY()+scale*(Math.sin(a)*px+Math.cos(a)*py);
        assertEquals(scale*px,hingeX,1e-9);
        assertEquals(scale*py,hingeY,1e-9);
      }
    }
    var upper=FinBarrierGeometry.armPose(true,1.0,6.0,scale);
    var lower=FinBarrierGeometry.armPose(false,1.0,6.0,scale);
    // Regression for the corrected sign after the in-game screenshot:
    // positive setting rotates upper +Z and lower -Z (opposite v0.7.18).
    double noseX=(1.4-8)/16.0;
    double uc= Math.cos(Math.toRadians(upper.angleDegrees()));
    double us= Math.sin(Math.toRadians(upper.angleDegrees()));
    double lc= Math.cos(Math.toRadians(lower.angleDegrees()));
    double ls= Math.sin(Math.toRadians(lower.angleDegrees()));
    double upperNoseY=upper.shiftY()+scale*(us*noseX+uc*((9.8-8)/16.0));
    double lowerNoseY=lower.shiftY()+scale*(ls*noseX+lc*((6.35-8)/16.0));
    assertEquals(6.0, upper.angleDegrees(), 1e-9);
    assertEquals(-6.0, lower.angleDegrees(), 1e-9);
    assertTrue(upperNoseY<scale*((9.8-8)/16.0)-0.10);
    assertTrue(lowerNoseY>scale*((6.35-8)/16.0)+0.10);
    var reversedUpper=FinBarrierGeometry.armPose(true,1.0,-6.0,scale);
    var reversedLower=FinBarrierGeometry.armPose(false,1.0,-6.0,scale);
    assertEquals(-6.0, reversedUpper.angleDegrees(), 1e-9);
    assertEquals(6.0, reversedLower.angleDegrees(), 1e-9);
    assertEquals(0.0, FinBarrierGeometry.armPose(true,1.0,0.0,scale).angleDegrees(), 1e-9);
    assertEquals(15.0, FinBarrierGeometry.armPose(true,1.0,15.0,scale).angleDegrees(), 1e-9);
    assertEquals(-15.0, FinBarrierGeometry.armPose(true,1.0,-15.0,scale).angleDegrees(), 1e-9);
    assertThrows(IllegalArgumentException.class,
        () -> FinBarrierGeometry.armPose(true,1.0,15.01,scale));
    assertThrows(IllegalArgumentException.class,
        () -> FinBarrierGeometry.armPose(true,1.0,-15.01,scale));
    assertThrows(IllegalArgumentException.class,
        () -> FinBarrierGeometry.armPose(true,1.0,Double.NaN,scale));
  }
  @Test void intercepts(){assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(-5,1.5,0,0,1.5,0)));
     assertTrue(Double.isFinite(FinBarrierGeometry.entryT(0,1.5,4,0,1.5,0)));
     assertEquals(Double.POSITIVE_INFINITY,FinBarrierGeometry.entryT(0,1.5,0,0,1.5,4));}
}
