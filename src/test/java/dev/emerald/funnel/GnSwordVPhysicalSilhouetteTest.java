package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class GnSwordVPhysicalSilhouetteTest {
    private JsonArray elements(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(
            Path.of("models","gn_sword_v_"+mode+".bbmodel"))).getAsJsonObject().getAsJsonArray("elements");
    }
    private double v(JsonObject el,String which,int axis){
        return el.getAsJsonArray(which).get(axis).getAsDouble();
    }
    private List<JsonObject> matching(String mode,String begins) throws Exception{
        List<JsonObject> found=new ArrayList<>();
        for(var e:elements(mode)){
            var obj=e.getAsJsonObject();
            if(obj.get("name").getAsString().startsWith(begins))found.add(obj);
        }
        return found;
    }
    @Test void swordHasPhysicalBlueArmourAndAVisibleExtendedGreenBlade() throws Exception {
        var blades=matching("sword","Quanta single emerald blade segment ");
        assertEquals(20,blades.size());
        double prev=Double.POSITIVE_INFINITY;
        for(var b:blades) {
            double width=v(b,"to",0)-v(b,"from",0);
            assertTrue(width<prev,"GN Sword V blade must taper at every step");
            prev=width;
        }
        assertTrue(v(blades.get(19),"to",2)>31.6);
        assertTrue(v(blades.get(0),"from",2)>18.5);
        var blue=matching("sword","GN Sword V primary royal-blue dorsal foil ");
        assertEquals(13,blue.size());
        assertTrue(v(blue.get(0),"to",1)>13.5,"Cobalt dorsal housing must be visible above core");
        assertEquals(13,matching("sword","GN Sword V port cobalt stepped fairing ").size());
        assertEquals(13,matching("sword","GN Sword V starboard cobalt cheek ").size());
    }
    @Test void busterRifleHasExactlyTwoSeparatedEnergyProngsAndBlueArmorEndsEarlier() throws Exception {
        List<JsonObject> midpoints=new ArrayList<>();
        for(String side:List.of("L","R")){
            var blade=matching("buster_rifle","GN emerald tapered cutting edge "+side+" ");
            assertEquals(20,blade.size());
            assertTrue(v(blade.get(19),"to",2)>31.6,"Long point must reach front of item space");
            midpoints.add(blade.get(12));
            var hood=matching("buster_rifle","GN QanT "+side+" long tapered sapphire sword-bit hood ");
            assertEquals(22,hood.size());
            assertTrue(v(hood.get(21),"to",2)<27,"Blue armour should stop before exposed green point");
            assertTrue(v(hood.get(0),"to",1)>v(blade.get(0),"to",1),"Top housing must be BLUE");
        }
        assertTrue(v(midpoints.get(0),"to",0)+1.5<v(midpoints.get(1),"from",0),
            "The two green blades must remain separated by a visible center channel");
    }
    @Test void editionFilesRetainUniqueEditableUuidAndIdenticalGameBounds() throws Exception{
        for(String mode:List.of("sword","buster_rifle")){
            var source=JsonParser.parseString(Files.readString(
                Path.of("models","gn_sword_v_"+mode+".bbmodel"))).getAsJsonObject();
            var game=JsonParser.parseString(Files.readString(
                Path.of("resourcepack/assets/funnel/models/item/gn_sword_v_"+mode+".json"))).getAsJsonObject();
            JsonArray a=source.getAsJsonArray("elements");
            JsonArray b=game.getAsJsonArray("elements");
            assertEquals(a.size(),b.size());
            Set<String> uuids=new HashSet<>();
            for(int i=0;i<a.size();i++) {
                JsonObject el=a.get(i).getAsJsonObject();
                JsonObject ge=b.get(i).getAsJsonObject();
                assertTrue(uuids.add(el.get("uuid").getAsString()),"Editor part UUID must be unique");
                assertEquals(el.get("from"),ge.get("from"));
                assertEquals(el.get("to"),ge.get("to"));
            }
        }
    }
}
