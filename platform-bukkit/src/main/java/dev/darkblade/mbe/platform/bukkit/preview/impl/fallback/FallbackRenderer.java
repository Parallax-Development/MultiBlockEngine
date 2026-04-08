package dev.darkblade.mbe.platform.bukkit.preview.impl.fallback;

import dev.darkblade.mbe.platform.bukkit.preview.api.BlockDisplayRenderer;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayBlockState;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayEntityHandle;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplaySpawnRequest;
import dev.darkblade.mbe.platform.bukkit.preview.api.DisplayTransform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FallbackRenderer implements BlockDisplayRenderer {
    @Override
    public DisplayEntityHandle spawn(Player player, DisplaySpawnRequest request) {
        warn();
        return new DisplayEntityHandle(-1, null);
    }

    @Override
    public void updateTransform(Player player, DisplayEntityHandle handle, DisplayTransform transform) {
        warn();
    }

    @Override
    public void updateBlock(Player player, DisplayEntityHandle handle, DisplayBlockState blockState) {
        warn();
    }

    @Override
    public void destroy(Player player, DisplayEntityHandle handle) {
        warn();
    }

    private void warn() {
        Bukkit.getLogger().warning("[MBE Preview] No compatible BlockDisplay renderer available for this protocol");
    }
}
