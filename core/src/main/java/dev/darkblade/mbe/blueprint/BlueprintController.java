package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.event.BlueprintCancelEvent;
import dev.darkblade.mbe.api.event.BlueprintConfirmEvent;
import dev.darkblade.mbe.api.event.BlueprintStartEvent;
import dev.darkblade.mbe.catalog.PreviewOriginResolver;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.preview.PreviewSession;
import dev.darkblade.mbe.preview.PreviewState;
import dev.darkblade.mbe.preview.StructurePreviewServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class BlueprintController {
    private final BuildContextService contextService;
    private final StructurePreviewServiceImpl previewService;
    private final BlueprintDefinitionResolver definitionResolver;
    private final PreviewOriginResolver originResolver;
    private final BlueprintHeldItemResolver heldItemResolver;

    public BlueprintController(
            BuildContextService contextService,
            StructurePreviewServiceImpl previewService,
            BlueprintDefinitionResolver definitionResolver,
            PreviewOriginResolver originResolver,
            BlueprintHeldItemResolver heldItemResolver
    ) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.previewService = Objects.requireNonNull(previewService, "previewService");
        this.definitionResolver = Objects.requireNonNull(definitionResolver, "definitionResolver");
        this.originResolver = Objects.requireNonNull(originResolver, "originResolver");
        this.heldItemResolver = Objects.requireNonNull(heldItemResolver, "heldItemResolver");
    }

    public boolean handleInput(Player player) {
        if (player == null) {
            return false;
        }
        PlayerBuildContext context = contextService.get(player);
        return ensureBlueprintSession(player, context);
    }

    public boolean handleLeftClick(Player player) {
        return cancel(player);
    }

    public boolean handleRightClick(Player player) {
        if (player == null) {
            return false;
        }
        PlayerBuildContext context = contextService.get(player);
        if (!ensureBlueprintSession(player, context)) {
            return false;
        }
        if (context.mode() != Mode.PREVIEW_PLACEMENT) {
            previewService.touch(player);
            return false;
        }
        MultiblockDefinition definition = resolveContextDefinition(context);
        PreviewSession session = context.preview();
        Location origin = context.lastResolvedOrigin();
        if (definition == null || session == null || origin == null) {
            return false;
        }
        BlueprintConfirmEvent confirmEvent = new BlueprintConfirmEvent(player, definition, origin);
        Bukkit.getPluginManager().callEvent(confirmEvent);
        if (confirmEvent.isCancelled()) {
            return false;
        }
        context.mode(Mode.BUILD_GUIDE);
        session.state(PreviewState.LOCKED);
        previewService.touch(player);
        return true;
    }

    public boolean handleHeldItem(Player player) {
        if (player == null) {
            return false;
        }
        return handleInput(player);
    }

    public void updatePreviewOnMove(Player player) {
        if (player == null) {
            return;
        }
        PlayerBuildContext context = contextService.get(player);
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
        previewService.touch(player);
    }

    public boolean startPlacement(Player player, MultiblockDefinition definition) {
        if (player == null || definition == null || definition.id() == null || definition.id().isBlank()) {
            return false;
        }
        BlueprintStartEvent startEvent = new BlueprintStartEvent(player, definition);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            return false;
        }
        if (previewService.hasActivePreview(player)) {
            previewService.destroyPreview(player);
        }
        PreviewSession session = previewService.startPreview(player, definition);
        if (session == null) {
            contextService.clear(player);
            return false;
        }
        session.state(PreviewState.MOVING);
        PlayerBuildContext context = contextService.get(player);
        context.mode(Mode.PREVIEW_PLACEMENT);
        context.preview(session);
        context.blueprintId(definition.id());
        context.activeBlueprint(definition);
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

    public boolean ensureBlueprintSession(Player player, PlayerBuildContext context) {
        if (player == null || context == null) {
            return false;
        }
        var blueprintStack = heldItemResolver.findBlueprint(player).orElse(null);
        if (blueprintStack == null) {
            if (context.mode() != Mode.NONE || previewService.hasActivePreview(player)) {
                cancel(player);
            }
            return false;
        }
        String structureId = heldItemResolver.blueprintId(blueprintStack).orElse(null);
        if (structureId == null) {
            if (context.mode() != Mode.NONE || previewService.hasActivePreview(player)) {
                cancel(player);
            }
            return false;
        }
        MultiblockDefinition definition = definitionResolver.resolve(structureId);
        if (definition == null) {
            cancel(player);
            return false;
        }
        if (!previewService.hasActivePreview(player) || context.preview() == null) {
            return startPlacement(player, definition);
        }
        if (!Objects.equals(context.blueprintId(), structureId)) {
            context.blueprintId(structureId);
            context.activeBlueprint(definition);
            PreviewSession existing = previewService.getSession(player);
            if (existing == null) {
                return startPlacement(player, definition);
            }
            previewService.switchDefinition(player, definition);
            context.preview(existing);
            context.mode(Mode.PREVIEW_PLACEMENT);
            if (existing.state() != PreviewState.MOVING) {
                existing.state(PreviewState.MOVING);
            }
            previewService.touch(player);
            return true;
        }
        context.activeBlueprint(definition);
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

    public boolean refreshState(Player player) {
        if (player == null) {
            return false;
        }
        return ensureBlueprintSession(player, contextService.get(player));
    }

    public boolean cancel(Player player) {
        if (player == null) {
            return false;
        }
        MultiblockDefinition definition = resolveContextDefinition(contextService.get(player));
        BlueprintCancelEvent cancelEvent = new BlueprintCancelEvent(player, definition);
        Bukkit.getPluginManager().callEvent(cancelEvent);
        if (cancelEvent.isCancelled()) {
            return false;
        }
        if (previewService.hasActivePreview(player)) {
            previewService.destroyPreview(player);
        }
        contextService.clear(player);
        return true;
    }

    private MultiblockDefinition resolveContextDefinition(PlayerBuildContext context) {
        if (context == null) {
            return null;
        }
        MultiblockDefinition active = context.activeBlueprint();
        if (active != null) {
            return active;
        }
        if (context.blueprintId() == null || context.blueprintId().isBlank()) {
            return null;
        }
        return definitionResolver.resolve(context.blueprintId());
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
