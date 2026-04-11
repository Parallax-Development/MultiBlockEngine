package dev.darkblade.mbe.api.persistence;

import dev.darkblade.mbe.api.persistence.item.ItemKey;

public interface StorageListener {

    void onInsert(ItemKey key, long amount);

    void onExtract(ItemKey key, long amount);

    void onClear();
}
