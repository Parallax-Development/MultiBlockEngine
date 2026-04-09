package dev.darkblade.mbe.core.application.service.io;

import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Objects;
import java.util.UUID;

public final class SimpleIOPort implements IOPort {

    private final BlockPos position;
    private final Direction face;
    private final IOType type;
    private final ChannelType channel;
    private final UUID networkId;
    private final MultiblockInstance owner;

    public SimpleIOPort(
            BlockPos position,
            Direction face,
            IOType type,
            ChannelType channel,
            UUID networkId,
            MultiblockInstance owner
    ) {
        this.position = Objects.requireNonNull(position, "position");
        this.face = Objects.requireNonNull(face, "face");
        this.type = Objects.requireNonNull(type, "type");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.networkId = Objects.requireNonNull(networkId, "networkId");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public BlockPos getPosition() {
        return position;
    }

    @Override
    public Direction getFace() {
        return face;
    }

    @Override
    public IOType getType() {
        return type;
    }

    @Override
    public ChannelType getChannel() {
        return channel;
    }

    @Override
    public UUID getNetworkId() {
        return networkId;
    }

    @Override
    public MultiblockInstance getOwner() {
        return owner;
    }
}
