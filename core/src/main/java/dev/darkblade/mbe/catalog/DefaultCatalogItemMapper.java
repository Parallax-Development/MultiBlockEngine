package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.preview.PreviewBlock;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class DefaultCatalogItemMapper implements CatalogItemMapper {
    @Override
    public ItemStack map(MultiblockDefinition definition) {
        Material icon = Material.STRUCTURE_BLOCK;
        int blockCount = 0;
        if (definition != null && definition.blocks() != null) {
            blockCount = definition.blocks().size();
            for (PreviewBlock block : definition.blocks()) {
                if (block == null || block.blockData() == null) {
                    continue;
                }
                Material material = toMaterial(block.blockData());
                if (material != null && material.isItem()) {
                    icon = material;
                    break;
                }
            }
        }
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + (definition == null ? "unknown" : definition.id()));
            List<String> lore = new ArrayList<>();
            lore.add("§7Bloques: §f" + blockCount);
            lore.add("§7Click para previsualizar");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material toMaterial(BlockData data) {
        if (data == null) {
            return Material.STRUCTURE_BLOCK;
        }
        Material material = data.getMaterial();
        return material == null ? Material.STRUCTURE_BLOCK : material;
    }
}
