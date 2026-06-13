package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceListener;
import dev.darkblade.mbe.api.service.ServiceScope;
import dev.darkblade.mbe.api.service.UnifiedServiceRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @deprecated Use UnifiedServiceRegistry instead.
 */
@Deprecated
public final class MBEServiceRegistry {
    private static final Pattern SERVICE_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_\\\\-]*:[a-z0-9][a-z0-9_.\\\\-]*$");
    private static final Logger LOGGER = Logger.getLogger(MBEServiceRegistry.class.getSimpleName());

    private final UnifiedServiceRegistry unifiedRegistry;
    private final CopyOnWriteArrayList<ServiceListener> listeners = new CopyOnWriteArrayList<>();

    public MBEServiceRegistry(UnifiedServiceRegistry unifiedRegistry) {
        this.unifiedRegistry = Objects.requireNonNull(unifiedRegistry, "unifiedRegistry");
    }

    public void register(MBEService service) {
        Objects.requireNonNull(service, "service");
        String id = normalizeServiceId(service.getServiceId());
        
        // Extract namespace as owner for legacy compatibility
        String ownerId = id.contains(":") ? id.split(":")[0] : "mbe:core";

        @SuppressWarnings("unchecked")
        Class<MBEService> clazz = (Class<MBEService>) service.getClass();

        unifiedRegistry.registerService(new DefaultServiceDescriptor<>(
                id,
                ownerId,
                clazz,
                service,
                ServiceScope.GLOBAL,
                0,
                true,
                true
        ));
        
        for (ServiceListener listener : listeners) {
            listener.onServiceAvailable(service);
        }
        LOGGER.info("MBEService registered id=" + id + " type=" + service.getClass().getName() + " via legacy adapter");
    }

    public <T> Optional<T> get(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        String normalizedId = normalizeServiceId(id);
        
        Optional<Object> result = unifiedRegistry.resolveById(normalizedId);
        if (result.isPresent() && type.isInstance(result.get())) {
            return Optional.of(type.cast(result.get()));
        }
        return Optional.empty();
    }

    public Collection<MBEService> getAll() {
        return unifiedRegistry.resolveAll(MBEService.class);
    }

    public <T> List<T> getByType(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return unifiedRegistry.resolveAll(type);
    }

    public Optional<MBEService> unregister(String id) {
        String normalized = normalizeServiceId(id);
        Optional<MBEService> service = get(normalized, MBEService.class);
        if (service.isPresent()) {
            unifiedRegistry.unregisterService(normalized);
            LOGGER.info("MBEService unregistered id=" + normalized + " via legacy adapter");
            return service;
        }
        return Optional.empty();
    }

    public void clear() {
        List<MBEService> services = unifiedRegistry.resolveAll(MBEService.class);
        for(MBEService service : services) {
            unifiedRegistry.unregisterService(service.getServiceId());
        }
    }

    public void addListener(ServiceListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(ServiceListener listener) {
        listeners.remove(listener);
    }

    private static String normalizeServiceId(String serviceId) {
        Objects.requireNonNull(serviceId, "serviceId");
        String normalized = serviceId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!SERVICE_ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid service id: " + serviceId + " (expected namespace:path)");
        }
        return normalized;
    }
}
