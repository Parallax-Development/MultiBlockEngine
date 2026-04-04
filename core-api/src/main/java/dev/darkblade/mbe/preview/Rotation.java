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
}
