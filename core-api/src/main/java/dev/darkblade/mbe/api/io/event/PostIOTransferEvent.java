package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.TransferResult;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PostIOTransferEvent extends IOTransferEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TransferResult result;

    public PostIOTransferEvent(IOPort from, IOPort to, IOPayload payload, TransferResult result) {
        super(from, to, payload);
        this.result = Objects.requireNonNull(result, "result");
    }

    public TransferResult getResult() {
        return result;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
