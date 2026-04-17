package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.wiring.NetworkType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class IONetworkMergeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NetworkType type;
    private final UUID fromNetwork;
    private final UUID toNetwork;

    public IONetworkMergeEvent(NetworkType type, UUID fromNetwork, UUID toNetwork) {
        this.type = Objects.requireNonNull(type, "type");
        this.fromNetwork = Objects.requireNonNull(fromNetwork, "fromNetwork");
        this.toNetwork = Objects.requireNonNull(toNetwork, "toNetwork");
    }

    public NetworkType getType() {
        return type;
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
