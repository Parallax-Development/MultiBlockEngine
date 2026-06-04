package dev.darkblade.mbe.core.application.service.multiblock;

import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.tick.Tickable;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MetricsService;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.action.Action;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;

public class MultiblockTickingService implements Tickable {

    private final MultiblockInstanceRegistry instanceRegistry;
    private final MetricsService metrics;
    
    private AddonLifecycleService addonManager;
    private long tickCounter = 0;

    public MultiblockTickingService(MultiblockInstanceRegistry instanceRegistry, MetricsService metrics) {
        this.instanceRegistry = instanceRegistry;
        this.metrics = metrics;
    }

    public void setAddonLifecycleService(AddonLifecycleService addonManager) {
        this.addonManager = addonManager;
    }

    @Override
    public void tick() {
        long startTime = System.nanoTime();
        long currentTick = tickCounter++;
        
        for (MultiblockInstance instance : instanceRegistry.getActiveInstancesSnapshot()) {
            if (instance.state() == MultiblockState.DISABLED || instance.state() == MultiblockState.DAMAGED) continue;
            
            if (currentTick % instance.type().tickInterval() != 0) continue;
            
            if (instance.type().onTickActions().isEmpty()) continue;
            
            if (!isPlayerNearby(instance.anchorLocation(), 64)) {
                continue;
            }
            
            for (Action action : instance.type().onTickActions()) {
                executeActionSafely("TICK", action, instance, null);
            }
        }
        
        metrics.recordTickTime(System.nanoTime() - startTime);
    }
    
    private boolean isPlayerNearby(Location loc, double radius) {
        if (loc.getWorld() == null) return false;
        if (!loc.getChunk().isLoaded()) return false;
        
        Collection<org.bukkit.entity.Entity> entities = loc.getWorld().getNearbyEntities(loc, radius, radius, radius, e -> e instanceof Player);
        return !entities.isEmpty();
    }

    public void executeActionSafely(String runtimePhase, Action action, MultiblockInstance instance, Player player) {
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
