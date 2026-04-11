package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolMode;

import java.util.Collection;
import java.util.List;

public final class WrenchTool implements Tool {

    private final List<ToolMode> modes;

    public WrenchTool(ToolMode... modes) {
        this.modes = List.of(modes);
    }

    @Override
    public String id() {
        return "wrench";
    }

    @Override
    public Collection<ToolMode> modes() {
        return modes;
    }
}
