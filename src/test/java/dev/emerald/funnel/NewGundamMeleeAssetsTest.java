package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Source/pack parity, reproducible client model safety, and blade geometry contracts. */
class NewGundamMeleeAssetsTest {
    private static final List<String> IDS = List.of(
            "beam_saber_0","beam_saber_1","beam_saber_2","beam_saber_3",
            "beam_saber_4","heat_hawk_off","heat_hawk_on");

    private static Map<String, byte[]> pack() throws Exception {
        Map<String, byte[]> data = new HashMap<>();
        try (InputStream stream = NewGundamMeleeAssetsTest.class.getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    data.put(entry.getName(), zip.readAllBytes());
                    zip.closeEntry();
                }
            }
        }
        return data;
    }

    private static JsonObject asJson(byte[] data) {
        assertNotNull(data);
        return JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject model(Map<String,byte[]> data, String id) {
        return asJson(data.get("assets/funnel/models/item/" + id + ".json"));
    }

    @Test void sevenModelsAndTheirEditableBlockbenchSourcesMatch() throws Exception {
        Map<String,byte[]> zip = pack();
        for (String id : IDS) {
            JsonObject packed = model(zip,id);
            JsonObject source = JsonParser.parseString(Files.readString(
                    Path.of("models", id+".bbmodel"))).getAsJsonObject();
            JsonArray modelElements = packed.getAsJsonArray("elements");
            JsonArray sourceElements = source.getAsJsonArray("elements");
            assertEquals(modelElements.size(), sourceElements.size(), id);
            for (int k=0;k<modelElements.size();k++) {
                JsonObject m = modelElements.get(k).getAsJsonObject();
                JsonObject bb = sourceElements.get(k).getAsJsonObject();
                assertEquals(m.get("from"),bb.get("from"),id+" cube "+k);
                assertEquals(m.get("to"),bb.get("to"),id+" cube "+k);
                for (int axis=0;axis<3;axis++) {
                    double lo=m.getAsJsonArray("from").get(axis).getAsDouble();
                    double hi=m.getAsJsonArray("to").get(axis).getAsDouble();
                    assertTrue(lo>=-16&&lo<hi&&hi<=32,id+" invalid axis "+axis);
                }
                for (var f : m.getAsJsonObject("faces").entrySet())
                    assertEquals("#0",f.getValue().getAsJsonObject().get("texture").getAsString());
            }
            String tex=id.startsWith("beam_saber_")?"beam_saber":id;
            assertEquals("funnel:item/"+tex,packed.getAsJsonObject("textures").get("0").getAsString());
            assertNotNull(zip.get("assets/funnel/items/"+id+".json"));
        }
    }

    @Test void saberHiltAndFourBladeGrowthStagesHaveIncreasingTipHeights() throws Exception {
        Map<String,byte[]> zip=pack();
        double previous=-1;
        for (int stage=0;stage<=4;stage++) {
            JsonArray cubes=model(zip,"beam_saber_"+stage).getAsJsonArray("elements");
            double max=Double.NEGATIVE_INFINITY;
            for (var el:cubes)
                max=Math.max(max,el.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
            assertTrue(max>previous,"Saber blade must visibly extend from stage "+(stage-1));
            if (stage==0) assertTrue(max<12,"Retracted model must contain only handle");
            if (stage==4) assertTrue(max>30,"Final blade must reach full length");
            previous=max;
        }
    }

    @Test void coldAndHotHeatHawkHaveTheSamePhysicalOutline() throws Exception {
        Map<String,byte[]> zip=pack();
        JsonArray off=model(zip,"heat_hawk_off").getAsJsonArray("elements");
        JsonArray on=model(zip,"heat_hawk_on").getAsJsonArray("elements");
        assertEquals(off.size(),on.size());
        assertTrue(on.size()>=60,"Heat Hawk needs detailed axe geometry");
        for(int k=0;k<off.size();k++) {
            assertEquals(off.get(k).getAsJsonObject().get("from"),
                    on.get(k).getAsJsonObject().get("from"));
            assertEquals(off.get(k).getAsJsonObject().get("to"),
                    on.get(k).getAsJsonObject().get("to"));
        }
        byte[] texOff=zip.get("assets/funnel/textures/item/heat_hawk_off.png");
        byte[] texOn=zip.get("assets/funnel/textures/item/heat_hawk_on.png");
        assertNotNull(texOff);
        assertNotNull(texOn);
        assertFalse(java.util.Arrays.equals(texOff,texOn));
    }

    @Test void originalWeaponPngsDecodeAt64Pixels() throws Exception {
        Map<String,byte[]> zip=pack();
        for(String id:List.of("beam_saber","heat_hawk_off","heat_hawk_on")) {
            byte[] raw=zip.get("assets/funnel/textures/item/"+id+".png");
            assertNotNull(raw);
            java.awt.image.BufferedImage image=javax.imageio.ImageIO.read(new ByteArrayInputStream(raw));
            assertNotNull(image,"Texture not valid PNG: "+id);
            assertEquals(64,image.getWidth());
            assertEquals(64,image.getHeight());
        }
    }

    @Test void gnBusterSwordIsGeometricallyMirrorSymmetricFrontAndBack() throws Exception {
        JsonArray cubes=model(pack(),"gn_sword_v_buster_sword").getAsJsonArray("elements");
        assertTrue(cubes.size()>=450);
        Set<String> shapes=new HashSet<>();
        for (var item:cubes) {
            JsonObject e=item.getAsJsonObject();
            shapes.add(key(e.getAsJsonArray("from"),e.getAsJsonArray("to"),false));
        }
        for (var item:cubes) {
            JsonObject e=item.getAsJsonObject();
            assertTrue(shapes.contains(key(e.getAsJsonArray("from"),e.getAsJsonArray("to"),true)),
                    "Asymmetric front/back detail found in GN Buster Sword");
        }
    }

    private static String key(JsonArray from, JsonArray to, boolean reversed) {
        double x1=from.get(0).getAsDouble(), x2=to.get(0).getAsDouble();
        double y1=from.get(1).getAsDouble(), y2=to.get(1).getAsDouble();
        double z1=from.get(2).getAsDouble(), z2=to.get(2).getAsDouble();
        if (reversed) { double a=18.44-z2,b=18.44-z1;z1=a;z2=b; }
        return String.format(Locale.ROOT, "%.3f/%.3f/%.3f/%.3f/%.3f/%.3f",
                x1,x2,y1,y2,z1,z2);
    }
}
