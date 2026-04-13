package dev.darkblade.mbe.core.application.command.service;

import dev.darkblade.mbe.api.command.MbeCommandService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DynamicCommandServiceRegistry {
    private final Supplier<List<MbeCommandService>> serviceSupplier;
    private final Map<String, MbeCommandService> snapshotById = new ConcurrentHashMap<>();

    public DynamicCommandServiceRegistry(Supplier<List<MbeCommandService>> serviceSupplier) {
        this.serviceSupplier = Objects.requireNonNull(serviceSupplier, "serviceSupplier");
    }

    public synchronized void refresh() {
        snapshotById.clear();
        List<MbeCommandService> services = serviceSupplier.get();
        if (services == null || services.isEmpty()) {
            return;
        }
        for (MbeCommandService service : services) {
            if (service == null || service.id() == null || service.id().isBlank()) {
                continue;
            }
            String id = normalize(service.id());
            snapshotById.putIfAbsent(id, service);
        }
    }

    public Optional<MbeCommandService> resolve(String id) {
        String key = normalize(id);
        if (key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshotById.get(key));
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(snapshotById.keySet());
        ids.sort(String::compareToIgnoreCase);
        return List.copyOf(ids);
    }

    public List<MbeCommandService> services() {
        return List.copyOf(snapshotById.values());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
