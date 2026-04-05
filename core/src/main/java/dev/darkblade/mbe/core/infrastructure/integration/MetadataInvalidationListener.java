package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MetadataValueChangeEvent;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.core.application.service.metadata.MetadataServiceImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class MetadataInvalidationListener implements Listener {
    private final MetadataServiceImpl metadataService;

    public MetadataInvalidationListener(MetadataServiceImpl metadataService) {
        this.metadataService = metadataService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(MultiblockBreakEvent event) {
        metadataService.invalidateInstance(event.getMultiblock());
    }

    @EventHandler
    public void onMetadataValueChanged(MetadataValueChangeEvent event) {
        metadataService.invalidateInstanceKey(event.getMultiblock(), event.getMetadataId());
    }
}
