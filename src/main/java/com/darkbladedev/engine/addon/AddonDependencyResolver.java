package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.addon.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class AddonDependencyResolver {

    public record Resolution(
        List<String> loadOrder,
        Map<String, String> failures,
        List<String> warnings
    ) {
    }

    public Resolution resolve(int coreApiVersion, Map<String, AddonMetadata> metadataById) {
        Map<String, String> failures = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        Map<String, AddonMetadata> eligible = new HashMap<>(metadataById);

        for (AddonMetadata meta : metadataById.values()) {
            if (meta.api() != coreApiVersion) {
                failures.put(meta.id(), "Core API " + coreApiVersion + " is not compatible (addon requires API " + meta.api() + ")");
                eligible.remove(meta.id());
            }
        }

        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (AddonMetadata meta : List.copyOf(eligible.values())) {
                String id = meta.id();

                if (hasMissingRequiredDependency(meta, eligible, metadataById, failures)) {
                    eligible.remove(id);
                    progressed = true;
                }
            }
        }

        Map<String, Set<String>> dependents = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();

        for (String id : eligible.keySet()) {
            dependents.put(id, new HashSet<>());
            indegree.put(id, 0);
        }

        for (AddonMetadata meta : eligible.values()) {
            for (String depId : meta.requiredDependencies().keySet()) {
                if (!eligible.containsKey(depId)) {
                    continue;
                }
                dependents.get(depId).add(meta.id());
                indegree.put(meta.id(), indegree.get(meta.id()) + 1);
            }
        }

        PriorityQueue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            order.add(id);

            List<String> sortedDependents = new ArrayList<>(dependents.getOrDefault(id, Set.of()));
            sortedDependents.sort(String::compareTo);
            for (String dependent : sortedDependents) {
                int next = indegree.computeIfPresent(dependent, (k, v) -> v - 1);
                if (next == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (order.size() != eligible.size()) {
            Set<String> cycleNodes = new HashSet<>(eligible.keySet());
            cycleNodes.removeAll(order);
            for (String id : cycleNodes) {
                failures.put(id, "Dependency cycle detected");
            }
        }

        if (order.size() == eligible.size()) {
            boolean stabilized = applyOptionalOrdering(order, eligible, warnings);
            if (!stabilized) {
                warnings.add("[MultiBlockEngine] Optional dependency ordering could not be fully stabilized due to an optional cycle");
            }
        }

        for (AddonMetadata meta : metadataById.values()) {
            if (failures.containsKey(meta.id())) {
                continue;
            }
            warnings.addAll(optionalDependencyWarnings(meta, eligible, metadataById));
        }

        return new Resolution(List.copyOf(order), Map.copyOf(failures), List.copyOf(warnings));
    }

    private static boolean applyOptionalOrdering(List<String> order, Map<String, AddonMetadata> eligible, List<String> warnings) {
        int limit = Math.max(1, order.size() * order.size());
        int passes = 0;
        boolean changed;

        do {
            changed = false;
            passes++;

            for (String id : List.copyOf(order)) {
                AddonMetadata meta = eligible.get(id);
                if (meta == null) {
                    continue;
                }

                List<String> optionalDeps = new ArrayList<>(meta.optionalDependencies().keySet());
                optionalDeps.sort(String::compareTo);

                for (String depId : optionalDeps) {
                    AddonMetadata depMeta = eligible.get(depId);
                    if (depMeta == null) {
                        continue;
                    }

                    Version min = meta.optionalDependencies().get(depId);
                    if (min != null && !depMeta.version().isAtLeast(min)) {
                        continue;
                    }

                    int idx = order.indexOf(id);
                    int depIdx = order.indexOf(depId);
                    if (depIdx < 0 || idx < 0) {
                        continue;
                    }

                    if (depIdx > idx) {
                        order.remove(idx);
                        int newDepIdx = order.indexOf(depId);
                        if (newDepIdx < 0) {
                            order.add(id);
                        } else {
                            order.add(newDepIdx + 1, id);
                        }
                        changed = true;
                        break;
                    }
                }

                if (changed) {
                    break;
                }
            }
        } while (changed && passes < limit);

        if (changed) {
            String sample = String.join(", ", order.stream().limit(10).toList());
            warnings.add("[MultiBlockEngine] Optional dependency ordering may be unstable; partial order sample: " + sample);
            return false;
        }

        return true;
    }

    private static boolean hasMissingRequiredDependency(
        AddonMetadata meta,
        Map<String, AddonMetadata> eligible,
        Map<String, AddonMetadata> all,
        Map<String, String> failures
    ) {
        for (Map.Entry<String, Version> dep : meta.requiredDependencies().entrySet()) {
            String depId = dep.getKey();
            Version min = dep.getValue();

            AddonMetadata found = all.get(depId);
            if (found == null) {
                failures.put(meta.id(), "Missing required dependency " + depId + " >=" + min);
                return true;
            }

            if (!eligible.containsKey(depId)) {
                failures.put(meta.id(), "Missing required dependency " + depId + " >=" + min);
                return true;
            }

            if (!found.version().isAtLeast(min)) {
                failures.put(meta.id(), "Required dependency " + depId + " >=" + min + " not satisfied (found " + found.version() + ")");
                return true;
            }
        }

        return false;
    }

    private static List<String> optionalDependencyWarnings(
        AddonMetadata meta,
        Map<String, AddonMetadata> eligible,
        Map<String, AddonMetadata> all
    ) {
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, Version> dep : meta.optionalDependencies().entrySet()) {
            String depId = dep.getKey();
            Version min = dep.getValue();

            AddonMetadata found = all.get(depId);
            if (found == null) {
                warnings.add("[MultiBlockEngine] Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not found (feature disabled)");
                continue;
            }

            if (!eligible.containsKey(depId)) {
                warnings.add("[MultiBlockEngine] Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not enabled (feature disabled)");
                continue;
            }

            if (!found.version().isAtLeast(min)) {
                warnings.add("[MultiBlockEngine] Addon " + meta.id() + " Optional dependency " + depId + " >=" + min + " not satisfied (found " + found.version() + ") (feature disabled)");
            }
        }
        return warnings;
    }
}
