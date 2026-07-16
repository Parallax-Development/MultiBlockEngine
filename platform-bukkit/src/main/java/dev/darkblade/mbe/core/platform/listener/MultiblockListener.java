package dev.darkblade.mbe.core.platform.listener;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchDispatcher;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.service.interaction.InteractionIntent;
import dev.darkblade.mbe.api.service.interaction.InteractionPipelineService;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolRegistry;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import dev.darkblade.mbe.core.application.service.interaction.DefaultInteractionPipelineService;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.tool.ToolDispatcher;
import dev.darkblade.mbe.core.application.service.ui.InteractionRouter;
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.core.platform.interaction.BukkitInteractionIntentFactory;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.api.service.lifecycle.MultiblockLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.Event;

public class MultiblockListener implements Listener {

    private static final MessageKey MSG_DISASSEMBLED = MessageKey.of("mbe", "core.wrench.disassembled");

    private final MultiblockRuntimeService manager;
    private final Consumer<dev.darkblade.mbe.api.event.MBEEvent> eventCaller;
    private final AssemblyCoordinator assembly;
    private final I18nService i18n;
    private final InteractionPipelineService interactionPipeline;
    private final BukkitInteractionIntentFactory intentFactory;
    private ToolDispatcher toolDispatcher;
    private ItemStackBridge itemStackBridge;
    private ToolRegistry toolRegistry;
    private dev.darkblade.mbe.api.platform.PlatformService platformService;
    private final MultiblockLifecycleService lifecycleService;

    public MultiblockListener(MultiblockRuntimeService manager) {
        this(manager, e -> {}, null, null, null, null, null, null);
    }

    public MultiblockListener(
            MultiblockRuntimeService manager,
            Consumer<dev.darkblade.mbe.api.event.MBEEvent> eventCaller,
            AssemblyCoordinator assembly,
            I18nService i18n,
            InteractionPipelineService interactionPipeline,
            BukkitInteractionIntentFactory intentFactory,
            dev.darkblade.mbe.api.platform.PlatformService platformService,
            MultiblockLifecycleService lifecycleService) {
        this.manager = manager;
        this.eventCaller = eventCaller;
        this.assembly = assembly;
        this.i18n = i18n;
        this.interactionPipeline = interactionPipeline;
        this.intentFactory = intentFactory == null ? new BukkitInteractionIntentFactory() : intentFactory;
        this.platformService = platformService;
        this.lifecycleService = lifecycleService;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (assembly == null) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        AssemblyContext ctx = new AssemblyContext(
                event.getPlayer(),
                event.getBlockPlaced(),
                null);
        assembly.tryAssembleFromPlacedBlock(event.getBlockPlaced(), ctx);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        InteractionIntent intent = intentFactory.from(event);
        if (intent == null) {
            return;
        }
        if (interactionPipeline == null) {
            if (assembly == null) {
                return;
            }
            AssemblyReport report = assembly.tryAssemble(intent);
            if (report != null && report.success()) {
                event.setCancelled(true);
                return;
            }
            if (report != null) {
                sendAssemblyFailureMessage(intent.player(), report);
            }
            return;
        }
        boolean cancelled = interactionPipeline.handle(intent);
        if (cancelled) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    private void sendAssemblyFailureMessage(Player player, dev.darkblade.mbe.api.assembly.AssemblyReport report) {
        if (player == null || report == null || report.reasonKey() == null || report.reasonKey().isBlank()) {
            return;
        }
        if (i18n == null) {
            return;
        }
        dev.darkblade.mbe.api.i18n.MessageKey key = dev.darkblade.mbe.api.i18n.MessageKey.of("mbe", "commands.error." + report.reasonKey());
        String translated = i18n.tr(player, key);
        if (translated == null || translated.isBlank() || translated.equals(key.path())) {
            return;
        }
        PlayerMessageService messageService = resolveMessageService();
        if (messageService != null) {
            messageService.send(player, new dev.darkblade.mbe.api.message.PlayerMessage(key, dev.darkblade.mbe.api.message.MessageChannel.CHAT, dev.darkblade.mbe.api.message.MessagePriority.HIGH, java.util.Map.of()));
            return;
        }
        i18n.send(player, key);
    }

    private PlayerMessageService resolveMessageService() {
        dev.darkblade.mbe.core.MultiBlockEngine plugin = dev.darkblade.mbe.core.MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }



    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(block.getLocation());
        if (instanceOpt.isPresent()) {
            MultiblockInstance instance = instanceOpt.get();

            dev.darkblade.mbe.api.platform.MBEPlayer mbePlayer = platformService != null ? platformService.wrap(event.getPlayer(), dev.darkblade.mbe.api.platform.MBEPlayer.class) : null;
            if (lifecycleService != null) {
                boolean success = lifecycleService.tryDisassemble(instance, mbePlayer);
                if (!success) {
                    event.setCancelled(true);
                }
            } else {
                MultiblockBreakEvent mbEvent = new MultiblockBreakEvent(instance, mbePlayer);
                eventCaller.accept(mbEvent);
                if (mbEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                manager.destroyInstance(instance);
            }
        }
    }
}
