package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.service.ServiceListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class MBEServiceRegistry {
    private static final Pattern SERVICE_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9_\\-]*:[a-z0-9][a-z0-9_.\\-]*$");

    private final ConcurrentHashMap<String, MBEService> services = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ServiceListener> listeners = new CopyOnWriteArrayList<>();

    public void register(MBEService service) {
        Objects.requireNonNull(service, "service");
        String id = normalizeServiceId(service.getServiceId());

        MBEService previous = services.putIfAbsent(id, service);
        if (previous != null && previous != service) {
            throw new IllegalStateException("Service id already registered: " + id);
        }

        if (previous == null) {
            for (ServiceListener listener : listeners) {
                listener.onServiceAvailable(service);
            }
        }
    }

    public <T> Optional<T> get(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        MBEService service = services.get(normalizeServiceId(id));
        if (service == null || !type.isInstance(service)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(service));
    }

    public Collection<MBEService> getAll() {
        return List.copyOf(services.values());
    }

    public <T> List<T> getByType(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return services.values().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    public Optional<MBEService> unregister(String id) {
        MBEService removed = services.remove(normalizeServiceId(id));
        return Optional.ofNullable(removed);
    }

    public void clear() {
        services.clear();
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
