package dev.darkblade.mbe.api.tool;

/**
 * Represents the type of interaction that triggered an action on a tool.
 */
public enum ActionTrigger {
    /** A simple left-click with the mouse. */
    LEFT_CLICK,
    /** A simple right-click with the mouse. */
    RIGHT_CLICK,
    /** A left-click performed while holding the shift key. */
    SHIFT_LEFT_CLICK,
    /** A right-click performed while holding the shift key. */
    SHIFT_RIGHT_CLICK;

    /**
     * Checks if this trigger is a shift-based interaction.
     * 
     * @return true if the trigger involves the shift key
     */
    public boolean isShift() {
        return this == SHIFT_LEFT_CLICK || this == SHIFT_RIGHT_CLICK;
    }

    /**
     * Checks if this trigger is a left-click (with or without shift).
     * 
     * @return true if the trigger is a left-click
     */
    public boolean isLeftClick() {
        return this == LEFT_CLICK || this == SHIFT_LEFT_CLICK;
    }

    /**
     * Checks if this trigger is a right-click (with or without shift).
     * 
     * @return true if the trigger is a right-click
     */
    public boolean isRightClick() {
        return this == RIGHT_CLICK || this == SHIFT_RIGHT_CLICK;
    }
}
