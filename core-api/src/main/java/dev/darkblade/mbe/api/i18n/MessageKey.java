package dev.darkblade.mbe.api.i18n;

import java.util.Locale;
import java.util.Objects;

public interface MessageKey {

    String origin();

    String path();

    default String fullKey() {
        return origin() + ":" + path();
    }

    static MessageKey of(String origin, String path) {
        return new DefaultMessageKey(origin, path);
    }

    static String normalizeOrigin(String origin) {
        String v = origin == null ? "" : origin.trim();
        if (v.isEmpty()) {
            return "unknown";
        }
        return v.toLowerCase(Locale.ROOT);
    }

    static String normalizePath(String path) {
        String v = path == null ? "" : path.trim();
        if (v.isEmpty()) {
            return "unknown";
        }
        return v;
    }

    record DefaultMessageKey(String origin, String path) implements MessageKey {
        public DefaultMessageKey {
            origin = MessageKey.normalizeOrigin(origin);
            path = MessageKey.normalizePath(path);
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(path, "path");
        }
    }
}

