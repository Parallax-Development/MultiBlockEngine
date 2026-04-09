package dev.darkblade.mbe.api.io.event;

import dev.darkblade.mbe.api.io.IOPayload;
import dev.darkblade.mbe.api.io.IOPort;
import org.bukkit.event.Event;

import java.util.Objects;

public abstract class IOTransferEvent extends Event {

    private final IOPort from;
    private final IOPort to;
    private IOPayload payload;

    protected IOTransferEvent(IOPort from, IOPort to, IOPayload payload) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public IOPort getFrom() {
        return from;
    }

    public IOPort getTo() {
        return to;
    }

    public IOPayload getPayload() {
        return payload;
    }

    public void setPayload(IOPayload payload) {
        this.payload = Objects.requireNonNull(payload, "payload");
    }
}
