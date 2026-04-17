package dev.darkblade.mbe.api.wiring;

import org.bukkit.block.Block;

import java.util.Collection;
import java.util.Optional;

public interface NetworkService {

    NetworkNode registerNode(NetworkType type, Block block, NodeDescriptor descriptor);

    void unregisterNode(NetworkType type, NetworkNode node);

    boolean connect(NetworkType type, NetworkNode a, NetworkNode b);

    void disconnect(NetworkType type, NetworkNode a, NetworkNode b);

    NetworkGraph getGraph(NetworkType type, NetworkNode node);
    
    Optional<NetworkNode> findNode(NetworkType type, Block block);
    
    Collection<NetworkNode> findAllNodes(Block block);
}

