package dev.emerald.funnel;

/** Five layered binders per side. Values are in player-relative block units. */
final class FreedomWingLayout {
    private FreedomWingLayout(){}
    record Pose(double x,double y,double z,double roll,double sweep,double scale){}
    private static final double[] X={.49,.60,.65,.57,.45};
    private static final double[] Y={1.51,1.39,1.23,1.12,1.04};
    private static final double[] Z={-.63,-.55,-.48,-.41,-.36};
    private static final double[] OPEN={50,68,86,105,122};
    private static final double[] FULL={52,70,88,107,124};
    private static final double[] CLOSED={164,169,174,179,184};
    private static final double[] SCALE={1.91,1.78,1.57,1.49,1.15};
    private static final double[] STOW={.84,.82,.79,.75,.69};
    static Pose pose(int row,double q,double c){
        q=Math.max(0,Math.min(1,q));c=Math.max(0,Math.min(1,c));
        return new Pose(lerp(.32+row*.025,X[row],q),
            lerp(1.53-row*.02,Y[row],q),lerp(-.42-row*.035,Z[row],q),
            lerp(CLOSED[row],lerp(OPEN[row],FULL[row],c),q),
            (2+row*1.7)*q,lerp(STOW[row],SCALE[row],q));
    }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
}
