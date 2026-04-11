package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Map;

public final class AssemblyMode implements ToolMode {

    private static final Map<ActionTrigger, ActionId> BINDINGS = Map.of(
            ActionTrigger.RIGHT_CLICK, WrenchActions.ASSEMBLE,
            ActionTrigger.LEFT_CLICK, WrenchActions.DISASSEMBLE,
            ActionTrigger.SHIFT_RIGHT_CLICK, WrenchActions.SWITCH_MODE,
            ActionTrigger.SHIFT_LEFT_CLICK, WrenchActions.INSPECT
    );

    @Override
    public String id() {
        return "assembly";
    }

    @Override
    public Map<ActionTrigger, ActionId> bindings() {
        return BINDINGS;
    }
}
