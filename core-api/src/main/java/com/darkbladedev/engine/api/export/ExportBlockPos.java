package com.darkbladedev.engine.api.export;

import java.util.UUID;

public record ExportBlockPos(UUID worldId, int x, int y, int z) {
}

