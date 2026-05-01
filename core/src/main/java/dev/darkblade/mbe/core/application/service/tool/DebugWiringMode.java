package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Map;

public final class DebugWiringMode implements ToolMode {

    private static final Map<ActionTrigger, ActionId> BINDINGS = Map.of(
            ActionTrigger.RIGHT_CLICK, WireCutterActions.DEBUG,
            ActionTrigger.SHIFT_RIGHT_CLICK, WrenchActions.SWITCH_MODE
    );

    @Override
    public String id() {
        return "debug_wiring";
    }

    @Override
    public Map<ActionTrigger, ActionId> bindings() {
        return BINDINGS;
    }
}
