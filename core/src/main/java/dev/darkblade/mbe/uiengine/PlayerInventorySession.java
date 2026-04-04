package dev.darkblade.mbe.uiengine;

import org.bukkit.inventory.Inventory;

import java.util.Map;

public record PlayerInventorySession(
        String viewId,
        Inventory inventory,
        Map<Integer, Object> bindings
) {
    public Object getBinding(int slot) {
        if (bindings == null) {
            return null;
        }
        return bindings.get(slot);
    }
}
