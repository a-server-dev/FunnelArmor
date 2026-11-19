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

    @Test void crescentBladeBulgesAtCenterAndHasHoseAndMechanicalHub() throws Exception {
        JsonArray cold = model(pack(), "heat_hawk_off").getAsJsonArray("elements");
        JsonArray hot = model(pack(), "heat_hawk_on").getAsJsonArray("elements");
        assertTrue(cold.size() >= 260, "Reference heat hawk requires a detailed head and crescent");
        assertEquals(cold.size(), hot.size(), "Heat never changes the underlying axe mesh");
        double endBottom = widestEdgeAt(cold, 18.2);
        double middle = widestEdgeAt(cold, 25.1);
        double endTop = widestEdgeAt(cold, 30.9);
        assertTrue(middle > endBottom + 3, "Wide crescent middle should bulge rightward");
        assertTrue(middle > endTop + 3, "Upper crescent must taper back inward");
        JsonObject src = JsonParser.parseString(Files.readString(
                Path.of("models/heat_hawk_on.bbmodel"))).getAsJsonObject();
        java.util.Set<String> groups = new HashSet<>();
        for (var entry : src.getAsJsonArray("outliner")) {
            JsonObject group = entry.getAsJsonObject();
            groups.add(group.get("name").getAsString());
            assertTrue(group.getAsJsonArray("children").size() > 0);
        }
        assertTrue(groups.containsAll(List.of("Crescent heat blade",
                "Circular GN hinge", "External hose loop")), "Axe must retain reference features");
        int distinctFaceColors = 0;
        for (int i = 0; i < cold.size(); i++) {
            JsonObject a = cold.get(i).getAsJsonObject();
            JsonObject z = hot.get(i).getAsJsonObject();
            assertEquals(a.get("from"), z.get("from"), "Cold/hot geometry mismatch");
            assertEquals(a.get("to"), z.get("to"), "Cold/hot geometry mismatch");
            if (!a.getAsJsonObject("faces").getAsJsonObject("south").get("uv")
                    .equals(z.getAsJsonObject("faces").getAsJsonObject("south").get("uv")))
                distinctFaceColors++;
        }
        assertTrue(distinctFaceColors >= 48,
                "Hot cutting edge needs distinct orange-to-white colors, not a uniform overlay");
    }

    private static double widestEdgeAt(JsonArray cubes, double y) {
        double xmax = Double.NEGATIVE_INFINITY;
        for (var entry : cubes) {
            JsonObject e = entry.getAsJsonObject();
            JsonArray from = e.getAsJsonArray("from"), to = e.getAsJsonArray("to");
            if (from.get(1).getAsDouble() < y && to.get(1).getAsDouble() > y)
                xmax = Math.max(xmax, to.get(0).getAsDouble());
        }
        return xmax;
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
        assertTrue(cubes.size()>=420);
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

    @Test void silverAndEmeraldBladeFacingIsIdenticalFrontAndBack() throws Exception {
        JsonArray elements = model(pack(), "gn_sword_v_buster_sword").getAsJsonArray("elements");
        int[][] colors = new int[2][16];
        for (var entry : elements) {
            JsonObject element = entry.getAsJsonObject();
            boolean front = element.getAsJsonArray("from").get(2).getAsDouble() >= 9.22;
            String face = front ? "south" : "north";
            JsonArray uv = element.getAsJsonObject("faces")
                    .getAsJsonObject(face).getAsJsonArray("uv");
            int tile = (int)Math.floor(uv.get(0).getAsDouble() / 4)
                    + 4 * (int)Math.floor(uv.get(1).getAsDouble() / 4);
            assertTrue(tile >= 0 && tile < 16);
            colors[front ? 0 : 1][tile]++;
        }
        assertArrayEquals(colors[0], colors[1],
                "GN Buster Sword must mirror the same bright-facing color palette");
        assertTrue(colors[0][3] >= 20, "Both sides must retain the polished silver blade");
        assertTrue(colors[0][8] + colors[0][9] >= 30,
                "Both sides must retain emerald GN energy lines");
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
