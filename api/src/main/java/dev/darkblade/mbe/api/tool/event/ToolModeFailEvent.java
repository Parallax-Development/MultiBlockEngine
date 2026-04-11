package dev.darkblade.mbe.api.tool.event;

import dev.darkblade.mbe.api.command.WrenchContext;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ToolModeFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String toolId;
    private final String modeId;
    private final WrenchContext context;
    private final String reason;

    public ToolModeFailEvent(String toolId, String modeId, WrenchContext context, String reason) {
        this.toolId = Objects.requireNonNull(toolId, "toolId");
        this.modeId = Objects.requireNonNull(modeId, "modeId");
        this.context = Objects.requireNonNull(context, "context");
        this.reason = Objects.requireNonNull(reason, "reason");
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

    public String getReason() {
        return reason;
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
