package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;

public final class WrenchActions {

    public static final String NS = "mbe";

    public static final ActionId ASSEMBLE = ActionId.of(NS, "assemble");
    public static final ActionId DISASSEMBLE = ActionId.of(NS, "disassemble");
    public static final ActionId INSPECT = ActionId.of(NS, "inspect");
    public static final ActionId SWITCH_MODE = ActionId.of(NS, "switch_mode");

    private WrenchActions() {
    }
}
