package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ToolMode;
import dev.darkblade.mbe.api.tool.ToolModeRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultToolModeRegistry implements ToolModeRegistry {

    private final ConcurrentHashMap<String, ToolMode> modes = new ConcurrentHashMap<>();

    @Override
    public ToolMode get(String id) {
        String normalized = normalize(id);
        if (normalized.isBlank()) {
            return null;
        }
        return modes.get(normalized);
    }

    @Override
    public Collection<ToolMode> all() {
        return List.copyOf(modes.values());
    }

    public void register(ToolMode mode) {
        Objects.requireNonNull(mode, "mode");
        String id = normalize(mode.id());
        if (id.isBlank()) {
            throw new IllegalArgumentException("mode.id");
        }
        ToolMode previous = modes.putIfAbsent(id, mode);
        if (previous != null) {
            throw new IllegalStateException("Tool mode already registered: " + id);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
