package dev.emerald.funnel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for the design-refinement pass (GP02 / Rex / Double X). */
class GundamHeavyWeaponDetailTest {
    private static JsonObject model(String id) throws Exception {
        return JsonParser.parseString(Files.readString(
            Path.of("models",id+".bbmodel"))).getAsJsonObject();
    }

    private static boolean hasPart(String id,String name) throws Exception {
        for(var e:model(id).getAsJsonArray("elements"))
            if(e.getAsJsonObject().get("name").getAsString().equals(name))return true;
        return false;
    }

    @Test void gp02ShieldIncludesWhiteHexagonalRadiatorAndRedEmblem() throws Exception {
        String name="gp02_atomic_shield";
        assertTrue(model(name).getAsJsonArray("elements").size()>125);
        for(int i=0;i<7;i++)
            assertTrue(hasPart(name,"Hexagonal white main shield plate "+i));
        for(int i=0;i<8;i++){
            assertTrue(hasPart(name,"GP02 red V emblem left swept wing "+i));
            assertTrue(hasPart(name,"GP02 red V emblem right swept wing "+i));
        }
        assertTrue(hasPart(name,"Rear bazooka integrated stowage rail"));
        assertTrue(hasPart(name,"Removable barrel securing clamp 10"));
        assertTrue(hasPart(name,"Port amber cooling shutter 6"));
        assertTrue(hasPart(name,"Starboard amber cooling shutter 6"));
    }

    @Test void oversizedMaceHasActivePileModelWithTrueExtendedTip() throws Exception {
        String id="barbatos_ultra_mace";
        String active="barbatos_ultra_mace_pile";
        assertTrue(hasPart(id,"Silver retracted central pile rod"));
        assertTrue(hasPart(active,"Piled penetrating tungsten sharp tip 5"));
        assertTrue(hasPart(active,"Actuated six-sided reinforcing collar"));
        double folded=maximumY(id);
        double extended=maximumY(active);
        assertTrue(extended-folded>2.0,"Pile tip must visibly extend beyond the mace head");
        assertTrue(model(active).getAsJsonArray("elements").size()
            >model(id).getAsJsonArray("elements").size());
    }

    @Test void maceHasFourFlatRadialBladesAndIndependentTeeth() throws Exception {
        for(String id:List.of("barbatos_ultra_mace","barbatos_ultra_mace_pile")){
            var m=model(id);
            var packed=JsonParser.parseString(Files.readString(Path.of(
                "resourcepack/assets/funnel/models/item",id+".json"))).getAsJsonObject();
            assertTrue(m.getAsJsonArray("elements").size()<150,"Avoid rounded stepped profile");
            for(String direction:List.of("East","West","North","South")){
                for(String part:List.of("long continuous main plate","long straight cutting edge",
                        "fork diagonal outer edge","root guard outer swept edge",
                        "root guard outer tip","tooth 1 flat face","tooth 2 flat face"))
                    assertTrue(hasPart(id,direction+" "+part),direction+" "+part);
            }
            double[] lo={99,99,99},hi={-99,-99,-99};
            int rotated=0;
            for(int i=0;i<m.getAsJsonArray("elements").size();i++){
                var cube=m.getAsJsonArray("elements").get(i).getAsJsonObject();
                var client=packed.getAsJsonArray("elements").get(i).getAsJsonObject();
                for(int a=0;a<3;a++){
                    lo[a]=Math.min(lo[a],cube.getAsJsonArray("from").get(a).getAsDouble());
                    hi[a]=Math.max(hi[a],cube.getAsJsonArray("to").get(a).getAsDouble());
                }
                if(cube.has("rotation")){
                    rotated++;
                    var rot=client.getAsJsonObject("rotation");
                    assertNotNull(rot);
                    assertEquals(cube.get("origin"),rot.get("origin"));
                    int axis=List.of("x","y","z").indexOf(rot.get("axis").getAsString());
                    assertTrue(axis>=0);
                    double angle=rot.get("angle").getAsDouble();
                    assertTrue(Math.abs(angle)==22.5 || Math.abs(angle)==45);
                    assertEquals(angle,cube.getAsJsonArray("rotation").get(axis).getAsDouble(),.000001);
                }
                if(cube.get("name").getAsString().endsWith("long continuous main plate")){
                    assertTrue(cube.getAsJsonArray("to").get(1).getAsDouble()
                         -cube.getAsJsonArray("from").get(1).getAsDouble()>13);
                    double x=cube.getAsJsonArray("to").get(0).getAsDouble()-cube.getAsJsonArray("from").get(0).getAsDouble();
                    double z=cube.getAsJsonArray("to").get(2).getAsDouble()-cube.getAsJsonArray("from").get(2).getAsDouble();
                    assertTrue(Math.min(x,z)<.35 && Math.max(x,z)>1.1,"Flat plate, not thick petal");
                }
            }
            assertEquals(8,rotated,"Four true diagonal forks and four swept guards");
            assertEquals(hi[0]-lo[0],hi[2]-lo[2],.0001,"Cross must not be flattened");
            assertTrue(hi[0]-lo[0]>6 && hi[0]-lo[0]<7 && hi[1]-lo[1]>45);
        }
    }

