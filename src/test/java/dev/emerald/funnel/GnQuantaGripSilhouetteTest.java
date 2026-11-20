package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GnQuantaGripSilhouetteTest {
    private JsonArray elements(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("models","gn_sword_v_"+mode+".bbmodel")))
            .getAsJsonObject().getAsJsonArray("elements");
    }
    private JsonObject named(String mode,String name) throws Exception {
        for(var x:elements(mode)){
            JsonObject el=x.getAsJsonObject();
            if(el.get("name").getAsString().equals(name))return el;
        }
        fail("Missing physical component: "+mode+"/"+name);
        return null;
    }
    private double axis(JsonObject el,String key,int axis) {
        return el.getAsJsonArray(key).get(axis).getAsDouble();
    }
    @Test void swordHasBroadVerticalBladeThatNarrowsToTip() throws Exception {
        double prev=Double.POSITIVE_INFINITY;
        for(int i=0;i<20;i++){
            JsonObject el=named("sword","QanT visible green sword blade "+i);
            double w=axis(el,"to",1)-axis(el,"from",1);
            assertTrue(w<prev,"Y-projected visible cutting blade should taper each step "+i);
            prev=w;
            if(i==0)assertTrue(w>7.0,"A broad first profile is needed to avoid the old green stick");
            if(i==19){
                assertTrue(w<1,"Tip must converge to a slim taper");
                assertTrue(axis(el,"to",2)>31.5);
            }
        }
        JsonObject hilt=named("sword","Rear blue rubber handle");
        assertTrue(axis(hilt,"to",2)<10,"Sword grip must be behind the green cutting blade");
        named("sword","GN Sword V exposed bright green GN condenser");
        named("sword","GN Sword V large green light-catcher ring frame");
    }
    @Test void busterRifleHasPhysicallyDistinctUpperLowerProngs() throws Exception {
        for(String side:List.of("L","R")){
            JsonObject blade=named("buster_rifle","GN emerald tapered cutting edge "+side+" 0");
            JsonObject hood=named("buster_rifle","GN QanT "+side+" long tapered sapphire sword-bit hood 0");
            assertTrue(axis(hood,"to",1)-axis(hood,"from",1)>0.9);
            assertTrue(axis(blade,"to",2)>16);
            named("buster_rifle",side+" Sword Bit white attachment flange");
        }
        JsonObject upper=named("buster_rifle","GN emerald tapered cutting edge L 12");
        JsonObject lower=named("buster_rifle","GN emerald tapered cutting edge R 12");
        assertTrue(axis(upper,"from",1)>axis(lower,"to",1)+0.5,
            "Two points must remain visible as distinct silhouettes even in side-on F5 view");
        JsonObject trigger=named("buster_rifle","Buster Rifle trigger block");
        assertTrue(axis(trigger,"to",2)<11,"Player grips the handle at the rear, not a GN edge");
    }
    @Test void poseMustNotRegressToNegativePitchThatDroppedRifleIntoGround() throws Exception {
        // Observed v0.16.0 Minecraft screenshots: X=-11 for Buster Rifle pointed
        // the entire assembly toward the player's feet. V0.17 carries it forward.
        for (String mode : List.of("sword", "buster_rifle")) {
            JsonObject model = JsonParser.parseString(Files.readString(
                Path.of("resourcepack", "assets", "funnel", "models", "item",
                    "gn_sword_v_" + mode + ".json"))).getAsJsonObject();
            JsonObject display = model.getAsJsonObject("display");
            for (String hand : List.of("firstperson_righthand", "firstperson_lefthand",
                    "thirdperson_righthand", "thirdperson_lefthand")) {
                JsonObject pose = display.getAsJsonObject(hand);
                JsonArray rot = pose.getAsJsonArray("rotation");
                double x = rot.get(0).getAsDouble(), y = rot.get(1).getAsDouble();
                assertTrue(x >= 28 && x <= 42,
                    mode + " " + hand + " must not revert to downward X pitch");
                assertTrue(y >= 120 && y <= 150,
                    mode + " " + hand + " should reveal both blades and the central receiver");
                assertTrue(pose.getAsJsonArray("scale").get(0).getAsDouble() <= .82,
                    mode + " " + hand + " should remain proportional to the player");
            }
        }
    }
}
