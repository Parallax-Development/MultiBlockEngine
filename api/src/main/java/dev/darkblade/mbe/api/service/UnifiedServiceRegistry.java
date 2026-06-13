package dev.darkblade.mbe.api.service;

import java.util.List;
import java.util.Optional;

public interface UnifiedServiceRegistry {
    <T> void registerService(ServiceDescriptor<T> descriptor);
    void unregisterService(String serviceId);
    
    <T> Optional<T> resolveService(Class<T> type, String ownerHint, ServiceScope scope);
    <T> List<T> resolveAll(Class<T> type);
    
    Optional<Object> resolveById(String serviceId);
    List<ServiceDescriptor<?>> getAllDescriptors();
}
