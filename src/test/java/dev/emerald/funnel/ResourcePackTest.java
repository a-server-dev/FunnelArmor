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
            assertTrue(entries.contains("assets/funnel/textures/block/funnel.png"));
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
                        assertEquals("funnel:block/funnel",
                                model.getAsJsonObject("textures").get("0").getAsString());
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
            assertEquals(6, checked, "4 drones + quad + controller");
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
