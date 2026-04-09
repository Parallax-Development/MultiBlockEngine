package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.mode.ToolMode;
import dev.darkblade.mbe.api.tool.mode.ToolModeRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultToolModeRegistry implements ToolModeRegistry {

    private final ConcurrentHashMap<String, ToolMode> modes = new ConcurrentHashMap<>();

    @Override
    public String getServiceId() {
        return "mbe:tool.mode_registry";
    }

    @Override
    public Optional<ToolMode> get(String id) {
        String normalized = normalize(id);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(modes.get(normalized));
    }

    @Override
    public Collection<ToolMode> getAll() {
        return List.copyOf(modes.values());
    }

    @Override
    public void register(ToolMode mode) {
        Objects.requireNonNull(mode, "mode");
        String id = normalize(mode.getId());
        if (id.isBlank()) {
            throw new IllegalArgumentException("mode.id");
        }
        modes.put(id, mode);
    }

    private static String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
