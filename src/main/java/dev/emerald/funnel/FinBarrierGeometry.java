package dev.emerald.funnel;
/** Player-relative geometry for four-corner overhead canopy. */
public final class FinBarrierGeometry {
    private FinBarrierGeometry() {}
    public static final double HALF_WIDTH = 1.48;
    public static final double FLOOR = 0.28;
    public static final double TOP = 3.14;
    public record Pose(double right,double up,double forward,boolean corner){}
    public static Pose pose(int index,double progress){
        if(index<0||index>=6)throw new IllegalArgumentException("Fin index 0..5 only");
        double p=ease(progress);
        double side=index%2==0?-1:1;
        if(index>=4)return new Pose(side*0.52,1.68,-0.7,false);
        double back=index<2?-1:1;
        return new Pose(mix(side*0.55,side*HALF_WIDTH,p),
            mix(1.62,TOP,p),mix(-0.72-(index/2)*0.12,back*HALF_WIDTH,p),true);
    }
    public static double approach(double progress,boolean opening,int ticks){
        if(!Double.isFinite(progress)||ticks<1)throw new IllegalArgumentException("Invalid barrier state");
        return Math.max(0,Math.min(1,progress+(opening?1:-1)/(double)ticks));
    }
    private static double ease(double x){
        x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);
    }
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    public static double entryT(double fromR,double fromY,double fromF,double toR,double toY,double toF){
        double[] a={fromR,fromY,fromF},b={toR,toY,toF};
        double[] lo={-HALF_WIDTH,FLOOR,-HALF_WIDTH},hi={HALF_WIDTH,TOP,HALF_WIDTH};
        for(int k=0;k<3;k++)if(!Double.isFinite(a[k])||!Double.isFinite(b[k]))return Double.POSITIVE_INFINITY;
        boolean inside=true;
        for(int k=0;k<3;k++)inside &= a[k]>=lo[k]&&a[k]<=hi[k];
        if(inside)return Double.POSITIVE_INFINITY;
        double enter=0,exit=1;
        for(int k=0;k<3;k++){
            double d=b[k]-a[k];
            if(Math.abs(d)<1.e-10){if(a[k]<lo[k]||a[k]>hi[k])return Double.POSITIVE_INFINITY;continue;}
            double t0=(lo[k]-a[k])/d,t1=(hi[k]-a[k])/d;
            if(t0>t1){double z=t0;t0=t1;t1=z;}
            enter=Math.max(enter,t0);exit=Math.min(exit,t1);
            if(enter>exit)return Double.POSITIVE_INFINITY;
        }
        return enter>0&&enter<=1?enter:Double.POSITIVE_INFINITY;
    }
}
