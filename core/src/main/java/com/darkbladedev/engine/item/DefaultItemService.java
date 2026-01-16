package com.darkbladedev.engine.item;

import com.darkbladedev.engine.api.item.ItemDefinition;
import com.darkbladedev.engine.api.item.ItemFactory;
import com.darkbladedev.engine.api.item.ItemInstance;
import com.darkbladedev.engine.api.item.ItemKey;
import com.darkbladedev.engine.api.item.ItemRegistry;
import com.darkbladedev.engine.api.item.ItemService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultItemService implements ItemService {

    private final DefaultItemRegistry registry = new DefaultItemRegistry();
    private final DefaultItemFactory factory = new DefaultItemFactory(registry);

    @Override
    public ItemRegistry registry() {
        return registry;
    }

    @Override
    public ItemFactory factory() {
        return factory;
    }

    private static final class DefaultItemRegistry implements ItemRegistry {

        private final Map<ItemKey, ItemDefinition> definitions = new ConcurrentHashMap<>();

        @Override
        public void register(ItemDefinition definition) {
            Objects.requireNonNull(definition, "definition");
            ItemKey key = Objects.requireNonNull(definition.key(), "definition.key()");

            definitions.put(key, definition);
        }

        @Override
        public ItemDefinition get(ItemKey key) {
            Objects.requireNonNull(key, "key");
            ItemDefinition def = definitions.get(key);
            if (def == null) {
                throw new IllegalArgumentException("Unknown ItemKey: " + key);
            }
            return def;
        }

        @Override
        public boolean exists(ItemKey key) {
            return key != null && definitions.containsKey(key);
        }

        @Override
        public Collection<ItemDefinition> all() {
            return new ArrayList<>(definitions.values());
        }
    }

    private static final class DefaultItemFactory implements ItemFactory {

        private final ItemRegistry registry;

        private DefaultItemFactory(ItemRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry");
        }

        @Override
        public ItemInstance create(ItemKey key) {
            return create(key, Map.of());
        }

        @Override
        public ItemInstance create(ItemKey key, Map<String, Object> data) {
            Objects.requireNonNull(key, "key");
            ItemDefinition def = registry.get(key);
            Map<String, Object> map = data == null || data.isEmpty() ? new HashMap<>() : new HashMap<>(data);
            return new DefaultItemInstance(def, map);
        }
    }

    private static final class DefaultItemInstance implements ItemInstance {

        private final ItemDefinition definition;
        private final Map<String, Object> data;

        private DefaultItemInstance(ItemDefinition definition, Map<String, Object> data) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.data = Objects.requireNonNull(data, "data");
        }

        @Override
        public ItemDefinition definition() {
            return definition;
        }

        @Override
        public Map<String, Object> data() {
            return data;
        }
    }
}
