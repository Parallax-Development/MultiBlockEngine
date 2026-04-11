package dev.darkblade.mbe.api.wiring;

import java.util.UUID;

public record BlockPos(UUID worldId, int x, int y, int z) {
}

