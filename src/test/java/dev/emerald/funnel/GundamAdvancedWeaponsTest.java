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
    @Test void wingsHaveNarrowArmorAndIntegratedDiagonalTips() throws Exception{
        for(String id:List.of("freedom_wing_blue","freedom_wing_navy")){
            var model=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            int edges=0;boolean narrow=false;
            for(var value:model.getAsJsonArray("elements")){
                var part=value.getAsJsonObject();
                var name=part.get("name").getAsString();
                assertFalse(name.startsWith("long pointed edge"),"Old detached edge");
                if(name.equals("slender continuous armor")){
                    narrow=true;
                    double width=part.getAsJsonArray("to").get(0).getAsDouble()-part.getAsJsonArray("from").get(0).getAsDouble();
                    assertTrue(width>=3 && width<4);
                }
                if(name.startsWith("integrated diagonal tip edge")){
                    edges++;
                    assertEquals(22.5,Math.abs(part.getAsJsonArray("rotation").get(2).getAsDouble()));
                }
            }
            assertTrue(narrow);assertEquals(2,edges);
        }
    }
    @Test void gatlingReceiversAreVerticalAndCadenceIsThreeTimesFaster() throws Exception{
        var model=JsonParser.parseString(Files.readString(Path.of("models","heavyarms_gatling_body.bbmodel"))).getAsJsonObject();
        var centers=new java.util.ArrayList<Double>();
        for(var value:model.getAsJsonArray("elements")){
            var part=value.getAsJsonObject();
            if(!part.get("name").getAsString().equals("octagonal dark receiver square"))continue;
            var lo=part.getAsJsonArray("from");var hi=part.getAsJsonArray("to");
            assertEquals(8,(lo.get(0).getAsDouble()+hi.get(0).getAsDouble())/2,.00001);
            centers.add((lo.get(1).getAsDouble()+hi.get(1).getAsDouble())/2);
        }
        assertEquals(2,centers.size());
        assertEquals(5.4,centers.get(0),.00001);
        assertEquals(10.6,centers.get(1),.00001);
        int ticks=0;for(int i=0;i<15;i++)ticks+=HeavyarmsCadence.interval(i);
        assertEquals(20,ticks);
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(code.contains("p.hasActiveItem()"));
        assertTrue(code.contains("DataComponentTypes.CONSUMABLE"));
        assertTrue(code.contains("boolean held=holdingTrigger(p,s)"));
        assertTrue(code.contains("if(k.equals(\"heavyarms\"))return;"));
        assertTrue(code.contains("float scale=1.18f;"));
        assertFalse(code.contains("s.firing=!s.firing"));
    }
    @Test void heavyarmsSparseTracersDoNotReduceHitRayCount() throws Exception{
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(code.contains("for(int i=0;i<muzzles.length;i++)"));
        assertTrue(code.contains("visual%2==0&&i==visual/2"));
        assertTrue(code.contains("for(double x=4;x<=length;x+=6)"));
        assertTrue(code.contains("damage(\"heavyarms\",.5)"));
        int start=code.indexOf("private void beam(");
        String beam=code.substring(start,code.indexOf("boolean command(",start));
        assertTrue(beam.indexOf("hurt(p,l,damage)")<beam.indexOf("if(!render)return"));
        assertTrue(beam.indexOf("if(!glow)")<beam.indexOf("Particle.FLASH"));
        assertFalse(code.substring(code.indexOf("private void gatling("),code.indexOf("private void freedom(")).contains("Particle.SMOKE"));
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
