package com.darkbladedev.engine.item.bridge;

import com.darkbladedev.engine.api.item.ItemDefinition;
import com.darkbladedev.engine.api.item.ItemInstance;
import com.darkbladedev.engine.api.item.ItemKey;
import com.darkbladedev.engine.api.item.ItemKeys;
import com.darkbladedev.engine.api.item.ItemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PdcItemStackBridge implements ItemStackBridge {

    private static final NamespacedKey KEY_ID = NamespacedKey.fromString("multiblockengine:mbe_item_id");
    private static final NamespacedKey KEY_VERSION = NamespacedKey.fromString("multiblockengine:mbe_item_version");
    private static final NamespacedKey KEY_UID = NamespacedKey.fromString("multiblockengine:mbe_item_uid");

    private final ItemService items;

    public PdcItemStackBridge(ItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override
    public ItemStack toItemStack(ItemInstance instance) {
        Objects.requireNonNull(instance, "instance");
        ItemDefinition def = Objects.requireNonNull(instance.definition(), "instance.definition()");
        ItemKey key = Objects.requireNonNull(def.key(), "definition.key()");

        Material material = resolveMaterial(def);
        ItemStack stack = new ItemStack(material);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = def.displayName();
            if (name != null && !name.isBlank()) {
                meta.displayName(Component.text(name, NamedTextColor.WHITE));
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (KEY_ID != null) {
                pdc.set(KEY_ID, PersistentDataType.STRING, key.id().toString());
            }
            if (KEY_VERSION != null) {
                pdc.set(KEY_VERSION, PersistentDataType.INTEGER, Math.max(0, key.version()));
            }

            Map<String, Object> props = def.properties();
            if (KEY_UID != null && props != null && Boolean.TRUE.equals(props.get("unstackable"))) {
                pdc.set(KEY_UID, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            stack.setItemMeta(meta);
        }

        return stack;
    }

    @Override
    public ItemInstance fromItemStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc == null) {
            return null;
        }

        String id = KEY_ID == null ? null : pdc.get(KEY_ID, PersistentDataType.STRING);
        Integer version = KEY_VERSION == null ? null : pdc.get(KEY_VERSION, PersistentDataType.INTEGER);
        if (id == null || id.isBlank()) {
            return null;
        }

        ItemKey key;
        try {
            key = ItemKeys.of(id.trim(), Math.max(0, version == null ? 0 : version));
        } catch (RuntimeException ex) {
            return null;
        }

        if (!items.registry().exists(key)) {
            return null;
        }

        return items.factory().create(key);
    }

    private static Material resolveMaterial(ItemDefinition def) {
        Map<String, Object> props = def == null ? null : def.properties();
        if (props != null) {
            Object v = props.get("material");
            if (v instanceof String s && !s.isBlank()) {
                Material m = Material.matchMaterial(s.trim().toUpperCase(Locale.ROOT));
                if (m != null) {
                    return m;
                }
            }
        }
        return Material.PAPER;
    }
}

