package dev.darkblade.mbe.api.wiring;

import java.util.Objects;

public record NetworkType(String id) {
    public NetworkType {
        Objects.requireNonNull(id, "NetworkType id cannot be null");
    }
}
