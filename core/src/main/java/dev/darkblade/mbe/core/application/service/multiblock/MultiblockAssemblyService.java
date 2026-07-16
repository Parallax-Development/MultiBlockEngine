package dev.darkblade.mbe.core.application.service.multiblock;

import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.infrastructure.persistence.InstanceStorageService;
import dev.darkblade.mbe.core.application.service.HologramService;

import org.bukkit.Location;
import org.bukkit.block.Block;
import dev.darkblade.mbe.api.event.EventBusService;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiblockAssemblyService {

    private static final BlockFace[] ROTATIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final MultiblockTypeRegistry typeRegistry;
    private final MultiblockInstanceRegistry instanceRegistry;
    private final MultiblockTickingService tickingService;
    private final HologramService holograms;
    
    private InstanceStorageService storage;
    private dev.darkblade.mbe.api.platform.PlatformService platformService;
    private EventBusService eventBus;

    public MultiblockAssemblyService(MultiblockTypeRegistry typeRegistry,
                                     MultiblockInstanceRegistry instanceRegistry,
                                     MultiblockTickingService tickingService,
                                     HologramService holograms) {
        this.typeRegistry = typeRegistry;
        this.instanceRegistry = instanceRegistry;
        this.tickingService = tickingService;
        this.holograms = holograms;
    }

    public void setStorage(InstanceStorageService storage) {
        this.storage = storage;
    }

    public void setPlatformService(dev.darkblade.mbe.api.platform.PlatformService platformService) {
        this.platformService = platformService;
    }

    public void setEventBus(EventBusService eventBus) {
        this.eventBus = eventBus;
    }

    public Optional<MultiblockInstance> tryCreate(Block anchor, MultiblockType type, Player player) {
        if (!type.controllerMatcher().matches(anchor)) {
            return Optional.empty();
        }

        List<BlockFace> candidates = new ArrayList<>();
        
        if (anchor.getBlockData() instanceof Directional directional) {
            candidates.add(directional.getFacing());
        } else {
            Collections.addAll(candidates, ROTATIONS);
        }

        for (BlockFace facing : candidates) {
            if (checkPattern(anchor, type, facing)) {
                MultiblockInstance instance = new MultiblockInstance(type, anchor.getLocation(), facing);
                instance.setVariable("signature", typeRegistry.signatureOf(type));
                instance.setVariable("variant", type.id());
                if (player != null) {
                    instance.setVariable("owner_uuid", player.getUniqueId().toString());
                }
                
                dev.darkblade.mbe.api.platform.MBEPlayer mbePlayer = player != null && platformService != null ? platformService.wrap(player, dev.darkblade.mbe.api.platform.MBEPlayer.class) : null;
                MultiblockFormEvent event = new MultiblockFormEvent(instance, mbePlayer);
                if (eventBus != null) {
                    eventBus.publish(event);
                }
                if (event.isCancelled()) {
                    return Optional.empty();
                }
                
                instanceRegistry.registerInstance(instance);
                
                for (Action action : type.onCreateActions()) {
                    tickingService.executeActionSafely("CREATE", action, instance, player);
                }
                
                if (storage != null && type.persistent()) {
                    storage.saveInstance(instance);
                }
                
                holograms.spawnHologram(instance);
                
                return Optional.of(instance);
            }
        }
        
        return Optional.empty();
    }

    public boolean checkPattern(Block anchor, MultiblockType type, BlockFace facing) {
        for (PatternEntry entry : type.pattern()) {
            Vector originalOffset = entry.offset();
            Vector rotatedOffset = rotateVector(originalOffset, facing);
            
            Block target = anchor.getRelative(rotatedOffset.getBlockX(), rotatedOffset.getBlockY(), rotatedOffset.getBlockZ());
            
            if (!target.getChunk().isLoaded()) {
                return false;
            }
            
            if (!entry.matcher().matches(target)) {
                if (!entry.optional()) {
                    return false;
                }
            }
        }
        return true;
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

    public Optional<MultiblockInstance> switchVariant(MultiblockInstance current, Player player) {
        if (current == null || current.anchorLocation() == null || current.type() == null) {
            return Optional.empty();
        }
        Object sigVar = current.getVariable("signature");
        String sig = sigVar == null ? null : String.valueOf(sigVar);
        if (sig == null || sig.isBlank() || "null".equalsIgnoreCase(sig)) {
            sig = typeRegistry.signatureOf(current.type());
        }
        List<MultiblockType> variants = typeRegistry.variantsForSignature(sig);
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        int idx = 0;
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).id().equalsIgnoreCase(current.type().id())) {
                idx = i;
                break;
            }
        }
        int nextIdx = (idx + 1) % variants.size();
        MultiblockType nextType = variants.get(nextIdx);
        Block anchorBlock = current.anchorLocation().getBlock();
        BlockFace facing = current.facing() == null ? BlockFace.NORTH : current.facing();
        if (!checkPattern(anchorBlock, nextType, facing)) {
            return Optional.empty();
        }
        Map<String, Object> preserved = new HashMap<>(current.getVariables());
        MultiblockInstance next = new MultiblockInstance(nextType, current.anchorLocation(), facing, current.state(), preserved);
        next.setVariable("signature", sig);
        next.setVariable("variant", nextType.id());
        if (player != null) {
            next.setVariable("owner_uuid", player.getUniqueId().toString());
        }
        dev.darkblade.mbe.api.platform.MBEPlayer mbePlayer = player != null && platformService != null ? platformService.wrap(player, dev.darkblade.mbe.api.platform.MBEPlayer.class) : null;
        MultiblockFormEvent event = new MultiblockFormEvent(next, mbePlayer);
        if (eventBus != null) {
            eventBus.publish(event);
        }
        if (event.isCancelled()) {
            return Optional.empty();
        }
        
        instanceRegistry.destroyInstance(current);
        holograms.removeHologram(current);
        if (storage != null && current.type().persistent()) {
            storage.deleteInstance(current);
        }

        instanceRegistry.registerInstance(next);
        
        for (Action action : nextType.onCreateActions()) {
            tickingService.executeActionSafely("CREATE", action, next, player);
        }
        if (storage != null && nextType.persistent()) {
            storage.saveInstance(next);
        }
        holograms.spawnHologram(next);
        return Optional.of(next);
    }
}
