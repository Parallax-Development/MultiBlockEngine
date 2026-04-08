package dev.darkblade.mbe.platform.bukkit.preview.api;

import java.util.Objects;

public record DisplaySpawnRequest(
    int entityId,
    double x,
    double y,
    double z,
    DisplayBlockState blockState,
    DisplayTransform transform
) {
    public DisplaySpawnRequest {
        Objects.requireNonNull(blockState, "blockState");
    }
}
