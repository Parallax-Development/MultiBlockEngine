package dev.darkblade.mbe.api.wiring;

import org.bukkit.block.Block;

/**
 * Functional contract for evaluating connectivity between a network node
 * and a target block across a specific direction/face.
 */
@FunctionalInterface
public interface ConnectionPredicate {

    /**
     * Evaluates whether a connection can be established between a source network node
     * and a target block in a specific direction.
     *
     * @param source The source network node.
     * @param direction The direction from the source node to the target block.
     * @param targetBlock The target Bukkit block.
     * @return true if the connection is valid and allowed, false otherwise.
     */
    boolean canConnect(NetworkNode source, Direction direction, Block targetBlock);
}
