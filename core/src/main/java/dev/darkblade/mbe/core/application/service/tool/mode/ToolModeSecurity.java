package dev.darkblade.mbe.core.application.service.tool.mode;

import dev.darkblade.mbe.api.command.WrenchContext;

public final class ToolModeSecurity {

    private ToolModeSecurity() {
    }

    public static boolean hasPermission(WrenchContext context, String permission) {
        return context != null
                && context.player() != null
                && permission != null
                && !permission.isBlank()
                && context.player().hasPermission(permission);
    }
}
