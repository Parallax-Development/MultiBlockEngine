package dev.darkblade.mbe.preview;

import dev.darkblade.mbe.api.event.StructurePreviewRequestEvent;
import dev.darkblade.mbe.api.event.EventBusService;

public final class StructurePreviewRequestListener {
    private final StructurePreviewService previewService;
    private final dev.darkblade.mbe.api.platform.PlatformService platformService;

    public StructurePreviewRequestListener(EventBusService eventBus, StructurePreviewService previewService, dev.darkblade.mbe.api.platform.PlatformService platformService) {
        this.previewService = previewService;
        this.platformService = platformService;
        eventBus.subscribe(StructurePreviewRequestEvent.class, this::onPreviewRequest);
    }

    public void onPreviewRequest(StructurePreviewRequestEvent event) {
        if (event.isCancelled()) {
            return;
        }
        org.bukkit.entity.Player player = platformService.unwrap(event.getPlayer(), org.bukkit.entity.Player.class);
        previewService.startPreview(player, event.getDefinition());
    }
}
