package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.addon.Version;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.addon.AddonException;
import dev.darkblade.mbe.core.application.service.addon.domain.DiscoveredAddon;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonState;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonAuditIndex;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonAuditReport;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonDiscoveryService {
    private enum ContractsEnforcementMode { WARN, ERROR }
    private static final Version MIN_DEPENDENCY_VERSION = Version.parse("0.0.0");
    
    private final MultiBlockEngine plugin;
    private final CoreLogger log;
    private final AddonRegistry registry;
    private final AddonAuditService auditService;
    private final AddonDependencyResolver dependencyResolver;
    private final File addonFolder;
    private final AddonDataDirectorySystem dataDirectorySystem;

    public AddonDiscoveryService(MultiBlockEngine plugin, CoreLogger log, AddonRegistry registry, AddonAuditService auditService, AddonDependencyResolver dependencyResolver, File addonFolder, AddonDataDirectorySystem dataDirectorySystem) {
        this.plugin = plugin;
        this.log = log;
        this.registry = registry;
        this.auditService = auditService;
        this.dependencyResolver = dependencyResolver;
        this.addonFolder = addonFolder;
        this.dataDirectorySystem = dataDirectorySystem;
    }

    private CoreLogger coreLogger(LogPhase phase) {
        return log;
    }

    public void loadAddons() {
        CoreLogger core = coreLogger(LogPhase.LOAD);

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
        if (files == null)
            return;

        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Map<String, List<DiscoveredAddon>> candidates = new HashMap<>();

        Map<File, AddonAuditIndex> auditIndexesByFile = new HashMap<>();

        for (File file : files) {
            try {
                AddonMetadata metadata = readMetadata(file);
                if (metadata == null) {
                    continue;
                }

                AddonAuditIndex auditIndex = auditService.buildAuditIndex(file, metadata);
                if (auditIndex != null) {
                    auditIndexesByFile.put(file, auditIndex);
                }

                candidates.computeIfAbsent(metadata.id(), k -> new ArrayList<>())
                        .add(new DiscoveredAddon(file, metadata));
            } catch (Exception e) {
                core.error("Failed to load addon from file", e, LogKv.kv("file", file.getName()));
            }
        }

        registry.discoveredAddons.clear();
        registry.loadedAddons.clear();
        registry.states.clear();
        registry.states.put(AddonRegistry.CORE_PROVIDER_ID, AddonState.ENABLED);
        registry.enableOrder.clear();
        registry.pendingExposures.clear();
        registry.exposedServices.clear();
        // serviceLifecycleManager.clearAddons();
        registry.resolvedOrder = List.of();

        for (Map.Entry<String, List<DiscoveredAddon>> entry : candidates.entrySet()) {
            String id = entry.getKey();
            List<DiscoveredAddon> list = entry.getValue();
            if (list.size() > 1) {
                StringBuilder sb = new StringBuilder();
                for (DiscoveredAddon d : list) {
                    if (!sb.isEmpty())
                        sb.append(", ");
                    sb.append(d.file().getName()).append("@").append(d.metadata().version());
                }
                core.error("Addon failed: multiple versions detected", LogKv.kv("id", id),
                        LogKv.kv("duplicates", sb.toString()));
                registry.states.put(id, AddonState.FAILED);
                continue;
            }

            DiscoveredAddon discovered = list.get(0);
            registry.discoveredAddons.put(id, discovered);
            registry.states.put(id, AddonState.DISCOVERED);
        }

        ContractsEnforcementMode contractsMode = contractsEnforcementMode();
        boolean strictContracts = contractsMode == ContractsEnforcementMode.ERROR;
        // this.serviceApiTypeMode = serviceApiTypeEnforcementModeFromConfig();
        // serviceRegistry.setApiTypeEnforcementMode(this.serviceApiTypeMode);
        core.info("Addon contracts enforcement mode", LogKv.kv("mode", contractsMode.name()));
        core.info("Addon service api type enforcement mode", LogKv.kv("mode", "ERROR"));

        Map<String, AddonAuditReport> auditReports = auditService.auditDiscoveredAddons(registry.discoveredAddons, auditIndexesByFile,
                strictContracts);
        for (AddonAuditReport report : auditReports.values()) {
            if (!report.violations().isEmpty()) {
                String violations = joinLimited(report.violations(), 10);
                if (report.fatal()) {
                    registry.states.put(report.addonId(), AddonState.FAILED);
                    core.error("Addon failed audit", LogKv.kv("id", report.addonId()),
                            LogKv.kv("file", report.fileName()), LogKv.kv("violations", violations));
                } else {
                    core.warn("Addon audit warnings", LogKv.kv("id", report.addonId()),
                            LogKv.kv("file", report.fileName()), LogKv.kv("violations", violations));
                }
            }

            if (!report.sharedApis().isEmpty()) {
                core.warn("Addon shared APIs detected", LogKv.kv("id", report.addonId()),
                        LogKv.kv("shared", joinLimited(new ArrayList<>(report.sharedApis()), 30)),
                        LogKv.kv("count", report.sharedApis().size()));
            }

            if (!report.declaredAddonRefs().isEmpty()) {
                core.info("Addon cross-addon references validated", LogKv.kv("id", report.addonId()),
                        LogKv.kv("refs", joinLimited(new ArrayList<>(report.declaredAddonRefs()), 30)),
                        LogKv.kv("count", report.declaredAddonRefs().size()));
            }

            if (!report.undeclaredAddonRefs().isEmpty()) {
                core.warn("Addon undeclared cross-addon references detected", LogKv.kv("id", report.addonId()),
                        LogKv.kv("refs", joinLimited(new ArrayList<>(report.undeclaredAddonRefs()), 30)),
                        LogKv.kv("count", report.undeclaredAddonRefs().size()));
            }

            if (!report.nonApiAccessRefs().isEmpty()) {
                core.warn("Addon cross-addon internal access detected", LogKv.kv("id", report.addonId()),
                        LogKv.kv("refs", joinLimited(new ArrayList<>(report.nonApiAccessRefs()), 30)),
                        LogKv.kv("count", report.nonApiAccessRefs().size()));
            }

            if (!report.embeddedJars().isEmpty()) {
                core.warn("Addon embeds nested jar libraries", LogKv.kv("id", report.addonId()),
                        LogKv.kv("jars", joinLimited(new ArrayList<>(report.embeddedJars()), 20)),
                        LogKv.kv("count", report.embeddedJars().size()));
            }
        }

        Map<String, AddonMetadata> metadataById = new HashMap<>();
        for (Map.Entry<String, DiscoveredAddon> e : registry.discoveredAddons.entrySet()) {
            metadataById.put(e.getKey(), e.getValue().metadata());
        }

        AddonDependencyResolver.Resolution resolution = dependencyResolver.resolve(MultiBlockEngine.getApiVersion(),
                metadataById);

        core.info("Addon dependency resolution complete",
                LogKv.kv("candidates", metadataById.size()),
                LogKv.kv("eligible", resolution.loadOrder().size()),
                LogKv.kv("failed", resolution.failures().size()),
                LogKv.kv("warnings", resolution.warnings().size()));

        for (String warning : resolution.warnings()) {
            core.warn("Addon dependency warning", LogKv.kv("warning", warning));
        }
        for (Map.Entry<String, String> fail : resolution.failures().entrySet()) {
            registry.states.put(fail.getKey(), AddonState.FAILED);
            core.error("Addon dependency failure", LogKv.kv("id", fail.getKey()), LogKv.kv("reason", fail.getValue()));
        }

        registry.resolvedOrder = resolution.loadOrder();

        if (!registry.resolvedOrder.isEmpty()) {
            core.info("Addon load order", LogKv.kv("order", joinLimited(registry.resolvedOrder, 30)),
                    LogKv.kv("count", registry.resolvedOrder.size()));
        }

        for (String id : registry.resolvedOrder) {
            if (registry.states.getOrDefault(id, AddonState.DISABLED) == AddonState.FAILED) {
                continue;
            }

            DiscoveredAddon discovered = registry.discoveredAddons.get(id);
            if (discovered == null) {
                continue;
            }

            try {
                // Delegated to AddonRuntimeLifecycleService
            } catch (Exception e) {
                // failAddon(id, AddonException.Phase.LOAD, "Unhandled exception during addon load", e, true);
            }
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
                coreLogger(LogPhase.LOAD).warn("Skipping addon: addon.yml requires id, version, main",
                        LogKv.kv("file", file.getName()));
                return null;
            }

            if (!id.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: invalid id", LogKv.kv("file", file.getName()),
                        LogKv.kv("id", id));
                return null;
            }

            Version version;
            try {
                version = Version.parse(versionStr);
            } catch (IllegalArgumentException e) {
                coreLogger(LogPhase.LOAD).warn("Skipping addon: invalid version", LogKv.kv("file", file.getName()),
                        LogKv.kv("version", versionStr));
                return null;
            }

            int apiVersion = parseAddonApiVersion(yaml, file.getName());

            Map<String, Version> required = parseYamlDependencies(yaml.get("dependencies"), id, file.getName(),
                    "dependencies");
            Map<String, Version> optional = parseYamlDependencies(yaml.get("soft-dependencies"), id, file.getName(),
                    "soft-dependencies");

            if (!required.isEmpty() && !optional.isEmpty()) {
                for (String depId : required.keySet()) {
                    if (optional.containsKey(depId)) {
                        coreLogger(LogPhase.LOAD).warn("Optional dependency overridden by required",
                                LogKv.kv("owner", id), LogKv.kv("dep", depId));
                    }
                }
                Map<String, Version> filteredOpt = new LinkedHashMap<>(optional);
                filteredOpt.keySet().removeAll(required.keySet());
                optional = filteredOpt;
            }

            List<String> dependsIds = new ArrayList<>(required.keySet());
            dependsIds.addAll(optional.keySet());
            dependsIds = List.copyOf(dependsIds);

            return new AddonMetadata(id, version, apiVersion, main, Map.copyOf(required), Map.copyOf(optional),
                    dependsIds);
        }
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
                        fieldName);
                putDependency(out, depId, min, ownerId, fileName, fieldName);
            }
            return Map.copyOf(out);
        }
        throw new IllegalArgumentException(
                "Invalid addon.yml in " + fileName + ": " + fieldName + " must be a list or map");
    }



    private void validateDependencyId(String ownerId, String depId, String fileName, String fieldName) {
        if (depId == null || !depId.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
            throw new IllegalArgumentException(
                    "Invalid addon.yml in " + fileName + ": Invalid dependency id in " + fieldName + ": " + depId);
        }
        if (depId.equals(ownerId)) {
            throw new IllegalArgumentException(
                    "Invalid addon.yml in " + fileName + ": Self dependency not allowed in " + fieldName);
        }
    }





    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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


    private void parseYamlDependencyEntry(Map<String, Version> out, Object entry, String ownerId, String fileName,
            String fieldName) {
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
            throw new IllegalArgumentException(
                    "Invalid addon.yml in " + fileName + ": " + fieldName + " entries must be strings or maps");
        }
        String depId = trimToNull(map.get("id") == null ? null : String.valueOf(map.get("id")));
        validateDependencyId(ownerId, depId, fileName, fieldName);
        String versionConstraint = map.get("version") == null ? null : String.valueOf(map.get("version"));
        Version min = parseDependencyVersionConstraint(ownerId, depId, versionConstraint, fileName, fieldName);
        putDependency(out, depId, min, ownerId, fileName, fieldName);
    }

    private Version parseDependencyVersionConstraint(String ownerId, String depId, String rawConstraint,
            String fileName, String fieldName) {
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
                    "Invalid addon.yml in " + fileName + ": Invalid dependency version in " + fieldName + ": " + depId
                            + " (" + constraint + ")");
        }
    }

    private void putDependency(Map<String, Version> out, String depId, Version min, String ownerId, String fileName,
            String fieldName) {
        if (out.containsKey(depId)) {
            coreLogger(LogPhase.LOAD).warn(
                    "Duplicate dependency declaration (last one wins)",
                    LogKv.kv("owner", ownerId),
                    LogKv.kv("dep", depId),
                    LogKv.kv("field", fieldName),
                    LogKv.kv("file", fileName));
        }
        out.put(depId, min == null ? MIN_DEPENDENCY_VERSION : min);
    }



}
