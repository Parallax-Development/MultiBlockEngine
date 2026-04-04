package dev.darkblade.mbe.api.editor;

import java.util.Locale;
import java.util.Objects;

public record EditorSessionType(String id) {
    public static final EditorSessionType PANEL_LINK = new EditorSessionType("panel_link");

    public EditorSessionType {
        Objects.requireNonNull(id, "id");
        id = id.trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id");
        }
    }
}
