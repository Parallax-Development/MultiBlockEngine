package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface DisplayEntityRenderer {
    int spawnBlockDisplay(Player player, Location location, BlockData blockData);
    void updateBlockDisplay(int entityId, BlockData blockData);
    void destroyEntities(Player player, Collection<Integer> entityIds);
}
