package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.core.application.service.addon.domain.AddonAuditIndex;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonAuditReport;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonReferenceHit;
import dev.darkblade.mbe.core.application.service.addon.domain.ClassFileInspection;
import dev.darkblade.mbe.core.application.service.addon.domain.DiscoveredAddon;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonAuditService {
    private static final String ADDON_API_DESCRIPTOR = "Ldev/darkblade/mbe/api/addon/AddonAPI;";
    private static final String ATTR_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";
    private static final String ATTR_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";

    private static final Map<String, String> REQUIRED_CAPABILITIES_BY_PACKAGE = Map.of(
            "dev/darkblade/mbe/api/ui/", "mbe-core:ui",
            "dev/darkblade/mbe/api/electricity/", "mbe-core:energy",
            "dev/darkblade/mbe/api/wiring/", "mbe-core:wiring",
            "dev/darkblade/mbe/api/persistence/", "mbe-core:persistence"
    );

    public AddonAuditIndex buildAuditIndex(File file, AddonMetadata metadata) {
        try (JarFile jar = new JarFile(file)) {
            List<String> classEntries = new ArrayList<>();
            Set<String> classInternalNames = new LinkedHashSet<>();
            Set<String> apiClasses = new LinkedHashSet<>();
            Set<String> apiContractClasses = new LinkedHashSet<>();
            Set<String> embeddedCoreApiClasses = new LinkedHashSet<>();
            Set<String> embeddedJars = new LinkedHashSet<>();
            Set<String> requiredCapabilities = new LinkedHashSet<>();

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

                if (name.startsWith("com/darkbladedev/engine/api/")
                        || name.startsWith("com/darkbladedev/engine/model/")) {
                    embeddedCoreApiClasses.add(fqcn);
                }

                try (InputStream in = jar.getInputStream(entry)) {
                    ClassFileInspection inspection = inspectClassFile(in);
                    if (inspection.classAnnotationDescriptors().contains(ADDON_API_DESCRIPTOR)) {
                        apiContractClasses.add(fqcn);
                    }
                    for (String refInternal : inspection.referencedClassNames()) {
                        for (Map.Entry<String, String> capReq : REQUIRED_CAPABILITIES_BY_PACKAGE.entrySet()) {
                            if (refInternal.startsWith(capReq.getKey())) {
                                requiredCapabilities.add(capReq.getValue());
                            }
                        }
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
                    Set.copyOf(embeddedJars),
                    Set.copyOf(requiredCapabilities));
        } catch (Exception ignored) {
            return null;
        }
    }

    public Map<String, AddonAuditReport> auditDiscoveredAddons(Map<String, DiscoveredAddon> discovered,
            Map<File, AddonAuditIndex> byFile, boolean strictContracts) {
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
            if (e.getValue().size() <= 1)
                continue;
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
            if (discoveredAddon == null)
                continue;
            Set<AddonReferenceHit> refs = scanCrossAddonReferences(discoveredAddon.file(), idx.classEntries(),
                    classOwnerByInternal, idx.addonId());
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

            DiscoveredAddon discoveredAddon = discovered.get(idx.addonId());
            if (discoveredAddon == null) {
                continue;
            }
            AddonMetadata metadata = discoveredAddon.metadata();
            if (metadata.environment() != null) {
                AddonMetadata.Environment env = metadata.environment();
                
                if (env.java() != null) {
                    try {
                        String sysJava = System.getProperty("java.version");
                        String cleanJava = sysJava.split("_")[0].replaceAll("[^0-9.]", "");
                        if (!cleanJava.contains(".")) cleanJava += ".0.0";
                        dev.darkblade.mbe.api.addon.Version sysJavaVer = dev.darkblade.mbe.api.addon.Version.parse(cleanJava);
                        if (sysJavaVer.compareTo(env.java()) < 0) {
                            violations.add("requires Java version >= " + env.java() + " but found " + sysJava);
                            fatal = true;
                        }
                    } catch (Exception ignored) {}
                }
                
                if (env.minecraft() != null) {
                    try {
                        if (org.bukkit.Bukkit.getServer() != null) {
                            String mcVerStr = org.bukkit.Bukkit.getBukkitVersion().split("-")[0];
                            dev.darkblade.mbe.api.addon.Version sysMcVer = dev.darkblade.mbe.api.addon.Version.parse(mcVerStr);
                            if (sysMcVer.compareTo(env.minecraft()) < 0) {
                                violations.add("requires Minecraft version >= " + env.minecraft() + " but found " + mcVerStr);
                                fatal = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                
                if (env.plugins() != null) {
                    for (Map.Entry<String, dev.darkblade.mbe.api.addon.Version> pluginDep : env.plugins().entrySet()) {
                        try {
                            if (org.bukkit.Bukkit.getServer() != null) {
                                org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin(pluginDep.getKey());
                                if (p == null) {
                                    violations.add("requires Bukkit plugin '" + pluginDep.getKey() + "' which is not installed");
                                    fatal = true;
                                } else {
                                    dev.darkblade.mbe.api.addon.Version pluginVer = dev.darkblade.mbe.api.addon.Version.parse(p.getDescription().getVersion().split("-")[0].replaceAll("[^0-9.]", ""));
                                    if (pluginVer.compareTo(pluginDep.getValue()) < 0) {
                                        violations.add("requires Bukkit plugin '" + pluginDep.getKey() + "' >= " + pluginDep.getValue() + " but found " + p.getDescription().getVersion());
                                        fatal = true;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            Set<String> shared = sharedApisByAddon.getOrDefault(idx.addonId(), Set.of());
            if (!shared.isEmpty()) {
                violations.add("shared API definitions duplicated across addons");
                fatal = true;
            }

            if (idx.requiredCapabilities() != null && !idx.requiredCapabilities().isEmpty()) {
                List<String> declaredCaps = metadata.capabilities() != null ? metadata.capabilities() : List.of();
                for (String reqCap : idx.requiredCapabilities()) {
                    if (!declaredCaps.contains(reqCap)) {
                        violations.add("references protected API but did not declare required capability: '" + reqCap + "'");
                        fatal = true;
                    }
                }
            }

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
                violations.add("references classes from addons without declared dependency: "
                        + joinLimited(undeclaredTargets, 10));
                fatal = true;
            }

            if (!nonApiRefs.isEmpty()) {
                if (strictContracts) {
                    violations.add("references non-@AddonAPI classes from dependencies (strict mode)");
                    fatal = true;
                } else {
                    violations.add("references non-@AddonAPI classes from dependencies (compat mode warning)");
                }
            }

            out.put(idx.addonId(), new AddonAuditReport(idx.addonId(), idx.fileName(), shared,
                    declaredRefsByAddon.getOrDefault(idx.addonId(), Set.of()),
                    undeclaredRefs, nonApiRefs, idx.embeddedJars(), List.copyOf(violations), fatal));
        }

        return out;
    }

    private Set<AddonReferenceHit> scanCrossAddonReferences(File addonJar, List<String> classEntries,
            Map<String, String> classOwnerByInternal, String selfId) {
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
                if (entry == null)
                    continue;
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

    private Set<AddonReferenceHit> scanClassReferences(InputStream rawIn, Map<String, String> classOwnerByInternal,
            String selfId) throws IOException {
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
            if (i > 0)
                sb.append('/');
            sb.append(parts[i]);
        }
        return sb.toString();
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
                if (ATTR_RUNTIME_VISIBLE_ANNOTATIONS.equals(attributeName)
                        || ATTR_RUNTIME_INVISIBLE_ANNOTATIONS.equals(attributeName)) {
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

    private static String joinLimited(List<String> list, int limit) {
        if (list == null || list.isEmpty()) return "";
        if (list.size() <= limit) return String.join(", ", list);
        return String.join(", ", list.subList(0, limit)) + " ... (+" + (list.size() - limit) + ")";
    }
}
