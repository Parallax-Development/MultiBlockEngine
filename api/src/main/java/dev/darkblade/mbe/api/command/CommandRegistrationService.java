package dev.darkblade.mbe.api.command;

/**
 * Service responsible for registering annotated command classes using Cloud Annotations.
 * Addons should inject this service to register their own commands cleanly.
 */
public interface CommandRegistrationService {
    
    /**
     * Parses the annotations on the given object and registers the commands.
     * @param instance The object containing @Command annotated methods.
     */
    void registerCommandClass(Object instance);

}
