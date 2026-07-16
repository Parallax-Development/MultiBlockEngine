package dev.darkblade.mbe.api.platform;

/**
 * Platform-agnostic representation of a Block.
 */
public interface MBEBlock {

    /**
     * @return the location of the block
     */
    MBELocation getLocation();

    /**
     * @return the world this block belongs to
     */
    MBEWorld getWorld();

    /**
     * @return the x coordinate
     */
    int getX();

    /**
     * @return the y coordinate
     */
    int getY();

    /**
     * @return the z coordinate
     */
    int getZ();

    /**
     * @return the material of the block as a namespaced string (e.g. "minecraft:stone")
     */
    String getType();
}
