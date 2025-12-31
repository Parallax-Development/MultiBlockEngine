package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;

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

    public AddonServiceRegistry(CoreLogger log) {
        this.log = Objects.requireNonNull(log, "log");
    }

    public synchronized <T> void register(String addonId, Class<T> serviceType, T service) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(service, "service");

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

    public synchronized <T> Optional<T> resolveIfEnabled(Class<T> serviceType, Function<String, AddonManager.AddonState> stateProvider) {
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(stateProvider, "stateProvider");

        ServiceEntry entry = services.get(serviceType);
        if (entry == null) {
            return Optional.empty();
        }

        AddonManager.AddonState state = stateProvider.apply(entry.providerAddonId());
        if (state != AddonManager.AddonState.ENABLED) {
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
}
