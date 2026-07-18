package dev.darkblade.mbe.api.item;

import net.kyori.adventure.key.Key;

import java.util.Map;

public interface ItemRequest {

    ItemKey itemKey();

    Map<Key, Object> parsedModifiers();

}
