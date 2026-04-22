package dev.darkblade.mbe.api.blueprint;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.entity.Player;

public interface BlueprintService {
    void startPlacement(Player player, MultiblockDefinition definition);

    void openCatalog(Player player);

    /**
     * Opens the Blueprint Crafting Table panel for the given player.
     * <p>
     * The player must insert a {@link org.bukkit.Material#PAPER} in the input slot
     * and then click a blueprint from the catalog to craft it.
     */
    void openCraftingTable(Player player);
}
