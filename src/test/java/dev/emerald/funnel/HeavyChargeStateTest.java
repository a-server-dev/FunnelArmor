package dev.emerald.funnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HeavyChargeStateTest {
    @Test void foldingPreservesChargeAndNeverAutoSpendsShots(){
        var c=new HeavyChargeState(4,3);
        c.resume();assertFalse(c.advance());assertFalse(c.advance());
        c.pause();assertEquals(50,c.percent());assertFalse(c.active());
        assertFalse(c.advance());assertEquals(50,c.percent());
        c.resume();assertFalse(c.advance());assertTrue(c.advance());
        assertFalse(c.active());assertTrue(c.ready());assertEquals(3,c.shots());
        assertFalse(c.advance());c.pause();c.resume();assertFalse(c.active());
        c.consume();assertEquals(2,c.shots());c.consume();assertEquals(1,c.shots());
        c.consume();assertFalse(c.ready());assertEquals(0,c.percent());
    }
    @Test void atomicRequiresOneManualShot(){
        var c=new HeavyChargeState(1,1);
        c.resume();assertTrue(c.advance());assertEquals(1,c.shots());
        assertFalse(c.advance());c.consume();assertFalse(c.ready());
        assertThrows(IllegalStateException.class,c::consume);
    }
}
