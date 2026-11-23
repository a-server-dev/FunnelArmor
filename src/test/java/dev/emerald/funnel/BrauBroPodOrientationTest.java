package dev.emerald.funnel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regression: after v0.24 twin-cone remodel, the turrets were reversed. */
class BrauBroPodOrientationTest {
    private static final double EPS = 1e-8;

    @Test void turretFacingUsesOwnerYawWithoutAnExtraHalfTurn() {
        for (var mount : BrauBroWaistLayout.MOUNTS) {
            for (float yaw : new float[] {0, 45, 90, 180, -90, -180}) {
                assertEquals(yaw + mount.yawOffset(),
                    BrauBroWaistLayout.podYaw(yaw, mount), .00001f);
            }
        }
    }

    @Test void muzzleWorldForwardMatchesCanonicalMinecraftPlayerFacing() {
        var south = BrauBroWaistLayout.podForward(0);
        assertEquals(0, south.x(), EPS);
        assertEquals(1, south.z(), EPS);

        var west = BrauBroWaistLayout.podForward(90);
        assertEquals(-1, west.x(), EPS);
        assertEquals(0, west.z(), EPS);

        var east = BrauBroWaistLayout.podForward(-90);
        assertEquals(1, east.x(), EPS);
        assertEquals(0, east.z(), EPS);

        var north = BrauBroWaistLayout.podForward(180);
        assertEquals(0, north.x(), EPS);
        assertEquals(-1, north.z(), EPS);
        for (float yaw : new float[] {0, 45, 90, 135, 180, 270, 359}) {
            var f = BrauBroWaistLayout.podForward(yaw);
            assertEquals(1, Math.hypot(f.x(),f.z()), EPS);
        }
    }

    @Test void authoredWhiteConesPointTowardLocalPositiveZAndCableStartsBehind() throws Exception {
        JsonObject model=JsonParser.parseString(Files.readString(Path.of(
            "models/brau_bro_twin_cannon.bbmodel"))).getAsJsonObject();
        double maxConeZ = Double.NEGATIVE_INFINITY;
        double minTopCableZ = Double.POSITIVE_INFINITY;
        int cones=0;
        for(var e:model.getAsJsonArray("elements")) {
            var cuboid=e.getAsJsonObject();
            String name=cuboid.get("name").getAsString();
            if(name.contains("final ceramic pinpoint")) {
                cones++;
                maxConeZ=Math.max(maxConeZ,
                    cuboid.getAsJsonArray("to").get(2).getAsDouble());
            }
            if(name.equals("Upper cable attachment terminal")) {
                minTopCableZ=cuboid.getAsJsonArray("from").get(2).getAsDouble();
            }
        }
        assertEquals(2, cones);
        assertTrue(maxConeZ > 20.5);
        assertTrue(minTopCableZ < 8);
        assertTrue(maxConeZ-minTopCableZ > 12);
    }

    @Test void itemDisplayFixedAndIntrinsicRotationsCancelWithoutEntityYawFlip() throws Exception {
        String source=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/BrauBroWaistCannons.java"));
        assertTrue(source.contains("home.setYaw(BrauBroWaistLayout.podYaw(base.getYaw(), mount))"));
        assertTrue(source.contains("BrauBroWaistLayout.podForward(turret.getYaw())"));
        assertTrue(source.contains("BrauBroWaistLayout.podForward(podLocations[i].getYaw())"));
        assertFalse(source.contains("home.setYaw(base.getYaw() + 180"));
        assertFalse(source.contains("Math.toRadians(turret.getYaw() - 180"));
        assertFalse(source.contains("Math.toRadians(podLocations[i].getYaw() - 180"));
        for(String id:List.of("brau_bro_twin_cannon", "brau_bro_single_cannon")) {
            JsonObject model=JsonParser.parseString(Files.readString(Path.of(
                "resourcepack/assets/funnel/models/item/"+id+".json")))
                .getAsJsonObject();
            var fixed=model.getAsJsonObject("display")
                .getAsJsonObject("fixed").getAsJsonArray("rotation");
            assertEquals(180, fixed.get(1).getAsInt());
            assertEquals(0, fixed.get(0).getAsInt());
            assertEquals(0, fixed.get(2).getAsInt());
        }
    }
}
