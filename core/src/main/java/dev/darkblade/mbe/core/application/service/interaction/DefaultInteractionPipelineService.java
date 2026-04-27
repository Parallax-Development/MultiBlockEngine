package dev.darkblade.mbe.core.application.service.interaction;

import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.service.interaction.InteractionHandler;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionPipelineService;
import dev.darkblade.mbe.api.service.interaction.InteractionSource;
import dev.darkblade.mbe.api.service.interaction.InteractionType;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.ui.InteractionRouter;
import dev.darkblade.mbe.core.application.service.wrench.DefaultWrenchDispatcher;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.block.TileState;
import org.bukkit.event.block.Action;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultInteractionPipelineService implements InteractionPipelineService {

    private final AssemblyCoordinator assemblyCoordinator;
    private final WrenchDispatcher wrenchDispatcher;
    private final InteractionRouter interactionRouter;
    private final ItemStackBridge itemStackBridge;
    private final MultiblockRuntimeService multiblockRuntimeService;
    private final List<InteractionHandler> handlers = new CopyOnWriteArrayList<>();

    public DefaultInteractionPipelineService(
            AssemblyCoordinator assemblyCoordinator,
            WrenchDispatcher wrenchDispatcher,
            InteractionRouter interactionRouter,
            ItemStackBridge itemStackBridge,
            MultiblockRuntimeService multiblockRuntimeService
    ) {
        this.assemblyCoordinator = assemblyCoordinator;
        this.wrenchDispatcher = wrenchDispatcher;
        this.interactionRouter = interactionRouter;
        this.itemStackBridge = itemStackBridge;
        this.multiblockRuntimeService = multiblockRuntimeService;
    }

    @Override
    public boolean handle(InteractionIntent intent) {
        if (intent == null) {
            return false;
        }

        InteractionIntent effectiveIntent = normalizeIntent(intent);
        boolean cancelVanilla = false;

        if (wrenchDispatcher != null) {
            Action action = toBukkitAction(effectiveIntent.type());
            if (effectiveIntent.player() != null && effectiveIntent.targetBlock() != null && action != null) {
                WrenchContext ctx = new WrenchContext(
                        effectiveIntent.player(),
                        effectiveIntent.targetBlock(),
                        action,
                        effectiveIntent.itemInHand(),
                        org.bukkit.inventory.EquipmentSlot.HAND
                );
                WrenchResult result = wrenchDispatcher.dispatch(ctx);
                if (result != null && !result.isPass()) {
                    cancelVanilla = true;
                }
            }
        }

        if (assemblyCoordinator != null && effectiveIntent.type() != InteractionType.WRENCH_USE) {
            AssemblyReport report = assemblyCoordinator.tryAssemble(effectiveIntent);
            if (report != null && report.success()) {
                cancelVanilla = true;
            }
        }

        if (!cancelVanilla && interactionRouter != null) {
            cancelVanilla = interactionRouter.route(intent);
        }

        if (!cancelVanilla && multiblockRuntimeService != null && effectiveIntent.targetBlock() != null) {
            Optional<MultiblockInstance> instanceOpt = multiblockRuntimeService.getInstanceAt(effectiveIntent.targetBlock().getLocation());
            if (instanceOpt.isPresent()) {
                MultiblockInstance instance = instanceOpt.get();
                if (instance.type().pattern().isEmpty() && effectiveIntent.targetBlock().getState() instanceof org.bukkit.block.TileState) {
                    cancelVanilla = true;
                }
                
                org.bukkit.event.block.Action bukkitAction = toBukkitAction(effectiveIntent.type());
                if (bukkitAction != null && effectiveIntent.player() != null) {
                    dev.darkblade.mbe.api.event.MultiblockInteractEvent mbEvent = new dev.darkblade.mbe.api.event.MultiblockInteractEvent(instance, effectiveIntent.player(), bukkitAction, effectiveIntent.targetBlock());
                    org.bukkit.Bukkit.getPluginManager().callEvent(mbEvent);
                    if (mbEvent.isCancelled()) {
                        cancelVanilla = true;
                    } else {
                        for (dev.darkblade.mbe.core.domain.action.Action a : instance.type().onInteractActions()) {
                            if (a != null && a.shouldExecuteOnInteract(bukkitAction)) {
                                if (a.cancelsVanillaOnInteract(bukkitAction)) {
                                    cancelVanilla = true;
                                }
                                try {
                                    a.execute(instance, effectiveIntent.player());
                                } catch (Throwable t) {
                                    org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "Failed to execute interact action", t);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (InteractionHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            try {
                boolean result = handler.handle(effectiveIntent);
                if (result) {
                    cancelVanilla = true;
                }
            } catch (Throwable ignored) {
            }
        }

        return cancelVanilla;
    }

    @Override
    public void registerHandler(InteractionHandler handler) {
        if (handler == null) {
            return;
        }
        handlers.add(handler);
    }

    private InteractionIntent normalizeIntent(InteractionIntent intent) {
        if (intent.type() == InteractionType.WRENCH_USE) {
            return intent;
        }
        if (!isWrench(intent)) {
            return intent;
        }
        return new InteractionIntent(
                intent.player(),
                InteractionType.WRENCH_USE,
                intent.targetBlock(),
                intent.itemInHand(),
                InteractionSource.WRENCH
        );
    }

    private boolean isWrench(InteractionIntent intent) {
        if (itemStackBridge == null || intent == null || intent.itemInHand() == null || intent.itemInHand().getType().isAir()) {
            return false;
        }
        ItemInstance instance;
        try {
            instance = itemStackBridge.fromItemStack(intent.itemInHand());
        } catch (Throwable t) {
            return false;
        }
        if (instance == null || instance.definition() == null) {
            return false;
        }
        ItemKey key = instance.definition().key();
        return DefaultWrenchDispatcher.WRENCH_KEY.equals(key);
    }

    private Action toBukkitAction(InteractionType type) {
        if (type == InteractionType.LEFT_CLICK_BLOCK) {
            return Action.LEFT_CLICK_BLOCK;
        }
        if (type == InteractionType.RIGHT_CLICK_BLOCK || type == InteractionType.SHIFT_RIGHT_CLICK || type == InteractionType.WRENCH_USE) {
            return Action.RIGHT_CLICK_BLOCK;
        }
        return null;
    }
}
