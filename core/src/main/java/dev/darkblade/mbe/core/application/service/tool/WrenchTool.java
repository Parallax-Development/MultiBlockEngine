package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.ToolItem;

import java.util.List;

public final class WrenchTool implements ToolItem {

    @Override
    public String getId() {
        return "wrench";
    }

    @Override
    public List<String> getSupportedModes() {
        return List.of(
                "config_io",
                "config_channel",
                "link_ports",
                "debug_io"
        );
    }
}
