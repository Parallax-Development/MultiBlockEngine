package dev.darkblade.mbe.api.wiring;

import org.bukkit.block.Block;

public interface NetworkService {

    NetworkNode registerNode(Block block, NodeDescriptor descriptor);

    void unregisterNode(NetworkNode node);

    boolean connect(NetworkNode a, NetworkNode b);

    void disconnect(NetworkNode a, NetworkNode b);

    NetworkGraph getGraph(NetworkNode node);
}

