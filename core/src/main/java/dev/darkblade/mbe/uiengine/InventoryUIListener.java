package dev.darkblade.mbe.uiengine;

import dev.darkblade.mbe.api.compat.InventoryCompatService;
import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

public final class InventoryUIListener implements Listener {
    private final InventorySessionStore sessions;
    private final BlueprintService blueprintService;
    private final InventoryCompatService compat;

    public InventoryUIListener(InventorySessionStore sessions, BlueprintService blueprintService, InventoryCompatService compat) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.blueprintService = Objects.requireNonNull(blueprintService, "blueprintService");
        this.compat = Objects.requireNonNull(compat, "compat");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        PlayerInventorySession session = sessions.get(player).orElse(null);
        if (session == null) {
            return;
        }
        Inventory topInventory = compat.topInventory(event);
        if (session.inventory() != topInventory) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= topInventory.getSize()) {
            return;
        }
        event.setCancelled(true);
        Object data = session.getBinding(rawSlot);
        if (data instanceof MultiblockDefinition definition) {
            blueprintService.startPlacement(player, definition);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        PlayerInventorySession session = sessions.get(player).orElse(null);
        if (session == null) {
            return;
        }
        if (session.inventory() == event.getInventory()) {
            sessions.clear(player);
        }
    }
}
