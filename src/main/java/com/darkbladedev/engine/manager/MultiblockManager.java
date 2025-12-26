package com.darkbladedev.engine.manager;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.event.MultiblockFormEvent;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.model.action.Action;
import com.darkbladedev.engine.storage.StorageManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockManager {
    private final Map<String, MultiblockType> types = new HashMap<>();
    private final Map<Location, MultiblockInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<Location, MultiblockInstance> blockToInstanceMap = new ConcurrentHashMap<>();
    private final MetricsManager metrics = new MetricsManager();
    private final HologramManager holograms = new HologramManager();
    private StorageManager storage;
    private BukkitTask tickTask;
    
    // Supported rotations
    private static final BlockFace[] ROTATIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public void setStorage(StorageManager storage) {
        this.storage = storage;
    }

    public void registerType(MultiblockType type) {
        types.put(type.id(), type);
    }
    
    public void unregisterAll() {
        stopTicking();
        holograms.removeAll();
        types.clear();
        activeInstances.clear();
        blockToInstanceMap.clear();
        metrics.reset();
    }
    
    public void reloadTypes(Collection<MultiblockType> newTypes) {
        types.clear();
        for (MultiblockType type : newTypes) {
            registerType(type);
        }
    }
    
    public void stopTicking() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
        }
        tickTask = null;
    }
    
    public void startTicking(MultiBlockEngine plugin) {
        stopTicking();
        
        // Load config for metrics
        metrics.setEnabled(plugin.getConfig().getBoolean("metrics", true));
        
        // Run every tick (1)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }
    
    public MetricsManager getMetrics() {
        return metrics;
    }
    
    private void tick() {
        long startTime = System.nanoTime();
        long currentTick = Bukkit.getCurrentTick();
        
        for (MultiblockInstance instance : activeInstances.values()) {
            // Allow tick execution even if INACTIVE, so logic can check state conditions
            // Optimization: If state is DISABLED, skip.
            if (instance.state() == MultiblockState.DISABLED || instance.state() == MultiblockState.DAMAGED) continue;
            
            // Check interval
            if (currentTick % instance.type().tickInterval() != 0) continue;
            
            // Check if should tick (actions exist)
            if (instance.type().onTickActions().isEmpty()) continue;
            
            // Adaptive Ticking: Check player distance
            // If no player within 64 blocks, skip tick
            if (!isPlayerNearby(instance.anchorLocation(), 64)) {
                continue;
            }
            
            // Execute tick actions
            for (Action action : instance.type().onTickActions()) {
                action.execute(instance);
            }
        }
        
        metrics.recordTickTime(System.nanoTime() - startTime);
    }
    
    private boolean isPlayerNearby(Location loc, double radius) {
        if (loc.getWorld() == null) return false;
        // Simple optimization: check if chunk is loaded first
        if (!loc.getChunk().isLoaded()) return false;
        
        // getNearbyPlayers is efficient in modern Paper/Spigot
        Collection<Player> players = loc.getNearbyPlayers(radius);
        return !players.isEmpty();
    }
    
    public Optional<MultiblockType> getType(String id) {
        return Optional.ofNullable(types.get(id));
    }
    
    public Collection<MultiblockType> getTypes() {
        return types.values();
    }
    
    /**
     * Tries to create a multiblock instance from a controller block.
     * Handles rotation automatically.
     */
    public Optional<MultiblockInstance> tryCreate(Block anchor, MultiblockType type, Player player) {
        // Check if anchor matches controller matcher
        if (!type.controllerMatcher().matches(anchor)) {
            return Optional.empty();
        }

        // Determine potential facings
        List<BlockFace> candidates = new ArrayList<>();
        
        // If block is directional, prioritize its facing
        if (anchor.getBlockData() instanceof Directional directional) {
            candidates.add(directional.getFacing());
        } else {
            // Otherwise, check all 4 cardinal directions
            Collections.addAll(candidates, ROTATIONS);
        }

        for (BlockFace facing : candidates) {
            if (checkPattern(anchor, type, facing)) {
                 // If valid, create instance with this facing
                MultiblockInstance instance = new MultiblockInstance(type, anchor.getLocation(), facing);
                
                // Fire Event
                MultiblockFormEvent event = new MultiblockFormEvent(instance, player);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return Optional.empty();
                }
                
                registerInstance(instance);
                
                // Execute onCreate actions
                for (Action action : type.onCreateActions()) {
                    action.execute(instance);
                }
                
                // Save to storage
                if (storage != null && type.persistent()) {
                    storage.saveInstance(instance);
                }
                
                holograms.spawnHologram(instance);
                
                return Optional.of(instance);
            }
        }
        
        return Optional.empty();
    }
    
    private boolean checkPattern(Block anchor, MultiblockType type, BlockFace facing) {
        for (PatternEntry entry : type.pattern()) {
            Vector originalOffset = entry.offset();
            Vector rotatedOffset = rotateVector(originalOffset, facing);
            
            Block target = anchor.getRelative(rotatedOffset.getBlockX(), rotatedOffset.getBlockY(), rotatedOffset.getBlockZ());
            
            // Chunk Safety Check
            if (!target.getChunk().isLoaded()) {
                // If chunk is not loaded, we cannot validate.
                // Fail creation to prevent loading chunks or working with partial data.
                return false;
            }

            if (!entry.matcher().matches(target)) {
                // If optional, skip matching failure
                if (entry.optional()) {
                    continue;
                }
                return false;
            }
            // Check overlap
            if (blockToInstanceMap.containsKey(target.getLocation())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Rotates a vector assuming original is NORTH facing.
     */
    private Vector rotateVector(Vector v, BlockFace facing) {
        int x = v.getBlockX();
        int y = v.getBlockY();
        int z = v.getBlockZ();
        
        return switch (facing) {
            case NORTH -> new Vector(x, y, z); // No rotation
            case EAST -> new Vector(-z, y, x); // 90 deg CW
            case SOUTH -> new Vector(-x, y, -z); // 180 deg
            case WEST -> new Vector(z, y, -x); // 270 deg CW
            default -> new Vector(x, y, z);
        };
    }
    
    public void registerInstance(MultiblockInstance instance) {
        activeInstances.put(instance.anchorLocation(), instance);
        // Map all blocks
        // Map controller (at 0,0,0 relative to anchor, which IS the anchor)
        blockToInstanceMap.put(instance.anchorLocation(), instance); 
        
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = rotateVector(entry.offset(), instance.facing());
            Location loc = instance.anchorLocation().clone().add(offset);
            blockToInstanceMap.put(loc, instance);
        }
        holograms.spawnHologram(instance);
    }
    
    public void destroyInstance(MultiblockInstance instance) {
        if (instance == null) return;
        activeInstances.remove(instance.anchorLocation());
        metrics.incrementDestroyed();
        
        // Unmap blocks
        blockToInstanceMap.remove(instance.anchorLocation());
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = rotateVector(entry.offset(), instance.facing());
            Location loc = instance.anchorLocation().clone().add(offset);
            blockToInstanceMap.remove(loc);
        }
        
        holograms.removeHologram(instance);
        
        // Remove from storage
        if (storage != null && instance.type().persistent()) {
            storage.deleteInstance(instance);
        }
    }
    
    public void updateInstanceState(MultiblockInstance instance, MultiblockState newState) {
        instance.setState(newState);
        // Since MultiblockInstance is now mutable for state/variables, we don't need to replace it in maps.
        // We just need to persist the change.
        
        // Persist change
        if (storage != null && instance.type().persistent()) {
            // Delete and re-save to update state in DB (inefficient but works for now)
            storage.deleteInstance(instance);
            storage.saveInstance(instance);
        }
    }
    
    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        return Optional.ofNullable(blockToInstanceMap.get(loc));
    }
}
