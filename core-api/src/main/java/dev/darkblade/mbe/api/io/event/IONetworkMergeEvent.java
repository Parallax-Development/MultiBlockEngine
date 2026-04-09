package dev.darkblade.mbe.api.io.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class IONetworkMergeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID fromNetwork;
    private final UUID toNetwork;

    public IONetworkMergeEvent(UUID fromNetwork, UUID toNetwork) {
        this.fromNetwork = Objects.requireNonNull(fromNetwork, "fromNetwork");
        this.toNetwork = Objects.requireNonNull(toNetwork, "toNetwork");
    }

    public UUID getFromNetwork() {
        return fromNetwork;
    }

    public UUID getToNetwork() {
        return toNetwork;
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
