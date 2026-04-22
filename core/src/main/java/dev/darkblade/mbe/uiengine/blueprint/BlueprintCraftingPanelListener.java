package dev.darkblade.mbe.uiengine.blueprint;

import dev.darkblade.mbe.api.blueprint.BlueprintCraftingService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Handles all player interactions with the Blueprint Crafting Table panel.
 * <p>
 * Interaction table:
 * <table>
 *   <tr><th>Event / Slot</th><th>Action</th></tr>
 *   <tr><td>Click — prev button (42)</td><td>Navigate to previous page and refresh</td></tr>
 *   <tr><td>Click — next button (43)</td><td>Navigate to next page and refresh</td></tr>
 *   <tr><td>Click — close button (45)</td><td>Close inventory</td></tr>
 *   <tr><td>Click — blueprint slot (10-13, 19-22, 28-31, 37-40)</td><td>Delegate to {@link BlueprintCraftingService#craft}</td></tr>
 *   <tr><td>Click — output slot (25) with item</td><td>Give blueprint to player; drop if full</td></tr>
 *   <tr><td>Click — input slot (15)</td><td>Allow only PAPER; cancel anything else</td></tr>
 *   <tr><td>Click — any other slot</td><td>Cancel (protect decoratives)</td></tr>
 *   <tr><td>InventoryCloseEvent</td><td>Return output item and input paper to player; clear session</td></tr>
 *   <tr><td>InventoryDragEvent</td><td>Cancel any drag that touches panel slots</td></tr>
 * </table>
 *
 * <p><b>This listener contains zero business logic.</b> Domain rules live in
 * {@link BlueprintCraftingService}.
 */
public final class BlueprintCraftingPanelListener implements Listener {

    private final BlueprintCraftingSessionStore sessionStore;
    private final BlueprintCraftingPanelRenderer renderer;
    private final BlueprintCraftingService craftingService;
    private final PlayerMessageService messageService;

    public BlueprintCraftingPanelListener(
            BlueprintCraftingSessionStore sessionStore,
            BlueprintCraftingPanelRenderer renderer,
            BlueprintCraftingService craftingService,
            PlayerMessageService messageService
    ) {
        this.sessionStore    = Objects.requireNonNull(sessionStore, "sessionStore");
        this.renderer        = Objects.requireNonNull(renderer, "renderer");
        this.craftingService = Objects.requireNonNull(craftingService, "craftingService");
        this.messageService  = Objects.requireNonNull(messageService, "messageService");
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BlueprintCraftingSession session = sessionStore.get(player).orElse(null);
        if (session == null) return;

        // Make sure click is on our inventory
        Inventory topInventory = event.getView().getTopInventory();
        if (!isCraftingTableInventory(topInventory) || topInventory != session.inventory()) return;

        int rawSlot = event.getRawSlot();

        // Clicks inside the player's own inventory (bottom half) — allow freely
        if (rawSlot >= topInventory.getSize()) return;

        event.setCancelled(true);

        // ----- Navigation buttons -----
        if (rawSlot == BlueprintCraftingPanelRenderer.PREV_SLOT) {
            if (session.hasPrevPage()) {
                session.prevPage();
                renderer.refreshPage(player, session);
            }
            return;
        }

        if (rawSlot == BlueprintCraftingPanelRenderer.NEXT_SLOT) {
            if (session.hasNextPage()) {
                session.nextPage();
                renderer.refreshPage(player, session);
            }
            return;
        }

        if (rawSlot == BlueprintCraftingPanelRenderer.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        // ----- Output slot — player takes the crafted blueprint -----
        if (rawSlot == session.outputSlot()) {
            ItemStack outputItem = session.inventory().getItem(session.outputSlot());
            if (outputItem != null && outputItem.getType() != Material.AIR) {
                session.inventory().setItem(session.outputSlot(), null);
                giveOrDrop(player, outputItem);
            }
            return;
        }

        // ----- Input slot — allow only PAPER placement -----
        if (rawSlot == session.inputSlot()) {
            // If output has an item already, block input changes to keep state consistent
            ItemStack outputItem = session.inventory().getItem(session.outputSlot());
            if (outputItem != null && outputItem.getType() != Material.AIR) {
                return; // cancelled already; block any modification
            }

            ItemStack cursor = event.getCursor();
            ItemStack current = session.inventory().getItem(session.inputSlot());

            // Allow taking the current paper back
            if ((cursor == null || cursor.getType() == Material.AIR)
                    && current != null && current.getType() != Material.AIR) {
                event.setCancelled(false);
                return;
            }

            // Allow placing paper only
            if (cursor != null && cursor.getType() == Material.PAPER) {
                event.setCancelled(false);
                return;
            }

            // Anything else is blocked
            return;
        }

        // ----- Blueprint catalog slots -----
        if (session.isBlueprintSlot(rawSlot)) {
            MultiblockDefinition selected = session.getBlueprintAt(rawSlot);
            if (selected == null) return;

            BlueprintCraftingService.CraftingTableResult result = craftingService.craft(player, selected);
            if (!result.isSuccess()) {
                handleCraftFailure(player, result.status());
            }
            return;
        }

        // All other top-inventory slots → already cancelled above (protect decoratives)
    }

    // -------------------------------------------------------------------------
    // Drag handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BlueprintCraftingSession session = sessionStore.get(player).orElse(null);
        if (session == null) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (!isCraftingTableInventory(topInventory) || topInventory != session.inventory()) return;

        int topSize = topInventory.getSize();
        // If any slot of the drag touches the top inventory, cancel the drag entirely
        boolean touchesTop = event.getRawSlots().stream().anyMatch(s -> s < topSize);
        if (touchesTop) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Close handling
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        BlueprintCraftingSession session = sessionStore.get(player).orElse(null);
        if (session == null) return;

        if (session.inventory() != event.getInventory()) return;

        // Return output item (crafted blueprint) if present
        ItemStack outputItem = session.inventory().getItem(session.outputSlot());
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            giveOrDrop(player, outputItem);
        }

        // Return input paper if present
        ItemStack inputItem = session.inventory().getItem(session.inputSlot());
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            giveOrDrop(player, inputItem);
        }

        sessionStore.remove(player);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void handleCraftFailure(Player player, BlueprintCraftingService.CraftingTableResult.Status status) {
        MessageKey key = switch (status) {
            case NO_PAPER        -> BlueprintCraftingMessageKeys.FEEDBACK_NO_PAPER;
            case OUTPUT_OCCUPIED -> BlueprintCraftingMessageKeys.FEEDBACK_OUTPUT_OCCUPIED;
            default              -> null;
        };
        if (key != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.HIGH, java.util.Map.of()));
        }
    }

    /**
     * Attempts to add the item to the player's inventory.
     * If the inventory is full, drops the item at the player's feet.
     */
    private void giveOrDrop(Player player, ItemStack item) {
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private boolean isCraftingTableInventory(Inventory inventory) {
        if (inventory == null) return false;
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof BlueprintCraftingPanelRenderer.CraftingTableHolder;
    }
}
