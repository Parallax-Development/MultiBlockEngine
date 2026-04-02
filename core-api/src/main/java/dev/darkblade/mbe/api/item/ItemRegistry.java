package dev.darkblade.mbe.api.item;

import java.util.Collection;

public interface ItemRegistry {

    void register(ItemDefinition definition);

    ItemDefinition get(ItemKey key);

    boolean exists(ItemKey key);

    Collection<ItemDefinition> all();
}

