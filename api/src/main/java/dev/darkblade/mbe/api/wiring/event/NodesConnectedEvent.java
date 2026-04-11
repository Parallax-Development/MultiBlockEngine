package dev.darkblade.mbe.api.wiring.event;

import dev.darkblade.mbe.api.wiring.NetworkConnection;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NodesConnectedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final NetworkConnection connection;

    public NodesConnectedEvent(@NotNull NetworkConnection connection) {
        this.connection = connection;
    }

    @NotNull
    public NetworkConnection getConnection() {
        return connection;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

