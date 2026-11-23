package dev.emerald.funnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Based on player-supplied November 23 F5 image and red hand sketch:
 * both rows must deploy AHEAD of the torso in an X-shaped upper/lower fan.
 * The old lower single-barrel units were behind the player, forming a cross.
 */
class BrauBroForwardFormationTest {
    private JsonObject parse(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void allFourDeployedPodCentersAreInFrontAndSplitIntoTwoHeights() {
        var mounts = BrauBroWaistLayout.MOUNTS;
        assertEquals(4, mounts.size());
        for (int i=0; i<4; i++) {
            var m=mounts.get(i);
            var p=BrauBroWaistLayout.launched(m,4.2);
            assertTrue(p.forward()>=2.3 && p.forward()<=3,
                "All four cannons must be visibly in front: pod="+i);
            assertTrue(Math.abs(p.side())>=1.15 && Math.abs(p.side())<=1.6);
            assertTrue(Math.abs(m.yawOffset())<20,
                "No rear-facing -160/160-degree single-barrel emitters");
            assertEquals(i<2,m.twin(),
                "Upper left/right should be twin barrels, lower left/right singles");
        }
        var upperLeft=BrauBroWaistLayout.launched(mounts.get(0),4.2);
        var upperRight=BrauBroWaistLayout.launched(mounts.get(1),4.2);
        var lowerLeft=BrauBroWaistLayout.launched(mounts.get(2),4.2);
        var lowerRight=BrauBroWaistLayout.launched(mounts.get(3),4.2);
        assertTrue(upperLeft.height()>2.6 && upperRight.height()>2.6,
            "Upper pair should hover ABOVE player's head");
        assertTrue(lowerLeft.height()>1.1 && lowerLeft.height()<1.5);
        assertTrue(lowerRight.height()>1.1 && lowerRight.height()<1.5);
        assertTrue(upperLeft.height()-lowerLeft.height()>1.4);
        assertEquals(-upperLeft.side(),upperRight.side(),1e-9);
        assertEquals(-lowerLeft.side(),lowerRight.side(),1e-9);
        assertEquals(upperLeft.forward(),upperRight.forward(),1e-9);
        assertEquals(lowerLeft.forward(),lowerRight.forward(),1e-9);
        assertTrue(Math.abs(lowerLeft.side())>Math.abs(upperLeft.side()),
            "Low row should fan out a little wider than the upper pair");
    }

    @Test void cableBasesAllEmergeAtTheFrontWaistAndStayInsideThePods() {
        for (var m:BrauBroWaistLayout.MOUNTS) {
            var c=BrauBroWaistLayout.cableBase(m);
            assertTrue(c.forward()>.1,"Backside cables would run through torso");
            assertTrue(c.height()>.8 && c.height()<1.1);
            assertTrue(Math.abs(c.side())<.5);
            assertEquals(Math.signum(m.side()),Math.signum(c.side()));
        }
    }

    @Test void extensionDistanceAlwaysMaintainsForwardUpperLowerTopology() {
        for (double d:new double[]{2,3,4.2,6,8}) {
            var m=BrauBroWaistLayout.MOUNTS;
            for (var e:m)
                assertTrue(BrauBroWaistLayout.launched(e,d).forward()>0,
                    "No gun should ever fly to the rear at reach="+d);
            assertTrue(BrauBroWaistLayout.launched(m.get(0),d).height()
                > BrauBroWaistLayout.launched(m.get(2),d).height()+.7);
        }
    }

    @Test void detailedSingleAndTwinModelsMatchEditableSourcesAndPackedUV() throws Exception {
        for (String mode:List.of("single_cannon","twin_cannon")) {
            JsonObject bb=parse(Path.of("models","brau_bro_"+mode+".bbmodel"));
            JsonObject pack=parse(Path.of("resourcepack","assets","funnel",
                "models","item","brau_bro_"+mode+".json"));
            JsonArray source=bb.getAsJsonArray("elements");
            JsonArray elements=pack.getAsJsonArray("elements");
            assertEquals(source.size(),elements.size());
            assertTrue(source.size()>=160,"Detailed weapon must not regress to a plain box");
            assertEquals("funnel:item/brau_bro_cannon",
                pack.getAsJsonObject("textures").get("0").getAsString());
            assertTrue(bb.getAsJsonArray("outliner").size()>=10);
            for (int i=0;i<source.size();i++) {
                var a=source.get(i).getAsJsonObject();
                var b=elements.get(i).getAsJsonObject();
                assertEquals(a.get("from"),b.get("from"));
                assertEquals(a.get("to"),b.get("to"));
                for (String face:List.of("north","south","east","west","up","down")) {
                    JsonArray atlas=a.getAsJsonObject("faces")
                        .getAsJsonObject(face).getAsJsonArray("uv");
                    JsonArray packed=b.getAsJsonObject("faces")
                        .getAsJsonObject(face).getAsJsonArray("uv");
                    for (int k=0;k<4;k++) {
                        assertEquals(atlas.get(k).getAsDouble()/4,
                            packed.get(k).getAsDouble(), 1e-6,
                            mode+" "+i+" "+face+" UV parity");
                    }
                }
            }
        }
    }

    @Test void separateGreenTipsAndIrisDetailsAreBuiltOnTheActualMuzzles() throws Exception {
        for (int count=1;count<=2;count++) {
            String mode=count==1?"single_cannon":"twin_cannon";
            var a=parse(Path.of("models","brau_bro_"+mode+".bbmodel"))
                .getAsJsonArray("elements");
            for(int barrel=1;barrel<=count;barrel++) {
                assertNotNull(find(a,"Emitter "+barrel+" brass halo crown"));
                assertNotNull(find(a,"Emitter "+barrel+" brass halo chin"));
                var base=find(a,"Emitter "+barrel+" emerald progressively tapered cone 0");
                var tip=find(a,"Emitter "+barrel+" emerald progressively tapered cone 4");
                assertTrue(tip.getAsJsonArray("from").get(2).getAsDouble()
                    > base.getAsJsonArray("to").get(2).getAsDouble());
                assertTrue(width(tip,0)<width(base,0));
                assertTrue(width(tip,1)<width(base,1));
            }
            assertNotNull(find(a,"Rear heavy black cable socket"));
            assertNotNull(find(a,"Top purple locking fastener"));
            assertNotNull(find(a,"Left vertical ventilation louvre 5"));
            assertNotNull(find(a,"Right vertical ventilation louvre 5"));
        }
    }

    private JsonObject find(JsonArray array,String name) {
        for (var element:array) {
            var e=element.getAsJsonObject();
            if (e.get("name").getAsString().equals(name))return e;
        }
        fail("Missing named detail "+name);
        return null;
    }
    private double width(JsonObject element,int axis) {
        return element.getAsJsonArray("to").get(axis).getAsDouble()
            - element.getAsJsonArray("from").get(axis).getAsDouble();
    }
}
