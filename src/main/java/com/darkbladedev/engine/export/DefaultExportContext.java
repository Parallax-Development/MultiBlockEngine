package com.darkbladedev.engine.export;

import com.darkbladedev.engine.api.export.ExportBlockPos;
import com.darkbladedev.engine.api.export.ExportContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultExportContext implements ExportContext {

    private final Map<ExportBlockPos, String> roles = new LinkedHashMap<>();
    private final Map<ExportBlockPos, Map<String, Object>> props = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    @Override
    public void markRole(ExportBlockPos pos, String role) {
        if (pos == null || role == null || role.isBlank()) {
            return;
        }
        String r = role.trim();
        if (r.equalsIgnoreCase("controller")) {
            roles.entrySet().removeIf(e -> e.getValue() != null && e.getValue().equalsIgnoreCase("controller"));
        }
        roles.put(pos, r);
    }

    @Override
    public void putProperty(ExportBlockPos pos, String key, Object value) {
        if (pos == null || key == null || key.isBlank()) {
            return;
        }
        props.compute(pos, (p, prev) -> {
            Map<String, Object> next = prev == null ? new LinkedHashMap<>() : new LinkedHashMap<>(prev);
            next.put(key.trim(), value);
            return Map.copyOf(next);
        });
    }

    @Override
    public Map<ExportBlockPos, String> roles() {
        return Map.copyOf(roles);
    }

    @Override
    public Map<ExportBlockPos, Map<String, Object>> properties() {
        return Map.copyOf(props);
    }

    @Override
    public void warn(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        warnings.add(message.trim());
    }

    @Override
    public List<String> warnings() {
        return List.copyOf(warnings);
    }
}

