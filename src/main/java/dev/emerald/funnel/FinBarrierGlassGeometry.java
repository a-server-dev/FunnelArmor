package dev.emerald.funnel;

import java.util.List;

/** Render-only geometry matching the existing head-height barrier hitbox. */
public final class FinBarrierGlassGeometry {
    private FinBarrierGlassGeometry() { }

    /** A Minecraft glass BlockDisplay's cube starts at [0,0,0]. */
    public record Panel(double right, double up, double forward,
                        double sizeRight, double sizeUp, double sizeForward) {
        public Panel {
            if (!Double.isFinite(right) || !Double.isFinite(up) || !Double.isFinite(forward)
                    || !Double.isFinite(sizeRight) || !Double.isFinite(sizeUp)
                    || !Double.isFinite(sizeForward)
                    || sizeRight <= 0 || sizeUp <= 0 || sizeForward <= 0)
                throw new IllegalArgumentException("Invalid glass panel");
        }
    }

    /**
     * World-space offset of the unit BlockDisplay cube's lower corner and
     * the yaw of its transformation (NOT the entity's yaw).
     * The entity itself must have zero yaw/pitch. Otherwise its inherited
     * player yaw compounds/cancels the transformation and separates the walls.
     */
    public record Placement(double x, double y, double z, double modelYawRadians) {}

    public static Placement placement(Panel panel, double playerYawDegrees) {
        if (panel == null || !Double.isFinite(playerYawDegrees))
            throw new IllegalArgumentException("Invalid glass placement");
        double yaw = Math.toRadians(playerYawDegrees);
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        return new Placement(
                cos * panel.right() - sin * panel.forward(),
                panel.up(),
                sin * panel.right() + cos * panel.forward(),
                -yaw);
    }

    /** Four walls and ceiling, no floor; panels have visual-only thickness. */
    public static List<Panel> panels(double thickness) {
        if (!Double.isFinite(thickness) || thickness < 0.005 || thickness > 0.2)
            throw new IllegalArgumentException("Invalid glass thickness");
        double hw = FinBarrierGeometry.HALF_WIDTH;
        double floor = FinBarrierGeometry.FLOOR;
        double top = FinBarrierGeometry.TOP;
        double size = hw * 2;
        double height = top - floor;
        double half = thickness / 2;
        return List.of(
                new Panel(-hw, floor, -hw - half, size, height, thickness), // back
                new Panel(-hw, floor,  hw - half, size, height, thickness), // front
                new Panel(-hw - half, floor, -hw, thickness, height, size), // left
                new Panel( hw - half, floor, -hw, thickness, height, size), // right
                new Panel(-hw, top - half, -hw, size, thickness, size)       // ceiling
        );
    }
}
