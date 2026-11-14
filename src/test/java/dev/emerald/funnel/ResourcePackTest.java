package dev.emerald.funnel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure resource-pack validation: runs without starting Paper or mocking a server.
 */
class ResourcePackTest {
    private JsonObject parse(byte[] bytes) {
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void bundledPackIsReadableAndHasAllModelsAndTexture() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream, "The JAR must contain funnel-pack.zip");
            Set<String> entries = new HashSet<>();
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    assertTrue(entries.add(entry.getName()), "Duplicate ZIP path: " + entry.getName());
                    zip.readAllBytes(); // reads the entry, checks CRC where supported
                    zip.closeEntry();
                }
            }
            assertTrue(entries.contains("pack.mcmeta"));
            assertTrue(entries.contains("assets/minecraft/atlases/blocks.json"), "Explicit funnel atlas registration must be bundled");
            assertTrue(entries.contains("assets/funnel/textures/block/funnel.png"));
            assertTrue(entries.contains("assets/minecraft/atlases/items.json"));
            assertTrue(entries.contains("assets/funnel/textures/item/funnel.png"));
            assertTrue(entries.contains("assets/funnel/models/item/funnel_drone_1_folded.json"));
            assertTrue(entries.contains("assets/funnel/items/funnel_drone_1_folded.json"));
            assertTrue(entries.contains("assets/funnel/textures/item/funnel_drone_1.png"));
            assertTrue(entries.contains("assets/funnel/textures/item/funnel_drone_3.png"),
                    "Qubeley bit requires the dedicated 64x64 purple texture");
            assertTrue(entries.contains("assets/funnel/textures/item/funnel_drone_4.png"),
                    "Unicorn-style shield needs its own 256px paint texture");
            for (int i = 1; i <= 4; i++) {
                assertTrue(entries.contains("assets/funnel/items/funnel_drone_" + i + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/funnel_drone_" + i + ".json"));
            }
            assertTrue(entries.contains("assets/funnel/items/funnel_controller.json"));
            assertTrue(entries.contains("assets/funnel/models/item/funnel_controller.json"));
        }
    }

    @Test
    void allModelsUseStringTextureReferences() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            int checked = 0;
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().startsWith("assets/funnel/models/item/")
                            && entry.getName().endsWith(".json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        String textureId = model.getAsJsonObject("textures").get("0").getAsString();
                        assertTrue(textureId.equals("funnel:item/funnel") ||
                                textureId.equals("funnel:block/funnel") ||
                                textureId.equals("funnel:item/funnel_drone_4") ||
                                textureId.equals("funnel:item/funnel_drone_1") ||
                                textureId.equals("funnel:item/funnel_drone_3"),
                                "Unexpected texture " + textureId);
                        if (entry.getName().endsWith("/funnel_drone_4.json")) {
                            assertEquals("funnel:item/funnel_drone_4", textureId);
                            assertEquals(137, model.getAsJsonArray("elements").size());
                        }
                        for (var element : model.getAsJsonArray("elements")) {
                            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                                assertEquals("#0", face.getValue().getAsJsonObject()
                                        .get("texture").getAsString(),
                                        entry.getName() + ": " + face.getKey());
                            }
                        }
                        checked++;
                    }
                    zip.closeEntry();
                }
            }
            assertEquals(10, checked,
                    "4 drones + folded fin + quad + controller + 3 barrier arm segments");
        }
    }

    @Test
    void barrierSegmentsPreserveAll145FinModelCubes() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            java.util.Map<String, Integer> counts = new java.util.HashMap<>();
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (path.matches("assets/funnel/models/item/funnel_drone_1_barrier_(upper|lower|core)\\.json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        counts.put(path, model.getAsJsonArray("elements").size());
                        assertEquals("funnel:item/funnel_drone_1",
                                model.getAsJsonObject("textures").get("0").getAsString());
                    }
                    zip.closeEntry();
                }
            }
            assertEquals(3, counts.size(), "The barrier requires all three models");
            assertEquals(62, counts.get("assets/funnel/models/item/funnel_drone_1_barrier_upper.json"));
            assertEquals(55, counts.get("assets/funnel/models/item/funnel_drone_1_barrier_lower.json"));
            assertEquals(28, counts.get("assets/funnel/models/item/funnel_drone_1_barrier_core.json"));
            assertEquals(145, counts.values().stream().mapToInt(Integer::intValue).sum(),
                    "Split models must not lose or duplicate fin geometry");
        }
    }

    @Test
    void droneVariantsHaveDifferentGeometryAndEnoughDetail() throws Exception {
        java.util.Map<String, String> signatures = new java.util.HashMap<>();
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (path.matches("assets/funnel/models/item/funnel_drone_[1-4]\\.json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        assertTrue(model.getAsJsonArray("elements").size() >= 20, path);
                        var geometry = new StringBuilder();
                        for (var cube : model.getAsJsonArray("elements")) {
                            JsonObject obj = cube.getAsJsonObject();
                            geometry.append(obj.get("from")).append(obj.get("to"));
                            if (obj.has("rotation")) geometry.append(obj.get("rotation"));
                        }
                        signatures.put(path, geometry.toString());
                    }
                    zip.closeEntry();
                }
            }
        }
        assertEquals(4, signatures.size(), "All 4 drones must be present");
        assertEquals(4, new HashSet<>(signatures.values()).size(), "All 4 drones need unique shapes");
    }

    @Test
    void qubeleyBitHasLongConicalSilhouetteAndDedicatedTexture() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            boolean modelFound = false, textureFound = false, atlasFound = false;
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (path.equals("assets/funnel/models/item/funnel_drone_3.json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        assertEquals("funnel:item/funnel_drone_3",
                                model.getAsJsonObject("textures").get("0").getAsString());
                        var elements = model.getAsJsonArray("elements");
                        assertTrue(elements.size() >= 300 && elements.size() <= 500,
                                "The smooth bell-shaped bit should be detailed but bounded");
                        double minX=100, minY=100, minZ=100, maxX=-100, maxY=-100, maxZ=-100;
                        int sideFacets=0;
                        for (var element : elements) {
                            var cuboid = element.getAsJsonObject();
                            var from = cuboid.getAsJsonArray("from");
                            var to = cuboid.getAsJsonArray("to");
                            minX=Math.min(minX,from.get(0).getAsDouble());
                            minY=Math.min(minY,from.get(1).getAsDouble());
                            minZ=Math.min(minZ,from.get(2).getAsDouble());
                            maxX=Math.max(maxX,to.get(0).getAsDouble());
                            maxY=Math.max(maxY,to.get(1).getAsDouble());
                            maxZ=Math.max(maxZ,to.get(2).getAsDouble());
                            if(cuboid.has("rotation")) {
                                String axis=cuboid.getAsJsonObject("rotation").get("axis").getAsString();
                                assertEquals("z", axis);
                                double angle=Math.abs(cuboid.getAsJsonObject("rotation")
                                        .get("angle").getAsDouble());
                                assertTrue(angle==22.5 || angle==45);
                                sideFacets++;
                            }
                        }
                        assertTrue(maxZ-minZ>25, "Bit should be long from nozzle to tail bell");
                        assertTrue(maxZ-minZ>(maxX-minX)*3, "Bit should be a slender cone, not a saucer");
                        assertEquals(maxX-minX,maxY-minY,0.001, "Bell should be round in section");
                        assertTrue(sideFacets>120, "Sixteen-sided tapered shell must be faceted smoothly");
                        modelFound=true;
                    }
                    else if(path.equals("assets/funnel/textures/item/funnel_drone_3.png")) {
                        byte[] imageBytes=zip.readAllBytes();
                        assertTrue(imageBytes.length>100);
                        assertEquals(137,imageBytes[0]&0xff);
                        assertEquals(80,imageBytes[1]&0xff);
                        assertEquals(78,imageBytes[2]&0xff);
                        assertEquals(71,imageBytes[3]&0xff);
                        assertEquals(64,imageBytes[19]&0xff);
                        assertEquals(64,imageBytes[23]&0xff);
                        textureFound=true;
                    }
                    else if(path.equals("assets/minecraft/atlases/items.json")) {
                        String atlas=new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                        assertTrue(atlas.contains("funnel:item/funnel_drone_3"));
                        atlasFound=true;
                    }
                    zip.closeEntry();
                }
            }
            assertTrue(modelFound && textureFound && atlasFound);
        }
    }

    @Test
    void unicornShieldUvsUseMinecraftTextureSpaceAndValidRotations() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            boolean verified = false;
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().equals("assets/funnel/models/item/funnel_drone_4.json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        assertEquals("funnel:item/funnel_drone_4",
                                model.getAsJsonObject("textures").get("0").getAsString());
                        for (var element : model.getAsJsonArray("elements")) {
                            JsonObject cuboid = element.getAsJsonObject();
                            for (var face : cuboid.getAsJsonObject("faces").entrySet()) {
                                var uv = face.getValue().getAsJsonObject().getAsJsonArray("uv");
                                assertEquals(4, uv.size());
                                for (var coord : uv) {
                                    assertTrue(coord.getAsDouble() >= 0 && coord.getAsDouble() <= 16,
                                            "UV must be scaled from 256px to 16-unit Minecraft space");
                                }
                            }
                            if (cuboid.has("rotation")) {
                                JsonObject rotation = cuboid.getAsJsonObject("rotation");
                                assertEquals("z", rotation.get("axis").getAsString());
                                assertTrue(Math.abs(rotation.get("angle").getAsDouble()) == 22.5 ||
                                        Math.abs(rotation.get("angle").getAsDouble()) == 45);
                            }
                        }
                        verified = true;
                    }
                    zip.closeEntry();
                }
            }
            assertTrue(verified, "Shield model missing from generated pack");
        }
    }

    @Test
    void itemDefinitionsPointToExistingModelFiles() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            Set<String> paths = new HashSet<>();
            Set<String> referencedModels = new HashSet<>();
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    paths.add(entry.getName());
                    if (entry.getName().startsWith("assets/funnel/items/") && entry.getName().endsWith(".json")) {
                        JsonObject item = parse(zip.readAllBytes());
                        String modelId = item.getAsJsonObject("model").get("model").getAsString();
                        assertTrue(modelId.startsWith("funnel:item/"), modelId);
                        referencedModels.add("assets/funnel/models/item/" +
                                modelId.substring("funnel:item/".length()) + ".json");
                    }
                    zip.closeEntry();
                }
            }
            assertFalse(referencedModels.isEmpty());
            assertTrue(paths.containsAll(referencedModels), "Every item model must exist in pack");
        }
    }
}
