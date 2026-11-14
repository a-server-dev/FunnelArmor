package dev.emerald.funnel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DroneModelScaleTest {
    @Test void qubeleyOnlyShrinksToQuarterInAllDisplays() {
        float base = DroneModelScale.forVariant(2, 1.45, 0.25);
        float small = DroneModelScale.forVariant(3, 1.45, 0.25);
        assertEquals(1.45, base, 0.00001);
        assertEquals(base / 4f, small, 0.00001);
        for (int variant : new int[] {1, 2, 4})
            assertEquals(base, DroneModelScale.forVariant(variant, 1.45, 0.25), 0.00001);
    }
    @Test void customMultiplierAndInvalidConfigHaveSafeLimits() {
        assertEquals(0.725, DroneModelScale.forVariant(3, 1.45, 0.50), 0.00001);
        assertEquals(0.3625, DroneModelScale.forVariant(3, 1.45, Double.NaN), 0.00001);
        assertEquals(0.0725, DroneModelScale.forVariant(3, 1.45, -20), 0.00001);
        assertEquals(2.90, DroneModelScale.forVariant(3, 1.45, 99), 0.00001);
        assertEquals(1.45, DroneModelScale.forVariant(4, 1.45, Double.NaN), 0.00001);
    }
}
