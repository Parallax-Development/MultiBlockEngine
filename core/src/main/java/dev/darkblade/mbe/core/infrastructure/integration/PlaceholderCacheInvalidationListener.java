package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryServiceImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class PlaceholderCacheInvalidationListener implements Listener {
    private final PlayerMultiblockQueryServiceImpl queryService;

    public PlaceholderCacheInvalidationListener(PlayerMultiblockQueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMultiblockForm(MultiblockFormEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            queryService.invalidateAll();
            return;
        }
        UUID playerId = player.getUniqueId();
        queryService.trackOwnership(playerId, event.getMultiblock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMultiblockBreak(MultiblockBreakEvent event) {
        queryService.removeOwnership(event.getMultiblock());
    }
}
