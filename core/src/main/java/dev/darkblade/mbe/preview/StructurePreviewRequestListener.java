package dev.darkblade.mbe.preview;

import dev.darkblade.mbe.api.event.StructurePreviewRequestEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class StructurePreviewRequestListener implements Listener {
    private final StructurePreviewService previewService;
    private final dev.darkblade.mbe.api.platform.PlatformService platformService;

    public StructurePreviewRequestListener(StructurePreviewService previewService, dev.darkblade.mbe.api.platform.PlatformService platformService) {
        this.previewService = previewService;
        this.platformService = platformService;
    }

    @EventHandler
    public void onPreviewRequest(StructurePreviewRequestEvent event) {
        if (event.isCancelled()) {
            return;
        }
        org.bukkit.entity.Player player = platformService.unwrap(event.getPlayer(), org.bukkit.entity.Player.class);
        previewService.startPreview(player, event.getDefinition());
    }
}
