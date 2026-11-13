package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShieldGuardGeometryTest {
    @Test
    void shieldOnlyBelongsToUnicornVariant() {
        for (int v = 1; v <= 3; v++) assertEquals(0, ShieldGuardGeometry.shieldCount(v));
        assertEquals(3, ShieldGuardGeometry.shieldCount(4));
    }

    @Test
    void blocksInboundArrowsAcrossFrontField() {
        assertTrue(ShieldGuardGeometry.intercepts(0, 1.3, 4.0, 0, 1.3, 1.0));
        assertTrue(ShieldGuardGeometry.intercepts(-1.1, 1.5, 2.8, -1.1, 1.5, 0.8));
        assertTrue(ShieldGuardGeometry.intercepts(1.6, 2.5, 2.2, 1.6, 2.5, 1.4));
    }

    @Test
    void doesNotBlockRearSideOrOutgoingProjectiles() {
        assertFalse(ShieldGuardGeometry.intercepts(0, 1, -3, 0, 1, -1));
        assertFalse(ShieldGuardGeometry.intercepts(0, 1, 1, 0, 1, 4));
        assertFalse(ShieldGuardGeometry.intercepts(0, 1, 2, 0, 1, 3));
    }

    @Test
    void doesNotBlockOutsideWidthOrHeight() {
        assertFalse(ShieldGuardGeometry.intercepts(3, 1, 4, 3, 1, 1));
        assertFalse(ShieldGuardGeometry.intercepts(0, 3.5, 4, 0, 3.5, 1));
        assertFalse(ShieldGuardGeometry.intercepts(0, -0.5, 4, 0, -0.5, 1));
    }

    @Test
    void rejectsNonFiniteAndFallbackOnlyFrontApproaches() {
        assertFalse(ShieldGuardGeometry.intercepts(Double.NaN, 1, 3, 0, 1, 0));
        assertTrue(ShieldGuardGeometry.blocksImpact(0.8, -0.2));
        assertFalse(ShieldGuardGeometry.blocksImpact(-2.0, -0.2));
        assertFalse(ShieldGuardGeometry.blocksImpact(0.8, 0.2));
    }
}
