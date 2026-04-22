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
    private final Consumer<Event> eventCaller;
    private final AssemblyCoordinator assembly;
    private final I18nService i18n;
    private final InteractionPipelineService interactionPipeline;
    private final BukkitInteractionIntentFactory intentFactory;
    private ToolDispatcher toolDispatcher;
    private ItemStackBridge itemStackBridge;
    private ToolRegistry toolRegistry;

    public MultiblockListener(MultiblockRuntimeService manager) {
        this(manager, Bukkit.getPluginManager()::callEvent, null, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller) {
        this(manager, eventCaller, null, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller, WrenchDispatcher wrenchDispatcher) {
        this(manager, eventCaller, wrenchDispatcher, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, WrenchDispatcher wrenchDispatcher) {
        this(manager, Bukkit.getPluginManager()::callEvent, wrenchDispatcher, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly) {
        this(manager, Bukkit.getPluginManager()::callEvent, wrenchDispatcher, assembly, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly, I18nService i18n) {
        this(manager, Bukkit.getPluginManager()::callEvent, wrenchDispatcher, assembly, i18n);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly) {
        this(manager, eventCaller, wrenchDispatcher, assembly, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly, I18nService i18n) {
        this(
                manager,
                eventCaller,
                assembly,
                i18n,
                new DefaultInteractionPipelineService(assembly, wrenchDispatcher, new InteractionRouter(), null, manager),
                new BukkitInteractionIntentFactory()
        );
    }

    public MultiblockListener(
            MultiblockRuntimeService manager,
            Consumer<Event> eventCaller,
            AssemblyCoordinator assembly,
            I18nService i18n,
            InteractionPipelineService interactionPipeline,
            BukkitInteractionIntentFactory intentFactory
    ) {
        this.manager = manager;
        this.eventCaller = eventCaller;
        this.assembly = assembly;
        this.i18n = i18n;
        this.interactionPipeline = interactionPipeline;
        this.intentFactory = intentFactory == null ? new BukkitInteractionIntentFactory() : intentFactory;
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
                null
        );
        assembly.tryAssembleFromPlacedBlock(event.getBlockPlaced(), ctx);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.ALLOW);
        if ((event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                && event.getClickedBlock() != null
                && isToolItem(event.getItem())) {
            ToolDispatcher dispatcher = resolveToolDispatcher();
            if (dispatcher == null) {
                return;
            }
            WrenchContext context = new WrenchContext(
                    event.getPlayer(),
                    event.getClickedBlock(),
                    event.getAction(),
                    event.getItem(),
                    event.getHand()
            );
            dispatcher.dispatch(context, resolveTrigger(event));
            event.setCancelled(true);
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
        if (interactionPipeline.handle(intent)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        }
    }

    private ActionTrigger resolveTrigger(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            return ActionTrigger.SHIFT_RIGHT_CLICK;
        }
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            return ActionTrigger.SHIFT_LEFT_CLICK;
        }
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return ActionTrigger.RIGHT_CLICK;
        }
        return ActionTrigger.LEFT_CLICK;
    }

    private boolean isToolItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemStackBridge bridge = resolveItemStackBridge();
        ToolRegistry registry = resolveToolRegistry();
        if (bridge == null || registry == null) {
            return false;
        }
        ItemInstance instance;
        try {
            instance = bridge.fromItemStack(item);
        } catch (Throwable t) {
            return false;
        }
        if (instance == null || instance.definition() == null || instance.definition().key() == null || instance.definition().key().id() == null) {
            return false;
        }
        String inferredToolId = instance.definition().key().id().key();
        return registry.get(inferredToolId) != null;
    }

    private ToolDispatcher resolveToolDispatcher() {
        if (toolDispatcher != null) {
            return toolDispatcher;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        toolDispatcher = plugin.getAddonLifecycleService().getCoreService(ToolDispatcher.class);
        return toolDispatcher;
    }

    private ItemStackBridge resolveItemStackBridge() {
        if (itemStackBridge != null) {
            return itemStackBridge;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        itemStackBridge = plugin.getAddonLifecycleService().getCoreService(ItemStackBridge.class);
        return itemStackBridge;
    }

    private ToolRegistry resolveToolRegistry() {
        if (toolRegistry != null) {
            return toolRegistry;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        toolRegistry = plugin.getAddonLifecycleService().getCoreService(ToolRegistry.class);
        return toolRegistry;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(block.getLocation());
        if (instanceOpt.isPresent()) {
            MultiblockInstance instance = instanceOpt.get();

            MultiblockBreakEvent mbEvent = new MultiblockBreakEvent(instance, event.getPlayer());
            eventCaller.accept(mbEvent);
            if (mbEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }
            
            for (dev.darkblade.mbe.core.domain.action.Action action : instance.type().onBreakActions()) {
                executeActionSafely("BREAK", action, instance, null);
            }
            unregisterLimit(instance, event.getPlayer());
            manager.destroyInstance(instance);
            sendDisassembledMessage(event.getPlayer(), instance);
        }
    }

    private void sendDisassembledMessage(Player player, MultiblockInstance instance) {
        if (player == null || instance == null || instance.type() == null) {
            return;
        }
        String typeId = instance.type().id() == null ? "" : instance.type().id();
        PlayerMessageService messageService = resolveMessageService();
        if (messageService != null) {
            messageService.send(player, new PlayerMessage(
                    MSG_DISASSEMBLED,
                    MessageChannel.CHAT,
                    MessagePriority.NORMAL,
                    Map.of("type", typeId)
            ));
            return;
        }
        I18nService service = resolveI18n();
        if (service != null) {
            service.send(player, MSG_DISASSEMBLED, Map.of("type", typeId));
        }
    }

    private I18nService resolveI18n() {
        if (i18n != null) {
            return i18n;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(I18nService.class);
    }

    private void sendAssemblyFailureMessage(Player player, AssemblyReport report) {
        if (player == null || report == null || report.reasonKey() == null || report.reasonKey().isBlank()) {
            return;
        }
        I18nService service = resolveI18n();
        if (service == null) {
            return;
        }
        MessageKey key = MessageKey.of("mbe", "commands.error." + report.reasonKey());
        String translated = service.tr(player, key);
        if (translated == null || translated.isBlank() || translated.equals(key.path())) {
            return;
        }
        PlayerMessageService messageService = resolveMessageService();
        if (messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.HIGH, Map.of()));
            return;
        }
        service.send(player, key);
    }

    private PlayerMessageService resolveMessageService() {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }

    private void unregisterLimit(MultiblockInstance instance, Player player) {
        if (instance == null || instance.type() == null) {
            return;
        }
        MultiblockLimitService limitService = resolveLimitService();
        if (limitService == null) {
            return;
        }
        UUID ownerId = resolveOwnerId(instance, player);
        if (ownerId == null) {
            return;
        }
        limitService.unregisterAssembly(ownerId, instance.type().id());
    }

    private MultiblockLimitService resolveLimitService() {
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return null;
        }
        return plugin.getAddonLifecycleService().getCoreService(MultiblockLimitService.class);
    }

    private UUID resolveOwnerId(MultiblockInstance instance, Player player) {
        if (player != null) {
            return player.getUniqueId();
        }
        Object owner = instance.getVariable("owner_uuid");
        if (owner == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(owner));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void executeActionSafely(String runtimePhase, dev.darkblade.mbe.core.domain.action.Action action, MultiblockInstance instance, Player player) {
        try {
            if (player != null) {
                action.execute(instance, player);
            } else {
                action.execute(instance);
            }
        } catch (Throwable t) {
            String ownerId = action != null ? action.ownerId() : null;
            String typeKey = action != null ? action.typeKey() : null;

            String actionName = "unknown";
            if (typeKey != null && !typeKey.isBlank()) {
                int idx = typeKey.lastIndexOf(':');
                actionName = idx >= 0 ? typeKey.substring(idx + 1) : typeKey;
            } else if (action != null) {
                actionName = action.getClass().getSimpleName();
            }

            Object counter = instance != null ? instance.getVariable("counter") : null;
            String msg = "[" + runtimePhase + "] Action '" + actionName + "' failed Context: counter=" + counter + " Multiblock=" + (instance != null ? instance.type().id() : "unknown") + " Execution continued";

            if (ownerId != null && !ownerId.isBlank() && MultiBlockEngine.getInstance().getAddonLifecycleService() != null) {
                MultiBlockEngine.getInstance().getAddonLifecycleService().failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, false);
            } else {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingService() != null ? MultiBlockEngine.getInstance().getLoggingService().core() : null;
                if (core != null) {
                    core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[] {
                        LogKv.kv("phase", runtimePhase),
                        LogKv.kv("multiblock", instance != null ? instance.type().id() : "unknown"),
                        LogKv.kv("action", actionName)
                    }, Set.of());
                } else {
                    MultiBlockEngine.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "[Runtime] " + msg + " Cause: " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                }
            }
        }
    }
}
