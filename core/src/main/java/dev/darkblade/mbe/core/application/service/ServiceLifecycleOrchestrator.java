package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceDescriptor;
import dev.darkblade.mbe.api.service.ServiceListener;
import dev.darkblade.mbe.api.service.ServiceScope;
import dev.darkblade.mbe.api.service.UnifiedServiceRegistry;
import dev.darkblade.mbe.core.application.service.tick.TickService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public final class ServiceLifecycleOrchestrator {
    public enum LifecyclePhase {
        CORE_SERVICES,
        ADDON_SERVICES,
        CONTENT_REGISTRATION,
        RUNTIME
    }

    private final UnifiedServiceRegistry registry;
    private final ServiceInjector injector;
    private final CoreLogger log;
    private final Map<String, List<String>> servicesByAddon = new ConcurrentHashMap<>();
    private final AtomicReference<LifecyclePhase> currentPhase = new AtomicReference<>(LifecyclePhase.CORE_SERVICES);

    public ServiceLifecycleOrchestrator(UnifiedServiceRegistry registry, ServiceInjector injector, CoreLogger log) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.injector = Objects.requireNonNull(injector, "injector");
        this.log = Objects.requireNonNull(log, "log");
    }

    public void clear() {
        stopTickServiceIfPresent("unknown");
        currentPhase.set(LifecyclePhase.CORE_SERVICES);
        servicesByAddon.clear();
        // Warning: clearing through orchestrator only unregisters MBEServices.
        for (MBEService service : registry.resolveAll(MBEService.class)) {
            registry.unregisterService(service.getServiceId());
        }
    }

    public void clearAddons() {
        stopTickServiceIfPresent("unknown");
        currentPhase.set(LifecyclePhase.CORE_SERVICES);
        for (String addonId : new java.util.ArrayList<>(servicesByAddon.keySet())) {
            if (!"mbe:core".equals(addonId)) {
                for (String serviceId : servicesByAddon.getOrDefault(addonId, List.of())) {
                    registry.unregisterService(serviceId);
                }
                servicesByAddon.remove(addonId);
            }
        }
    }

    public LifecyclePhase getCurrentPhase() {
        return currentPhase.get();
    }

    public void setCurrentPhase(LifecyclePhase phase) {
        if (phase != null) {
            currentPhase.set(phase);
        }
    }



    public void registerService(String addonId, MBEService service) {
        String owner = normalizeAddonId(addonId);
        try {
            registry.registerService(new DefaultServiceDescriptor<>(
                    service.getServiceId(),
                    owner,
                    (Class<MBEService>) service.getClass(),
                    service,
                    ServiceScope.GLOBAL,
                    0,
                    true,
                    true
            ));
            service.onLoad();
            servicesByAddon.compute(owner, (key, previous) -> {
                List<String> list = previous == null ? new ArrayList<>() : new ArrayList<>(previous);
                list.add(service.getServiceId());
                return List.copyOf(list);
            });
            log.logInternal(scope(owner), LogPhase.SERVICE_REGISTER, LogLevel.INFO, "Service registered", null, new LogKv[] {
                LogKv.kv("addonId", owner),
                LogKv.kv("serviceId", service.getServiceId()),
                LogKv.kv("serviceType", service.getClass().getName())
            }, Set.of());
        } catch (Throwable t) {
            log.logInternal(scope(owner), LogPhase.SERVICE_REGISTER, LogLevel.WARN, "Service registration failed", t, new LogKv[] {
                LogKv.kv("addonId", owner),
                LogKv.kv("serviceId", service == null ? "null" : service.getServiceId())
            }, Set.of());
        }
    }

    public void discoverAndRegister(String addonId, Object holder) {
        if (holder == null) {
            return;
        }
        for (Field field : fieldsOf(holder.getClass())) {
            field.setAccessible(true);
            try {
                Object value = field.get(holder);
                if (value instanceof MBEService service) {
                    registerService(addonId, service);
                }
            } catch (Throwable t) {
                log.logInternal(scope(addonId), LogPhase.SERVICE_REGISTER, LogLevel.WARN, "Service discovery field access failed", t, new LogKv[] {
                    LogKv.kv("addonId", addonId),
                    LogKv.kv("field", field.getDeclaringClass().getName() + "#" + field.getName())
                }, Set.of());
            }
        }
    }

    public void injectAddon(String addonId, Object addon) {
        injector.inject(addon, normalizeAddonId(addonId));
    }

    public void injectServices(String addonId) {
        for (MBEService service : servicesOf(addonId)) {
            injector.inject(service, normalizeAddonId(addonId));
        }
    }

    public void enableServices(String addonId) {
        String owner = normalizeAddonId(addonId);
        for (MBEService service : servicesOf(owner)) {
            try {
                service.onEnable();
                log.logInternal(scope(owner), LogPhase.ENABLE, LogLevel.DEBUG, "Service enabled", null, new LogKv[] {
                    LogKv.kv("addonId", owner),
                    LogKv.kv("serviceId", service.getServiceId())
                }, Set.of());
            } catch (Throwable t) {
                log.logInternal(scope(owner), LogPhase.ENABLE, LogLevel.WARN, "Service onEnable failed", t, new LogKv[] {
                    LogKv.kv("addonId", owner),
                    LogKv.kv("serviceId", service.getServiceId())
                }, Set.of());
            }
        }
        startTickServiceIfPresent(owner);
    }

    public void disableServices(String addonId) {
        String owner = normalizeAddonId(addonId);
        stopTickServiceIfPresent(owner);
        List<MBEService> services = new ArrayList<>(servicesOf(owner));
        Collections.reverse(services);

        for (MBEService service : services) {
            try {
                service.onDisable();
            } catch (Throwable t) {
                log.logInternal(scope(owner), LogPhase.DISABLE, LogLevel.WARN, "Service onDisable failed", t, new LogKv[] {
                    LogKv.kv("addonId", owner),
                    LogKv.kv("serviceId", service.getServiceId())
                }, Set.of());
            } finally {
                registry.unregisterService(service.getServiceId());
            }
        }

        servicesByAddon.remove(owner);
    }

    public void reloadServices(String addonId) {
        String owner = normalizeAddonId(addonId);
        for (MBEService service : servicesOf(owner)) {
            try {
                service.onReload();
                log.logInternal(scope(owner), LogPhase.RUNTIME, LogLevel.DEBUG, "Service reloaded", null, new LogKv[] {
                    LogKv.kv("addonId", owner),
                    LogKv.kv("serviceId", service.getServiceId())
                }, Set.of());
            } catch (Throwable t) {
                log.logInternal(scope(owner), LogPhase.RUNTIME, LogLevel.WARN, "Service onReload failed", t, new LogKv[] {
                    LogKv.kv("addonId", owner),
                    LogKv.kv("serviceId", service.getServiceId())
                }, Set.of());
            }
        }
    }

    public void reloadAllServices() {
        for (String addonId : getServiceIdsByAddon().keySet()) {
            reloadServices(addonId);
        }
    }

    private void startTickServiceIfPresent(String addonId) {
        for (TickService tickService : registry.resolveAll(TickService.class)) {
            try {
                tickService.start();
            } catch (Throwable t) {
                log.logInternal(scope(addonId), LogPhase.ENABLE, LogLevel.WARN, "TickService start failed", t, new LogKv[] {
                    LogKv.kv("addonId", addonId)
                }, Set.of());
            }
        }
    }

    private void stopTickServiceIfPresent(String addonId) {
        for (TickService tickService : registry.resolveAll(TickService.class)) {
            try {
                tickService.stop();
            } catch (Throwable t) {
                log.logInternal(scope(addonId), LogPhase.DISABLE, LogLevel.WARN, "TickService stop failed", t, new LogKv[] {
                    LogKv.kv("addonId", addonId)
                }, Set.of());
            }
        }
    }

    public <T> List<T> getByType(Class<T> type) {
        return registry.resolveAll(type);
    }

    public <T> java.util.Optional<T> get(String serviceId, Class<T> type) {
        return registry.resolveById(serviceId).filter(type::isInstance).map(type::cast);
    }

    public void addListener(ServiceListener listener) {
        // Listener support should be refactored to UnifiedServiceRegistry if needed.
    }

    public void removeListener(ServiceListener listener) {
        // Listener support should be refactored to UnifiedServiceRegistry if needed.
    }

    private List<MBEService> servicesOf(String addonId) {
        List<String> ids = servicesByAddon.getOrDefault(normalizeAddonId(addonId), List.of());
        List<MBEService> result = new CopyOnWriteArrayList<>();
        for (String id : ids) {
            registry.resolveById(id).filter(MBEService.class::isInstance).map(MBEService.class::cast).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    public Map<String, List<String>> getServiceIdsByAddon() {
        Map<String, List<String>> snapshot = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : servicesByAddon.entrySet()) {
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(snapshot);
    }

    public java.util.Collection<MBEService> getAllRegistered() {
        return registry.resolveAll(MBEService.class);
    }

    private static String normalizeAddonId(String addonId) {
        if (addonId == null || addonId.isBlank()) {
            return "unknown";
        }
        return addonId;
    }

    private static Field[] fieldsOf(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                fields.add(field);
            }
            cursor = cursor.getSuperclass();
        }
        return fields.toArray(Field[]::new);
    }

    private static LogScope scope(String addonId) {
        if (addonId == null || addonId.isBlank() || "unknown".equals(addonId)) {
            return new LogScope.Core();
        }
        return new LogScope.Addon(addonId, "unknown");
    }
}
