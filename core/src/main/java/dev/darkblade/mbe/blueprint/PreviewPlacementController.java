package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.catalog.PreviewOriginResolver;
import dev.darkblade.mbe.preview.PreviewSession;
import dev.darkblade.mbe.preview.PreviewState;
import dev.darkblade.mbe.preview.StructurePreviewServiceImpl;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;

public final class PreviewPlacementController implements Listener {
    private final BuildContextService contextService;
    private final PreviewOriginResolver originResolver;
    private final StructurePreviewServiceImpl previewService;
    private final BlueprintInputListener inputListener;

    public PreviewPlacementController(
        BuildContextService contextService,
        PreviewOriginResolver originResolver,
        StructurePreviewServiceImpl previewService,
        BlueprintInputListener inputListener
    ) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.originResolver = Objects.requireNonNull(originResolver, "originResolver");
        this.previewService = Objects.requireNonNull(previewService, "previewService");
        this.inputListener = Objects.requireNonNull(inputListener, "inputListener");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerBuildContext context = contextService.get(player);
        if (!inputListener.ensureBlueprintSession(player, context)) {
            return;
        }
        if (context.mode() != Mode.PREVIEW_PLACEMENT) {
            previewService.touch(player);
            return;
        }
        PreviewSession session = context.preview();
        if (session == null || session.state() != PreviewState.MOVING) {
            previewService.touch(player);
            return;
        }
        Location origin = originResolver.resolve(player);
        if (origin == null) {
            return;
        }
        Location snapped = origin.getBlock().getLocation();
        if (sameBlock(snapped, context.lastResolvedOrigin())) {
            previewService.touch(player);
            return;
        }
        previewService.updatePreviewOrigin(player, snapped);
        context.lastResolvedOrigin(snapped);
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) {
            return false;
        }
        if (!Objects.equals(a.getWorld(), b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }
}
