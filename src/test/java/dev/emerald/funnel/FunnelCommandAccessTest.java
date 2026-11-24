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

    @Test void catalogHasThirteenUniqueNonLegacyEquipmentKinds() {
        assertEquals(13, FunnelGiftCatalog.ALL.size());
        assertEquals(13, new HashSet<>(FunnelGiftCatalog.ALL).size());
        assertTrue(FunnelGiftCatalog.OPTIONS.containsAll(FunnelGiftCatalog.ALL));
        assertTrue(FunnelGiftCatalog.supported("all"));
        assertTrue(FunnelGiftCatalog.supported("gnv"));
        assertFalse(FunnelGiftCatalog.supported("2"));
        assertFalse(FunnelGiftCatalog.supported("invalid"));
    }
}
