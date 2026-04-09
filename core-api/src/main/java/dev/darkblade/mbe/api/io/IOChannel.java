package dev.darkblade.mbe.api.io;

public interface IOChannel {

    ChannelType getType();

    boolean canTransfer(IOPayload payload);

    TransferResult transfer(IOPort from, IOPort to, IOPayload payload);
}
