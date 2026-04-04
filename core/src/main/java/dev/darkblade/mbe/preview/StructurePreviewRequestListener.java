package dev.darkblade.mbe.preview;

import dev.darkblade.mbe.api.event.StructurePreviewRequestEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class StructurePreviewRequestListener implements Listener {
    private final StructurePreviewService previewService;

    public StructurePreviewRequestListener(StructurePreviewService previewService) {
        this.previewService = previewService;
    }

    @EventHandler
    public void onPreviewRequest(StructurePreviewRequestEvent event) {
        if (event.isCancelled()) {
            return;
        }
        previewService.startPreview(event.getPlayer(), event.getDefinition());
    }
}
