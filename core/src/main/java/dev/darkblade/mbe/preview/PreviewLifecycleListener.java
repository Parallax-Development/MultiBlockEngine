package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PreviewLifecycleListener implements Listener {
    private final StructurePreviewServiceImpl previewService;

    public PreviewLifecycleListener(StructurePreviewServiceImpl previewService) {
        this.previewService = previewService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        previewService.destroyPreview(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        previewService.destroyPreview(event.getPlayer());
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        if (!previewService.hasActivePreview(event.getPlayer())) {
            return;
        }
        previewService.touch(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!previewService.hasActivePreview(event.getPlayer())) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            previewService.touch(event.getPlayer());
            return;
        }
        Location origin = event.getClickedBlock().getLocation();
        if (event.getBlockFace() != null) {
            origin = origin.add(event.getBlockFace().getModX(), event.getBlockFace().getModY(), event.getBlockFace().getModZ());
        }
        previewService.updatePreviewOrigin(event.getPlayer(), origin);
    }
}
