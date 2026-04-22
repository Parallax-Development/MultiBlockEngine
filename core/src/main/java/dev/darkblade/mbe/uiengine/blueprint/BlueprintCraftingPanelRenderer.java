package dev.darkblade.mbe.uiengine.blueprint;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.catalog.StructureCatalogService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Builds and refreshes the Blueprint Crafting Table inventory panel.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Creates the 54-slot Bukkit inventory with the correct i18n title.</li>
 *   <li>Places decorative glass panes in border/filler slots.</li>
 *   <li>Renders the current page of blueprint icons in the catalog zone.</li>
 *   <li>Renders navigation buttons (prev, next, close) and the crafting-table deco.</li>
 *   <li>Opens the inventory for the player and registers the session.</li>
 * </ul>
 *
 * <p><b>This class contains zero business logic.</b> It never mutates domain state.
 */
public final class BlueprintCraftingPanelRenderer {

    // -------------------------------------------------------------------------
    // Layout constants (derived from x-docs/blueprint_table-layout.yml)
    // -------------------------------------------------------------------------

    /** Slots occupied by the paginated blueprint catalog icons. */
    static final List<Integer> BLUEPRINT_SLOTS = List.of(
            10, 11, 12, 13,
            19, 20, 21, 22,
            28, 29, 30, 31,
            37, 38, 39, 40
    );

    /** Slot where the player places a paper to enable crafting. */
    static final int INPUT_SLOT  = 15;

    /** Slot where the crafted blueprint appears after selection. */
    static final int OUTPUT_SLOT = 25;

    /** Crafting Table decorative item slot. */
    private static final int DECO_CRAFTING_TABLE_SLOT = 24;

    /** Previous-page button slot. */
    static final int PREV_SLOT  = 42;

    /** Next-page button slot. */
    static final int NEXT_SLOT  = 43;

    /** Close-panel button slot. */
    static final int CLOSE_SLOT = 45;

    /**
     * All slot indices that are purely decorative. Computed as complement of all special slots.
     */
    private static final List<Integer> DECO_SLOTS;

