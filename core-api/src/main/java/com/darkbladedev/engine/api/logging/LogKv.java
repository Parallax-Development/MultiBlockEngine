package com.darkbladedev.engine.api.logging;

import java.util.Objects;

public record LogKv(String key, Object value) {
    public LogKv {
        Objects.requireNonNull(key, "key");
    }

    public static LogKv kv(String key, Object value) {
        return new LogKv(key, value);
    }
}

