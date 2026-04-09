package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.io.SimpleIOPort;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.UUID;

public final class IOPortToolSupport {

    private IOPortToolSupport() {
    }

    public static Optional<IOPort> findClickedPort(IOService ioService, MultiblockRuntimeService runtimeService, Block clickedBlock) {
        if (ioService == null || runtimeService == null || clickedBlock == null || clickedBlock.getWorld() == null) {
            return Optional.empty();
        }
        Optional<MultiblockInstance> instanceOpt = runtimeService.getInstanceAt(clickedBlock.getLocation());
        if (instanceOpt.isEmpty()) {
            return Optional.empty();
        }
        MultiblockInstance instance = instanceOpt.get();
        UUID worldId = clickedBlock.getWorld().getUID();
        int x = clickedBlock.getX();
        int y = clickedBlock.getY();
        int z = clickedBlock.getZ();
        for (IOPort port : ioService.getPorts(instance)) {
            BlockPos position = port.getPosition();
            if (position != null
                    && worldId.equals(position.worldId())
                    && x == position.x()
                    && y == position.y()
                    && z == position.z()) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
    }

    public static IOPort replace(IOService ioService, IOPort current, IOType nextType, ChannelType nextChannel, UUID nextNetwork) {
        ioService.unregisterPort(current);
        IOPort updated = new SimpleIOPort(
                current.getPosition(),
                current.getFace(),
                nextType,
                nextChannel,
                nextNetwork,
                current.getOwner()
        );
        ioService.registerPort(updated);
        return updated;
    }
}
