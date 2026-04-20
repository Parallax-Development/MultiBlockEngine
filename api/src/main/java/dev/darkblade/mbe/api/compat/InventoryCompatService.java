package dev.darkblade.mbe.api.compat;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface InventoryCompatService {
    Inventory topInventory(org.bukkit.event.inventory.InventoryEvent event);
    String viewTitle(org.bukkit.event.inventory.InventoryEvent event);

    Inventory topInventory(org.bukkit.entity.Player player);
    String viewTitle(org.bukkit.entity.Player player);

    // Kept for backward compatibility if any module specifically compiled against InventoryClickEvent
    default Inventory topInventory(InventoryClickEvent event) {
        return topInventory((org.bukkit.event.inventory.InventoryEvent) event);
    }
    
    default String viewTitle(InventoryClickEvent event) {
        return viewTitle((org.bukkit.event.inventory.InventoryEvent) event);
    }

    default boolean isTopInventoryClick(InventoryClickEvent event, Inventory inventory) {
        if (inventory == null || event == null) {
            return false;
        }
        return inventory.equals(topInventory((org.bukkit.event.inventory.InventoryEvent) event));
    }
}
