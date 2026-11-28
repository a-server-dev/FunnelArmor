package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class FunnelCommandAccessTest {
    @Test void operatorNamedAdminOrPermissionHolderCanRunCommands() {
        assertTrue(FunnelCommandAccess.authorizedPlayer("EmeraldREX91", false));
        assertTrue(FunnelCommandAccess.authorizedPlayer("emeraldrex91", false));
        assertTrue(FunnelCommandAccess.authorizedPlayer("OtherPlayer", true));
        assertTrue(FunnelCommandAccess.authorizedPlayer("OtherPlayer", false, true));
        assertFalse(FunnelCommandAccess.authorizedPlayer("OtherPlayer", false, false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("OtherPlayer", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("EmeraldREX9", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("EmeraldREX91x", false));
        assertFalse(FunnelCommandAccess.authorizedPlayer("", false));
    }

    @Test void catalogHasSixteenUniqueNonLegacyEquipmentKinds() {
        assertEquals(16, FunnelGiftCatalog.ALL.size());
        assertEquals(16, new HashSet<>(FunnelGiftCatalog.ALL).size());
        assertEquals(java.util.List.of("all"), FunnelGiftCatalog.OPTIONS);
        for(String kind:FunnelGiftCatalog.ALL) assertFalse(FunnelGiftCatalog.supported(kind));
        assertTrue(FunnelGiftCatalog.supported("all"));
        assertTrue(FunnelGiftCatalog.ALL.containsAll(java.util.List.of("hammer", "destiny")));
        for (String kind : java.util.List.of("gn", "gn2", "gnv")) {
            assertFalse(FunnelGiftCatalog.supported(kind));
            assertFalse(FunnelGiftCatalog.ALL.contains(kind));
        }
        assertFalse(FunnelGiftCatalog.supported("2"));
        assertFalse(FunnelGiftCatalog.supported("invalid"));
    }
}
