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

class GnReferenceWeaponModelsTest {
    private Map<String,byte[]> pack() throws Exception {
        Map<String,byte[]> entries=new HashMap<>();
        try(InputStream in=getClass().getResourceAsStream("/funnel-pack.zip")){
            assertNotNull(in);
            try(ZipInputStream zip=new ZipInputStream(in)){
                ZipEntry e;
                while((e=zip.getNextEntry())!=null){
                    entries.put(e.getName(),zip.readAllBytes());
                    zip.closeEntry();
                }
            }
        }
        return entries;
    }
    private JsonObject model(Map<String,byte[]> entries,String key){
        byte[] bytes=entries.get("assets/funnel/models/item/"+key+".json");
        assertNotNull(bytes,key);
        return JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private double v(JsonObject el,String key,int i){
        return el.getAsJsonArray(key).get(i).getAsDouble();
    }
    @Test void eachWeaponHasEditableModelAndValidGeometry() throws Exception {
        Map<String,byte[]> entries=pack();
        for(String id:new String[]{"gn_sword_ii_sword","gn_sword_ii_rifle","gn_sword_v_sword","gn_sword_v_rifle","gn_sword_v_buster_sword","gn_sword_v_buster_rifle"}){
            JsonObject game=model(entries,id);
            JsonArray boxes=game.getAsJsonArray("elements");
            assertTrue(boxes.size()>=40,id);
            JsonObject source=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            JsonArray original=source.getAsJsonArray("elements");
            assertEquals(boxes.size(),original.size(),id);
            for(int i=0;i<boxes.size();i++){
                JsonObject e=boxes.get(i).getAsJsonObject(),bb=original.get(i).getAsJsonObject();
                assertEquals(e.get("from"),bb.get("from"));
                assertEquals(e.get("to"),bb.get("to"));
                for(int k=0;k<3;k++)
                    assertTrue(v(e,"from",k)>=-16 && v(e,"to",k)<=32 && v(e,"from",k)<v(e,"to",k),id+" cube "+i);
            }
            assertNotNull(entries.get("assets/funnel/items/"+id+".json"),id+" item definition");
            String texture=game.getAsJsonObject("textures").get("0").getAsString();
            assertTrue(texture.startsWith("funnel:item/"),id+" namespace");
            String tex=texture.substring("funnel:item/".length());
            assertNotNull(entries.get("assets/funnel/textures/item/"+tex+".png"),id+" texture");
        }
    }
    @Test void QanTSwordAndDoubleOSwordsAreIndependentAndMappedCorrectly() throws Exception {
        Map<String,byte[]> e=pack();
        JsonObject v=model(e,"gn_sword_v_sword");
        JsonObject ii=model(e,"gn_sword_ii_sword");
        JsonObject rifle=model(e,"gn_sword_ii_rifle");
        assertNotEquals(v.getAsJsonArray("elements").size(),ii.getAsJsonArray("elements").size());
        assertEquals(ii.getAsJsonArray("elements").size(),rifle.getAsJsonArray("elements").size());
        String vcode=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GnSwordCombat.java"));
        String iicode=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GnSwordIICombat.java"));
        assertTrue(vcode.contains("gn_sword_v_weapon"));
        assertTrue(iicode.contains("gn_sword_ii_weapon"));
        assertFalse(vcode.contains("updateLeftSword("),"Qan[T] must be single-handed");
        assertTrue(iicode.contains("updateLeftSword("),"Double-O must display two");
        assertTrue(iicode.contains("dualVisualMarker"));
        assertFalse(iicode.contains("setItemInOffHand("),"Do not overwrite player offhand");
        String plugin=Files.readString(Path.of("src/main/java/dev/emerald/funnel/FunnelArmorPlugin.java"));
        assertTrue(plugin.contains("gnSwordIICombat.start()"));
        assertTrue(plugin.contains("equalsIgnoreCase(\"gn2\")"));
    }
    @Test void QanTBusterHasTwoLongTaperedEmeraldTips() throws Exception {
        JsonObject source=JsonParser.parseString(Files.readString(
                Path.of("models/gn_sword_v_buster_rifle.bbmodel"))).getAsJsonObject();
        JsonArray elements=source.getAsJsonArray("elements");
        for(String side:new String[]{"L","R"}){
            double lastWidth=Double.POSITIVE_INFINITY;
            int count=0,tip=0;
            for(var item:elements){
                JsonObject el=item.getAsJsonObject();
                String name=el.get("name").getAsString();
                if(!name.startsWith("GN emerald tapered cutting edge "+side+" "))continue;
                double width=v(el,"to",0)-v(el,"from",0);
                assertTrue(width<lastWidth,"blade becomes sharper toward tip");
                lastWidth=width;
                count++;
                if(v(el,"to",2)>31.6)tip++;
            }
            assertEquals(20,count);
            assertTrue(tip>=1);
        }
    }
}