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

class GnQuantaGripSilhouetteTest {
    private JsonArray cubes(String key) throws Exception {
        return JsonParser.parseString(Files.readString(
                Path.of("models","gn_sword_v_"+key+".bbmodel"))).getAsJsonObject()
                .getAsJsonArray("elements");
    }
    private double val(JsonObject c,String key,int axis) {
        return c.getAsJsonArray(key).get(axis).getAsDouble();
    }
    private JsonObject named(JsonArray cubes,String name) {
        for(var element:cubes) {
            JsonObject c=element.getAsJsonObject();
            if(c.get("name").getAsString().equals(name)) return c;
        }
        fail("Missing modeled component "+name);
        return null;
    }
    @Test void swordHasSingleBroadGreenRootAndSharpTip() throws Exception {
        JsonArray elements=cubes("sword");
        assertTrue(elements.size()<250,"Avoid the previous 348-cuboid iron/slab effect");
        double first=0,last=0;
        for(int i=0;i<20;i++) {
            JsonObject c=named(elements,"Quanta single emerald blade segment "+i);
            double width=val(c,"to",0)-val(c,"from",0);
            if(i==0)first=width;
            if(i==19)last=width;
        }
        assertTrue(first>5.5,"GN Sword V must have a visible broad single cutting edge");
        assertTrue(last<1,"GN Sword V must converge to a sharp point");
        assertNotNull(named(elements,"GN Sword V black blade rotation axle"));
        assertNotNull(named(elements,"GN Sword V recessed green central power core"));
    }
    @Test void rifleGripIsShortAndGunBarrelIsNotRectangular() throws Exception {
        JsonArray elements=cubes("rifle");
        JsonObject grip=named(elements,"Power Receiver / part 001");
        double height=val(grip,"to",1)-val(grip,"from",1);
        assertTrue(height<=4.1,"Pistol grip must not reach the player's knee");
        JsonObject barrel=named(elements,"Barrel & GN Sword Bit Assembly / part 011");
        assertTrue(val(barrel,"to",2)<=22.2,"Gun barrel must not be an uninterrupted rectangular slab");
    }
    @Test void busterRifleUsesBlueArmorAboveThinnerGreenTwinBlades() throws Exception {
        JsonArray all=cubes("buster_rifle");
        for(String side:List.of("L","R")) {
            JsonObject blade=named(all,"Broad QanT GN energy cutting plane "+side+" 0");
            JsonObject armor=named(all,"GN QanT "+side+" long tapered sapphire sword-bit hood 0");
            double bladeThickness=val(blade,"to",1)-val(blade,"from",1);
            double hoodThickness=val(armor,"to",1)-val(armor,"from",1);
            assertTrue(bladeThickness<1.1,"GN blade should not resemble a thick opaque bar");
            assertTrue(hoodThickness>1,"Blue sword-bit armor must have visible physical depth");
            assertTrue(val(armor,"to",1)>val(blade,"to",1),"Armor belongs above blade, not underneath");
        }
    }
}