    private static double maximumY(String id) throws Exception {
        double maximum=-99;
        for(var e:model(id).getAsJsonArray("elements"))maximum=Math.max(
            maximum,e.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
        return maximum;
    }

    @Test void dxReflectorHasSixPetalModelAndHingedRotationLogic() throws Exception {
        assertTrue(model("gx_satellite_reflector").getAsJsonArray("elements").size()>130);
        assertTrue(hasPart("gx_satellite_reflector","Reflector golden faceted inset wing 4"));
        assertTrue(hasPart("gx_satellite_reflector","Precise reflectance hexagon dot 45"));
        assertTrue(hasPart("gx_satellite_reflector","Rear radiator support black fold-out rib 6"));
        assertTrue(model("gx_satellite_barrel").getAsJsonArray("elements").size()>80);
        assertTrue(hasPart("gx_satellite_barrel","Long ceramic-white upper energy exchanger"));
        assertTrue(hasPart("gx_satellite_barrel","Emerald core emitting focus"));
        String src=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/GundamHeavyWeapons.java"));
        assertTrue(src.contains("v.reflectors.size()!=6 || v.barrels.size()!=2"));
        assertTrue(src.contains("rotateZ(roll).rotateX(hingePitch)"));
        assertTrue(src.contains("new Quaternionf().rotateX(aim)"));
        assertTrue(src.contains("axis.multiply(DX_MUZZLE_DISTANCE)") && src.contains("DX_MUZZLE_DISTANCE = 1.70"),
            "Beam must emerge from actual animated muzzle tip, not inside cannon");
        assertTrue(src.contains("setY(-Math.sin(pitch))"),
            "Barrel muzzle must follow player look pitch");
        assertTrue(src.contains("1.57+.18*p"));
        assertTrue(src.contains("Color.fromRGB(255,225,93)"));
        assertTrue(src.contains("backpackVisual,1.08f"));
        assertTrue(src.contains("satelliteMuzzle(barrel,v.barrelPitch)"));
        assertTrue(hasPart("gx_twin_satellite_controller","Backpack central frame"));
    }

    @Test void uniquePackedPaletteIsAReal64SquareAndContainsRedGoldWhiteAndCyan() throws Exception {
        boolean found=false;
        try(var stream=getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try(var zip=new ZipInputStream(stream)) {
                ZipEntry e;
                while((e=zip.getNextEntry())!=null) {
                    if(e.getName().equals("assets/funnel/textures/item/heavy_weapon_palette.png")) {
                        found=true;
                        var img=ImageIO.read(new ByteArrayInputStream(zip.readAllBytes()));
                        assertNotNull(img);
                        assertEquals(64,img.getWidth());
                        assertEquals(64,img.getHeight());
                        int white=img.getRGB(24,24);
                        assertTrue(((white>>16)&255)>220);
                        int gold=img.getRGB(40,40);
                        assertTrue(((gold>>16)&255)>180 && ((gold>>8)&255)>120);
                        int red=img.getRGB(8,56);
                        assertTrue(((red>>16)&255)>155 && ((red>>8)&255)<100);
                        int cyan=img.getRGB(40,56);
                        assertTrue(((cyan>>8)&255)>160 && (cyan&255)>175);
                    }
                    zip.closeEntry();
                }
            }
        }
        assertTrue(found,"Dedicated color atlas must be shipped inside built JAR");
    }

    @Test void controlsKeepOldActionsAndAddSafePileBunkerWithCompleteCleanup() throws Exception {
        String src=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/GundamHeavyWeapons.java"));
        assertTrue(src.contains("if(p.isSneaking())pileBunker(p);"));
        assertTrue(src.contains("else slam(p);"));
        assertTrue(src.contains("restoreMace(e.getPlayer())"));
        assertTrue(src.contains("restoreMace(e.getEntity())"));
        assertTrue(src.contains("restoreMace(player);"));
        assertTrue(src.contains("mace.pile-cooldown-ticks"));
        assertTrue(src.contains("world.rayTraceBlocks(eye,ray,reach,"));
        assertTrue(src.contains("damage,shooter"));
        assertTrue(src.contains("rayTraceBlocks("));
        assertFalse(src.contains("createExplosion("));
        assertFalse(src.contains("setType(Material.AIR)"));
    }
}
