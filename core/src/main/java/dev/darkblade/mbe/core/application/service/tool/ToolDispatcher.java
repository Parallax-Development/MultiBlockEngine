package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.ActionTrigger;
import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.tool.ToolActionRegistry;
import dev.darkblade.mbe.api.tool.ToolMode;
import dev.darkblade.mbe.api.tool.ToolModeRegistry;
import dev.darkblade.mbe.api.tool.ToolRegistry;
import dev.darkblade.mbe.api.tool.ToolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

public final class ToolDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToolDispatcher.class);

    private final ToolStateResolver stateResolver;
    private final ToolRegistry toolRegistry;
    private final ToolModeRegistry modeRegistry;
    private final ToolActionRegistry actionRegistry;

    public ToolDispatcher(
            ToolStateResolver stateResolver,
            ToolRegistry toolRegistry,
            ToolModeRegistry modeRegistry,
            ToolActionRegistry actionRegistry
    ) {
        this.stateResolver = Objects.requireNonNull(stateResolver, "stateResolver");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.modeRegistry = Objects.requireNonNull(modeRegistry, "modeRegistry");
        this.actionRegistry = Objects.requireNonNull(actionRegistry, "actionRegistry");
    }

    public WrenchResult dispatch(WrenchContext context, ActionTrigger trigger) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(trigger, "trigger");
        ToolState state = stateResolver.resolve(context.item());
        if (state == null) {
            return WrenchResult.noop();
        }
        Tool tool = toolRegistry.get(state.toolId());
        if (tool == null) {
            return WrenchResult.noop();
        }
        ToolMode mode = modeRegistry.get(state.modeId());
        if (mode == null) {
            return WrenchResult.noop();
        }
        ActionId actionId = mode.resolve(trigger).orElse(null);
        if (actionId == null) {
            return WrenchResult.noop();
        }
        ToolAction action = actionRegistry.get(actionId);
        if (action == null) {
            LOGGER.warn("Tool action not found for id={} tool={} mode={} trigger={}", actionId, tool.id(), mode.id(), trigger);
            return WrenchResult.noop();
        }
        WrenchResult result = action.execute(context);
        return result == null ? WrenchResult.noop() : result;
    }

    public boolean isToolItem(WrenchContext context) {
        if (context == null || context.item() == null || context.item().getType().isAir()) {
            return false;
        }
        ToolState state = stateResolver.resolve(context.item());
        return state != null && toolRegistry.get(normalize(state.toolId())) != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
