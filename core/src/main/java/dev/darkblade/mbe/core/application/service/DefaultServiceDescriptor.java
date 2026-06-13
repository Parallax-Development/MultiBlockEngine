package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.service.ServiceDescriptor;
import dev.darkblade.mbe.api.service.ServiceScope;

public class DefaultServiceDescriptor<T> implements ServiceDescriptor<T> {
    private final String serviceId;
    private final String ownerAddonId;
    private final Class<T> serviceType;
    private final T instance;
    private final ServiceScope scope;
    private final int priority;
    private final boolean lifecycleManaged;
    private final boolean exposed;

    public DefaultServiceDescriptor(String serviceId, String ownerAddonId, Class<T> serviceType, T instance, ServiceScope scope, int priority, boolean lifecycleManaged, boolean exposed) {
        this.serviceId = serviceId;
        this.ownerAddonId = ownerAddonId;
        this.serviceType = serviceType;
        this.instance = instance;
        this.scope = scope;
        this.priority = priority;
        this.lifecycleManaged = lifecycleManaged;
        this.exposed = exposed;
    }

    @Override
    public String getServiceId() { return serviceId; }

    @Override
    public String getOwnerAddonId() { return ownerAddonId; }

    @Override
    public Class<T> getServiceType() { return serviceType; }

    @Override
    public T getInstance() { return instance; }

    @Override
    public ServiceScope getScope() { return scope; }

    @Override
    public int getPriority() { return priority; }

    @Override
    public boolean isLifecycleManaged() { return lifecycleManaged; }

    @Override
    public boolean isExposed() { return exposed; }
}
