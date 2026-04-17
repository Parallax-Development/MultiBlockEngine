package dev.darkblade.mbe.api.compat;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface InventoryCompatService {
    Inventory topInventory(InventoryClickEvent event);

    default boolean isTopInventoryClick(InventoryClickEvent event, Inventory inventory) {
        if (inventory == null || event == null) {
            return false;
        }
        return inventory.equals(topInventory(event));
    }
}
