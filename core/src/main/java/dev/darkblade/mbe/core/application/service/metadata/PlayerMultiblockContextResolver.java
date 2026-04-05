package dev.darkblade.mbe.core.application.service.metadata;

import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public final class PlayerMultiblockContextResolver {
    private final MultiblockRuntimeService runtimeService;
    private final double maxDistanceSquared;

    public PlayerMultiblockContextResolver(MultiblockRuntimeService runtimeService, double maxDistance) {
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
        double safeMaxDistance = Math.max(1D, maxDistance);
        this.maxDistanceSquared = safeMaxDistance * safeMaxDistance;
    }

    public @Nullable MultiblockInstance resolveNearest(Player player) {
        if (player == null || !player.isOnline() || player.getWorld() == null) {
            return null;
        }
        Location origin = player.getLocation();
        Collection<MultiblockInstance> instances = runtimeService.getActiveInstancesSnapshot();
        if (instances.isEmpty()) {
            return null;
        }
        MultiblockInstance best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MultiblockInstance instance : instances) {
            if (instance == null || instance.anchorLocation() == null || instance.anchorLocation().getWorld() == null) {
                continue;
            }
            if (!instance.anchorLocation().getWorld().equals(origin.getWorld())) {
                continue;
            }
            double distance = instance.anchorLocation().distanceSquared(origin);
            if (distance > maxDistanceSquared) {
                continue;
            }
            if (distance < bestDistance) {
                best = instance;
                bestDistance = distance;
            }
        }
        return best;
    }
}
