package dev.darkblade.mbe.api.command;

import dev.darkblade.mbe.api.i18n.MessageKey;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record WrenchResult(
        boolean handled,
        boolean cancelEvent,
        @Nullable MessageKey message,
        Map<String, ?> params
) {

    public static WrenchResult notHandled() {
        return new WrenchResult(false, false, null, Map.of());
    }

    public static WrenchResult handled(boolean cancelEvent) {
        return new WrenchResult(true, cancelEvent, null, Map.of());
    }

    public static WrenchResult handled(MessageKey message, Map<String, ?> params, boolean cancelEvent) {
        return new WrenchResult(true, cancelEvent, message, params == null ? Map.of() : params);
    }
}

