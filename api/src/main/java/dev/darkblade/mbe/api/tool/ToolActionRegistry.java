package dev.darkblade.mbe.api.tool;

public interface ToolActionRegistry {
    ToolAction get(ActionId id);
    void register(ToolAction action);
}
