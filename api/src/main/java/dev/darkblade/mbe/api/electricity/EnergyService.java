package dev.darkblade.mbe.api.electricity;

import java.util.Optional;
import java.util.UUID;

public interface EnergyService {
    void register(EnergyNode node);

    void unregister(UUID networkNodeId);

    Optional<EnergyNode> getNode(UUID networkNodeId);
}

