package dev.darkblade.mbe.platform.bukkit.preview.api;

import org.bukkit.entity.Player;

public interface BlockDisplayRenderer {
    DisplayEntityHandle spawn(Player player, DisplaySpawnRequest request);

    void updateTransform(Player player, DisplayEntityHandle handle, DisplayTransform transform);

    void updateBlock(Player player, DisplayEntityHandle handle, DisplayBlockState blockState);

    void destroy(Player player, DisplayEntityHandle handle);
}
