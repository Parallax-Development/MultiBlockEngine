package dev.darkblade.mbe.api.electricity.event;

import dev.darkblade.mbe.api.wiring.NetworkGraph;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EnergyStarvationEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final NetworkGraph graph;
    private final long missing;

    public EnergyStarvationEvent(@NotNull NetworkGraph graph, long missing) {
        this.graph = graph;
        this.missing = missing;
    }

    @NotNull
    public NetworkGraph getGraph() {
        return graph;
    }

    public long getMissing() {
        return missing;
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

