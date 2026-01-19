package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class AddonServiceRegistry {

    private enum ResolutionLogKind {
        RESOLVED,
        BLOCKED
    }

    private record ResolutionLogKey(String requesterAddonId, String providerAddonId, Class<?> serviceType, ResolutionLogKind kind, String state) {
        private ResolutionLogKey {
            requesterAddonId = requesterAddonId == null ? "" : requesterAddonId;
            providerAddonId = providerAddonId == null ? "" : providerAddonId;
            serviceType = Objects.requireNonNull(serviceType, "serviceType");
            kind = Objects.requireNonNull(kind, "kind");
            state = state == null ? "" : state;
        }
    }

    private static final Duration DEFAULT_RESOLVE_LOG_THROTTLE = Duration.ofSeconds(30);
    private static final int LOG_STATE_CLEANUP_THRESHOLD = 2048;

    private record ServiceEntry(String providerAddonId, Object service) {}

    private final Map<Class<?>, ServiceEntry> services = new HashMap<>();
    private final Map<ResolutionLogKey, Long> lastResolutionLogNanos = new HashMap<>();
    private final CoreLogger log;
    private final ClassLoader coreApiClassLoader;
    private final long resolveLogThrottleNanos;
    private final LongSupplier nowNanos;

    public AddonServiceRegistry(CoreLogger log) {
        this(log, DEFAULT_RESOLVE_LOG_THROTTLE, System::nanoTime);
    }

    AddonServiceRegistry(CoreLogger log, Duration resolveLogThrottle, LongSupplier nowNanos) {
        this.log = Objects.requireNonNull(log, "log");
        this.coreApiClassLoader = com.darkbladedev.engine.api.MultiblockAPI.class.getClassLoader();
        Duration throttle = resolveLogThrottle == null ? DEFAULT_RESOLVE_LOG_THROTTLE : resolveLogThrottle;
        long ns = throttle.toNanos();
        this.resolveLogThrottleNanos = ns <= 0L ? DEFAULT_RESOLVE_LOG_THROTTLE.toNanos() : ns;
        this.nowNanos = Objects.requireNonNull(nowNanos, "nowNanos");
    }

    public synchronized <T> void register(String addonId, Class<T> serviceType, T service) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(service, "service");

        validateCoreApiType(addonId, LogPhase.SERVICE_REGISTER, "register", serviceType);

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

    public synchronized <T> Optional<T> resolveIfEnabled(String addonId, Class<T> serviceType, Function<String, AddonManager.AddonState> stateProvider) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(stateProvider, "stateProvider");

        validateCoreApiType(addonId, LogPhase.SERVICE_RESOLVE, "resolve", serviceType);

        ServiceEntry entry = services.get(serviceType);
        if (entry == null) {
            return Optional.empty();
        }

        AddonManager.AddonState state = stateProvider.apply(entry.providerAddonId());
        if (state != AddonManager.AddonState.ENABLED) {
            if (shouldLogResolution(addonId, entry.providerAddonId(), serviceType, ResolutionLogKind.BLOCKED, state.name())) {
                log.logInternal(new LogScope.Core(), LogPhase.SERVICE_RESOLVE, LogLevel.DEBUG, "Service resolve blocked: provider not enabled", null, new LogKv[] {
                    LogKv.kv("service", serviceType.getName()),
                    LogKv.kv("provider", entry.providerAddonId()),
                    LogKv.kv("state", state.name())
                }, Set.of());
            }
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

        if (shouldLogResolution(addonId, entry.providerAddonId(), serviceType, ResolutionLogKind.RESOLVED, "")) {
            log.logInternal(new LogScope.Core(), LogPhase.SERVICE_RESOLVE, LogLevel.TRACE, "Service resolved", null, new LogKv[] {
                LogKv.kv("service", serviceType.getName()),
                LogKv.kv("provider", entry.providerAddonId())
            }, Set.of());
        }
        return Optional.of(serviceType.cast(svc));
    }

    private boolean shouldLogResolution(String requesterAddonId, String providerAddonId, Class<?> serviceType, ResolutionLogKind kind, String state) {
        long now = nowNanos.getAsLong();
        ResolutionLogKey key = new ResolutionLogKey(requesterAddonId, providerAddonId, serviceType, kind, state);
        Long last = lastResolutionLogNanos.get(key);
        if (last != null && (now - last) < resolveLogThrottleNanos) {
            return false;
        }
        lastResolutionLogNanos.put(key, now);
        cleanupResolutionLogState(now);
        return true;
    }

    private void cleanupResolutionLogState(long now) {
        if (lastResolutionLogNanos.size() < LOG_STATE_CLEANUP_THRESHOLD) {
            return;
        }
        long cutoff = now - (resolveLogThrottleNanos * 5L);
        lastResolutionLogNanos.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private void validateCoreApiType(String addonId, LogPhase phase, String op, Class<?> type) {
        ClassLoader cl = type.getClassLoader();
        if (cl == coreApiClassLoader) {
            return;
        }

        log.logInternal(new LogScope.Core(), phase, LogLevel.FATAL,
            "Invalid service type (must belong to core-api)",
            null,
            new LogKv[] {
                LogKv.kv("addonId", addonId),
                LogKv.kv("op", op),
                LogKv.kv("service", type.getName()),
                LogKv.kv("serviceCl", cl == null ? "bootstrap" : cl.toString()),
                LogKv.kv("coreApiCl", coreApiClassLoader == null ? "bootstrap" : coreApiClassLoader.toString())
            },
            Set.of()
        );

        throw new IllegalArgumentException(
            "Invalid service " + op + ": Service type " + type.getName() + " is not part of core-api. " +
                "Move the service interface/DTOs to core-api and depend on it as compileOnly."
        );
    }
}
