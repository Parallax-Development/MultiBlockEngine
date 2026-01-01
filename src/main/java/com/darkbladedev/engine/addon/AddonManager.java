package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.addon.AddonException;
import com.darkbladedev.engine.api.addon.MultiblockAddon;
import com.darkbladedev.engine.api.addon.Version;
import com.darkbladedev.engine.api.logging.AddonLogger;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.EngineLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

public class AddonManager {

    public enum AddonState {
        DISCOVERED,
        LOADED,
        ENABLED,
        FAILED,
        DISABLED
    }

    private record DiscoveredAddon(File file, AddonMetadata metadata) {}

    private record LoadedAddon(
        AddonMetadata metadata,
        MultiblockAddon addon,
        URLClassLoader classLoader,
        AddonLogger logger,
        AtomicReference<LogPhase> phase,
        Path dataFolder
    ) {}

    private record PendingExposure(Class<?> api, Object implementation, ServicePriority priority) {}

    private record ExposedService(Class<?> api, Object provider) {}

    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final CoreLogger log;
    private final File addonFolder;
    private final AddonDataDirectorySystem dataDirectorySystem;
    private final AddonServiceRegistry serviceRegistry;
    private final AddonDependencyResolver dependencyResolver;
    private final Map<String, DiscoveredAddon> discoveredAddons = new HashMap<>();
    private final Map<String, LoadedAddon> loadedAddons = new HashMap<>();
    private final Map<String, AddonState> states = new HashMap<>();
    private final ArrayDeque<String> enableOrder = new ArrayDeque<>();
    private List<String> resolvedOrder = List.of();
    private final Map<String, List<PendingExposure>> pendingExposures = new ConcurrentHashMap<>();
    private final Map<String, List<ExposedService>> exposedServices = new ConcurrentHashMap<>();
    private final ClassLoader coreApiClassLoader = MultiblockAPI.class.getClassLoader();

    public AddonManager(MultiBlockEngine plugin, MultiblockAPI api, CoreLogger log) {
        this.plugin = plugin;
        this.api = api;
        this.log = Objects.requireNonNull(log, "log");
        this.addonFolder = new File(plugin.getDataFolder(), "addons");
        this.dataDirectorySystem = new AddonDataDirectorySystem(this.log, this.addonFolder.toPath());
        this.serviceRegistry = new AddonServiceRegistry(this.log);
        this.dependencyResolver = new AddonDependencyResolver();
    }

    public void loadAddons() {
        EngineLogger core = coreLogger(LogPhase.LOAD);

        if (!addonFolder.exists()) {
            addonFolder.mkdirs();
        }

        try {
            dataDirectorySystem.ensureRootDirectory();
        } catch (Exception e) {
            core.fatal("Failed to initialize addons root folder", e, LogKv.kv("path", addonFolder.getAbsolutePath()));
            return;
        }

        File[] files = addonFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<String, List<DiscoveredAddon>> candidates = new HashMap<>();

        for (File file : files) {
            try {
                AddonMetadata metadata = readMetadata(file);
                if (metadata == null) {
                    continue;
                }

                candidates.computeIfAbsent(metadata.id(), k -> new ArrayList<>()).add(new DiscoveredAddon(file, metadata));
            } catch (Exception e) {
                core.error("Failed to load addon from file", e, LogKv.kv("file", file.getName()));
            }
        }

        discoveredAddons.clear();
        loadedAddons.clear();
        states.clear();
        enableOrder.clear();
        pendingExposures.clear();
        exposedServices.clear();
        resolvedOrder = List.of();

        for (Map.Entry<String, List<DiscoveredAddon>> entry : candidates.entrySet()) {
            String id = entry.getKey();
            List<DiscoveredAddon> list = entry.getValue();
            if (list.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (DiscoveredAddon d : list) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(d.file().getName()).append("@").append(d.metadata().version());
                }
                core.error("Addon failed: multiple versions detected", LogKv.kv("id", id), LogKv.kv("duplicates", sb.toString()));
                states.put(id, AddonState.FAILED);
                continue;
            }

            DiscoveredAddon discovered = list.get(0);
            discoveredAddons.put(id, discovered);
            states.put(id, AddonState.DISCOVERED);
        }

