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
            for (String name : java.util.List.of("funnel_icon_fin", "funnel_icon_shield",
                    "funnel_icon_qubeley", "funnel_icon_gund")) {
                assertTrue(entries.contains("assets/funnel/items/" + name + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/" + name + ".json"));
                assertTrue(entries.contains("assets/funnel/textures/item/" + name + ".png"));
            }
            assertTrue(entries.contains("assets/funnel/textures/item/funnel_drone_4.png"),
                    "Unicorn-style shield needs its own 256px paint texture");
            for (int i = 1; i <= 4; i++) {
                assertTrue(entries.contains("assets/funnel/items/funnel_drone_" + i + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/funnel_drone_" + i + ".json"));
            }
            assertTrue(entries.contains("assets/funnel/items/funnel_controller.json"));
            assertTrue(entries.contains("assets/funnel/models/item/funnel_controller.json"));
            for (String name : java.util.List.of("aerial_controller", "aerial_bit", "aerial_escutcheon", "aerial_gunbit_rifle")) {
                assertTrue(entries.contains("assets/funnel/items/" + name + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/" + name + ".json"));
            }
            for (int i=1;i<=11;i++) {
                String name = String.format(java.util.Locale.ROOT, "aerial_bit_%02d", i);
                assertTrue(entries.contains("assets/funnel/items/" + name + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/" + name + ".json"));
            }
            assertTrue(entries.contains("assets/funnel/models/item/aerial_escutcheon_blocking.json"));
            for (int i=1;i<=11;i++) {
                String part = String.format(java.util.Locale.ROOT, "aerial_v2_bit_%02d",i);
                assertTrue(entries.contains("assets/funnel/items/" + part + ".json"));
                assertTrue(entries.contains("assets/funnel/models/item/" + part + ".json"));
            }
            assertTrue(entries.contains("assets/funnel/items/aerial_v2_escutcheon.json"));
            assertTrue(entries.contains("assets/funnel/models/item/aerial_v2_escutcheon.json"));
            assertFalse(entries.contains("assets/funnel/items/aerial_bettermodel_hidden.json"),
                    "Deprecated BetterModel overlay must not be in the vanilla resource pack");
            assertFalse(entries.contains("assets/funnel/models/item/aerial_bettermodel_hidden.json"),
                    "Deprecated BetterModel overlay must not be in the vanilla resource pack");
            assertTrue(entries.contains("assets/funnel/textures/item/aerial_escutcheon.png"));
            assertTrue(entries.contains("assets/funnel/textures/item/aerial_gunbit_rifle.png"));
        }
    }

    @Test
    void allModelsUseStringTextureReferences() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            int checked = 0;
            int aerialAliases = 0;
            int aerialExports = 0;
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().startsWith("assets/funnel/models/item/")
                            && entry.getName().endsWith(".json")) {
                        JsonObject model = parse(zip.readAllBytes());
                        // Aerial previews temporarily inherit existing verified models.
                        // Final approved free-mesh BBModels are separately tracked under models/.
                        if (entry.getName().matches("assets/funnel/models/item/funnel_icon_(fin|shield|qubeley|gund)\\.json")) {
                            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
                            assertEquals("funnel:item/" + entry.getName().substring(
                                    entry.getName().lastIndexOf('/')+1, entry.getName().length()-5),
                                    model.getAsJsonObject("textures").get("layer0").getAsString());
                            var gui = model.getAsJsonObject("display").getAsJsonObject("gui");
                            assertEquals(1, gui.getAsJsonArray("scale").get(0).getAsDouble());
                            zip.closeEntry();
                            continue;
                        }
                        if (entry.getName().matches("assets/funnel/models/item/aerial_[a-z0-9_]+\\.json")) {
                            if (model.has("elements")) {
                                assertTrue(model.getAsJsonArray("elements").size() > 0, entry.getName());
                                assertTrue(model.getAsJsonObject("textures").get("0").getAsString()
                                                .startsWith("funnel:item/aerial_"),
                                        "Aerial source-based preview needs its own texture");
                                for (var element : model.getAsJsonArray("elements")) {
                                    for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet())
                                        assertEquals("#0", face.getValue().getAsJsonObject().get("texture").getAsString());
                                }
                                aerialExports++;
                            } else {
                                assertTrue(model.has("parent"), entry.getName() + " must declare a parent");
                                aerialAliases++;
                            }
                            zip.closeEntry();
                            continue;
                        }
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
            assertEquals(2, aerialAliases, "Generic Aerial controller and legacy bit aliases");
            assertEquals(26, aerialExports, "14 existing Aerial models + 11 new bit-first pieces + assembled preview");
        }
    }

    @Test
    void bitFirstEscutcheonIsExactlyTheUnionOfElevenIndependentBits() throws Exception {
        java.util.Map<String, JsonObject> models = new java.util.HashMap<>();
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip");
             ZipInputStream zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry=zip.getNextEntry()) != null) {
                String path = entry.getName();
                if (path.matches("assets/funnel/models/item/aerial_v2_bit_[0-9]{2}\\.json")
                        || path.equals("assets/funnel/models/item/aerial_v2_escutcheon.json")) {
                    models.put(path, parse(zip.readAllBytes()));
                }
                zip.closeEntry();
            }
        }
        assertEquals(12, models.size(), "11 detached bits + joined Escutcheon");
        int count = 0;
        for (int i=1;i<=11;i++) {
            String name = String.format(java.util.Locale.ROOT,
                    "assets/funnel/models/item/aerial_v2_bit_%02d.json",i);
            JsonObject model = models.get(name);
            assertNotNull(model,name);
            int size = model.getAsJsonArray("elements").size();
            assertTrue(size >= 8, "Each bit must have individually authored surfaces: "+name);
            count += size;
        }
        JsonObject joined = models.get("assets/funnel/models/item/aerial_v2_escutcheon.json");
        assertNotNull(joined);
        assertEquals(count,joined.getAsJsonArray("elements").size(),
                "The joined shield may only use geometry authored in the 11 standalone bits");
        assertEquals("funnel:item/aerial_escutcheon",
                joined.getAsJsonObject("textures").get("0").getAsString());
        // The earlier preview accidentally reproduced the EX-PLODED pieces rather than
        // the joined reference: enforce a tight, tall shield silhouette.
        double minX=Double.POSITIVE_INFINITY, minY=Double.POSITIVE_INFINITY;
        double maxX=Double.NEGATIVE_INFINITY, maxY=Double.NEGATIVE_INFINITY;
        int brightWhiteFaces=0;
        for(var element : joined.getAsJsonArray("elements")) {
            var cube=element.getAsJsonObject();
            minX=Math.min(minX,cube.getAsJsonArray("from").get(0).getAsDouble());
            minY=Math.min(minY,cube.getAsJsonArray("from").get(1).getAsDouble());
            maxX=Math.max(maxX,cube.getAsJsonArray("to").get(0).getAsDouble());
            maxY=Math.max(maxY,cube.getAsJsonArray("to").get(1).getAsDouble());
            var uv=cube.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv");
            if(uv.get(0).getAsDouble()>4.0 && uv.get(0).getAsDouble()<5.0
                    && uv.get(1).getAsDouble()<2.0) brightWhiteFaces++;
        }
        assertTrue((maxY-minY)/(maxX-minX)>2.10 && (maxY-minY)/(maxX-minX)<2.70,
                "The joined shield must match the primary reference: broad white panels, not thin sticks");
        assertTrue(brightWhiteFaces>45,
                "White front armor must use the brighter white texture patch rather than dark grey");
        assertTrue(count>=180,"Surface bevels and colored details must survive bit-first joining");
    }

    @Test
    void legacyInventoryIconsAreFramedWithoutChangingWorldDisplays() throws Exception {
        java.util.Map<String, Double> expected = java.util.Map.of(
                "funnel_drone_1", .70,
                "funnel_drone_2", .95,
                "funnel_drone_3", .42,
                "funnel_drone_4", .48);
        java.util.Set<String> checked = new java.util.HashSet<>();
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip");
             ZipInputStream zip = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                for (var check : expected.entrySet()) {
                    if (!entry.getName().equals("assets/funnel/models/item/" +
                            check.getKey() + ".json")) continue;
                    JsonObject model = parse(zip.readAllBytes());
                    JsonObject display = model.getAsJsonObject("display");
                    assertNotNull(display, entry.getName());
                    assertEquals(check.getValue(),
                            display.getAsJsonObject("gui").getAsJsonArray("scale")
                                    .get(0).getAsDouble(), .000001);
                    var fixed = display.getAsJsonObject("fixed").getAsJsonArray("scale");
                    for (int i = 0; i < 3; i++)
                        assertEquals(1.0, fixed.get(i).getAsDouble(), .000001,
                                "Never change in-world ItemDisplay dimensions");
                    assertTrue(model.getAsJsonArray("elements").size() > 0);
                    checked.add(check.getKey());
                    break;
                }
                zip.closeEntry();
            }
        }
        assertEquals(expected.keySet(), checked, "All four original variants must be present");
    }

    @Test
    void approved3DControllerAndHandModelsHaveExpectedTransforms() throws Exception {
        java.util.Map<String, JsonObject> found = new java.util.HashMap<>();
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.equals("assets/funnel/models/item/aerial_bit_01.json")
                            || name.equals("assets/funnel/models/item/aerial_escutcheon.json")
                            || name.equals("assets/funnel/models/item/aerial_gunbit_rifle.json")) {
                        found.put(name.substring(name.lastIndexOf('/') + 1), parse(zip.readAllBytes()));
                    }
                    zip.closeEntry();
                }
            }
        }
        assertEquals(3, found.size());
        JsonObject bit = found.get("aerial_bit_01.json");
        assertTrue(bit.getAsJsonArray("elements").size() > 0);
        assertTrue(bit.getAsJsonObject("display").getAsJsonObject("gui")
                .getAsJsonArray("scale").get(0).getAsDouble() > 1.0);
        JsonObject shield = found.get("aerial_escutcheon.json");
        JsonObject rifle = found.get("aerial_gunbit_rifle.json");
        assertEquals(681, shield.getAsJsonArray("elements").size(),
                "Escutcheon must preserve the approved outline using fine cross-section elements");
        assertEquals(540, rifle.getAsJsonArray("elements").size(), "Rifle has mirrored geometry on both sides");
        assertNotNull(shield.getAsJsonObject("display").getAsJsonObject("firstperson_lefthand"));
        assertNotNull(shield.getAsJsonObject("display").getAsJsonObject("thirdperson_lefthand"));
        assertNotNull(rifle.getAsJsonObject("display").getAsJsonObject("firstperson_righthand"));
        assertNotNull(rifle.getAsJsonObject("display").getAsJsonObject("thirdperson_righthand"));
        assertEquals(1.44, rifle.getAsJsonObject("display").getAsJsonObject("firstperson_righthand")
                .getAsJsonArray("scale").get(0).getAsDouble(),.00001);
        assertEquals(1.47, rifle.getAsJsonObject("display").getAsJsonObject("thirdperson_righthand")
                .getAsJsonArray("scale").get(0).getAsDouble(),.00001);
    }

    @Test
    void aerialShieldUsesVanillaBlockingStateButKeepsEscutcheonGeometry() throws Exception {
        java.util.Map<String, JsonObject> found = new java.util.HashMap<>();
        try (InputStream stream = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(stream);
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.equals("assets/funnel/items/aerial_escutcheon.json")
                            || name.equals("assets/funnel/models/item/aerial_escutcheon.json")
                            || name.equals("assets/funnel/models/item/aerial_escutcheon_blocking.json"))
                        found.put(name, parse(zip.readAllBytes()));
                    assertNotEquals("assets/minecraft/items/shield.json", name,
                            "Do not replace the appearance of normal vanilla shields globally");
                    zip.closeEntry();
                }
            }
        }
        assertEquals(3, found.size());
        var custom = found.get("assets/funnel/items/aerial_escutcheon.json")
                .getAsJsonObject("model");
        assertEquals("minecraft:condition", custom.get("type").getAsString());
        assertEquals("minecraft:using_item", custom.get("property").getAsString());
        assertEquals("funnel:item/aerial_escutcheon",
                custom.getAsJsonObject("on_false").get("model").getAsString());
        assertEquals("funnel:item/aerial_escutcheon_blocking",
                custom.getAsJsonObject("on_true").get("model").getAsString());
        JsonObject idle = found.get("assets/funnel/models/item/aerial_escutcheon.json");
        JsonObject raised = found.get("assets/funnel/models/item/aerial_escutcheon_blocking.json");
        assertEquals(681, idle.getAsJsonArray("elements").size());
        assertEquals(idle.get("elements"), raised.get("elements"),
                "Blocking must only change pose, never remove GUND bits");
        assertEquals(idle.get("textures"), raised.get("textures"));
        var idleView = idle.getAsJsonObject("display");
        var raisedView = raised.getAsJsonObject("display");
        assertTrue(idleView.getAsJsonObject("thirdperson_lefthand")
                .getAsJsonArray("scale").get(0).getAsDouble() >= 1.8);
        assertTrue(raisedView.getAsJsonObject("thirdperson_lefthand")
                .getAsJsonArray("scale").get(0).getAsDouble() >= 2.0);
        assertNotEquals(idleView.getAsJsonObject("thirdperson_lefthand"),
                raisedView.getAsJsonObject("thirdperson_lefthand"),
                "A real Minecraft shield has separate holding and blocking positions");
        assertNotEquals(idleView.getAsJsonObject("firstperson_lefthand"),
                raisedView.getAsJsonObject("firstperson_lefthand"));
        assertEquals(180, idleView.getAsJsonObject("thirdperson_lefthand")
                .getAsJsonArray("rotation").get(1).getAsInt(),
                "Escutcheon plate should face outward, not be edge-on");
    }

    @Test
    void aerialNormalBitProfilesRetainApprovedTapers() throws Exception {
        int[] minimumCubes = {25, 25, 25, 20, 18, 18, 25, 90, 90, 45, 45};
        int tested = 0;
        try (InputStream input = getClass().getResourceAsStream("/funnel-pack.zip")) {
            assertNotNull(input);
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.matches("assets/funnel/models/item/aerial_bit_\\d\\d\\.json")) {
                        int n = Integer.parseInt(name.substring(name.length()-7,name.length()-5));
                        JsonObject m = parse(zip.readAllBytes());
                        var elements = m.getAsJsonArray("elements");
                        assertTrue(elements.size() >= minimumCubes[n-1],
                            "Loss of original GUND-bit tapered profile: "+name);
                        var minSizes = new java.util.HashSet<String>();
                        for (var item : elements) {
                            JsonObject e = item.getAsJsonObject();
                            var lo = e.getAsJsonArray("from");
                            var hi = e.getAsJsonArray("to");
                            double x = hi.get(0).getAsDouble() - lo.get(0).getAsDouble();
                            double y = hi.get(1).getAsDouble() - lo.get(1).getAsDouble();
                            assertTrue(x > 0 && y > 0);
                            minSizes.add(String.format(java.util.Locale.ROOT, "%.3f", x));
                        }
                        assertTrue(minSizes.size() >= 3,"Untapered, bounding-box only bit "+name);
                        tested++;
                    }
                    zip.closeEntry();
                }
            }
        }
        assertEquals(11, tested);
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
                        assertTrue(elements.size() >= 550 && elements.size() <= 650,
                                "The smooth bell-shaped bit should be detailed but bounded");
                        double minX=100, minY=100, minZ=100, maxX=-100, maxY=-100, maxZ=-100;
                        int sideFacets=0;
                        for (var element : elements) {
                            var cuboid = element.getAsJsonObject();
                            var from = cuboid.getAsJsonArray("from");
                            var to = cuboid.getAsJsonArray("to");
                            for (int axis=0; axis<3; axis++)
                                assertTrue(to.get(axis).getAsDouble()>from.get(axis).getAsDouble(),
                                        "Zero-width or inverted cube at axis " + axis);
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
                        JsonObject definition = item.getAsJsonObject("model");
                        if ("minecraft:condition".equals(definition.get("type").getAsString())) {
                            assertEquals("minecraft:using_item",
                                    definition.get("property").getAsString());
                            for (String state : java.util.List.of("on_false", "on_true")) {
                                JsonObject child = definition.getAsJsonObject(state);
                                assertEquals("minecraft:model", child.get("type").getAsString());
                                String id = child.get("model").getAsString();
                                assertTrue(id.startsWith("funnel:item/"), id);
                                referencedModels.add("assets/funnel/models/item/" +
                                        id.substring("funnel:item/".length()) + ".json");
                            }
                        } else {
                            assertEquals("minecraft:model", definition.get("type").getAsString());
                            String modelId = definition.get("model").getAsString();
                            assertTrue(modelId.startsWith("funnel:item/"), modelId);
                            referencedModels.add("assets/funnel/models/item/" +
                                    modelId.substring("funnel:item/".length()) + ".json");
                        }
                    }
                    zip.closeEntry();
                }
            }
            assertFalse(referencedModels.isEmpty());
            assertTrue(paths.containsAll(referencedModels), "Every item model must exist in pack");
        }
    }
}
