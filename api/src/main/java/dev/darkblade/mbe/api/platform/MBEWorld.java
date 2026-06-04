package dev.darkblade.mbe.api.platform;

import java.util.UUID;

/**
 * Platform-agnostic representation of a World.
 */
public interface MBEWorld {

    /**
     * @return the unique ID of the world
     */
    UUID getUniqueId();

    /**
     * @return the name of the world
     */
    String getName();

    /**
     * Get the block at the specified coordinates.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block at the given coordinates
     */
    MBEBlock getBlockAt(int x, int y, int z);
}