        Map<String, AddonMetadata> metadataById = new HashMap<>();
        for (Map.Entry<String, DiscoveredAddon> e : discoveredAddons.entrySet()) {
            metadataById.put(e.getKey(), e.getValue().metadata());
        }

        AddonDependencyResolver.Resolution resolution = dependencyResolver.resolve(MultiBlockEngine.getApiVersion(), metadataById);

        core.info("Addon dependency resolution complete",
            LogKv.kv("candidates", metadataById.size()),
            LogKv.kv("eligible", resolution.loadOrder().size()),
            LogKv.kv("failed", resolution.failures().size()),
            LogKv.kv("warnings", resolution.warnings().size())
        );

        for (String warning : resolution.warnings()) {
            core.warn("Addon dependency warning", LogKv.kv("warning", warning));
        }
        for (Map.Entry<String, String> fail : resolution.failures().entrySet()) {
            states.put(fail.getKey(), AddonState.FAILED);
            core.error("Addon dependency failure", LogKv.kv("id", fail.getKey()), LogKv.kv("reason", fail.getValue()));
        }

        resolvedOrder = resolution.loadOrder();

        if (!resolvedOrder.isEmpty()) {
            core.info("Addon load order", LogKv.kv("order", joinLimited(resolvedOrder, 30)), LogKv.kv("count", resolvedOrder.size()));
        }

