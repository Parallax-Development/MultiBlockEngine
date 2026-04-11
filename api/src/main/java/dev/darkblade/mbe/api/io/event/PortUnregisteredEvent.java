package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.io.IOPort;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PortUnregisteredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final IOPort port;

    public PortUnregisteredEvent(IOPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public IOPort getPort() {
        return port;
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
