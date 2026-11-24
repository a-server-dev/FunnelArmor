package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure asset and integration regressions, no Paper server required. */
class GundamHeavyWeaponsTest {
    private static final List<String> MODELS=List.of(
        "gp02_atomic_bazooka","gp02_atomic_shield","barbatos_ultra_mace",
        "barbatos_ultra_mace_pile",
        "gx_twin_satellite_controller","gx_satellite_reflector",
        "gx_satellite_barrel");

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void allSixModelsHaveCompleteEditableAndPackedGeometry() throws Exception {
        for(String name:MODELS) {
            JsonObject source=read(Path.of("models",name+".bbmodel"));
            JsonObject packed=read(Path.of("resourcepack","assets","funnel",
                "models","item",name+".json"));
            JsonObject item=read(Path.of("resourcepack","assets","funnel",
                "items",name+".json"));
            assertEquals("funnel:item/"+name,
                item.getAsJsonObject("model").get("model").getAsString());
            assertEquals("funnel:item/heavy_weapon_palette",
                packed.getAsJsonObject("textures").get("0").getAsString());
            var original=source.getAsJsonArray("elements");
            var exported=packed.getAsJsonArray("elements");
            assertEquals(original.size(),exported.size(),name+" model content differs");
            assertTrue(original.size()>=20,name+" must use real detailed geometry");
            assertFalse(source.getAsJsonArray("outliner").isEmpty());
            assertFalse(source.getAsJsonArray("textures").isEmpty());
            var ids=new HashSet<String>();
            for(int i=0;i<original.size();i++) {
                var e=original.get(i).getAsJsonObject();
                var p=exported.get(i).getAsJsonObject();
                assertTrue(ids.add(e.get("uuid").getAsString()));
                assertEquals(e.get("from"),p.get("from"));
                assertEquals(e.get("to"),p.get("to"));
                for(int axis=0;axis<3;axis++){
                    double low=e.getAsJsonArray("from").get(axis).getAsDouble();
                    double high=e.getAsJsonArray("to").get(axis).getAsDouble();
                    assertTrue(low>=-16 && high<=32 && low<high,name+" invalid bound");
                }
                for(String face:List.of("north","south","east","west","up","down")) {
                    JsonArray uv=e.getAsJsonObject("faces")
                        .getAsJsonObject(face).getAsJsonArray("uv");
                    JsonArray render=p.getAsJsonObject("faces")
                        .getAsJsonObject(face).getAsJsonArray("uv");
                    for(int axis=0;axis<4;axis++)
                        assertEquals(uv.get(axis).getAsDouble()/4,
                            render.get(axis).getAsDouble(),.000001);
                    assertEquals("#0",p.getAsJsonObject("faces")
                        .getAsJsonObject(face).get("texture").getAsString());
                }
            }
        }
    }

    @Test void allThreeAreIncludedInSharedGiveAllCatalogWithNoPerWeaponGive() {
        assertEquals(13,FunnelGiftCatalog.ALL.size());
        for(String type:List.of("atomic","mace","satellite")) {
            assertTrue(FunnelGiftCatalog.ALL.contains(type));
            assertTrue(FunnelGiftCatalog.OPTIONS.contains(type));
        }
        assertEquals(13,new HashSet<>(FunnelGiftCatalog.ALL).size());
    }

    @Test void atomicShieldAndSatelliteReflectorsAreSeparateVisualModels() throws Exception {
        var bazooka=read(Path.of("models","gp02_atomic_bazooka.bbmodel"));
        var shield=read(Path.of("models","gp02_atomic_shield.bbmodel"));
        var mace=read(Path.of("models","barbatos_ultra_mace.bbmodel"));
        var panels=read(Path.of("models","gx_satellite_reflector.bbmodel"));
        var cannon=read(Path.of("models","gx_satellite_barrel.bbmodel"));
        assertTrue(bazooka.getAsJsonArray("elements").size()>70);
        assertTrue(shield.getAsJsonArray("elements").size()>55);
        assertTrue(mace.getAsJsonArray("elements").size()>65);
        assertTrue(panels.getAsJsonArray("elements").size()>=20);
        assertTrue(cannon.getAsJsonArray("elements").size()>=30);
    }

    @Test void energyBlastUsesNoBlockMutationAndAllVisualsAreCleaned() throws Exception {
        String code=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/GundamHeavyWeapons.java"));
        assertTrue(code.contains("private void launchAtomic(Player p)"));
        assertTrue(code.contains("private void atomicBlast(Player shooter,Location origin)"));
        assertTrue(code.contains("private void slam(Player p)"));
        assertTrue(code.contains("private void fireSatellite(Player p)"));
        assertTrue(code.contains("private void pileBunker(Player p)"));
        assertTrue(code.contains("private void restoreMace(Player player)"));
        assertTrue(code.contains("new Charge(kind,itemId,p.getWorld().getUID(),now)"));
        assertTrue(code.contains("charging.remove(playerId)"));
        assertTrue(code.contains("v.reflectors.clear();v.barrels.clear();"));
        assertTrue(code.contains("v.reflectors.size()!=6 || v.barrels.size()!=2"));
        assertTrue(code.contains("new org.bukkit.scheduler.BukkitRunnable()"));
        assertTrue(code.contains("w.rayTraceBlocks("));
        assertTrue(code.contains("target.damage("));
        assertFalse(code.contains("createExplosion("));
        assertFalse(code.contains("setType(Material.AIR)"));
        assertFalse(code.contains("new TNTPrimed"));
        assertFalse(code.contains("spawnEntity("));
    }

    @Test void chargingAndControlsMatchThreeDistinctWeaponTypes() throws Exception {
        String code=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/GundamHeavyWeapons.java"));
        assertTrue(code.contains("ATOMIC.equals(type)&&right"));
        assertTrue(code.contains("MACE.equals(type)&&right"));
        assertTrue(code.contains("SATELLITE.equals(type)"));
        assertTrue(code.contains("if(left||p.isSneaking())toggle(p);"));
        assertTrue(code.contains("else begin(p,SATELLITE);"));
        assertTrue(code.contains("if(!c.kind.equals(kind(p))") ||
            code.contains("||!c.kind.equals(kind(p))"));
        assertTrue(code.contains("p.getWorld().playSound("));
    }
}
