package dev.darkblade.mbe.api.item;

import dev.darkblade.mbe.api.persistence.item.NamespacedKey;
import org.jetbrains.annotations.Nullable;

public interface ItemKey extends dev.darkblade.mbe.api.persistence.item.ItemKey {

    NamespacedKey id();

    int version();

    @Override
    default NamespacedKey type() {
        return id();
    }

    @Override
    default int damage() {
        return version();
    }

    @Override
    default @Nullable String nbtHash() {
        return null;
    }

}
