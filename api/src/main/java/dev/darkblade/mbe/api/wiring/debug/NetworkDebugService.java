package dev.darkblade.mbe.api.wiring.debug;

import dev.darkblade.mbe.api.wiring.BlockPos;
import dev.darkblade.mbe.api.wiring.Direction;
import dev.darkblade.mbe.api.wiring.NetworkNode;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface NetworkDebugService {

    Optional<NetworkNode> nodeAt(BlockPos pos);

    Optional<NetworkSnapshot> inspect(BlockPos pos);

    boolean toggleEdge(NetworkNode node, Direction dir);

    boolean visualize(NetworkSnapshot snapshot, Duration time, UUID playerId);
}

