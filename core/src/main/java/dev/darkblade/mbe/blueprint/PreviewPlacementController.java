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
}
