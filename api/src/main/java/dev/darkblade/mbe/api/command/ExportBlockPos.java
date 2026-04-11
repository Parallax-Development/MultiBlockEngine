package dev.darkblade.mbe.api.command;

import java.util.UUID;

public record ExportBlockPos(UUID worldId, int x, int y, int z) {
}

