package com.darkbladedev.engine.manager;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.addon.AddonManager;
import com.darkbladedev.engine.api.event.MultiblockFormEvent;
import com.darkbladedev.engine.api.addon.AddonException;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.model.MultiblockState;
import com.darkbladedev.engine.model.MultiblockSource;
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
import java.util.Set;

public class MultiblockManager {
    private final Map<String, MultiblockType> types = new HashMap<>();
    private final Map<String, MultiblockSource> sourcesByTypeId = new HashMap<>();
    private final Map<Location, MultiblockInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<Location, MultiblockInstance> blockToInstanceMap = new ConcurrentHashMap<>();
    private final Set<Location> capabilitiesInitialized = ConcurrentHashMap.newKeySet();
    private final MetricsManager metrics = new MetricsManager();
    private final HologramManager holograms = new HologramManager();
    private AddonManager addonManager;
    private StorageManager storage;
    private BukkitTask tickTask;
    private final Map<String, List<MultiblockType>> variantsBySignature = new HashMap<>();
    
    // Supported rotations
    private static final BlockFace[] ROTATIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public void setStorage(StorageManager storage) {
        this.storage = storage;
    }

    public void setAddonManager(AddonManager addonManager) {
        this.addonManager = addonManager;
    }

    public void registerType(MultiblockType type) {
        registerType(type, new MultiblockSource(MultiblockSource.Type.USER_DEFINED, "<runtime>"));
    }

