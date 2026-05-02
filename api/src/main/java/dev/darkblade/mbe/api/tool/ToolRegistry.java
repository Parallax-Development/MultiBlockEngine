package dev.darkblade.mbe.api.tool;

import java.util.Collection;

public interface ToolRegistry {
    Tool get(String id);
    Collection<Tool> all();
    void register(Tool tool);
}
