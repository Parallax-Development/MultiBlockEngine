package dev.darkblade.mbe.core.application.service.addon.crossref;

import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDeclaration;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDependency;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMetrics;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMode;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceResolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AddonCrossReferenceService {
    private static final String REFERENCE_ID_PATTERN = "^[a-z0-9][a-z0-9_\\-]*:[a-z0-9][a-z0-9_.\\-/]*$";

    private final Map<String, Node> nodesByReferenceId = new LinkedHashMap<>();
    private CrossReferenceMetrics metrics = new CrossReferenceMetrics(0, 0, 0, 0L, 0L);

    public synchronized void clear() {
        nodesByReferenceId.clear();
        metrics = new CrossReferenceMetrics(0, 0, 0, 0L, 0L);
    }

    public synchronized <T> void declare(String addonId, CrossReferenceDeclaration<T> declaration) {
        Objects.requireNonNull(addonId, "addonId");
        Objects.requireNonNull(declaration, "declaration");
        String owner = normalizeAddonId(addonId);
        String referenceId = normalizeReferenceId(declaration.referenceId());
        if (!referenceId.startsWith(owner + ":")) {
            throw new IllegalArgumentException("Cross-reference id must start with addon id namespace prefix: " + owner + ":");
        }
        if (nodesByReferenceId.containsKey(referenceId)) {
            throw new IllegalStateException("Cross-reference id already declared: " + referenceId);
        }
        nodesByReferenceId.put(referenceId, new Node(owner, declaration));
    }

    public synchronized CompilationReport compileAndInitialize() {
        long compileStart = System.nanoTime();
        Map<String, List<String>> failuresByAddon = new LinkedHashMap<>();
        Set<String> failedReferences = new LinkedHashSet<>();
        validateRequiredDependencies(failedReferences, failuresByAddon);
        validateInvalidEagerCycles(failedReferences, failuresByAddon);
        long compileNanos = Math.max(0L, System.nanoTime() - compileStart);

        long initializeStart = System.nanoTime();
        initializeReadyReferences(failedReferences, failuresByAddon);
        long initializeNanos = Math.max(0L, System.nanoTime() - initializeStart);

        int initialized = 0;
        for (Node node : nodesByReferenceId.values()) {
            if (node.instance != null) {
                initialized++;
            }
        }
        int failed = failedReferences.size();
        metrics = new CrossReferenceMetrics(nodesByReferenceId.size(), initialized, failed, compileNanos, initializeNanos);
        return new CompilationReport(copyFailures(failuresByAddon), metrics);
    }

    public synchronized <T> Optional<T> resolve(String referenceId, Class<T> type) {
        Objects.requireNonNull(type, "type");
        Node node = nodesByReferenceId.get(normalizeReferenceId(referenceId));
        if (node == null || node.instance == null || !type.isInstance(node.instance)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(node.instance));
    }

    public synchronized <T> CrossReferenceHandle<T> handle(String referenceId, Class<T> type) {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(type, "type");
        String normalized = normalizeReferenceId(referenceId);
        return () -> resolve(normalized, type);
    }

    public synchronized CrossReferenceMetrics metrics() {
        return metrics;
    }

    private void validateRequiredDependencies(Set<String> failedReferences, Map<String, List<String>> failuresByAddon) {
        for (Node node : nodesByReferenceId.values()) {
            for (CrossReferenceDependency dependency : node.dependencies) {
                if (!dependency.required()) {
                    continue;
                }
                if (nodesByReferenceId.containsKey(normalizeReferenceId(dependency.targetReferenceId()))) {
                    continue;
                }
                failedReferences.add(node.referenceId);
                failure(failuresByAddon, node.addonId, "Missing required cross-reference dependency " + dependency.targetReferenceId() + " for " + node.referenceId);
            }
        }
    }

    private void validateInvalidEagerCycles(Set<String> failedReferences, Map<String, List<String>> failuresByAddon) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Node node : nodesByReferenceId.values()) {
            graph.put(node.referenceId, new LinkedHashSet<>());
        }
        for (Node node : nodesByReferenceId.values()) {
            for (CrossReferenceDependency dependency : node.dependencies) {
                if (!dependency.required() || dependency.mode() != CrossReferenceMode.EAGER) {
                    continue;
                }
                String targetId = normalizeReferenceId(dependency.targetReferenceId());
                if (!nodesByReferenceId.containsKey(targetId)) {
                    continue;
                }
                graph.get(node.referenceId).add(targetId);
            }
        }

        List<Set<String>> components = stronglyConnectedComponents(graph);
        for (Set<String> component : components) {
            if (component.size() > 1) {
                for (String referenceId : component) {
                    Node node = nodesByReferenceId.get(referenceId);
                    if (node != null) {
                        failedReferences.add(node.referenceId);
                        failure(failuresByAddon, node.addonId, "Invalid eager circular cross-reference detected for " + referenceId + " in cycle " + component);
                    }
                }
                continue;
            }

            String only = component.iterator().next();
            Set<String> edges = graph.getOrDefault(only, Set.of());
            if (edges.contains(only)) {
                Node node = nodesByReferenceId.get(only);
                if (node != null) {
                    failedReferences.add(node.referenceId);
                    failure(failuresByAddon, node.addonId, "Invalid eager self-reference cycle detected for " + only);
                }
            }
        }
    }

    private void initializeReadyReferences(Set<String> failedReferences, Map<String, List<String>> failuresByAddon) {
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (Node node : nodesByReferenceId.values()) {
                if (node.instance != null || failedReferences.contains(node.referenceId)) {
                    continue;
                }
                if (!canInstantiate(node, failedReferences)) {
                    continue;
                }
                try {
                    Object created = node.factory.apply(new ResolverView(this));
                    if (created == null) {
                        failedReferences.add(node.referenceId);
                        failure(failuresByAddon, node.addonId, "Cross-reference factory returned null for " + node.referenceId);
                        continue;
                    }
                    if (!node.contractType.isInstance(created)) {
                        failedReferences.add(node.referenceId);
                        failure(failuresByAddon, node.addonId, "Cross-reference factory type mismatch for " + node.referenceId + ". Expected " + node.contractType.getName() + " but found " + created.getClass().getName());
                        continue;
                    }
                    node.instance = created;
                    progressed = true;
                } catch (Throwable t) {
                    failedReferences.add(node.referenceId);
                    failure(failuresByAddon, node.addonId, "Cross-reference factory failed for " + node.referenceId + ": " + t.getClass().getSimpleName() + " - " + String.valueOf(t.getMessage()));
                }
            }
        }

        for (Node node : nodesByReferenceId.values()) {
            if (node.instance != null || failedReferences.contains(node.referenceId)) {
                continue;
            }
            if (hasPendingRequiredEagerDependency(node, failedReferences)) {
                failedReferences.add(node.referenceId);
                failure(failuresByAddon, node.addonId, "Cross-reference " + node.referenceId + " was not initialized because required eager dependencies were not satisfied");
            }
        }
    }

    private boolean hasPendingRequiredEagerDependency(Node node, Set<String> failedReferences) {
        for (CrossReferenceDependency dependency : node.dependencies) {
            if (!dependency.required() || dependency.mode() != CrossReferenceMode.EAGER) {
                continue;
            }
            Node dep = nodesByReferenceId.get(normalizeReferenceId(dependency.targetReferenceId()));
            if (dep == null || dep.instance == null || failedReferences.contains(dep.referenceId)) {
                return true;
            }
        }
        return false;
    }

    private boolean canInstantiate(Node node, Set<String> failedReferences) {
        for (CrossReferenceDependency dependency : node.dependencies) {
            if (!dependency.required() || dependency.mode() != CrossReferenceMode.EAGER) {
                continue;
            }
            String targetId = normalizeReferenceId(dependency.targetReferenceId());
            if (failedReferences.contains(targetId)) {
                return false;
            }
            Node dep = nodesByReferenceId.get(targetId);
            if (dep == null || dep.instance == null) {
                return false;
            }
        }
        return true;
    }

    private static void failure(Map<String, List<String>> failuresByAddon, String addonId, String message) {
        failuresByAddon.computeIfAbsent(addonId, k -> new ArrayList<>()).add(message);
    }

    private static Map<String, List<String>> copyFailures(Map<String, List<String>> failuresByAddon) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : failuresByAddon.entrySet()) {
            out.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Map.copyOf(out);
    }

    private static String normalizeAddonId(String addonId) {
        Objects.requireNonNull(addonId, "addonId");
        String normalized = addonId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("addonId");
        }
        return normalized;
    }

    private static String normalizeReferenceId(String referenceId) {
        Objects.requireNonNull(referenceId, "referenceId");
        String normalized = referenceId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank() || !normalized.matches(REFERENCE_ID_PATTERN)) {
            throw new IllegalArgumentException("Invalid cross-reference id: " + referenceId + " (expected namespace:path)");
        }
        return normalized;
    }

    private static List<Set<String>> stronglyConnectedComponents(Map<String, Set<String>> graph) {
        Map<String, Integer> indexByNode = new HashMap<>();
        Map<String, Integer> lowByNode = new HashMap<>();
        ArrayDeque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new HashSet<>();
        List<Set<String>> out = new ArrayList<>();
        int[] index = {0};

        for (String node : graph.keySet()) {
            if (!indexByNode.containsKey(node)) {
                sccDfs(node, graph, indexByNode, lowByNode, stack, onStack, out, index);
            }
        }
        return out;
    }

    private static void sccDfs(
        String node,
        Map<String, Set<String>> graph,
        Map<String, Integer> indexByNode,
        Map<String, Integer> lowByNode,
        ArrayDeque<String> stack,
        Set<String> onStack,
        List<Set<String>> out,
        int[] index
    ) {
        indexByNode.put(node, index[0]);
        lowByNode.put(node, index[0]);
        index[0]++;
        stack.push(node);
        onStack.add(node);

        for (String next : graph.getOrDefault(node, Set.of())) {
            if (!indexByNode.containsKey(next)) {
                sccDfs(next, graph, indexByNode, lowByNode, stack, onStack, out, index);
                lowByNode.put(node, Math.min(lowByNode.get(node), lowByNode.get(next)));
            } else if (onStack.contains(next)) {
                lowByNode.put(node, Math.min(lowByNode.get(node), indexByNode.get(next)));
            }
        }

        if (Objects.equals(lowByNode.get(node), indexByNode.get(node))) {
            Set<String> component = new LinkedHashSet<>();
            while (!stack.isEmpty()) {
                String n = stack.pop();
                onStack.remove(n);
                component.add(n);
                if (n.equals(node)) {
                    break;
                }
            }
            out.add(component);
        }
    }

    public record CompilationReport(
        Map<String, List<String>> failuresByAddon,
        CrossReferenceMetrics metrics
    ) {
        public boolean successful() {
            return failuresByAddon.isEmpty();
        }

        public List<String> failuresFor(String addonId) {
            if (addonId == null) {
                return List.of();
            }
            return failuresByAddon.getOrDefault(addonId.toLowerCase(java.util.Locale.ROOT), List.of());
        }
    }

    private static final class ResolverView implements CrossReferenceResolver {
        private final AddonCrossReferenceService manager;

        private ResolverView(AddonCrossReferenceService manager) {
            this.manager = manager;
        }

        @Override
        public <T> Optional<T> resolve(String referenceId, Class<T> type) {
            return manager.resolve(referenceId, type);
        }

        @Override
        public <T> CrossReferenceHandle<T> handle(String referenceId, Class<T> type) {
            return manager.handle(referenceId, type);
        }
    }

    private static final class Node {
        private final String addonId;
        private final String referenceId;
        private final Class<?> contractType;
        private final java.util.function.Function<CrossReferenceResolver, ?> factory;
        private final List<CrossReferenceDependency> dependencies;
        private Object instance;

        private Node(String addonId, CrossReferenceDeclaration<?> declaration) {
            this.addonId = addonId;
            this.referenceId = normalizeReferenceId(declaration.referenceId());
            this.contractType = declaration.contractType();
            this.factory = declaration.factory();
            this.dependencies = declaration.dependencies() == null ? List.of() : List.copyOf(declaration.dependencies());
        }
    }
}
