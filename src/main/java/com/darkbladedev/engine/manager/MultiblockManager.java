package com.darkbladedev.engine.manager;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockType;
import com.darkbladedev.engine.model.PatternEntry;
import com.darkbladedev.engine.storage.StorageManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockManager {
    private final Map<String, MultiblockType> types = new HashMap<>();
    private final Map<Location, MultiblockInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<Location, MultiblockInstance> blockToInstanceMap = new ConcurrentHashMap<>();
    private StorageManager storage;

    public void setStorage(StorageManager storage) {
        this.storage = storage;
    }

    public void registerType(MultiblockType type) {
        types.put(type.id(), type);
    }
    
    public void unregisterAll() {
        types.clear();
        activeInstances.clear();
        blockToInstanceMap.clear();
    }
    
    public Optional<MultiblockType> getType(String id) {
        return Optional.ofNullable(types.get(id));
    }
    
    public Collection<MultiblockType> getTypes() {
        return types.values();
    }
    
    /**
     * Tries to create a multiblock instance from a controller block.
     */
    public Optional<MultiblockInstance> tryCreate(Block anchor, MultiblockType type) {
        // Check if anchor matches controller matcher
        if (!type.controllerMatcher().matches(anchor)) {
            return Optional.empty();
        }

        // Validate pattern
        for (PatternEntry entry : type.pattern()) {
            Vector offset = entry.offset();
            Block target = anchor.getRelative(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ());
            if (!entry.matcher().matches(target)) {
                return Optional.empty();
            }
            // Check if block is already part of another instance?
            if (blockToInstanceMap.containsKey(target.getLocation())) {
                // Overlap detected. Policy?
                // For now, fail creation.
                return Optional.empty();
            }
        }
        
        // If valid, create instance
        MultiblockInstance instance = new MultiblockInstance(type, anchor.getLocation());
        registerInstance(instance);
        
        // Save to storage
        if (storage != null && type.persistent()) {
            storage.saveInstance(instance);
        }
        
        return Optional.of(instance);
    }
    
    public void registerInstance(MultiblockInstance instance) {
        activeInstances.put(instance.anchorLocation(), instance);
        // Map all blocks
        // Map controller (at 0,0,0 relative to anchor, which IS the anchor)
        blockToInstanceMap.put(instance.anchorLocation(), instance); 
        
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = entry.offset();
            Location loc = instance.anchorLocation().clone().add(offset);
            blockToInstanceMap.put(loc, instance);
        }
    }
    
    public void destroyInstance(MultiblockInstance instance) {
        if (instance == null) return;
        activeInstances.remove(instance.anchorLocation());
        
        // Unmap blocks
        blockToInstanceMap.remove(instance.anchorLocation());
        for (PatternEntry entry : instance.type().pattern()) {
            Vector offset = entry.offset();
            Location loc = instance.anchorLocation().clone().add(offset);
            blockToInstanceMap.remove(loc);
        }
        
        // Remove from storage
        if (storage != null && instance.type().persistent()) {
            storage.deleteInstance(instance);
        }
    }
    
    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        return Optional.ofNullable(blockToInstanceMap.get(loc));
    }
}
