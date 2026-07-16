package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.addon.MultiblockAddon;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.AddonLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.event.ComponentAvailabilityEvent;
import dev.darkblade.mbe.api.event.ComponentChangeType;
import dev.darkblade.mbe.api.event.ComponentKind;
import dev.darkblade.mbe.core.application.service.addon.domain.DiscoveredAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonState;
import dev.darkblade.mbe.core.application.service.addon.domain.LoadedAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.ExposedService;
import dev.darkblade.mbe.core.application.service.addon.domain.PendingExposure;
import dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator;
import org.bukkit.Bukkit;
import java.util.HashSet;
import java.util.Arrays;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.concurrent.ConcurrentHashMap;

import dev.darkblade.mbe.api.command.MbeCommandService;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonRuntimeLifecycleService {
    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final CoreLogger log;
    private final AddonRegistry registry;
    private final AddonServiceRegistry serviceRegistry;
    private final ServiceLifecycleOrchestrator serviceLifecycleManager;
    private final AddonDataDirectorySystem dataDirectorySystem;
    private final AddonLifecycleService facade;

    private final Map<String, AddonLoggingSettings> addonLogging = new ConcurrentHashMap<>();
    private record AddonLoggingSettings(boolean suppressNonCritical, LogLevel minLevelWhenSuppressed) {}


    public AddonRuntimeLifecycleService(MultiBlockEngine plugin, MultiblockAPI api, CoreLogger log, AddonRegistry registry, AddonServiceRegistry serviceRegistry, ServiceLifecycleOrchestrator serviceLifecycleManager, AddonDataDirectorySystem dataDirectorySystem, AddonLifecycleService facade) {
        this.plugin = plugin;
        this.api = api;
        this.log = log;
        this.registry = registry;
        this.serviceRegistry = serviceRegistry;
        this.serviceLifecycleManager = serviceLifecycleManager;
        this.dataDirectorySystem = dataDirectorySystem;
        this.facade = facade;
    }

    
    private String missingRequiredEnabledDependencies(AddonMetadata meta) {
        for (String req : meta.requiredDependencies().keySet()) {
            if (registry.states.getOrDefault(req, AddonState.DISABLED) != AddonState.ENABLED) {
                return req;
            }
        }
        return null;
    }

    private static LogLevel parseLevel(String raw, LogLevel fallback) {
        if (raw == null) return fallback;
        try { return LogLevel.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT)); } 
        catch (IllegalArgumentException e) { return fallback; }
    }

    private CoreLogger coreLogger(LogPhase phase) {
        return log;
    }

    public void enableAddons() {
        CoreLogger core = coreLogger(LogPhase.ENABLE);
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CORE_SERVICES);
        serviceLifecycleManager.injectServices(AddonRegistry.CORE_PROVIDER_ID);
        serviceLifecycleManager.enableServices(AddonRegistry.CORE_PROVIDER_ID);

        for (String id : registry.resolvedOrder) {
            LoadedAddon loaded = registry.loadedAddons.get(id);
            if (loaded == null)
                continue;
            if (registry.states.getOrDefault(id, AddonState.DISABLED) != AddonState.LOADED)
                continue;
            serviceLifecycleManager.injectAddon(id, loaded.addon());
            serviceLifecycleManager.injectServices(id);
        }

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.ADDON_SERVICES);
        List<String> contentRegistrationOrder = new ArrayList<>();
        for (String id : registry.resolvedOrder) {
            LoadedAddon loaded = registry.loadedAddons.get(id);
            if (loaded == null)
                continue;
            if (registry.states.getOrDefault(id, AddonState.DISABLED) != AddonState.LOADED)
                continue;

            String missing = missingRequiredEnabledDependencies(loaded.metadata());
            if (missing != null) {
                core.error("Addon failed", LogKv.kv("id", id), LogKv.kv("reason", missing));
                registry.states.put(id, AddonState.FAILED);
                AddonLifecycleService.close(loaded.classLoader());
                continue;
            }

            loaded.phase().set(LogPhase.ENABLE);
            serviceLifecycleManager.enableServices(id);
            contentRegistrationOrder.add(id);
        }

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CONTENT_REGISTRATION);
        for (String id : contentRegistrationOrder) {
            LoadedAddon loaded = registry.loadedAddons.get(id);
            if (loaded == null) {
                continue;
            }
            try {
                loaded.addon().onEnable();

                List<PendingExposure> exposures = registry.pendingExposures.getOrDefault(id, List.of());
                boolean exposureFailed = false;
                for (PendingExposure exposure : exposures) {
                    try {
                        Object provider = BukkitServiceBridge.exposeProviderRaw(plugin, exposure.api(),
                                exposure.implementation(), exposure.priority());
                        trackExposedService(id, exposure.api(), provider);
                        log.logInternal(new LogScope.Addon(id, facade.addonVersion(id)), LogPhase.SERVICE_REGISTER,
                                LogLevel.INFO,
                                "Public service exposed",
                                null,
                                new LogKv[] {
                                        LogKv.kv("service", exposure.api().getName()),
                                        LogKv.kv("priority", exposure.priority().name())
                                },
                                Set.of());
                    } catch (Throwable t) {
                        facade.failAddon(id, AddonException.Phase.ENABLE,
                                "Failed to expose public service: " + exposure.api().getName(), t, true);
                        exposureFailed = true;
                        break;
                    }
                }

                registry.pendingExposures.remove(id);

                if (exposureFailed) {
                    unexposeAddonServices(id);
                    AddonLifecycleService.close(loaded.classLoader());
                    continue;
                }

                registry.states.put(id, AddonState.ENABLED);
                registry.enableOrder.addLast(id);
                loaded.logger().withPhase(LogPhase.ENABLE).info("Enabled");
            } catch (AddonException e) {
                facade.failAddon(id, AddonException.Phase.ENABLE, e.getMessage(), e.getCause(), e.isFatal());
                unexposeAddonServices(id);
                AddonLifecycleService.close(loaded.classLoader());
            } catch (Throwable t) {
                facade.failAddon(id, AddonException.Phase.ENABLE, "Unhandled exception during onEnable", t, true);
                unexposeAddonServices(id);
                AddonLifecycleService.close(loaded.classLoader());
            }
        }
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.RUNTIME);
    }

    public void disableAddons() {
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.ADDON_SERVICES);
        registry.pendingExposures.clear();
        while (!registry.enableOrder.isEmpty()) {
            String id = registry.enableOrder.removeLast();
            LoadedAddon loaded = registry.loadedAddons.get(id);
            if (loaded == null)
                continue;

            try {
                unexposeAddonServices(id);
                loaded.phase().set(LogPhase.DISABLE);
                loaded.addon().onDisable();
                serviceLifecycleManager.disableServices(id);
                loaded.logger().withPhase(LogPhase.DISABLE).info("Disabled");
            } catch (Throwable t) {
                facade.failAddon(id, AddonException.Phase.DISABLE, "Unhandled exception during onDisable", t, false);
            }

            registry.states.put(id, AddonState.DISABLED);
            AddonLifecycleService.close(loaded.classLoader());
        }

        for (String id : new HashSet<>(registry.loadedAddons.keySet())) {
            LoadedAddon loaded = registry.loadedAddons.remove(id);
            if (loaded != null) {
                AddonLifecycleService.close(loaded.classLoader());
            }
            registry.states.putIfAbsent(id, AddonState.DISABLED);
        }

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CORE_SERVICES);
        serviceLifecycleManager.disableServices(AddonRegistry.CORE_PROVIDER_ID);
        registry.exposedServices.clear();
        serviceLifecycleManager.clear();
    }

    public void loadAddon(DiscoveredAddon discovered) throws IOException {
        AddonMetadata metadata = discovered.metadata();
        String addonId = metadata.id();
        if (registry.states.getOrDefault(addonId, AddonState.DISABLED) == AddonState.FAILED) {
            return;
        }

        URL[] urls = { discovered.file().toURI().toURL() };
        List<AddonClassLoader> dependencyLoaders = dependencyClassLoaders(metadata);
        AddonClassLoader loader = new AddonClassLoader(addonId, urls, plugin.getClass().getClassLoader(),
                dependencyLoaders);
        AtomicReference<LogPhase> phaseRef = new AtomicReference<>(LogPhase.LOAD);
        AddonLogger addonLogger = log.forAddon(addonId, metadata.version().toString(), phaseRef::get);

        MultiblockAddon addon;
        try {
            Class<?> clazz = loader.loadClass(metadata.mainClass());
            if (!MultiblockAddon.class.isAssignableFrom(clazz)) {
                facade.failAddon(addonId, AddonException.Phase.LOAD,
                        "Main class does not implement MultiblockAddon (possible shaded api / classloader conflict): "
                                + metadata.mainClass(),
                        null, true);
                AddonLifecycleService.close(loader);
                return;
            }
            addon = (MultiblockAddon) clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            facade.failAddon(addonId, AddonException.Phase.LOAD,
                    "Failed to instantiate addon main class: " + metadata.mainClass(), t, true);
            AddonLifecycleService.close(loader);
            return;
        }

        String reportedId;
        try {
            reportedId = Objects.requireNonNull(addon.getId(), "addon.getId()");
        } catch (Throwable t) {
            facade.failAddon(addonId, AddonException.Phase.LOAD, "Addon getId() failed", t, true);
            AddonLifecycleService.close(loader);
            return;
        }

        if (!reportedId.equals(addonId)) {
            facade.failAddon(addonId, AddonException.Phase.LOAD,
                    "Addon id mismatch. addon.yml=" + addonId + " getId()=" + reportedId, null, true);
            AddonLifecycleService.close(loader);
            return;
        }

        String reportedVersion;
        try {
            reportedVersion = Objects.requireNonNull(addon.getVersion(), "addon.getVersion()");
        } catch (Throwable t) {
            facade.failAddon(addonId, AddonException.Phase.LOAD, "Addon getVersion() failed", t, true);
            AddonLifecycleService.close(loader);
            return;
        }

        if (!reportedVersion.trim().equals(metadata.version().raw())) {
            facade.failAddon(addonId, AddonException.Phase.LOAD, "Addon version mismatch. addon.yml="
                    + metadata.version().raw() + " getVersion()=" + reportedVersion.trim(), null, true);
            AddonLifecycleService.close(loader);
            return;
        }

        Path dataFolder;
        try {
            dataFolder = dataDirectorySystem.ensureAddonDataFolder(addonId);
        } catch (Exception e) {
            Path failedPath;
            try {
                String folderName = AddonDataDirectorySystem.normalizeAddonFolderName(addonId);
                failedPath = new File(plugin.getDataFolder(), "addons").toPath().resolve(folderName).normalize();
            } catch (Exception ignored) {
                failedPath = new File(plugin.getDataFolder(), "addons").toPath();
            }
            dataDirectorySystem.logFs(addonId, "LOAD", failedPath, e, "addon failed");
            facade.failAddon(addonId, AddonException.Phase.LOAD, "Failed to prepare addon data folder", e, true);
            AddonLifecycleService.close(loader);
            return;
        }

        ensureAddonConfigAndLogging(discovered.file(), addonId, dataFolder, addonLogger);

        SimpleAddonContext context = new SimpleAddonContext(
                addonId,
                plugin,
                api,
                addonLogger,
                dataFolder,
                facade,
                serviceRegistry,
                serviceLifecycleManager,
                loader);
        try {
            phaseRef.set(LogPhase.LOAD);
            addon.onLoad(context);
            serviceLifecycleManager.discoverAndRegister(addonId, addon);
        } catch (AddonException e) {
            facade.failAddon(addonId, AddonException.Phase.LOAD, e.getMessage(), e.getCause(), e.isFatal());
            unexposeAddonServices(addonId);
            AddonLifecycleService.close(loader);
            return;
        } catch (Throwable t) {
            facade.failAddon(addonId, AddonException.Phase.LOAD, "Unhandled exception during onLoad", t, true);
            unexposeAddonServices(addonId);
            AddonLifecycleService.close(loader);
            return;
        }

        registry.loadedAddons.put(addonId, new LoadedAddon(metadata, addon, loader, addonLogger, phaseRef, dataFolder));
        registry.states.put(addonId, AddonState.LOADED);
        addonLogger.withPhase(LogPhase.LOAD).info("Loaded", LogKv.kv("version", metadata.version().toString()));
    }

    private List<AddonClassLoader> dependencyClassLoaders(AddonMetadata metadata) {
        if (metadata == null || metadata.dependsIds() == null || metadata.dependsIds().isEmpty()) {
            return List.of();
        }

        List<AddonClassLoader> out = new ArrayList<>();
        for (String depId : metadata.dependsIds()) {
            LoadedAddon dep = registry.loadedAddons.get(depId);
            if (dep == null) {
                continue;
            }
            if (registry.states.getOrDefault(depId, AddonState.DISABLED) != AddonState.LOADED) {
                continue;
            }
            out.add(dep.classLoader());
        }
        return List.copyOf(out);
    }

    private void ensureAddonConfigAndLogging(File addonFile, String addonId, Path dataFolder, AddonLogger addonLogger) {
        String key = addonId == null ? "" : addonId.toLowerCase(java.util.Locale.ROOT);
        try {
            Path configPath = dataFolder.resolve("config.yml");
            byte[] addonDefaultConfig = null;
            try (JarFile jar = new JarFile(addonFile)) {
                JarEntry entry = jar.getJarEntry("config.yml");
                if (entry != null) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        addonDefaultConfig = in.readAllBytes();
                    }
                }
            }

            if (!Files.exists(configPath)) {
                if (addonDefaultConfig != null && addonDefaultConfig.length > 0) {
                    try (InputStream in = new ByteArrayInputStream(addonDefaultConfig)) {
                        Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else if (addonDefaultConfig != null && addonDefaultConfig.length > 0) {
                byte[] existing = Files.readAllBytes(configPath);
                byte[] coreDefault = null;
                try (InputStream in = plugin.getResource("config.yml")) {
                    if (in != null) {
                        coreDefault = in.readAllBytes();
                    }
                }
                if (coreDefault != null && Arrays.equals(existing, coreDefault)) {
                    try (InputStream in = new ByteArrayInputStream(addonDefaultConfig)) {
                        Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            if (Files.exists(configPath)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configPath.toFile());
                boolean suppressNonCritical = yaml.getBoolean("logging.suppressNonCritical",
                        yaml.getBoolean("logging.suppress", false));
                String minStr = yaml.getString("logging.minLevelWhenSuppressed",
                        yaml.getString("logging.minLevel", "ERROR"));
                LogLevel min = parseLevel(minStr, LogLevel.ERROR);
                addonLogging.put(key, new AddonLoggingSettings(suppressNonCritical, min));
            }
        } catch (Throwable t) {
            addonLogging.remove(key);
            if (addonLogger != null) {
                addonLogger.withPhase(LogPhase.LOAD).warn("Failed to load addon config.yml logging settings",
                        LogKv.kv("addon", addonId));
            }
        }
    }

    private void trackExposedService(String addonId, Class<?> api, Object provider) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(provider, "provider");

        registry.exposedServices.compute(addonId, (k, list) -> {
            List<ExposedService> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.add(new ExposedService(api, provider));
            return List.copyOf(next);
        });
    }

    void unexposeAddonServices(String addonId) {
        List<ExposedService> services = registry.exposedServices.remove(addonId);
        if (services == null || services.isEmpty()) {
            return;
        }

        for (ExposedService svc : services) {
            try {
                BukkitServiceBridge.unexpose(svc.api(), svc.provider());
                ComponentKind kind = MbeCommandService.class.isAssignableFrom(svc.api()) ? ComponentKind.COMMAND_SERVICE
                        : ComponentKind.SERVICE;
                publishComponentAvailability(addonId, svc.api().getName(), kind, ComponentChangeType.REMOVED);
            } catch (Throwable t) {
                log.logInternal(new LogScope.Addon(addonId, facade.addonVersion(addonId)), LogPhase.SERVICE_REGISTER,
                        LogLevel.WARN,
                        "Failed to unexpose public service",
                        t,
                        new LogKv[] { LogKv.kv("service", svc.api().getName()) },
                        Set.of());
            }
        }
    }

    void publishComponentAvailability(String addonId, String componentId, ComponentKind kind,
            ComponentChangeType changeType) {
        if (addonId == null || addonId.isBlank() || componentId == null || componentId.isBlank() || kind == null
                || changeType == null) {
            return;
        }
        dev.darkblade.mbe.api.event.EventBusService eventBus = facade.getCoreService(dev.darkblade.mbe.api.event.EventBusService.class);
        if (eventBus == null) {
            return;
        }
        eventBus.publish(new ComponentAvailabilityEvent(addonId, componentId, kind, changeType));
    }


}