package dev.darkblade.mbe.blueprint;

import dev.darkblade.mbe.api.blueprint.BlueprintCraftingService;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.uiengine.blueprint.BlueprintCraftingSession;
import dev.darkblade.mbe.uiengine.blueprint.BlueprintCraftingSessionStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Core implementation of {@link BlueprintCraftingService}.
 * <p>
 * Handles the transactional crafting step:
 * <ol>
 *   <li>Validates that the player has an active session.</li>
 *   <li>Validates that a valid paper is in the input slot (slot 15).</li>
 *   <li>Validates that the output slot (slot 25) is empty.</li>
 *   <li>Consumes one paper from the input slot.</li>
 *   <li>Places the blueprint {@link ItemStack} in the output slot.</li>
 * </ol>
 */
public final class BlueprintCraftingServiceImpl implements BlueprintCraftingService {

    private final BlueprintCraftingSessionStore sessionStore;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;

    public BlueprintCraftingServiceImpl(
            BlueprintCraftingSessionStore sessionStore,
            ItemService itemService,
            ItemStackBridge itemStackBridge
    ) {
        this.sessionStore    = Objects.requireNonNull(sessionStore, "sessionStore");
        this.itemService     = Objects.requireNonNull(itemService, "itemService");
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
    }

    @Override
    public CraftingTableResult craft(Player player, MultiblockDefinition selected) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(selected, "selected");

        BlueprintCraftingSession session = sessionStore.get(player).orElse(null);
        if (session == null) {
            return new CraftingTableResult(CraftingTableResult.Status.NO_SESSION);
        }

        Inventory inventory = session.inventory();

        // Validate output is free
        ItemStack outputItem = inventory.getItem(session.outputSlot());
        if (outputItem != null && outputItem.getType() != Material.AIR) {
            return new CraftingTableResult(CraftingTableResult.Status.OUTPUT_OCCUPIED);
        }

        // Validate input has paper
        ItemStack inputItem = inventory.getItem(session.inputSlot());
        if (inputItem == null || inputItem.getType() != Material.PAPER || inputItem.getAmount() < 1) {
            return new CraftingTableResult(CraftingTableResult.Status.NO_PAPER);
        }

        // Consume one paper from input
        if (inputItem.getAmount() > 1) {
            inputItem.setAmount(inputItem.getAmount() - 1);
        } else {
            inventory.setItem(session.inputSlot(), null);
        }

        // Create blueprint item and place in output
        ItemStack blueprintStack = BlueprintItem.create(itemService, itemStackBridge, selected);
        if (blueprintStack == null) {
            // Blueprint creation failed — refund the paper
            inventory.setItem(session.inputSlot(), new ItemStack(Material.PAPER, 1));
            return new CraftingTableResult(CraftingTableResult.Status.NO_SESSION);
        }
        inventory.setItem(session.outputSlot(), blueprintStack);

        return new CraftingTableResult(CraftingTableResult.Status.SUCCESS);
    }
}
