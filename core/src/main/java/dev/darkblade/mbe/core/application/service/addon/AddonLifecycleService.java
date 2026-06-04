package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.api.event.ComponentChangeType;
import dev.darkblade.mbe.api.event.ComponentKind;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.core.application.service.addon.domain.DiscoveredAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonState;
import dev.darkblade.mbe.core.application.service.addon.domain.LoadedAddon;
import dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator;
import dev.darkblade.mbe.core.application.service.ServiceInjector;
import dev.darkblade.mbe.core.application.service.MBEServiceRegistry;
import dev.darkblade.mbe.core.application.service.DefaultUnifiedServiceRegistry;
import dev.darkblade.mbe.core.application.service.DefaultResolutionPolicy;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonLifecycleService {
    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final CoreLogger log;

    private final AddonRegistry registry;
    private final AddonServiceRegistry serviceRegistry;
    private final ServiceLifecycleOrchestrator serviceLifecycleManager;
    private final AddonDataDirectorySystem dataDirectorySystem;

    private final AddonAuditService auditService;
    private final AddonDependencyResolver dependencyResolver;
    private final AddonDiscoveryService discoveryService;
    private final AddonRuntimeLifecycleService runtimeService;
    private final DefaultUnifiedServiceRegistry unifiedRegistry;

    public AddonLifecycleService(MultiBlockEngine plugin, MultiblockAPI api, CoreLogger log) {
        this.plugin = plugin;
        this.api = api;
        this.log = log;

        this.registry = new AddonRegistry();
        this.dataDirectorySystem = new AddonDataDirectorySystem(this.log, new java.io.File(plugin.getDataFolder(), "addons").toPath());

        this.unifiedRegistry = new DefaultUnifiedServiceRegistry(new DefaultResolutionPolicy());
        this.serviceRegistry = new AddonServiceRegistry(this.unifiedRegistry, this.log);
        MBEServiceRegistry mbeServiceRegistry = new MBEServiceRegistry(this.unifiedRegistry);
        ServiceInjector serviceInjector = new ServiceInjector(this.unifiedRegistry, this.log,
                (ownerId, type) -> unifiedRegistry.resolveService(type, null, null));

        this.serviceLifecycleManager = new ServiceLifecycleOrchestrator(this.unifiedRegistry, serviceInjector, this.log);

        File addonFolder = new File(plugin.getDataFolder(), "addons");
        this.auditService = new AddonAuditService();
        this.dependencyResolver = new AddonDependencyResolver();

        this.discoveryService = new AddonDiscoveryService(plugin, log, registry, auditService, dependencyResolver, addonFolder, dataDirectorySystem);
        this.runtimeService = new AddonRuntimeLifecycleService(plugin, api, log, registry, serviceRegistry, serviceLifecycleManager, dataDirectorySystem, this);
    }

    public void loadAddons() {
        discoveryService.loadAddons();
    }

    public void enableAddons() {
        runtimeService.enableAddons();
    }

    public void disableAddons() {
        runtimeService.disableAddons();
    }

    public AddonState getState(String addonId) {
        if (AddonRegistry.CORE_PROVIDER_ID.equals(addonId)) {
            return AddonState.ENABLED;
        }
        return registry.states.getOrDefault(addonId, AddonState.DISABLED);
    }

    public <T> void registerCoreService(Class<T> type, T provider) {
        unifiedRegistry.registerService(new dev.darkblade.mbe.core.application.service.DefaultServiceDescriptor<>(
                type.getName(),
                AddonRegistry.CORE_PROVIDER_ID,
                type,
                provider,
                dev.darkblade.mbe.api.service.ServiceScope.GLOBAL,
                0,
                false,
                true
        ));
    }

    public <T> void registerAddonTypedService(String addonId, Class<T> type, T provider) {
        unifiedRegistry.registerService(new dev.darkblade.mbe.core.application.service.DefaultServiceDescriptor<>(
                type.getName(),
                addonId,
                type,
                provider,
                dev.darkblade.mbe.api.service.ServiceScope.GLOBAL,
                0,
                false,
                true
        ));
    }

    public <T> void queueServiceExposure(String addonId, Class<T> api, T provider, org.bukkit.plugin.ServicePriority priority) {
        registry.pendingExposures.computeIfAbsent(addonId, k -> new ArrayList<>()).add(new dev.darkblade.mbe.core.application.service.addon.domain.PendingExposure(api, provider, priority));
    }

    public <T> java.util.List<T> getServicesByType(Class<T> type) {
        return serviceLifecycleManager.getByType(type);
    }

    public ServiceLifecycleOrchestrator.LifecyclePhase getCurrentLifecyclePhase() {
        return serviceLifecycleManager.getCurrentPhase();
    }

    public void registerCoreMbeService(MBEService service) {
        serviceLifecycleManager.registerService(AddonRegistry.CORE_PROVIDER_ID, service);
        runtimeService.publishComponentAvailability(
                AddonRegistry.CORE_PROVIDER_ID,
                service.getServiceId(),
                service instanceof MbeCommandService ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED);
    }

    public void registerAddonMbeService(String addonId, MBEService service) {
        runtimeService.publishComponentAvailability(
                addonId,
                service.getServiceId(),
                service instanceof MbeCommandService ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED);
    }

    public <T> T getCoreService(Class<T> serviceType) {
        List<T> dynamic = serviceLifecycleManager.getByType(serviceType);
        if (!dynamic.isEmpty()) {
            return dynamic.get(0);
        }
        return serviceRegistry.resolveIfEnabled(AddonRegistry.CORE_PROVIDER_ID, serviceType, this::getState).orElse(null);
    }

    public <T> T getService(Class<T> serviceType) {
        return getCoreService(serviceType);
    }

    public record AddonRuntime(String id, ClassLoader classLoader, Path dataFolder) {}

    public List<AddonRuntime> listLoadedAddons() {
        try {
            List<AddonRuntime> out = new ArrayList<>();
            List<String> ids = new ArrayList<>(registry.loadedAddons.keySet());
            ids.sort(String::compareToIgnoreCase);
            for (String id : ids) {
                LoadedAddon loaded = registry.loadedAddons.get(id);
                if (loaded == null) {
                    continue;
                }
                out.add(new AddonRuntime(id, loaded.classLoader(), loaded.dataFolder()));
            }
            return List.copyOf(out);
        } catch (Throwable t) {
            return List.of();
        }
    }

    public List<AddonInfo> getAddonInfoList() {
        List<AddonInfo> result = new ArrayList<>();
        List<String> allIds = new ArrayList<>(registry.states.keySet());
        allIds.sort(String::compareToIgnoreCase);
        java.util.Map<String, List<String>> serviceMap = serviceLifecycleManager.getServiceIdsByAddon();
        for (String id : allIds) {
            if (AddonRegistry.CORE_PROVIDER_ID.equals(id)) {
                continue;
            }
            AddonState state = registry.states.getOrDefault(id, AddonState.DISABLED);
            String version = addonVersion(id);
            List<String> deps = List.of();
            DiscoveredAddon discovered = registry.discoveredAddons.get(id);
            if (discovered != null && discovered.metadata().dependsIds() != null) {
                deps = List.copyOf(discovered.metadata().dependsIds());
            }
            List<String> serviceIds = serviceMap.getOrDefault(id, List.of());
            result.add(new AddonInfo(id, version, state, serviceIds, deps));
        }
        return List.copyOf(result);
    }

    public Optional<AddonInfo> getAddonInfo(String addonId) {
        if (addonId == null || addonId.isBlank()) {
            return Optional.empty();
        }
        String key = addonId.trim();
        AddonState state = registry.states.get(key);
        if (state == null) {
            return Optional.empty();
        }
        if (AddonRegistry.CORE_PROVIDER_ID.equals(key)) {
            java.util.Map<String, List<String>> serviceMap = serviceLifecycleManager.getServiceIdsByAddon();
            List<String> coreServices = serviceMap.getOrDefault(AddonRegistry.CORE_PROVIDER_ID, List.of());
            return Optional.of(new AddonInfo(AddonRegistry.CORE_PROVIDER_ID, "core", state, coreServices, List.of()));
        }
        String version = addonVersion(key);
        List<String> deps = List.of();
        DiscoveredAddon discovered = registry.discoveredAddons.get(key);
        if (discovered != null && discovered.metadata().dependsIds() != null) {
            deps = List.copyOf(discovered.metadata().dependsIds());
        }
        java.util.Map<String, List<String>> serviceMap = serviceLifecycleManager.getServiceIdsByAddon();
        List<String> serviceIds = serviceMap.getOrDefault(key, List.of());
        return Optional.of(new AddonInfo(key, version, state, serviceIds, deps));
    }

    public void failAddon(String addonId, AddonException.Phase phase, String message, Throwable cause, boolean fatal) {
        if (addonId == null || addonId.isBlank()) {
            addonId = "unknown";
        }

        LogPhase logPhase = phaseToLogPhase(phase);
        LogLevel level = fatal ? LogLevel.FATAL : LogLevel.ERROR;
        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), logPhase, level, message, cause,
                new LogKv[] {
                        LogKv.kv("addonId", addonId),
                        LogKv.kv("phase", phase == null ? "" : phase.name()),
                        LogKv.kv("fatal", fatal)
                }, Set.of());

        boolean markFailed = fatal || phase == AddonException.Phase.LOAD || phase == AddonException.Phase.ENABLE;
        if (markFailed) {
            registry.states.put(addonId, AddonState.FAILED);
        }

        LoadedAddon loaded = registry.loadedAddons.get(addonId);
        if (fatal && loaded != null) {
            try {
                runtimeService.unexposeAddonServices(addonId);
                loaded.phase().set(LogPhase.DISABLE);
                loaded.addon().onDisable();
                serviceLifecycleManager.disableServices(addonId);
            } catch (Throwable t) {
                log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.DISABLE, LogLevel.ERROR,
                        "Error during disable after failure", t, null, Set.of());
            }
        }
    }

    String addonVersion(String addonId) {
        if (AddonRegistry.CORE_PROVIDER_ID.equals(addonId)) {
            return plugin.getDescription().getVersion();
        }
        DiscoveredAddon d = registry.discoveredAddons.get(addonId);
        if (d != null) {
            return d.metadata().version().toString();
        }
        return "unknown";
    }

    private static LogPhase phaseToLogPhase(AddonException.Phase phase) {
        if (phase == null) return LogPhase.LOAD;
        return switch (phase) {
            case LOAD -> LogPhase.LOAD;
            case ENABLE -> LogPhase.ENABLE;
            case DISABLE -> LogPhase.DISABLE;
            default -> LogPhase.LOAD;
        };
    }

    static void close(URLClassLoader loader) {
        try {
            if (loader != null) {
                loader.close();
            }
        } catch (IOException ignored) {}
    }

    public ServiceLifecycleOrchestrator getServiceLifecycleOrchestrator() {
        return serviceLifecycleManager;
    }
}
