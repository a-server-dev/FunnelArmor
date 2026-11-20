package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression based on 2026-11-20 live F5 feedback.
 * The prior L/R bit centers were roughly 7.5 pixels apart in X
 * and 6.2 pixels apart in Y, so the weapon looked like detached swords.
 */
class GnBusterIntegratedShapeTest {
    private JsonArray load(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
            "models", "gn_sword_v_" + mode + ".bbmodel"
        ))).getAsJsonObject().getAsJsonArray("elements");
    }
    private JsonObject find(JsonArray a, String name) {
        for(var v : a) {
            JsonObject el=v.getAsJsonObject();
            if(name.equals(el.get("name").getAsString())) return el;
        }
        fail("Missing core assembly part " + name);
        return null;
    }
    private double v(JsonObject element, String field, int axis) {
        return element.getAsJsonArray(field).get(axis).getAsDouble();
    }
    private double center(JsonObject element, int axis) {
        return (v(element,"from",axis) + v(element,"to",axis))/2.0;
    }
    @Test void twinBitRootsAreCloseToTheSharedReceiver() throws Exception {
        var a=load("buster_rifle");
        var left=find(a,"L Sword Bit deep blue shoulder");
        var right=find(a,"R Sword Bit deep blue shoulder");
        var gun=find(a,"Buster Rifle central steel truss");
        assertTrue(Math.abs(center(left,0)-center(right,0))<4.5,
            "The two sword bits must be integrated instead of separated giant swords");
        assertTrue(Math.abs(center(left,1)-center(right,1))<5.0);
        assertTrue(Math.abs(center(left,0)-center(gun,0))<2.5);
        assertTrue(Math.abs(center(right,0)-center(gun,0))<2.5);
        assertTrue(v(gun,"to",2)>22,
            "A real long central receiver must carry the two attached bits");
        assertNotNull(find(a,"Buster Rifle crossbar to upper prong"));
        assertNotNull(find(a,"Buster Rifle crossbar to lower prong"));
    }
    @Test void twoGreenCuttersKeepTheirOwnTipsAndFormOneWeaponProfile() throws Exception {
        var a=load("buster_rifle");
        for(int i=0;i<20;i++) {
            var left=find(a,"GN emerald tapered cutting edge L " + i);
            var right=find(a,"GN emerald tapered cutting edge R " + i);
            double gap=v(left,"from",1)-v(right,"to",1);
            assertTrue(gap>=-.1 && gap<=4.8,
                "The edges must be separated but not fly apart: segment " + i);
            assertTrue(Math.abs(center(left,0)-center(right,0))<4,
                "Both cutting tips should belong to one combined weapon");
            if(i==19){
                assertTrue(v(left,"to",2)>31.6);
                assertTrue(v(right,"to",2)>31.6);
            }
        }
    }
    @Test void avoidLongWhiteTubesInBladeRegion() throws Exception {
        var a=load("buster_rifle");
        int longWhiteParts=0;
        for(var e : a){
            JsonObject part=e.getAsJsonObject();
            String name=part.get("name").getAsString().toLowerCase(java.util.Locale.ROOT);
            if(name.contains("white") && v(part,"to",2)-v(part,"from",2)>4.0)
                longWhiteParts++;
        }
        assertEquals(0,longWhiteParts,"White should be small inset details, never long barrels");
    }
}
