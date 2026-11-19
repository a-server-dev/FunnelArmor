package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Ensures approved GN II stays separate while Qan[T] weapons gain real detail. */
class GnQuantaDetailTest {
    private JsonObject bb(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(
            Path.of("models/gn_sword_v_" + mode + ".bbmodel"))).getAsJsonObject();
    }
    private JsonObject game(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(
            Path.of("resourcepack/assets/funnel/models/item/gn_sword_v_" + mode + ".json"))).getAsJsonObject();
    }
    private double coord(JsonObject cube,String field,int axis) {
        return cube.getAsJsonArray(field).get(axis).getAsDouble();
    }
    @Test void allFourModesAreDetailedAndGripPosesAreDistinct() throws Exception {
        for (String mode : List.of("sword","rifle","buster_sword","buster_rifle")) {
            JsonObject source=bb(mode), packed=game(mode);
            JsonArray elements=source.getAsJsonArray("elements");
            int min= switch(mode) {
                case "sword" -> 170;
                case "rifle" -> 240;
                case "buster_sword" -> 580;
                default -> 320;
            };
            assertTrue(elements.size()>=min,mode+" must retain substantive layers without meaningless duplicate cubes");
            assertEquals(elements.size(),packed.getAsJsonArray("elements").size());
            for(int i=0;i<elements.size();i++) {
                JsonObject cube=elements.get(i).getAsJsonObject();
                JsonObject output=packed.getAsJsonArray("elements").get(i).getAsJsonObject();
                assertEquals(cube.get("from"),output.get("from"),mode+" cube "+i+" from");
                assertEquals(cube.get("to"),output.get("to"),mode+" cube "+i+" to");
                for(int axis=0;axis<3;axis++)
                    assertTrue(coord(cube,"from",axis)>=-16
                            && coord(cube,"to",axis)<=32
                            && coord(cube,"from",axis)<coord(cube,"to",axis),
                            mode+" invalid element "+i+" axis "+axis);
            }
            JsonObject display=packed.getAsJsonObject("display");
            for(String hand:List.of("firstperson_righthand","thirdperson_righthand")) {
                JsonObject pose=display.getAsJsonObject(hand);
                assertTrue(pose.getAsJsonArray("translation").get(1).getAsDouble()>=1.5,
                        mode+" grip should be raised toward the hand");
                assertTrue(pose.getAsJsonArray("scale").get(0).getAsDouble()<=1.04,
                        mode+" must not swallow player body");
            }
        }
    }
    @Test void busterRifleHasTwoBroadSeparatedCuttingWings() throws Exception {
        JsonObject source=bb("buster_rifle");
        int left=0,right=0;
        for(var element:source.getAsJsonArray("elements")){
            JsonObject el=element.getAsJsonObject();
            String name=el.get("name").getAsString();
            if(!name.startsWith("Broad QanT GN energy cutting plane"))continue;
            if(name.contains(" L "))left++; else if(name.contains(" R "))right++;
            assertTrue(coord(el,"to",2)>13,"Blades must extend beyond receiver");
        }
        assertEquals(25,left);
        assertEquals(25,right);
    }
    @Test void swordHasSingleBladeWhileBusterSwordKeepsSixBitAssemblies() throws Exception {
        JsonObject sword=bb("sword");
        int single=0;
        for(var el:sword.getAsJsonArray("elements"))
            if(el.getAsJsonObject().get("name").getAsString().startsWith("Quanta single emerald blade segment"))single++;
        assertEquals(20,single);
        JsonObject buster=bb("buster_sword");
        int bitGroups=0;
        for(var g:buster.getAsJsonArray("outliner")) {
            String name=g.getAsJsonObject().get("name").getAsString();
            if(name.startsWith("GN Sword Bit "))bitGroups++;
        }
        assertEquals(6,bitGroups);
    }
}
