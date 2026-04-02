package dev.darkblade.mbe.api.wiring.event;

import dev.darkblade.mbe.api.wiring.NetworkGraph;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NetworkSplitEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final NetworkGraph original;
    private final List<NetworkGraph> resulting;

    public NetworkSplitEvent(@NotNull NetworkGraph original, @NotNull List<NetworkGraph> resulting) {
        this.original = original;
        this.resulting = List.copyOf(resulting);
    }

    @NotNull
    public NetworkGraph getOriginal() {
        return original;
    }

    @NotNull
    public List<NetworkGraph> getResulting() {
        return resulting;
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

