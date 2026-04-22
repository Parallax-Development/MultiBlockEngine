package dev.darkblade.mbe.uiengine.blueprint;

import dev.darkblade.mbe.api.i18n.MessageKey;

/**
 * Typed message keys for the Blueprint Crafting Table panel.
 * <p>
 * All keys belong to the {@code "mbe"} origin and resolve from
 * {@code lang/<locale>/services.yml} under the {@code services.crafting.ui} path.
 */
public enum BlueprintCraftingMessageKeys implements MessageKey {

    TITLE("services.crafting.ui.title"),
    EMPTY("services.crafting.ui.empty"),
    BLUEPRINT_NAME("services.crafting.ui.blueprint.name"),
    BLUEPRINT_CLICK_TO_SELECT("services.crafting.ui.blueprint.click_to_select"),
    INPUT_NAME("services.crafting.ui.input.name"),
    INPUT_HINT("services.crafting.ui.input.hint"),
    BTN_PREV_PAGE("services.crafting.ui.btn.prev_page"),
    BTN_NEXT_PAGE("services.crafting.ui.btn.next_page"),
    BTN_CLOSE("services.crafting.ui.btn.close"),
    FEEDBACK_NO_PAPER("services.crafting.ui.feedback.no_paper"),
    FEEDBACK_OUTPUT_OCCUPIED("services.crafting.ui.feedback.output_occupied");

    private static final String ORIGIN = "mbe";
    private final String path;

    BlueprintCraftingMessageKeys(String path) {
        this.path = path;
    }

    @Override
    public String origin() {
        return ORIGIN;
    }

    @Override
    public String path() {
        return path;
    }
}
