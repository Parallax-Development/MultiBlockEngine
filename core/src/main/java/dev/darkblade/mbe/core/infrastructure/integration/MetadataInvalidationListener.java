package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.MetadataValueChangeEvent;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.core.application.service.metadata.MetadataServiceImpl;
import dev.darkblade.mbe.api.event.EventBusService;

public final class MetadataInvalidationListener {
    private final MetadataServiceImpl metadataService;

    public MetadataInvalidationListener(EventBusService eventBus, MetadataServiceImpl metadataService) {
        this.metadataService = metadataService;
        eventBus.subscribe(MultiblockBreakEvent.class, this::onBreak);
        eventBus.subscribe(MetadataValueChangeEvent.class, this::onMetadataValueChanged);
    }

    public void onBreak(MultiblockBreakEvent event) {
        metadataService.invalidateInstance(event.getMultiblock());
    }

    public void onMetadataValueChanged(MetadataValueChangeEvent event) {
        metadataService.invalidateInstanceKey(event.getMultiblock(), event.getMetadataId());
    }
}
