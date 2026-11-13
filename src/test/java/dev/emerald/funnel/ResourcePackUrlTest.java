package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePackUrlTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef01234567";

    @Test void createsImmutablePublicPackKeys() {
        assertEquals("funnel-pack-" + HASH + ".zip", ResourcePackUrl.fileName(HASH));
        assertEquals("https://packs.example.com/funnel-pack-" + HASH + ".zip",
                ResourcePackUrl.fromBase("https://packs.example.com/", HASH));
        assertEquals("https://pub-abc.r2.dev/packs/funnel-pack-" + HASH + ".zip",
                ResourcePackUrl.fromBase(" https://pub-abc.r2.dev/packs/ ", HASH));
        assertEquals(ResourcePackUrl.fileName(HASH),
                ResourcePackUrl.fileName(HASH.toUpperCase(java.util.Locale.ROOT)));
    }

    @Test void invalidHashOrUnsafeBaseIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ResourcePackUrl.fileName("abc"));
        assertThrows(IllegalArgumentException.class, () -> ResourcePackUrl.fromBase("", HASH));
        assertThrows(IllegalArgumentException.class, () -> ResourcePackUrl.fromBase("http://packs.example.com", HASH));
        assertThrows(IllegalArgumentException.class, () -> ResourcePackUrl.fromBase("https://packs.example.com/?x=1", HASH));
        assertThrows(IllegalArgumentException.class, () -> ResourcePackUrl.fromBase("https://u@packs.example.com", HASH));
    }

    @Test void githubReleaseMatchesPublishedTagAndFilename() {
        String expected = "https://github.com/a-server-dev/FunnelArmor/releases/download/pack-"
                + HASH + "/funnel-pack-" + HASH + ".zip";
        assertEquals(expected, ResourcePackUrl.githubRelease("a-server-dev/FunnelArmor", HASH));
        assertEquals(expected, ResourcePackUrl.githubRelease("a-server-dev/FunnelArmor",
                HASH.toUpperCase(java.util.Locale.ROOT)));
        assertThrows(IllegalArgumentException.class,
                () -> ResourcePackUrl.githubRelease("a-server-dev/../FunnelArmor", HASH));
        assertThrows(IllegalArgumentException.class,
                () -> ResourcePackUrl.githubRelease("", HASH));
        assertThrows(IllegalArgumentException.class,
                () -> ResourcePackUrl.githubRelease("a-server-dev/FunnelArmor", "xyz"));
    }

    @Test void changedContentHashProducesDifferentCachedObject() {
        assertNotEquals(ResourcePackUrl.fromBase("https://packs.example.com", HASH),
                ResourcePackUrl.fromBase("https://packs.example.com", "a".repeat(40)));
    }
}
