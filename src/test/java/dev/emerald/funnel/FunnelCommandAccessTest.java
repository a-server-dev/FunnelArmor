package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class FunnelCommandAccessTest {
    @Test void onlyOperatorOrNamedAdminCanRunCommands() {
        assertTrue(FunnelCommandAccess.authorizedPlayer("EmeraldREX91", false));
        assertTrue(FunnelCommandAccess.authorizedPlayer("emeraldrex91", false));
        assertTrue(FunnelCommandAccess.authorizedPlayer("OtherPlayer", true));
        assertFalse(FunnelCommandAccess.authorizedPlayer("OtherPlayer", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("EmeraldREX9", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("EmeraldREX91x", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("", false));
    }

    @Test void catalogHasElevenUniqueNonLegacyEquipmentKinds() {
        assertEquals(14, FunnelGiftCatalog.ALL.size());
        assertEquals(14, new HashSet<>(FunnelGiftCatalog.ALL).size());
        assertTrue(FunnelGiftCatalog.OPTIONS.containsAll(FunnelGiftCatalog.ALL));
        assertTrue(FunnelGiftCatalog.supported("all"));
        for (String kind : java.util.List.of("gn", "gn2", "gnv")) {
            assertFalse(FunnelGiftCatalog.supported(kind));
            assertFalse(FunnelGiftCatalog.ALL.contains(kind));
        }
        assertFalse(FunnelGiftCatalog.supported("2"));
        assertFalse(FunnelGiftCatalog.supported("invalid"));
    }
}
