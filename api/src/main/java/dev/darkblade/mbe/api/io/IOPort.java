package dev.darkblade.mbe.api.io;

import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.UUID;

public interface IOPort {

    BlockPos getPosition();

    Direction getFace();

    IOType getType();

    ChannelType getChannel();

    UUID getNetworkId();

    MultiblockInstance getOwner();
}
