package dev.emerald.funnel;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class GundamAdvancedWeaponsTest {
    @Test void allElevenModelsHaveMatchingEditableAndPackedCubes() throws Exception {
        for(String id:List.of("epyon_shield","epyon_rod_link","epyon_rod_tip","heavyarms_gatling_body","heavyarms_gatling_rotor","freedom_backpack","freedom_wing_blue","freedom_wing_navy","freedom_balaena","freedom_railgun","freedom_lupus_rifle")){
            var source=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            var game=JsonParser.parseString(Files.readString(Path.of("resourcepack/assets/funnel/models/item",id+".json"))).getAsJsonObject();
            var item=JsonParser.parseString(Files.readString(Path.of("resourcepack/assets/funnel/items",id+".json"))).getAsJsonObject();
            assertEquals("funnel:item/"+id,item.getAsJsonObject("model").get("model").getAsString());
            var a=source.getAsJsonArray("elements");var b=game.getAsJsonArray("elements");
            assertTrue(a.size()>=12,id);assertEquals(a.size(),b.size());
            assertEquals(source.get("display"),game.get("display"));
            for(int i=0;i<a.size();i++){
                var x=a.get(i).getAsJsonObject();var y=b.get(i).getAsJsonObject();
                assertEquals(x.get("from"),y.get("from"));assertEquals(x.get("to"),y.get("to"));
                for(int axis=0;axis<3;axis++){
                    double lo=x.getAsJsonArray("from").get(axis).getAsDouble(),hi=x.getAsJsonArray("to").get(axis).getAsDouble();
                    assertTrue(lo>=-16 && hi<=32 && hi>lo,id);
                }
                if(x.has("rotation")){var rot=y.getAsJsonObject("rotation");assertEquals(x.get("origin"),rot.get("origin"));}
            }
        }
    }
    @Test void weaponsAreRegisteredAndGnRemainsUndistributable(){
        assertTrue(FunnelGiftCatalog.ALL.containsAll(List.of("epyon","heavyarms","freedom")));
        assertEquals(14,FunnelGiftCatalog.ALL.size());
        assertFalse(FunnelGiftCatalog.supported("gn"));
        assertFalse(FunnelGiftCatalog.supported("gn2"));
        assertFalse(FunnelGiftCatalog.supported("gnv"));
    }
    @Test void freedomModesSeparateWingAndCannonDeployment(){
        var closed=FreedomMode.CLOSED;
        assertFalse(closed.wingsOpen());
        assertFalse(closed.fullBurst());
        var himat=closed.next(false);
        assertEquals(FreedomMode.HIMAT,himat);
        assertTrue(himat.wingsOpen());
        assertFalse(himat.fullBurst());
        assertEquals(closed,himat.next(false));
        assertEquals(himat,closed.next(true));
        var full=himat.next(true);
        assertEquals(FreedomMode.FULL_BURST,full);
        assertTrue(full.wingsOpen());
        assertTrue(full.fullBurst());
        assertEquals(himat,full.next(true));
        assertEquals(closed,full.next(false));
    }
    @Test void wingsHaveNoDetachedEdgeSpikesAndWideMainPanels() throws Exception{
        for(String id:List.of("freedom_wing_blue","freedom_wing_navy")){
            var model=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            boolean wide=false;
            for(var value:model.getAsJsonArray("elements")){
                var part=value.getAsJsonObject();
                var name=part.get("name").getAsString();
                assertFalse(name.startsWith("long pointed edge"),"Old detached edge");
                if(name.equals("broad uninterrupted armor")){
                    wide=true;
                    assertTrue(part.getAsJsonArray("to").get(0).getAsDouble()-part.getAsJsonArray("from").get(0).getAsDouble()>=4.5);
                    assertTrue(part.getAsJsonArray("to").get(1).getAsDouble()-part.getAsJsonArray("from").get(1).getAsDouble()>=8);
                }
            }
            assertTrue(wide);
        }
    }
    @Test void combatUsesPhysicalSegmentsFourRotorsTenWingsAndFiveGuns() throws Exception{
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(code.contains("ROD_LINKS=14, ROTORS=4, WINGS=10"));
        assertTrue(code.contains("new int[]{11,12,13,14,15}"));
        assertTrue(code.contains("rayTraceBlocks"));
        assertTrue(code.contains("s.freedomMode.fullBurst()"));
        assertTrue(code.contains("s.cannonProgress<.98"));
        assertTrue(code.contains("new Quaternionf().rotateY((float)Math.PI)"));
        assertTrue(code.contains("(-5.3-8)*.94/16"));
        assertTrue(code.contains("rayTraceEntities"));
        assertTrue(code.contains("hit.add(l.getUniqueId())"));
        assertTrue(code.contains("s.overheated=true"));
        assertTrue(code.contains("d.setPersistent(false)"));
        assertFalse(code.contains("createExplosion("));
    }
}
