package dev.darkblade.mbe.api.persistence;

import dev.darkblade.mbe.api.persistence.item.ItemKey;

import java.util.Map;

public interface StorageSnapshot {

    long timestamp();

    Map<ItemKey, Long> entries();
}
