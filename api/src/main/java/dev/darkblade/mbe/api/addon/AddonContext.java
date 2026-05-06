package dev.darkblade.mbe.api.addon;

import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.assembly.MultiblockBuilder;
import dev.darkblade.mbe.api.logging.AddonLogger;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceListener;
import dev.darkblade.mbe.api.command.WrenchInteractable;
import dev.darkblade.mbe.core.domain.BlockMatcher;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.domain.condition.Condition;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface AddonContext {
    String getAddonId();
    AddonLogger getLogger();
    String getEngineVersion();
    int getApiVersion();
    MultiblockAPI getAPI();

    Path getDataFolder();

    <T> void registerService(Class<T> serviceType, T service);
    <T> T getService(Class<T> serviceType);
    default <T extends MBEService> T getMbeService(Class<T> serviceType) {
        return getService(serviceType);
    }
    default void registerService(MBEService service) {
        throw new UnsupportedOperationException("Dynamic service registration not available");
    }
    default <T> Optional<T> getService(String serviceId, Class<T> serviceType) {
        return Optional.empty();
    }
    default <T> List<T> getServicesByType(Class<T> serviceType) {
        return List.of();
    }
    default void addServiceListener(ServiceListener listener) {
    }
    default void removeServiceListener(ServiceListener listener) {
    }

    <T> void exposeService(Class<T> api, T implementation, ServicePriority priority);

    default <T> void exposeService(Class<T> api, T implementation) {
        exposeService(api, implementation, ServicePriority.Normal);
    }

    
    void registerAction(String key, Function<Map<String, Object>, Action> factory);
    void registerCondition(String key, Function<Map<String, Object>, Condition> factory);
    void registerWrenchAction(String key, WrenchInteractable interactable);
    void registerMatcher(String prefix, Function<String, BlockMatcher> factory);
    void registerListener(Listener listener);
    MultiblockBuilder createMultiblock(String id);
    void registerMultiblock(MultiblockType type);
    
    void runTask(Runnable task);
    void runTaskAsync(Runnable task);
    
    default void registerCommand(String name, org.bukkit.command.CommandExecutor executor, String... aliases) {
        registerCommand(name, executor, executor instanceof org.bukkit.command.TabCompleter tc ? tc : null, aliases);
    }

    void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter, String... aliases);

    /**
     * Gets an embedded resource from the addon JAR.
     *
     * @param filename the relative path to the resource inside the JAR
     * @return the input stream for the resource, or null if not found
     */
    @Nullable
    InputStream getResource(@NotNull String filename);

    /**
     * Saves an embedded resource from the addon JAR to the addon data folder.
     *
     * @param resourcePath the relative path to the resource inside the JAR
     * @param replace      whether to replace the file if it already exists
     * @throws IllegalArgumentException if the resource is null or empty, or if the resource does not exist
     */
    void saveResource(@NotNull String resourcePath, boolean replace);
}
