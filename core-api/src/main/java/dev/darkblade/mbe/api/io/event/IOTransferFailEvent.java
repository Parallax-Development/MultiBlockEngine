package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class IOTransferFailEvent extends IOTransferEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String reason;

    public IOTransferFailEvent(IOPort from, IOPort to, IOPayload payload, String reason) {
        super(from, to, payload);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public String getReason() {
        return reason;
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
