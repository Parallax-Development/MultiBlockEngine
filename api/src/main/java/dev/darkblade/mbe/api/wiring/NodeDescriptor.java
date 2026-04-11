package dev.darkblade.mbe.api.wiring;

import java.util.Set;

public record NodeDescriptor(Set<Direction> connectableFaces) {
    public NodeDescriptor {
        connectableFaces = connectableFaces == null ? Set.of() : Set.copyOf(connectableFaces);
    }
}

