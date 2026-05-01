package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Map;

public final class SplitMode implements ToolMode {

    private static final Map<ActionTrigger, ActionId> BINDINGS = Map.of(
            ActionTrigger.RIGHT_CLICK, WireCutterActions.SPLIT,
            ActionTrigger.SHIFT_RIGHT_CLICK, WrenchActions.SWITCH_MODE
    );

    @Override
    public String id() {
        return "split_network";
    }

    @Override
    public Map<ActionTrigger, ActionId> bindings() {
        return BINDINGS;
    }
}
