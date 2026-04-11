package dev.darkblade.mbe.api.item;

import java.util.Map;

public interface ItemFactory {

    ItemInstance create(ItemKey key);

    ItemInstance create(ItemKey key, Map<String, Object> data);
}

