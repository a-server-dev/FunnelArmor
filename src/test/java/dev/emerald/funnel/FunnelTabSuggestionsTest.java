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

    @Test void publicUsersSeeOnlyPermittedCommandsAndNoTargetNames(){
        var a=new FunnelCommandAccess.Access(false,true,true,true);
        var top=FunnelTabSuggestions.complete(new String[]{""},a,true,1,ONLINE);
        assertTrue(top.containsAll(List.of("pack","status","formation","shield","barrier","hammer","aerial")));
        assertFalse(top.contains("give"));assertFalse(top.contains("reload"));assertFalse(top.contains("diagnose"));assertFalse(top.contains("angle"));
        assertEquals(List.of("status"),FunnelTabSuggestions.complete(new String[]{"aerial",""},a,true,1,ONLINE));
        assertEquals(List.of(),FunnelTabSuggestions.complete(new String[]{"pack",""},a,true,1,ONLINE));
        assertEquals(List.of(),FunnelTabSuggestions.complete(new String[]{"give",""},a,true,1,ONLINE));
        assertEquals(List.of(),FunnelTabSuggestions.complete(new String[]{"aerial","preview",""},a,true,1,ONLINE));
        assertEquals(List.of("pack"),FunnelTabSuggestions.complete(new String[]{""},new FunnelCommandAccess.Access(false,true,false,false),true,1,ONLINE));
    }

    @Test void fullAdminCommandsAndConsoleCompletionStayAvailable() {
        var player = FunnelTabSuggestions.complete(new String[]{""},true,true,ONLINE);
        assertTrue(player.containsAll(List.of("give","pack","status","aerial",
            "brau","buster","gn","gn2","gnv","saber","hawk",
            "atomic","mace","satellite","epyon","heavyarms","freedom","hammer","destiny","formation",
            "barrier","shield","diagnose","reload","angle")));
        var console=FunnelTabSuggestions.complete(new String[]{""},true,false,ONLINE);
        assertTrue(console.containsAll(List.of("give","pack","status","diagnose")));
        assertFalse(console.contains("formation"));
        assertFalse(console.contains("shield"));
    }

    @Test void giveAllIncludesEveryAvailableEquipmentAndSupportsNamedRecipient() {
        List<String> offers = FunnelTabSuggestions.complete(
            new String[]{"give",""},true,true,ONLINE);
        assertEquals(List.of("all"), offers);
        for(String kind:FunnelGiftCatalog.ALL) assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give",kind,""},true,true,ONLINE));
        for (String kind : List.of("gn","gn2","gnv")) {
            assertFalse(offers.contains(kind));
            assertEquals(List.of(), FunnelTabSuggestions.complete(new String[]{"give",kind,""},true,true,ONLINE));
        }
        assertEquals(List.of("East"), FunnelTabSuggestions.complete(
            new String[]{"give","all","ea"},true,true,ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{"give","brAu","pe"},true,true,ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{"give","2",""},true,true,ONLINE));
        assertEquals(List.of(), FunnelTabSuggestions.complete(
            new String[]{"give","all","Pen4",""},true,true,ONLINE));
    }

    @Test void weaponSpecificGiveCompletionsAreRemovedButStatusRemains() {
        for (String weapon : List.of("aerial","brau","buster","gn","gnv",
                "gn2","saber","hawk","atomic","mace","satellite","epyon","heavyarms","freedom","hammer","destiny")) {
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
