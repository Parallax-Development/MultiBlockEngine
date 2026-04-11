package dev.darkblade.mbe.api.item;

import java.util.Map;

public interface ItemInstance {

    ItemDefinition definition();

    Map<String, Object> data();
}

