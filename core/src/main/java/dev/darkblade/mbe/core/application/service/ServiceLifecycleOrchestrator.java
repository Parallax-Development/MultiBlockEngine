package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ServiceLifecycleOrchestrator {
    private final MBEServiceRegistry registry;
    private final ServiceInjector injector;
    private final CoreLogger log;
    private final Map<String, List<String>> servicesByAddon = new ConcurrentHashMap<>();

    public ServiceLifecycleOrchestrator(MBEServiceRegistry registry, ServiceInjector injector, CoreLogger log) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.injector = Objects.requireNonNull(injector, "injector");
        this.log = Objects.requireNonNull(log, "log");
    }

    public void clear() {
        servicesByAddon.clear();
        registry.clear();
    }

    public void registerService(String addonId, MBEService service) {
        String owner = normalizeAddonId(addonId);
        try {
            registry.register(service);
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
    }

    public void disableServices(String addonId) {
        String owner = normalizeAddonId(addonId);
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
                registry.unregister(service.getServiceId());
            }
        }

        servicesByAddon.remove(owner);
    }

    public <T> List<T> getByType(Class<T> type) {
        return registry.getByType(type);
    }

    public <T> java.util.Optional<T> get(String serviceId, Class<T> type) {
        return registry.get(serviceId, type);
    }

    public void addListener(ServiceListener listener) {
        registry.addListener(listener);
    }

    public void removeListener(ServiceListener listener) {
        registry.removeListener(listener);
    }

    private List<MBEService> servicesOf(String addonId) {
        List<String> ids = servicesByAddon.getOrDefault(normalizeAddonId(addonId), List.of());
        List<MBEService> result = new CopyOnWriteArrayList<>();
        for (String id : ids) {
            registry.get(id, MBEService.class).ifPresent(result::add);
        }
        return List.copyOf(result);
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
