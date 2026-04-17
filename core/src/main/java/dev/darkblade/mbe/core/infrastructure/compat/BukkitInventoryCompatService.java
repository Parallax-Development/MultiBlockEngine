package dev.darkblade.mbe.core.infrastructure.compat;

import dev.darkblade.mbe.api.compat.InventoryCompatService;
import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class BukkitInventoryCompatService implements InventoryCompatService, MBEService {
    private static final String SERVICE_ID = "mbe:compat.inventory";

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public Inventory topInventory(InventoryClickEvent event) {
        if (event == null) {
            return null;
        }
        return event.getInventory();
    }
}
