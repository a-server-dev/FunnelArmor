package dev.emerald.funnel;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class GundamAdvancedWeaponsTest {
    @Test void allTwelveModelsHaveMatchingEditableAndPackedCubes() throws Exception {
        for(String id:List.of("epyon_shield","epyon_beam_sword","epyon_rod_link","epyon_rod_tip","heavyarms_gatling_body","heavyarms_gatling_rotor","freedom_backpack","freedom_wing_blue","freedom_wing_navy","freedom_balaena","freedom_railgun","freedom_lupus_rifle")){
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
    @Test void wingsHaveLongAngularArmorAndBrightIntegratedPanels() throws Exception{
        for(String id:List.of("freedom_wing_blue","freedom_wing_navy")){
            var model=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            int edges=0,blue=0;double tip=0;
            for(var value:model.getAsJsonArray("elements")){
                var part=value.getAsJsonObject();var name=part.get("name").getAsString();
                assertFalse(name.startsWith("long pointed edge"));
                if(name.startsWith("integrated diagonal tip edge")){
                    edges++;
                    assertEquals(22.5,Math.abs(part.getAsJsonArray("rotation").get(2).getAsDouble()),.00001);
                }
                if(name.startsWith("blue armor face"))blue++;
                tip=Math.max(tip,part.getAsJsonArray("to").get(1).getAsDouble());
            }
            assertEquals(2,edges);assertTrue(blue>=8);assertTrue(tip>=29.5);
            var game=JsonParser.parseString(Files.readString(Path.of("resourcepack/assets/funnel/models/item",id+".json"))).getAsJsonObject();
            assertEquals("funnel:item/freedom_wing_palette",game.getAsJsonObject("textures").get("0").getAsString());
        }
        var palette=javax.imageio.ImageIO.read(Path.of("resourcepack/assets/funnel/textures/item/freedom_wing_palette.png").toFile());
        assertNotNull(palette);assertEquals(64,palette.getWidth());assertEquals(64,palette.getHeight());
        assertEquals(0x387de7,palette.getRGB(40,24)&0xffffff);
    }
    @Test void freedomOpenWingsFormWideLayeredSilhouetteAndStowAboveGround(){
        double maxX=0,maxY=0,lastY=10,lastZ=-10;
        for(int i=0;i<5;i++){
            var open=FreedomWingLayout.pose(i,1,0);
            var full=FreedomWingLayout.pose(i,1,1);
            var closed=FreedomWingLayout.pose(i,0,0);
            double length=(i%2==0?31.0:30.0)-8;
            double radius=length*full.scale()/16;
            maxX=Math.max(maxX,full.x()+radius*Math.sin(Math.toRadians(full.roll()))*Math.cos(Math.toRadians(full.sweep())));
            maxY=Math.max(maxY,full.y()+radius*Math.cos(Math.toRadians(full.roll())));
            assertTrue(open.y()<lastY);assertTrue(open.z()>lastZ);
            lastY=open.y();lastZ=open.z();
            assertTrue(closed.y()+length*closed.scale()/16*Math.cos(Math.toRadians(closed.roll()))>.15);
        }
        assertTrue(maxX>2.6,"over five blocks wide");
        assertTrue(maxY>3.0,"upper wing clears head");
        assertTrue(FreedomWingLayout.pose(0,1,0).scale()>FreedomWingLayout.pose(4,1,0).scale());
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
    @Test void epyonUsesActualShieldAndPersistentOffhandEscrow() throws Exception{
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/EpyonOffhand.java"));
        assertTrue(code.contains("new ItemStack(Material.SHIELD)"));
        assertTrue(code.contains("original.serializeAsBytes()"));
        assertTrue(code.contains("ItemStack.deserializeBytes(bytes)"));
        assertTrue(code.contains("PersistentDataType.BYTE_ARRAY"));
        assertTrue(code.contains("if(slot<0)return"));
        assertTrue(code.contains("e.getDrops().removeIf(this::temporary)"));
        assertTrue(code.contains("ClickType.SWAP_OFFHAND"));
        String combat=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(combat.contains("swordAttack(p)"));
        assertTrue(combat.contains("if(p.isSneaking())"));
        assertTrue(combat.contains("epyonOffhand.restore(p)"));
        assertTrue(combat.contains("if(!epyonOffhand.equipped(p))"));
    }
    @Test void combatUsesPhysicalSegmentsFourRotorsTenWingsAndFiveGuns() throws Exception{
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        assertTrue(code.contains("ROD_LINKS=14, ROTORS=4, WINGS=10"));
        assertTrue(code.contains("new int[]{11,12,13,14,15}"));
        assertTrue(code.contains("rayTraceBlocks"));
        assertTrue(code.contains("s.freedomMode.fullBurst()"));
        assertTrue(code.contains("s.cannonProgress<.98"));
        assertTrue(code.contains("epyonOffhand.equip(p)"));
        assertFalse(code.contains("s.parts.add(spawn(p,\"epyon_shield\"))"));
        assertTrue(code.contains("rayTraceEntities"));
        assertTrue(code.contains("hit.add(l.getUniqueId())"));
        assertTrue(code.contains("s.overheated=true"));
        assertTrue(code.contains("d.setPersistent(false)"));
        assertFalse(code.contains("createExplosion("));
    }
}
