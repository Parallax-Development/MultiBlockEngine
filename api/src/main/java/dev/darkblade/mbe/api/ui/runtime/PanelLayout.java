package dev.darkblade.mbe.api.ui.runtime;

import java.util.Map;

public record PanelLayout(Map<String, Object> properties) {
    public PanelLayout {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }
}
