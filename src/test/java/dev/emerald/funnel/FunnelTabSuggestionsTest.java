package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FunnelTabSuggestionsTest {
    private static final List<String> ONLINE = List.of("EmeraldREX91", "East", "Pen4");

    @Test void unauthorizedPlayersSeeNoCommandsOrArgumentSuggestions() {
        for (String[] args : new String[][] {
                {""}, {"give", ""}, {"give", "all", ""}, {"pack", ""},
                {"status"}, {"aerial", "preview", ""}, {"gn", ""},
                {"formation", ""}, {"angle", ""}, {"brau", ""} }) {
            assertEquals(List.of(), FunnelTabSuggestions.complete(args,false,true,ONLINE));
        }
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{""},false,false,ONLINE));
    }

    @Test void fullAdminCommandsAndConsoleCompletionStayAvailable() {
        var player = FunnelTabSuggestions.complete(new String[]{""},true,true,ONLINE);
        assertTrue(player.containsAll(List.of("give","pack","status","aerial",
            "brau","buster","gn","gn2","gnv","saber","hawk",
            "atomic","mace","satellite","formation",
            "barrier","shield","diagnose","reload","angle")));
        var console=FunnelTabSuggestions.complete(new String[]{""},true,false,ONLINE);
        assertTrue(console.containsAll(List.of("give","pack","status","diagnose")));
        assertFalse(console.contains("formation"));
        assertFalse(console.contains("shield"));
    }

    @Test void giveAllIncludesEveryAvailableEquipmentAndSupportsNamedRecipient() {
        List<String> offers = FunnelTabSuggestions.complete(
            new String[]{"give",""},true,true,ONLINE);
        assertEquals(12, offers.size());
        assertTrue(offers.containsAll(FunnelGiftCatalog.ALL));
        assertTrue(offers.contains("all"));
        for (String kind : List.of("gn","gn2","gnv")) {
            assertFalse(offers.contains(kind));
            assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give",kind,""},true,true,ONLINE));
        }
        assertEquals(List.of("East"), FunnelTabSuggestions.complete(
            new String[]{"give","all","ea"},true,true,ONLINE));
        assertEquals(List.of("Pen4"), FunnelTabSuggestions.complete(
            new String[]{"give","brAu","pe"},true,true,ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{"give","2",""},true,true,ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{"give","all","Pen4",""},true,true,ONLINE));
    }

    @Test void weaponSpecificGiveCompletionsAreRemovedButStatusRemains() {
        for (String weapon : List.of("aerial","brau","buster","gn","gnv",
                "gn2","saber","hawk","atomic","mace","satellite")) {
            var subcommands=FunnelTabSuggestions.complete(
                new String[]{weapon,""},true,true,ONLINE);
            assertTrue(subcommands.contains("status"),weapon);
            assertFalse(subcommands.contains("give"),weapon);
            assertEquals(List.of(),FunnelTabSuggestions.complete(
                new String[]{weapon,"give",""},true,true,ONLINE));
        }
        assertEquals(List.of("preview","status"),FunnelTabSuggestions.complete(
            new String[]{"aerial",""},true,true,ONLINE));
        assertEquals(List.of("1","10","11"),FunnelTabSuggestions.complete(
            new String[]{"aerial","preview","1"},true,true,ONLINE));
        assertEquals(List.of("shield"),FunnelTabSuggestions.complete(
            new String[]{"aerial","preview","sh"},true,true,ONLINE));
    }

    @Test void formationStillUsesWeaponAvailabilityAndConsoleRestrictions() {
        var list=FunnelTabSuggestions.complete(
            new String[]{"formation",""},true,true,3,ONLINE);
        assertEquals(List.of("front","next","orbit","rear","wings"),list);
        assertEquals(List.of("spiral"),FunnelTabSuggestions.complete(
            new String[]{"formation","spi"},true,true,2,ONLINE));
        assertEquals(List.of(),FunnelTabSuggestions.complete(
            new String[]{"formation",""},true,false,ONLINE));
    }

    @Test void packAndSignedAngleCompletionsRemainAdminOnly() {
        assertEquals(List.of("East"), FunnelTabSuggestions.complete(
            new String[]{"pack","eas"},true,true,ONLINE));
        assertEquals(List.of("Pen4"), FunnelTabSuggestions.complete(
            new String[]{"pack","pen"},true,false,ONLINE));
        assertEquals(List.of("-10","-15","-6"),FunnelTabSuggestions.complete(
            new String[]{"angle","-"},true,true,ONLINE));
        assertEquals(List.of(),FunnelTabSuggestions.complete(
            new String[]{"angle","-",""},true,true,ONLINE));
    }
}
