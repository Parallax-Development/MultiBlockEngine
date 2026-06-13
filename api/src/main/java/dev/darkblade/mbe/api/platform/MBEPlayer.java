package dev.darkblade.mbe.api.platform;

import java.util.UUID;

/**
 * Platform-agnostic representation of a Player.
 */
public interface MBEPlayer {

    /**
     * @return the unique ID of the player
     */
    UUID getUniqueId();

    /**
     * @return the name of the player
     */
    String getName();

    /**
     * Send a raw string message to the player.
     * 
     * @param message the message to send
     */
    void sendMessage(String message);

    /**
     * Check if the player has a permission.
     * 
     * @param permission the permission node
     * @return true if the player has the permission
     */
    boolean hasPermission(String permission);

    /**
     * @return the location of the player
     */
    MBELocation getLocation();
}
