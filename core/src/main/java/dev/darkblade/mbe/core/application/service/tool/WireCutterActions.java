package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ActionId;

public final class WireCutterActions {
    public static final String NS = "mbe";

    public static final ActionId DISCONNECT = ActionId.of(NS, "disconnect_nodes");
    public static final ActionId SPLIT = ActionId.of(NS, "split_network");
    public static final ActionId DEBUG = ActionId.of(NS, "debug_wiring");

    private WireCutterActions() {}
}
