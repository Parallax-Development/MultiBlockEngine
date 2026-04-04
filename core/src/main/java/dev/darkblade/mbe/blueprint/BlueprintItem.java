package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class BlueprintItem {
    public static final ItemKey BLUEPRINT_KEY = ItemKeys.of("mbe:blueprint", 0);
    public static final String DATA_STRUCTURE_ID = "structureId";

    private BlueprintItem() {
    }

    public static ItemStack create(ItemService itemService, ItemStackBridge bridge, MultiblockDefinition definition) {
        if (itemService == null || bridge == null || definition == null || definition.id() == null || definition.id().isBlank()) {
            return null;
        }
        ItemInstance instance = itemService.factory().create(BLUEPRINT_KEY, Map.of(DATA_STRUCTURE_ID, definition.id()));
        ItemStack stack = bridge.toItemStack(instance);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Blueprint: " + definition.id(), NamedTextColor.AQUA));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
