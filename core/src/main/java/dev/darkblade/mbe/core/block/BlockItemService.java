package dev.darkblade.mbe.core.block;

import dev.darkblade.mbe.api.block.BlockDefinition;
import dev.darkblade.mbe.api.block.BlockKey;
import dev.darkblade.mbe.api.block.BlockRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class BlockItemService {
    public static final String NBT_BLOCK_ID = "block_mbe_id";
    private final BlockRegistry blockRegistry;
    private final NamespacedKey blockIdKey;

    public BlockItemService(Plugin plugin, BlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
        this.blockIdKey = new NamespacedKey(plugin, NBT_BLOCK_ID);
    }

    public ItemStack createItemStack(BlockKey blockKey) {
        BlockDefinition def = blockRegistry.get(blockKey);
        if (def == null) return null;

        Material material = Material.matchMaterial(def.blockMaterial());
        if (material == null) material = Material.STONE;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (def.displayName() != null && def.displayName().text() != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', def.displayName().text()));
            }
            meta.getPersistentDataContainer().set(blockIdKey, PersistentDataType.STRING, def.key().toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public BlockKey getBlockKey(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(blockIdKey, PersistentDataType.STRING)) {
            return null;
        }
        String idStr = pdc.get(blockIdKey, PersistentDataType.STRING);
        if (idStr == null || idStr.isEmpty()) return null;
        try {
            return BlockKey.of(idStr);
        } catch (Exception e) {
            return null;
        }
    }
}
