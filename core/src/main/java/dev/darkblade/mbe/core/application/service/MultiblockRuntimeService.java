package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockAssemblyService;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockCapabilityInitializer;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockInstanceRegistry;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockTickingService;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockTypeRegistry;
import dev.darkblade.mbe.api.tick.Tickable;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockState;
import dev.darkblade.mbe.core.domain.MultiblockSource;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.infrastructure.persistence.InstanceStorageService;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade that provides backward compatibility for plugins relying on the legacy MultiblockRuntimeService.
 * @deprecated Use specific multiblock services (MultiblockTypeRegistry, MultiblockInstanceRegistry, etc.) instead.
 */
@Deprecated
public class MultiblockRuntimeService implements Tickable {
    
    private final MultiblockTypeRegistry typeRegistry;
    private final MultiblockInstanceRegistry instanceRegistry;
    private final MultiblockCapabilityInitializer capabilityInitializer;
    private final MultiblockAssemblyService assemblyService;
    private final MultiblockTickingService tickingService;
    private final HologramService holograms;
    private final MetricsService metrics;
    
    private AddonLifecycleService addonManager;
    private InstanceStorageService storage;
    private dev.darkblade.mbe.api.platform.PlatformService platformService;

    public MultiblockRuntimeService(MultiblockTypeRegistry typeRegistry,
                                    MultiblockInstanceRegistry instanceRegistry,
                                    MultiblockCapabilityInitializer capabilityInitializer,
                                    MultiblockAssemblyService assemblyService,
                                    MultiblockTickingService tickingService,
                                    HologramService holograms,
                                    MetricsService metrics) {
        this.typeRegistry = typeRegistry;
        this.instanceRegistry = instanceRegistry;
        this.capabilityInitializer = capabilityInitializer;
        this.assemblyService = assemblyService;
        this.tickingService = tickingService;
        this.holograms = holograms;
        this.metrics = metrics;
    }

    public void setStorage(InstanceStorageService storage) {
        this.storage = storage;
        this.assemblyService.setStorage(storage);
    }

    public void setPlatformService(dev.darkblade.mbe.api.platform.PlatformService platformService) {
        this.platformService = platformService;
        this.assemblyService.setPlatformService(platformService);
    }

    public void setAddonLifecycleService(AddonLifecycleService addonManager) {
        this.addonManager = addonManager;
        this.capabilityInitializer.setAddonManager(addonManager);
        this.tickingService.setAddonLifecycleService(addonManager);
    }

    public void registerType(MultiblockType type) {
        typeRegistry.registerType(type);
    }

    public void registerType(MultiblockType type, MultiblockSource source) {
        typeRegistry.registerType(type, source);
    }
    
    public Optional<MultiblockType> getType(String id) {
        return typeRegistry.getType(id);
    }

    public Optional<MultiblockSource> getSource(String typeId) {
        return typeRegistry.getSource(typeId);
    }
    
    public Collection<MultiblockType> getTypes() {
        return typeRegistry.getTypes();
    }

    public List<MultiblockType> getTypesDeterministic() {
        return typeRegistry.getTypesDeterministic();
    }
    
    public void unregisterAll() {
        holograms.removeAll();
        typeRegistry.unregisterAll();
        instanceRegistry.unregisterAll();
        capabilityInitializer.unregisterAll();
        metrics.reset();
    }
    
    public void reloadTypes(Collection<MultiblockType> newTypes) {
        typeRegistry.reloadTypesWithSources(newTypes, null);
    }

    public void reloadTypesWithSources(Collection<MultiblockType> newTypes, Map<String, MultiblockSource> sources) {
        typeRegistry.reloadTypesWithSources(newTypes, sources);
    }

    public String signatureOf(MultiblockType type) {
        return typeRegistry.signatureOf(type);
    }
    
    public MetricsService getMetrics() {
        return metrics;
    }

    @Override
    public void tick() {
        tickingService.tick();
    }
    
    public Optional<MultiblockInstance> tryCreate(Block anchor, MultiblockType type, Player player) {
        return assemblyService.tryCreate(anchor, type, player);
    }

    public List<MultiblockType> variantsForSignature(String signature) {
        return typeRegistry.variantsForSignature(signature);
    }

    public Optional<MultiblockInstance> switchVariant(MultiblockInstance current, Player player) {
        return assemblyService.switchVariant(current, player);
    }

    public void registerInstance(MultiblockInstance instance) {
        registerInstance(instance, true);
    }

    public void registerInstance(MultiblockInstance instance, boolean initializeCapabilities) {
        instanceRegistry.registerInstance(instance);
        if (initializeCapabilities) {
            capabilityInitializer.initializeCapabilitiesOnce(instance);
        }
    }

    public void initializePendingCapabilities() {
        capabilityInitializer.initializePendingCapabilities(instanceRegistry.getActiveInstancesSnapshot());
    }

    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        return instanceRegistry.getInstanceAt(loc);
    }

    public Collection<MultiblockInstance> getActiveInstancesSnapshot() {
        return instanceRegistry.getActiveInstancesSnapshot();
    }
    
    public void destroyInstance(MultiblockInstance instance) {
        instanceRegistry.destroyInstance(instance);
        capabilityInitializer.unregister(instance);
        holograms.removeHologram(instance);
        
        if (storage != null && instance.type().persistent()) {
            storage.deleteInstance(instance);
        }
        
        metrics.incrementDestroyed();
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
        return instanceRegistry.isInstanceActive(instance);
    }
}
