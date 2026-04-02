package dev.darkblade.mbe.api.persistence.item;

import java.util.Locale;
import java.util.Objects;

public record NamespacedKey(String namespace, String key) implements Comparable<NamespacedKey> {

    public NamespacedKey {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(key, "key");
        String ns = namespace.trim();
        String k = key.trim();
        if (ns.isEmpty() || k.isEmpty()) {
            throw new IllegalArgumentException("NamespacedKey parts cannot be blank");
        }
        namespace = ns.toLowerCase(Locale.ROOT);
        key = k.toLowerCase(Locale.ROOT);
    }

    public static NamespacedKey of(String namespace, String key) {
        return new NamespacedKey(namespace, key);
    }

    public static NamespacedKey parse(String value) {
        Objects.requireNonNull(value, "value");
        int idx = value.indexOf(':');
        if (idx <= 0 || idx == value.length() - 1) {
            throw new IllegalArgumentException("Invalid namespaced key: " + value);
        }
        return new NamespacedKey(value.substring(0, idx), value.substring(idx + 1));
    }

    @Override
    public int compareTo(NamespacedKey o) {
        if (o == null) {
            return 1;
        }
        int ns = namespace.compareTo(o.namespace);
        if (ns != 0) {
            return ns;
        }
        return key.compareTo(o.key);
    }

    @Override
    public String toString() {
        return namespace + ':' + key;
    }
}
