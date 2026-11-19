package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Silhouette and historical symmetry requirements. */
class GnQuantaSilhouetteReworkTest {
    private JsonObject model(String mode,boolean editable) throws Exception{
        Path path=editable?Path.of("models","gn_sword_v_"+mode+".bbmodel"):
            Path.of("resourcepack","assets","funnel","models","item","gn_sword_v_"+mode+".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    private double coord(JsonObject e,String f,int k){return e.getAsJsonArray(f).get(k).getAsDouble();}
    private boolean has(String m,String name) throws Exception{
        for(var el:model(m,true).getAsJsonArray("elements")){
            if(el.getAsJsonObject().get("name").getAsString().equals(name))return true;
        }
        return false;
    }
    @Test void GNBladeIsNotMadeOfOneHugeWhiteIronSlab() throws Exception {
        assertTrue(has("sword","GN Sword V exposed bright green GN condenser"));
        assertTrue(has("sword","Raised sapphire armor sweep 0"));
        assertTrue(has("sword","GN Sword V emerald root base broad"));
        for(var x:model("sword",true).getAsJsonArray("elements")){
            JsonObject e=x.getAsJsonObject();
            double y=coord(e,"to",1)-coord(e,"from",1);
            double z=coord(e,"to",2)-coord(e,"from",2);
            boolean shell=e.get("name").getAsString().contains("armour")
                 ||e.get("name").getAsString().contains("receiver");
            if(shell)assertFalse(y>4&&z>8,"Large unbroken rectangle would recreate an iron");
        }
    }
    @Test void twinBluePodsDoNotHideGreenDistalTips() throws Exception {
        for(String side:List.of("L","R")){
            int pods=0,blades=0;
            double furthestBlue=0,furthestGreen=0;
            for(var e:model("buster_rifle",true).getAsJsonArray("elements")){
                JsonObject p=e.getAsJsonObject();String name=p.get("name").getAsString();
                if(name.startsWith("GN QanT "+side+" long tapered sapphire sword-bit hood")){
                    pods++;furthestBlue=Math.max(furthestBlue,coord(p,"to",2));
                }
                if(name.startsWith("GN emerald tapered cutting edge "+side+" ")){
                    blades++;furthestGreen=Math.max(furthestGreen,coord(p,"to",2));
                }
            }
            assertEquals(22,pods);
            assertEquals(20,blades);
            assertTrue(furthestBlue+4<furthestGreen,"Green cutting spear must extend beyond blue armor");
        }
    }
    private String key(JsonObject c,boolean flip){
        double[] f=new double[3],t=new double[3];
        for(int i=0;i<3;i++){f[i]=coord(c,"from",i);t[i]=coord(c,"to",i);}
        if(flip){double old=f[2];f[2]=18.44-t[2];t[2]=18.44-old;}
        return String.format(java.util.Locale.ROOT,"%.3f/%.3f/%.3f/%.3f/%.3f/%.3f",
            f[0],f[1],f[2],t[0],t[1],t[2]);
    }
    @Test void approvedBusterSwordRemainsMirrorSymmetric() throws Exception{
        Set<String> shapes=new HashSet<>();
        JsonArray a=model("buster_sword",true).getAsJsonArray("elements");
        for(var v:a)shapes.add(key(v.getAsJsonObject(),false));
        for(var v:a)assertTrue(shapes.contains(key(v.getAsJsonObject(),true)));
    }
    @Test void bbmodelCuboidsMatchGameItemModel() throws Exception{
        for(String mode:List.of("sword","rifle","buster_sword","buster_rifle")){
            JsonArray a=model(mode,true).getAsJsonArray("elements"),
                b=model(mode,false).getAsJsonArray("elements");
            assertEquals(a.size(),b.size());
            for(int i=0;i<a.size();i++){
                assertEquals(a.get(i).getAsJsonObject().get("from"),b.get(i).getAsJsonObject().get("from"));
                assertEquals(a.get(i).getAsJsonObject().get("to"),b.get(i).getAsJsonObject().get("to"));
            }
        }
    }
}
