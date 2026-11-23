package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for the user-supplied 2026-11-23 F5 screenshot:
 * 12 disconnected rods floated around the player instead of 4 real cables
 * connecting each pod to the waist.
 */
class BrauBroCableContinuityTest {
    private String source() throws Exception {
        return Files.readString(Path.of("src/main/java/dev/emerald/funnel/BrauBroWaistCannons.java"));
    }

    @Test void exactlyOneContinuousWireIsUsedForEachOfTheFourCannons() throws Exception {
        var body=source();
        assertTrue(body.contains("CABLE_SEGMENTS = 1;"),
            "One cable entity per pod prevents gaps between independently interpolated links");
        assertTrue(body.contains("new ArrayList<>(4 * CABLE_SEGMENTS)"));
        assertTrue(body.contains("state.cables.get(i)"));
        assertTrue(body.contains("ensureCables(player, state)"));
        assertFalse(body.contains("CABLE_SEGMENTS = 3;"));
    }

    @Test void wireRotationMustNotBeAppliedTwiceThroughTeleportYaw() throws Exception {
        var body=source();
        assertTrue(body.contains("new Quaternionf().rotationTo("),
            "The cable quaternion must directly align its Y axis between endpoints");
        assertTrue(body.contains("neutral.setYaw(0f)"));
        assertTrue(body.contains("neutral.setPitch(0f)"));
        assertTrue(body.contains("segment.teleport(neutral)"));
        assertFalse(body.contains("segment.teleport(a)"),
            "A Location cloned from the player would incorrectly add the player yaw");
        assertTrue(body.contains("d.setTeleportDuration(0)"));
        assertTrue(body.contains("d.setInterpolationDuration(0)"));
    }

    @Test void deployedCannonsPursueLivePlayerFormationWhileWireUsesRealEndpoints() throws Exception {
        var body=source();
        assertTrue(body.contains("state.extended ? worldOffset(base,"),
            "Every deployed turret destination must use live player position and yaw");
        assertTrue(body.contains("BrauBroWaistLayout.launched(mount, reach())"));
        assertTrue(body.contains("BrauBroFirePattern.followStep(distance, reach())"),
            "World-space pursuit must be smoothed, not instantly rotated with the player");
        assertFalse(body.contains("launchTargets"),
            "Frozen world targets produced non-following turrets in v0.21");
        assertTrue(body.contains("podLocations[i].getYaw() - 180.0"));
        assertTrue(body.contains("worldOffset(player.getLocation(), forward, right,"));
        assertTrue(body.contains("segment(state.cables.get(i), harness, coupling)"));
        assertTrue(body.contains("runTaskTimer(plugin, this::tick, 1L, 1L)"),
            "Both wire endpoints must refresh each tick");
    }
}
