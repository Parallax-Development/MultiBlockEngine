package dev.darkblade.mbe.api.platform;

/**
 * Platform-agnostic representation of an ItemStack.
 */
public interface MBEItemStack {

    /**
     * @return the type of the item as a namespaced string (e.g. "minecraft:stick")
     */
    String getType();

    /**
     * @return the amount of items in this stack
     */
    int getAmount();

    /**
     * Set the amount of items in this stack.
     * @param amount the new amount
     */
    void setAmount(int amount);
    
    /**
     * @return true if the item is basically empty/air
     */
    boolean isEmpty();
}
