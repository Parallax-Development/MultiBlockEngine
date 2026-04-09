package dev.darkblade.mbe.core.application.service.io;

import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.wiring.NetworkGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class InternalNetwork {

    private final Map<ChannelType, List<IOPort>> ports = new ConcurrentHashMap<>();
    private final Map<UUID, NetworkGraph> subNetworks = new ConcurrentHashMap<>();

    void register(IOPort port) {
        ports.computeIfAbsent(port.getChannel(), unused -> new ArrayList<>()).add(port);
    }

    void unregister(IOPort port) {
        List<IOPort> byChannel = ports.get(port.getChannel());
        if (byChannel != null) {
            byChannel.removeIf(existing -> samePort(existing, port));
            if (byChannel.isEmpty()) {
                ports.remove(port.getChannel());
            }
        }
        if (!containsNetwork(port.getNetworkId())) {
            subNetworks.remove(port.getNetworkId());
        }
    }

    List<IOPort> ports(ChannelType channelType) {
        List<IOPort> found = ports.get(channelType);
        if (found == null || found.isEmpty()) {
            return List.of();
        }
        return List.copyOf(found);
    }

    void trackNetwork(UUID networkId, NetworkGraph graph) {
        if (networkId == null || graph == null) {
            return;
        }
        subNetworks.put(networkId, graph);
    }

    private boolean containsNetwork(UUID networkId) {
        if (networkId == null) {
            return false;
        }
        for (List<IOPort> value : ports.values()) {
            for (IOPort port : value) {
                if (networkId.equals(port.getNetworkId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean samePort(IOPort left, IOPort right) {
        return left != null
                && right != null
                && left.getPosition().equals(right.getPosition())
                && left.getOwner() == right.getOwner();
    }
}
