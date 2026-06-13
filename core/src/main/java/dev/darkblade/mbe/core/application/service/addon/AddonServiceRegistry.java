package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.service.ServiceScope;
import dev.darkblade.mbe.api.service.UnifiedServiceRegistry;
import dev.darkblade.mbe.core.application.service.DefaultServiceDescriptor;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @deprecated Use UnifiedServiceRegistry instead.
 */
@Deprecated
public final class AddonServiceRegistry {

    public enum ApiTypeEnforcementMode {
        WARN,
        ERROR
    }

    private final UnifiedServiceRegistry unifiedRegistry;
    private final CoreLogger log;
    private final ClassLoader apiClassLoader;
    private volatile ApiTypeEnforcementMode apiTypeEnforcementMode = ApiTypeEnforcementMode.ERROR;

    public AddonServiceRegistry(UnifiedServiceRegistry unifiedRegistry, CoreLogger log) {
        this.unifiedRegistry = Objects.requireNonNull(unifiedRegistry, "unifiedRegistry");
        this.log = Objects.requireNonNull(log, "log");
        this.apiClassLoader = dev.darkblade.mbe.api.MultiblockAPI.class.getClassLoader();
    }

    public void setApiTypeEnforcementMode(ApiTypeEnforcementMode mode) {
        this.apiTypeEnforcementMode = mode == null ? ApiTypeEnforcementMode.ERROR : mode;
    }

    public synchronized <T> void register(String addonId, Class<T> serviceType, T service) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(service, "service");

        validateApiType(addonId, LogPhase.SERVICE_REGISTER, "register", serviceType);

        String id = addonId + ":" + serviceType.getSimpleName().toLowerCase(java.util.Locale.ROOT);

        unifiedRegistry.registerService(new DefaultServiceDescriptor<>(
                id,
                addonId,
                serviceType,
                service,
                ServiceScope.GLOBAL,
                0,
                false,
                true));

        log.logInternal(new LogScope.Core(), LogPhase.SERVICE_REGISTER, LogLevel.DEBUG,
                "Service registered via legacy adapter", null, new LogKv[] {
                        LogKv.kv("service", serviceType.getName()),
                        LogKv.kv("provider", addonId)
                }, Set.of());
    }

    public synchronized <T> Optional<T> resolveIfEnabled(String addonId, Class<T> serviceType,
            Function<String, dev.darkblade.mbe.core.application.service.addon.domain.AddonState> stateProvider) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(stateProvider, "stateProvider");

        validateApiType(addonId, LogPhase.SERVICE_RESOLVE, "resolve", serviceType);

        // Uses unified registry
        return unifiedRegistry.resolveService(serviceType, null, null);
    }

    /**
     * @deprecated Validation removed to allow addons to expose their own service
     *             APIs.
     */
    private void validateApiType(String addonId, LogPhase phase, String op, Class<?> type) {
    }
}
