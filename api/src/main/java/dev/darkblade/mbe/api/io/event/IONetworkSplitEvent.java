package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.wiring.NetworkType;
import dev.darkblade.mbe.api.event.MBEEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IONetworkSplitEvent implements MBEEvent {

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

}
