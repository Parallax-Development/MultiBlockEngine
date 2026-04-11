package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.tool.ToolMode;
import dev.darkblade.mbe.api.tool.ToolRegistry;
import dev.darkblade.mbe.api.tool.ToolState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SwitchModeAction implements ToolAction {

    private static final MessageKey MSG_SWITCHED = MessageKey.of("mbe", "core.tool.mode_switched");

    private final ToolStateResolver stateResolver;
    private final ToolRegistry toolRegistry;
    private final I18nService i18n;

    public SwitchModeAction(ToolStateResolver stateResolver, ToolRegistry toolRegistry, I18nService i18n) {
        this.stateResolver = Objects.requireNonNull(stateResolver, "stateResolver");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.i18n = i18n;
    }

    @Override
    public ActionId id() {
        return WrenchActions.SWITCH_MODE;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        ToolState state = stateResolver.resolve(context.item());
        if (state == null) {
            return WrenchResult.noop();
        }
        Tool tool = toolRegistry.get(state.toolId());
        if (tool == null) {
            return WrenchResult.noop();
        }
        List<ToolMode> modes = new ArrayList<>(tool.modes());
        if (modes.isEmpty()) {
            return WrenchResult.noop();
        }
        String current = normalize(state.modeId());
        int currentIndex = 0;
        for (int i = 0; i < modes.size(); i++) {
            if (normalize(modes.get(i).id()).equals(current)) {
                currentIndex = i;
                break;
            }
        }
        ToolMode nextMode = modes.get((currentIndex + 1) % modes.size());
        ToolState nextState = new ToolState(tool.id(), nextMode.id());
        stateResolver.save(context.item(), nextState);
        if (i18n != null && context.player() != null) {
            i18n.send(context.player(), MSG_SWITCHED, Map.of("mode", nextMode.id()));
        }
        return WrenchResult.success(MSG_SWITCHED.path(), Map.of("mode", nextMode.id()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
