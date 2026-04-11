package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public final class AddonServiceRegistry {

    private record ServiceEntry(String providerAddonId, Object service) {}

    private final Map<Class<?>, ServiceEntry> services = new HashMap<>();
    private final CoreLogger log;
    private final ClassLoader apiClassLoader;

    public AddonServiceRegistry(CoreLogger log) {
        this.log = Objects.requireNonNull(log, "log");
        this.apiClassLoader = dev.darkblade.mbe.api.MultiblockAPI.class.getClassLoader();
    }

    public synchronized <T> void register(String addonId, Class<T> serviceType, T service) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(service, "service");

        validateApiType(addonId, LogPhase.SERVICE_REGISTER, "register", serviceType);

        ServiceEntry existing = services.get(serviceType);
        if (existing != null && !existing.providerAddonId().equals(addonId)) {
            log.logInternal(new LogScope.Core(), LogPhase.SERVICE_REGISTER, LogLevel.ERROR, "Service already registered", null, new LogKv[] {
                LogKv.kv("service", serviceType.getName()),
                LogKv.kv("provider", existing.providerAddonId()),
                LogKv.kv("attempt", addonId)
            }, Set.of());
            throw new IllegalStateException("Service already registered: " + serviceType.getName() + " Provider=" + existing.providerAddonId());
        }

        services.put(serviceType, new ServiceEntry(addonId, service));

        log.logInternal(new LogScope.Core(), LogPhase.SERVICE_REGISTER, LogLevel.DEBUG, "Service registered", null, new LogKv[] {
            LogKv.kv("service", serviceType.getName()),
            LogKv.kv("provider", addonId)
        }, Set.of());
    }

    public synchronized <T> Optional<T> resolveIfEnabled(String addonId, Class<T> serviceType, Function<String, AddonLifecycleService.AddonState> stateProvider) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(stateProvider, "stateProvider");

        validateApiType(addonId, LogPhase.SERVICE_RESOLVE, "resolve", serviceType);

        ServiceEntry entry = services.get(serviceType);
        if (entry == null) {
            return Optional.empty();
        }

        AddonLifecycleService.AddonState state = stateProvider.apply(entry.providerAddonId());
        if (state != AddonLifecycleService.AddonState.ENABLED) {
            log.logInternal(new LogScope.Core(), LogPhase.SERVICE_RESOLVE, LogLevel.DEBUG, "Service resolve blocked: provider not enabled", null, new LogKv[] {
                LogKv.kv("service", serviceType.getName()),
                LogKv.kv("provider", entry.providerAddonId()),
                LogKv.kv("state", state.name())
            }, Set.of());
            return Optional.empty();
        }

        Object svc = entry.service();
        if (!serviceType.isInstance(svc)) {
            log.logInternal(new LogScope.Core(), LogPhase.SERVICE_RESOLVE, LogLevel.ERROR, "Service resolve failed: wrong type", null, new LogKv[] {
                LogKv.kv("service", serviceType.getName()),
                LogKv.kv("provider", entry.providerAddonId()),
                LogKv.kv("actual", svc == null ? "null" : svc.getClass().getName())
            }, Set.of());
            return Optional.empty();
        }

        log.logInternal(new LogScope.Core(), LogPhase.SERVICE_RESOLVE, LogLevel.DEBUG, "Service resolved", null, new LogKv[] {
            LogKv.kv("service", serviceType.getName()),
            LogKv.kv("provider", entry.providerAddonId())
        }, Set.of());
        return Optional.of(serviceType.cast(svc));
    }

    private void validateApiType(String addonId, LogPhase phase, String op, Class<?> type) {
        ClassLoader cl = type.getClassLoader();
        if (cl == apiClassLoader) {
            return;
        }

        log.logInternal(new LogScope.Core(), phase, LogLevel.FATAL,
            "Invalid service type (must belong to api)",
            null,
            new LogKv[] {
                LogKv.kv("addonId", addonId),
                LogKv.kv("op", op),
                LogKv.kv("service", type.getName()),
                LogKv.kv("serviceCl", cl == null ? "bootstrap" : cl.toString()),
                LogKv.kv("apiCl", apiClassLoader == null ? "bootstrap" : apiClassLoader.toString())
            },
            Set.of()
        );

        throw new IllegalArgumentException(
            "Invalid service " + op + ": Service type " + type.getName() + " is not part of api. " +
                "Move the service interface/DTOs to api and depend on it as compileOnly."
        );
    }
}
