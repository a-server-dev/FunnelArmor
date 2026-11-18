package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FunnelTabSuggestionsTest {
    private static final List<String> ONLINE = List.of("EmeraldREX91", "East", "Pen4");

    @Test void playerGetsRelevantCommandsButNoAdminActions() {
        var result = FunnelTabSuggestions.complete(new String[] {""}, false, true, ONLINE);
        assertEquals(List.of("aerial", "barrier", "buster", "formation", "gn", "pack", "shield", "status"), result);
        assertEquals(List.of("pack"), FunnelTabSuggestions.complete(new String[]{"Pa"}, false, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give",""}, false, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"angle","-"}, false, true, ONLINE));
    }

    @Test void adminGetsAllSubcommandsAndGiveArguments() {
        var commands = FunnelTabSuggestions.complete(new String[]{""}, true, true, ONLINE);
        assertTrue(commands.containsAll(List.of("aerial", "buster", "gn", "diagnose", "give", "angle", "pack", "reload", "status", "shield", "formation")));
        assertEquals(List.of("diagnose"),
                FunnelTabSuggestions.complete(new String[]{"diag"}, true, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"diag"}, false, true, ONLINE));
        assertEquals(List.of("1","3","4","all"),
                FunnelTabSuggestions.complete(new String[]{"give",""}, true, true, ONLINE));
        assertEquals(List.of("3"), FunnelTabSuggestions.complete(new String[]{"GIVE","3"}, true, true, ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give","2"}, true, true, ONLINE));
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

    @Test void qubeleyVariantNeverSuggestsSpiral() {
        var list = FunnelTabSuggestions.complete(new String[]{"formation",""}, false,
                true, 3, ONLINE);
        assertEquals(List.of("front","next","orbit","rear","wings"), list);
        assertEquals(List.of(), FunnelTabSuggestions.complete(
                new String[]{"formation","spi"}, false, true, 3, ONLINE));
        assertEquals(List.of("spiral"), FunnelTabSuggestions.complete(
                new String[]{"formation","spi"}, false, true, 2, ONLINE));
    }

    @Test void adminCanCompleteSignedAngles() {
        assertEquals(List.of("-10", "-15", "-6"),
                FunnelTabSuggestions.complete(new String[]{"angle","-"}, true, true, ONLINE));
        assertEquals(List.of("6"),
                FunnelTabSuggestions.complete(new String[]{"angle","6"}, true, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"angle","-6",""}, true, true, ONLINE));
    }

    @Test void aerialCompletionsAreVisibleAndPermissionScoped() {
        assertEquals(List.of("aerial"),
                FunnelTabSuggestions.complete(new String[]{"aer"}, false, true, ONLINE));
        assertEquals(List.of("status"),
                FunnelTabSuggestions.complete(new String[]{"aerial", ""}, false, true, ONLINE));
        assertEquals(List.of("give", "preview", "status"),
                FunnelTabSuggestions.complete(new String[]{"aerial", ""}, true, true, ONLINE));
        assertEquals(List.of("1", "10", "11"),
                FunnelTabSuggestions.complete(new String[]{"aerial", "preview", "1"}, true, true, ONLINE));
        assertEquals(List.of("shield"),
                FunnelTabSuggestions.complete(new String[]{"aerial", "preview", "sh"}, true, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"aerial", "preview", ""}, false, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"aerial", "preview", ""}, true, false, ONLINE));
        assertEquals(List.of("give"),
                FunnelTabSuggestions.complete(new String[]{"AERIAL", "Gi"}, true, true, ONLINE));
        assertEquals(List.of("East"),
                FunnelTabSuggestions.complete(new String[]{"aerial", "give", "ea"}, true, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"aerial", "give", "ea"}, false, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"aerial", "status", ""}, false, true, ONLINE));
    }

    @Test void busterCompletionsRespectPermissions() {
        assertEquals(List.of("buster"),
                FunnelTabSuggestions.complete(new String[]{"bust"}, false, true, ONLINE));
        assertEquals(List.of("status"),
                FunnelTabSuggestions.complete(new String[]{"buster", ""}, false, true, ONLINE));
        assertEquals(List.of("give","status"),
                FunnelTabSuggestions.complete(new String[]{"buster", ""}, true, true, ONLINE));
        assertEquals(List.of("East"),
                FunnelTabSuggestions.complete(new String[]{"buster","give","ea"}, true, true, ONLINE));
        assertEquals(List.of(),
                FunnelTabSuggestions.complete(new String[]{"buster","give",""}, false, true, ONLINE));
    }

    @Test void gnSwordCompletionRespectsPermissionScope() {
        assertEquals(List.of("gn"), FunnelTabSuggestions.complete(
                new String[]{"gn"}, false, true, ONLINE));
        assertEquals(List.of("status"), FunnelTabSuggestions.complete(
                new String[]{"gn",""}, false, true, ONLINE));
        assertEquals(List.of("give","status"), FunnelTabSuggestions.complete(
                new String[]{"gn",""}, true, true, ONLINE));
        assertEquals(List.of("East"), FunnelTabSuggestions.complete(
                new String[]{"gn","give","Ea"}, true, true, ONLINE));
    }

    @Test void consoleOnlyGetsSafeRelevantCompletions() {
        assertEquals(List.of("aerial","angle","buster","diagnose","give","gn","pack","reload","status"),
                FunnelTabSuggestions.complete(new String[]{""}, true, false, ONLINE));
        assertEquals(List.of("Pen4"),
                FunnelTabSuggestions.complete(new String[]{"pack","pen"}, true, false, ONLINE));
    }
}
