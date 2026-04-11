package dev.darkblade.mbe.core.infrastructure.bridge.item;

import dev.darkblade.mbe.api.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

public interface ItemStackBridge {

    ItemStack toItemStack(ItemInstance instance);

    ItemInstance fromItemStack(ItemStack stack);
}
