package dev.darkblade.mbe.api.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageUtils {

    private MessageUtils() {
    }

    public static Map<String, Object> params(Object... entries) {
        if (entries == null || entries.length == 0) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            Object k = entries[i];
            Object v = (i + 1) < entries.length ? entries[i + 1] : null;
            if (k == null) {
                continue;
            }
            String key = String.valueOf(k);
            if (key.isBlank()) {
                continue;
            }
            out.put(key, v);
        }
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }
}

