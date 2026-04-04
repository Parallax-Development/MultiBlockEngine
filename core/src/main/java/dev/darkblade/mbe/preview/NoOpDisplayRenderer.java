package dev.darkblade.mbe.preview;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class NoOpDisplayRenderer implements DisplayEntityRenderer {
    @Override
    public int spawnBlockDisplay(Player player, Location location, BlockData blockData) {
        return -1;
    }

    @Override
    public void updateBlockDisplay(int entityId, BlockData blockData) {
    }

    @Override
    public void destroyEntities(Player player, Collection<Integer> entityIds) {
    }
}
