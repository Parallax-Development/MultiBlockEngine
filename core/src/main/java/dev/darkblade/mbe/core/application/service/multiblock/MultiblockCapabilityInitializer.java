package dev.darkblade.mbe.core.application.service.multiblock;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiblockCapabilityInitializer {
    private final Set<Location> capabilitiesInitialized = ConcurrentHashMap.newKeySet();
    private AddonLifecycleService addonManager;

    public void setAddonManager(AddonLifecycleService addonManager) {
        this.addonManager = addonManager;
    }

    public void initializeCapabilitiesOnce(MultiblockInstance instance) {
        if (instance == null || instance.anchorLocation() == null) {
            return;
        }
        Location key = instance.anchorLocation();
        if (!capabilitiesInitialized.add(key)) {
            return;
        }
        initializeCapabilities(instance);
    }

    public void initializePendingCapabilities(Collection<MultiblockInstance> instances) {
        for (MultiblockInstance instance : instances) {
            initializeCapabilitiesOnce(instance);
        }
    }

    public void unregister(MultiblockInstance instance) {
        if (instance != null && instance.anchorLocation() != null) {
            capabilitiesInitialized.remove(instance.anchorLocation());
        }
    }

    public void unregisterAll() {
        capabilitiesInitialized.clear();
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
                    CoreLogger core = MultiBlockEngine.getInstance().getLoggingService() != null ? MultiBlockEngine.getInstance().getLoggingService().core() : null;
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
}
