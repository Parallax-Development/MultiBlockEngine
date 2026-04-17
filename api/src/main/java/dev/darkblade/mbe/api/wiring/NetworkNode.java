package dev.darkblade.mbe.api.wiring;

import java.util.Set;
import java.util.UUID;

public interface NetworkNode {
    UUID id();
    
    NetworkType type();

    BlockPos position();

    Set<Direction> connectableFaces();
}

