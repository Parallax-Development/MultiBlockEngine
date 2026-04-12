package dev.darkblade.mbe.api.ui.runtime;

import java.util.Locale;
import java.util.Objects;

public record PanelId(String value) {
    public PanelId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Panel id cannot be blank");
        }
    }

    public static PanelId of(String id) {
        return new PanelId(id);
    }
}
