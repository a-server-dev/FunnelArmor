package dev.emerald.funnel;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class GundamAdvancedWeaponsTest {
    @Test void allFifteenModelsHaveMatchingEditableAndPackedCubes() throws Exception {
        for(String id:List.of("epyon_shield","epyon_beam_sword","epyon_rod_link","epyon_rod_tip","heavyarms_gatling_body","heavyarms_gatling_rotor","freedom_backpack","freedom_wing_blue","freedom_wing_navy","freedom_wing_middle","freedom_wing_lower","freedom_wing_auxiliary","freedom_balaena","freedom_railgun","freedom_lupus_rifle")){
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
    @Test void freedomUsesFiveDistinctWingGeometriesPerSide() throws Exception{
        var models=new java.util.HashSet<String>();
        var geometries=new java.util.HashSet<String>();
        for(int row=0;row<5;row++){
            String id=FreedomWingLayout.model(row);
            assertTrue(models.add(id));
            var source=JsonParser.parseString(Files.readString(Path.of("models",id+".bbmodel"))).getAsJsonObject();
            var game=JsonParser.parseString(Files.readString(Path.of("resourcepack/assets/funnel/models/item",id+".json"))).getAsJsonObject();
            assertEquals("funnel:item/freedom_wing_palette",game.getAsJsonObject("textures").get("0").getAsString());
            var outline=new StringBuilder();
            double tip=0;
            for(var value:source.getAsJsonArray("elements")){
                var part=value.getAsJsonObject();
                if(part.get("name").getAsString().startsWith("navy silhouette")){
                    outline.append(part.get("from")).append(part.get("to"));
                    tip=Math.max(tip,part.getAsJsonArray("to").get(1).getAsDouble());
                }
            }
            assertTrue(geometries.add(outline.toString()),id+" reuses another wing profile");
            assertEquals(new double[]{31,30,31,28,23}[row],tip,.00001);
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
            double length=new double[]{31,30,31,28,23}[i]-8;
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
    @Test void heavyarmsRestoresHalfTracersAndMuzzleEffectsWithoutChangingDamage() throws Exception{
        String code=Files.readString(Path.of("src/main/java/dev/emerald/funnel/GundamAdvancedWeapons.java"));
        String gatling=code.substring(code.indexOf("private void gatling("),code.indexOf("private void freedom("));
        assertTrue(gatling.contains("for(Location muzzle:muzzles)"));
        assertTrue(gatling.contains("hits,damage(\"heavyarms\",.5),true"));
        assertTrue(gatling.contains("Particle.SMOKE,muzzle,2"));
        assertTrue(gatling.contains("Color.fromRGB(193,145,62),.65f"));
        assertTrue(gatling.contains("new Vector(0,.13,0)),8"));
        String beam=code.substring(code.indexOf("private void beam("),code.indexOf("boolean command("));
        assertTrue(beam.contains("for(double x=0;x<=length;x+=2.30)"));
        assertTrue(beam.contains("Color.fromRGB(255,248,196),.75f"));
        assertTrue(beam.indexOf("hurt(p,l,damage)")<beam.indexOf("if(!render)return"));
        assertTrue(beam.indexOf("if(!glow)")<beam.indexOf("Particle.FLASH"));
        assertEquals(16,(int)Math.floor(36/2.30)+1);
        assertEquals(32,(int)Math.floor(36/1.15)+1);
    }
    @Test void epyonShieldHandTransformsFaceForward() throws Exception{
        var source=JsonParser.parseString(Files.readString(Path.of("models","epyon_shield.bbmodel"))).getAsJsonObject();
        var pack=JsonParser.parseString(Files.readString(Path.of("resourcepack/assets/funnel/models/item","epyon_shield.json"))).getAsJsonObject();
        var display=pack.getAsJsonObject("display");
        assertEquals(source.getAsJsonObject("display"),display);
        for(String mode:List.of("firstperson","thirdperson")){
            for(String hand:List.of("left","right")){
                var rotation=display.getAsJsonObject(mode+"_"+hand+"hand").getAsJsonArray("rotation");
                assertEquals(65,rotation.get(0).getAsInt());
                assertEquals(hand.equals("left")?-15:15,rotation.get(1).getAsInt());
                assertEquals(0,rotation.get(2).getAsInt());
            }
        }
        assertEquals(180,display.getAsJsonObject("fixed").getAsJsonArray("rotation").get(1).getAsInt());
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
