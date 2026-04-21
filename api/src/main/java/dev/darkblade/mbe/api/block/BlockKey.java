package dev.darkblade.mbe.api.block;

import dev.darkblade.mbe.api.persistence.item.NamespacedKey;

import java.util.Objects;

public record BlockKey(NamespacedKey id) {
    public BlockKey {
        Objects.requireNonNull(id, "id");
    }

    public static BlockKey of(String namespacedId) {
        Objects.requireNonNull(namespacedId, "namespacedId");
        return new BlockKey(NamespacedKey.parse(namespacedId));
    }

    public static BlockKey of(NamespacedKey id) {
        return new BlockKey(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
