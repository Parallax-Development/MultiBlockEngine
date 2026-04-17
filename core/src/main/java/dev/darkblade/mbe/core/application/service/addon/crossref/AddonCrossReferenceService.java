package dev.darkblade.mbe.core.application.service.addon.crossref;

import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDeclaration;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceDependency;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMetrics;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceMode;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceResolver;
import dev.darkblade.mbe.core.graph.dependency.DependencyEdge;
import dev.darkblade.mbe.core.graph.dependency.DependencyGraphResolver;
import dev.darkblade.mbe.core.graph.dependency.DependencyNode;

import java.util.ArrayList;
import java.util.Collection;
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
        
        DependencyGraphResolver<Node> resolver = new DependencyGraphResolver<>();
        DependencyGraphResolver.ResolutionResult<Node> result = resolver.resolve(nodesByReferenceId.values());
        
        for (Map.Entry<String, String> failure : result.failures().entrySet()) {
            failedReferences.add(failure.getKey());
            Node n = nodesByReferenceId.get(failure.getKey());
            if (n != null) {
                failure(failuresByAddon, n.addonId, failure.getValue());
            }
        }
        
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

    // validateRequiredDependencies and validateInvalidEagerCycles were removed and delegated to DependencyGraphResolver

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

    // stronglyConnectedComponents and sccDfs were removed and delegated to DependencyGraphResolver

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

    private static final class Node implements DependencyNode<Node> {
        private final String addonId;
        private final String referenceId;
        private final Class<?> contractType;
        private final java.util.function.Function<CrossReferenceResolver, ?> factory;
        private final List<CrossReferenceDependency> dependencies;
        private final List<DependencyEdge> edges = new ArrayList<>();
        private Object instance;

        private Node(String addonId, CrossReferenceDeclaration<?> declaration) {
            this.addonId = addonId;
            this.referenceId = normalizeReferenceId(declaration.referenceId());
            this.contractType = declaration.contractType();
            this.factory = declaration.factory();
            this.dependencies = declaration.dependencies() == null ? List.of() : List.copyOf(declaration.dependencies());
            for (CrossReferenceDependency dep : this.dependencies) {
                // CrossReferenceMode is converted to DependencyMode
                dev.darkblade.mbe.core.graph.dependency.DependencyMode internalMode;
                if (dep.mode() == CrossReferenceMode.EAGER) {
                    internalMode = dev.darkblade.mbe.core.graph.dependency.DependencyMode.RUNTIME_EAGER;
                } else {
                    internalMode = dev.darkblade.mbe.core.graph.dependency.DependencyMode.RUNTIME_LAZY;
                }
                this.edges.add(new DependencyEdge(dep.targetReferenceId(), dep.required(), internalMode));
            }
        }

        @Override
        public String id() {
            return referenceId;
        }

        @Override
        public Node payload() {
            return this;
        }

        @Override
        public Collection<DependencyEdge> edges() {
            return edges;
        }
    }
}
