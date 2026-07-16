package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryServiceImpl;
import dev.darkblade.mbe.api.event.EventBusService;
import org.bukkit.entity.Player;

public final class QueryCacheInvalidationListener {
    private final PlayerMultiblockQueryServiceImpl queryService;

    public QueryCacheInvalidationListener(EventBusService eventBus, PlayerMultiblockQueryServiceImpl queryService) {
        this.queryService = queryService;
        eventBus.subscribe(MultiblockFormEvent.class, this::onForm);
        eventBus.subscribe(MultiblockBreakEvent.class, this::onBreak);
    }

    public void onForm(MultiblockFormEvent event) {
        // If a player forms a multiblock, we might need to invalidate their caches
        // or track ownership.
        if (event.getPlayer() != null) {
            // Using Bukkit Player UUID as the cache is mapped by UUID
            Player player = org.bukkit.Bukkit.getPlayer(event.getPlayer().getUniqueId());
            if (player != null) {
                queryService.trackOwnership(player.getUniqueId(), event.getMultiblock());
                queryService.invalidatePlayer(player.getUniqueId());
            }
        } else {
            queryService.invalidateAll();
        }
    }

    public void onBreak(MultiblockBreakEvent event) {
        // When a multiblock is destroyed, we remove it from the query cache tracking
        queryService.removeOwnership(event.getMultiblock());
    }
}
