package dev.darkblade.mbe.blueprint;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

public final class PreviewPlacementController implements Listener {
    private final BlueprintController controller;

    public PreviewPlacementController(BlueprintController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!controller.refreshState(player)) {
            return;
        }
        controller.updatePreviewOnMove(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemSwitch(org.bukkit.event.player.PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            int previous = event.getPreviousSlot();
            int current = event.getNewSlot();
            // Calculate scroll direction.
            // If going from 0 to 8, they scrolled left (decrement)
            // If going from 8 to 0, they scrolled right (increment)
            int delta = current - previous;
            if (previous == 0 && current == 8) delta = -1;
            if (previous == 8 && current == 0) delta = 1;
            
            if (controller.handleLayerChange(player, delta)) {
                event.setCancelled(true);
            }
        }
    }
}