    public void registerType(MultiblockType type, MultiblockSource source) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (source == null) {
            source = new MultiblockSource(MultiblockSource.Type.USER_DEFINED, "<runtime>");
        }
        if (types.containsKey(type.id())) {
            throw new IllegalArgumentException("Duplicate multiblock id: " + type.id());
        }
        types.put(type.id(), type);
        sourcesByTypeId.put(type.id(), source);
        String sig = computeSignature(type);
        variantsBySignature.compute(sig, (k, list) -> {
            List<MultiblockType> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.add(type);
            next.sort(this::variantComparator);
            return List.copyOf(next);
        });
    }
    
    public Optional<MultiblockType> getType(String id) {
        return Optional.ofNullable(types.get(id));
    }

    public Optional<MultiblockSource> getSource(String typeId) {
        return Optional.ofNullable(sourcesByTypeId.get(typeId));
    }
    
    public Collection<MultiblockType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public List<MultiblockType> getTypesDeterministic() {
        List<MultiblockType> out = new ArrayList<>(types.values());
        out.sort(this::variantComparator);
        return List.copyOf(out);
    }
    
    public void unregisterAll() {
        stopTicking();
        holograms.removeAll();
        types.clear();
        sourcesByTypeId.clear();
        activeInstances.clear();
        blockToInstanceMap.clear();
        capabilitiesInitialized.clear();
        metrics.reset();
        variantsBySignature.clear();
    }
    
    public void reloadTypes(Collection<MultiblockType> newTypes) {
        Map<String, MultiblockType> runtimeTypes = new LinkedHashMap<>();
        Map<String, MultiblockSource> runtimeSources = new LinkedHashMap<>();
        for (Map.Entry<String, MultiblockType> e : types.entrySet()) {
            String id = e.getKey();
            MultiblockSource src = sourcesByTypeId.get(id);
            if (src != null && "<runtime>".equals(src.path())) {
                runtimeTypes.put(id, e.getValue());
                runtimeSources.put(id, src);
            }
        }

        types.clear();
        sourcesByTypeId.clear();
        variantsBySignature.clear();

        for (MultiblockType type : newTypes) {
            if (type == null) {
                continue;
            }
            if (runtimeTypes.containsKey(type.id())) {
                continue;
            }
            registerType(type);
        }

        for (Map.Entry<String, MultiblockType> e : runtimeTypes.entrySet()) {
            MultiblockSource src = runtimeSources.get(e.getKey());
            registerType(e.getValue(), src);
        }
    }

    public void reloadTypesWithSources(Collection<MultiblockType> newTypes, Map<String, MultiblockSource> sources) {
        Map<String, MultiblockType> runtimeTypes = new LinkedHashMap<>();
        Map<String, MultiblockSource> runtimeSources = new LinkedHashMap<>();
        for (Map.Entry<String, MultiblockType> e : types.entrySet()) {
            String id = e.getKey();
            MultiblockSource src = sourcesByTypeId.get(id);
            if (src != null && "<runtime>".equals(src.path())) {
                runtimeTypes.put(id, e.getValue());
                runtimeSources.put(id, src);
            }
        }

        types.clear();
        sourcesByTypeId.clear();
        variantsBySignature.clear();

        Map<String, MultiblockSource> src = sources == null ? Map.of() : sources;
        for (MultiblockType type : newTypes) {
            if (type == null) {
                continue;
            }
            if (runtimeTypes.containsKey(type.id())) {
                continue;
            }
            MultiblockSource source = src.get(type.id());
            registerType(type, source);
        }

        for (Map.Entry<String, MultiblockType> e : runtimeTypes.entrySet()) {
            MultiblockSource source = runtimeSources.get(e.getKey());
            registerType(e.getValue(), source);
        }
    }

    public String signatureOf(MultiblockType type) {
        return computeSignature(type);
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
                executeActionSafely("TICK", action, instance, null);
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
                instance.setVariable("signature", computeSignature(type));
                instance.setVariable("variant", type.id());
                
                // Fire Event
                MultiblockFormEvent event = new MultiblockFormEvent(instance, player);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return Optional.empty();
                }
                
                registerInstance(instance);
                
                // Execute onCreate actions
                for (Action action : type.onCreateActions()) {
                    executeActionSafely("CREATE", action, instance, player);
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

    private void executeActionSafely(String runtimePhase, Action action, MultiblockInstance instance, Player player) {
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

            if (addonManager != null && ownerId != null && !ownerId.isBlank() && !"core".equalsIgnoreCase(ownerId)) {
                addonManager.failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, false);
            } else {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingManager() != null ? MultiBlockEngine.getInstance().getLoggingManager().core() : null;
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
    
    private boolean checkPattern(Block anchor, MultiblockType type, BlockFace facing) {
        for (PatternEntry entry : type.pattern()) {
            Vector originalOffset = entry.offset();
            Vector rotatedOffset = rotateVector(originalOffset, facing);
            
            Block target = anchor.getRelative(rotatedOffset.getBlockX(), rotatedOffset.getBlockY(), rotatedOffset.getBlockZ());
            
            // Chunk Safety Check
            if (!target.getChunk().isLoaded()) {
                // If chunk is not loaded, we cannot validate.
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
        // Assume default is NORTH
        switch (facing) {
            case NORTH: return v.clone();
            case EAST: return new Vector(-v.getZ(), v.getY(), v.getX());
            case SOUTH: return new Vector(-v.getX(), v.getY(), -v.getZ());
            case WEST: return new Vector(v.getZ(), v.getY(), -v.getX());
            default: return v.clone();
        }
    }

    public List<MultiblockType> variantsForSignature(String signature) {
        List<MultiblockType> list = variantsBySignature.get(signature);
        return list == null ? List.of() : list;
    }

    public Optional<MultiblockInstance> switchVariant(MultiblockInstance current, Player player) {
        if (current == null || current.anchorLocation() == null || current.type() == null) {
            return Optional.empty();
        }
        Object sigVar = current.getVariable("signature");
        String sig = sigVar == null ? null : String.valueOf(sigVar);
        if (sig == null || sig.isBlank() || "null".equalsIgnoreCase(sig)) {
            sig = computeSignature(current.type());
        }
        List<MultiblockType> variants = variantsForSignature(sig);
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
        MultiblockFormEvent event = new MultiblockFormEvent(next, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Optional.empty();
        }
        destroyInstance(current);
        registerInstance(next);
        for (Action action : nextType.onCreateActions()) {
            executeActionSafely("CREATE", action, next, player);
        }
        if (storage != null && nextType.persistent()) {
            storage.saveInstance(next);
        }
        holograms.spawnHologram(next);
        return Optional.of(next);
    }

    private int variantComparator(MultiblockType a, MultiblockType b) {
        MultiblockSource.Type aSrc = sourceTypeOf(a);
        MultiblockSource.Type bSrc = sourceTypeOf(b);
        if (aSrc != bSrc) {
            return aSrc == MultiblockSource.Type.CORE_DEFAULT ? -1 : 1;
        }
        return a.id().compareToIgnoreCase(b.id());
    }

    private MultiblockSource.Type sourceTypeOf(MultiblockType type) {
        if (type == null || type.id() == null) {
            return MultiblockSource.Type.USER_DEFINED;
        }
        MultiblockSource source = sourcesByTypeId.get(type.id());
        return source == null ? MultiblockSource.Type.USER_DEFINED : source.type();
    }

    private boolean containsNamespace(String id) {
        return id != null && id.contains(":");
    }

    public String computeSignature(MultiblockType type) {
        String raw = computeSignatureRaw(type);
        return sha256(raw);
    }

    private String computeSignatureRaw(MultiblockType type) {
        StringBuilder sb = new StringBuilder();
        sb.append("controller:").append(matcherKey(type.controllerMatcher())).append("|");
        List<String> parts = new ArrayList<>();
        for (PatternEntry entry : type.pattern()) {
            Vector o = entry.offset();
            String k = matcherKey(entry.matcher());
            parts.add(o.getBlockX() + "," + o.getBlockY() + "," + o.getBlockZ() + "=" + k + (entry.optional() ? "?" : "!"));
        }
        parts.sort(String::compareToIgnoreCase);
        sb.append(String.join(";", parts));
        return sb.toString();
    }

    private String matcherKey(com.darkbladedev.engine.model.BlockMatcher matcher) {
        if (matcher == null) {
            return "";
        }
        if (matcher instanceof com.darkbladedev.engine.model.matcher.ExactMaterialMatcher m) {
            return m.material() == null ? "" : m.material().name();
        }
        if (matcher instanceof com.darkbladedev.engine.model.matcher.TagMatcher m) {
            if (m.tag() == null || m.tag().getKey() == null) {
                return "";
            }
            return "#" + m.tag().getKey();
        }
        if (matcher instanceof com.darkbladedev.engine.model.matcher.AirMatcher) {
            return "AIR";
        }
        if (matcher instanceof com.darkbladedev.engine.model.matcher.BlockDataMatcher m) {
            return m.expectedData() == null ? "" : m.expectedData().getAsString();
        }
        if (matcher instanceof com.darkbladedev.engine.model.matcher.AnyOfMatcher m) {
            List<String> parts = new ArrayList<>();
            for (com.darkbladedev.engine.model.BlockMatcher sub : m.matchers()) {
                String s = matcherKey(sub);
                if (!s.isBlank()) {
                    parts.add(s);
                }
            }
            parts.sort(String::compareToIgnoreCase);
            return String.join("|", parts);
        }
        return matcher.getClass().getSimpleName();
    }

    private String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return s;
        }
    }

    public void registerInstance(MultiblockInstance instance) {
        registerInstance(instance, true);
    }

    public void registerInstance(MultiblockInstance instance, boolean initializeCapabilities) {
        activeInstances.put(instance.anchorLocation(), instance);
        for (Location loc : instanceOccupiedLocations(instance)) {
            blockToInstanceMap.put(loc, instance);
        }
        if (initializeCapabilities) {
            initializeCapabilitiesOnce(instance);
        }
    }

    public void initializePendingCapabilities() {
        for (MultiblockInstance instance : activeInstances.values()) {
            initializeCapabilitiesOnce(instance);
        }
    }

    private void initializeCapabilitiesOnce(MultiblockInstance instance) {
        if (instance == null) {
            return;
        }
        Location key = instance.anchorLocation();
        if (!capabilitiesInitialized.add(key)) {
            return;
        }
        initializeCapabilities(instance);
    }

    private void initializeCapabilities(MultiblockInstance instance) {
        for (MultiblockType.CapabilityFactory factory : instance.type().capabilityFactories()) {
            if (factory == null) continue;

            String ownerId = factory.ownerId();
            try {
                var capability = factory.factory().apply(instance);
                if (capability == null) {
                    throw new IllegalStateException("Capability factory returned null");
                }
                instance.addCapability(capability);
            } catch (Throwable t) {
                String msg = "Failed to initialize capability for multiblock " + instance.type().id();
                if (addonManager != null && ownerId != null && !ownerId.isBlank() && !"core".equalsIgnoreCase(ownerId)) {
                    addonManager.failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, true);
                } else {
                    CoreLogger core = MultiBlockEngine.getInstance().getLoggingManager() != null ? MultiBlockEngine.getInstance().getLoggingManager().core() : null;
                    if (core != null) {
                        core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[] {
                            LogKv.kv("owner", ownerId == null ? "" : ownerId),
                            LogKv.kv("multiblock", instance.type().id())
                        }, Set.of());
                    } else {
                        MultiBlockEngine.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "[Capability][Owner:" + ownerId + "] " + msg, t);
                    }
                }
            }
        }
    }
    
    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        return Optional.ofNullable(blockToInstanceMap.get(loc));
    }
    
    public void destroyInstance(MultiblockInstance instance) {
        activeInstances.remove(instance.anchorLocation());
        for (Location loc : instanceOccupiedLocations(instance)) {
            blockToInstanceMap.remove(loc);
        }
        capabilitiesInitialized.remove(instance.anchorLocation());
        holograms.removeHologram(instance);
        
        if (storage != null && instance.type().persistent()) {
            storage.deleteInstance(instance);
        }
        
        metrics.incrementDestroyed();
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
    
    public void updateInstanceState(MultiblockInstance instance, MultiblockState newState) {
        instance.setState(newState);
        if (storage != null && instance.type().persistent()) {
            storage.saveInstance(instance);
        }
    }

    public void persistInstance(MultiblockInstance instance) {
        if (instance == null) {
            return;
        }
        if (storage != null && instance.type().persistent()) {
            storage.saveInstance(instance);
        }
    }
    
    public boolean isInstanceActive(MultiblockInstance instance) {
        return activeInstances.containsKey(instance.anchorLocation());
    }
}
