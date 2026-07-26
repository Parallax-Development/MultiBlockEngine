package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class BlueprintItem {
    public static final ItemKey BLUEPRINT_KEY = ItemKeys.of("mbe:blueprint", 0);
    public static final String DATA_STRUCTURE_ID = "mbe:multiblock";

    private BlueprintItem() {
    }

    public static ItemStack create(ItemService itemService, ItemStackBridge bridge, MultiblockDefinition definition) {
        return create(itemService, bridge, definition, null);
    }

    public static ItemStack create(ItemService itemService, ItemStackBridge bridge, MultiblockDefinition definition, CommandSender sender) {
        if (itemService == null || bridge == null || definition == null || definition.id() == null || definition.id().isBlank()) {
            return null;
        }
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put(DATA_STRUCTURE_ID, definition.id());
        data.put("multiblock_display_name", definition.id());
        data.put("count", 0);
        data.put("total", definition.blocks() != null ? definition.blocks().size() : 0);
        
        ItemInstance instance = itemService.factory().create(BLUEPRINT_KEY, data);
        ItemStack stack = bridge.toItemStack(instance, sender);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(dev.darkblade.mbe.core.internal.tooling.StringUtil.toLegacy(Component.text("Blueprint: " + definition.id(), NamedTextColor.AQUA)));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
