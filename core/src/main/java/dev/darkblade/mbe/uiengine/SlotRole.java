package dev.darkblade.mbe.uiengine;

/**
 * Semantic role of a specific slot within an inventory view.
 * <p>
 * Roles are declared in {@code inventories.yml} under the {@code slot_roles} map and are
 * loaded by {@link InventoryConfigLoader}. They are stored in
 * {@link InventoryViewDefinition#slotRoles()} so that specialised listeners (e.g.
 * {@code BlueprintCraftingPanelListener}) can identify functional slots without
 * hard-coding slot indices in Java.
 */
public enum SlotRole {

    /**
     * A slot where the player deposits an ingredient (e.g. a paper to craft a blueprint).
     * Players may freely put or take items from this slot.
     */
    INPUT,

    /**
     * A slot where the result of an operation is placed (e.g. the crafted blueprint).
     * The slot cannot be interacted with by default — the player takes its contents via a
     * normal click, after which the slot is cleared.
     */
    OUTPUT,

    /** Navigation button — navigate to the previous page of a paged catalog. */
    ACTION_PREV,

    /** Navigation button — navigate to the next page of a paged catalog. */
    ACTION_NEXT,

    /** Navigation button — close the panel. */
    ACTION_CLOSE
}
