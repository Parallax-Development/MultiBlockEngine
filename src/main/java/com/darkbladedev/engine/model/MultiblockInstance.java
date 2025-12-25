package com.darkbladedev.engine.model;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public record MultiblockInstance(
    MultiblockType type,
    Location anchorLocation,
    BlockFace facing,
    MultiblockState state
) {
    public MultiblockInstance(MultiblockType type, Location anchorLocation, BlockFace facing) {
        this(type, anchorLocation, facing, MultiblockState.ACTIVE);
    }

    public MultiblockInstance withState(MultiblockState newState) {
        return new MultiblockInstance(type, anchorLocation, facing, newState);
    }
}
