package dev.darkblade.mbe.api.logging;

import dev.darkblade.mbe.api.i18n.MessageKey;

public interface ErrorCode {

    MessageKey messageKey();

    default String id() {
        MessageKey key = messageKey();
        return key == null ? "unknown" : key.fullKey();
    }
}

