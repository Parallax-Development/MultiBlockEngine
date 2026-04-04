package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CatalogMenu {
    public static final int SIZE = 54;
    public static final int PREV_SLOT = 45;
    public static final int NEXT_SLOT = 53;
    public static final int MAX_ITEMS_PER_PAGE = 45;
    private final StructureCatalogService catalogService;
    private final CatalogItemMapper itemMapper;

    public CatalogMenu(StructureCatalogService catalogService, CatalogItemMapper itemMapper) {
        this.catalogService = Objects.requireNonNull(catalogService, "catalogService");
        this.itemMapper = Objects.requireNonNull(itemMapper, "itemMapper");
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        if (player == null) {
            return;
        }
        List<MultiblockDefinition> definitions = catalogService.getAll();
        int totalPages = Math.max(1, (int) Math.ceil((double) definitions.size() / MAX_ITEMS_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * MAX_ITEMS_PER_PAGE;
        int to = Math.min(definitions.size(), from + MAX_ITEMS_PER_PAGE);
        List<MultiblockDefinition> pageItems = definitions.subList(from, to);
        MenuHolder holder = new MenuHolder(safePage, totalPages);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "Catálogo de Estructuras (" + (safePage + 1) + "/" + totalPages + ")");
        holder.inventory(inventory);

        int slot = 0;
        for (MultiblockDefinition definition : pageItems) {
            inventory.setItem(slot, itemMapper.map(definition));
            holder.entries().put(slot, definition);
            slot++;
        }
        if (safePage > 0) {
            inventory.setItem(PREV_SLOT, navItem(Material.ARROW, "§ePágina anterior"));
        }
        if (safePage < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, navItem(Material.ARROW, "§ePágina siguiente"));
        }
        inventory.setItem(49, navItem(Material.BARRIER, "§cCerrar"));
        player.openInventory(inventory);
    }

    private ItemStack navItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        private final int page;
        private final int totalPages;
        private final Map<Integer, MultiblockDefinition> entries = new LinkedHashMap<>();

        private MenuHolder(int page, int totalPages) {
            this.page = page;
            this.totalPages = totalPages;
        }

        private void inventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public int page() {
            return page;
        }

        public int totalPages() {
            return totalPages;
        }

        public Map<Integer, MultiblockDefinition> entries() {
            return entries;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
