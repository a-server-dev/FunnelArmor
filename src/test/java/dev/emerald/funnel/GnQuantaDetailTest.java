package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Validate genuine source-model detail, not an arbitrary high cuboid count. */
class GnQuantaDetailTest {
    private JsonObject read(String mode,boolean source) throws Exception {
        String file="gn_sword_v_"+mode+(source?".bbmodel":".json");
        Path path=source?Path.of("models",file):
              Path.of("resourcepack","assets","funnel","models","item",file);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    private double c(JsonObject e,String name,int axis){
        return e.getAsJsonArray(name).get(axis).getAsDouble();
    }
    @Test void allModesRemainEditableAndPreciselyMatchPack() throws Exception {
        for(String mode:List.of("sword","rifle","buster_sword","buster_rifle")){
            JsonObject source=read(mode,true),game=read(mode,false);
            JsonArray a=source.getAsJsonArray("elements"),b=game.getAsJsonArray("elements");
            assertTrue(a.size()>=40 && a.size()<=1500,mode+" should have useful detail without unbounded overdraw");
            assertEquals(a.size(),b.size(),mode);
            for(int i=0;i<a.size();i++){
                JsonObject el=a.get(i).getAsJsonObject(),g=b.get(i).getAsJsonObject();
                assertEquals(el.get("from"),g.get("from"));
                assertEquals(el.get("to"),g.get("to"));
                for(int axis=0;axis<3;axis++)
                    assertTrue(c(el,"from",axis)>=-16&&c(el,"to",axis)<=32
                        &&c(el,"from",axis)<c(el,"to",axis),mode+" part "+i);
            }
            JsonObject display=game.getAsJsonObject("display");
            for(String hand:List.of("firstperson_righthand","thirdperson_righthand")){
                assertTrue(display.getAsJsonObject(hand).getAsJsonArray("translation").get(1).getAsDouble()>1.4);
            }
        }
    }
    @Test void sourceRebuildKeepsStructuredGeometryWithinBudget() throws Exception {
        int sword = read("sword", true).getAsJsonArray("elements").size();
        int rifle = read("buster_rifle", true).getAsJsonArray("elements").size();
        assertTrue(sword >= 200 && sword <= 400,
            "Sword guard, condenser, crystal edge and frame require independently editable details");
        assertTrue(rifle >= 420 && rifle <= 650,
            "The two Sword Bit assemblies must have independent blue/green parts");
    }
    @Test void QanTSwordIsSingleAndBusterSwordRetainsSixBits() throws Exception {
        JsonObject sword=read("sword",true);
        long count=0;
        for(var part:sword.getAsJsonArray("elements")){
            if(part.getAsJsonObject().get("name").getAsString().startsWith("QanT visible green sword blade "))count++;
        }
        assertEquals(20,count);
        JsonObject buster=read("buster_sword",true);
        int bitGroups=0;
        for(var gr:buster.getAsJsonArray("outliner")){
            if(gr.getAsJsonObject().get("name").getAsString().startsWith("GN Sword Bit "))bitGroups++;
        }
        assertEquals(6,bitGroups);
    }
}
