package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.tool.ToolActionRegistry;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultToolActionRegistry implements ToolActionRegistry {

    private final ConcurrentHashMap<String, ToolAction> actions = new ConcurrentHashMap<>();

    @Override
    public ToolAction get(ActionId id) {
        String normalized = normalize(id);
        if (normalized.isBlank()) {
            return null;
        }
        return actions.get(normalized);
    }

    public void register(ToolAction action) {
        Objects.requireNonNull(action, "action");
        String id = normalize(action.id());
        if (id.isBlank()) {
            throw new IllegalArgumentException("action.id");
        }
        ToolAction previous = actions.putIfAbsent(id, action);
        if (previous != null) {
            throw new IllegalStateException("Tool action already registered: " + id);
        }
    }

    private String normalize(ActionId id) {
        if (id == null) {
            return "";
        }
        return normalize(id.toString());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
