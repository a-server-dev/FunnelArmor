package dev.emerald.funnel;

/**
 * Minecraft-independent drone formation math. Coordinates use the player's
 * orientation: +forward = looking direction, +right = player's right.
 * All offsets are in blocks, with up measured from the player's feet.
 */
public final class FormationGeometry {
    private FormationGeometry() {}

    public record Position(double right, double up, double forward, double yawOffset) {}

    public static int countForVariant(int variant) {
        return switch (variant) {
            case 1, 2 -> 6;
            case 3 -> 12;
            case 4 -> 3;
            default -> 0;
        };
    }

    public static Position position(FormationMode mode, int variant, int index, double ticks,
                                    double angularSpeed, double radius, double height,
                                    double bobAmplitude) {
        int count = countForVariant(variant);
        if (count == 0 || index < 0 || index >= count) {
            throw new IllegalArgumentException("Invalid variant/index: " + variant + "/" + index);
        }

        double bob = bobAmplitude * Math.sin(ticks * 0.09 + index * 1.47);
        double up = height + bob;
        double right;
        double forward;
        double yawOffset = 0;

        switch (mode) {
            case ORBIT -> {
                int perRing = variant == 3 ? 6 : count;
                int ring = index / perRing;
                int slot = index % perRing;
                double angle = ticks * angularSpeed
                        + 2 * Math.PI * slot / perRing
                        + (ring == 0 ? 0 : Math.PI / perRing);
                double r = radius + (variant == 4 ? 0.45 : 0) + ring * 0.45;
                right = Math.cos(angle) * r;
                forward = Math.sin(angle) * r;
                up += ring * 0.75;
                yawOffset = Math.toDegrees(angle);
            }
            case WINGS -> {
                if (count % 2 == 1 && index == count - 1) {
                    right = 0;
                    forward = -0.95;
                    up += 0.65;
                } else {
                    int rank = index / 2;
                    int side = index % 2 == 0 ? -1 : 1;
                    right = side * (radius + 0.12 * (rank % 3));
                    forward = -0.35 - 0.6 * (rank / 3);
                    up += 0.48 * (rank % 3);
                }
                yawOffset = right < 0 ? -25 : (right > 0 ? 25 : 0);
            }
            case REAR -> {
                int columns = count > 6 ? 6 : count;
                int row = index / columns;
                int slot = index % columns;
                right = (slot - (columns - 1) / 2.0) * 0.64;
                forward = -radius - 0.3 * row - 0.22 * Math.abs(slot - (columns - 1) / 2.0);
                up += row * 0.64 + (slot % 2) * 0.16;
            }
            case FRONT -> {
                int columns = count > 6 ? 6 : count;
                int row = index / columns;
                int slot = index % columns;
                double spread = count == 3 ? Math.PI * 0.31 : Math.PI * 0.48;
                double angle = columns == 1 ? 0 : -spread + 2 * spread * slot / (columns - 1);
                right = Math.sin(angle) * (radius + row * 0.28);
                forward = Math.cos(angle) * (radius + row * 0.28);
                up += row * 0.66;
                if (count == 3 && slot == 1) up += 0.48;
                yawOffset = -Math.toDegrees(angle) * 0.4;
            }
            case SPIRAL -> {
                double angle = ticks * angularSpeed * 1.25
                        + (2.0 * Math.PI * 2 * index) / count;
                double r = radius + 0.18 * Math.sin(ticks * 0.035 + index);
                right = Math.cos(angle) * r;
                forward = Math.sin(angle) * r;
                up += (index - (count - 1) / 2.0) * (variant == 3 ? 0.20 : 0.32);
                yawOffset = Math.toDegrees(angle);
            }
            default -> throw new IllegalStateException("Unknown formation: " + mode);
        }

        return new Position(right, up, forward, yawOffset);
    }
}
