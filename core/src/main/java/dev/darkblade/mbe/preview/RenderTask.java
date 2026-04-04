package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

record RenderTask(Player player, PreviewSession session, long renderVersion, BlockPosition blockPosition, Location worldLocation, BlockData blockData) {
}
