package dev.darkblade.mbe.api.persistence;

import dev.darkblade.mbe.api.persistence.item.ItemKey;

import java.util.Map;

public interface StorageService {

    long getAmount(ItemKey key);

    boolean contains(ItemKey key);

    Map<ItemKey, Long> getAll();

    long getTotalItems();

    long getDistinctItemCount();

    StorageResult insert(ItemKey key, long amount);

    StorageResult extract(ItemKey key, long amount);

    boolean canInsert(ItemKey key, long amount);

    boolean canExtract(ItemKey key, long amount);

    void clear();

    StorageSnapshot snapshot();

    void addListener(StorageListener listener);

    void removeListener(StorageListener listener);
}