        for (String id : resolvedOrder) {
            if (states.getOrDefault(id, AddonState.DISABLED) == AddonState.FAILED) {
                continue;
            }

            DiscoveredAddon discovered = discoveredAddons.get(id);
            if (discovered == null) {
                continue;
            }

            try {
                loadAddon(discovered);
            } catch (Exception e) {
                failAddon(id, AddonException.Phase.LOAD, "Unhandled exception during addon load", e, true);
            }
        }
    }

    public AddonState getState(String addonId) {
        return states.getOrDefault(addonId, AddonState.DISABLED);
    }

    public <T> void queueServiceExposure(String addonId, Class<T> api, T implementation, ServicePriority priority) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(priority, "priority");

        validateCoreApiType(addonId, api);

        pendingExposures.compute(addonId, (k, list) -> {
            List<PendingExposure> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.add(new PendingExposure(api, implementation, priority));
            return List.copyOf(next);
        });

        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.SERVICE_REGISTER, LogLevel.DEBUG,
            "Service exposure queued", null,
            new LogKv[] { LogKv.kv("service", api.getName()), LogKv.kv("priority", priority.name()) },
            Set.of()
        );
    }

    private void validateCoreApiType(String addonId, Class<?> apiType) {
        ClassLoader cl = apiType.getClassLoader();
        if (cl == coreApiClassLoader) {
            return;
        }

        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.LOAD, LogLevel.FATAL,
            "Invalid service exposure: service type is not part of core-api",
            null,
            new LogKv[] {
                LogKv.kv("service", apiType.getName()),
                LogKv.kv("serviceCl", cl == null ? "bootstrap" : cl.toString()),
                LogKv.kv("coreApiCl", coreApiClassLoader == null ? "bootstrap" : coreApiClassLoader.toString())
            },
            Set.of()
        );

        throw new IllegalArgumentException(
            "Invalid service exposure: Service type " + apiType.getName() + " is not part of core-api. " +
                "Move the service interface/DTOs to core-api and depend on it as compileOnly."
        );
    }

    public void failAddon(String addonId, AddonException.Phase phase, String message, Throwable cause, boolean fatal) {
        if (addonId == null || addonId.isBlank()) {
            addonId = "unknown";
        }

        LogPhase logPhase = phaseToLogPhase(phase);
        LogLevel level = fatal ? LogLevel.FATAL : LogLevel.ERROR;
        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), logPhase, level, message, cause, new LogKv[] {
            LogKv.kv("addonId", addonId),
            LogKv.kv("phase", phase == null ? "" : phase.name()),
            LogKv.kv("fatal", fatal)
        }, Set.of());

        boolean markFailed = fatal || phase == AddonException.Phase.LOAD || phase == AddonException.Phase.ENABLE;
        if (markFailed) {
            states.put(addonId, AddonState.FAILED);
        }

        LoadedAddon loaded = loadedAddons.get(addonId);
        if (fatal && loaded != null) {
            try {
                unexposeAddonServices(addonId);
                loaded.phase().set(LogPhase.DISABLE);
                loaded.addon().onDisable();
            } catch (Throwable t) {
                log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.DISABLE, LogLevel.ERROR, "Error during disable after failure", t, null, Set.of());
            }
        }
    }

    private void loadAddon(DiscoveredAddon discovered) throws IOException {
        AddonMetadata metadata = discovered.metadata();
        String addonId = metadata.id();
        if (states.getOrDefault(addonId, AddonState.DISABLED) == AddonState.FAILED) {
            return;
        }

        URL[] urls = {discovered.file().toURI().toURL()};
        URLClassLoader loader = new URLClassLoader(urls, plugin.getClass().getClassLoader());
        AtomicReference<LogPhase> phaseRef = new AtomicReference<>(LogPhase.LOAD);
        AddonLogger addonLogger = log.forAddon(addonId, metadata.version().toString(), phaseRef::get);

        MultiblockAddon addon;
        try {
            Class<?> clazz = loader.loadClass(metadata.mainClass());
            if (!MultiblockAddon.class.isAssignableFrom(clazz)) {
                failAddon(addonId, AddonException.Phase.LOAD, "Main class does not implement MultiblockAddon: " + metadata.mainClass(), null, true);
                close(loader);
                return;
            }
            addon = (MultiblockAddon) clazz.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            failAddon(addonId, AddonException.Phase.LOAD, "Failed to instantiate addon main class: " + metadata.mainClass(), t, true);
            close(loader);
            return;
        }

        String reportedId;
        try {
            reportedId = Objects.requireNonNull(addon.getId(), "addon.getId()");
        } catch (Throwable t) {
            failAddon(addonId, AddonException.Phase.LOAD, "Addon getId() failed", t, true);
            close(loader);
            return;
        }

        if (!reportedId.equals(addonId)) {
            failAddon(addonId, AddonException.Phase.LOAD, "Addon id mismatch. addon.properties=" + addonId + " getId()=" + reportedId, null, true);
            close(loader);
            return;
        }

        String reportedVersion;
        try {
            reportedVersion = Objects.requireNonNull(addon.getVersion(), "addon.getVersion()");
        } catch (Throwable t) {
            failAddon(addonId, AddonException.Phase.LOAD, "Addon getVersion() failed", t, true);
            close(loader);
            return;
        }

        if (!reportedVersion.trim().equals(metadata.version().raw())) {
            failAddon(addonId, AddonException.Phase.LOAD, "Addon version mismatch. addon.properties=" + metadata.version().raw() + " getVersion()=" + reportedVersion.trim(), null, true);
            close(loader);
            return;
        }

        Path dataFolder;
        try {
            dataFolder = dataDirectorySystem.ensureAddonDataFolder(addonId);
        } catch (Exception e) {
            Path failedPath;
            try {
                String folderName = AddonDataDirectorySystem.normalizeAddonFolderName(addonId);
                failedPath = addonFolder.toPath().resolve(folderName).normalize();
            } catch (Exception ignored) {
                failedPath = addonFolder.toPath();
            }
            dataDirectorySystem.logFs(addonId, "LOAD", failedPath, e, "addon failed");
            failAddon(addonId, AddonException.Phase.LOAD, "Failed to prepare addon data folder", e, true);
            close(loader);
            return;
        }

        SimpleAddonContext context = new SimpleAddonContext(addonId, plugin, api, addonLogger, dataFolder, this, serviceRegistry);
        try {
            phaseRef.set(LogPhase.LOAD);
            addon.onLoad(context);
        } catch (AddonException e) {
            failAddon(addonId, AddonException.Phase.LOAD, e.getMessage(), e.getCause(), e.isFatal());
            close(loader);
            return;
        } catch (Throwable t) {
            failAddon(addonId, AddonException.Phase.LOAD, "Unhandled exception during onLoad", t, true);
            close(loader);
            return;
        }

        loadedAddons.put(addonId, new LoadedAddon(metadata, addon, loader, addonLogger, phaseRef, dataFolder));
        states.put(addonId, AddonState.LOADED);
        addonLogger.withPhase(LogPhase.LOAD).info("Loaded", LogKv.kv("version", metadata.version().toString()));
    }

    private AddonMetadata readMetadata(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("addon.properties");
            if (entry == null) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: missing addon.properties", LogKv.kv("file", file.getName()));
                return null;
            }

            Properties props = new Properties();
            try (InputStream in = jar.getInputStream(entry)) {
                props.load(in);
            }

            String id = trimToNull(props.getProperty("id"));
            String versionStr = trimToNull(props.getProperty("version"));
            String main = trimToNull(props.getProperty("main"));

            if (main == null) {
                Attributes attributes = jar.getManifest() != null ? jar.getManifest().getMainAttributes() : null;
                if (attributes != null) {
                    main = trimToNull(attributes.getValue("Multiblock-Addon-Main"));
                }
            }

            if (id == null || versionStr == null || main == null) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: addon.properties requires id, version, main", LogKv.kv("file", file.getName()));
                return null;
            }

            if (!id.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: invalid id", LogKv.kv("file", file.getName()), LogKv.kv("id", id));
                return null;
            }

            Version version;
            try {
                version = Version.parse(versionStr);
            } catch (IllegalArgumentException e) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: invalid version", LogKv.kv("file", file.getName()), LogKv.kv("version", versionStr));
                return null;
            }

            int apiVersion;
            String apiStr = trimToNull(props.getProperty("api"));
            if (apiStr == null) {
                apiStr = trimToNull(props.getProperty("apiVersion"));
            }

            if (apiStr == null) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: missing api", LogKv.kv("file", file.getName()));
                return null;
            }

            try {
                apiVersion = Integer.parseInt(apiStr);
            } catch (NumberFormatException ignored) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: invalid api", LogKv.kv("file", file.getName()), LogKv.kv("api", apiStr));
                return null;
            }

            Map<String, Version> required = parseDependencyMap(id, trimToNull(props.getProperty("depends.required")), file.getName());
            Map<String, Version> optional = parseDependencyMap(id, trimToNull(props.getProperty("depends.optional")), file.getName());

            String legacy = trimToNull(props.getProperty("depends"));
            if (legacy != null && required.isEmpty()) {
                Version min = Version.parse("0.0.0");
                Map<String, Version> legacyReq = new HashMap<>();
                for (String part : legacy.split("[,; ]+")) {
                    String dep = trimToNull(part);
                    if (dep == null) continue;
                    if (!dep.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
                        coreLogger(LogPhase.LOAD).warn("Invalid legacy depends entry", LogKv.kv("owner", id), LogKv.kv("dep", dep));
                        continue;
                    }
                    if (dep.equals(id)) {
                        coreLogger(LogPhase.LOAD).warn("Ignoring self-dependency", LogKv.kv("owner", id));
                        continue;
                    }
                    legacyReq.put(dep, min);
                }
                required = Map.copyOf(legacyReq);
            }

            if (!required.isEmpty() && !optional.isEmpty()) {
                for (String depId : required.keySet()) {
                    if (optional.containsKey(depId)) {
                        coreLogger(LogPhase.LOAD).warn("Optional dependency overridden by required", LogKv.kv("owner", id), LogKv.kv("dep", depId));
                    }
                }
                Map<String, Version> filteredOpt = new HashMap<>(optional);
                filteredOpt.keySet().removeAll(required.keySet());
                optional = Map.copyOf(filteredOpt);
            }

            List<String> dependsIds = new ArrayList<>(required.keySet());
            dependsIds.addAll(optional.keySet());
            dependsIds = List.copyOf(dependsIds);

            return new AddonMetadata(id, version, apiVersion, main, required, optional, dependsIds);
        }
    }

    private Map<String, Version> parseDependencyMap(String ownerId, String raw, String fileName) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }

        Map<String, Version> map = new HashMap<>();
        for (String part : raw.split("[,; ]+")) {
            String token = trimToNull(part);
            if (token == null) continue;

            int idx = token.indexOf(">=");
            if (idx < 1 || idx + 2 >= token.length()) {
                throw new IllegalArgumentException("Invalid addon.properties in " + ownerId + ": Invalid dependency format: " + token + " (expected <id>>=<version>)");
            }

            String depId = trimToNull(token.substring(0, idx));
            String verStr = trimToNull(token.substring(idx + 2));

            if (depId == null || verStr == null) {
                throw new IllegalArgumentException("Invalid addon.properties in " + ownerId + ": Invalid dependency format: " + token + " (expected <id>>=<version>)");
            }

            if (!depId.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
                throw new IllegalArgumentException("Invalid addon.properties in " + ownerId + ": Invalid dependency id: " + depId);
            }

            if (depId.equals(ownerId)) {
                throw new IllegalArgumentException("Invalid addon.properties in " + ownerId + ": Self dependency not allowed");
            }

            Version min;
            try {
                min = Version.parse(verStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid addon.properties in " + ownerId + ": Invalid dependency version: " + depId + ">=" + verStr);
            }

            if (map.containsKey(depId)) {
                coreLogger(LogPhase.LOAD).warn("Duplicate dependency (last one wins)", LogKv.kv("owner", ownerId), LogKv.kv("dep", depId), LogKv.kv("file", fileName));
            }
            map.put(depId, min);
        }

        return Map.copyOf(map);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void close(URLClassLoader loader) {
        try {
            loader.close();
        } catch (IOException ignored) {
        }
    }

    public void enableAddons() {
        EngineLogger core = coreLogger(LogPhase.ENABLE);

        for (String id : resolvedOrder) {
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;
            if (states.getOrDefault(id, AddonState.DISABLED) != AddonState.LOADED) continue;

            String missing = missingRequiredEnabledDependencies(loaded.metadata());
            if (missing != null) {
                core.error("Addon failed", LogKv.kv("id", id), LogKv.kv("reason", missing));
                states.put(id, AddonState.FAILED);
                close(loaded.classLoader());
                continue;
            }

            try {
                loaded.phase().set(LogPhase.ENABLE);
                loaded.addon().onEnable();

                List<PendingExposure> exposures = pendingExposures.getOrDefault(id, List.of());
                boolean exposureFailed = false;
                for (PendingExposure exposure : exposures) {
                    try {
                        Object provider = BukkitServiceBridge.exposeProviderRaw(plugin, exposure.api(), exposure.implementation(), exposure.priority());
                        trackExposedService(id, exposure.api(), provider);
                        log.logInternal(new LogScope.Addon(id, addonVersion(id)), LogPhase.SERVICE_REGISTER, LogLevel.INFO,
                            "Public service exposed",
                            null,
                            new LogKv[] {
                                LogKv.kv("service", exposure.api().getName()),
                                LogKv.kv("priority", exposure.priority().name())
                            },
                            Set.of()
                        );
                    } catch (Throwable t) {
                        failAddon(id, AddonException.Phase.ENABLE, "Failed to expose public service: " + exposure.api().getName(), t, true);
                        exposureFailed = true;
                        break;
                    }
                }

                pendingExposures.remove(id);

                if (exposureFailed) {
                    unexposeAddonServices(id);
                    close(loaded.classLoader());
                    continue;
                }

                states.put(id, AddonState.ENABLED);
                enableOrder.addLast(id);
                loaded.logger().withPhase(LogPhase.ENABLE).info("Enabled");
            } catch (AddonException e) {
                failAddon(id, AddonException.Phase.ENABLE, e.getMessage(), e.getCause(), e.isFatal());
                unexposeAddonServices(id);
                close(loaded.classLoader());
            } catch (Throwable t) {
                failAddon(id, AddonException.Phase.ENABLE, "Unhandled exception during onEnable", t, true);
                unexposeAddonServices(id);
                close(loaded.classLoader());
            }
        }
    }

    public void disableAddons() {
        pendingExposures.clear();
        while (!enableOrder.isEmpty()) {
            String id = enableOrder.removeLast();
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;

            try {
                unexposeAddonServices(id);
                loaded.phase().set(LogPhase.DISABLE);
                loaded.addon().onDisable();
                loaded.logger().withPhase(LogPhase.DISABLE).info("Disabled");
            } catch (Throwable t) {
                failAddon(id, AddonException.Phase.DISABLE, "Unhandled exception during onDisable", t, false);
            }

            states.put(id, AddonState.DISABLED);
            close(loaded.classLoader());
        }

        for (String id : new HashSet<>(loadedAddons.keySet())) {
            LoadedAddon loaded = loadedAddons.remove(id);
            if (loaded != null) {
                close(loaded.classLoader());
            }
            states.putIfAbsent(id, AddonState.DISABLED);
        }

        exposedServices.clear();
    }

    private void trackExposedService(String addonId, Class<?> api, Object provider) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(provider, "provider");

        exposedServices.compute(addonId, (k, list) -> {
            List<ExposedService> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.add(new ExposedService(api, provider));
            return List.copyOf(next);
        });
    }

    private void unexposeAddonServices(String addonId) {
        List<ExposedService> services = exposedServices.remove(addonId);
        if (services == null || services.isEmpty()) {
            return;
        }

        for (ExposedService svc : services) {
            try {
                BukkitServiceBridge.unexpose(svc.api(), svc.provider());
            } catch (Throwable t) {
                log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.SERVICE_REGISTER, LogLevel.WARN,
                    "Failed to unexpose public service",
                    t,
                    new LogKv[] { LogKv.kv("service", svc.api().getName()) },
                    Set.of()
                );
            }
        }
    }

    private EngineLogger coreLogger(LogPhase phase) {
        return (level, message, throwable, fields) -> log.logInternal(new LogScope.Core(), phase, level, message, throwable, fields, Set.of());
    }

    private String addonVersion(String addonId) {
        LoadedAddon loaded = loadedAddons.get(addonId);
        if (loaded != null) {
            return loaded.metadata().version().toString();
        }
        DiscoveredAddon discovered = discoveredAddons.get(addonId);
        if (discovered != null) {
            return discovered.metadata().version().toString();
        }
        return "unknown";
    }

    private static String joinLimited(List<String> ids, int limit) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }

        int max = Math.max(0, limit);
        int shown = Math.min(max, ids.size());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(ids.get(i));
        }

        int remaining = ids.size() - shown;
        if (remaining > 0) {
            sb.append(" … +").append(remaining);
        }

        return sb.toString();
    }

    private static LogPhase phaseToLogPhase(AddonException.Phase phase) {
        if (phase == null) return LogPhase.RUNTIME;
        return switch (phase) {
            case LOAD -> LogPhase.LOAD;
            case ENABLE -> LogPhase.ENABLE;
            case DISABLE -> LogPhase.DISABLE;
            default -> LogPhase.RUNTIME;
        };
    }

    private String missingRequiredEnabledDependencies(AddonMetadata meta) {
        List<String> missing = new ArrayList<>();

        for (String depId : meta.requiredDependencies().keySet()) {
            if (states.getOrDefault(depId, AddonState.DISABLED) != AddonState.ENABLED) {
                Version min = meta.requiredDependencies().get(depId);
                missing.add(depId + " >=" + min);
            }
        }

        if (missing.isEmpty()) {
            return null;
        }

        return "Missing required dependency " + String.join(", ", missing);
    }

}
