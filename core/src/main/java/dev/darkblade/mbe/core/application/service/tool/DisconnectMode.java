package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Map;

public final class DisconnectMode implements ToolMode {

    private static final Map<ActionTrigger, ActionId> BINDINGS = Map.of(
            ActionTrigger.RIGHT_CLICK, WireCutterActions.DISCONNECT,
            ActionTrigger.SHIFT_RIGHT_CLICK, WrenchActions.SWITCH_MODE
    );

    @Override
    public String id() {
        return "disconnect_nodes";
    }

    @Override
    public Map<ActionTrigger, ActionId> bindings() {
        return BINDINGS;
    }
}
