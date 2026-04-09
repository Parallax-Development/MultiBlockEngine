package dev.darkblade.mbe.core.application.service.io;

import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOChannel;
import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.TransferResult;

import java.util.Objects;

public final class BasicIOChannel implements IOChannel {

    private final ChannelType type;

    public BasicIOChannel(ChannelType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public ChannelType getType() {
        return type;
    }

    @Override
    public boolean canTransfer(IOPayload payload) {
        return payload != null && payload.getType() == type;
    }

    @Override
    public TransferResult transfer(IOPort from, IOPort to, IOPayload payload) {
        if (from == null || to == null || !canTransfer(payload)) {
            return TransferResult.FAIL;
        }
        return TransferResult.SUCCESS;
    }
}
