package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface DisplayEntityRenderer {
    int spawnBlockDisplay(Player player, Location location, BlockData blockData);
    
    default int spawnBlockDisplay(Player player, Location location, BlockData blockData, float tx, float ty, float tz, float sx, float sy, float sz) {
        return spawnBlockDisplay(player, location, blockData);
    }
    
    void updateBlockDisplay(int entityId, BlockData blockData);
    
    default void highlightError(Player player, int entityId) {
        // Fallback does nothing
    }
    
    void destroyEntities(Player player, Collection<Integer> entityIds);
}
