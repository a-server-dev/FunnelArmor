package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Guard real projected cutting-edge geometry rather than obsolete part numbers. */
class GnSwordVPhysicalSilhouetteTest {
    private JsonArray cubes(String mode) throws Exception{
        return JsonParser.parseString(Files.readString(Path.of("models",
            "gn_sword_v_"+mode+".bbmodel"))).getAsJsonObject().getAsJsonArray("elements");
    }
    private double v(JsonObject a,String s,int i){return a.getAsJsonArray(s).get(i).getAsDouble();}
    private List<JsonObject> segments(String mode,String prefix) throws Exception {
        List<JsonObject> x=new ArrayList<>();
        for(var e:cubes(mode)){
            JsonObject a=e.getAsJsonObject();
            if(a.get("name").getAsString().startsWith(prefix))x.add(a);
        }
        return x;
    }
    @Test void swordGreenBladeHasLargeVisibleRootAndFineTip() throws Exception {
        var green=segments("sword","QanT visible green sword blade ");
        assertEquals(20,green.size());
        double root=v(green.get(0),"to",1)-v(green.get(0),"from",1);
        double tip=v(green.get(19),"to",1)-v(green.get(19),"from",1);
        assertTrue(root>7,"Green sword needs a large exposed Y/Z face, not an X-only width");
        assertTrue(tip<root*.15);
        assertTrue(v(green.get(19),"to",2)>31.6);
        assertEquals(11,segments("sword","Raised sapphire armor sweep ").size());
        assertEquals(20,segments("sword","QanT visible green sword blade ").size());
    }
    @Test void busterRifleHasTwoIndependentOffsetYProfiles() throws Exception {
        var upper=segments("buster_rifle","GN emerald tapered cutting edge L ");
        var lower=segments("buster_rifle","GN emerald tapered cutting edge R ");
        assertEquals(20,upper.size());
        assertEquals(20,lower.size());
        for(int i=1;i<20;i++)for(var blade:List.of(upper,lower)){
            double before=v(blade.get(i-1),"to",1)-v(blade.get(i-1),"from",1);
            double after=v(blade.get(i),"to",1)-v(blade.get(i),"from",1);
            assertTrue(after<before,"Tip should converge in projected Y as well as X");
        }
        assertTrue(v(upper.get(12),"from",1)>v(lower.get(12),"to",1));
        for(var blade:List.of(upper,lower))assertTrue(v(blade.get(19),"to",2)>31.6);
    }
    @Test void allSourcesHaveUniqueUUIDAndIdenticalPackCuboids() throws Exception {
        for(String mode:List.of("sword","buster_rifle")){
            JsonArray a=cubes(mode);
            JsonObject p=JsonParser.parseString(Files.readString(
                Path.of("resourcepack","assets","funnel","models","item",
                "gn_sword_v_"+mode+".json"))).getAsJsonObject();
            JsonArray b=p.getAsJsonArray("elements");
            assertEquals(a.size(),b.size());
            Set<String> ids=new HashSet<>();
            for(int i=0;i<a.size();i++){
                JsonObject el=a.get(i).getAsJsonObject(),item=b.get(i).getAsJsonObject();
                assertTrue(ids.add(el.get("uuid").getAsString()));
                assertEquals(el.get("from"),item.get("from"));
                assertEquals(el.get("to"),item.get("to"));
                for(int k=0;k<3;k++){
                    assertTrue(v(el,"from",k)>=-16 &&v(el,"to",k)<=32 &&v(el,"from",k)<v(el,"to",k));
                }
            }
        }
    }
}
