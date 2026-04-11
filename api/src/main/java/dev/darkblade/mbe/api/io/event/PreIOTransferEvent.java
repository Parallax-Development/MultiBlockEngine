package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.TransferResult;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PreIOTransferEvent extends IOTransferEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private TransferResult overrideResult;

    public PreIOTransferEvent(IOPort from, IOPort to, IOPayload payload) {
        super(from, to, payload);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public TransferResult getOverrideResult() {
        return overrideResult;
    }

    public void setOverrideResult(TransferResult overrideResult) {
        this.overrideResult = overrideResult;
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
