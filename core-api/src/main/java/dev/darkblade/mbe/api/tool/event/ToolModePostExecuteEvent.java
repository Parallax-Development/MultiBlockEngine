package dev.darkblade.mbe.api.tool.event;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ToolModePostExecuteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String toolId;
    private final String modeId;
    private final WrenchContext context;
    private final WrenchResult result;

    public ToolModePostExecuteEvent(String toolId, String modeId, WrenchContext context, WrenchResult result) {
        this.toolId = Objects.requireNonNull(toolId, "toolId");
        this.modeId = Objects.requireNonNull(modeId, "modeId");
        this.context = Objects.requireNonNull(context, "context");
        this.result = Objects.requireNonNull(result, "result");
    }

    public String getToolId() {
        return toolId;
    }

    public String getModeId() {
        return modeId;
    }

    public WrenchContext getContext() {
        return context;
    }

    public WrenchResult getResult() {
        return result;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
