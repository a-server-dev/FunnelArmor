package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Catch a regression to solid iron/slab-looking weapon hulls. */
class GnQuantaSilhouetteReworkTest {
    private JsonObject model(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
                "models","gn_sword_v_"+mode+".bbmodel"))).getAsJsonObject();
    }
    private JsonObject game(String mode) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
                "resourcepack","assets","funnel","models","item",
                "gn_sword_v_"+mode+".json"))).getAsJsonObject();
    }
    private double d(JsonObject c,String f,int a) {
        return c.getAsJsonArray(f).get(a).getAsDouble();
    }
    private Set<String> names(String mode) throws Exception {
        Set<String> s=new HashSet<>();
        for(var e:model(mode).getAsJsonArray("elements"))
            s.add(e.getAsJsonObject().get("name").getAsString());
        return s;
    }
    @Test void formerlySolidSwordReceiverNowIsRecessed() throws Exception {
        JsonArray els=model("sword").getAsJsonArray("elements");
        JsonObject originalCore=els.get(5).getAsJsonObject();
        double width=d(originalCore,"to",0)-d(originalCore,"from",0);
        double height=d(originalCore,"to",1)-d(originalCore,"from",1);
        assertTrue(width<=3.6,"Large white ironing-board receiver must be removed");
        assertTrue(height<2,"Internal core should not cover all outer armour");
        var n=names("sword");
        assertTrue(n.contains("GN Sword V recessed green central power core"));
        assertTrue(n.contains("GN Sword V razor crystal thick spine 23"));
        assertTrue(n.contains("Quanta staggered royal blue top crest 0"));
    }
    @Test void convertedRifleCannotRevertToSimpleBox() throws Exception {
        JsonArray els=model("rifle").getAsJsonArray("elements");
        JsonObject receiver=els.get(4).getAsJsonObject();
        assertTrue(d(receiver,"to",0)-d(receiver,"from",0)<3.7);
        assertTrue(d(receiver,"to",1)-d(receiver,"from",1)<4.0);
        var n=names("rifle");
        assertTrue(n.contains("GN V rifle green condenser luminous inset"));
        assertTrue(n.contains("GN V rifle tapered silver sword-shell upper edge 17"));
    }
    @Test void busterRifleGreenBladesCarryDistinctArmoredHoods() throws Exception {
        var n=names("buster_rifle");
        for(String side:List.of("L","R")) {
            assertTrue(n.contains("GN QanT "+side+" long tapered sapphire sword-bit hood 21"));
            assertTrue(n.contains("GN QanT "+side+" emerald translucent-inspired chamfer 13"));
        }
    }
    @Test void busterSwordHasMirroredDoubleFaceElements() throws Exception {
        JsonObject source=model("buster_sword");
        Set<String> bounds=new HashSet<>();
        for(var e:source.getAsJsonArray("elements")) {
            JsonObject c=e.getAsJsonObject();
            bounds.add(key(c,false));
        }
        for(var e:source.getAsJsonArray("elements"))
            assertTrue(bounds.contains(key(e.getAsJsonObject(),true)),
                    "Every Buster Sword cuboid should have a true rear twin");
        var n=names("buster_sword");
        assertTrue(n.contains("Buster Sword mono-edge crystal spear tip 15 front"));
        assertTrue(n.contains("Buster Sword mono-edge crystal spear tip 15 back"));
    }
    private String key(JsonObject c,boolean mirrored){
        double[] f=new double[3],t=new double[3];
        for(int i=0;i<3;i++){f[i]=d(c,"from",i);t[i]=d(c,"to",i);}
        if(mirrored){double b=18.44-t[2],e=18.44-f[2];f[2]=b;t[2]=e;}
        return String.format(java.util.Locale.ROOT,"%.3f:%.3f:%.3f:%.3f:%.3f:%.3f",
                    f[0],f[1],f[2],t[0],t[1],t[2]);
    }
    @Test void blockbenchAndPackHaveIdenticalCuboids() throws Exception {
        for(var mode:List.of("sword","rifle","buster_rifle","buster_sword")){
            JsonArray a=model(mode).getAsJsonArray("elements");
            JsonArray b=game(mode).getAsJsonArray("elements");
            assertEquals(a.size(),b.size(),mode);
            for(int i=0;i<a.size();i++){
                assertEquals(a.get(i).getAsJsonObject().get("from"),b.get(i).getAsJsonObject().get("from"));
                assertEquals(a.get(i).getAsJsonObject().get("to"),b.get(i).getAsJsonObject().get("to"));
            }
        }
    }
}
