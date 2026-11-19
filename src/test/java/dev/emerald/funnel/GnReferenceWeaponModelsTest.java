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
            if(get(e,"to",2)<31.7||get(e,"from",2)<30.5)continue;
            if(get(e,"to",0)<7)left++;
            if(get(e,"from",0)>9)right++;
        }
        assertTrue(left>=1&&right>=1,"Both GN cutting edges must reach sharp independent tips");
    }

    @Test void swordAndBusterSwordHeldAnglesAreUpdatedByNinetyDegrees() throws Exception {
        Map<String,byte[]> zip=pack();
        JsonObject sword=model(zip,"gn_sword_v_sword");
        JsonObject buster=model(zip,"gn_sword_v_buster_sword");
        for(String hand:new String[]{"firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand"}){
            JsonArray a=sword.getAsJsonObject("display").getAsJsonObject(hand).getAsJsonArray("rotation");
            assertEquals(0,a.get(0).getAsInt(),"GN Sword blade must point forward, not hang below the arm");
            var gripPose=sword.getAsJsonObject("display").getAsJsonObject(hand);
            assertTrue(gripPose.getAsJsonArray("translation").get(2).getAsDouble()<=-12,
                    "GN Sword handle must sit at the wrist rather than the cutting edge");
            JsonArray b=buster.getAsJsonObject("display").getAsJsonObject(hand).getAsJsonArray("rotation");
            assertTrue(b.get(1).getAsInt()==263||b.get(1).getAsInt()==270,
                    "Buster Sword face must be rotated 90° about its long axis");
        }
    }

    @Test void leftSwordDisplayNeverWritesToActualOffhandSlot() throws Exception {
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GnSwordCombat.java"));
        assertTrue(code.contains("updateLeftSword(player)"),"Dual GN Sword must render in-game");
        assertTrue(code.contains("setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED)"),
                "Left world display must use FIXED to avoid duplicate native hand transforms");
        assertTrue(code.contains("new Vector(facing.getZ(), 0, -facing.getX())"),
                "Left sword must be placed on the player's left, not their right");
        assertTrue(code.contains("add(left.multiply(0.42))"),
                "Secondary GN Sword must align beside the player's left arm");
        assertTrue(code.contains("double gripToPivot = (8.0 - (-7.0)) * 0.76 / 16.0"),
                "World display must be anchored to the model's actual grip position");
        assertTrue(code.contains("leftHand.setPitch(0.0f)"),
                "World display must not tip a long sword into a vertical pose");
        assertFalse(code.contains("ItemDisplay.ItemDisplayTransform.THIRDPERSON_LEFTHAND"),
                "Never apply hand-attached item transforms to free floating sword");
        assertFalse(code.contains("setItemInOffHand("),
                "Do not overwrite user's real offhand inventory");
        assertTrue(code.contains("dualVisualMarker"),"Left-hand display needs cleanup marker");
        assertTrue(code.contains("clearLeftSword(owner)"),"Visual displays must be removed on transitions");
        assertTrue(code.contains("setPersistent(false)"),"Visual helper must not persist as an entity");
        assertFalse(code.contains("setItemInOffHand("),"Do not overwrite user's offhand inventory");
    }
    @Test void gnBusterRifleIsSlenderHorizontalWithRealTaperedTwinBlades() throws Exception {
        Map<String,byte[]> zip=pack();
        JsonObject model=model(zip,"gn_sword_v_buster_rifle");
        JsonArray elems=model.getAsJsonArray("elements");
        assertTrue(elems.size()>=233,"Each cutting blade needs extended taper and dorsal armoring");
        double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY;
        double minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;
        int longLeft=0,longRight=0;
        for(var item:elems){
            JsonObject e=item.getAsJsonObject();
            minX=Math.min(minX,get(e,"from",0));
            maxX=Math.max(maxX,get(e,"to",0));
            minY=Math.min(minY,get(e,"from",1));
            maxY=Math.max(maxY,get(e,"to",1));
            if(get(e,"from",2)>=30.5 && get(e,"to",2)>31.7) {
                if(get(e,"to",0)<7)longLeft++;
                if(get(e,"from",0)>9)longRight++;
            }
        }
        assertTrue(maxX-minX<11,"Twin cutting blades may fan apart, but the total width must stay compact");
        assertTrue(maxY-minY<9,"Receiver should be vertically slim");
        assertTrue(longLeft>=1&&longRight>=1,"Both emerald prongs must reach the far tip");
        JsonObject display=model.getAsJsonObject("display");
        for(String hand:new String[]{"firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand"}){
            JsonObject pose=display.getAsJsonObject(hand);
            assertEquals(0,pose.getAsJsonArray("rotation").get(0).getAsInt(),
                    "Barrel must point in front of the hand, not downward into the ground");
            assertTrue(pose.getAsJsonArray("translation").get(2).getAsDouble()<=-12,
                    "Gun must be mounted by its rear stock/grip, not the front emitter");
            assertTrue(pose.getAsJsonArray("scale").get(0).getAsDouble()<1.0,
                    "Large gun must not engulf the character");
        }
        JsonObject source=JsonParser.parseString(Files.readString(
                Path.of("models/gn_sword_v_buster_rifle.bbmodel"))).getAsJsonObject();
        assertEquals(elems.size(),source.getAsJsonArray("elements").size());
    }

    @Test void bothGnBladeTipsNarrowTowardTheirExtremities() throws Exception {
        JsonObject original=JsonParser.parseString(Files.readString(
                Path.of("models/gn_sword_v_buster_rifle.bbmodel"))).getAsJsonObject();
        JsonArray elements=original.getAsJsonArray("elements");
        for(String side:new String[]{"L","R"}){
            double previous=Double.POSITIVE_INFINITY;
            int count=0;
            for(var item:elements){
                JsonObject element=item.getAsJsonObject();
                String name=element.get("name").getAsString();
                if(!name.startsWith("GN emerald tapered cutting edge "+side+" "))continue;
                double width=get(element,"to",0)-get(element,"from",0);
                assertTrue(width<previous,"Each later green blade section must taper toward its tip");
                previous=width;
                count++;
            }
            assertEquals(20,count,"Each GN blade must be a long twenty-section taper");
        }
    }

}
