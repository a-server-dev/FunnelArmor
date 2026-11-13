package dev.emerald.funnel;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FormationGeometryTest {
    @Test
    void preservesEachVariantDroneCount() {
        assertEquals(6, FormationGeometry.countForVariant(1));
        assertEquals(6, FormationGeometry.countForVariant(2));
        assertEquals(12, FormationGeometry.countForVariant(3));
        assertEquals(3, FormationGeometry.countForVariant(4));
    }

    @Test
    void cyclesAndParsesFormationChoices() {
        assertEquals(5, FormationMode.values().length);
        assertEquals(FormationMode.WINGS, FormationMode.ORBIT.next());
        assertEquals(FormationMode.ORBIT, FormationMode.SPIRAL.next());
        for (FormationMode mode : FormationMode.values()) {
            assertEquals(mode, FormationMode.parse(mode.id()));
        }
        assertEquals(FormationMode.ORBIT, FormationMode.orDefault("unknown"));
        assertNull(FormationMode.parse("unknown"));
    }

    @Test
    void positionsAreFiniteAndDistinctForEveryStyleAndVariant() {
        for (FormationMode mode : FormationMode.values()) {
            for (int variant = 1; variant <= 4; variant++) {
                int count = FormationGeometry.countForVariant(variant);
                Set<String> places = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    var p = FormationGeometry.position(mode, variant, i, 12, 0.055, 1.65, 1.55, 0.22);
                    assertTrue(Double.isFinite(p.right()) && Double.isFinite(p.forward())
                            && Double.isFinite(p.up()) && Double.isFinite(p.yawOffset()));
                    places.add(String.format(java.util.Locale.ROOT,"%.4f/%.4f/%.4f",p.right(),p.forward(),p.up()));
                }
                assertEquals(count, places.size(), mode + " variant " + variant + " overlaps");
            }
        }
    }

    @Test
    void rearAndFrontStayOnExpectedSides() {
        for (int variant = 1; variant <= 4; variant++) {
            for (int i = 0; i < FormationGeometry.countForVariant(variant); i++) {
                var rear = FormationGeometry.position(FormationMode.REAR, variant, i, 0, 0.055, 1.65, 1.55, 0);
                var front = FormationGeometry.position(FormationMode.FRONT, variant, i, 0, 0.055, 1.65, 1.55, 0);
                assertTrue(rear.forward() < 0);
                assertTrue(front.forward() > 0);
            }
        }
    }

    @Test
    void differentModesProduceDifferentPositions() {
        Set<String> shapes = new HashSet<>();
        for (FormationMode mode : FormationMode.values()) {
            var p = FormationGeometry.position(mode, 1, 0, 10, 0.055, 1.65, 1.55, 0);
            shapes.add(p.right() + "/" + p.forward());
        }
        assertEquals(FormationMode.values().length, shapes.size());
    }

    @Test
    void spiralBuildsTallOpposedTwinStrandsRatherThanFlatOrbit() {
        for (int variant = 1; variant <= 4; variant++) {
            int count = FormationGeometry.countForVariant(variant);
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < count; i++) {
                var p = FormationGeometry.position(FormationMode.SPIRAL, variant, i,
                        0, 0.055, 1.65, 1.55, 0);
                min = Math.min(min, p.up());
                max = Math.max(max, p.up());
                // Each opposing strand shares the same level but not the same position.
                if (i % 2 == 0 && i + 1 < count) {
                    var opposite = FormationGeometry.position(FormationMode.SPIRAL, variant,
                            i + 1, 0, 0.055, 1.65, 1.55, 0);
                    assertEquals(-p.right(), opposite.right(), 1.e-9);
                    assertEquals(-p.forward(), opposite.forward(), 1.e-9);
                }
            }
            assertTrue(max - min > 3.0, "Spiral should extend through several vertical blocks");
            double orbitMin = Double.POSITIVE_INFINITY, orbitMax = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < count; i++) {
                var p = FormationGeometry.position(FormationMode.ORBIT, variant, i,
                        0, 0.055, 1.65, 1.55, 0);
                orbitMin = Math.min(orbitMin, p.up());
                orbitMax = Math.max(orbitMax, p.up());
            }
            assertTrue(max - min > orbitMax - orbitMin + 1.5,
                    "Spiral must visibly differ in vertical spread from orbit");
        }
    }

    @Test
    void spiralModelsFaceOutwardInsteadOfTowardPlayer() {
        for (int variant = 1; variant <= 4; variant++) {
            for (double ticks : new double[] {0, 12, 100}) {
                for (int i = 0; i < FormationGeometry.countForVariant(variant); i++) {
                    var p = FormationGeometry.position(FormationMode.SPIRAL, variant, i,
                            ticks, 0.055, 1.65, 1.55, 0);
                    // Local +forward, converted from Minecraft yaw convention.
                    double yaw = Math.toRadians(p.yawOffset());
                    double faceRight = -Math.sin(yaw);
                    double faceForward = Math.cos(yaw);
                    double dot = faceRight * p.right() + faceForward * p.forward();
                    assertTrue(dot > 0.5, "A spiral funnel must face away from the helix axis");
                }
            }
        }
    }
}