    static {
        List<Integer> deco = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!BLUEPRINT_SLOTS.contains(i)
                    && i != INPUT_SLOT
                    && i != OUTPUT_SLOT
                    && i != DECO_CRAFTING_TABLE_SLOT
                    && i != PREV_SLOT
                    && i != NEXT_SLOT
                    && i != CLOSE_SLOT) {
                deco.add(i);
            }
        }
        DECO_SLOTS = List.copyOf(deco);
    }

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final StructureCatalogService catalogService;
    private final BlueprintCraftingSessionStore sessionStore;
    private final I18nService i18n;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;

    public BlueprintCraftingPanelRenderer(
            StructureCatalogService catalogService,
            BlueprintCraftingSessionStore sessionStore,
            I18nService i18n,
            ItemService itemService,
            ItemStackBridge itemStackBridge
    ) {
        this.catalogService   = Objects.requireNonNull(catalogService, "catalogService");
        this.sessionStore     = Objects.requireNonNull(sessionStore, "sessionStore");
        this.i18n             = Objects.requireNonNull(i18n, "i18n");
        this.itemService      = Objects.requireNonNull(itemService, "itemService");
        this.itemStackBridge  = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens the Blueprint Crafting Table panel for the given player.
     * Creates a new {@link BlueprintCraftingSession} and stores it in the session store.
     */
    public void open(Player player) {
        Objects.requireNonNull(player, "player");

        Locale locale = resolveLocale(player);
        String title = color(resolve(BlueprintCraftingMessageKeys.TITLE, locale));
        Inventory inventory = Bukkit.createInventory(new CraftingTableHolder(), 54, title);

        List<MultiblockDefinition> allBlueprints = new ArrayList<>(catalogService.getAll());
        BlueprintCraftingSession session = new BlueprintCraftingSession(
                inventory,
                allBlueprints,
                BLUEPRINT_SLOTS,
                INPUT_SLOT,
                OUTPUT_SLOT
        );

        renderStatic(inventory, locale);
        renderPage(session, locale);

        sessionStore.put(player, session);
        player.openInventory(inventory);
    }

    /**
     * Re-renders only the dynamic catalog zone after a page change.
     * Static items (glass panes, buttons) are not touched.
     */
    public void refreshPage(Player player, BlueprintCraftingSession session) {
        Objects.requireNonNull(session, "session");
        Locale locale = resolveLocale(player);
        renderPage(session, locale);
    }

    // -------------------------------------------------------------------------
    // Rendering helpers
    // -------------------------------------------------------------------------

    /**
     * Places all static items: decorative glass panes, navigation buttons, and deco crafting table.
     */
    private void renderStatic(Inventory inventory, Locale locale) {
        String emptyName = color(resolve(BlueprintCraftingMessageKeys.EMPTY, locale));

        ItemStack deco = namedItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, emptyName);
        for (int slot : DECO_SLOTS) {
            inventory.setItem(slot, deco);
        }

        inventory.setItem(DECO_CRAFTING_TABLE_SLOT,
                namedItem(Material.CRAFTING_TABLE, emptyName));

        inventory.setItem(PREV_SLOT,
                namedItem(Material.WHITE_STAINED_GLASS_PANE,
                        color("&f" + resolve(BlueprintCraftingMessageKeys.BTN_PREV_PAGE, locale))));
        inventory.setItem(NEXT_SLOT,
                namedItem(Material.WHITE_STAINED_GLASS_PANE,
                        color("&f" + resolve(BlueprintCraftingMessageKeys.BTN_NEXT_PAGE, locale))));
        inventory.setItem(CLOSE_SLOT,
                namedItem(Material.RED_STAINED_GLASS_PANE,
                        color("&c" + resolve(BlueprintCraftingMessageKeys.BTN_CLOSE, locale))));
    }

    /**
     * Renders the blueprint catalog zone for the session's current page.
     * Empty slots on the current page are filled with a light-gray filler pane.
     */
    private void renderPage(BlueprintCraftingSession session, Locale locale) {
        Inventory inventory = session.inventory();
        List<MultiblockDefinition> page = session.currentPageItems();
        List<Integer> slots = session.blueprintSlots();

        String emptyName  = color(resolve(BlueprintCraftingMessageKeys.EMPTY, locale));
        String namePattern = resolve(BlueprintCraftingMessageKeys.BLUEPRINT_NAME, locale);
        String hint        = color(resolve(BlueprintCraftingMessageKeys.BLUEPRINT_CLICK_TO_SELECT, locale));

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (i < page.size()) {
                MultiblockDefinition def = page.get(i);
                String name = color(namePattern.replace("{id}", def.id() == null ? "?" : def.id()));
                inventory.setItem(slot, blueprintIcon(name, hint));
            } else {
                inventory.setItem(slot, namedItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, emptyName));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Item factories
    // -------------------------------------------------------------------------

    private ItemStack blueprintIcon(String name, String loreLine) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add(loreLine);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // -------------------------------------------------------------------------
    // i18n helpers
    // -------------------------------------------------------------------------

    private String resolve(MessageKey key, Locale locale) {
        String value = i18n.resolve(key, locale);
        return (value == null || value.isBlank()) ? key.path() : value;
    }

    private Locale resolveLocale(Player player) {
        try {
            return i18n.localeProvider().localeOf(player);
        } catch (Exception e) {
            return Locale.ENGLISH;
        }
    }

    @SuppressWarnings("deprecation")
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    // -------------------------------------------------------------------------
    // InventoryHolder marker
    // -------------------------------------------------------------------------

    /**
     * Marker {@link InventoryHolder} so listeners can identify inventories belonging
     * to the Blueprint Crafting Table panel.
     */
    public record CraftingTableHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
