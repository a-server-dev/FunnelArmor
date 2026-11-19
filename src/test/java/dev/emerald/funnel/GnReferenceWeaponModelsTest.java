package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Reference-inspired weapon design and first/third person orientation regression tests. */
class GnReferenceWeaponModelsTest {
    private Map<String, byte[]> pack() throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (InputStream in = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(in);
            try (ZipInputStream zip = new ZipInputStream(in)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries.put(entry.getName(), zip.readAllBytes());
                    zip.closeEntry();
                }
            }
        }
        return entries;
    }
    private JsonObject model(Map<String,byte[]> entries, String name) {
        byte[] data = entries.get("assets/funnel/models/item/" + name + ".json");
        assertNotNull(data, name + " missing from resource pack");
        return JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private double get(JsonObject el,String key,int i) {
        return el.getAsJsonArray(key).get(i).getAsDouble();
    }

    @Test void silverTealGunbladeAndLongTwinGreenProngsHaveValidGeometry() throws Exception {
        Map<String,byte[]> zip=pack();
        for (String id : new String[]{"gn_sword_v_sword","gn_sword_v_buster_rifle"}) {
            JsonObject model=model(zip,id);
            JsonArray elems=model.getAsJsonArray("elements");
            assertTrue(elems.size() >= 135);
            assertEquals("funnel:item/"+id,model.getAsJsonObject("textures").get("0").getAsString());
            JsonObject source=JsonParser.parseString(Files.readString(
                    Path.of("models",id+".bbmodel"))).getAsJsonObject();
            JsonArray sourceElems=source.getAsJsonArray("elements");
            assertEquals(elems.size(),sourceElems.size());
            for(int i=0;i<elems.size();i++){
                JsonObject cube=elems.get(i).getAsJsonObject(),bb=sourceElems.get(i).getAsJsonObject();
                assertEquals(cube.get("from"),bb.get("from"));
                assertEquals(cube.get("to"),bb.get("to"));
                for(int axis=0;axis<3;axis++) {
                    double lo=get(cube,"from",axis),hi=get(cube,"to",axis);
                    assertTrue(lo>=-16&&lo<hi&&hi<=32,id+" invalid cube axis "+axis);
                }
            }
            assertTrue(zip.containsKey("assets/funnel/textures/item/"+id+".png"));
        }
        JsonArray gunblade=model(zip,"gn_sword_v_sword").getAsJsonArray("elements");
        // Gunblade blade runs underneath the gun receiver, not as an upright fantasy-sword spike.
        long tealUnderblade=0;
        for(var item:gunblade){
            JsonObject e=item.getAsJsonObject();
            if(get(e,"from",1)<9&&get(e,"to",2)>22&&get(e,"from",2)>=4)
                tealUnderblade++;
        }
        assertTrue(tealUnderblade>=8,"GN Sword must have a distinct lower turquoise blade");
        JsonArray rifle=model(zip,"gn_sword_v_buster_rifle").getAsJsonArray("elements");
        int left=0,right=0;
        for(var item:rifle){
            JsonObject e=item.getAsJsonObject();
            if(get(e,"to",2)<30.8||get(e,"from",2)<18)continue;
            if(get(e,"to",0)<7)left++;
            if(get(e,"from",0)>9)right++;
        }
        assertTrue(left>=2&&right>=2,"GN Buster Rifle must have two separate long GN prongs");
    }

    @Test void swordAndBusterSwordHeldAnglesAreUpdatedByNinetyDegrees() throws Exception {
        Map<String,byte[]> zip=pack();
        JsonObject sword=model(zip,"gn_sword_v_sword");
        JsonObject buster=model(zip,"gn_sword_v_buster_sword");
        for(String hand:new String[]{"firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand"}){
            JsonArray a=sword.getAsJsonObject("display").getAsJsonObject(hand).getAsJsonArray("rotation");
            assertEquals(-90,a.get(0).getAsInt(),"Gunblade must be lifted upright on X");
            JsonArray b=buster.getAsJsonObject("display").getAsJsonObject(hand).getAsJsonArray("rotation");
            assertTrue(b.get(1).getAsInt()==263||b.get(1).getAsInt()==270,
                    "Buster Sword face must be rotated 90° about its long axis");
        }
    }

    @Test void leftSwordDisplayNeverWritesToActualOffhandSlot() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GnSwordCombat.java"));
        assertTrue(code.contains("updateLeftSword(player)"),"Dual GN Sword must render in-game");
        assertTrue(code.contains("setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND)"),
                "Left sword must use the native left-hand model orientation, not the fixed preview pose");
        assertTrue(code.contains("new Vector(facing.getZ(), 0, -facing.getX())"),
                "Left sword must be placed on the player's left, not their right");
        assertTrue(code.contains("add(left.multiply(0.54))"),
                "Left-handed offset must be applied to the helper display");
        assertFalse(code.contains("setPitch(-90.0f +"),
                "Do not double-rotate the sword on top of its -90 degree item transform");
        assertTrue(code.contains("dualVisualMarker"),"Left-hand display needs cleanup marker");
        assertTrue(code.contains("clearLeftSword(owner)"),"Visual displays must be removed on transitions");
        assertTrue(code.contains("setPersistent(false)"),"Visual helper must not persist as an entity");
        assertFalse(code.contains("setItemInOffHand("),"Do not overwrite user's offhand inventory");
    }
}
