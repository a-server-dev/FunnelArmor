package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Geometry checks on the exact pack embedded in the plugin, without Paper. */
class AerialPreviewGeometryTest {
    private Map<String, JsonObject> models() throws Exception {
        Map<String, JsonObject> result = new TreeMap<>();
        try (var stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try (var zip = new ZipInputStream(stream)) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().startsWith("assets/funnel/models/item/aerial_v2_")) {
                        String name = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                        result.put(name, JsonParser.parseString(new String(zip.readAllBytes(),
                                StandardCharsets.UTF_8)).getAsJsonObject());
                    }
                    zip.closeEntry();
                }
            }
        }
        assertEquals(13, result.size(), "11 individual models + assembled and blocking shield");
        return result;
    }

    private JsonArray bits(Map<String, JsonObject> models, int id) {
        return models.get("aerial_v2_bit_%02d.json".formatted(id)).getAsJsonArray("elements");
    }

    private double coordinate(JsonObject cube, String key, int axis) {
        return cube.getAsJsonArray(key).get(axis).getAsDouble();
    }

    @Test
    void assembledBitsPreserveEveryFaceDimensionAndRotationPivot() throws Exception {
        var models = models();
        var shield = models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements");
        int offset = 0;
        for (int id = 1; id <= 11; id++) {
            var part = bits(models, id);
            var first = part.get(0).getAsJsonObject();
            var joinedFirst = shield.get(offset).getAsJsonObject();
            double[] shift = new double[3];
            for (int axis = 0; axis < 3; axis++)
                shift[axis] = coordinate(joinedFirst, "from", axis) - coordinate(first, "from", axis);
            for (var element : part) {
                var local = element.getAsJsonObject();
                var joined = shield.get(offset++).getAsJsonObject();
                assertEquals(local.get("faces"), joined.get("faces"), "Bit " + id);
                for (String key : List.of("from", "to"))
                    for (int axis = 0; axis < 3; axis++)
                        assertEquals(coordinate(local, key, axis) + shift[axis],
                                coordinate(joined, key, axis), .000021, "Bit " + id + " " + key);
                assertEquals(local.has("rotation"), joined.has("rotation"));
                if (local.has("rotation")) {
                    var a = local.getAsJsonObject("rotation");
                    var b = joined.getAsJsonObject("rotation");
                    for (String key : List.of("axis", "angle", "rescale"))
                        assertEquals(a.get(key), b.get(key));
                    for (int axis = 0; axis < 3; axis++)
                        assertEquals(coordinate(a, "origin", axis) + shift[axis],
                                coordinate(b, "origin", axis), .000021, "Rotation pivot must translate with bit");
                }
            }
        }
        assertEquals(offset + 5, shield.size(), "Only the unchanged five grip elements are shield-only");
        double[][] from = {{6.84,12.33,6.34},{7.67,12.66,6.57},{7.67,11.78,6.57},
                {7.32,13.05,7.17},{7.32,11.71,7.17}};
        double[][] to = {{9.16,12.71,6.66},{8.33,13.26,8.075},{8.33,12.38,8.075},
                {8.68,13.32,8.08},{8.68,11.98,8.08}};
        for (int i = 0; i < 5; i++) {
            var grip = shield.get(offset + i).getAsJsonObject();
            assertFalse(grip.has("rotation"));
            for (int axis = 0; axis < 3; axis++) {
                assertEquals(from[i][axis], coordinate(grip, "from", axis), .000001);
                assertEquals(to[i][axis], coordinate(grip, "to", axis), .000001);
            }
        }
    }

    private List<double[]> corners(JsonObject cube) {
        var result = new ArrayList<double[]>();
        for (String x : List.of("from", "to"))
            for (String y : List.of("from", "to"))
                for (String z : List.of("from", "to")) {
                    double px = coordinate(cube, x, 0), py = coordinate(cube, y, 1);
                    double pz = coordinate(cube, z, 2);
                    if (cube.has("rotation")) {
                        var r = cube.getAsJsonObject("rotation");
                        assertEquals("z", r.get("axis").getAsString());
                        double angle = r.get("angle").getAsDouble();
                        assertTrue(List.of(-45.0, -22.5, 0.0, 22.5, 45.0).contains(angle));
                        assertFalse(r.get("rescale").getAsBoolean());
                        double ox = coordinate(r, "origin", 0), oy = coordinate(r, "origin", 1);
                        double a = Math.toRadians(angle), dx = px - ox, dy = py - oy;
                        px = ox + Math.cos(a) * dx - Math.sin(a) * dy;
                        py = oy + Math.sin(a) * dx + Math.cos(a) * dy;
                    }
                    result.add(new double[]{px, py, pz});
                }
        return result;
    }

    @Test
    void rotatedShouldersAreMirroredCenteredAndStayWithinTheNarrowShield() throws Exception {
        var models = models();
        for (int id : new int[]{5, 6}) {
            var part = bits(models, id);
            double[] lo = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] hi = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (var element : part) {
                var cube = element.getAsJsonObject();
                assertEquals(id == 5 ? 22.5 : -22.5,
                        cube.getAsJsonObject("rotation").get("angle").getAsDouble());
                for (var point : corners(cube)) for (int axis = 0; axis < 3; axis++) {
                    lo[axis] = Math.min(lo[axis], point[axis]);
                    hi[axis] = Math.max(hi[axis], point[axis]);
                }
            }
            for (int axis = 0; axis < 3; axis++)
                assertEquals(8, (lo[axis] + hi[axis]) / 2, .00002,
                        "Standalone centering must include rotation");
        }
        var left = bits(models, 5);
        var right = bits(models, 6);
        assertEquals(left.size(), right.size());
        for (int i = 0; i < left.size(); i++) {
            var a = left.get(i).getAsJsonObject();
            var b = right.get(i).getAsJsonObject();
            assertEquals(16 - coordinate(a, "to", 0), coordinate(b, "from", 0), .00002);
            assertEquals(16 - coordinate(a, "from", 0), coordinate(b, "to", 0), .00002);
            assertEquals(a.get("faces"), b.get("faces"));
        }
        double loX = 32, hiX = -16, loY = 32, hiY = -16;
        for (var e : models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements")) {
            for (var p : corners(e.getAsJsonObject())) {
                for (double value : p) assertTrue(Double.isFinite(value) && value >= -16 && value <= 32);
                loX = Math.min(loX, p[0]); hiX = Math.max(hiX, p[0]);
                loY = Math.min(loY, p[1]); hiY = Math.max(hiY, p[1]);
            }
        }
        assertTrue(hiX - loX < 8);
        assertTrue((hiY - loY) / (hiX - loX) > 2.77 && (hiY - loY) / (hiX - loX) < 3.25);
    }

    @Test
    void joinedBlueShouldersAreRepositionedSymmetricallyWithoutRotationChanges() throws Exception {
        var models = models();
        var shield = models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements");
        int start = 0;
        for (int i = 1; i < 5; i++) start += bits(models, i).size();
        double[][] target = {{5.48889, 15.12, 8.56}, {9.08951, 15.12, 8.56}};
        for (int id : new int[]{5, 6}) {
            var part = bits(models, id);
            var cube = shield.get(start).getAsJsonObject();
            for (int axis = 0; axis < 3; axis++)
                assertEquals(target[id - 5][axis], coordinate(cube, "from", axis), .000021,
                        "Joined shoulder translation must match the v0.8.34 placement");
            assertEquals(id == 5 ? 22.5 : -22.5,
                    cube.getAsJsonObject("rotation").get("angle").getAsDouble());
            start += part.size();
        }
    }

    @Test
    void lowerInnerArmorShowsTwoDistinctBladesWithBalancedMiddleGap() throws Exception {
        var models = models();
        var shield = models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements");
        int offset = 0;
        for (int id = 1; id < 10; id++) offset += bits(models, id).size();
        int leftSize = bits(models, 10).size();
        for (double y : new double[]{3, 6, 9}) {
            double leftEdge = -16, rightEdge = 32;
            for (int i = 0; i < leftSize; i++) {
                var a = shield.get(offset + i).getAsJsonObject();
                var b = shield.get(offset + leftSize + i).getAsJsonObject();
                if (coordinate(a, "from", 1) <= y && coordinate(a, "to", 1) >= y)
                    leftEdge = Math.max(leftEdge, coordinate(a, "to", 0));
                if (coordinate(b, "from", 1) <= y && coordinate(b, "to", 1) >= y)
                    rightEdge = Math.min(rightEdge, coordinate(b, "from", 0));
            }
            double gap = rightEdge - leftEdge;
            assertEquals(.44, gap, .000021, "Separate two grey blades without reshaping them at Y=" + y);
            assertEquals(8, (leftEdge + rightEdge) / 2, .000021, "Grey blades should remain mirror-symmetric");
        }
    }

    @Test
    void raisedWhiteOuterArmorFitsUnderShouldersWithoutChangingItsShapes() throws Exception {
        var models = models();
        var shield = models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements");
        int start = 0;
        for (int i = 1; i < 7; i++) start += bits(models, i).size();
        for (int id : new int[]{7, 8}) {
            var part = bits(models, id);
            double maxY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < part.size(); i++)
                maxY = Math.max(maxY, coordinate(shield.get(start + i).getAsJsonObject(), "to", 1));
            assertEquals(15.90, maxY, .000021,
                    "White shell upper tip should be tucked behind its blue shoulder");
            start += part.size();
        }
    }

    @Test
    void outerFinSideColorAndProductionShieldBlockingShareExactBitGeometry() throws Exception {
        var models = models();
        for (int id : new int[]{7, 8}) {
            var bit = bits(models, id);
            String exterior = id == 7 ? "west" : "east";
            String opposite = id == 7 ? "east" : "west";
            int exteriorBlue = 0;
            for (var item : bit) {
                var faces = item.getAsJsonObject().getAsJsonObject("faces");
                if (!faces.getAsJsonObject(exterior).get("uv")
                        .equals(faces.getAsJsonObject(opposite).get("uv"))) {
                    exteriorBlue++;
                    assertEquals(faces.getAsJsonObject(opposite).get("uv"),
                            faces.getAsJsonObject("north").get("uv"),
                            "White front faces must stay unchanged");
                }
            }
            assertTrue(exteriorBlue >= 20, "Both lower white bits need blue lateral trim");
        }
        var idle=models.get("aerial_v2_escutcheon.json");
        var block=models.get("aerial_v2_escutcheon_blocking.json");
        assertEquals(idle.get("elements"), block.get("elements"),
                "Native blocking must preserve all eleven bits and the five-piece handle");
        assertNotEquals(idle.getAsJsonObject("display").get("thirdperson_lefthand"),
                block.getAsJsonObject("display").get("thirdperson_lefthand"));
    }

    @Test
    void outerArmorTipsTaperAndInnerLapJointsBelongToTheirBits() throws Exception {
        var models = models();
        var shield = models.get("aerial_v2_escutcheon.json").getAsJsonArray("elements");
        int offset = 0;
        for (int id = 1; id < 7; id++) offset += bits(models, id).size();
        for (int id : new int[]{7, 8}) {
            var part = bits(models, id);
            double minY = Double.POSITIVE_INFINITY;
            for (var e : part) minY = Math.min(minY, coordinate(e.getAsJsonObject(), "from", 1));
            double loX = 32, hiX = -16;
            for (var e : part) {
                var cube = e.getAsJsonObject();
                if (Math.abs(coordinate(cube, "from", 1) - minY) < .00001) {
                    loX = Math.min(loX, coordinate(cube, "from", 0));
                    hiX = Math.max(hiX, coordinate(cube, "to", 0));
                }
            }
            assertTrue(hiX - loX > 0 && hiX - loX < .25,
                    "White outer tip must taper instead of retaining the old broad flat end");
            // Sample the new white lap behind the nose/crest connection in
            // joined coordinates. It must be supplied by bit 07/08 itself.
            double x = id == 7 ? 6.73 : 9.27;
            boolean covered = false;
            for (int i = 0; i < part.size(); i++) {
                var cube = shield.get(offset + i).getAsJsonObject();
                if (coordinate(cube, "from", 0) <= x && coordinate(cube, "to", 0) >= x
                        && coordinate(cube, "from", 1) <= 13.83 && coordinate(cube, "to", 1) >= 13.83
                        && coordinate(cube, "from", 2) <= 8.5 && coordinate(cube, "to", 2) >= 8.5)
                    covered = true;
            }
            assertTrue(covered, "Inner lap joint must be part of independent bit " + id);
            offset += part.size();
        }
    }

}
