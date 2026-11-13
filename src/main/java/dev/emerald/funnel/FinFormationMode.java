package dev.emerald.funnel;
import java.util.Locale;
public enum FinFormationMode {
    STORED("stored","格納",null), ORBIT("orbit","周回",FormationMode.ORBIT),
    WINGS("wings","左右展開",FormationMode.WINGS), REAR("rear","背面待機",FormationMode.REAR),
    FRONT("front","前方防御",FormationMode.FRONT), SPIRAL("spiral","螺旋",FormationMode.SPIRAL);
    private final String id,displayName;
    private final FormationMode equivalent;
    FinFormationMode(String id,String displayName,FormationMode equivalent) {
        this.id=id;this.displayName=displayName;this.equivalent=equivalent;
    }
    public String id(){return id;}
    public String displayName(){return displayName;}
    public FormationMode equivalent(){return equivalent;}
    public FinFormationMode next(){return values()[(ordinal()+1)%values().length];}
    public static FinFormationMode parse(String value){
        if(value==null)return null;
        String trimmed=value.trim();
        for(FinFormationMode mode:values())
            if(mode.id.equals(trimmed.toLowerCase(Locale.ROOT))||mode.displayName.equals(trimmed))return mode;
        return null;
    }
    public static FinFormationMode orDefault(String value){FinFormationMode mode=parse(value);return mode==null?STORED:mode;}
    public static FinFormationMode fromLegacy(String previous){
        FinFormationMode mode=parse(previous);return mode==null?ORBIT:mode;
    }
}
