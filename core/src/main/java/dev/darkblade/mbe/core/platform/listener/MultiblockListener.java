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
import dev.darkblade.mbe.core.domain.assembly.AssemblyCoordinator;
import dev.darkblade.mbe.api.assembly.AssemblyContext;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.Event;

public class MultiblockListener implements Listener {

    private final MultiblockRuntimeService manager;
    private final Consumer<Event> eventCaller;
    private final WrenchDispatcher wrenchDispatcher;
    private final AssemblyCoordinator assembly;

    public MultiblockListener(MultiblockRuntimeService manager) {
        this(manager, Bukkit.getPluginManager()::callEvent, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller) {
        this(manager, eventCaller, null, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller, WrenchDispatcher wrenchDispatcher) {
        this(manager, eventCaller, wrenchDispatcher, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, WrenchDispatcher wrenchDispatcher) {
        this(manager, Bukkit.getPluginManager()::callEvent, wrenchDispatcher, null);
    }

    public MultiblockListener(MultiblockRuntimeService manager, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly) {
        this(manager, Bukkit.getPluginManager()::callEvent, wrenchDispatcher, assembly);
    }


    public MultiblockListener(MultiblockRuntimeService manager, Consumer<Event> eventCaller, WrenchDispatcher wrenchDispatcher, AssemblyCoordinator assembly) {
        this.manager = manager;
        this.eventCaller = eventCaller;
        this.wrenchDispatcher = wrenchDispatcher;
        this.assembly = assembly;
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
                AssemblyContext.Cause.BLOCK_PLACE,
                event.getPlayer(),
                event.getBlockPlaced(),
                null,
                event.getItemInHand(),
                null,
                event.getPlayer() != null && event.getPlayer().isSneaking(),
                Map.of()
        );
        assembly.tryAssembleFromPlacedBlock(event.getBlockPlaced(), ctx);
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (wrenchDispatcher != null) {
            WrenchContext ctx = new WrenchContext(
                    event.getPlayer(),
                    event.getClickedBlock(),
                    event.getAction(),
                    event.getItem(),
                    event.getHand()
            );

            WrenchResult result = wrenchDispatcher.dispatch(ctx);
            if (result != null && result.cancelEvent()) {
                event.setCancelled(true);
                return;
            }
        }

        if (assembly == null) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getPlayer() == null || !event.getPlayer().isSneaking()) {
            return;
        }

        AssemblyContext ctx = new AssemblyContext(
                AssemblyContext.Cause.PLAYER_INTERACT,
                event.getPlayer(),
                event.getClickedBlock(),
                event.getAction(),
                event.getItem(),
                event.getHand(),
                event.getPlayer() != null && event.getPlayer().isSneaking(),
                Map.of("wrench", false)
        );
        assembly.tryAssembleAt(event.getClickedBlock(), ctx);
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
            
            // Execute Break Actions
            for (dev.darkblade.mbe.core.domain.action.Action action : instance.type().onBreakActions()) {
                executeActionSafely("BREAK", action, instance, null);
            }
            
            manager.destroyInstance(instance);
            event.getPlayer().sendMessage(Component.textOfChildren(
                    Component.text("Structure destroyed: ", NamedTextColor.RED),
                    Component.text(instance.type().id(), NamedTextColor.WHITE)
            ));
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
