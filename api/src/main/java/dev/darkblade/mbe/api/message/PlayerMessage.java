package dev.darkblade.mbe.api.message;

import dev.darkblade.mbe.api.i18n.MessageKey;

import java.util.Map;

public record PlayerMessage(
        MessageKey key,
        MessageChannel channel,
        MessagePriority priority,
        Map<String, Object> placeholders
) {
    public PlayerMessage {
        placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }
}
