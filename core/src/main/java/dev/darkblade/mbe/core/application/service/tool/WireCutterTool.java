package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ToolItem;

import java.util.List;

public final class WireCutterTool implements ToolItem {

    @Override
    public String getId() {
        return "wire_cutter";
    }

    @Override
    public List<String> getSupportedModes() {
        return List.of(
                "disconnect_nodes",
                "split_network",
                "debug_wiring"
        );
    }
}
