package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrauBroFiringAndFollowTest {
    @Test void fourReferencePodsHaveEightRealMuzzleOffsetsInTheExpectedOrder() {
        var guns = BrauBroFirePattern.BARRELS;
        assertEquals(8, guns.size());
        assertEquals(List.of(0,0,1,1,2,2,3,3),
            guns.stream().map(BrauBroFirePattern.Barrel::podIndex).toList());
        for (int i=0; i<4; i++) {
            assertEquals(-guns.get(i*2).lateral(),
                guns.get(i*2+1).lateral(), 1e-8);
            assertTrue(Math.abs(guns.get(i*2).lateral()) > .03);
        }
        assertEquals(.102, Math.abs(guns.get(4).lateral()),1e-8);
        assertEquals(.102, Math.abs(guns.get(6).lateral()),1e-8);
        assertEquals(4, new HashSet<>(guns.stream()
            .map(BrauBroFirePattern.Barrel::podIndex).toList()).size());
    }

    @Test void firingConfigCannotCreateInfiniteRangeOrUnboundedAttackRate() {
        assertEquals(4, BrauBroFirePattern.cooldown(-1));
        assertEquals(120, BrauBroFirePattern.cooldown(999));
        assertEquals(20, BrauBroFirePattern.cooldown(20));
        assertEquals(4, BrauBroFirePattern.range(-10), 1e-8);
        assertEquals(48, BrauBroFirePattern.range(999), 1e-8);
        assertEquals(30, BrauBroFirePattern.range(Double.NaN), 1e-8);
        assertEquals(0, BrauBroFirePattern.damage(-100), 1e-8);
        assertEquals(.5, BrauBroFirePattern.damage(999), 1e-8);
        assertEquals(.5, BrauBroFirePattern.damage(Double.POSITIVE_INFINITY), 1e-8);
    }

    @Test void allEightBeamsTogetherCannotExceedHalfHpPerVictim() {
        for (int hits=1; hits<=8; hits++) {
            assertEquals(.5, BrauBroFirePattern.burstDamage(.5, .5, hits), 1e-8);
            assertEquals(.5, BrauBroFirePattern.burstDamage(1.5, 12, hits), 1e-8);
        }
        assertEquals(0, BrauBroFirePattern.burstDamage(.5,.5,0), 1e-8);
        assertEquals(.2, BrauBroFirePattern.burstDamage(.5,.2,8), 1e-8);
        assertEquals(0, BrauBroFirePattern.burstDamage(0,.5,8), 1e-8);
    }

    @Test void deployedCannonsPursueMovingPlayerQuicklyButNotAsRigidAttachments() {
        double reach=4.2;
        double slow=BrauBroFirePattern.followStep(.2, reach);
        double medium=BrauBroFirePattern.followStep(1.4, reach);
        double fast=BrauBroFirePattern.followStep(5.0, reach);
        assertTrue(slow > 0 && slow < medium);
        assertTrue(medium < fast);
        assertTrue(fast <= 2.01601);
        assertEquals(fast,BrauBroFirePattern.followStep(1000,reach), 1e-8);
        assertTrue(BrauBroFirePattern.followStep(.2,2) < fast);
        for (var mount:BrauBroWaistLayout.MOUNTS) {
            var offset=BrauBroWaistLayout.launched(mount,reach);
            assertTrue(Math.abs(offset.forward()) > .5);
            assertTrue(Math.abs(offset.side()) > .5);
        }
    }

    @Test void onlyNormalRightClickFiresWhileExtendedAndOldControlsRemain() throws Exception {
        String source=Files.readString(Path.of(
            "src/main/java/dev/emerald/funnel/BrauBroWaistCannons.java"));
        assertTrue(source.contains("if (left || player.isSneaking()) toggle(player);"));
        assertTrue(source.contains("else fire(player);"));
        assertTrue(source.contains("!state.extended"));
        assertTrue(source.contains("tick < state.nextFireTick"));
        assertTrue(source.contains("BrauBroFirePattern.BARRELS"));
        assertTrue(source.contains("rayTraceBlocks("));
        assertTrue(source.contains("rayTraceEntities("));
        assertTrue(source.contains("hitCounts.merge("));
        assertTrue(source.contains("BrauBroFirePattern.burstDamage(perBeam, maxBurstDamage,"));
        assertTrue(source.contains("target.damage(damage, player)"));
        assertTrue(source.contains("Color.fromRGB(105, 240, 191)"));
        assertTrue(source.contains("state.extended ? worldOffset(base,"));
        assertFalse(source.contains("launchTargets"));
        assertFalse(source.contains("launchYaws"));
    }
}
