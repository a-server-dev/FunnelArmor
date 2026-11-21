package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrauBroWaistCannonsTest {
    private static final Path PACK = Path.of("resourcepack", "assets", "funnel");
    private static final Path MODEL = PACK.resolve("models").resolve("item");
    private static final Path ITEM = PACK.resolve("items");

    private JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void handheldControllerDeploysExactlyFourWaistMountedPods() {
        var mounts = BrauBroWaistLayout.MOUNTS;
        assertEquals(4, mounts.size());
        assertEquals(2, mounts.stream().filter(BrauBroWaistLayout.Mount::twin).count());
        assertEquals(2, mounts.stream().filter(m -> !m.twin()).count());
        assertEquals(-mounts.get(1).side(), mounts.get(0).side(), .00001);
        assertEquals(-mounts.get(3).side(), mounts.get(2).side(), .00001);
        assertEquals(mounts.get(0).forward(), mounts.get(1).forward(), .00001);
        assertEquals(mounts.get(2).forward(), mounts.get(3).forward(), .00001);
        assertTrue(mounts.get(0).forward() > 0 && mounts.get(2).forward() < 0);
        for (var mount : mounts) {
            assertTrue(Math.abs(mount.side()) >= .30 && Math.abs(mount.side()) <= .65);
            assertTrue(mount.height() >= .80 && mount.height() <= 1.1);
            assertTrue(Math.abs(mount.forward()) < .40);
        }
    }

    @Test void eachPodAndHeldControllerHaveSeparatePackModelAndEditableSource() throws Exception {
        assertNotNull(ImageIO.read(new File(
            "resourcepack/assets/funnel/textures/item/brau_bro_cannon.png")));
        for (String id : List.of("brau_bro_controller", "brau_bro_single_cannon",
                "brau_bro_twin_cannon")) {
            JsonObject item = read(ITEM.resolve(id + ".json"));
            assertEquals("funnel:item/" + id,
                item.getAsJsonObject("model").get("model").getAsString());
            JsonObject pack = read(MODEL.resolve(id + ".json"));
            assertEquals("funnel:item/brau_bro_cannon",
                pack.getAsJsonObject("textures").get("0").getAsString());
            JsonObject bb = read(Path.of("models", id + ".bbmodel"));
            JsonArray raw = bb.getAsJsonArray("elements");
            JsonArray game = pack.getAsJsonArray("elements");
            assertEquals(raw.size(), game.size());
            assertFalse(raw.isEmpty());
            assertFalse(bb.getAsJsonArray("textures").isEmpty());
            HashSet<String> uuids = new HashSet<>();
            for (int index = 0; index < raw.size(); index++) {
                JsonObject source = raw.get(index).getAsJsonObject();
                JsonObject packed = game.get(index).getAsJsonObject();
                assertTrue(uuids.add(source.get("uuid").getAsString()));
                assertEquals(source.get("from"), packed.get("from"));
                assertEquals(source.get("to"), packed.get("to"));
                for (int k=0; k<3; k++) {
                    double lo = source.getAsJsonArray("from").get(k).getAsDouble();
                    double hi = source.getAsJsonArray("to").get(k).getAsDouble();
                    assertTrue(lo >= -16 && hi <= 32 && lo < hi);
                }
                for (String face : List.of("north","south","east","west","up","down")) {
                    assertEquals("#0", packed.getAsJsonObject("faces")
                        .getAsJsonObject(face).get("texture").getAsString());
                }
            }
        }
    }

    @Test void doubleGunHasTwiceAsManyIndependentlyModeledMuzzles() throws Exception {
        var single=read(Path.of("models/brau_bro_single_cannon.bbmodel"))
            .getAsJsonArray("elements");
        var twin=read(Path.of("models/brau_bro_twin_cannon.bbmodel"))
            .getAsJsonArray("elements");
        long singleFocus=0,twinFocus=0;
        for(var x : single) if(x.getAsJsonObject().get("name").getAsString()
                .endsWith("green particle focus")) singleFocus++;
        for(var x : twin) if(x.getAsJsonObject().get("name").getAsString()
                .endsWith("green particle focus")) twinFocus++;
        assertEquals(1, singleFocus);
        assertEquals(2, twinFocus);
        assertTrue(twin.size() > single.size());
    }
}
