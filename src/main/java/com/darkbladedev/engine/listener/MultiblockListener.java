package com.darkbladedev.engine.listener;

import com.darkbladedev.engine.api.event.MultiblockInteractEvent;
import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.addon.AddonException;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.manager.MultiblockManager;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;
import java.util.Set;

import org.bukkit.inventory.EquipmentSlot;

public class MultiblockListener implements Listener {

    private final MultiblockManager manager;

    public MultiblockListener(MultiblockManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        checkController(block, event.getPlayer());
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        // Fix: Prevent double execution (OffHand + MainHand)
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        // Check if interacting with existing structure
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(event.getClickedBlock().getLocation());
        if (instanceOpt.isPresent()) {
             MultiblockInstance instance = instanceOpt.get();
             MultiblockInteractEvent mbEvent = new MultiblockInteractEvent(instance, event.getPlayer(), event.getAction(), event.getClickedBlock());
             Bukkit.getPluginManager().callEvent(mbEvent);
             if (mbEvent.isCancelled()) {
                 event.setCancelled(true);
                 return;
             }
             
             // Execute Interact Actions
             for (com.darkbladedev.engine.model.action.Action action : instance.type().onInteractActions()) {
                 executeActionSafely("INTERACT", action, instance, event.getPlayer());
             }
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            checkController(event.getClickedBlock(), event.getPlayer());
        }
    }

    @SuppressWarnings("deprecation")
    private void checkController(Block block, Player player) {
        // Optimization: In a real plugin, we would have a map of Material -> List<MultiblockType>
        // to avoid iterating all types.
        for (MultiblockType type : manager.getTypes()) {
            if (type.controllerMatcher().matches(block)) {
                // Potential controller.
                // Check if already an instance
                if (manager.getInstanceAt(block.getLocation()).isPresent()) {
                    continue; // Already active
                }
                
                Optional<MultiblockInstance> instance = manager.tryCreate(block, type, player);
                if (instance.isPresent()) {
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "Structure formed: " + type.id());
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Optional<MultiblockInstance> instanceOpt = manager.getInstanceAt(block.getLocation());
        if (instanceOpt.isPresent()) {
            MultiblockInstance instance = instanceOpt.get();
            
            // Execute Break Actions
            for (com.darkbladedev.engine.model.action.Action action : instance.type().onBreakActions()) {
                executeActionSafely("BREAK", action, instance, null);
            }
            
            manager.destroyInstance(instance);
            event.getPlayer().sendMessage(ChatColor.RED + "Structure destroyed: " + instance.type().id());
        }
    }

    private void executeActionSafely(String runtimePhase, com.darkbladedev.engine.model.action.Action action, MultiblockInstance instance, Player player) {
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

            if (ownerId != null && !ownerId.isBlank() && MultiBlockEngine.getInstance().getAddonManager() != null) {
                MultiBlockEngine.getInstance().getAddonManager().failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, false);
            } else {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingManager() != null ? MultiBlockEngine.getInstance().getLoggingManager().core() : null;
                if (core != null) {
                    core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[] {
                        LogKv.kv("phase", runtimePhase),
                        LogKv.kv("multiblock", instance != null ? instance.type().id() : "unknown"),
                        LogKv.kv("action", actionName)
                    }, Set.of());
                } else {
                    MultiBlockEngine.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "[MultiBlockEngine][Runtime] " + msg + " Cause: " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                }
            }
        }
    }
}
