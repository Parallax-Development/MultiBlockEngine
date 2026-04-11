package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultToolRegistry implements ToolRegistry {

    private final ConcurrentHashMap<String, Tool> tools = new ConcurrentHashMap<>();

    @Override
    public Tool get(String id) {
        String normalized = normalize(id);
        if (normalized.isBlank()) {
            return null;
        }
        return tools.get(normalized);
    }

    @Override
    public Collection<Tool> all() {
        return List.copyOf(tools.values());
    }

    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        String normalized = normalize(tool.id());
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("tool.id");
        }
        Tool previous = tools.putIfAbsent(normalized, tool);
        if (previous != null) {
            throw new IllegalStateException("Tool already registered: " + normalized);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
