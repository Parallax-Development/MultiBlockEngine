package dev.darkblade.mbe.preview;

import dev.darkblade.mbe.blueprint.BuildContextService;
import dev.darkblade.mbe.blueprint.Mode;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class PreviewBlockPlaceListener implements Listener {
    private final StructurePreviewServiceImpl previewService;
    private final BuildContextService contextService;

    public PreviewBlockPlaceListener(StructurePreviewServiceImpl previewService, BuildContextService contextService) {
        this.previewService = previewService;
        this.contextService = contextService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!previewService.hasActivePreview(event.getPlayer())) {
            return;
        }
        if (contextService.get(event.getPlayer()).mode() != Mode.BUILD_GUIDE) {
            return;
        }
        Block block = event.getBlockPlaced();
        BlockPosition position = new BlockPosition(
            block.getWorld(),
            block.getX(),
            block.getY(),
            block.getZ()
        );
        previewService.handlePlacedBlock(event.getPlayer(), position, block.getBlockData());
        if (!previewService.hasActivePreview(event.getPlayer())) {
            contextService.clear(event.getPlayer());
            return;
        }
        previewService.touch(event.getPlayer());
    }
}
