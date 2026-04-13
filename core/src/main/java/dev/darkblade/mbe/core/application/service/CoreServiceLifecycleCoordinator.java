package dev.darkblade.mbe.core.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CoreServiceLifecycleCoordinator {
    private final Map<String, CoreServiceLifecycle> lifecycleByServiceId = new LinkedHashMap<>();

    public synchronized void register(ManagedCoreService service) {
        Objects.requireNonNull(service, "service");
        register(service.getManagedCoreServiceId(), service::onCoreLoad, service::onCoreEnable, service::onCoreDisable);
    }

    public synchronized void register(String serviceId, Runnable onLoad, Runnable onEnable, Runnable onDisable) {
        String normalized = normalizeServiceId(serviceId);
        lifecycleByServiceId.put(
                normalized,
                new CoreServiceLifecycle(
                        normalized,
                        onLoad == null ? () -> { } : onLoad,
                        onEnable == null ? () -> { } : onEnable,
                        onDisable == null ? () -> { } : onDisable
                )
        );
    }

    public synchronized void loadAll() {
        for (CoreServiceLifecycle lifecycle : lifecycleByServiceId.values()) {
            lifecycle.onLoad().run();
        }
    }

    public synchronized void enableAll() {
        for (CoreServiceLifecycle lifecycle : lifecycleByServiceId.values()) {
            lifecycle.onEnable().run();
        }
    }

    public synchronized void disableAll() {
        RuntimeException failure = null;
        List<CoreServiceLifecycle> services = new ArrayList<>(lifecycleByServiceId.values());
        for (int i = services.size() - 1; i >= 0; i--) {
            CoreServiceLifecycle lifecycle = services.get(i);
            try {
                lifecycle.onDisable().run();
            } catch (RuntimeException ex) {
                if (failure == null) {
                    failure = new RuntimeException("Failed to disable core managed services");
                }
                failure.addSuppressed(ex);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static String normalizeServiceId(String serviceId) {
        String normalized = Objects.requireNonNull(serviceId, "serviceId").trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("serviceId");
        }
        return normalized;
    }

    private record CoreServiceLifecycle(String serviceId, Runnable onLoad, Runnable onEnable, Runnable onDisable) {
    }
}
