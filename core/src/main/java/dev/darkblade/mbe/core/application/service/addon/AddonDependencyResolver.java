package dev.darkblade.mbe.core.application.service.addon;

import dev.darkblade.mbe.api.addon.Version;
import dev.darkblade.mbe.core.graph.dependency.DependencyEdge;
import dev.darkblade.mbe.core.graph.dependency.DependencyGraphResolver;
import dev.darkblade.mbe.core.graph.dependency.DependencyMode;
import dev.darkblade.mbe.core.graph.dependency.DependencyNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AddonDependencyResolver {

    public record Resolution(
        List<String> loadOrder,
        Map<String, String> failures,
        List<String> warnings
    ) {
    }

    public Resolution resolve(int apiVersion, Map<String, AddonMetadata> metadataById) {
        Map<String, String> failures = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        Map<String, AddonMetadata> eligible = new HashMap<>(metadataById);

        for (AddonMetadata meta : metadataById.values()) {
            if (meta.api() != apiVersion) {
                failures.put(meta.id(), "API " + apiVersion + " is not compatible (addon requires API " + meta.api() + ")");
                eligible.remove(meta.id());
            }
        }

        List<DependencyNode<AddonMetadata>> nodes = new ArrayList<>();
        for (AddonMetadata meta : eligible.values()) {
            boolean valid = true;
            for (Map.Entry<String, Version> dep : meta.requiredDependencies().entrySet()) {
                String depId = dep.getKey();
                Version min = dep.getValue();
                AddonMetadata found = metadataById.get(depId);
                if (found != null && !found.version().isAtLeast(min)) {
                    failures.put(meta.id(), "Required dependency " + depId + " >=" + min + " not satisfied (found " + found.version() + ")");
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                continue;
            }

            nodes.add(new AddonNode(meta, eligible));
        }

        DependencyGraphResolver<AddonMetadata> resolver = new DependencyGraphResolver<>();
        DependencyGraphResolver.ResolutionResult<AddonMetadata> result = resolver.resolve(nodes);

        failures.putAll(result.failures());
        warnings.addAll(result.warnings());

        List<String> loadOrder = new ArrayList<>();
        for (DependencyNode<AddonMetadata> node : result.orderedNodes()) {
            loadOrder.add(node.id());
        }

        for (AddonMetadata meta : metadataById.values()) {
            if (failures.containsKey(meta.id())) {
                continue;
            }
            warnings.addAll(optionalDependencyWarnings(meta, metadataById, loadOrder));
        }

        return new Resolution(List.copyOf(loadOrder), Map.copyOf(failures), List.copyOf(warnings));
    }

    private static List<String> optionalDependencyWarnings(
        AddonMetadata meta,
        Map<String, AddonMetadata> all,
        List<String> loadOrder
    ) {
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, Version> dep : meta.optionalDependencies().entrySet()) {
            String depId = dep.getKey();
            Version min = dep.getValue();

            AddonMetadata found = all.get(depId);
            if (found == null) {
                warnings.add(" Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not found (feature disabled)");
                continue;
            }

            if (!loadOrder.contains(depId)) {
                warnings.add(" Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not enabled (feature disabled)");
                continue;
            }

            if (!found.version().isAtLeast(min)) {
                warnings.add(" Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not satisfied (found " + found.version() + ") (feature disabled)");
            }
        }
        return warnings;
    }

    private static class AddonNode implements DependencyNode<AddonMetadata> {
        private final AddonMetadata meta;
        private final List<DependencyEdge> edges = new ArrayList<>();

        public AddonNode(AddonMetadata meta, Map<String, AddonMetadata> eligible) {
            this.meta = meta;
            for (String req : meta.requiredDependencies().keySet()) {
                edges.add(new DependencyEdge(req, true, DependencyMode.LOAD_TIME));
            }
            for (Map.Entry<String, Version> opt : meta.optionalDependencies().entrySet()) {
                String depId = opt.getKey();
                Version min = opt.getValue();
                AddonMetadata found = eligible.get(depId);
                if (found != null && found.version().isAtLeast(min)) {
                    edges.add(new DependencyEdge(depId, false, DependencyMode.LOAD_TIME));
                }
            }
        }

        @Override
        public String id() {
            return meta.id();
        }

        @Override
        public AddonMetadata payload() {
            return meta;
        }

        @Override
        public Collection<DependencyEdge> edges() {
            return edges;
        }
    }
}
