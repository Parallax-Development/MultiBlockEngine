package com.darkbladedev.engine.assembly;

import com.darkbladedev.engine.api.assembly.AssemblyTrigger;
import com.darkbladedev.engine.api.assembly.AssemblyTriggerRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultAssemblyTriggerRegistry implements AssemblyTriggerRegistry {

    private final Map<String, AssemblyTrigger> triggers = new ConcurrentHashMap<>();

    @Override
    public void register(AssemblyTrigger trigger) {
        Objects.requireNonNull(trigger, "trigger");
        String id = normalizeId(trigger.id());
        requireNamespacedKey(id);
        triggers.put(id, trigger);
    }

    @Override
    public Optional<AssemblyTrigger> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(triggers.get(normalizeId(id)));
    }

    @Override
    public Collection<AssemblyTrigger> all() {
        return snapshotById().values();
    }

    public Map<String, AssemblyTrigger> snapshotById() {
        return Map.copyOf(new LinkedHashMap<>(triggers));
    }

    static void requireNamespacedKey(String key) {
        Objects.requireNonNull(key, "key");
        String v = key.trim();
        if (v.isEmpty() || v.indexOf(':') <= 0 || v.endsWith(":")) {
            throw new IllegalArgumentException("Invalid key (expected <namespace:key>): " + key);
        }
    }

    static String normalizeId(String id) {
        return (id == null ? "" : id.trim()).toLowerCase(Locale.ROOT);
    }
}

