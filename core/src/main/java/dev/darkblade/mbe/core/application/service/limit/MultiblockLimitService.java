package dev.darkblade.mbe.core.application.service.limit;

import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface MultiblockLimitService extends MBEService {

    boolean canAssemble(Player player, String multiblockId);

    boolean canAssemble(UUID playerId, String multiblockId, MultiblockLimitDefinition definition);

    int getCurrentCount(Player player, String multiblockId);

    int getLimit(Player player, String multiblockId);

    void registerAssembly(Player player, String multiblockId);

    void registerAssembly(UUID playerId, String multiblockId);

    void unregisterAssembly(Player player, String multiblockId);

    void unregisterAssembly(UUID playerId, String multiblockId);
}
