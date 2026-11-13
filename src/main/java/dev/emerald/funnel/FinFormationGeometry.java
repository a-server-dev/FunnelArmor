package dev.emerald.funnel;
public final class FinFormationGeometry{
    private FinFormationGeometry(){}
    public static FormationGeometry.Position position(FinFormationMode mode,
        int index,double ticks,double angularSpeed,double radius,double height,double bobAmplitude){
        if(index<0||index>=6)throw new IllegalArgumentException("Fin index must be 0..5");
        if(mode==FinFormationMode.STORED)return new FormationGeometry.Position(
            -0.74-index*0.17,1.50+index*0.09,-0.48-index*0.035,0.0);
        return FormationGeometry.position(mode.equivalent(),1,index,ticks,angularSpeed,radius,height,bobAmplitude);
    }
}
