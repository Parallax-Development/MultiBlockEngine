package dev.darkblade.mbe.core.application.service.wiring;

import dev.darkblade.mbe.api.wiring.ConnectionPredicate;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;
import dev.darkblade.mbe.api.wiring.NetworkService;
import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.wiring.PortResolutionService;
import dev.darkblade.mbe.core.application.service.multiblock.MultiblockInstanceRegistry;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.Optional;

public final class WireConnectionEvaluator implements ConnectionPredicate {

    private final NetworkService networkService;
    private final PortResolutionService portResolutionService;
    private final MultiblockInstanceRegistry instanceRegistry;

    public WireConnectionEvaluator(
            NetworkService networkService,
            PortResolutionService portResolutionService,
            MultiblockInstanceRegistry instanceRegistry
    ) {
        this.networkService = Objects.requireNonNull(networkService, "networkService");
        this.portResolutionService = portResolutionService;
        this.instanceRegistry = instanceRegistry;
    }

    @Override
    public boolean canConnect(NetworkNode source, Direction direction, Block targetBlock) {
        if (source == null || direction == null || targetBlock == null || targetBlock.getWorld() == null) {
            return false;
        }

        // 1. Check connectable faces on source node
        if (source.connectableFaces() != null && !source.connectableFaces().isEmpty() && !source.connectableFaces().contains(direction)) {
            return false;
        }

        NetworkType type = source.type();

        // 2. Direct NetworkNode lookup in target block
        Optional<NetworkNode> targetNodeOpt = networkService.findNode(type, targetBlock);
        if (targetNodeOpt.isPresent()) {
            NetworkNode targetNode = targetNodeOpt.get();
            Direction opposite = direction.opposite();
            if (targetNode.connectableFaces() != null && !targetNode.connectableFaces().isEmpty()
                    && !targetNode.connectableFaces().contains(opposite)) {
                return false;
            }
            return true;
        }

        // 3. Multiblock IO Port evaluation
        if (instanceRegistry != null && portResolutionService != null) {
            Location loc = targetBlock.getLocation();
            Optional<MultiblockInstance> multiblockOpt = instanceRegistry.getInstanceAt(loc);
            if (multiblockOpt.isPresent()) {
                MultiblockInstance instance = multiblockOpt.get();
                // Check if targetBlock is a formally resolved IOPort / PortDefinition
                boolean isPort = portResolutionService.resolveAll(instance).stream()
                        .anyMatch(resolved -> resolved.location() != null
                                && resolved.location().getBlockX() == targetBlock.getX()
                                && resolved.location().getBlockY() == targetBlock.getY()
                                && resolved.location().getBlockZ() == targetBlock.getZ());
                
                // If it is part of a multiblock but NOT a formal IO Port (e.g. casing/decorative block), reject!
                if (!isPort) {
                    return false;
                }
                return true;
            }
        }

        return false;
    }
}
