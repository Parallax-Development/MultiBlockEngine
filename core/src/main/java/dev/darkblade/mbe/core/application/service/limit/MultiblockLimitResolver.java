package dev.darkblade.mbe.core.application.service.limit;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface MultiblockLimitResolver {

    Optional<MultiblockLimitDefinition> resolve(Player player, String multiblockId);
}
