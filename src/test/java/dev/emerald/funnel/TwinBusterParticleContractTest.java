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
    @Test void noCameraAdjacentFlashAndWardenShockwaveRingsPresent() throws Exception {
        String code = Files.readString(Path.of(
                "src/main/java/dev/emerald/funnel/TwinBusterCombat.java"));
        assertFalse(code.contains("Particle.FLASH"),
                "Remove bright FLASH particles entirely from the Twin Buster Rifle");
        assertTrue(code.contains("Material.FEATHER"),
                "Inert base item suppresses vanilla right-use action");
        assertTrue(code.contains("world.spawnParticle(Particle.SONIC_BOOM, ring, 1"),
                "Combined beam should include spaced Warden shockwave rings");
        assertTrue(code.contains("world.spawnParticle(Particle.SONIC_BOOM, impact, 1"),
                "Impact should include a Warden shockwave");
    }
}
