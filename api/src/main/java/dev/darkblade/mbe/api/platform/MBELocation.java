package dev.darkblade.mbe.api.platform;

/**
 * Platform-agnostic representation of a Location.
 */
public interface MBELocation {

    /**
     * @return the world this location belongs to
     */
    MBEWorld getWorld();

    /**
     * @return the x coordinate
     */
    double getX();

    /**
     * @return the y coordinate
     */
    double getY();

    /**
     * @return the z coordinate
     */
    double getZ();

    /**
     * @return the block x coordinate
     */
    int getBlockX();

    /**
     * @return the block y coordinate
     */
    int getBlockY();

    /**
     * @return the block z coordinate
     */
    int getBlockZ();

    /**
     * @return the yaw
     */
    float getYaw();

    /**
     * @return the pitch
     */
    float getPitch();

    /**
     * @return the block at this location
     */
    MBEBlock getBlock();
}
