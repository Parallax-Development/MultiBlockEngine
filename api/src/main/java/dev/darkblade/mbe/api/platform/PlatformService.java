package dev.darkblade.mbe.api.platform;

import dev.darkblade.mbe.api.service.MBEService;

import java.util.Optional;
import java.util.UUID;

/**
 * Service to manage platform-agnostic representations.
 */
public interface PlatformService extends MBEService {

    /**
     * @param uuid the unique ID of the player
     * @return the platform-agnostic player, if online
     */
    Optional<MBEPlayer> getPlayer(UUID uuid);

    /**
     * @param name the exact name of the player
     * @return the platform-agnostic player, if online
     */
    Optional<MBEPlayer> getPlayerExact(String name);

    /**
     * @param uuid the unique ID of the world
     * @return the platform-agnostic world, if loaded
     */
    Optional<MBEWorld> getWorld(UUID uuid);

    /**
     * @param name the name of the world
     * @return the platform-agnostic world, if loaded
     */
    Optional<MBEWorld> getWorld(String name);

    /**
     * Unwraps a platform-agnostic object into its underlying platform-specific
     * representation.
     * For example, unwrapping an MBEPlayer into a org.bukkit.entity.Player.
     * 
     * @param wrapped the agnostic object
     * @param type    the requested underlying type class
     * @param <T>     the requested underlying type
     * @return the unwrapped object, or null if it cannot be unwrapped to the
     *         specified type
     */
    <T> T unwrap(Object wrapped, Class<T> type);

    /**
     * Wraps a platform-specific object into its platform-agnostic representation.
     * For example, wrapping a org.bukkit.entity.Player into an MBEPlayer.
     *
     * @param raw  the native platform object
     * @param type the requested agnostic type class
     * @param <T>  the requested agnostic type
     * @return the wrapped object, or null if it cannot be wrapped to the specified
     *         type
     */
    <T> T wrap(Object raw, Class<T> type);
}
