package dev.darkblade.mbe.api.ui.runtime;

import java.util.Map;

public record PanelDefinition(
    String id,
    PanelLayout layout,
    Map<String, PanelAction> actions
) {
    public PanelDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }
        layout = layout == null ? new PanelLayout(Map.of()) : layout;
        actions = actions == null ? Map.of() : Map.copyOf(actions);
    }
}
