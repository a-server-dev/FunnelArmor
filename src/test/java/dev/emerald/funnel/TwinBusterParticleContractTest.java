package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Prevents accidental use of Paper 26.3 FLASH without its mandatory Color data.
 * Checks the source-level bridge because plugin runtime particles require a running server.
 */
class TwinBusterParticleContractTest {
    @Test void allFlashEffectsUseTheColorRequiredByPaper() throws Exception {
        String code = Files.readString(Path.of(
                "src/main/java/dev/emerald/funnel/TwinBusterCombat.java"));
        assertTrue(code.contains("private static void coloredFlash(World world, Location at, int count,"));
        assertTrue(code.contains("double spread, Color color)"));
        assertTrue(code.contains("world.spawnParticle(Particle.FLASH, at, count,"));
        assertTrue(code.contains("spread, spread, spread, 0, color);"));
        assertEquals(1, code.split("spawnParticle\\(Particle\\.FLASH", -1).length - 1,
                "Only the color-aware helper may directly spawn FLASH particles");
        assertEquals(1, code.split("coloredFlash\\(world,", -1).length - 1,
                "Muzzle FLASH was deliberately removed; only distant impact may flash");
        assertTrue(code.contains("world.spawnParticle(Particle.SONIC_BOOM, ring, 1"),
                "Combined beam should include spaced Warden shockwave rings");
        assertTrue(code.contains("world.spawnParticle(Particle.SONIC_BOOM, impact, 1"),
                "Impact should include a Warden shockwave");
    }
}
