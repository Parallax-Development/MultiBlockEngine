package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.MultiblockAPI;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.api.addon.MultiblockAddon;
import dev.darkblade.mbe.api.addon.Version;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMetrics;
import dev.darkblade.mbe.api.logging.AddonLogger;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.EngineLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.event.ComponentAvailabilityEvent;
import dev.darkblade.mbe.api.event.ComponentChangeType;
import dev.darkblade.mbe.api.event.ComponentKind;
import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.core.application.service.addon.crossref.AddonCrossReferenceService;
import dev.darkblade.mbe.core.application.service.MBEServiceRegistry;
import dev.darkblade.mbe.core.application.service.ServiceInjector;
import dev.darkblade.mbe.core.application.service.ServiceLifecycleOrchestrator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

public class AddonLifecycleService {

    private static final String CORE_PROVIDER_ID = "mbe:core";
    private static final Version MIN_DEPENDENCY_VERSION = Version.parse("0.0.0");
    private static final String ADDON_API_DESCRIPTOR = "Ldev/darkblade/mbe/api/addon/AddonAPI;";
    private static final String ATTR_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    private static final String ATTR_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";

    private enum ContractsEnforcementMode {
        WARN,
        ERROR
    }

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
        AddonClassLoader classLoader,
        AddonLogger logger,
        AtomicReference<LogPhase> phase,
        Path dataFolder
    ) {}

    private record PendingExposure(Class<?> api, Object implementation, ServicePriority priority) {}

    private record ExposedService(Class<?> api, Object provider) {}

    private record AddonAuditIndex(
        String addonId,
        String fileName,
        String mainClass,
        String rootPrefixInternal,
        List<String> classEntries,
        Set<String> classInternalNames,
        Set<String> apiClasses,
        Set<String> apiContractClasses,
        Set<String> embeddedCoreApiClasses,
        Set<String> embeddedJars
    ) {}

    private record AddonAuditReport(
        String addonId,
        String fileName,
        Set<String> sharedApis,
        Set<String> declaredAddonRefs,
        Set<String> undeclaredAddonRefs,
        Set<String> nonApiAccessRefs,
        Set<String> embeddedJars,
        List<String> violations,
        boolean fatal
    ) {}

    private record AddonReferenceHit(String targetAddonId, String ownerClass, String targetClass) {}
    private record ClassFileInspection(String ownerInternalName, Set<String> referencedClassNames, Set<String> classAnnotationDescriptors) {}

    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final CoreLogger log;
    private final File addonFolder;
    private final AddonDataDirectorySystem dataDirectorySystem;
    private final AddonServiceRegistry serviceRegistry;
    private final ServiceLifecycleOrchestrator serviceLifecycleManager;
    private final AddonCrossReferenceService crossReferenceManager;
    private final AddonDependencyResolver dependencyResolver;
    private final ConcurrentHashMap<String, AddonLoggingSettings> addonLogging = new ConcurrentHashMap<>();
    private final Map<String, DiscoveredAddon> discoveredAddons = new HashMap<>();
    private final Map<String, LoadedAddon> loadedAddons = new HashMap<>();
    private final Map<String, AddonState> states = new HashMap<>();
    private final ArrayDeque<String> enableOrder = new ArrayDeque<>();
    private List<String> resolvedOrder = List.of();
    private final Map<String, List<PendingExposure>> pendingExposures = new ConcurrentHashMap<>();
    private final Map<String, List<ExposedService>> exposedServices = new ConcurrentHashMap<>();
    private final ClassLoader apiClassLoader = MultiblockAPI.class.getClassLoader();
    private volatile AddonServiceRegistry.ApiTypeEnforcementMode serviceApiTypeMode = AddonServiceRegistry.ApiTypeEnforcementMode.ERROR;

    public AddonLifecycleService(MultiBlockEngine plugin, MultiblockAPI api, CoreLogger log) {
        this.plugin = plugin;
        this.api = api;
        this.log = Objects.requireNonNull(log, "log");
        this.addonFolder = new File(plugin.getDataFolder(), "addons");
        this.dataDirectorySystem = new AddonDataDirectorySystem(this.log, this.addonFolder.toPath());
        this.serviceRegistry = new AddonServiceRegistry(this.log);
        MBEServiceRegistry mbeServiceRegistry = new MBEServiceRegistry();
        this.crossReferenceManager = new AddonCrossReferenceService();
        ServiceInjector serviceInjector = new ServiceInjector(mbeServiceRegistry, this.crossReferenceManager, this.log, this::resolveExternalService);
        this.serviceLifecycleManager = new ServiceLifecycleOrchestrator(mbeServiceRegistry, serviceInjector, this.log);
        this.dependencyResolver = new AddonDependencyResolver();

        this.log.setGate((scope, level) -> {
            if (!(scope instanceof LogScope.Addon addon)) {
                return true;
            }
            String id = addon.addonId();
            String key = id == null ? "" : id.toLowerCase(java.util.Locale.ROOT);
            AddonLoggingSettings settings = addonLogging.get(key);
            if (settings == null || !settings.suppressNonCritical()) {
                return true;
            }
            return level.ordinal() >= settings.minLevelWhenSuppressed().ordinal();
        });
    }

    private record AddonLoggingSettings(boolean suppressNonCritical, LogLevel minLevelWhenSuppressed) {
        private AddonLoggingSettings {
            suppressNonCritical = suppressNonCritical;
            minLevelWhenSuppressed = minLevelWhenSuppressed == null ? LogLevel.ERROR : minLevelWhenSuppressed;
        }
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

        Map<File, AddonAuditIndex> auditIndexesByFile = new HashMap<>();

        for (File file : files) {
            try {
                AddonMetadata metadata = readMetadata(file);
                if (metadata == null) {
                    continue;
                }

                AddonAuditIndex auditIndex = buildAuditIndex(file, metadata);
                if (auditIndex != null) {
                    auditIndexesByFile.put(file, auditIndex);
                }

                candidates.computeIfAbsent(metadata.id(), k -> new ArrayList<>()).add(new DiscoveredAddon(file, metadata));
            } catch (Exception e) {
                core.error("Failed to load addon from file", e, LogKv.kv("file", file.getName()));
            }
        }

        discoveredAddons.clear();
        loadedAddons.clear();
        states.clear();
        states.put(CORE_PROVIDER_ID, AddonState.ENABLED);
        enableOrder.clear();
        pendingExposures.clear();
        exposedServices.clear();
        serviceLifecycleManager.clear();
        crossReferenceManager.clear();
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

        ContractsEnforcementMode contractsMode = contractsEnforcementMode();
        boolean strictContracts = contractsMode == ContractsEnforcementMode.ERROR;
        this.serviceApiTypeMode = serviceApiTypeEnforcementModeFromConfig();
        serviceRegistry.setApiTypeEnforcementMode(this.serviceApiTypeMode);
        core.info("Addon contracts enforcement mode", LogKv.kv("mode", contractsMode.name()));
        core.info("Addon service api type enforcement mode", LogKv.kv("mode", this.serviceApiTypeMode.name()));

        Map<String, AddonAuditReport> auditReports = auditDiscoveredAddons(discoveredAddons, auditIndexesByFile, strictContracts);
        for (AddonAuditReport report : auditReports.values()) {
            if (!report.violations().isEmpty()) {
                String violations = joinLimited(report.violations(), 10);
                if (report.fatal()) {
                    states.put(report.addonId(), AddonState.FAILED);
                    core.error("Addon failed audit", LogKv.kv("id", report.addonId()), LogKv.kv("file", report.fileName()), LogKv.kv("violations", violations));
                } else {
                    core.warn("Addon audit warnings", LogKv.kv("id", report.addonId()), LogKv.kv("file", report.fileName()), LogKv.kv("violations", violations));
                }
            }

            if (!report.sharedApis().isEmpty()) {
                core.warn("Addon shared APIs detected", LogKv.kv("id", report.addonId()), LogKv.kv("shared", joinLimited(new ArrayList<>(report.sharedApis()), 30)), LogKv.kv("count", report.sharedApis().size()));
            }

            if (!report.declaredAddonRefs().isEmpty()) {
                core.info("Addon cross-addon references validated", LogKv.kv("id", report.addonId()), LogKv.kv("refs", joinLimited(new ArrayList<>(report.declaredAddonRefs()), 30)), LogKv.kv("count", report.declaredAddonRefs().size()));
            }

            if (!report.undeclaredAddonRefs().isEmpty()) {
                core.warn("Addon undeclared cross-addon references detected", LogKv.kv("id", report.addonId()), LogKv.kv("refs", joinLimited(new ArrayList<>(report.undeclaredAddonRefs()), 30)), LogKv.kv("count", report.undeclaredAddonRefs().size()));
            }

            if (!report.nonApiAccessRefs().isEmpty()) {
                core.warn("Addon cross-addon internal access detected", LogKv.kv("id", report.addonId()), LogKv.kv("refs", joinLimited(new ArrayList<>(report.nonApiAccessRefs()), 30)), LogKv.kv("count", report.nonApiAccessRefs().size()));
            }

            if (!report.embeddedJars().isEmpty()) {
                core.warn("Addon embeds nested jar libraries", LogKv.kv("id", report.addonId()), LogKv.kv("jars", joinLimited(new ArrayList<>(report.embeddedJars()), 20)), LogKv.kv("count", report.embeddedJars().size()));
            }
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

        AddonCrossReferenceService.CompilationReport crossReferenceCompilation = crossReferenceManager.compileAndInitialize();
        CrossReferenceMetrics crossReferenceMetrics = crossReferenceCompilation.metrics();
        core.info("Addon cross-reference compile complete",
            LogKv.kv("declared", crossReferenceMetrics.declaredReferences()),
            LogKv.kv("initialized", crossReferenceMetrics.initializedReferences()),
            LogKv.kv("failedReferences", crossReferenceMetrics.failedReferences()),
            LogKv.kv("compileMillis", crossReferenceMetrics.compileNanos() / 1_000_000L),
            LogKv.kv("initializeMillis", crossReferenceMetrics.initializeNanos() / 1_000_000L),
            LogKv.kv("totalMillis", crossReferenceMetrics.totalNanos() / 1_000_000L)
        );

        for (Map.Entry<String, List<String>> failure : crossReferenceCompilation.failuresByAddon().entrySet()) {
            String addonId = failure.getKey();
            String reason = joinLimited(failure.getValue(), 10);
            states.put(addonId, AddonState.FAILED);
            core.error("Addon cross-reference failure", LogKv.kv("id", addonId), LogKv.kv("reason", reason));
        }
    }

    public AddonState getState(String addonId) {
        if (CORE_PROVIDER_ID.equals(addonId)) {
            return AddonState.ENABLED;
        }
        return states.getOrDefault(addonId, AddonState.DISABLED);
    }

    public <T> void registerCoreService(Class<T> serviceType, T service) {
        serviceRegistry.register(CORE_PROVIDER_ID, serviceType, service);
        publishComponentAvailability(
                CORE_PROVIDER_ID,
                buildTypedServiceId(CORE_PROVIDER_ID, serviceType),
                MbeCommandService.class.isAssignableFrom(serviceType) ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED
        );
    }

    public void registerCoreMbeService(MBEService service) {
        serviceLifecycleManager.registerService(CORE_PROVIDER_ID, service);
        publishComponentAvailability(
                CORE_PROVIDER_ID,
                service.getServiceId(),
                service instanceof MbeCommandService ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED
        );
    }

    public <T> T getCoreService(Class<T> serviceType) {
        List<T> dynamic = serviceLifecycleManager.getByType(serviceType);
        if (!dynamic.isEmpty()) {
            return dynamic.get(0);
        }
        return serviceRegistry.resolveIfEnabled(CORE_PROVIDER_ID, serviceType, this::getState).orElse(null);
    }

    public <T> T getService(Class<T> serviceType) {
        return getCoreService(serviceType);
    }

    public <T> List<T> getServicesByType(Class<T> serviceType) {
        return serviceLifecycleManager.getByType(serviceType);
    }

    public <T> void registerAddonTypedService(String addonId, Class<T> serviceType, T service) {
        publishComponentAvailability(
                addonId,
                buildTypedServiceId(addonId, serviceType),
                MbeCommandService.class.isAssignableFrom(serviceType) ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED
        );
    }

    public void registerAddonMbeService(String addonId, MBEService service) {
        publishComponentAvailability(
                addonId,
                service.getServiceId(),
                service instanceof MbeCommandService ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE,
                ComponentChangeType.ADDED
        );
    }

    public ServiceLifecycleOrchestrator.LifecyclePhase getCurrentLifecyclePhase() {
        return serviceLifecycleManager.getCurrentPhase();
    }

    private Optional<?> resolveExternalService(String ownerId, Class<?> serviceType) {
        if (serviceType == null) {
            return Optional.empty();
        }
        String owner = trimToNull(ownerId);
        if (owner == null) {
            owner = CORE_PROVIDER_ID;
        }
        Optional<?> resolved = resolveExternalServiceByType(owner, serviceType);
        if (resolved.isPresent() || CORE_PROVIDER_ID.equals(owner)) {
            return resolved;
        }
        return resolveExternalServiceByType(CORE_PROVIDER_ID, serviceType);
    }

    private <T> Optional<T> resolveExternalServiceByType(String ownerId, Class<T> serviceType) {
        List<T> dynamic = serviceLifecycleManager.getByType(serviceType);
        if (!dynamic.isEmpty()) {
            return Optional.of(dynamic.get(0));
        }
        return serviceRegistry.resolveIfEnabled(ownerId, serviceType, this::getState);
    }

    private static String buildTypedServiceId(String ownerAddonId, Class<?> serviceType) {
        String owner = ownerAddonId.trim().toLowerCase(java.util.Locale.ROOT);
        String typeName = serviceType.getName().toLowerCase(java.util.Locale.ROOT).replace('$', '.');
        return owner + ":typed." + typeName;
    }

    public record AddonRuntime(String id, ClassLoader classLoader, Path dataFolder) {
        public AddonRuntime {
            if (id == null || id.isBlank()) {
                id = "unknown";
            }
            if (classLoader == null) {
                classLoader = AddonLifecycleService.class.getClassLoader();
            }
            if (dataFolder == null) {
                dataFolder = Path.of(".").toAbsolutePath().normalize();
            }
        }
    }

    public List<AddonRuntime> listLoadedAddons() {
        try {
            List<AddonRuntime> out = new ArrayList<>();
            List<String> ids = new ArrayList<>(loadedAddons.keySet());
            ids.sort(String::compareToIgnoreCase);
            for (String id : ids) {
                LoadedAddon loaded = loadedAddons.get(id);
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

    public <T> void queueServiceExposure(String addonId, Class<T> api, T implementation, ServicePriority priority) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(priority, "priority");

        validateApiType(addonId, api);

        Object provider;
        try {
            provider = BukkitServiceBridge.exposeProviderRaw(plugin, api, implementation, priority);
        } catch (Throwable t) {
            log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.SERVICE_REGISTER, LogLevel.ERROR,
                "Failed to expose public service",
                t,
                new LogKv[] {
                    LogKv.kv("service", api.getName()),
                    LogKv.kv("priority", priority.name())
                },
                Set.of()
            );
            if (t instanceof RuntimeException rt) {
                throw rt;
            }
            throw new RuntimeException(t);
        }

        trackExposedService(addonId, api, provider);

        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.SERVICE_REGISTER, LogLevel.INFO,
            "Public service exposed",
            null,
            new LogKv[] {
                LogKv.kv("service", api.getName()),
                LogKv.kv("priority", priority.name())
            },
            Set.of()
        );
    }

    private void validateApiType(String addonId, Class<?> apiType) {
        ClassLoader cl = apiType.getClassLoader();
        if (cl == apiClassLoader) {
            return;
        }

        AddonServiceRegistry.ApiTypeEnforcementMode mode = this.serviceApiTypeMode;
        LogLevel level = mode == AddonServiceRegistry.ApiTypeEnforcementMode.ERROR ? LogLevel.FATAL : LogLevel.WARN;
        log.logInternal(new LogScope.Addon(addonId, addonVersion(addonId)), LogPhase.LOAD, level,
            "Service exposure type is not part of api",
            null,
            new LogKv[] {
                LogKv.kv("mode", mode.name()),
                LogKv.kv("service", apiType.getName()),
                LogKv.kv("serviceCl", cl == null ? "bootstrap" : cl.toString()),
                LogKv.kv("apiCl", apiClassLoader == null ? "bootstrap" : apiClassLoader.toString())
            },
            Set.of()
        );

        if (mode == AddonServiceRegistry.ApiTypeEnforcementMode.ERROR) {
            throw new IllegalArgumentException(
                "Invalid service exposure: Service type " + apiType.getName() + " is not part of api. " +
                    "Move the service interface/DTOs to api and depend on it as compileOnly."
            );
        }
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
                serviceLifecycleManager.disableServices(addonId);
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
        List<AddonClassLoader> dependencyLoaders = dependencyClassLoaders(metadata);
        AddonClassLoader loader = new AddonClassLoader(addonId, urls, plugin.getClass().getClassLoader(), dependencyLoaders);
        AtomicReference<LogPhase> phaseRef = new AtomicReference<>(LogPhase.LOAD);
        AddonLogger addonLogger = log.forAddon(addonId, metadata.version().toString(), phaseRef::get);

        MultiblockAddon addon;
        try {
            Class<?> clazz = loader.loadClass(metadata.mainClass());
            if (!MultiblockAddon.class.isAssignableFrom(clazz)) {
                failAddon(addonId, AddonException.Phase.LOAD, "Main class does not implement MultiblockAddon (possible shaded api / classloader conflict): " + metadata.mainClass(), null, true);
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
            failAddon(addonId, AddonException.Phase.LOAD, "Addon id mismatch. addon.yml=" + addonId + " getId()=" + reportedId, null, true);
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
            failAddon(addonId, AddonException.Phase.LOAD, "Addon version mismatch. addon.yml=" + metadata.version().raw() + " getVersion()=" + reportedVersion.trim(), null, true);
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

        ensureAddonConfigAndLogging(discovered.file(), addonId, dataFolder, addonLogger);

        SimpleAddonContext context = new SimpleAddonContext(
                addonId,
                plugin,
                api,
                addonLogger,
                dataFolder,
                this,
                serviceRegistry,
                serviceLifecycleManager,
                crossReferenceManager
        );
        try {
            phaseRef.set(LogPhase.LOAD);
            addon.onLoad(context);
            serviceLifecycleManager.discoverAndRegister(addonId, addon);
        } catch (AddonException e) {
            failAddon(addonId, AddonException.Phase.LOAD, e.getMessage(), e.getCause(), e.isFatal());
            unexposeAddonServices(addonId);
            close(loader);
            return;
        } catch (Throwable t) {
            failAddon(addonId, AddonException.Phase.LOAD, "Unhandled exception during onLoad", t, true);
            unexposeAddonServices(addonId);
            close(loader);
            return;
        }

        loadedAddons.put(addonId, new LoadedAddon(metadata, addon, loader, addonLogger, phaseRef, dataFolder));
        states.put(addonId, AddonState.LOADED);
        addonLogger.withPhase(LogPhase.LOAD).info("Loaded", LogKv.kv("version", metadata.version().toString()));
    }

    private List<AddonClassLoader> dependencyClassLoaders(AddonMetadata metadata) {
        if (metadata == null || metadata.dependsIds() == null || metadata.dependsIds().isEmpty()) {
            return List.of();
        }

        List<AddonClassLoader> out = new ArrayList<>();
        for (String depId : metadata.dependsIds()) {
            LoadedAddon dep = loadedAddons.get(depId);
            if (dep == null) {
                continue;
            }
            if (states.getOrDefault(depId, AddonState.DISABLED) != AddonState.LOADED) {
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
                boolean suppressNonCritical = yaml.getBoolean("logging.suppressNonCritical", yaml.getBoolean("logging.suppress", false));
                String minStr = yaml.getString("logging.minLevelWhenSuppressed", yaml.getString("logging.minLevel", "ERROR"));
                LogLevel min = parseLevel(minStr, LogLevel.ERROR);
                addonLogging.put(key, new AddonLoggingSettings(suppressNonCritical, min));
            }
        } catch (Throwable t) {
            addonLogging.remove(key);
            if (addonLogger != null) {
                addonLogger.withPhase(LogPhase.LOAD).warn("Failed to load addon config.yml logging settings", LogKv.kv("addon", addonId));
            }
        }
    }

    private static LogLevel parseLevel(String raw, LogLevel fallback) {
        if (raw == null) {
            return fallback;
        }
        String t = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (t.isEmpty()) {
            return fallback;
        }
        try {
            return LogLevel.valueOf(t);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private AddonMetadata readMetadata(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("addon.yml");
            if (entry == null) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: missing addon.yml", LogKv.kv("file", file.getName()));
                return null;
            }

            YamlConfiguration yaml;
            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                yaml = YamlConfiguration.loadConfiguration(reader);
            }

            String id = trimToNull(yaml.getString("id"));
            String versionStr = trimToNull(yaml.getString("version"));
            String main = trimToNull(yaml.getString("main"));

            if (main == null) {
                Attributes attributes = jar.getManifest() != null ? jar.getManifest().getMainAttributes() : null;
                if (attributes != null) {
                    main = trimToNull(attributes.getValue("Multiblock-Addon-Main"));
                }
            }

            if (id == null || versionStr == null || main == null) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: addon.yml requires id, version, main", LogKv.kv("file", file.getName()));
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

            int apiVersion = parseAddonApiVersion(yaml, file.getName());

            Map<String, Version> required = parseYamlDependencies(yaml.get("dependencies"), id, file.getName(), "dependencies");
            Map<String, Version> optional = parseYamlDependencies(yaml.get("soft-dependencies"), id, file.getName(), "soft-dependencies");

            if (!required.isEmpty() && !optional.isEmpty()) {
                for (String depId : required.keySet()) {
                    if (optional.containsKey(depId)) {
                        coreLogger(LogPhase.LOAD).warn("Optional dependency overridden by required", LogKv.kv("owner", id), LogKv.kv("dep", depId));
                    }
                }
                Map<String, Version> filteredOpt = new LinkedHashMap<>(optional);
                filteredOpt.keySet().removeAll(required.keySet());
                optional = filteredOpt;
            }

            List<String> dependsIds = new ArrayList<>(required.keySet());
            dependsIds.addAll(optional.keySet());
            dependsIds = List.copyOf(dependsIds);

            return new AddonMetadata(id, version, apiVersion, main, Map.copyOf(required), Map.copyOf(optional), dependsIds);
        }
    }

    private AddonAuditIndex buildAuditIndex(File file, AddonMetadata metadata) {
        try (JarFile jar = new JarFile(file)) {
            List<String> classEntries = new ArrayList<>();
            Set<String> classInternalNames = new LinkedHashSet<>();
            Set<String> apiClasses = new LinkedHashSet<>();
            Set<String> apiContractClasses = new LinkedHashSet<>();
            Set<String> embeddedCoreApiClasses = new LinkedHashSet<>();
            Set<String> embeddedJars = new LinkedHashSet<>();

            String rootPrefixInternal = rootPrefixInternal(metadata.mainClass());

            jar.stream().forEach(entry -> {
                String name = entry.getName();
                if (name.endsWith(".jar") && !name.startsWith("META-INF/")) {
                    embeddedJars.add(name);
                }
                if (!name.endsWith(".class")) {
                    return;
                }
                if (name.startsWith("META-INF/")) {
                    return;
                }

                classEntries.add(name);
                String internalName = name.substring(0, name.length() - ".class".length());
                classInternalNames.add(internalName);
                String fqcn = name.substring(0, name.length() - ".class".length()).replace('/', '.');

                if (fqcn.contains(".api.")) {
                    apiClasses.add(fqcn);
                }

                if (name.startsWith("com/darkbladedev/engine/api/") || name.startsWith("com/darkbladedev/engine/model/")) {
                    embeddedCoreApiClasses.add(fqcn);
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    ClassFileInspection inspection = inspectClassFile(in);
                    if (inspection.classAnnotationDescriptors().contains(ADDON_API_DESCRIPTOR)) {
                        apiContractClasses.add(fqcn);
                    }
                } catch (Exception ignored) {
                }
            });

            return new AddonAuditIndex(
                metadata.id(),
                file.getName(),
                metadata.mainClass(),
                rootPrefixInternal,
                List.copyOf(classEntries),
                Set.copyOf(classInternalNames),
                Set.copyOf(apiClasses),
                Set.copyOf(apiContractClasses),
                Set.copyOf(embeddedCoreApiClasses),
                Set.copyOf(embeddedJars)
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, AddonAuditReport> auditDiscoveredAddons(Map<String, DiscoveredAddon> discovered, Map<File, AddonAuditIndex> byFile, boolean strictContracts) {
        Map<String, AddonAuditIndex> indexes = new LinkedHashMap<>();
        for (DiscoveredAddon d : discovered.values()) {
            AddonAuditIndex idx = byFile.get(d.file());
            if (idx != null) {
                indexes.put(d.metadata().id(), idx);
            }
        }

        Map<String, List<String>> apiOwners = new LinkedHashMap<>();
        for (AddonAuditIndex idx : indexes.values()) {
            for (String api : idx.apiClasses()) {
                apiOwners.computeIfAbsent(api, k -> new ArrayList<>()).add(idx.addonId());
            }
        }

        Map<String, Set<String>> sharedApisByAddon = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : apiOwners.entrySet()) {
            if (e.getValue().size() <= 1) continue;
            for (String owner : e.getValue()) {
                sharedApisByAddon.computeIfAbsent(owner, k -> new LinkedHashSet<>()).add(e.getKey());
            }
        }

        Map<String, Set<String>> declaredRefsByAddon = new LinkedHashMap<>();
        Map<String, Set<String>> undeclaredRefsByAddon = new LinkedHashMap<>();
        Map<String, Set<String>> nonApiRefsByAddon = new LinkedHashMap<>();
        Map<String, String> classOwnerByInternal = new LinkedHashMap<>();
        Map<String, Set<String>> apiContractsByAddon = new LinkedHashMap<>();
        for (AddonAuditIndex idx : indexes.values()) {
            for (String classInternal : idx.classInternalNames()) {
                classOwnerByInternal.put(classInternal, idx.addonId());
            }
            Set<String> apiContracts = new LinkedHashSet<>();
            for (String fqcn : idx.apiContractClasses()) {
                apiContracts.add(fqcn.replace('.', '/'));
            }
            apiContractsByAddon.put(idx.addonId(), Set.copyOf(apiContracts));
        }

        for (AddonAuditIndex idx : indexes.values()) {
            DiscoveredAddon discoveredAddon = discovered.get(idx.addonId());
            if (discoveredAddon == null) continue;
            Set<AddonReferenceHit> refs = scanCrossAddonReferences(discoveredAddon.file(), idx.classEntries(), classOwnerByInternal, idx.addonId());
            if (refs.isEmpty()) {
                continue;
            }

            Set<String> declaredDeps = new LinkedHashSet<>(discoveredAddon.metadata().dependsIds());
            Set<String> declaredRefs = new LinkedHashSet<>();
            Set<String> undeclaredRefs = new LinkedHashSet<>();
            Set<String> nonApiRefs = new LinkedHashSet<>();
            for (AddonReferenceHit ref : refs) {
                String entry = ref.targetAddonId() + "::" + ref.ownerClass() + " -> " + ref.targetClass();
                if (declaredDeps.contains(ref.targetAddonId())) {
                    declaredRefs.add(entry);
                    Set<String> apiContracts = apiContractsByAddon.getOrDefault(ref.targetAddonId(), Set.of());
                    if (!apiContracts.contains(ref.targetClass().replace('.', '/'))) {
                        nonApiRefs.add(entry);
                    }
                } else {
                    undeclaredRefs.add(entry);
                }
            }

            if (!declaredRefs.isEmpty()) {
                declaredRefsByAddon.put(idx.addonId(), Set.copyOf(declaredRefs));
            }
            if (!undeclaredRefs.isEmpty()) {
                undeclaredRefsByAddon.put(idx.addonId(), Set.copyOf(undeclaredRefs));
            }
            if (!nonApiRefs.isEmpty()) {
                nonApiRefsByAddon.put(idx.addonId(), Set.copyOf(nonApiRefs));
            }
        }

        Map<String, AddonAuditReport> out = new LinkedHashMap<>();
        for (AddonAuditIndex idx : indexes.values()) {
            List<String> violations = new ArrayList<>();
            boolean fatal = false;

            if (!idx.embeddedCoreApiClasses().isEmpty()) {
                violations.add("embeds api classes (shaded/relocated api)");
                fatal = true;
            }

            if (!idx.embeddedJars().isEmpty()) {
                violations.add("embeds nested jar libraries inside addon");
                fatal = true;
            }

            Set<String> shared = sharedApisByAddon.getOrDefault(idx.addonId(), Set.of());
            if (!shared.isEmpty()) {
                violations.add("shared API definitions duplicated across addons");
                fatal = true;
            }

            Set<String> declaredRefs = declaredRefsByAddon.getOrDefault(idx.addonId(), Set.of());
            Set<String> undeclaredRefs = undeclaredRefsByAddon.getOrDefault(idx.addonId(), Set.of());
            Set<String> nonApiRefs = nonApiRefsByAddon.getOrDefault(idx.addonId(), Set.of());
            if (!undeclaredRefs.isEmpty()) {
                List<String> undeclaredTargets = new ArrayList<>();
                for (String ref : undeclaredRefs) {
                    int sep = ref.indexOf("::");
                    String target = sep < 0 ? ref : ref.substring(0, sep);
                    if (!undeclaredTargets.contains(target)) {
                        undeclaredTargets.add(target);
                    }
                }
                violations.add("references classes from addons without declared dependency: " + joinLimited(undeclaredTargets, 10));
                fatal = true;
            }

            if (!nonApiRefs.isEmpty()) {
                if (strictContracts) {
                    violations.add("references non-@AddonAPI classes from dependencies (strict mode)");
                } else {
                    violations.add("references non-@AddonAPI classes from dependencies (compat mode warning)");
                }
                if (strictContracts) {
                    fatal = true;
                }
            }

            out.put(idx.addonId(), new AddonAuditReport(idx.addonId(), idx.fileName(), shared, declaredRefs, undeclaredRefs, nonApiRefs, idx.embeddedJars(), List.copyOf(violations), fatal));
        }

        return out;
    }

    private Set<AddonReferenceHit> scanCrossAddonReferences(File addonJar, List<String> classEntries, Map<String, String> classOwnerByInternal, String selfId) {
        if (classEntries == null || classEntries.isEmpty()) {
            return Set.of();
        }
        if (classOwnerByInternal == null || classOwnerByInternal.isEmpty()) {
            return Set.of();
        }

        Set<AddonReferenceHit> hits = new LinkedHashSet<>();
        try (JarFile jar = new JarFile(addonJar)) {
            for (String entryName : classEntries) {
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) continue;
                try (InputStream in = jar.getInputStream(entry)) {
                    Set<AddonReferenceHit> foundInClass = scanClassReferences(in, classOwnerByInternal, selfId);
                    if (!foundInClass.isEmpty()) {
                        hits.addAll(foundInClass);
                        if (hits.size() >= 20) {
                            return Set.copyOf(hits);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return Set.of();
        }

        return Set.copyOf(hits);
    }

    private Set<AddonReferenceHit> scanClassReferences(InputStream rawIn, Map<String, String> classOwnerByInternal, String selfId) throws IOException {
        ClassFileInspection inspection = inspectClassFile(rawIn);
        if (inspection.ownerInternalName() == null || inspection.ownerInternalName().isBlank()) {
            return Set.of();
        }
        String ownerClass = inspection.ownerInternalName().replace('/', '.');
        Set<AddonReferenceHit> hits = new LinkedHashSet<>();
        for (String refInternal : inspection.referencedClassNames()) {
            String normalized = normalizeInternalClassName(refInternal);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            String targetAddon = classOwnerByInternal.get(normalized);
            if (targetAddon == null || targetAddon.equals(selfId)) {
                continue;
            }
            hits.add(new AddonReferenceHit(targetAddon, ownerClass, normalized.replace('/', '.')));
        }
        return Set.copyOf(hits);
    }

    private static String rootPrefixInternal(String mainClass) {
        if (mainClass == null) {
            return "";
        }
        String pkg;
        int idx = mainClass.lastIndexOf('.');
        pkg = idx < 0 ? "" : mainClass.substring(0, idx);

        String[] parts = pkg.split("\\.");
        int take = Math.min(parts.length, 4);
        if (take <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < take; i++) {
            if (i > 0) sb.append('/');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private ContractsEnforcementMode contractsEnforcementMode() {
        String raw = null;
        try {
            raw = trimToNull(plugin.getConfig().getString("addons.contracts.nonApiAccess"));
            if (raw == null) {
                raw = trimToNull(plugin.getConfig().getString("addons.audit.nonApiAccess"));
            }
        } catch (Throwable ignored) {
            raw = null;
        }
        if (raw == null) {
            return ContractsEnforcementMode.WARN;
        }
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if ("ERROR".equals(normalized) || "STRICT".equals(normalized)) {
            return ContractsEnforcementMode.ERROR;
        }
        return ContractsEnforcementMode.WARN;
    }

    private AddonServiceRegistry.ApiTypeEnforcementMode serviceApiTypeEnforcementModeFromConfig() {
        String raw = null;
        try {
            raw = trimToNull(plugin.getConfig().getString("addons.contracts.serviceApiType"));
            if (raw == null) {
                raw = trimToNull(plugin.getConfig().getString("addons.audit.serviceApiType"));
            }
        } catch (Throwable ignored) {
            raw = null;
        }
        if (raw == null) {
            return AddonServiceRegistry.ApiTypeEnforcementMode.WARN;
        }
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if ("ERROR".equals(normalized) || "STRICT".equals(normalized)) {
            return AddonServiceRegistry.ApiTypeEnforcementMode.ERROR;
        }
        return AddonServiceRegistry.ApiTypeEnforcementMode.WARN;
    }

    private ClassFileInspection inspectClassFile(InputStream rawIn) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(rawIn))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                return new ClassFileInspection("", Set.of(), Set.of());
            }
            in.readUnsignedShort();
            in.readUnsignedShort();
            int cpCount = in.readUnsignedShort();
            String[] utf8 = new String[cpCount];
            int[] classNameIndex = new int[cpCount];

            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8[i] = in.readUTF();
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> {
                        in.readLong();
                        i++;
                    }
                    case 7 -> classNameIndex[i] = in.readUnsignedShort();
                    case 8, 16, 19, 20 -> in.readUnsignedShort();
                    case 9, 10, 11, 12, 17, 18 -> {
                        in.readUnsignedShort();
                        in.readUnsignedShort();
                    }
                    case 15 -> {
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                    }
                    default -> {
                        return new ClassFileInspection("", Set.of(), Set.of());
                    }
                }
            }

            in.readUnsignedShort();
            int thisClassIndex = in.readUnsignedShort();
            in.readUnsignedShort();

            String ownerInternal = "";
            if (thisClassIndex > 0 && thisClassIndex < classNameIndex.length) {
                int ownerNameIdx = classNameIndex[thisClassIndex];
                if (ownerNameIdx > 0 && ownerNameIdx < utf8.length) {
                    ownerInternal = normalizeInternalClassName(utf8[ownerNameIdx]);
                    if (ownerInternal == null) {
                        ownerInternal = "";
                    }
                }
            }

            Set<String> referenced = new LinkedHashSet<>();
            for (int i = 1; i < classNameIndex.length; i++) {
                int nameIdx = classNameIndex[i];
                if (nameIdx <= 0 || nameIdx >= utf8.length) {
                    continue;
                }
                String ref = normalizeInternalClassName(utf8[nameIdx]);
                if (ref != null && !ref.isBlank()) {
                    referenced.add(ref);
                }
            }
            referenced.remove(ownerInternal);

            int interfaceCount = in.readUnsignedShort();
            for (int i = 0; i < interfaceCount; i++) {
                in.readUnsignedShort();
            }

            int fieldsCount = in.readUnsignedShort();
            for (int i = 0; i < fieldsCount; i++) {
                skipMemberInfo(in);
            }

            int methodsCount = in.readUnsignedShort();
            for (int i = 0; i < methodsCount; i++) {
                skipMemberInfo(in);
            }

            Set<String> classAnnotations = new LinkedHashSet<>();
            int attributesCount = in.readUnsignedShort();
            for (int i = 0; i < attributesCount; i++) {
                int nameIndex = in.readUnsignedShort();
                long length = Integer.toUnsignedLong(in.readInt());
                String attributeName = nameIndex > 0 && nameIndex < utf8.length ? utf8[nameIndex] : null;
                if (length <= 0L) {
                    continue;
                }
                if (ATTR_RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName) || ATTR_RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
                    byte[] data = in.readNBytes((int) length);
                    collectAnnotationDescriptors(data, utf8, classAnnotations);
                    continue;
                }
                in.skipNBytes(length);
            }

            return new ClassFileInspection(ownerInternal, Set.copyOf(referenced), Set.copyOf(classAnnotations));
        }
    }

    private static void skipMemberInfo(DataInputStream in) throws IOException {
        in.readUnsignedShort();
        in.readUnsignedShort();
        in.readUnsignedShort();
        int attributesCount = in.readUnsignedShort();
        for (int j = 0; j < attributesCount; j++) {
            in.readUnsignedShort();
            long len = Integer.toUnsignedLong(in.readInt());
            in.skipNBytes(len);
        }
    }

    private static void collectAnnotationDescriptors(byte[] data, String[] utf8, Set<String> out) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(data)))) {
            int numAnnotations = in.readUnsignedShort();
            for (int i = 0; i < numAnnotations; i++) {
                int typeIndex = in.readUnsignedShort();
                String descriptor = typeIndex > 0 && typeIndex < utf8.length ? utf8[typeIndex] : null;
                if (descriptor != null && !descriptor.isBlank()) {
                    out.add(descriptor);
                }
                int pairs = in.readUnsignedShort();
                for (int p = 0; p < pairs; p++) {
                    in.readUnsignedShort();
                    skipElementValue(in);
                }
            }
        }
    }

    private static void skipElementValue(DataInputStream in) throws IOException {
        int tag = in.readUnsignedByte();
        switch (tag) {
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's' -> in.readUnsignedShort();
            case 'e' -> {
                in.readUnsignedShort();
                in.readUnsignedShort();
            }
            case 'c' -> in.readUnsignedShort();
            case '@' -> {
                in.readUnsignedShort();
                int pairs = in.readUnsignedShort();
                for (int i = 0; i < pairs; i++) {
                    in.readUnsignedShort();
                    skipElementValue(in);
                }
            }
            case '[' -> {
                int count = in.readUnsignedShort();
                for (int i = 0; i < count; i++) {
                    skipElementValue(in);
                }
            }
            default -> {
            }
        }
    }

    private static String normalizeInternalClassName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        while (v.startsWith("[")) {
            v = v.substring(1);
        }
        if (v.startsWith("L") && v.endsWith(";")) {
            v = v.substring(1, v.length() - 1);
        }
        if (v.isBlank()) {
            return null;
        }
        return v.contains(".") ? v.replace('.', '/') : v;
    }

    private int parseAddonApiVersion(YamlConfiguration yaml, String fileName) {
        Object raw = yaml.get("api");
        if (raw == null) {
            raw = yaml.get("apiVersion");
        }
        if (raw == null) {
            return MultiBlockEngine.getApiVersion();
        }
        String text = trimToNull(String.valueOf(raw));
        if (text == null) {
            return MultiBlockEngine.getApiVersion();
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("Invalid addon.yml in " + fileName + ": Invalid api value: " + text);
        }
    }

    private Map<String, Version> parseYamlDependencies(Object raw, String ownerId, String fileName, String fieldName) {
        if (raw == null) {
            return Map.of();
        }

        Map<String, Version> out = new LinkedHashMap<>();
        if (raw instanceof List<?> list) {
            for (Object entry : list) {
                parseYamlDependencyEntry(out, entry, ownerId, fileName, fieldName);
            }
            return Map.copyOf(out);
        }
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String depId = trimToNull(entry.getKey() == null ? null : String.valueOf(entry.getKey()));
                validateDependencyId(ownerId, depId, fileName, fieldName);
                Version min = parseDependencyVersionConstraint(
                    ownerId,
                    depId,
                    entry.getValue() == null ? null : String.valueOf(entry.getValue()),
                    fileName,
                    fieldName
                );
                putDependency(out, depId, min, ownerId, fileName, fieldName);
            }
            return Map.copyOf(out);
        }
        throw new IllegalArgumentException("Invalid addon.yml in " + fileName + ": " + fieldName + " must be a list or map");
    }

    private void parseYamlDependencyEntry(Map<String, Version> out, Object entry, String ownerId, String fileName, String fieldName) {
        if (entry == null) {
            return;
        }
        if (entry instanceof String token) {
            String depId = trimToNull(token);
            validateDependencyId(ownerId, depId, fileName, fieldName);
            putDependency(out, depId, MIN_DEPENDENCY_VERSION, ownerId, fileName, fieldName);
            return;
        }
        if (!(entry instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Invalid addon.yml in " + fileName + ": " + fieldName + " entries must be strings or maps");
        }
        String depId = trimToNull(map.get("id") == null ? null : String.valueOf(map.get("id")));
        validateDependencyId(ownerId, depId, fileName, fieldName);
        String versionConstraint = map.get("version") == null ? null : String.valueOf(map.get("version"));
        Version min = parseDependencyVersionConstraint(ownerId, depId, versionConstraint, fileName, fieldName);
        putDependency(out, depId, min, ownerId, fileName, fieldName);
    }

    private void validateDependencyId(String ownerId, String depId, String fileName, String fieldName) {
        if (depId == null || !depId.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
            throw new IllegalArgumentException("Invalid addon.yml in " + fileName + ": Invalid dependency id in " + fieldName + ": " + depId);
        }
        if (depId.equals(ownerId)) {
            throw new IllegalArgumentException("Invalid addon.yml in " + fileName + ": Self dependency not allowed in " + fieldName);
        }
    }

    private Version parseDependencyVersionConstraint(String ownerId, String depId, String rawConstraint, String fileName, String fieldName) {
        String constraint = trimToNull(rawConstraint);
        if (constraint == null) {
            return MIN_DEPENDENCY_VERSION;
        }

        String candidate = null;
        for (String token : constraint.split("\\s+")) {
            String t = trimToNull(token);
            if (t == null) {
                continue;
            }
            if (t.startsWith(">=")) {
                candidate = trimToNull(t.substring(2));
                break;
            }
            if (candidate == null) {
                candidate = t.startsWith("=") ? trimToNull(t.substring(1)) : t;
            }
        }
        if (candidate == null) {
            return MIN_DEPENDENCY_VERSION;
        }
        try {
            return Version.parse(candidate);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid addon.yml in " + fileName + ": Invalid dependency version in " + fieldName + ": " + depId + " (" + constraint + ")"
            );
        }
    }

    private void putDependency(Map<String, Version> out, String depId, Version min, String ownerId, String fileName, String fieldName) {
        if (out.containsKey(depId)) {
            coreLogger(LogPhase.LOAD).warn(
                "Duplicate dependency declaration (last one wins)",
                LogKv.kv("owner", ownerId),
                LogKv.kv("dep", depId),
                LogKv.kv("field", fieldName),
                LogKv.kv("file", fileName)
            );
        }
        out.put(depId, min == null ? MIN_DEPENDENCY_VERSION : min);
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

    private static final class AddonClassLoader extends URLClassLoader {
        private final String addonId;
        private final List<AddonClassLoader> dependencies;

        private AddonClassLoader(String addonId, URL[] urls, ClassLoader parent, List<AddonClassLoader> dependencies) {
            super(urls, parent);
            this.addonId = addonId == null ? "unknown" : addonId;
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }

                if (isParentFirst(name)) {
                    try {
                        Class<?> parentClass = getParent().loadClass(name);
                        if (resolve) {
                            resolveClass(parentClass);
                        }
                        return parentClass;
                    } catch (ClassNotFoundException ignored) {
                    }
                }

                Class<?> local = tryLoadOwnClass(name);
                if (local != null) {
                    if (resolve) {
                        resolveClass(local);
                    }
                    return local;
                }

                for (AddonClassLoader dependency : dependencies) {
                    if (dependency == null) {
                        continue;
                    }
                    Class<?> depClass = dependency.tryLoadOwnClass(name);
                    if (depClass != null) {
                        if (resolve) {
                            resolveClass(depClass);
                        }
                        return depClass;
                    }
                }

                Class<?> parentClass = getParent().loadClass(name);
                if (resolve) {
                    resolveClass(parentClass);
                }
                return parentClass;
            }
        }

        private Class<?> tryLoadOwnClass(String name) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                try {
                    return findClass(name);
                } catch (ClassNotFoundException ignored) {
                    return null;
                }
            }
        }

        private boolean isParentFirst(String name) {
            if (name == null || name.isBlank()) {
                return true;
            }
            return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.bukkit.")
                || name.startsWith("io.papermc.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("com.destroystokyo.paper.");
        }

        @Override
        public String toString() {
            return "AddonClassLoader[" + addonId + "]";
        }
    }

    public void enableAddons() {
        EngineLogger core = coreLogger(LogPhase.ENABLE);
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CORE_SERVICES);
        serviceLifecycleManager.injectServices(CORE_PROVIDER_ID);
        serviceLifecycleManager.enableServices(CORE_PROVIDER_ID);

        for (String id : resolvedOrder) {
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;
            if (states.getOrDefault(id, AddonState.DISABLED) != AddonState.LOADED) continue;
            serviceLifecycleManager.injectAddon(id, loaded.addon());
            serviceLifecycleManager.injectServices(id);
        }

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.ADDON_SERVICES);
        List<String> contentRegistrationOrder = new ArrayList<>();
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

            loaded.phase().set(LogPhase.ENABLE);
            serviceLifecycleManager.enableServices(id);
            contentRegistrationOrder.add(id);
        }

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CONTENT_REGISTRATION);
        for (String id : contentRegistrationOrder) {
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) {
                continue;
            }
            try {
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
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.RUNTIME);
    }

    public void disableAddons() {
        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.ADDON_SERVICES);
        pendingExposures.clear();
        while (!enableOrder.isEmpty()) {
            String id = enableOrder.removeLast();
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;

            try {
                unexposeAddonServices(id);
                loaded.phase().set(LogPhase.DISABLE);
                loaded.addon().onDisable();
                serviceLifecycleManager.disableServices(id);
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

        serviceLifecycleManager.setCurrentPhase(ServiceLifecycleOrchestrator.LifecyclePhase.CORE_SERVICES);
        serviceLifecycleManager.disableServices(CORE_PROVIDER_ID);
        exposedServices.clear();
        serviceLifecycleManager.clear();
        crossReferenceManager.clear();
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
                ComponentKind kind = MbeCommandService.class.isAssignableFrom(svc.api()) ? ComponentKind.COMMAND_SERVICE : ComponentKind.SERVICE;
                publishComponentAvailability(addonId, svc.api().getName(), kind, ComponentChangeType.REMOVED);
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

    private void publishComponentAvailability(String addonId, String componentId, ComponentKind kind, ComponentChangeType changeType) {
        if (addonId == null || addonId.isBlank() || componentId == null || componentId.isBlank() || kind == null || changeType == null) {
            return;
        }
        if (Bukkit.getServer() == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(new ComponentAvailabilityEvent(addonId, componentId, kind, changeType));
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
