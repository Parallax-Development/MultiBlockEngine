package dev.darkblade.mbe.core.application.service.wiring;

import dev.darkblade.mbe.api.io.event.IONetworkMergeEvent;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.NetworkConnection;
import dev.darkblade.mbe.api.wiring.NetworkGraph;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import org.bukkit.block.Block;
import org.bukkit.event.Event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class DefaultNetworkService implements NetworkService {

    private final Consumer<Event> eventCaller;
    private final ConcurrentHashMap<NetworkType, TopologyState> topologies = new ConcurrentHashMap<>();

    public DefaultNetworkService(Consumer<Event> eventCaller) {
        this.eventCaller = Objects.requireNonNull(eventCaller, "eventCaller");
    }

    private TopologyState getTopology(NetworkType type) {
        if (type == null) {
            throw new IllegalArgumentException("NetworkType cannot be null");
        }
        return topologies.computeIfAbsent(type, unused -> new TopologyState(type, eventCaller));
    }

    @Override
    public NetworkNode registerNode(NetworkType type, Block block, NodeDescriptor descriptor) {
        return getTopology(type).registerNode(block, descriptor);
    }

    @Override
    public void unregisterNode(NetworkType type, NetworkNode node) {
        getTopology(type).unregisterNode(node);
    }

    @Override
    public boolean connect(NetworkType type, NetworkNode a, NetworkNode b) {
        return getTopology(type).connect(a, b);
    }

    @Override
    public void disconnect(NetworkType type, NetworkNode a, NetworkNode b) {
        getTopology(type).disconnect(a, b);
    }

    @Override
    public NetworkGraph getGraph(NetworkType type, NetworkNode node) {
        return getTopology(type).getGraph(node);
    }

    @Override
    public Optional<NetworkNode> findNode(NetworkType type, Block block) {
        return getTopology(type).findNode(block);
    }

    @Override
    public Collection<NetworkNode> findAllNodes(Block block) {
        List<NetworkNode> result = new ArrayList<>();
        for (TopologyState topology : topologies.values()) {
            topology.findNode(block).ifPresent(result::add);
        }
        return result;
    }

    public UUID networkId(NetworkType type, NetworkNode node) {
        return getTopology(type).networkId(node);
    }

    public Collection<NetworkNode> neighbors(NetworkType type, NetworkNode node) {
        return getTopology(type).neighbors(node);
    }

    private static class TopologyState {
        private final NetworkType type;
        private final Consumer<Event> eventCaller;
        private final ConcurrentHashMap<UUID, NodeImpl> nodesById = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<BlockPos, UUID> nodeIndex = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<UUID, Set<UUID>> adjacency = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<EdgeKey, ConnectionImpl> connections = new ConcurrentHashMap<>();
        private volatile Map<UUID, UUID> networkByNode = Map.of();
        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();

        public TopologyState(NetworkType type, Consumer<Event> eventCaller) {
            this.type = type;
            this.eventCaller = eventCaller;
        }

        public NetworkNode registerNode(Block block, NodeDescriptor descriptor) {
            if (block == null || block.getWorld() == null) {
                throw new IllegalArgumentException("block");
            }
            lock.lock();
            try {
                NodeDescriptor safeDescriptor = descriptor == null ? new NodeDescriptor(Set.of()) : descriptor;
                BlockPos position = new BlockPos(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
                UUID existing = nodeIndex.get(position);
                if (existing != null) {
                    NodeImpl found = nodesById.get(existing);
                    if (found != null) {
                        NodeImpl updated = new NodeImpl(found.id(), type, position, safeDescriptor.connectableFaces());
                        nodesById.put(updated.id(), updated);
                        return updated;
                    }
                }
                UUID id = UUID.randomUUID();
                NodeImpl created = new NodeImpl(id, type, position, safeDescriptor.connectableFaces());
                nodesById.put(id, created);
                nodeIndex.put(position, id);
                adjacency.putIfAbsent(id, ConcurrentHashMap.newKeySet());
                
                Map<UUID, UUID> next = new HashMap<>(networkByNode);
                next.put(id, UUID.randomUUID());
                this.networkByNode = Map.copyOf(next);
                return created;
            } finally {
                lock.unlock();
            }
        }

        public void unregisterNode(NetworkNode node) {
            lock.lock();
            try {
                NodeImpl resolved = resolve(node);
                if (resolved == null) {
                    return;
                }
                UUID id = resolved.id();
                Set<UUID> neighbors = new HashSet<>(adjacency.getOrDefault(id, Set.of()));
                for (UUID neighbor : neighbors) {
                    disconnectById(id, neighbor);
                }
                adjacency.remove(id);
                nodesById.remove(id);
                nodeIndex.remove(resolved.position());
                recomputeNetworks();
            } finally {
                lock.unlock();
            }
        }

        public boolean connect(NetworkNode a, NetworkNode b) {
            lock.lock();
            try {
                NodeImpl left = resolve(a);
                NodeImpl right = resolve(b);
                if (left == null || right == null || left.id().equals(right.id())) {
                    return false;
                }
                EdgeKey key = EdgeKey.of(left.id(), right.id());
                if (connections.containsKey(key)) {
                    return false;
                }
                UUID leftNetwork = networkByNode.get(left.id());
                UUID rightNetwork = networkByNode.get(right.id());
                adjacency.computeIfAbsent(left.id(), unused -> ConcurrentHashMap.newKeySet()).add(right.id());
                adjacency.computeIfAbsent(right.id(), unused -> ConcurrentHashMap.newKeySet()).add(left.id());
                connections.put(key, new ConnectionImpl(UUID.randomUUID(), type, left, right));
                recomputeNetworks();
                if (leftNetwork != null && rightNetwork != null && !leftNetwork.equals(rightNetwork)) {
                    eventCaller.accept(new IONetworkMergeEvent(type, rightNetwork, leftNetwork));
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        public void disconnect(NetworkNode a, NetworkNode b) {
            lock.lock();
            try {
                NodeImpl left = resolve(a);
                NodeImpl right = resolve(b);
                if (left == null || right == null || left.id().equals(right.id())) {
                    return;
                }
                disconnectById(left.id(), right.id());
                recomputeNetworks();
            } finally {
                lock.unlock();
            }
        }

        public NetworkGraph getGraph(NetworkNode node) {
            NodeImpl start = resolve(node);
            if (start == null) {
                return new GraphImpl(UUID.randomUUID(), type, Set.of(), Set.of());
            }
            UUID networkId = networkByNode.getOrDefault(start.id(), UUID.randomUUID());
            Set<UUID> visited = bfs(start.id());
            Set<NetworkNode> graphNodes = new LinkedHashSet<>();
            Set<NetworkConnection> graphConnections = new LinkedHashSet<>();
            for (UUID id : visited) {
                NodeImpl current = nodesById.get(id);
                if (current != null) {
                    graphNodes.add(current);
                }
            }
            for (ConnectionImpl connection : connections.values()) {
                if (visited.contains(connection.from().id()) && visited.contains(connection.to().id())) {
                    graphConnections.add(connection);
                }
            }
            return new GraphImpl(networkId, type, Set.copyOf(graphNodes), Set.copyOf(graphConnections));
        }

        public Optional<NetworkNode> findNode(Block block) {
            if (block == null || block.getWorld() == null) {
                return Optional.empty();
            }
            UUID id = nodeIndex.get(new BlockPos(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()));
            if (id == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(nodesById.get(id));
        }

        public UUID networkId(NetworkNode node) {
            NodeImpl resolved = resolve(node);
            if (resolved == null) {
                return null;
            }
            return networkByNode.get(resolved.id());
        }

        public Collection<NetworkNode> neighbors(NetworkNode node) {
            NodeImpl resolved = resolve(node);
            if (resolved == null) {
                return List.of();
            }
            Set<UUID> ids = adjacency.getOrDefault(resolved.id(), Set.of());
            List<NetworkNode> out = new ArrayList<>();
            for (UUID id : ids) {
                NodeImpl neighbor = nodesById.get(id);
                if (neighbor != null) {
                    out.add(neighbor);
                }
            }
            return List.copyOf(out);
        }

        private void disconnectById(UUID a, UUID b) {
            adjacency.computeIfPresent(a, (id, set) -> {
                set.remove(b);
                return set;
            });
            adjacency.computeIfPresent(b, (id, set) -> {
                set.remove(a);
                return set;
            });
            connections.remove(EdgeKey.of(a, b));
        }

        private NodeImpl resolve(NetworkNode node) {
            if (node == null || node.id() == null || !type.equals(node.type())) {
                return null;
            }
            return nodesById.get(node.id());
        }

        private Set<UUID> bfs(UUID start) {
            Set<UUID> visited = new LinkedHashSet<>();
            ArrayDeque<UUID> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                UUID current = queue.removeFirst();
                if (!visited.add(current)) {
                    continue;
                }
                for (UUID next : adjacency.getOrDefault(current, Set.of())) {
                    if (!visited.contains(next)) {
                        queue.addLast(next);
                    }
                }
            }
            return visited;
        }

        private void recomputeNetworks() {
            Map<UUID, UUID> next = new HashMap<>();
            Set<UUID> pending = new HashSet<>(nodesById.keySet());
            Set<UUID> assigned = new HashSet<>();
            while (!pending.isEmpty()) {
                UUID seed = pending.iterator().next();
                Set<UUID> component = bfs(seed);
                pending.removeAll(component);
                UUID chosen = chooseNetworkId(component, assigned);
                assigned.add(chosen);
                for (UUID id : component) {
                    next.put(id, chosen);
                }
            }
            this.networkByNode = Map.copyOf(next);
        }

        private UUID chooseNetworkId(Set<UUID> component, Set<UUID> assigned) {
            UUID chosen = component.stream()
                    .map(networkByNode::get)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(UUID::toString))
                    .findFirst()
                    .orElse(null);
            if (chosen == null || assigned.contains(chosen)) {
                return UUID.randomUUID();
            }
            return chosen;
        }
    }

    private record EdgeKey(UUID a, UUID b) {
        private static EdgeKey of(UUID left, UUID right) {
            if (left == null || right == null) {
                throw new IllegalArgumentException("edge");
            }
            return left.compareTo(right) <= 0 ? new EdgeKey(left, right) : new EdgeKey(right, left);
        }
    }

    private record NodeImpl(UUID id, NetworkType type, BlockPos position, Set<dev.darkblade.mbe.api.wiring.Direction> connectableFaces) implements NetworkNode {
        private NodeImpl {
            connectableFaces = connectableFaces == null ? Set.of() : Set.copyOf(connectableFaces);
        }
    }

    private record ConnectionImpl(UUID id, NetworkType type, NodeImpl from, NodeImpl to) implements NetworkConnection {
    }

    private record GraphImpl(UUID id, NetworkType type, Set<NetworkNode> nodes, Set<NetworkConnection> connections) implements NetworkGraph {
    }
}
