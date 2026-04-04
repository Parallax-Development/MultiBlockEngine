package dev.darkblade.mbe.api.ui.binding;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record PanelBinding(
        UUID id,
        String panelId,
        String world,
        int x,
        int y,
        int z,
        String triggerType
) {
    public PanelBinding {
        Objects.requireNonNull(id, "id");
        panelId = requireText(panelId, "panelId");
        world = requireText(world, "world");
        triggerType = requireText(triggerType, "triggerType").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String out = value.trim();
        if (out.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return out;
    }
}
