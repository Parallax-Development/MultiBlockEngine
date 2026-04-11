package dev.darkblade.mbe.api.blueprint;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.entity.Player;

public interface BlueprintService {
    void startPlacement(Player player, MultiblockDefinition definition);

    void openCatalog(Player player);
}
