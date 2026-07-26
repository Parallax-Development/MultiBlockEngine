package dev.darkblade.mbe.preview;

public enum Rotation {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Rotation nextClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public static Rotation fromYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 45 && yaw < 135) {
            return WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return NORTH;
        } else if (yaw >= 225 && yaw < 315) {
            return EAST;
        } else {
            return SOUTH;
        }
    }
}
