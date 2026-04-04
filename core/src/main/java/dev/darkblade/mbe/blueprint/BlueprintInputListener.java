package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.catalog.PreviewOriginResolver;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.preview.PreviewSession;
import dev.darkblade.mbe.preview.PreviewState;
import dev.darkblade.mbe.preview.StructurePreviewServiceImpl;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class BlueprintInputListener implements Listener {
    private final BuildContextService contextService;
    private final StructurePreviewServiceImpl previewService;
    private final BlueprintDefinitionResolver definitionResolver;
    private final PreviewOriginResolver originResolver;
    private final ItemStackBridge itemStackBridge;

    public BlueprintInputListener(
        BuildContextService contextService,
        StructurePreviewServiceImpl previewService,
        BlueprintDefinitionResolver definitionResolver,
        PreviewOriginResolver originResolver,
        ItemStackBridge itemStackBridge
    ) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.previewService = Objects.requireNonNull(previewService, "previewService");
        this.definitionResolver = Objects.requireNonNull(definitionResolver, "definitionResolver");
        this.originResolver = Objects.requireNonNull(originResolver, "originResolver");
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        PlayerBuildContext context = contextService.get(player);
        if (!ensureBlueprintSession(player, context)) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            cancel(player);
            return;
        }
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && context.mode() == Mode.PREVIEW_PLACEMENT) {
            event.setCancelled(true);
            context.mode(Mode.BUILD_GUIDE);
            PreviewSession session = context.preview();
            if (session == null) {
                session = previewService.getSession(player);
                context.preview(session);
            }
            if (session != null) {
                session.state(PreviewState.LOCKED);
            }
            previewService.touch(player);
            return;
        }
        previewService.touch(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerBuildContext context = contextService.get(player);
        ItemStack next = player.getInventory().getItem(event.getNewSlot());
        if (!BlueprintItem.isBlueprint(next, itemStackBridge)) {
            if (context.mode() != Mode.NONE || previewService.hasActivePreview(player)) {
                cancel(player);
            }
            return;
        }
        ensureBlueprintSession(player, context);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancel(event.getPlayer());
    }

    boolean ensureBlueprintSession(Player player, PlayerBuildContext context) {
        if (player == null || context == null) {
            return false;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String structureId = BlueprintItem.structureId(mainHand, itemStackBridge);
        if (structureId == null) {
            if (context.mode() != Mode.NONE || previewService.hasActivePreview(player)) {
                cancel(player);
            }
            return false;
        }
        if (!Objects.equals(context.blueprintId(), structureId) || !previewService.hasActivePreview(player) || context.preview() == null) {
            MultiblockDefinition definition = definitionResolver.resolve(structureId);
            if (definition == null) {
                cancel(player);
                return false;
            }
            if (previewService.hasActivePreview(player)) {
                previewService.destroyPreview(player);
            }
            PreviewSession session = previewService.startPreview(player, definition);
            if (session == null) {
                cancel(player);
                return false;
            }
            session.state(PreviewState.MOVING);
            context.preview(session);
            context.mode(Mode.PREVIEW_PLACEMENT);
            context.blueprintId(structureId);
            Location origin = originResolver.resolve(player);
            if (origin != null) {
                Location snapped = origin.getBlock().getLocation();
                previewService.updatePreviewOrigin(player, snapped);
                context.lastResolvedOrigin(snapped);
            } else {
                context.lastResolvedOrigin(null);
            }
            previewService.touch(player);
            return true;
        }
        if (context.mode() == Mode.NONE) {
            context.mode(Mode.PREVIEW_PLACEMENT);
            PreviewSession session = context.preview();
            if (session != null) {
                session.state(PreviewState.MOVING);
            }
        }
        previewService.touch(player);
        return true;
    }

    void cancel(Player player) {
        if (player == null) {
            return;
        }
        if (previewService.hasActivePreview(player)) {
            previewService.destroyPreview(player);
        }
        contextService.clear(player);
    }
}
