package dev.darkblade.mbe.api.wiring.debug;

import dev.darkblade.mbe.api.wiring.NetworkConnection;
import dev.darkblade.mbe.api.wiring.NetworkNode;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public record NetworkSnapshot(
        UUID networkId,
        Collection<NetworkNode> nodes,
        Collection<NetworkConnection> connections,
        Map<String, Object> metrics
) {
}

