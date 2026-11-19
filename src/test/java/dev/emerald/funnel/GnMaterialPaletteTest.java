package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pixel-level material regression: model-part names cannot stand in for true
 * atlas pixel colors. The 0.15.0 implementation inadvertently mapped cobalt
 * sword-bit armor to a very light gray swatch, yielding two white cylinders.
 */
class GnMaterialPaletteTest {
    private static final Path SOURCE = Path.of("models");
    private static final Path PACK = Path.of("resourcepack", "assets", "funnel", "models", "item");
    private static final Path TEXTURE = Path.of("resourcepack", "assets", "funnel",
        "textures", "item", "gn_sword_v_buster_rifle.png");

    private JsonObject load(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
    private JsonObject named(JsonArray elements, String name) {
        for (var el : elements) {
            JsonObject o = el.getAsJsonObject();
            if (name.equals(o.get("name").getAsString())) return o;
        }
        fail("Missing editable part: " + name);
        return null;
    }
    private int colorAt(BufferedImage texture, JsonObject element) {
        JsonArray uv = element.getAsJsonObject("faces").getAsJsonObject("north")
            .getAsJsonArray("uv");
        int x = (int) Math.floor((uv.get(0).getAsDouble() + uv.get(2).getAsDouble()) / 2);
        int y = (int) Math.floor((uv.get(1).getAsDouble() + uv.get(3).getAsDouble()) / 2);
        assertTrue(x >= 0 && x < 64 && y >= 0 && y < 64);
        return texture.getRGB(x, y);
    }
    private int red(int rgb) { return (rgb >>> 16) & 255; }
    private int green(int rgb) { return (rgb >>> 8) & 255; }
    private int blue(int rgb) { return rgb & 255; }

    @Test void everyBlueSwordBitShellSamplesBluePixelsNotWhiteOrGray() throws Exception {
        BufferedImage image = ImageIO.read(TEXTURE.toFile());
        assertNotNull(image);
        assertEquals(64, image.getWidth());
        assertEquals(64, image.getHeight());
        var source = load(SOURCE.resolve("gn_sword_v_buster_rifle.bbmodel"))
            .getAsJsonArray("elements");
        for (String side : List.of("L", "R")) {
            for (int i = 0; i < 22; i++) {
                int c = colorAt(image, named(source,
                    "GN QanT " + side + " long tapered sapphire sword-bit hood " + i));
                assertTrue(blue(c) > red(c) + 70 && blue(c) > green(c) + 35,
                    "Cobalt hood must sample actual BLUE pixels: " + side + "/" + i);
                assertTrue(red(c) < 125, "White-slab regression: " + side + "/" + i);
            }
        }
    }
    @Test void swordAndRifleBothUseRealGreenCuttingFaces() throws Exception {
        BufferedImage image = ImageIO.read(TEXTURE.toFile());
        for (String mode : List.of("sword", "buster_rifle")) {
            var source = load(SOURCE.resolve("gn_sword_v_" + mode + ".bbmodel"))
                .getAsJsonArray("elements");
            var pack = load(PACK.resolve("gn_sword_v_" + mode + ".json"));
            assertEquals("funnel:item/gn_sword_v_buster_rifle",
                pack.getAsJsonObject("textures").get("0").getAsString());
            for (String side : mode.equals("sword") ? List.of("") : List.of("L", "R")) {
                for (int i = 0; i < 20; i++) {
                    String name = mode.equals("sword")
                        ? "QanT visible green sword blade " + i
                        : "GN emerald tapered cutting edge " + side + " " + i;
                    int c = colorAt(image, named(source, name));
                    assertTrue(green(c) > red(c) + 80 && green(c) > blue(c) + 25,
                        name + " is not assigned to a bright green palette region");
                }
            }
        }
    }
    @Test void singleSwordHasOneBladeAndBusterHasTwoSeparateBladeLines() throws Exception {
        JsonArray one = load(SOURCE.resolve("gn_sword_v_sword.bbmodel"))
            .getAsJsonArray("elements");
        JsonArray two = load(SOURCE.resolve("gn_sword_v_buster_rifle.bbmodel"))
            .getAsJsonArray("elements");
        assertTrue(one.size() >= 200);
        assertTrue(two.size() >= 420);
        for (int i = 0; i < 20; i++) {
            assertNotNull(named(one, "QanT visible green sword blade " + i));
            JsonObject a = named(two, "GN emerald tapered cutting edge L " + i);
            JsonObject b = named(two, "GN emerald tapered cutting edge R " + i);
            if (i >= 8) {
                double upperBottom = a.getAsJsonArray("from").get(1).getAsDouble();
                double lowerTop = b.getAsJsonArray("to").get(1).getAsDouble();
                assertTrue(upperBottom > lowerTop + 1.4,
                    "Independent green emitters must not merge into white double tubes");
            }
        }
    }
}
