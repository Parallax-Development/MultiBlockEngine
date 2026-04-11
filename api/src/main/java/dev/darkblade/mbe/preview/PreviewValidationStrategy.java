package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public interface PreviewValidationStrategy {
    PreviewBlockState validate(Location location, BlockData expected);
}
