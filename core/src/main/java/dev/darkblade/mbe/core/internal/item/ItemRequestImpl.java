package dev.darkblade.mbe.core.internal.item;

import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemRequest;
import net.kyori.adventure.key.Key;

import java.util.Map;
import java.util.Objects;

public class ItemRequestImpl implements ItemRequest {

    private final ItemKey itemKey;
    private final Map<Key, Object> parsedModifiers;

    public ItemRequestImpl(ItemKey itemKey, Map<Key, Object> parsedModifiers) {
        this.itemKey = Objects.requireNonNull(itemKey, "itemKey");
        this.parsedModifiers = Map.copyOf(Objects.requireNonNull(parsedModifiers, "parsedModifiers"));
    }

    @Override
    public ItemKey itemKey() {
        return itemKey;
    }

    @Override
    public Map<Key, Object> parsedModifiers() {
        return parsedModifiers;
    }
}
