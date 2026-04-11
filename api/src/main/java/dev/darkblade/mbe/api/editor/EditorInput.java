package dev.darkblade.mbe.api.editor;

import java.util.Objects;
import java.util.Optional;

public final class EditorInput {
    public enum Type {
        BLOCK_CLICK,
        ENTITY_CLICK,
        CHAT_INPUT
    }

    private final Type type;
    private final Object payload;

    public EditorInput(Type type, Object payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = payload;
    }

    public Type type() {
        return type;
    }

    public Object payload() {
        return payload;
    }

    public <T> Optional<T> payloadAs(Class<T> payloadType) {
        if (payloadType == null || payload == null || !payloadType.isInstance(payload)) {
            return Optional.empty();
        }
        return Optional.of(payloadType.cast(payload));
    }
}
