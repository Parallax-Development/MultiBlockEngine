package dev.darkblade.mbe.api.tool;

import java.util.Collection;

public interface ToolModeRegistry {
    ToolMode get(String id);
    Collection<ToolMode> all();
    void register(ToolMode mode);
}
