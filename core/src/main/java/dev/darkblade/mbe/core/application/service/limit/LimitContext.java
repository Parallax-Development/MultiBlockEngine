package dev.darkblade.mbe.core.application.service.limit;

import org.bukkit.World;

import java.util.UUID;

public record LimitContext(
        UUID playerId,
        String multiblockId,
        World world
) {
}
