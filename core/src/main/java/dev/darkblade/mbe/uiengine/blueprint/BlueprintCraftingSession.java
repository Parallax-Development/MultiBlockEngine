package dev.darkblade.mbe.uiengine.blueprint;

import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the per-player state of the Blueprint Crafting Table panel.
 * <p>
 * Manages the current page, the full list of available blueprints, and the fixed
 * slot indices for the input (paper) and output (crafted blueprint) slots.
 * <p>
 * Thread-safety: all mutable state is accessed on the main thread via Bukkit events.
 */
public final class BlueprintCraftingSession {

    /** Inventory managed by this session. */
    private final Inventory inventory;

    /** All blueprints available to browse, ordered as provided by the catalog. */
    private final List<MultiblockDefinition> allBlueprints;

    /**
     * The ordered list of slot indices where blueprint icons are rendered.
     * Matches the layout: slots 10-13, 19-22, 28-31, 37-40 (16 slots per page).
     */
    private final List<Integer> blueprintSlots;

    /** Slot that accepts the paper ingredient (slot 15 in the 6-row layout). */
    private final int inputSlot;

    /** Slot where the crafted blueprint is placed (slot 25 in the 6-row layout). */
    private final int outputSlot;

    /** Zero-based current page index. */
    private int currentPage;

    public BlueprintCraftingSession(
            Inventory inventory,
            List<MultiblockDefinition> allBlueprints,
            List<Integer> blueprintSlots,
            int inputSlot,
            int outputSlot
    ) {
        this.inventory       = Objects.requireNonNull(inventory, "inventory");
        this.allBlueprints   = List.copyOf(Objects.requireNonNull(allBlueprints, "allBlueprints"));
        this.blueprintSlots  = List.copyOf(Objects.requireNonNull(blueprintSlots, "blueprintSlots"));
        this.inputSlot       = inputSlot;
        this.outputSlot      = outputSlot;
        this.currentPage     = 0;
    }

    // -------------------------------------------------------------------------
    // Inventory accessor
    // -------------------------------------------------------------------------

    public Inventory inventory() {
        return inventory;
    }

    // -------------------------------------------------------------------------
    // Slot accessors
    // -------------------------------------------------------------------------

    public int inputSlot() {
        return inputSlot;
    }

    public int outputSlot() {
        return outputSlot;
    }

    public List<Integer> blueprintSlots() {
        return blueprintSlots;
    }

    public boolean isBlueprintSlot(int rawSlot) {
        return blueprintSlots.contains(rawSlot);
    }

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    public int pageSize() {
        return blueprintSlots.size();
    }

    public int totalPages() {
        if (allBlueprints.isEmpty()) return 1;
        return (int) Math.ceil((double) allBlueprints.size() / pageSize());
    }

    public int currentPage() {
        return currentPage;
    }

    public boolean hasNextPage() {
        return currentPage < totalPages() - 1;
    }

    public boolean hasPrevPage() {
        return currentPage > 0;
    }

    public void nextPage() {
        if (hasNextPage()) currentPage++;
    }

    public void prevPage() {
        if (hasPrevPage()) currentPage--;
    }

    /**
     * Returns the slice of blueprints that belong to the current page.
     * The list may be smaller than {@link #pageSize()} on the last page.
     */
    public List<MultiblockDefinition> currentPageItems() {
        int from = currentPage * pageSize();
        int to   = Math.min(from + pageSize(), allBlueprints.size());
        if (from >= allBlueprints.size()) return List.of();
        return Collections.unmodifiableList(allBlueprints.subList(from, to));
    }

    /**
     * Returns the {@link MultiblockDefinition} bound to the given raw slot for the current page,
     * or {@code null} if the slot is empty (no blueprint at that position on this page).
     */
    public MultiblockDefinition getBlueprintAt(int rawSlot) {
        int positionInPage = blueprintSlots.indexOf(rawSlot);
        if (positionInPage < 0) return null;
        List<MultiblockDefinition> page = currentPageItems();
        if (positionInPage >= page.size()) return null;
        return page.get(positionInPage);
    }
}
