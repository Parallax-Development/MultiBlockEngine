package dev.darkblade.mbe.api.wiring.event;

import dev.darkblade.mbe.api.wiring.NetworkGraph;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NetworkMergedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final NetworkGraph from;
    private final NetworkGraph into;

    public NetworkMergedEvent(@NotNull NetworkGraph from, @NotNull NetworkGraph into) {
        this.from = from;
        this.into = into;
    }

    @NotNull
    public NetworkGraph getFrom() {
        return from;
    }

    @NotNull
    public NetworkGraph getInto() {
        return into;
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

