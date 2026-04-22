package dev.darkblade.mbe.api.blueprint;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.entity.Player;

/**
 * Service contract for the blueprint crafting table panel.
 * <p>
 * Handles the transactional interaction between the player, the paper input slot,
 * and the blueprint output slot. Business logic must never be embedded in UI listeners
 * — they delegate here.
 */
public interface BlueprintCraftingService {

    /**
     * Attempts to craft a blueprint from the given {@link MultiblockDefinition}.
     * <p>
     * Preconditions (validated internally):
     * <ul>
     *   <li>The player must have an active {@code BlueprintCraftingSession}.</li>
     *   <li>The input slot (slot 15) of that session must contain a {@link org.bukkit.Material#PAPER} item.</li>
     *   <li>The output slot (slot 25) must be empty.</li>
     * </ul>
     *
     * @param player   the player who triggered the crafting action
     * @param selected the multiblock definition selected from the catalog
     * @return a {@link CraftingTableResult} describing the outcome
     */
    CraftingTableResult craft(Player player, MultiblockDefinition selected);

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /** Immutable result returned by {@link #craft}. */
    record CraftingTableResult(Status status) {

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public enum Status {
            /** Blueprint was crafted and placed in the output slot. */
            SUCCESS,
            /** The input slot does not contain a valid paper. */
            NO_PAPER,
            /** The output slot already holds an item — player must take it first. */
            OUTPUT_OCCUPIED,
            /** The player has no active crafting session. */
            NO_SESSION
        }
    }
}
