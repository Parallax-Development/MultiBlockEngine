package dev.darkblade.mbe.catalog;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class RaycastPreviewOriginResolver implements PreviewOriginResolver {
    private final int raycastDistance;

    public RaycastPreviewOriginResolver(int raycastDistance) {
        this.raycastDistance = Math.max(2, raycastDistance);
    }

    @Override
    public Location resolve(Player player) {
        if (player == null) {
            return null;
        }
        Block target = player.getTargetBlockExact(raycastDistance);
        if (target != null && target.getWorld() != null) {
            Location base = target.getLocation().add(0, 1, 0);
            return base.getBlock().getLocation();
        }
        Location eye = player.getEyeLocation();
        Location projected = eye.clone().add(eye.getDirection().normalize().multiply(3.0D));
        return projected.getBlock().getLocation();
    }
}
