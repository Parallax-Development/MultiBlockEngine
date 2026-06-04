package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.service.ResolutionPolicy;
import dev.darkblade.mbe.api.service.ServiceDescriptor;
import dev.darkblade.mbe.api.service.ServiceScope;
import dev.darkblade.mbe.api.service.UnifiedServiceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultUnifiedServiceRegistry implements UnifiedServiceRegistry {
    private final Map<String, ServiceDescriptor<?>> byIdIndex = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<ServiceDescriptor<?>>> byTypeIndex = new ConcurrentHashMap<>();
    private final ResolutionPolicy resolutionPolicy;

    public DefaultUnifiedServiceRegistry(ResolutionPolicy resolutionPolicy) {
        this.resolutionPolicy = resolutionPolicy;
    }

    @Override
    public <T> void registerService(ServiceDescriptor<T> descriptor) {
        byIdIndex.put(descriptor.getServiceId(), descriptor);
        byTypeIndex.computeIfAbsent(descriptor.getServiceType(), k -> new CopyOnWriteArrayList<>()).add(descriptor);
    }

    @Override
    public void unregisterService(String serviceId) {
        ServiceDescriptor<?> descriptor = byIdIndex.remove(serviceId);
        if (descriptor != null) {
            List<ServiceDescriptor<?>> list = byTypeIndex.get(descriptor.getServiceType());
            if (list != null) {
                list.remove(descriptor);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> resolveService(Class<T> type, String ownerHint, ServiceScope scope) {
        List<ServiceDescriptor<T>> typedCandidates = new ArrayList<>();
        for (ServiceDescriptor<?> c : byIdIndex.values()) {
            if (type.isAssignableFrom(c.getServiceType())) {
                typedCandidates.add((ServiceDescriptor<T>) c);
            }
        }
        
        if (typedCandidates.isEmpty()) {
            return Optional.empty();
        }

        return resolutionPolicy.select(typedCandidates, ownerHint, scope)
                .map(ServiceDescriptor::getInstance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> resolveAll(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (ServiceDescriptor<?> descriptor : byIdIndex.values()) {
            if (type.isAssignableFrom(descriptor.getServiceType())) {
                result.add((T) descriptor.getInstance());
            }
        }
        return result;
    }

    @Override
    public Optional<Object> resolveById(String serviceId) {
        ServiceDescriptor<?> descriptor = byIdIndex.get(serviceId);
        return descriptor != null ? Optional.of(descriptor.getInstance()) : Optional.empty();
    }

    @Override
    public List<ServiceDescriptor<?>> getAllDescriptors() {
        return new ArrayList<>(byIdIndex.values());
    }
}
