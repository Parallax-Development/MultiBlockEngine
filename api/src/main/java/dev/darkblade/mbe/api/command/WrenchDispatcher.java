package dev.darkblade.mbe.api.command;

import java.util.Objects;

public interface WrenchDispatcher {

    void registerAction(String key, WrenchInteractable interactable);

    WrenchResult dispatch(WrenchContext context);

    static String normalizeKey(String key) {
        String v = key == null ? "" : key.trim();
        return v.toLowerCase(java.util.Locale.ROOT);
    }

    static void requireNamespacedKey(String key) {
        Objects.requireNonNull(key, "key");
        String v = key.trim();
        if (v.isEmpty() || v.indexOf(':') <= 0 || v.endsWith(":")) {
            throw new IllegalArgumentException("Invalid key (expected <namespace:key>): " + key);
        }
    }
}

