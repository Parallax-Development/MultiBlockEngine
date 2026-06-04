package dev.darkblade.mbe.core.application.service.multiblock;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.domain.MultiblockState;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockInstanceRegistry {
    private final Map<Location, MultiblockInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<Location, MultiblockInstance> blockToInstanceMap = new ConcurrentHashMap<>();

    public void registerInstance(MultiblockInstance instance) {
        if (instance == null || instance.anchorLocation() == null) {
            return;
        }
        activeInstances.put(instance.anchorLocation(), instance);
        for (Location loc : instanceOccupiedLocations(instance)) {
            blockToInstanceMap.put(loc, instance);
        }
    }

    public void destroyInstance(MultiblockInstance instance) {
        if (instance == null || instance.anchorLocation() == null) {
            return;
        }
        activeInstances.remove(instance.anchorLocation());
        for (Location loc : instanceOccupiedLocations(instance)) {
            blockToInstanceMap.remove(loc);
        }
    }

    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        return Optional.ofNullable(blockToInstanceMap.get(loc));
    }

    public Collection<MultiblockInstance> getActiveInstancesSnapshot() {
        return List.copyOf(activeInstances.values());
    }

    public boolean isInstanceActive(MultiblockInstance instance) {
        if (instance == null || instance.anchorLocation() == null) return false;
        return activeInstances.containsKey(instance.anchorLocation());
    }

    public void unregisterAll() {
        activeInstances.clear();
        blockToInstanceMap.clear();
    }

    private List<Location> instanceOccupiedLocations(MultiblockInstance instance) {
        if (instance == null) {
            return List.of();
        }
        Location anchor = instance.anchorLocation();
        if (anchor == null || anchor.getWorld() == null) {
            return List.of();
        }

        List<Location> out = new ArrayList<>();
        out.add(anchor);

        BlockFace facing = instance.facing() == null ? BlockFace.NORTH : instance.facing();
        for (PatternEntry entry : instance.type().pattern()) {
            if (entry == null || entry.offset() == null) {
                continue;
            }
            Vector rotated = rotateVector(entry.offset(), facing);
            Location loc = anchor.clone().add(rotated.getBlockX(), rotated.getBlockY(), rotated.getBlockZ());
            out.add(loc);
        }

        return out;
    }

    private Vector rotateVector(Vector v, BlockFace facing) {
        switch (facing) {
            case NORTH: return v.clone();
            case EAST: return new Vector(-v.getZ(), v.getY(), v.getX());
            case SOUTH: return new Vector(-v.getX(), v.getY(), -v.getZ());
            case WEST: return new Vector(v.getZ(), v.getY(), -v.getX());
            default: return v.clone();
        }
    }
}
