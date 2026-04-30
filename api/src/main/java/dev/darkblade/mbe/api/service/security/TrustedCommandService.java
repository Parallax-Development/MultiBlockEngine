package dev.darkblade.mbe.api.service.security;

import dev.darkblade.mbe.api.service.MBEService;

/**
 * Service responsible for validating commands against a whitelist.
 * This is a security measure to prevent arbitrary command execution from YAML configurations.
 */
public interface TrustedCommandService extends MBEService {

    /**
     * Checks if the given command is trusted and allowed to be executed.
     * 
     * @param command The full command string (including arguments).
     * @return true if the command is trusted, false otherwise.
     */
    boolean isTrusted(String command);

    /**
     * Reloads the whitelist from configuration.
     */
    void reload();

}
