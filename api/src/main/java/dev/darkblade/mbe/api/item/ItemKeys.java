package dev.darkblade.mbe.api.item;

import dev.darkblade.mbe.api.persistence.item.NamespacedKey;

import java.util.Objects;

public final class ItemKeys {

    private ItemKeys() {
    }

    public static ItemKey of(NamespacedKey id, int version) {
        Objects.requireNonNull(id, "id");
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
        return new ValueItemKey(id, version);
    }

    public static ItemKey of(String namespacedId, int version) {
        Objects.requireNonNull(namespacedId, "namespacedId");
        return of(NamespacedKey.parse(namespacedId), version);
    }

    private record ValueItemKey(NamespacedKey id, int version) implements ItemKey {
        private ValueItemKey {
            Objects.requireNonNull(id, "id");
            if (version < 0) {
                throw new IllegalArgumentException("version must be >= 0");
            }
        }
    }
}

