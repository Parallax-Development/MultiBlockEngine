package com.darkbladedev.engine.item.bridge;

import com.darkbladedev.engine.api.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

public interface ItemStackBridge {

    ItemStack toItemStack(ItemInstance instance);

    ItemInstance fromItemStack(ItemStack stack);
}
