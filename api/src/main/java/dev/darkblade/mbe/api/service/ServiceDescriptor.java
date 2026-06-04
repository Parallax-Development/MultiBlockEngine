package dev.darkblade.mbe.api.service;

public interface ServiceDescriptor<T> {
    String getServiceId();
    String getOwnerAddonId();
    Class<T> getServiceType();
    T getInstance();
    ServiceScope getScope();
    int getPriority();
    boolean isLifecycleManaged();
    boolean isExposed();
}
