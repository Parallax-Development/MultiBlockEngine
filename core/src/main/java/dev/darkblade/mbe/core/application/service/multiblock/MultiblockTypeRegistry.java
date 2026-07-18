package dev.darkblade.mbe.core.application.service.multiblock;

import dev.darkblade.mbe.core.domain.MultiblockSource;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;

import org.bukkit.util.Vector;

import java.util.*;

public class MultiblockTypeRegistry {
    private final Map<String, MultiblockType> types = new HashMap<>();
    private final Map<String, MultiblockSource> sourcesByTypeId = new HashMap<>();
    private final Map<String, List<MultiblockType>> variantsBySignature = new HashMap<>();

    public void registerType(MultiblockType type) {
        registerType(type, new MultiblockSource(MultiblockSource.Type.USER_DEFINED, "<runtime>"));
    }

    public void registerType(MultiblockType type, MultiblockSource source) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (source == null) {
            source = new MultiblockSource(MultiblockSource.Type.USER_DEFINED, "<runtime>");
        }
        if (types.containsKey(type.id().toString())) {
            throw new IllegalArgumentException("Duplicate multiblock id: " + type.id().toString());
        }
        types.put(type.id().toString(), type);
        sourcesByTypeId.put(type.id().toString(), source);
        String sig = computeSignature(type);
        variantsBySignature.compute(sig, (k, list) -> {
            List<MultiblockType> next = list == null ? new ArrayList<>() : new ArrayList<>(list);
            next.add(type);
            next.sort(this::variantComparator);
            return List.copyOf(next);
        });
    }

    public Optional<MultiblockType> getType(String id) {
        return Optional.ofNullable(types.get(id));
    }

    public Optional<MultiblockSource> getSource(String typeId) {
        return Optional.ofNullable(sourcesByTypeId.get(typeId));
    }

    public Collection<MultiblockType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public List<MultiblockType> getTypesDeterministic() {
        List<MultiblockType> out = new ArrayList<>(types.values());
        out.sort(this::variantComparator);
        return List.copyOf(out);
    }

    public void unregisterAll() {
        types.clear();
        sourcesByTypeId.clear();
        variantsBySignature.clear();
    }

    public void reloadTypesWithSources(Collection<MultiblockType> newTypes, Map<String, MultiblockSource> sources) {
        Map<String, MultiblockType> runtimeTypes = new LinkedHashMap<>();
        Map<String, MultiblockSource> runtimeSources = new LinkedHashMap<>();
        for (Map.Entry<String, MultiblockType> e : types.entrySet()) {
            String id = e.getKey();
            MultiblockSource src = sourcesByTypeId.get(id);
            if (src != null && "<runtime>".equals(src.path())) {
                runtimeTypes.put(id, e.getValue());
                runtimeSources.put(id, src);
            }
        }

        types.clear();
        sourcesByTypeId.clear();
        variantsBySignature.clear();

        Map<String, MultiblockSource> src = sources == null ? Map.of() : sources;
        for (MultiblockType type : newTypes) {
            if (type == null) {
                continue;
            }
            if (runtimeTypes.containsKey(type.id().toString())) {
                continue;
            }
            MultiblockSource source = src.get(type.id().toString());
            registerType(type, source);
        }

        for (Map.Entry<String, MultiblockType> e : runtimeTypes.entrySet()) {
            MultiblockSource source = runtimeSources.get(e.getKey());
            registerType(e.getValue(), source);
        }
    }

    public String signatureOf(MultiblockType type) {
        return computeSignature(type);
    }

    public List<MultiblockType> variantsForSignature(String signature) {
        List<MultiblockType> list = variantsBySignature.get(signature);
        return list == null ? List.of() : list;
    }

    private int variantComparator(MultiblockType a, MultiblockType b) {
        MultiblockSource.Type aSrc = sourceTypeOf(a);
        MultiblockSource.Type bSrc = sourceTypeOf(b);
        if (aSrc != bSrc) {
            return aSrc == MultiblockSource.Type.CORE_DEFAULT ? -1 : 1;
        }
        return a.id().compareTo(b.id());
    }

    private MultiblockSource.Type sourceTypeOf(MultiblockType type) {
        if (type == null || type.id().toString() == null) {
            return MultiblockSource.Type.USER_DEFINED;
        }
        MultiblockSource source = sourcesByTypeId.get(type.id().toString());
        return source == null ? MultiblockSource.Type.USER_DEFINED : source.type();
    }

    private String computeSignature(MultiblockType type) {
        String raw = computeSignatureRaw(type);
        return sha256(raw);
    }

    private String computeSignatureRaw(MultiblockType type) {
        StringBuilder sb = new StringBuilder();
        sb.append("controller:").append(matcherKey(type.controllerMatcher())).append("|");
        List<String> parts = new ArrayList<>();
        for (PatternEntry entry : type.pattern()) {
            Vector o = entry.offset();
            String k = matcherKey(entry.matcher());
            parts.add(o.getBlockX() + "," + o.getBlockY() + "," + o.getBlockZ() + "=" + k + (entry.optional() ? "?" : "!"));
        }
        parts.sort(String::compareToIgnoreCase);
        sb.append(String.join(";", parts));
        return sb.toString();
    }

    private String matcherKey(dev.darkblade.mbe.core.domain.BlockMatcher matcher) {
        if (matcher == null) {
            return "";
        }
        if (matcher instanceof dev.darkblade.mbe.core.domain.rule.ExactMaterialMatcher m) {
            return m.material() == null ? "" : m.material().name();
        }
        if (matcher instanceof dev.darkblade.mbe.core.domain.rule.TagMatcher m) {
            if (m.tag() == null || m.tag().getKey() == null) {
                return "";
            }
            return "#" + m.tag().getKey();
        }
        if (matcher instanceof dev.darkblade.mbe.core.domain.rule.AirMatcher) {
            return "AIR";
        }
        if (matcher instanceof dev.darkblade.mbe.core.domain.rule.BlockDataMatcher m) {
            return m.expectedData() == null ? "" : m.expectedData().getAsString();
        }
        if (matcher instanceof dev.darkblade.mbe.core.domain.rule.AnyOfMatcher m) {
            List<String> parts = new ArrayList<>();
            for (dev.darkblade.mbe.core.domain.BlockMatcher sub : m.matchers()) {
                String s = matcherKey(sub);
                if (!s.isBlank()) {
                    parts.add(s);
                }
            }
            parts.sort(String::compareToIgnoreCase);
            return String.join("|", parts);
        }
        return matcher.getClass().getSimpleName();
    }

    private String sha256(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(d.length * 2);
            for (byte b : d) {
                String h = Integer.toHexString(b & 0xFF);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return s;
        }
    }
}
