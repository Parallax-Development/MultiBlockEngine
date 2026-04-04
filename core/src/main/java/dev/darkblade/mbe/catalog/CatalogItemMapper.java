package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.inventory.ItemStack;

public interface CatalogItemMapper {
    ItemStack map(MultiblockDefinition definition);
}
