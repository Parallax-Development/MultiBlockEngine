package dev.darkblade.mbe.api.wiring;

import java.util.Collection;
import java.util.UUID;

public interface NetworkGraph {
    UUID id();

    NetworkType type();

    Collection<NetworkNode> nodes();

    Collection<NetworkConnection> connections();
}

