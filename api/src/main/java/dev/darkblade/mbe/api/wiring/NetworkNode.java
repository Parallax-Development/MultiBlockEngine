package dev.darkblade.mbe.api.wiring;

import java.util.Set;
import java.util.UUID;

public interface NetworkNode {
    UUID id();

    BlockPos position();

    Set<Direction> connectableFaces();
}

