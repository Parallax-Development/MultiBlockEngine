package dev.darkblade.mbe.core.infrastructure.integration;

import dev.darkblade.mbe.api.event.EventBusService;
import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.event.MultiblockFormEvent;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.NodeDescriptor;
import dev.darkblade.mbe.api.wiring.PortDefinition;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MultiblockWiringBridge {

    private final NetworkService networkService;
    private final PortResolutionService portResolutionService;
    private final Map<UUID, Map<Location, NetworkNode>> registeredMultiblockNodes = new ConcurrentHashMap<>();

    public MultiblockWiringBridge(
            EventBusService eventBus,
            NetworkService networkService,
            PortResolutionService portResolutionService
    ) {
        this.networkService = Objects.requireNonNull(networkService, "networkService");
        this.portResolutionService = Objects.requireNonNull(portResolutionService, "portResolutionService");

        if (eventBus != null) {
            eventBus.subscribe(MultiblockFormEvent.class, this::onMultiblockForm);
            eventBus.subscribe(MultiblockBreakEvent.class, this::onMultiblockBreak);
        }
    }

    public void onMultiblockForm(MultiblockFormEvent event) {
        MultiblockInstance instance = event.getMultiblock();
        if (instance == null || instance.type() == null) {
            return;
        }

        UUID instanceId = instanceIdFor(instance);
        Map<Location, NetworkNode> nodes = new HashMap<>();

        for (PortResolutionService.ResolvedPort resolved : portResolutionService.resolveAll(instance)) {
            PortDefinition definition = resolved.definition();
            Location location = resolved.location();
            if (definition == null || location == null || location.getWorld() == null) {
                continue;
            }

            Block block = location.getBlock();
            NetworkType networkType = parseNetworkType(definition.type());

            NodeDescriptor descriptor = new NodeDescriptor(Set.of(
                    Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN
            ));

            NetworkNode node = networkService.registerNode(networkType, block, descriptor);
            if (node != null) {
                nodes.put(location, node);
                // Connect to any adjacent cable nodes that are already registered
                for (Direction dir : descriptor.connectableFaces()) {
                    Block adjacent = block.getRelative(dir.toBlockFace());
                    networkService.findNode(networkType, adjacent)
                            .ifPresent(adj -> networkService.connect(networkType, node, adj));
                }
            }
        }

        if (!nodes.isEmpty()) {
            registeredMultiblockNodes.put(instanceId, nodes);

            // Connect internal port nodes of the same network type to each other (internal bus)
            Map<NetworkType, List<NetworkNode>> nodesByType = new HashMap<>();
            for (NetworkNode n : nodes.values()) {
                nodesByType.computeIfAbsent(n.type(), unused -> new ArrayList<>()).add(n);
            }
            for (List<NetworkNode> typeNodes : nodesByType.values()) {
                for (int i = 0; i < typeNodes.size(); i++) {
                    for (int j = i + 1; j < typeNodes.size(); j++) {
                        networkService.connect(typeNodes.get(i).type(), typeNodes.get(i), typeNodes.get(j));
                    }
                }
            }
        }
    }

    public void onMultiblockBreak(MultiblockBreakEvent event) {
        MultiblockInstance instance = event.getMultiblock();
        if (instance == null) {
            return;
        }

        UUID instanceId = instanceIdFor(instance);
        Map<Location, NetworkNode> nodes = registeredMultiblockNodes.remove(instanceId);
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (NetworkNode node : nodes.values()) {
            if (node != null) {
                networkService.unregisterNode(node.type(), node);
            }
        }
    }

    public Map<UUID, Map<Location, NetworkNode>> getRegisteredNodes() {
        return Map.copyOf(registeredMultiblockNodes);
    }

    private static NetworkType parseNetworkType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return NetworkType.ENERGY;
        }
        String normalized = rawType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "energy" -> NetworkType.ENERGY;
            case "information", "data" -> NetworkType.INFORMATION;
            case "item" -> NetworkType.ITEM;
            case "fluid" -> NetworkType.FLUID;
            default -> new NetworkType(normalized);
        };
    }

    private static UUID instanceIdFor(MultiblockInstance instance) {
        Location anchor = instance.anchorLocation();
        if (anchor == null || anchor.getWorld() == null) {
            return UUID.randomUUID();
        }
        String key = instance.type().id() + "|" + anchor.getWorld().getUID() + "|" + anchor.getBlockX() + "|" + anchor.getBlockY() + "|" + anchor.getBlockZ();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }
}
