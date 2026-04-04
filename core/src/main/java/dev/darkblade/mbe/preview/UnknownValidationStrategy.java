package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public final class UnknownValidationStrategy implements PreviewValidationStrategy {
    @Override
    public PreviewBlockState validate(Location location, BlockData expected) {
        return PreviewBlockState.UNKNOWN;
    }
}
