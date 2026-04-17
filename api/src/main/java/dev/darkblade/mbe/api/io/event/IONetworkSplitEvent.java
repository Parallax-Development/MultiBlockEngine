package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.wiring.NetworkType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IONetworkSplitEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final NetworkType type;
    private final UUID original;
    private final List<UUID> resulting;

    public IONetworkSplitEvent(NetworkType type, UUID original, List<UUID> resulting) {
        this.type = Objects.requireNonNull(type, "type");
        this.original = Objects.requireNonNull(original, "original");
        this.resulting = List.copyOf(Objects.requireNonNull(resulting, "resulting"));
    }

    public NetworkType getType() {
        return type;
    }

    public UUID getOriginal() {
        return original;
    }

    public List<UUID> getResulting() {
        return resulting;
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
