package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.emerald.funnel.AerialInputRouting.Intent.*;

class AerialInputRoutingTest {
    @Test void rifleWorksWhileControllerIsInBothHands() {
        assertEquals(RIFLE_TOGGLE, AerialInputRouting.route(true, true, true, true, false));
        assertEquals(NONE, AerialInputRouting.route(false, true, true, true, false));
        assertEquals(RIFLE_FIRE, AerialInputRouting.route(true, true, true, false, true));
        assertEquals(NONE, AerialInputRouting.route(false, true, true, false, true));
    }
    @Test void shieldWorksForBothHandsAndOffhandOnly() {
        assertEquals(SHIELD_TOGGLE, AerialInputRouting.route(true, true, true, false, false));
        assertEquals(SHIELD_TOGGLE, AerialInputRouting.route(false, true, true, false, false));
        assertEquals(SHIELD_TOGGLE, AerialInputRouting.route(false, false, true, false, false));
        assertEquals(SHIELD_TOGGLE, AerialInputRouting.route(false, false, true, true, false));
        assertEquals(NONE, AerialInputRouting.route(true, false, true, false, false));
    }
    @Test void normalLegacyWeaponsAreNeverHandledAsAerial() {
        for (boolean hand : new boolean[]{true,false}) {
            for (boolean sneak : new boolean[]{true,false})
                assertEquals(NONE, AerialInputRouting.route(hand, false, false, sneak, false));
        }
        assertEquals(NONE, AerialInputRouting.route(true, false, true, true, false));
    }
    @Test void mainHandFreeModeRemainsAutomatic() {
        assertEquals(NONE, AerialInputRouting.route(true, true, false, false, false));
        assertEquals(RIFLE_TOGGLE, AerialInputRouting.route(true, true, false, true, false));
        assertEquals(RIFLE_FIRE, AerialInputRouting.route(true, true, false, false, true));
    }
}
