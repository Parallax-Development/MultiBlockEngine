package dev.darkblade.mbe.api.addon;

import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDeclaration;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMetrics;
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

    default <T> void declareCrossReference(CrossReferenceDeclaration<T> declaration) {
        throw new UnsupportedOperationException("Cross-reference declaration is not available");
    }

    default <T> Optional<T> getCrossReference(String referenceId, Class<T> type) {
        return Optional.empty();
    }

    default <T> CrossReferenceHandle<T> getCrossReferenceHandle(String referenceId, Class<T> type) {
        return CrossReferenceHandle.unresolved();
    }

    default CrossReferenceMetrics getCrossReferenceMetrics() {
        return new CrossReferenceMetrics(0, 0, 0, 0L, 0L);
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
}
