package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.core.application.service.query.PlayerMultiblockQueryServiceImpl;
import dev.darkblade.mbe.api.event.EventBusService;
import java.util.UUID;

public final class PlaceholderCacheInvalidationListener {
    private final PlayerMultiblockQueryServiceImpl queryService;

    public PlaceholderCacheInvalidationListener(EventBusService eventBus, PlayerMultiblockQueryServiceImpl queryService) {
        this.queryService = queryService;
        eventBus.subscribe(MultiblockFormEvent.class, this::onMultiblockForm);
        eventBus.subscribe(MultiblockBreakEvent.class, this::onMultiblockBreak);
    }

    public void onMultiblockForm(MultiblockFormEvent event) {
        dev.darkblade.mbe.api.platform.MBEPlayer player = event.getPlayer();
        if (player == null) {
            queryService.invalidateAll();
            return;
        }
        UUID playerId = player.getUniqueId();
        queryService.trackOwnership(playerId, event.getMultiblock());
    }

    public void onMultiblockBreak(MultiblockBreakEvent event) {
        queryService.removeOwnership(event.getMultiblock());
    }
}
