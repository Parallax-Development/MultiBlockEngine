package dev.darkblade.mbe.api.wiring;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

public interface PortResolutionService {

    record ResolvedPort(PortDefinition definition, Location location) {
    }

    Optional<Location> resolveBlock(MultiblockInstance instance, PortBlockRef ref);

    Optional<Location> resolvePort(MultiblockInstance instance, String portId);

    Optional<Location> resolvePort(MultiblockInstance instance, PortDefinition port);

    List<ResolvedPort> resolveAll(MultiblockInstance instance);

    List<ResolvedPort> resolveByType(MultiblockInstance instance, String type);

    List<ResolvedPort> resolveByDirection(MultiblockInstance instance, PortDirection direction);

    List<ResolvedPort> resolveByCapability(MultiblockInstance instance, String capability);
}

