package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FunnelTabSuggestionsTest {
    private static final List<String> ONLINE = List.of("EmeraldREX91", "East", "Pen4");

    @Test void playerGetsRelevantCommandsButNoAdminActions() {
        var result = FunnelTabSuggestions.complete(new String[] {""}, false, true, ONLINE);
        assertEquals(List.of("barrier", "formation", "pack", "shield", "status"), result);
        assertEquals(List.of("pack"), FunnelTabSuggestions.complete(new String[]{"Pa"}, false, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give",""}, false, true, ONLINE));
    }

    @Test void adminGetsAllSubcommandsAndGiveArguments() {
        var commands = FunnelTabSuggestions.complete(new String[]{""}, true, true, ONLINE);
        assertTrue(commands.containsAll(List.of("give", "pack", "reload", "status", "shield", "formation")));
        assertEquals(List.of("1","2","3","4","all"),
                FunnelTabSuggestions.complete(new String[]{"give",""}, true, true, ONLINE));
        assertEquals(List.of("3"), FunnelTabSuggestions.complete(new String[]{"GIVE","3"}, true, true, ONLINE));
        assertEquals(List.of("EmeraldREX91"),
                FunnelTabSuggestions.complete(new String[]{"give","4","emer"}, true, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give","4","EmeraldREX91",""}, true, true, ONLINE));
    }

    @Test void formationAndPlayerNameSuggestionsAreCaseInsensitive() {
        var modes = FunnelTabSuggestions.complete(new String[]{"formation",""}, false, true, ONLINE);
        for (FormationMode mode : FormationMode.values()) assertTrue(modes.contains(mode.id()));
        assertTrue(modes.contains("next"));
        assertEquals(List.of("spiral"),
                FunnelTabSuggestions.complete(new String[]{"formation","SP"}, false, true, ONLINE));
        assertEquals(List.of("East"), FunnelTabSuggestions.complete(new String[]{"pack","eas"}, true, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"pack","eas"}, false, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"formation",""}, true, false, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"status",""}, true, true, ONLINE));
    }

    @Test void consoleOnlyGetsSafeRelevantCompletions() {
        assertEquals(List.of("give","pack","reload","status"),
                FunnelTabSuggestions.complete(new String[]{""}, true, false, ONLINE));
        assertEquals(List.of("Pen4"),
                FunnelTabSuggestions.complete(new String[]{"pack","pen"}, true, false, ONLINE));
    }
}
