package dev.darkblade.mbe.preview;

import org.bukkit.block.data.BlockData;

public record PreviewBlock(Vector3i localPosition, BlockData blockData) {
}
