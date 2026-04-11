package dev.darkblade.mbe.api.wiring;

import org.bukkit.block.BlockFace;

public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST,
    UP,
    DOWN;

    public Direction opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    public BlockFace toBlockFace() {
        return switch (this) {
            case NORTH -> BlockFace.NORTH;
            case EAST -> BlockFace.EAST;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case UP -> BlockFace.UP;
            case DOWN -> BlockFace.DOWN;
        };
    }

    public static Direction fromBlockFace(BlockFace face) {
        if (face == null) {
            return null;
        }
        return switch (face) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
            default -> null;
        };
    }
}

