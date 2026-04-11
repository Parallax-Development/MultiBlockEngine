package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.wiring.NetworkGraph;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NetworkOverloadedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final NetworkGraph graph;
    private final long overflow;

    public NetworkOverloadedEvent(@NotNull NetworkGraph graph, long overflow) {
        this.graph = graph;
        this.overflow = overflow;
    }

    @NotNull
    public NetworkGraph getGraph() {
        return graph;
    }

    public long getOverflow() {
        return overflow;
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

