package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the GN Buster Sword silhouette and source/model synchronization. */
class GnBusterSwordSilhouetteTest {
    private static final String ITEM_MODEL =
            "assets/funnel/models/item/gn_sword_v_buster_sword.json";

    private static JsonObject embeddedModel() throws Exception {
        try (InputStream stream = GnBusterSwordSilhouetteTest.class
                .getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream, "Missing generated resource pack");
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    if (ITEM_MODEL.equals(e.getName())) {
                        return JsonParser.parseString(
                                new String(zip.readAllBytes(), StandardCharsets.UTF_8))
                                .getAsJsonObject();
                    }
                    zip.closeEntry();
                }
            }
        }
        fail("GN Buster Sword model was not packed");
        throw new AssertionError("unreachable");
    }

    private static double[] horizontalSpan(JsonArray parts, double height) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (var part : parts) {
            JsonObject cube = part.getAsJsonObject();
            JsonArray from = cube.getAsJsonArray("from");
            JsonArray to = cube.getAsJsonArray("to");
            if (from.get(1).getAsDouble() < height
                    && to.get(1).getAsDouble() > height) {
                min = Math.min(min, from.get(0).getAsDouble());
                max = Math.max(max, to.get(0).getAsDouble());
            }
        }
        assertTrue(Double.isFinite(min) && Double.isFinite(max),
                "No metal at height " + height + " — interrupted blade");
        return new double[] {min, max};
    }

    @Test
    void bladeIsOneCentralTaperInsteadOfSixParallelSpears() throws Exception {
        JsonObject model = embeddedModel();
        JsonArray parts = model.getAsJsonArray("elements");
        double[] heights = {14.5, 19.5, 24.5, 28.8, 30.5, 31.2};
        double lastWidth = Double.POSITIVE_INFINITY;
        for (double height : heights) {
            double[] span = horizontalSpan(parts, height);
            double width = span[1] - span[0];
            assertTrue(width < lastWidth,
                    "Blade must taper toward a single tip at y=" + height);
            assertEquals(8.0, (span[0] + span[1]) / 2.0, 0.6,
                    "The main tip must remain centered");
            lastWidth = width;
        }
        assertTrue(lastWidth < 0.65, "The tip must narrow to one sharp point");
        assertTrue(horizontalSpan(parts, 14.5)[1] - horizontalSpan(parts, 14.5)[0] > 9,
                "The hilt end must still read as a heavy buster sword");

        assertEquals("funnel:item/gn_sword_v_buster_sword",
                model.getAsJsonObject("textures").get("0").getAsString());
        assertTrue(parts.size() >= 180, "Do not flatten mechanical detail");
        for (var element : parts) {
            JsonObject cube = element.getAsJsonObject();
            for (int axis = 0; axis < 3; axis++) {
                double lo = cube.getAsJsonArray("from").get(axis).getAsDouble();
                double hi = cube.getAsJsonArray("to").get(axis).getAsDouble();
                assertTrue(lo >= -16 && lo < hi && hi <= 32,
                        "Illegal model coordinate " + lo + ".." + hi);
            }
        }
    }

    @Test
    void sixIntegratedBitsAndEditableBlockbenchGeometryAgreeWithPack() throws Exception {
        JsonObject model = embeddedModel();
        JsonArray packed = model.getAsJsonArray("elements");
        JsonObject source = JsonParser.parseString(Files.readString(
                Path.of("models/gn_sword_v_buster_sword.bbmodel"))).getAsJsonObject();
        JsonArray cubes = source.getAsJsonArray("elements");
        assertEquals(packed.size(), cubes.size(), "BBModel and pack must have same geometry");
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject sourceCube = cubes.get(i).getAsJsonObject();
            JsonObject packedCube = packed.get(i).getAsJsonObject();
            for (String property : new String[]{"from", "to"}) {
                assertEquals(sourceCube.get(property), packedCube.get(property),
                        "Mismatch at cube " + i + " " + property);
            }
        }
        Set<String> bits = new HashSet<>();
        for (var group : source.getAsJsonArray("outliner")) {
            JsonObject g = group.getAsJsonObject();
            String name = g.get("name").getAsString();
            if (!name.startsWith("GN Sword Bit ")) continue;
            assertTrue(g.getAsJsonArray("children").size() >= 10,
                    "Each integrated bit must have mechanical detail: " + name);
            bits.add(name);
        }
        assertEquals(Set.of("GN Sword Bit L1", "GN Sword Bit L2", "GN Sword Bit L3",
                "GN Sword Bit R1", "GN Sword Bit R2", "GN Sword Bit R3"), bits);
    }
}
