package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.application.service.addon.domain.AddonState;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonLoggingSettings;
import dev.darkblade.mbe.core.application.service.addon.domain.DiscoveredAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.ExposedService;
import dev.darkblade.mbe.core.application.service.addon.domain.LoadedAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.PendingExposure;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AddonRegistry {
    public static final String CORE_PROVIDER_ID = "mbe:core";
    
    public final ConcurrentHashMap<String, AddonLoggingSettings> addonLogging = new ConcurrentHashMap<>();
    public final Map<String, DiscoveredAddon> discoveredAddons = new HashMap<>();
    public final Map<String, LoadedAddon> loadedAddons = new HashMap<>();
    public final Map<String, AddonState> states = new HashMap<>();
    public final ArrayDeque<String> enableOrder = new ArrayDeque<>();
    public List<String> resolvedOrder = List.of();
    public final Map<String, List<PendingExposure>> pendingExposures = new ConcurrentHashMap<>();
    public final Map<String, List<ExposedService>> exposedServices = new ConcurrentHashMap<>();
    
    public void clearForReload() {
        discoveredAddons.clear();
        loadedAddons.clear();
        states.clear();
        states.put(CORE_PROVIDER_ID, AddonState.ENABLED);
        enableOrder.clear();
        pendingExposures.clear();
        exposedServices.clear();
        resolvedOrder = List.of();
    }
}
