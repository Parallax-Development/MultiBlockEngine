package dev.darkblade.mbe.core.graph.dependency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

public final class DependencyGraphResolver<T> {

    public record ResolutionResult<T>(
        List<DependencyNode<T>> orderedNodes,
        Map<String, String> failures,
        List<String> warnings
    ) {
    }

    public ResolutionResult<T> resolve(Collection<? extends DependencyNode<T>> initialNodes) {
        Map<String, String> failures = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, DependencyNode<T>> eligible = new HashMap<>();

        for (DependencyNode<T> node : initialNodes) {
            eligible.put(node.id(), node);
        }

        // 1. Transitive removal of nodes with missing required dependencies
        boolean progressed = true;
        while (progressed) {
            progressed = false;
            for (DependencyNode<T> node : List.copyOf(eligible.values())) {
                for (DependencyEdge edge : node.edges()) {
                    if (edge.required()) {
                        DependencyNode<T> target = eligible.get(edge.targetId());
                        if (target == null) {
                            eligible.remove(node.id());
                            failures.put(node.id(), "Missing required dependency: " + edge.targetId());
                            progressed = true;
                            break;
                        }
                    }
                }
            }
        }

        // 2. Build graph of HARD edges for eligible nodes
        Map<String, Set<String>> graph = new HashMap<>();
        for (String id : eligible.keySet()) {
            graph.put(id, new HashSet<>());
        }

        for (DependencyNode<T> node : eligible.values()) {
            for (DependencyEdge edge : node.edges()) {
                if (edge.required() && edge.mode() != DependencyMode.RUNTIME_LAZY) {
                    if (eligible.containsKey(edge.targetId())) {
                        graph.get(node.id()).add(edge.targetId());
                    }
                }
            }
        }

        // 3. Find invalid cycles using Tarjan's SCC
        List<Set<String>> components = stronglyConnectedComponents(graph);
        for (Set<String> component : components) {
            if (component.size() > 1) {
                for (String id : component) {
                    eligible.remove(id);
                    failures.put(id, "Dependency cycle detected in component: " + component);
                }
            } else {
                String only = component.iterator().next();
                if (graph.get(only).contains(only)) { // Self-reference
                    eligible.remove(only);
                    failures.put(only, "Self-reference cycle detected");
                }
            }
        }

        // Transitive removal after cycle detection
        progressed = true;
        while (progressed) {
            progressed = false;
            for (DependencyNode<T> node : List.copyOf(eligible.values())) {
                for (DependencyEdge edge : node.edges()) {
                    if (edge.required()) {
                        DependencyNode<T> target = eligible.get(edge.targetId());
                        if (target == null) {
                            eligible.remove(node.id());
                            failures.put(node.id(), "Missing required dependency (removed due to cycle): " + edge.targetId());
                            progressed = true;
                            break;
                        }
                    }
                }
            }
        }

        // Re-build reverse graph for remaining eligible nodes to do Kahn's topological sort
        Map<String, Set<String>> reverseGraph = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String id : eligible.keySet()) {
            reverseGraph.put(id, new HashSet<>());
            indegree.put(id, 0);
        }

        for (DependencyNode<T> node : eligible.values()) {
            for (DependencyEdge edge : node.edges()) {
                if (edge.required() && edge.mode() != DependencyMode.RUNTIME_LAZY) {
                    if (eligible.containsKey(edge.targetId())) {
                        reverseGraph.get(edge.targetId()).add(node.id()); // Reverse mapping: Dependency -> Dependent
                        indegree.put(node.id(), indegree.get(node.id()) + 1);
                    }
                }
            }
        }

        // 4. Topological Sort (Kahn's algorithm)
        PriorityQueue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<DependencyNode<T>> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            DependencyNode<T> node = eligible.get(id);
            order.add(node);

            List<String> sortedDependents = new ArrayList<>(reverseGraph.getOrDefault(id, Set.of()));
            sortedDependents.sort(String::compareTo);
            for (String dependent : sortedDependents) {
                int next = indegree.computeIfPresent(dependent, (k, v) -> v - 1);
                if (next == 0) {
                    queue.add(dependent);
                }
            }
        }

        // 5. Apply soft ordering (optional dependencies)
        applySoftOrdering(order, eligible, warnings);

        return new ResolutionResult<>(order, failures, warnings);
    }

    private void applySoftOrdering(List<DependencyNode<T>> order, Map<String, DependencyNode<T>> eligible, List<String> warnings) {
        int limit = Math.max(1, order.size() * order.size());
        int passes = 0;
        boolean changed;

        do {
            changed = false;
            passes++;

            for (DependencyNode<T> node : List.copyOf(order)) {
                List<String> optionalDeps = new ArrayList<>();
                for (DependencyEdge edge : node.edges()) {
                    if (!edge.required() && edge.mode() == DependencyMode.LOAD_TIME) {
                        optionalDeps.add(edge.targetId());
                    }
                }
                optionalDeps.sort(String::compareTo);

                for (String depId : optionalDeps) {
                    if (!eligible.containsKey(depId)) {
                        continue;
                    }

                    int idx = order.indexOf(node);
                    int depIdx = -1;
                    for (int i = 0; i < order.size(); i++) {
                        if (order.get(i).id().equals(depId)) {
                            depIdx = i;
                            break;
                        }
                    }

                    if (depIdx < 0 || idx < 0) {
                        continue;
                    }

                    if (depIdx > idx) {
                        order.remove(idx);
                        int newDepIdx = -1;
                        for (int i = 0; i < order.size(); i++) {
                            if (order.get(i).id().equals(depId)) {
                                newDepIdx = i;
                                break;
                            }
                        }
                        if (newDepIdx < 0) {
                            order.add(node);
                        } else {
                            order.add(newDepIdx + 1, node);
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
            String sample = String.join(", ", order.stream().limit(10).map(DependencyNode::id).toList());
            warnings.add(" Optional dependency ordering may be unstable; partial order sample: " + sample);
        }
    }

    private List<Set<String>> stronglyConnectedComponents(Map<String, Set<String>> graph) {
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

    private void sccDfs(
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
}
